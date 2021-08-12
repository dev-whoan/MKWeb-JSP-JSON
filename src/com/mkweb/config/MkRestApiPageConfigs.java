package com.mkweb.config;

import java.io.File;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.mkweb.data.MkDeviceData;
import com.mkweb.utils.MkJsonData;
import com.mkweb.data.MkPageJsonData;
import com.mkweb.entity.MkPageConfigCan;
import com.mkweb.logger.MkLogger;

public class MkRestApiPageConfigs extends MkPageConfigCan{
	private HashMap<String, ArrayList<MkPageJsonData>> page_configs = new HashMap<String, ArrayList<MkPageJsonData>>();
	private File[] defaultFiles = null;

	private static MkRestApiPageConfigs pc = null;
	private long lastModified[]; 
	private static final String TAG = "[MkRestPageConfigs]";
	private static final MkLogger mklogger = new MkLogger(TAG);
	private static final String auth = MkConfigReader.Me().get("mkweb.restapi.search.usekey");

	public static MkRestApiPageConfigs Me() {
		if(pc == null)
			pc = new MkRestApiPageConfigs();
		return pc;
	}
	private String[] ctr_list = {
			"name",
			"debug",
			"dir",
			"dir_key",
			"page"
	};
	private String[] ctr_info = new String[ctr_list.length];
	private String[] svc_list = {
			"obj",
			"result",
			"method"	
	};
	
	@Override
	public void setPageConfigs(File[] pageConfigs) {
		page_configs.clear();
		defaultFiles = pageConfigs;
		ArrayList<MkPageJsonData> pageJsonData = null;
		lastModified = new long[pageConfigs.length];
		int lmi = 0;
		for(File defaultFile : defaultFiles)
		{
			if(defaultFile.isDirectory())
				continue;
			
			lastModified[lmi++] = defaultFile.lastModified();
			mklogger.info("=*=*=*=*=*=*=* MkWeb API Page Configs Start*=*=*=*=*=*=*=*=");
			mklogger.info("File: " + defaultFile.getAbsolutePath());
			mklogger.info("=            " + defaultFile.getName() +"              =");
			if(defaultFile == null || !defaultFile.exists())
			{
				mklogger.error("Config file is not exists or null");
				return;
			}

			try(FileReader reader = new FileReader(defaultFile)){
				pageJsonData = new ArrayList<MkPageJsonData>();
				JSONParser jsonParser = new JSONParser();
				JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
				JSONObject pageObject = (JSONObject) jsonObject.get("Controller");

				String pageName = pageObject.get("name").toString();
				String lastURI = pageObject.get("last_uri").toString();
				
				String pageDebugLevel = pageObject.get("debug").toString();
				
				String pageAPI = pageObject.get("api").toString();
				JSONArray serviceArray = (JSONArray) pageObject.get("services");

				JSONObject pageDevice = (JSONObject) pageObject.get("device");

				ArrayList<MkDeviceData> deviceConfig = new ArrayList<>();
				
				Set<String> deviceConfigKey = pageDevice.keySet();

				Iterator<String> deviceConfigIter = deviceConfigKey.iterator();
				while(deviceConfigIter.hasNext()) {
					String deviceControlName = deviceConfigIter.next();
					
					Object dO = pageDevice.get(deviceControlName);
					if(dO != null) {
						JSONObject deviceObject = (JSONObject) dO;

						MkDeviceData tempDevice = new MkDeviceData();
						HashMap<String, String[]> tempDeviceInfo = new HashMap<>();
						
						tempDevice.setControlName(deviceControlName);	// desktop, android, ios

						Set<String> deviceObjectKey = deviceObject.keySet();
						Iterator<String> iterator = deviceObjectKey.iterator();
						while(iterator.hasNext()) {
							String iNext = iterator.next();
							JSONObject deviceService = (JSONObject) deviceObject.get(iNext);
							
							String[] tempDeviceServiceInfo = new String[device_service_filter.length];
							for(int di = 0; di < device_service_filter.length; di++) {
								tempDeviceServiceInfo[di] = deviceService.get(device_service_filter[di]).toString();
								tempDeviceInfo.put(iNext, tempDeviceServiceInfo);
							}
							
							tempDevice.setDeviceInfo(tempDeviceInfo);
						}
						
						if(tempDeviceInfo.get("default") == null) {
							mklogger.temp("[" + defaultFile.getName() +"] Every view controller's device tag must include at least one platform that includes default service. (Device Tag : " + deviceControlName +")", false);
							mklogger.temp("The settings for this view controller is terminated.", false);
							mklogger.flush("error");
							return;
						}
						deviceConfig.add(tempDevice);
					}
				}
				
				for(int i = 0; i < serviceArray.size(); i++) {
					JSONObject serviceObject = (JSONObject) serviceArray.get(i);
					boolean isPageStatic = false; 
					String serviceId = null;
					String serviceType = null;
					JSONObject serviceKinds = null;
					String serviceParameter = null;
					String serviceObjectType = null;
					String serviceMethod = null;

					try {
						isPageStatic = serviceObject.get("page_static").toString().contentEquals("true") ? true : false;
						serviceParameter = serviceObject.get("parameter_name").toString();
						serviceObjectType = serviceObject.get("obj").toString();
						serviceMethod = serviceObject.get("method").toString();
						
						serviceKinds = (JSONObject) serviceObject.get("type");
						
						serviceType = serviceKinds.get("kind").toString();
						serviceId = serviceKinds.get("id").toString();
						
					}catch(NullPointerException npe) {
						 mklogger.error("[Controller: " + pageName + "] Some service of the page doesn't have attributes. Please check the page config.");
						 return;
					}
					
					MkJsonData mkJsonData = new MkJsonData(serviceObject.get("value").toString());
					JSONObject tempValues = null;
					String[] page_value = null;
					
					if(mkJsonData.setJsonObject()) {
						tempValues = mkJsonData.getJsonObject();
					}
					if(tempValues.size() == 0) {
						mklogger.error("[Controller: " + pageName + " | Service ID: " + serviceId+ "] Service doesn't have any value. Service must have at least one value. If the service does not include any value, please create blank one.");
						mklogger.debug("{\"1\":\"\"}");
						continue;
					}
					page_value = new String[tempValues.size()];
					
					for(int j = 0; j < tempValues.size(); j++) {
						page_value[j] = tempValues.get("" + (j+1)).toString();
					}
					
					String controlName = lastURI;
					/*	 Add Index Page	*/
					if(controlName.contentEquals("/")) {
						mklogger.temp("[" + pageName + "] RESTful API view last_uri property must have value.", false);
						mklogger.temp("The settings for this view controller is terminated.", false);
						mklogger.flush("error");
						return;
					}
					
					MkPageJsonData curData = setPageJsonData(isPageStatic,
							controlName,
							lastURI,
							serviceId,
							serviceType,
							pageDebugLevel,
							deviceConfig,
							serviceObjectType,
							serviceMethod,
							serviceParameter,
							page_value,
							(pageAPI.toLowerCase().contentEquals("yes")),
							auth);
					
					printPageInfo(mklogger, curData, "info");
					pageJsonData.add(curData);
					page_configs.put(controlName, pageJsonData);
				}
			} catch (FileNotFoundException e) {
				mklogger.error(e.getMessage());
				e.printStackTrace();
			} catch (IOException e) {
				mklogger.error(e.getMessage());
				e.printStackTrace();
			} catch (ParseException e) {
				mklogger.error(e.getMessage());
				e.printStackTrace();
			}
			mklogger.info("=*=*=*=*=*=*=* MkWeb API Page Configs  Done*=*=*=*=*=*=*=*=");
		}
	}
	
	@Override
	public ArrayList<MkPageJsonData> getControl(String k) {
		for(int i = 0; i < defaultFiles.length; i++)
		{
			if(lastModified[i] != defaultFiles[i].lastModified()){
				setPageConfigs(defaultFiles);
				mklogger.info("==============Reload API Page Config files==============");
				mklogger.info("========Caused by  : different modified time========");
				mklogger.info("==============Reload API Page Config files==============");
				break;
			}
		}
		mklogger.debug("k: " + k);
		if(k == null) {
			mklogger.error(" : Input String data is null");
			return null;
		}

		if(page_configs.get(k) == null)
		{
			mklogger.error(" : The control is unknown. [called control name: " + k + "]");
			return null;
		}
		return page_configs.get(k);
	}
	
	@Override
	protected MkPageJsonData setPageJsonData(boolean pageStatic, String controlName, String pageLastURI, String serviceName, String serviceType, String debugLevel, ArrayList<MkDeviceData> device, String objectType, String method, String PRM_NAME, String[] VAL_INFO, boolean isApi, String auth) {
		MkPageJsonData result = new MkPageJsonData();
		
		result.setPageStatic(pageStatic);
		result.setControlName(controlName);
		result.setLastURI(pageLastURI);
		result.setDebug(debugLevel);
		
		result.setDevice(device);
		
		result.setServiceName(serviceName);		
		result.setServiceType(serviceType);

		result.setObjectType(objectType);
		result.setMethod(method);
		result.setParameter(PRM_NAME);
		result.setData(VAL_INFO);
		result.setAPI(isApi);

		result.setAuthorizedRequire(auth);
		
		LinkedHashMap<String, Boolean> PAGE_VALUE = null;
		PAGE_VALUE = pageValueToHashMap(VAL_INFO);
		result.setPageValue(PAGE_VALUE);
		return result;
	}
	public boolean isApiPageSet() {
		// TODO Auto-generated method stub
		return false;
	}
}
