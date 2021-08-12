package com.mkweb.data;

import java.util.HashMap;

public class MkDeviceData extends AbsJsonData{
	/*	Mother : controlName : default, android, ios	*/
	/*	key: default, eng, ko ... 	*/
	
	/* 
	 * index 0: pageName	: URI
	 * index 1: filePath	:
	 * index 2: pageURI		: URI dir
	 */
	private HashMap<String, String[]> deviceInfo = new HashMap<>();
	
	public HashMap<String, String[]> getDeviceInfo() {	return this.deviceInfo;	}
	public String[] getDeviceInfo(String service) {	return this.deviceInfo.get(service);	}
	
	public void setDeviceInfo(HashMap<String, String[]> deviceInfo) {	this.deviceInfo = deviceInfo;	}
	public void setDeviceInfo(String service, String[] infors) {	this.deviceInfo.put(service, infors);	}
}
