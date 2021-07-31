package com.mkweb.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkweb.logger.MkLogger;


public class MkJsonData {
	private JSONObject jsonObject = null;
	private JSONArray jsonArray = null;
	private String data = null;

	private static final String TAG = "[MkJsonData]";
	private static final MkLogger mklogger = new MkLogger(TAG);

	public MkJsonData() {
		data = null;
		jsonObject = null;
	}

	public MkJsonData(String data) {
		this.data = data;
		jsonObject = null;
	}

	private static JSONObject isValidDataForJson(String data) {
		boolean isDone = false;
		try {
			JSONObject jo = new JSONObject();
			JSONParser parser = new JSONParser();
			
			Object obj = parser.parse(data);
			jo = (JSONObject) obj;
			isDone = true;
			return jo;
		} catch (ParseException e) {
			mklogger.error("(func isValidDataForJson) ParseException:: " + e + " Given data is not valid for JSONObject.\n" + data);
		} catch(NullPointerException e) {
			mklogger.error("(func isValidDataForJson) NullPoitnerException:: " + e + "\nGiven data is not valid for JSONObject.\n" + data);
		} catch(Exception e){
			mklogger.error("(func isValidDataForJson) error : " + e);
		} finally {
			if(!isDone) {
				return null;
			}
		}
		return null;
	}
	
	private static JSONArray isValidDataForJsonArray(String data) {
		boolean isDone = false;
		try {
			String temp = "[" + data + "]";
			JSONArray ja = new JSONArray();
			JSONParser parser = new JSONParser();
			
			Object obj = parser.parse(temp);
			ja = (JSONArray) obj;
			mklogger.debug("ja : " +ja);
			isDone = true;
			return ja;
		} catch (ParseException e) {
			mklogger.error("(func isValidDataForJsonArray) ParseException:: " + e + " Given data is not valid for JSONObject.\n" + data);
		} catch(NullPointerException e2) {
			mklogger.error("(func isValidDataForJsonArray) NullPoitnerException:: " + e2 + "\nGiven data is not valid for JSONObject.\n" + data);
		} catch(Exception e){
			mklogger.error("(func isValidDataForJsonArray) error : " + e);
		} finally {
			if(!isDone) {
				return null;
			}
		}
		return null;
	}

	public static String stringToJsonString(String stdataringData) {
		String[] requestParams = null;
		HashMap<String, ArrayList<String>> reqData = null;
		ArrayList<String> reqParams = null;
		String reqToJson = null;

		reqData = new HashMap<>();
		reqParams = new ArrayList<>();

		requestParams = stdataringData.split("&");
		for(int i = 0; i <requestParams.length; i++) {
			String[] splits = requestParams[i].split("=");
			String secondKey = null;
			boolean needSecondKey = false;
			if(splits.length == 2) {
				String key = splits[0];
				if(splits[0].contains("[")) {
					if(!splits[0].contains("]")) {
						mklogger.error(" Given data Parentheses not matching!");
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
				mklogger.error(" Given data is not valid." + requestParams[i]);
				return null;
			}
		}
		
		if(reqParams != null) {
			HashSet<String> tt = new HashSet<String>(reqParams);
			reqParams = new ArrayList<String>(tt);
			tt = null;
			
			reqToJson = "{";
			
			for(int i = 0; i < reqParams.size(); i++) {
				ArrayList<String> list = reqData.get(reqParams.get(i));
				
				if(list.size() == 1) {
					reqToJson += "\"" + reqParams.get(i) + "\":\"" + list.get(0) + "\"";
				}
				else if(list.size() > 1) {
					reqToJson += "\"" + reqParams.get(i) + "\":{";
					for(int j = 0; j < list.size(); j++) {
					
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
	
	public static void printObject(JSONObject jsonObject) {
		if(jsonObject == null) {
			mklogger.error("(func printObject) JSONObject is null");
			return;
		}
	    Set entrySet = jsonObject.keySet();
	    Iterator iter = entrySet.iterator();
	    
		String result = "";
		while(iter.hasNext()) {
			String key = (String) iter.next();
			result += "{'"+key+"':'"+jsonObject.get(key) + "'}";
		}
		mklogger.info("[JSONObject]: "+result);
	}
	
	public boolean setJsonObject() {
		if(this.data == null) {
			mklogger.error("(func setJsonObject) No given data.");
			return false;
		}

		if((jsonObject = isValidDataForJson(this.data)) == null) {
			return false;
		}

		return true;
	}

	public boolean setJsonObject(String data) {
		this.data = data;

		if((jsonObject = isValidDataForJson(this.data)) == null) {
			return false;
		}

		return true;
	}
	
	public boolean setJsonArray() {
		if(this.data == null) {
			mklogger.error("(func setJsonArray) No given data.");
			return false;
		}

		if((jsonArray = isValidDataForJsonArray(this.data)) == null) {
			return false;
		}
		
		return true;
	}
	
	public static JSONObject objectMapToJson(Map<String, Object> map) {
		try {
			return new JSONObject(map);
		} catch (Exception e) {
			mklogger.error("func mapToJson(): Given data is not valid." + map);
			return null;
		}
	}

	public static String removeQuoteFromJsonObject(JSONObject jsonObject){
		return jsonObject.toString().replaceAll("\\\\\"", "");
	}

	public static String removeQuoteFromJsonArray(JSONArray jsonArray){
		return jsonArray.toString().replaceAll("\\\\\"", "");
	}

	public static JSONObject mapToJson(Map<String, String> map) {
		try {
			return new JSONObject(map);
		} catch (Exception e) {
			mklogger.error("func mapToJson(): Given data is not valid." + map);
			return null;
		}
	}
	
	public static String jsonToPretty(JSONObject jsonObject) throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
	}
	
	public static String jsonToPretty(JSONArray jsonObject) throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
	}

	public static JSONObject createJsonObject(String data){
		JSONObject jsonObject = isValidDataForJson(data);
		return jsonObject;
	}
	
	public JSONObject getJsonObject() {	return this.jsonObject;	}
	public JSONArray getJsonArray() {	return this.jsonArray;	}
	public String getData() {	return this.data;	}
	public void setData(String data) {	this.data = data;	}
}