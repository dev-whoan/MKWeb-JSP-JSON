package com.mkweb.config;

import java.io.File;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.mkweb.can.MkPageConfigCan;
import com.mkweb.data.MkJsonData;
import com.mkweb.data.PageJsonData;
import com.mkweb.logger.MkLogger;

public class MkRestApiPageConfigs extends MkPageConfigCan{
	private HashMap<String, ArrayList<PageJsonData>> page_configs = new HashMap<String, ArrayList<PageJsonData>>();
	private File[] defaultFiles = null;

	private static MkRestApiPageConfigs pc = null;
	private long lastModified[]; 
	private MkLogger mklogger = MkLogger.Me();

	private String TAG = "[PageConfigs]";

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
		ArrayList<PageJsonData> pageJsonData = null;
		lastModified = new long[pageConfigs.length];
		int lmi = 0;
		for(File defaultFile : defaultFiles)
		{
			lastModified[lmi++] = defaultFile.lastModified();
			mklogger.info("=*=*=*=*=*=*=* MkWeb API Page Configs Start*=*=*=*=*=*=*=*=");
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

				String pageName = pageObject.get("name").toString();
				String pageDebugLevel = pageObject.get("debug").toString();
				String pageFilePath = pageObject.get("path").toString();
				String pageFile = pageObject.get("file").toString();
				String pageURI = pageObject.get("uri").toString();
				String pageAPI = pageObject.get("api").toString();
				JSONArray serviceArray = (JSONArray) pageObject.get("services");

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
						mklogger.error(TAG, "[Controller: " + pageName + " | Service ID: " + serviceId+ "] Service doesn't have any value. Service must have at least one value. If the service does not include any value, please create blank one.");
						mklogger.debug(TAG, "{\"1\":\"\"}");
						continue;
					}
					page_value = new String[tempValues.size()];
					
					for(int j = 0; j < tempValues.size(); j++) {
						page_value[j] = tempValues.get("" + (j+1)).toString();
					}
					
					String[] ctr_info = {pageName, pageDebugLevel, pageFilePath, pageURI, pageFile};

					PageJsonData curData = setPageJsonData(isPageStatic,
							serviceId,
							serviceType,
							ctr_info,
							serviceObjectType,
							serviceMethod,
							serviceParameter,
							page_value,
							(pageAPI.toLowerCase().contentEquals("yes")));
					
					printPageInfo(curData, "info");
					pageJsonData.add(curData);
					page_configs.put(ctr_info[0], pageJsonData);
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
	public void printPageInfo(PageJsonData jsonData, String type) {
		String[] VAL_INFO = jsonData.getData();
		
		String valMsg = "";
		
		for(int i = 0; i < VAL_INFO.length; i++) {
			valMsg += VAL_INFO[i];
			
			if(i < VAL_INFO.length-1) {
				valMsg += ", ";
			}
		}
		
		String tempMsg = "\n=============================Page Control  :  " + jsonData.getControlName() + "=============================="
				+ "\n|View Dir:\t" + jsonData.getPageURI() + "\t\tView Page:\t" + jsonData.getPageName()
				+ "\n|Logical Dir:\t" + jsonData.getLogicalDir() + "\t\tDebug Level:\t" + jsonData.getDebug()
				+ "\n|Page Static:\t" + jsonData.getPageStatic() + "\t\tService Name:\t" + jsonData.getServiceName()
				+ "\n|Type:\t" + jsonData.getServiceType() + "\tParameter:\t" + jsonData.getParameter()
				+ "\n|API :\t" + jsonData.IsApiPage();
		if(!type.contentEquals("no-sql")) {
			tempMsg +="\n|SQL:\t" + jsonData.getObjectType() + "\tMethod\t" + jsonData.getMethod()
					+ "\n|Value:\t" + valMsg
					+ "\n============================================================================";
			mklogger.temp(tempMsg, false);
			mklogger.flush(type);
		}else {
			tempMsg += "\n============================================================================";
			mklogger.temp(tempMsg, false);
			mklogger.flush("info");
		}
	}

	@Override
	public ArrayList<PageJsonData> getControl(String k) {
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

		if(k == null) {
			mklogger.error(TAG, " : Input String data is null");
			return null;
		}

		if(page_configs.get(k) == null)
		{
			mklogger.error(TAG, " : The control is unknown. [called control name: " + k + "]");
			return null;
		}
		return page_configs.get(k);
	}
	
	@Override
	protected PageJsonData setPageJsonData(boolean pageStatic, String serviceName, String serviceType, String[] ctr_info, String objectType, String method, String PRM_NAME, String[] VAL_INFO, boolean isApi) {
		PageJsonData result = new PageJsonData();
		
		result.setPageStatic(pageStatic);
		result.setControlName(ctr_info[0]);
		result.setDebug(ctr_info[1]);
		result.setPageURI(ctr_info[2]);
		result.setLogicalDir(ctr_info[3]);
		result.setPageName(ctr_info[4]);
		
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
	public boolean isApiPageSet() {
		// TODO Auto-generated method stub
		return false;
	}
}
