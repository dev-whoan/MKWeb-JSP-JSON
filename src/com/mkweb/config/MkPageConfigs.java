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
import com.mkweb.data.PageJsonData;
import com.mkweb.logger.MkLogger;

public class MkPageConfigs extends MkPageConfigCan{
	private HashMap<String, ArrayList<PageJsonData>> page_configs = new HashMap<String, ArrayList<PageJsonData>>();
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
	
	private ArrayList<String> setPageParamToStrig(String pageParam) {
		if(pageParam == null)
			return null;
		String[] tempPageParam = pageParam.split("@set" + "\\(");
		String[] tempPageParam2 = new String[tempPageParam.length];
		if(tempPageParam.length == 1) 
			return null;

		for(int i = 0; i < tempPageParam.length; i++) {
			tempPageParam2[i] = tempPageParam[i].split("=")[0];
		}

		if(tempPageParam2.length == 1)
			return null;

		ArrayList<String> result = new ArrayList<String>();

		for(int i = 1; i < tempPageParam2.length; i++) {
			result.add(tempPageParam2[i].trim());
		}

		return result;
	}
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

				String pageName = pageObject.get("name").toString();
				String pageDebugLevel = pageObject.get("debug").toString();
				String pageFilePath = pageObject.get("path").toString();
				String pageFile = pageObject.get("file").toString();
				String pageURI = pageObject.get("uri").toString();
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
					
					String serviceValue = null;
					JSONObject serviceValues[] = null;
					JSONArray serviceValueArray = null;
					String[] page_value = null;
					try {
						serviceValueArray = (JSONArray) serviceObject.get("value");
						serviceValue = null;
						
						serviceValues = new JSONObject[serviceValueArray.size()];
						
						for(int j = 0; j < serviceValues.length; j++) {
							serviceValues[j] = (JSONObject) serviceValueArray.get(j);
						}
					}catch(Exception e) {
						mklogger.warn(TAG, "[Controller: " + pageName + " | Service ID: " + serviceId + "] We recommend you to set page value into JSONArray. But you set as a single String.");
					}finally {
						serviceValueArray = null;
						serviceValue = serviceObject.get("value").toString();
					}
					
					if(serviceValues != null) {
						page_value = new String[serviceValues.length];
						
						for(int j = 0; j < serviceValues.length; j++) {
							page_value[j] = serviceValues[j].get("" + (j+1)).toString();
						}
						
					}else {
						page_value = new String[1];
						page_value[0] = serviceValue;
					}
					String[] ctr_info = {pageName, pageDebugLevel, pageFilePath, pageURI, pageFile};
					String[] sql_info = {serviceObjectType, serviceMethod};

					PageJsonData curData = setPageJsonData(isPageStatic,
							serviceId,
							serviceType,
							ctr_info,
							sql_info,
							serviceParameter,
							page_value);
					
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
			mklogger.info("=*=*=*=*=*=*=* MkWeb Page Configs  Done*=*=*=*=*=*=*=*=");
		}
	}

	@Override
	public void printPageInfo(PageJsonData jsonData, String type) {

		String[] SQL_INFO = jsonData.getSql();
		String sql_info = "";
		for(int i = 0; i < SQL_INFO.length; i++) {
			if(i != SQL_INFO.length-1)
				sql_info += SQL_INFO[i] + "\t";
			else
				sql_info += SQL_INFO[i];
		}
		
		String[] VAL_INFO = jsonData.getData();
		
		String valMsg = "";
		
		for(int i = 0; i < VAL_INFO.length; i++) {
			valMsg += VAL_INFO[i];
			
			if(i < VAL_INFO.length-1) {
				valMsg += ", ";
			}
		}
		
		String tempMsg = "\n忙式式式式式式式式式式式式式式式式式式式式式式式式式式式式Page Control  :  " + jsonData.getControlName() + "式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式"
				+ "\n弛View Dir:\t" + jsonData.getPageURI() + "\t\tView Page:\t" + jsonData.getPageName()
				+ "\n弛Logical Dir:\t" + jsonData.getLogicalDir() + "\t\tDebug Level:\t" + jsonData.getDebug()
				+ "\n弛Page Static:\t" + jsonData.getPageStatic() + "\t\tService Name:\t" + jsonData.getServiceName()
				+ "\n弛Type:\t" + jsonData.getServiceType() + "\tParameter:\t" + jsonData.getParameter();

		if(!type.contentEquals("no-sql")) {
			tempMsg +="\n弛SQL:\t" + sql_info
					+ "\n弛Value:\t" + valMsg
					+ "\n戌式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式";
			mklogger.temp(tempMsg, false);
			mklogger.flush(type);
		}else {
			tempMsg += "\n戌式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式";
			mklogger.temp(tempMsg, false);
			mklogger.flush("info");
		}
		
	}

	@Override
	public ArrayList<PageJsonData> getControl(String mkPage) {
		for(int i = 0; i < defaultFiles.length; i++)
		{
			if(lastModified[i] != defaultFiles[i].lastModified()){
				setPageConfigs(defaultFiles);
				mklogger.info("==============Reload Page Config files==============");
				mklogger.info("========Caused by  : different modified time========");
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
			mklogger.error(TAG + " : The control is unknown. [called control name: " + mkPage + "]");
			return null;
		}
		return page_configs.get(mkPage);
	}

	@Override
	protected PageJsonData setPageJsonData(boolean pageStatic, String serviceName, String serviceType, String[] ctr_info, String[] sqlInfo, String PRM_NAME, String[] VAL_INFO) {
		PageJsonData result = new PageJsonData();
		
		result.setPageStatic(pageStatic);
		result.setControlName(ctr_info[0]);
		result.setDebug(ctr_info[1]);
		result.setPageURI(ctr_info[2]);
		result.setLogicalDir(ctr_info[3]);
		result.setPageName(ctr_info[4]);
		
		result.setServiceName(serviceName);
		result.setServiceType(serviceType);

		result.setSql(sqlInfo);
		result.setParameter(PRM_NAME);
		result.setData(VAL_INFO);

		LinkedHashMap<String, Boolean> PAGE_VALUE = null;
		PAGE_VALUE = pageValueToHashMap(VAL_INFO);
		result.setPageValue(PAGE_VALUE);
		return result;
	}
}
