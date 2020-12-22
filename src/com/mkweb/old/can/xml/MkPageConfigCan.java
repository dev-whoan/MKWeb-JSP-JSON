package com.mkweb.old.can.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import com.mkweb.data.PageJsonData;
import com.mkweb.logger.MkLogger;

public abstract class MkPageConfigCan extends PageJsonData {
	protected String[] svc_list = null;
	protected String[] ctr_list = null;
	protected String[] ctr_info = null;
	
	protected void setSlList(String[] sl_list) {
		this.svc_list = sl_list;
	}
	protected void setClList(String[] cl_list) {
		this.ctr_list = cl_list;
	}
	protected void setClInfo(String[] cl_info) {
		this.ctr_info = cl_info;
	}
	
	protected String[] getSlList() {
		return this.svc_list;
	}
	protected String[] getClList() {
		return this.ctr_list;
	}
	protected String[] getClInfo() {
		return this.ctr_info;
	}
	
	public abstract ArrayList<PageJsonData> getControl(String k);
	public abstract void setPageConfigs(File[] pageConfigs);
	public abstract void printPageInfo(PageJsonData jsonData, String type);
	protected abstract PageJsonData setPageJsonData(String pageParamName, ArrayList<String> pageParam, String serviceName, String serviceType, String[] ctr_info, String[] sqlInfo, String PRM_NAME, String VAL_INFO, String STRUCTURE);
	protected LinkedHashMap<String, Boolean> pageValueToHashMap(String pageValue){
    	LinkedHashMap<String, Boolean> result = null;
    	if(pageValue == null || pageValue.length() == 0) 
    		return null;
    	
    	result = new LinkedHashMap<>();
    	String[] splits = pageValue.split("@set");
    	
    	if(splits.length == 1)
    		return null;
    	for(int i = 1; i < splits.length; i++) {
    		String s = null;
    		splits[i] = splits[i].trim();
    		
    		if(splits[i].contains(".")) {
    			s = splits[i].split("\\.")[1];
    		}else {
    			if(splits[i].contains("("))
    				s = splits[i].split("\\(")[1];
    			else
    				s = splits[i];
    		}
    		
    		if(!s.contains("=")) {
    			MkLogger.Me().error("[PageConfigCan] Invalid Page Value. There is no equal character. :: " + s);
    			return null;
    		}
    		
    		s = s.split("=")[0].trim();
    		result.put(s, true);
    	}
    	return result;
    }
	
}
