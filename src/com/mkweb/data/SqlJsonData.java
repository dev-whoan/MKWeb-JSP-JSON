package com.mkweb.data;

import java.util.ArrayList;


public class SqlJsonData extends AbsJsonData {
	   private String db = null;
	   private boolean allowSingle = false;
	   private boolean allowLike = false;
	   private String debugLevel = null;
	   private ArrayList<String> columnData = null;
	   
	   public String getDB() { return this.db; }
	   public boolean getAllowSingle() {	return this.allowSingle;	}
	   public boolean getAllowLike() {	return this.allowLike;	}
	   public String getDebugLevel() {	return this.debugLevel;	}
	   public ArrayList<String>	getColumnData(){	return this.columnData;	}
	   
	   public void setDB(String db) { this.db = db;	}
	   public void setAllowSingle(String as) {	this.allowSingle = (as.equals("yes") ? true : false);	}
	   public void setAllowLike(String al) {	this.allowLike = (al.equals("yes") ? true : false);	}
	   public void setDebugLevel(String dl) {	this.debugLevel = dl;	}
	   public void setColumnData(ArrayList<String> cd) {	this.columnData = cd;	}
}