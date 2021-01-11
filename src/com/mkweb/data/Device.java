package com.mkweb.data;

import java.util.HashMap;

public class Device extends AbsJsonData{
	/*
	 * protected String serviceName = null;
	protected String serviceType = null;
	protected String controlName = null;
	protected String[] data = null;
	protected String Tag = null;
	protected static String absPath = "/WEB-INF";
	 */
	
	/*	Mother : controlName : default, android, ios	*/
	/*	key: default, eng, ko ... 등 언어 설정 	*/
	
	/* 
	 * index 0: pageName	: 실제 파일 이름 / 연결 종단 URI
	 * index 1: filePath	: 실제 파일 경로
	 * index 2: pageURI		: 연결 URI dir
	 */
	private HashMap<String, String[]> deviceInfo = new HashMap<>();
	
	public HashMap<String, String[]> getDeviceInfo() {	return this.deviceInfo;	}
	public String[] getDeviceInfo(String service) {	return this.deviceInfo.get(service);	}
	
	public void setDeviceInfo(HashMap<String, String[]> deviceInfo) {	this.deviceInfo = deviceInfo;	}
	public void setDeviceInfo(String service, String[] infors) {	this.deviceInfo.put(service, infors);	}
}
