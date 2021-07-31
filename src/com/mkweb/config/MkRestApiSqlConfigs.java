package com.mkweb.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.mkweb.utils.MkJsonData;
import com.mkweb.data.MkSqlJsonData;
import com.mkweb.entity.MkSqlConfigCan;
import com.mkweb.logger.MkLogger;

public class MkRestApiSqlConfigs extends MkSqlConfigCan {
	private HashMap<String, ArrayList<MkSqlJsonData>> sql_configs = new HashMap<String, ArrayList<MkSqlJsonData>>();
	private File[] defaultFiles = null;
	private static MkRestApiSqlConfigs mrasc = null;
	private long[] lastModified; 
	private static final String TAG = "[MkRestSQLConfigs]";
	private static final MkLogger mklogger = new MkLogger(TAG);

	public static MkRestApiSqlConfigs Me() {
		if(mrasc == null)
			mrasc = new MkRestApiSqlConfigs();
		return mrasc;
	}

	public void setSqlConfigs(File[] sqlConfigs) {
		sql_configs.clear();
		defaultFiles = sqlConfigs;
		ArrayList<MkSqlJsonData> sqlJsonData = null;
		lastModified = new long[sqlConfigs.length];
		int lmi = 0;
		for(File defaultFile : defaultFiles)
		{
			if(defaultFile.isDirectory())
				continue;
			
			lastModified[lmi++] = defaultFile.lastModified();
			mklogger.info("=*=*=*=*=*=*=* MkWeb API Sql  Configs Start*=*=*=*=*=*=*=*=");
			mklogger.info("File: " + defaultFile.getAbsolutePath());
			mklogger.info("=            " + defaultFile.getName() +"              =");
			if(defaultFile == null || !defaultFile.exists())
			{
				mklogger.error("Config file is not exists or null");
				return;
			}

			try(FileReader reader = new FileReader(defaultFile)){
				sqlJsonData = new ArrayList<MkSqlJsonData>();
				JSONParser jsonParser = new JSONParser();
				JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
				JSONObject sqlObject = (JSONObject) jsonObject.get("Controller");
				
				String sqlName = sqlObject.get("name").toString();
				
				String sqlDebugLevel = sqlObject.get("debug").toString();
				
				String sqlDB = sqlObject.get("db").toString();
				String sqlAPI = sqlObject.get("api").toString();
				String sqlTable = sqlObject.get("table").toString();
				
				Object sqlParamObject = sqlObject.get("parameter");
				String[] sqlParameters = null;
				if(sqlParamObject != null) {
					JSONObject sqlParameterObject = (JSONObject) sqlParamObject;
					sqlParameters = new String[sqlParameterObject.size()];
					for(int j = 0; j < sqlParameterObject.size(); j++) {
						sqlParameters[j] = sqlParameterObject.get("" + (j+1)).toString();
					}
					
					if(sqlParameters.length == 1 && sqlParameters[0].contentEquals("*")) {
						sqlParameters = null;
					}
				}
				
				
				MkJsonData mkJsonData = new MkJsonData(sqlObject.get("condition").toString());
				JSONObject tempValues = null;
				String[] serviceConditions = null;
				
				if(mkJsonData.setJsonObject()) {
					tempValues = mkJsonData.getJsonObject();
				}
				if(tempValues.size() == 0) {
					mklogger.error("[Controller: " + controlName + " | Service ID: " + serviceName + "] Service doesn't have any value. Service must have at least one value. If the service does not include any value, please create blank one.");
					mklogger.debug("{\"1\":\"\"}");
					continue;
				}
				
				serviceConditions = new String[tempValues.size()];
				
				for(int j = 0; j < tempValues.size(); j++) {
					serviceConditions[j] = tempValues.get("" + (j+1)).toString();
				}
				
				JSONArray serviceArray = (JSONArray) sqlObject.get("services");

				for(int i = 0; i < serviceArray.size(); i++) {
					JSONObject serviceObject = (JSONObject) serviceArray.get(i);
					String serviceId = null;
					String[] serviceQuery = new String[1];
					HashMap<String, Object> tableData = new HashMap<>();;

					try {
						serviceId = serviceObject.get("id").toString();
						String serviceColumns = null;
						String serviceDatas = null;
						MkJsonData mjd = new MkJsonData(serviceObject.get("query").toString());
						if(!mjd.setJsonObject()) {
							mklogger.debug("Failed to set MkJsonObject service name : " + serviceId);
							return;
						}
						
						JSONObject serviceQueryData = mjd.getJsonObject();
						serviceQuery = new String[serviceQueryData.size()+1];
						
						if(serviceQuery.length != 5) {
							mklogger.error("[Controller: " + controlName + " | service: "+serviceId+"] The format of query is not valid. Please check your page configs.");
							return;
						}
						
						serviceQuery[0] = serviceQueryData.get("crud").toString();
						tableData.put("from", sqlTable);
				//		serviceQuery[2] = sqlTable;
						serviceQuery[3] = serviceQueryData.get("where").toString();
						
						MkJsonData serviceColumn = new MkJsonData(serviceQueryData.get("column").toString());
						if(!serviceColumn.setJsonObject()) {
							mklogger.debug("Failed to set MkJsonObject service name : " + serviceId +"(column)");
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
							mklogger.debug("Failed to set MkJsonObject service name : " + serviceId +"(data)");
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
						serviceQuery[2] = serviceDatas;
					}catch(NullPointerException npe) {
						mklogger.error("[Controller: " + sqlName + "] Some service of the SQL doesn't have attributes. Please check the SQL config.");
						return;
					}

					MkSqlJsonData sqlData = new MkSqlJsonData();
					String[] finalQuery = createSQL(serviceQuery, tableData, true);
					
					sqlData.setRawSql(serviceQuery);
					sqlData.setControlName(sqlName);
					sqlData.setParameters(sqlParameters);
					//ID = 0, DB = 1
					sqlData.setServiceName(serviceId);
					sqlData.setTableData(tableData);
					sqlData.setDB(sqlDB);
					sqlData.setData(finalQuery);
					sqlData.setDebugLevel(sqlDebugLevel);
					sqlData.setApiSQL(sqlAPI.toLowerCase().contentEquals("yes"));
					sqlData.setCondition(serviceConditions);
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
			mklogger.info("=*=*=*=*=*=*=* MkWeb API Sql  Configs  Done*=*=*=*=*=*=*=*=");
		}
	}

	public void printSqlInfo(MkSqlJsonData jsonData, String type) {
		String conditions = "";
		int conditionLength = (jsonData.getCondition() != null ? jsonData.getCondition().length : -1);
		
		for(int i = 0; i < conditionLength; i++) {
			conditions += jsonData.getCondition()[i];
			
			if(i < conditionLength -1)
				conditions += ", ";
		}
		String tempMsg = "\n===========================SQL Control  :  " + jsonData.getControlName() + "============================="
				+ "\n|SQL ID:\t" + jsonData.getServiceName() + "\t\t API:\t" + jsonData.IsApiSql()
				+ "\n|SQL DB:\t" + jsonData.getDB()
				+ "\n|SQL Debug:\t" + jsonData.getDebugLevel()
				+ "\n|sql Query:\t" + jsonData.getData()[0].trim()
				+ "\n|conditions:\t" + conditions
				+ "\n============================================================================";
		
		mklogger.temp(tempMsg, false);
		mklogger.flush(type);
	}
	
	public ArrayList<MkSqlJsonData> getControl(String controlName) {
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
	
	public ArrayList<MkSqlJsonData> getControlByServiceName(String serviceName){
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
		mklogger.debug("my iter : " + iter);
		Iterator sqlIterator = iter.iterator();
		String resultControlName = null;
		ArrayList<MkSqlJsonData> jsonData = null;
		while(sqlIterator.hasNext()) {
			String controlName = sqlIterator.next().toString();
			jsonData = getControl(controlName);
			for(MkSqlJsonData curData : jsonData) {
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
