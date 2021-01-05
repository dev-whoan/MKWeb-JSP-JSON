package com.mkweb.restapi;

import java.util.LinkedHashMap;

import org.json.simple.JSONObject;

import com.mkweb.config.MkConfigReader;
import com.mkweb.logger.MkLogger;

public class MkRestApiResponse {
	private long responseLife = -1L;
	private String responseResult = null;
	private int responseCount = 0;
	private int responseCode = -1;
	private String responseMessage = null;
	private String responseStatus = null;
	private String documentURL = null;
	private String contentType = null;
	private long contentLength = -1L; 
	private String TAG = "[MkRestApiResponse]";
	private MkLogger mklogger = MkLogger.Me();
	
	MkRestApiResponse(){	documentURL = MkConfigReader.Me().get("mkweb.web.hostname") + "/" + MkConfigReader.Me().get("mkweb.restapi.docs") + "/";	}
	MkRestApiResponse(String jsonString, int code, int count){
		mklogger.debug(TAG, " Called");
		responseResult = jsonString;
		setLife();
		responseCount = count;
		responseCode = code;
		documentURL = MkConfigReader.Me().get("mkweb.web.hostname") + "/" + MkConfigReader.Me().get("mkweb.restapi.docs") + "/";
	}
	
	public String getData() {
		if(!needUpdate())
			return this.responseResult;
		else {
			return null;
		}
	}
	
	public long getLife() {	return this.responseLife;	}
	public int getCount() {	return this.responseCount;	}
	public int getCode() {	return this.responseCode;	}
	public String getMessage() {	return this.responseMessage;	}
	public String getDocs()	{	setDocs(responseCode);	return this.documentURL;	}
	public String getStatus() {	setStatus();	return this.responseStatus;	}
	public String getContentType() {	return this.contentType;	}
	public long getContentLength() {	return this.contentLength;	}
	
	public void setCode(int responseCode) {	this.responseCode = responseCode;	}
	public void setData(JSONObject jsonObject) {	this.responseResult = jsonObject.toString();	}
	private void setLife() {	this.responseLife = System.currentTimeMillis() + Integer.parseInt(MkConfigReader.Me().get("mkweb.restapi.lifecycle")) * 60 * 1000;	}
	public void setCount(int count) {	this.responseCount += count;	}
	public void setMessage(String msg) {	this.responseMessage = msg;	}
	private void setDocs(int errorcode) {	this.documentURL += ("" + errorcode);	}
	public void setContentType(String contentType) {	this.contentType = contentType;	}
	public void setContentLength(long contentLength) {	this.contentLength = contentLength;	}
	
	public String generateResult(boolean success) {
		String result = null;
		String temp = null;
		if(success) {
			result = "";
		}else{
			temp = "\"error\":{" +
						"\"message\":\"" + getMessage() + "\"," +
						"\"code\":\"" + getCode() + "\"," +
						"\"status\":\"" + getStatus() + "\"," +
						"\"info\":\"" + getDocs() + "\"}";
			contentLength = temp.length();
			result = "{" +
					"\"response\":\"HTTP 1.1 " + getCode() + " " + getStatus() + "\"," +
					"\"Content-Type\":\""+ getContentType()+"\"," +
					"\"Content-Length\":\"" + getContentLength() + "\"," +
					temp;
			result += "}";
		}
		return result;
	}
	
	private void setStatus() {
		switch(responseCode) {
		case -1:
			responseStatus = "No Request";
			break;
		case 200:
			responseStatus = "OK";
			break;
		case 201:
			responseStatus = "Created";
			break;
		case 204:
			responseStatus = "No Content(No data)";
			break;
		case 400:
			responseStatus = "Bad Request";
			break;
		case 401:
			responseStatus = "Unauthorized";
			break;
		case 403:
			responseStatus = "Forbidden";
			break;
		case 404:
			responseStatus = "Not Found";
			break;
		case 405:
			responseStatus = "Method Not Allowed";
			break;
		case 409:
			responseStatus = "Conflict";
			break;
		case 429:
			responseStatus = "Too Many Requests";
			break;
		case 500:
			responseStatus = "Server Error";
			break;
		}
	}

	public boolean needUpdate() {	return (this.responseLife >= System.currentTimeMillis());	}
}