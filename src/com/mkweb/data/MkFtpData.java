package com.mkweb.data;

public class MkFtpData extends AbsJsonData{
	   private String debugLevel = null;
	   private String path = null;
	   
	   public String getPath() {	return this.path;	}
	   public String getDebugLevel() {	return this.debugLevel;	}

	   public void setPath(String path) {		this.path = path;		}
	   public void setDebugLevel(String dl) {	this.debugLevel = dl;	}
}
