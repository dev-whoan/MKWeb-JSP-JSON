package com.mkweb.entity;

public interface JsonData {
	String getControlName();
	void setControlName(String controlName);
	
	String getServiceName();
	void setServiceName(String serviceName);
	
	String[] getData();
	void setData(String[] data);
}