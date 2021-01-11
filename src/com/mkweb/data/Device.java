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
	/*	key: default, eng, ko ... �� ��� ���� 	*/
	
	/* 
	 * index 0: pageURI		: ���� URI dir
	 * index 1: filePath	: ���� ���� ���
	 * index 2: pageName	: ���� ���� �̸� / ���� ���� URI
	 */
	private HashMap<String, String[]> deviceInfo = new HashMap<>();
	
	public HashMap<String, String[]> getDeviceInfo() {	return this.deviceInfo;	}
	public String[] getDeviceInfo(String service) {	return this.deviceInfo.get(service);	}
	
	public void setDeviceInfo(HashMap<String, String[]> deviceInfo) {	this.deviceInfo = deviceInfo;	}
	public void setDeviceInfo(String service, String[] infors) {	this.deviceInfo.put(service, infors);	}
}