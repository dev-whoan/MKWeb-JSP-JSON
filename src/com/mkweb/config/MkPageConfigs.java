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

import com.mkweb.can.MkPageConfigCan;
import com.mkweb.data.Device;
import com.mkweb.data.MkJsonData;
import com.mkweb.data.PageJsonData;
import com.mkweb.logger.MkLogger;

public class MkPageConfigs extends MkPageConfigCan{
	private HashMap<String, ArrayList<PageJsonData>> page_configs = new HashMap<String, ArrayList<PageJsonData>>();
	private HashMap<String, String> allowURI = new HashMap<String, String>();
	private File[] defaultFiles = null;

	private static MkPageConfigs pc = null;
	private long lastModified[];
	private MkLogger mklogger = MkLogger.Me();
	private String TAG = "[PageConfigs]";

	public static MkPageConfigs Me() {
		if(pc == null)
			pc = new MkPageConfigs();
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
			"method"	
	};
	
	@Override
	public void setPageConfigs(File[] pageConfigs) {
		page_configs.clear();
		defaultFiles = pageConfigs;
		ArrayList<PageJsonData> pageJsonData = null;
		lastModified = new long[pageConfigs.length];
		int lmi = 0;
		for(File defaultFile : defaultFiles)
		{
			if(defaultFile.isDirectory())
				continue;
			
			lastModified[lmi++] = defaultFile.lastModified();
			mklogger.info("=*=*=*=*=*=*=* MkWeb Page Configs Start*=*=*=*=*=*=*=*=");
			mklogger.info(TAG + "File: " + defaultFile.getAbsolutePath());
			mklogger.info("=            " + defaultFile.getName() +"              =");
			if(defaultFile == null || !defaultFile.exists())
			{
				mklogger.error("Config file is not exists or null");
				return;
			}

			try(FileReader reader = new FileReader(defaultFile)){
				pageJsonData = new ArrayList<PageJsonData>();
				JSONParser jsonParser = new JSONParser();
				JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
				JSONObject pageObject = (JSONObject) jsonObject.get("Controller");

				String controlName = pageObject.get("name").toString();
				String lastURI = pageObject.get("last_uri").toString();
				String pageDebugLevel = pageObject.get("debug").toString();
				/*
				Device로 이전 사항
				String pageFilePath = pageObject.get("path").toString();
				String pageFile = pageObject.get("file").toString();
				String pageURI = pageObject.get("uri").toString();
				*/
				String pageAPI = pageObject.get("api").toString();
				JSONArray serviceArray = (JSONArray) pageObject.get("services");
				
				boolean isPageStatic = false; 
				String serviceId = null;
				String serviceType = null;
				JSONObject serviceKinds = null;
				String serviceParameter = null;
				String serviceObjectType = null;
				String serviceMethod = null;
				String[] page_value = null;
				boolean isApiService = false;
				
				/*
				 * device태그는 무조건 있고 JSONObject
				 */
				JSONObject pageDevice = (JSONObject) pageObject.get("device");
				
				/*	디바이스 구분 : desktop, android, ios 최대 3개*/
				ArrayList<Device> deviceConfig = new ArrayList<>();
				
				Set<String> deviceConfigKey = pageDevice.keySet();

				Iterator<String> deviceConfigIter = deviceConfigKey.iterator();
				while(deviceConfigIter.hasNext()) {
					String deviceControlName = deviceConfigIter.next();
					
					Object dO = pageDevice.get(deviceControlName);
					if(dO != null) {
						//desktop, android, ios에 대한 JSONObject
						JSONObject deviceObject = (JSONObject) dO;
						
						//만들기 위한 Device 설정
						Device tempDevice = new Device();
						// tempDeviceInfo(language, JSONObject(path, file, uri));
						HashMap<String, String[]> tempDeviceInfo = new HashMap<>();
						
						tempDevice.setControlName(deviceControlName);	// desktop, android, ios
						
						//Device Controller가 갖고있는 key를 찾아야 합니다.
						Set<String> deviceObjectKey = deviceObject.keySet();
						Iterator<String> iterator = deviceObjectKey.iterator();
						while(iterator.hasNext()) {
							String iNext = iterator.next();
							tempDevice.setServiceName(iNext);
							JSONObject deviceService = (JSONObject) deviceObject.get(iNext);
							
							String[] tempDeviceServiceInfo = new String[device_service_filter.length];
							for(int di = 0; di < device_service_filter.length; di++) {
								String deviceURI = deviceService.get(device_service_filter[di]).toString();
								tempDeviceServiceInfo[di] = deviceURI;
								tempDeviceInfo.put(iNext, tempDeviceServiceInfo);
								if(di == 2) {
									if(!deviceURI.contentEquals("") && deviceURI != null) {
										if(!lastURI.contentEquals("") && lastURI != null) {
											if(  deviceURI.charAt(deviceURI.length()-1) != '/' ) {
												deviceURI += "/";
											}											
										}
									}
									
									allowURI.put((deviceURI + lastURI), controlName);
								}
							}
							
							tempDevice.setDeviceInfo(tempDeviceInfo);
						}
						
						if(tempDeviceInfo.get("default") == null) {
							mklogger.temp(TAG + "[" + defaultFile.getName() +"] Every view controller's device tag must include at least one platform that includes default service. (Device Tag : " + deviceControlName +")", false);
							mklogger.temp("The settings for this view controller is terminated.", false);
							mklogger.flush("error");
							return;
						}
						deviceConfig.add(tempDevice);
					}
				}
				
				
				if(serviceArray.size() > 0) {
					for(int i = 0; i < serviceArray.size(); i++) {
						JSONObject serviceObject = (JSONObject) serviceArray.get(i);
						
						try {
							isPageStatic = serviceObject.get("page_static").toString().contentEquals("true") ? true : false;
							serviceParameter = serviceObject.get("parameter_name").toString();
							serviceObjectType = serviceObject.get("obj").toString();
							serviceMethod = serviceObject.get("method").toString();
							
							serviceKinds = (JSONObject) serviceObject.get("type");
							
							serviceType = serviceKinds.get("kind").toString();
							serviceId = serviceKinds.get("id").toString();
						}catch(NullPointerException npe) {
							 mklogger.error("[Controller: " + controlName + "] Some service of the page doesn't have attributes. Please check the page config.");
							 return;
						}
						
						MkJsonData mkJsonData = new MkJsonData(serviceObject.get("value").toString());
						JSONObject tempValues = null;
						
						if(mkJsonData.setJsonObject()) {
							tempValues = mkJsonData.getJsonObject();
						}
						if(tempValues.size() == 0) {
							mklogger.error(TAG, "[Controller: " + controlName + " | Service ID: " + serviceId+ "] Service doesn't have any value. Service must have at least one value. If the service does not include any value, please create blank one.");
							mklogger.debug(TAG, "{\"1\":\"\"}");
							continue;
						}
						page_value = new String[tempValues.size()];
						
						for(int j = 0; j < tempValues.size(); j++) {
							page_value[j] = tempValues.get("" + (j+1)).toString();
						}
						
						isApiService = (pageAPI.toLowerCase().contentEquals("yes"));
						
						/*	 Add Index Page	*/
						if(controlName.contentEquals("/")) 
							controlName = "";
						
						PageJsonData curData = setPageJsonData(isPageStatic,
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
								isApiService);
						
						printPageInfo(mklogger, TAG, curData, "info");
						pageJsonData.add(curData);
						page_configs.put(controlName, pageJsonData);
					}
				}else {
					serviceId = "No Service";
					serviceType = "No Service";
					serviceObjectType = "No service";
					serviceMethod = "No service";
					page_value = new String[1];
					page_value[0] = "";
					isApiService = false;
					
					/*	 Add Index Page	*/
					if(controlName.contentEquals("/")) 
						controlName = "";
					PageJsonData curData = setPageJsonData(isPageStatic,
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
							isApiService);

					printPageInfo(mklogger, TAG, curData, "info");
					pageJsonData.add(curData);
					page_configs.put(controlName, pageJsonData);
				}
				
			} catch (FileNotFoundException e) {
				mklogger.error(TAG, "defaultFile.getName()FileNOtFoundException: " + e.getMessage());
				e.printStackTrace();
			} catch (IOException e) {
				mklogger.error(TAG, "IOException: " + e.getMessage());
				e.printStackTrace();
			} catch (ParseException e) {
				mklogger.error(TAG, "ParseException : " + e.getMessage());
				e.printStackTrace();
			} 
			mklogger.info("=*=*=*=*=*=*=* MkWeb Page Configs  Done*=*=*=*=*=*=*=*=");
		}
	}

	private String getURIControl(String requestURI) {	return allowURI.get(requestURI);	}
	
	@Override
	public ArrayList<PageJsonData> getControl(String requestURI) {
		String mkPage = getURIControl(requestURI);
		for(int i = 0; i < defaultFiles.length; i++)
		{
			if(lastModified[i] != defaultFiles[i].lastModified()){
				defaultFiles[i].lastModified();
				setPageConfigs(defaultFiles);
				mklogger.info("==============Reload Page Config files==============");
				mklogger.info("========Caused by  : different modified time========");
				mklogger.info("File: " + defaultFiles[i].getName() + "|" + defaultFiles[i].lastModified());
				mklogger.info("==============Reload Page Config files==============");
				break;
			}
		}

		if(mkPage == null) {
			mklogger.error(TAG + " : Input String data is null");
			return null;
		}

		if(page_configs.get(mkPage) == null)
		{
			mklogger.error(TAG + " : The control is unknown. [called control name: " + mkPage + ", uri:" + requestURI + "]");
			return null;
		}
		return page_configs.get(mkPage);
	}

	@Override
	protected PageJsonData setPageJsonData(boolean pageStatic, String controlName, String pageLastURI, String serviceName, String serviceType, String debugLevel, ArrayList<Device> device, String objectType, String method, String PRM_NAME, String[] VAL_INFO, boolean isApi) {
		PageJsonData result = new PageJsonData();
		
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

		LinkedHashMap<String, Boolean> PAGE_VALUE = null;
		PAGE_VALUE = pageValueToHashMap(VAL_INFO);
		result.setPageValue(PAGE_VALUE);
		return result;
	}
}
