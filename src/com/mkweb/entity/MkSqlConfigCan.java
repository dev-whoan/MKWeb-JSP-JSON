package com.mkweb.entity;

import java.io.File;

import com.mkweb.data.MkSqlJsonData;

public abstract class MkSqlConfigCan extends MkSqlJsonData{
	public abstract Object getControl(String controlName);
	public abstract Object getControlByServiceName(String serviceName);
	public abstract void setSqlConfigs(File[] sqlConfigs);
	
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
}
