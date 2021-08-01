package com.mkweb.data;

public class MkFtpData extends AbsJsonData{
	private String debugLevel = null;
	private String path = null;
	private int maxCounts = -1;

	public String getPath() {	return this.path;	}
	public String getDebugLevel() {	return this.debugLevel;	}
	public int getMaxCount(){	return this.maxCounts; }


	public MkFtpData setPath(String path) {		this.path = path;		return this;	}
	public MkFtpData setDebugLevel(String dl) {	this.debugLevel = dl;	return this;	}
	public MkFtpData setMaxCount(int maxCounts){	this.maxCounts = maxCounts; return this;	}
}
