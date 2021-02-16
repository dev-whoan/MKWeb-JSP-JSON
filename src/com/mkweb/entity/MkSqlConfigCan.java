package com.mkweb.entity;

import java.io.File;
import java.util.HashMap;

import com.mkweb.data.MkSqlJsonData;

public abstract class MkSqlConfigCan extends MkSqlJsonData{
	public abstract Object getControl(String controlName);
	public abstract Object getControlByServiceName(String serviceName);
	public abstract void setSqlConfigs(File[] sqlConfigs);
	
	protected String[] createSQL(String[] befQuery, HashMap<String, Object> tableData, boolean isApi) {
		String[] result = new String[1];
		
		String rawFrom = tableData.get("from").toString();
		String dataFrom = rawFrom;
		
		boolean doJoin = (tableData.get("join") != null);
		String joinType = "";
		String joinFrom = "";
		String joinOn = "";
		
		if(doJoin) {
			joinType = tableData.get("type").toString();
			joinFrom = tableData.get("joinfrom").toString();
			joinOn = tableData.get("on").toString();
			
			dataFrom = rawFrom + " " + joinType + " " + joinFrom + " ON " + joinOn;
		}
		switch(befQuery[0].toLowerCase()) {
		case "select":
			if(!isApi) {
				if(befQuery[3].length() > 0)
					result[0] = "SELECT " + befQuery[1] + " FROM " + dataFrom+ " WHERE " + befQuery[3] + ";";
				else
					result[0] = "SELECT " + befQuery[1] + " FROM " + dataFrom + ";";
			}else {
				result[0] = "SELECT " + befQuery[1] + " FROM " + dataFrom + " WHERE " + befQuery[3] + ";";
			}
			
			break;
		case "insert":
			result[0] = "INSERT INTO " + dataFrom + "(" + befQuery[1] + ") VALUE(" + befQuery[2] + ");";
			break;
		case "update":
			
			String[] tempColumns = befQuery[1].split(",");
			String[] tempDatas = befQuery[2].split(",");
			String tempField = "";
			if(tempColumns.length != tempDatas.length) {
			//	mklogger.error(TAG, " UPDATE Query is not valid. Columns count and data count is not same");
				return null;
			}
			
			for(int i = 0; i < tempColumns.length; i++) {
				tempField += tempColumns[i] + "=" + tempDatas[i];
				
				if(i < tempColumns.length -1)
					tempField += ", ";
			}
			result[0] = "UPDATE " + dataFrom + " SET " + tempField + " WHERE " + befQuery[3] + ";";
			break;
		
		case "delete":
			result[0] = "DELETE FROM " + dataFrom + " WHERE " + befQuery[3] + ";";
			break;
		}
		return result;
	}
	
	/*
	protected String[] createSQL(String[] befQuery, boolean isApi) {
		String[] result = new String[1];

		switch(befQuery[0].toLowerCase()) {
		case "select":
			if(!isApi) {
				if(befQuery[4].length() > 0)
					result[0] = "SELECT " + befQuery[1] + " FROM " + befQuery[2] + " WHERE " + befQuery[4] + ";";
				else
					result[0] = "SELECT " + befQuery[1] + " FROM " + befQuery[2] + ";";
			}else {
				result[0] = "SELECT " + befQuery[1] + " FROM " + befQuery[2] + " WHERE " + befQuery[4] + ";";
			}
			
			break;
		case "insert":
			result[0] = "INSERT INTO " + befQuery[2] + "(" + befQuery[1] + ") VALUE(" + befQuery[3] + ");";
			break;
		case "update":
			
			String[] tempColumns = befQuery[1].split(",");
			String[] tempDatas = befQuery[3].split(",");
			String tempField = "";
			if(tempColumns.length != tempDatas.length) {
			//	mklogger.error(TAG, " UPDATE Query is not valid. Columns count and data count is not same");
				return null;
			}
			
			for(int i = 0; i < tempColumns.length; i++) {
				tempField += tempColumns[i] + "=" + tempDatas[i];
				
				if(i < tempColumns.length -1)
					tempField += ", ";
			}
			result[0] = "UPDATE " + befQuery[2] + " SET " + tempField + " WHERE " + befQuery[4] + ";";
			break;
		
		case "delete":
			result[0] = "DELETE FROM " + befQuery[2] + " WHERE " + befQuery[4] + ";";
			break;
		}
		return result;
	}
	*/
}
