package com.mkweb.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.mkweb.logger.MkLogger;


public class MkJsonData {
	private JSONObject jsonObject = null;
	private String data = null;

	private String TAG = "[MkJsonData]";
	private MkLogger mklogger = MkLogger.Me();

	public MkJsonData() {
		data = null;
		jsonObject = null;
	}

	public MkJsonData(String data) {
		this.data = data;
		jsonObject = null;
	}

	private JSONObject isValidDataForJson(String data) {
		boolean isDone = false;
		try {
	//		mklogger.debug(TAG, "check data : " + data);
			JSONObject jo = new JSONObject();
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(data);
			jo = (JSONObject) obj;
			isDone = true;
			return jo;
		} catch (ParseException e) {
			mklogger.error(TAG, "(func isValidDataForJson) ParseException:: " + e + " Given data is not valid for JSONObject.\n" + data);
		} catch(NullPointerException e2) {
			mklogger.error(TAG, "(func isValidDataForJson) NullPoitnerException:: " + e2 + "\nGiven data is not valid for JSONObject.\n" + data);
		}finally {
			if(!isDone) {
				return null;
			}
		}
		return null;
	}

	public String stringToJsonString(String stdataringData) {
		String[] requestParams = null;
		HashMap<String, ArrayList<String>> reqData = null;
		ArrayList<String> reqParams = null;
		String reqToJson = null;

		reqData = new HashMap<>();
		reqParams = new ArrayList<>();
		//잘라야해
		requestParams = data.split("&");
		//'{"search_key":"apple", "person":{"name":"민환","age":24}}';
		// {"search_key":"apple", "person":{"name":"민환", "age":"24"}}
		//'{"search_key":"apple", "name":"민환"}';
		for(int i = 0; i <requestParams.length; i++) {
			String[] splits = requestParams[i].split("=");
			String secondKey = null;
			boolean needSecondKey = false;
			if(splits.length == 2) {
				String key = splits[0];
				if(splits[0].contains("[")) {
					if(!splits[0].contains("]")) {
						//예외
						mklogger.error(TAG, " Given data Parentheses not matching!");
						return null;
					}else {
						key = splits[0].split("\\[")[0];
						secondKey = splits[0].split("\\[")[1].split("\\]")[0];
						needSecondKey = true;
					}
				}
				ArrayList<String> list = null;
				if(reqData.get(key) != null) {
					list = reqData.get(key);
					if(needSecondKey) {
						list.add(secondKey + "&" + splits[1]);
					}else {
						list.add(splits[1]);
					}
				}else {
					list = new ArrayList<>();
					if(needSecondKey) {
						list.add(secondKey + "&" + splits[1]);
					}else {
						list.add(splits[1]);
					}
				}
				reqData.put(key, list);
				reqParams.add(key);
			}else {
				mklogger.error(TAG, " Given data is not valid." + requestParams[i]);
				return null;
			}
			//search_key=apple
			//name=민환
			//[]이 있는지 확인
			//person[name]=민환
			//person[age]=24
		}
		
		if(reqParams != null) {
			HashSet<String> tt = new HashSet<String>(reqParams);
			reqParams = new ArrayList<String>(tt);
			tt = null;
			
			reqToJson = "{";
			
			for(int i = 0; i < reqParams.size(); i++) {
				ArrayList<String> list = reqData.get(reqParams.get(i));
				
				//한개요소라면
				if(list.size() == 1) {
					reqToJson += "\"" + reqParams.get(i) + "\":\"" + list.get(0) + "\"";
				}
				//2개 이상이라면
				else if(list.size() > 1) {
					//person[name]=민환
					//person[age]=24
					//, "person":{"name":"민환","age":24}              }';
					reqToJson += "\"" + reqParams.get(i) + "\":{";
					for(int j = 0; j < list.size(); j++) {
					//	&로 잘라야함
						String secondKey = list.get(j).split("&")[0];
						String value = list.get(j).split("&")[1];
						reqToJson += "\"" + secondKey + "\":" + "\"" + value + "\"";
						
						if(j < list.size() - 1) {
							reqToJson += ", ";
						}
					}
					reqToJson += "}";
				}
				
				if(i < reqParams.size() - 1)
					reqToJson += ", ";
			}
			reqToJson += "}";
		}
		
		return reqToJson;
	}
	
	public void printObject(JSONObject jsonObject) {
		if(jsonObject == null) {
			mklogger.error(TAG, "(func printObject) JSONObject is null");
			return;
		}
	    Set entrySet = jsonObject.keySet();
	    Iterator iter = entrySet.iterator();
	    
		String result = "";
		while(iter.hasNext()) {
			String key = (String) iter.next();
			result += "{'"+key+"':'"+jsonObject.get(key) + "'}";
		}
		mklogger.info(TAG, "[JSONObject]: "+result);
	}
	
	public boolean setJsonObject() {
		if(this.data == null) {
			mklogger.error(TAG, "(func setJsonObject) No given data.");
			return false;
		}

		if((jsonObject = isValidDataForJson(this.data)) == null) {
			return false;
		}

		return true;
	}
	public JSONObject getJsonObject() {	return this.jsonObject;	}
	public String getData() {	return this.data;	}
	public void setData(String data) {	this.data = data;	}
}
