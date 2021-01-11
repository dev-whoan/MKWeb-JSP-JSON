package com.mkweb.can;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import com.mkweb.data.PageJsonData;

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
	protected abstract PageJsonData setPageJsonData(boolean pageStatic, String controlName, String serviceName, String serviceType, String[] ctr_info, String objectType, String method, String PRM_NAME, String[] VAL_INFO, boolean isApi);

	protected LinkedHashMap<String, Boolean> pageValueToHashMap(String[] pageValue){
    	LinkedHashMap<String, Boolean> result = null;
    	if(pageValue == null) 
    		return null;
    	else if(pageValue.length == 0)
    		return null;
    	
    	result = new LinkedHashMap<>();
    	for(int i = 0; i < pageValue.length; i++) {
    		if(pageValue[i].length() > 0)
    			result.put(pageValue[i], true);
    	}
    	return result;
    }
	
}