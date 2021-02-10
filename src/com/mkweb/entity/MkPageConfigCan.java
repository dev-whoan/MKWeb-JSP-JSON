package com.mkweb.entity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import com.mkweb.data.Device;
import com.mkweb.data.MkPageJsonData;
import com.mkweb.logger.MkLogger;

public abstract class MkPageConfigCan extends MkPageJsonData {
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
	protected String[] device_service_filter = {
			"path",
			"file",
			"uri"
	};
	
	public abstract ArrayList<MkPageJsonData> getControl(String k);
	public abstract void setPageConfigs(File[] pageConfigs);
	protected abstract MkPageJsonData setPageJsonData(boolean pageStatic, String controlName, String pageLastURI, String serviceName, String serviceType, String debugLevel, ArrayList<Device> device, String objectType, String method, String PRM_NAME, String[] VAL_INFO, boolean isApi);

	public void printPageInfo(MkLogger mklogger, String TAG, MkPageJsonData jsonData, String type) {
		String[] VAL_INFO = jsonData.getData();
		String valMsg = "";
		for(int i = 0; i < VAL_INFO.length; i++) {
			valMsg += VAL_INFO[i];
			
			if(i < VAL_INFO.length-1) {
				valMsg += ", ";
			}
		}
		
		String deviceMessage = "\n|==============================Device Control===============================";
		ArrayList<Device> devices = jsonData.getAllDevices();
		
		for(Device d : devices) {
			String deviceControl = d.getControlName();
			HashMap<String, String[]> deviceLanguage = d.getDeviceInfo();
			deviceMessage += "\n|Device Type:\t" + deviceControl;
			
			Set<String> deviceObjectKey = deviceLanguage.keySet();
			Iterator<String> iterator = deviceObjectKey.iterator();
			while(iterator.hasNext()) {
				String ds = iterator.next();
				String[] dsi = deviceLanguage.get(ds);
				deviceMessage += "\n|Language:\t" + ds + "\t\tFile Path:\t" + dsi[0]
						+ "\n|File Name:\t" + dsi[1] + "\t\tDevice URI:\t" + dsi[2];
			}
		}
		deviceMessage += "\n|===========================================================================";

		String tempMsg = "\n===============================Page Control================================="
				+ "\n|Control Name:\t(" + jsonData.getControlName() + ")\t\tLast URI:\t" + jsonData.getLastURI()
				+ deviceMessage
				+ "\n|Debug Level:\t" + jsonData.getDebug()
				+ "\n|Page Static:\t" + jsonData.getPageStatic() + "\t\tService Name:\t" + jsonData.getServiceName()
				+ "\n|Type:\t" + jsonData.getServiceType() + "\tParameter:\t" + jsonData.getParameter()
				+ "\n|API :\t" + jsonData.IsApiPage();

		if(!type.contentEquals("no-sql")) {
			tempMsg +="\n|SQL:\t" + jsonData.getObjectType() + "\tMethod:\t" + jsonData.getMethod()
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