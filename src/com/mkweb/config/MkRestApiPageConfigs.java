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
				JSONParser jsonParser = new JSONParser();
				Object curObject = jsonParser.parse(reader);
				JSONArray currentJsonArray = (JSONArray) curObject;
				
				mklogger.debug(TAG, "\nJSON Array\n" + currentJsonArray);
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
		String controlName = jsonData.getControlName();
		String pageParamName = jsonData.getPageStaticParamsName();
		ArrayList<String> pageParams = jsonData.getPageStaticParams();
		String serviceName = jsonData.getServiceName();
		String serviceType = jsonData.getServiceType();
		String logicalDir = jsonData.getLogicalDir();

		String pageDir = jsonData.getDir();
		String pageName = jsonData.getPageName();
		String debugLevel = jsonData.getDebug();

		String[] SQL_INFO = jsonData.getSql();
		String sql_info = "SQL:\t";
		for(int i = 0; i < SQL_INFO.length; i++) {
			if(i != SQL_INFO.length-1)
				sql_info += SQL_INFO[i] + "\t";
			else
				sql_info += SQL_INFO[i];
		}
		String PRM_NAME = jsonData.getParameter();
		String VAL_INFO = jsonData.getData();
		String PAGE_VAL = null;

		String valMsg = "No Page Value";
		String[] valBuffer = null;
		
		if(VAL_INFO != null)
		{
			valBuffer = VAL_INFO.split("\n");
			PAGE_VAL = "";
		
			for (int ab = 0; ab < valBuffer.length; ab++) {
				String tempVal = valBuffer[ab].trim();
				if(valMsg == "")
					valMsg = tempVal;
				else
					valMsg += ("\n\t" + tempVal);
			}
			LinkedHashMap<String, Boolean> pvv = jsonData.getPageValue();
			if(pvv != null) {
				for(int aabb = 0; aabb < pvv.size(); aabb++) {
					PAGE_VAL += pvv.get(aabb);
					if(aabb < pvv.size()-1) {
						PAGE_VAL += ", ";
					}
				}
			}
			
		}
		String PRM = "";
		if(pageParams != null) {
			for(int pr = 0; pr < pageParams.size(); pr++) {
				PRM += pageParams.get(pr);
				if(pr < pageParams.size() - 1)
					PRM += ", ";
			}
		}else {
			PRM = null;
		}
		String tempMsg = "\n忙式式式式式式式式式式式式式式式式式式式式式式式式式式Page Control  :  " + controlName + "式式式式式式式式式式式式式式式式式式式式式式式式式式式式"
				+ "\n弛View Dir:\t" + pageDir + "\t\tView Page:\t" + pageName
				+ "\n弛Logical Dir:\t" + logicalDir + "\t\tDebug Level:\t" + debugLevel
				+ "\n弛Static Param Name:\t" + pageParamName + "\t\tStatic Param Value:\t" + PRM
				+ "\n弛Service Name:\t" + serviceName + "\nType:\t" + serviceType + "\tParameter:\t" + PRM_NAME;

		if(type == "info") {
			tempMsg +="\n弛SQL:\t" + sql_info
					+ "\n弛Value:\t" + valMsg
					+ "\n弛SET  :\t" + PAGE_VAL
					+ "\n戌式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式";
		}else {
			tempMsg += "\n戌式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式";
		}
		mklogger.temp(tempMsg, false);
		mklogger.flush("info");
	}

	@Override
	public ArrayList<PageJsonData> getControl(String k) {
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

		if(k == null) {
			mklogger.error(TAG + " : Input String data is null");
			return null;
		}

		if(page_configs.get(k) == null)
		{
			mklogger.error(TAG + " : The control is unknown. [called control name: " + k + "]");
			return null;
		}
		return page_configs.get(k);
	}
	
	@Override
	protected PageJsonData setPageJsonData(String pageParamName, ArrayList<String> pageParam, String serviceName, String serviceType, String[] ctr_info, String[] sqlInfo, String PRM_NAME, String VAL_INFO, String STRUCTURE) {
		PageJsonData result = new PageJsonData();
		result.setPageStaticParamName(pageParamName);
		result.setPageStaticParams(pageParam);
		result.setControlName(ctr_info[0]);
		result.setServiceName(serviceName);
		result.setServiceType(serviceType);
		result.setLogicalDir(ctr_info[3]);
		result.setDir(ctr_info[2]);
		
		result.setPageName(ctr_info[4]);
		result.setDebug(ctr_info[1]);

		result.setSql(sqlInfo);
		result.setParameter(PRM_NAME);
		result.setData(VAL_INFO);

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
