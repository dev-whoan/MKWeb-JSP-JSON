package com.mkweb.impl;

public interface JsonData {
	String getTag();
	String getControlName();
	void setControlName(String controlName);
	
	String[] getData();
	void setData(String[] data);
	
	String getMyInfo();
}