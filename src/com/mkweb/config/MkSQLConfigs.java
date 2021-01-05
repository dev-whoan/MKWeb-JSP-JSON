package com.mkweb.config;

import java.io.File;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.w3c.dom.Node;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import com.mkweb.can.MkSqlConfigCan;
import com.mkweb.data.MkJsonData;
import com.mkweb.data.PageJsonData;
import com.mkweb.data.SqlJsonData;
import com.mkweb.logger.MkLogger;

public class MkSQLConfigs extends MkSqlConfigCan {
	private HashMap<String, ArrayList<SqlJsonData>> sql_configs = new HashMap<String, ArrayList<SqlJsonData>>();
	private File[] defaultFiles = null;
	private static MkSQLConfigs sxc = null;
	private long[] lastModified = null;
	private MkLogger mklogger = MkLogger.Me();
	private String TAG = "[SQLXmlConfigs]";

	public static MkSQLConfigs Me() {
		if(sxc == null)
			sxc = new MkSQLConfigs();
		return sxc;
	}

	public void setSqlConfigs(File[] sqlConfigs) {
		sql_configs.clear();
		defaultFiles = sqlConfigs;
		ArrayList<SqlJsonData> sqlJsonData = null;
		lastModified = new long[sqlConfigs.length];
		int lmi = 0;
		for(File defaultFile : defaultFiles)
		{
			lastModified[lmi++] = defaultFile.lastModified();
			mklogger.info("=*=*=*=*=*=*=* MkWeb Sql  Configs Start*=*=*=*=*=*=*=*=");
			mklogger.info(TAG + "File: " + defaultFile.getAbsolutePath());
			mklogger.info("=            " + defaultFile.getName() +"              =");
			if(defaultFile == null || !defaultFile.exists())
			{
				mklogger.error("Config file is not exists or null");
				return;
			}

			try(FileReader reader = new FileReader(defaultFile)){
				sqlJsonData = new ArrayList<SqlJsonData>();
				JSONParser jsonParser = new JSONParser();
				JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
				JSONObject sqlObject = (JSONObject) jsonObject.get("Controller");
				
				String sqlName = sqlObject.get("name").toString();
				String sqlDebugLevel = sqlObject.get("debug").toString();
				String sqlDB = sqlObject.get("db").toString();
				String sqlAPI = sqlObject.get("api").toString();
				
				JSONArray serviceArray = (JSONArray) sqlObject.get("services");

				for(int i = 0; i < serviceArray.size(); i++) {
					JSONObject serviceObject = (JSONObject) serviceArray.get(i);
					String serviceId = null;
					String[] serviceQuery = null;
					try {
						serviceId = serviceObject.get("id").toString();

						String serviceColumns = null;
						String serviceDatas = null;
						MkJsonData mjd = new MkJsonData(serviceObject.get("query").toString());
						if(!mjd.setJsonObject()) {
							mklogger.debug(TAG, "Failed to set MkJsonObject service name : " + serviceId);
							return;
						}
						
						JSONObject serviceQueryData = mjd.getJsonObject();
						serviceQuery = new String[serviceQueryData.size()];
						
						if(serviceQuery.length != 5) {
							mklogger.error(TAG, "[Controller: " + controlName + " | service: "+serviceId+"] The format of query is not valid. Please check your page configs.");
							return;
						}
						
						serviceQuery[0] = serviceQueryData.get("crud").toString();
						serviceQuery[2] = serviceQueryData.get("table").toString();
						serviceQuery[4] = serviceQueryData.get("where").toString();
						
						MkJsonData serviceColumn = new MkJsonData(serviceQueryData.get("column").toString());
						if(!serviceColumn.setJsonObject()) {
							mklogger.debug(TAG, "Failed to set MkJsonObject service name : " + serviceId +"(column)");
							return;
						}
						JSONObject jsonColumns = serviceColumn.getJsonObject();
						serviceColumns = "";
						for(int k = 0; k < jsonColumns.size(); k++) {
							serviceColumns += jsonColumns.get("" + (k+1)).toString();
							
							if(k < jsonColumns.size()-1)
								serviceColumns += ",";
						}
						
						MkJsonData serviceData = new MkJsonData(serviceQueryData.get("data").toString());
						
						if(!serviceData.setJsonObject()) {
							mklogger.debug(TAG, "Failed to set MkJsonObject service name : " + serviceId +"(data)");
							return;
						}
						JSONObject jsonDatas = serviceData.getJsonObject();
						serviceDatas = "";
						for(int k = 0; k < jsonDatas.size(); k++) {
							serviceDatas += "@" + jsonDatas.get("" + (k+1)).toString() + "@";
							
							if(k < jsonDatas.size()-1)
								serviceDatas += ",";
						}
						
						serviceQuery[1] = serviceColumns;
						serviceQuery[3] = serviceDatas;
					}catch(NullPointerException npe) {
						mklogger.error("[Controller: " + controlName + "] Some service of the SQL doesn't have attributes. Please check the SQL config.");
						return;
					}

					SqlJsonData sqlData = new SqlJsonData();
					
					String[] finalQuery = createSQL(serviceQuery, false);
					
					sqlData.setControlName(sqlName);
					//ID = 0, DB = 1
					sqlData.setServiceName(serviceId);
					sqlData.setDB(sqlDB);
					sqlData.setData(finalQuery);
					sqlData.setDebugLevel(sqlDebugLevel);
					sqlData.setApiSQL((sqlAPI.toLowerCase().contentEquals("yes")));
					
					sqlJsonData.add(sqlData);
					printSqlInfo(sqlData, "info");
				}
				
				sql_configs.put(sqlName, sqlJsonData);
			} catch (FileNotFoundException e) {
				mklogger.error(e.getMessage());
				e.printStackTrace();
			} catch (IOException e) {
				mklogger.error(e.getMessage());
				e.printStackTrace();
			} catch (ParseException e) {
				mklogger.error(e.getMessage());
				e.printStackTrace();
			}
			mklogger.info("=*=*=*=*=*=*=* MkWeb Sql  Configs  Done*=*=*=*=*=*=*=*=");
		}
	}

	public void printSqlInfo(SqlJsonData jsonData, String type) {
		String tempMsg = "\n===========================SQL Control  :  " + jsonData.getControlName() + "============================="
				+ "\n|SQL ID:\t" + jsonData.getServiceName() + "\t\t API:\t" + jsonData.IsApiSql()
				+ "\n|SQL DB:\t" + jsonData.getDB()
				+ "\n|SQL Debug:\t" + jsonData.getDebugLevel()
				+ "\n|sql Query:\t" + jsonData.getData()[0].trim()
				+ "\n============================================================================";
		
		mklogger.temp(tempMsg, false);
		mklogger.flush(type);
	}
	
	public ArrayList<SqlJsonData> getControl(String controlName) {
		for(int i = 0; i < defaultFiles.length; i++)
		{
			if(lastModified[i] != defaultFiles[i].lastModified()){
				setSqlConfigs(defaultFiles);
				mklogger.info("==============Reload SQL Config files==============");
				mklogger.info("========Caused by : different modified time========");
				mklogger.info("==============Reload SQL Config files==============");
				break;
			}
		}

		return sql_configs.get(controlName);
	}
	
	public ArrayList<SqlJsonData> getControlByServiceName(String serviceName){
		for(int i = 0; i < defaultFiles.length; i++) {
			if(lastModified[i] != defaultFiles[i].lastModified()){
				setSqlConfigs(defaultFiles);
				mklogger.info("==============Reload SQL Config files==============");
				mklogger.info("========Caused by : different modified time========");
				mklogger.info("==============Reload SQL Config files==============");
				break;
			}
		}
		
		Set iter = sql_configs.keySet();
		Iterator sqlIterator = iter.iterator();
		String resultControlName = null;
		ArrayList<SqlJsonData> jsonData = null;
		while(sqlIterator.hasNext()) {
			String controlName = sqlIterator.next().toString();
			jsonData = getControl(controlName);
			
			for(SqlJsonData curData : jsonData) {
				if(serviceName.contentEquals(curData.getServiceName())) {
					resultControlName = controlName;
					break;
				}
			}
			
			if(resultControlName != null) {
				break;
			}
			jsonData = null;
		}
		
		return jsonData;
	}
}
