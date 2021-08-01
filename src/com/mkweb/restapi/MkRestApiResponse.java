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
	private int contentLength = -1; 
	private static final String TAG = "[MkRestApiResponse]";
	private static final MkLogger mklogger = new MkLogger(TAG);
	private long lifeTime = -1L;

	MkRestApiResponse(){	documentURL = MkConfigReader.Me().get("mkweb.web.hostname") + "/" + MkConfigReader.Me().get("mkweb.restapi.docs") + "/";
	//	lifeTime = Integer.parseInt(MkConfigReader.Me().get("mkweb.restapi.lifecycle")) * 60 * 1000;
	}
	MkRestApiResponse(String jsonString, int code, int count){
		mklogger.debug(" Called");
		responseResult = jsonString;
		setLife();
		responseCount = count;
		responseCode = code;
		documentURL = MkConfigReader.Me().get("mkweb.web.hostname") + "/" + MkConfigReader.Me().get("mkweb.restapi.docs") + "/";
	//	lifeTime = Integer.parseInt(MkConfigReader.Me().get("mkweb.restapi.lifecycle")) * 60 * 1000;
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
	public int  getContentLength() {	return this.contentLength;	}
	public long getLifeTime() {	return this.lifeTime;	}

	public void setCode(int responseCode) {	this.responseCode = responseCode;	}
	public void setData(JSONObject jsonObject) {	this.responseResult = jsonObject.toString();	}
	private void setLife() {	this.responseLife = System.currentTimeMillis() + lifeTime;	}
	public void setCount(int count) {	this.responseCount += count;	}
	public void setMessage(String msg) {	this.responseMessage = msg;	}
	private void setDocs(int errorcode) {	this.documentURL += ("" + errorcode);	}
	public void setContentType(String contentType) {	this.contentType = contentType;	}
	public void setContentLength(int contentLength) {	this.contentLength = contentLength;	}

	public String generateResult(boolean success, String method, String prefix) {
		String result = null;
		String temp = null;
		if(!success) {
			switch(method) {
			case "options":
			{
				if(!prefix.contentEquals("")) {
					result = "{\n" +
							"  \"code\":\"" + getCode() + "\",\n" +
							"  \"response\":\"HTTP/1.1 " + getCode() + " " + getStatus() + "\",\n" +
							"  \"Content-Type\":" + "\"" + getContentType() + "\",\n" +
							prefix +"\n" +
							"}";
					break;
				}else {
					result = "{\n" +
							"  \"code\":\"" + getCode() + "\",\n" +
							"  \"response\":\"HTTP/1.1 " + getCode() + " " + getStatus() + "\",\n" +
							"  \"Content-Type\":" + "\"" + getContentType() + "\"\n" +
							"}";
				}
			}

			default:
				temp = "  \"error\":{\n" +
						"    \"message\":\"" + getMessage() + "\",\n" +
						"    \"code\":\"" + getCode() + "\",\n" +
						"    \"status\":\"" + getStatus() + "\",\n" +
						"    \"info\":\"" + getDocs() + "\"\n  }";
				contentLength = temp.length();
				result = "{\n" +
						"  \"code\":\"" + getCode() + "\",\n" +
						"  \"response\":\"HTTP/1.1 " + getCode() + " " + getStatus() + "\",\n" +
						"  \"Content-Type\":" + "\"" + getContentType() + "\",\n" +
						"  \"Content-Length\":" + "\"" + getContentLength() + "\",\n" +
						temp + "\n" +
						"}";
				break;
			}

		}else {
			switch(method) {
			case "put":
				result = "{\n" +
						"  \"code\":\"" + getCode() + "\",\n" +
						"  \"response\":\"HTTP 1.1 " + getCode() + " " + getStatus() + "\",\n" +
						"  \"Content-Type\":" + "\"" + getContentType() + "\",\n" +
						"  \"Content-Length\":" + "\"" + getContentLength() + "\"\n" +
						"}";
				break;
			case "delete":
				result = "{\n" +
						"  \"code\":\"" + getCode() + "\",\n" +
						"  \"response\":\"HTTP 1.1 " + getCode() + " " + getStatus() + "\",\n" +
						"  \"Content-Type\":" + "\"" + getContentType() + "\",\n" +
						"  \"Content-Length\":" + "\"" + getContentLength() + "\"\n" +
						"}";
				break;
			default:
				result = "{\n" +
						"  \"code\":\"" + getCode() + "\",\n" +
						"  \"response\":\"HTTP 1.1 " + getCode() + " " + getStatus() + "\",\n" +
						"  \"Content-Type\":" + "\"" + getContentType() + "\",\n" +
						"  \"Content-Length\":" + "\"" + getContentLength() + "\"," +
						prefix +
						"}";
				break;
			}
		}
		return result;
	}

	public String generateResult(int code, String method, String prefix, boolean pretty, long startMillis) {
		String result = null;
		String temp = null;
		long took = System.currentTimeMillis() - startMillis;
		if(pretty) {
			if(code >= 400) {
				temp = "  \"error\":{\n" +
						"  \"message\":\"" + getMessage() + "\",\n" +
						"  \"code\":\"" + getCode() + "\",\n" +
						"  \"status\":\"" + getStatus() + "\",\n" +
						"  \"info\":\"" + getDocs() + "\"\n  }";
				contentLength = temp.length();
				result = "{\n" +
						"  \"took\":\"" + took + "\",\n" +
						"  \"code\":\"" + getCode() + "\",\n" +
						"  \"response\":\"HTTP/1.1 " + getCode() + " " + getStatus() + "\",\n" +
						"  \"Content-Type\":" + "\"" + getContentType() + "\",\n" +
						"  \"Content-Length\":" + "\"" + getContentLength() + "\",\n" +
						temp + "\n" +
						"}";
			}else {
				switch(method) {
				case "put":
					result = "{\n" +
							"  \"took\":\"" + took + "\",\n" +
							"  \"code\":\"" + getCode() + "\",\n" +
							"  \"response\":\"HTTP 1.1 " + getCode() + " " + getStatus() + "\",\n" +
							"  \"Content-Type\":" + "\"" + getContentType() + "\",\n" +
							"  \"Content-Length\":" + "\"" + getContentLength() + "\"\n" +
							"}";
					break;
				case "delete":
					result = "{\n" +
							"  \"took\":\"" + took + "\",\n" +
							"  \"code\":\"" + getCode() + "\",\n" +
							"  \"response\":\"HTTP 1.1 " + getCode() + " " + getStatus() + "\",\n" +
							"  \"Content-Type\":" + "\"" + getContentType() + "\",\n" +
							"  \"Content-Length\":" + "\"" + getContentLength() + "\"\n" +
							"}";
					break;
				case "options":
				{
					if(!prefix.contentEquals("")) {
						result = "{\n" +
								"  \"took\":\"" + took + "\",\n" +
								"  \"code\":\"" + getCode() + "\",\n" +
								"  \"response\":\"HTTP/1.1 " + getCode() + " " + getStatus() + "\",\n" +
								"  \"Content-Type\":" + "\"" + getContentType() + "\",\n" +
								prefix +"\n" +
								"}";
						break;
					}else {
						result = "{\n" +
								"  \"took\":\"" + took + "\",\n" +
								"  \"code\":\"" + getCode() + "\",\n" +
								"  \"response\":\"HTTP/1.1 " + getCode() + " " + getStatus() + "\",\n" +
								"  \"Content-Type\":" + "\"" + getContentType() + "\"\n" +
								"}";
					}
				}
				default:
					result = "{\n" +
							"  \"took\":\"" + took + "\",\n" +
							"  \"code\":\"" + getCode() + "\",\n" +
							"  \"response\":\"HTTP 1.1 " + getCode() + " " + getStatus() + "\",\n" +
							"  \"Content-Type\":" + "\"" + getContentType() + "\",\n" +
							"  \"Content-Length\":" + "\"" + getContentLength() + "\"," +
							prefix +
							"}";
					break;
				}
			}
		}else {
			if(code >= 400) {
				temp = "\"error\":{" +
						"\"message\":\"" + getMessage() + "\"," +
						"\"code\":\"" + getCode() + "\"," +
						"\"status\":\"" + getStatus() + "\"," +
						"\"info\":\"" + getDocs() + "\"}";
				contentLength = temp.length();
				result = "{" +
						"\"took\":\"" + took + "\"," +
						"\"code\":\"" + getCode() + "\"," +
						"\"response\":\"HTTP/1.1 " + getCode() + " " + getStatus() + "\"," +
						"\"Content-Type\":" + "\"" + getContentType() + "\"," +
						"\"Content-Length\":" + "\"" + getContentLength() + "\"," +
						temp + 
						"}";
			}else {
				switch(method) {
				case "put":
					result = "{" +
							"\"took\":\"" + took + "\"," +
							"\"code\":\"" + getCode() + "\"," +
							"\"response\":\"HTTP 1.1 " + getCode() + " " + getStatus() + "\"," +
							"\"Content-Type\":" + "\"" + getContentType() + "\"," +
							"\"Content-Length\":" + "\"" + getContentLength() + "\"" +
							"}";
					break;
				case "delete":
					result = "{" +
							"\"took\":\"" + took + "\"," +
							"\"code\":\"" + getCode() + "\"," +
							"\"response\":\"HTTP 1.1 " + getCode() + " " + getStatus() + "\"," +
							"\"Content-Type\":" + "\"" + getContentType() + "\"," +
							"\"Content-Length\":" + "\"" + getContentLength() + "\"" +
							"}";
					break;
				case "options":
				{
					if(!prefix.contentEquals("")) {
						result = "{" +
								"\"took\":\"" + took + "\"," +
								"\"code\":\"" + getCode() + "\"," +
								"\"response\":\"HTTP/1.1 " + getCode() + " " + getStatus() + "\"," +
								"\"Content-Type\":" + "\"" + getContentType() + "\"," +
								prefix +
								"}";
						break;
					}else {
						result = "{" +
								"\"took\":\"" + took + "\"," +
								"\"code\":\"" + getCode() + "\"," +
								"\"response\":\"HTTP/1.1 " + getCode() + " " + getStatus() + "\"," +
								"\"Content-Type\":" + "\"" + getContentType() + "\"" +
								"}";
					}
				}
				default:
					result = "{" +
							"\"took\":\"" + took + "\"," +
							"\"code\":\"" + getCode() + "\"," +
							"\"response\":\"HTTP 1.1 " + getCode() + " " + getStatus() + "\"," +
							"\"Content-Type\":" + "\"" + getContentType() + "\"," +
							"\"Content-Length\":" + "\"" + getContentLength() + "\"," +
							prefix +
							"}";
					break;
				}
			}
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
		case 406:
			responseStatus = "Content Type Not Supported";
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