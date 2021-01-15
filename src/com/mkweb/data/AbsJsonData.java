package com.mkweb.data;

import com.mkweb.impl.JsonData;

public class AbsJsonData implements JsonData {
	
	protected String serviceName = null;
	protected String serviceType = null;
	protected String controlName = null;
	protected String[] data = null;
	protected String Tag = null;
	protected static String absPath = "/WEB-INF";
	
	
	public void setServiceName(String serviceName) { this.serviceName = serviceName; }
	public String getServiceName() {	return this.serviceName;	}
	
	public void setServiceType(String serviceType) { this.serviceType = serviceType; }
	public String getServiceType() {	return this.serviceType;	}
	
	public void setControlName(String controlName)	{	this.controlName = controlName;	}
	public String getControlName() {	return this.controlName;	}
	
	public void setData(String[] data) {	this.data = data;	}
	public String[] getData() {	return this.data;	}
	
	public static String getAbsPath()	{	return absPath;	}
}
