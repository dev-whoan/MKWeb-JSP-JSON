package com.mkweb.restapi;

import java.util.HashMap;

import com.mkweb.data.MkJsonData;

public class MkRestApiData extends MkJsonData{
	//«Ï¥ı º≥¡§ ππππ¿÷¡ˆ?
	private int responseCode = -1;
	private String message = null;
	private String contentType = null;
	private String character = "UTF-8";
	private HashMap<Integer, String> status = null;
	private boolean beauty = false;
	MkRestApiData(){
		super();
		setStatus();
	}
	
	MkRestApiData(String str){
		super(str);
		setStatus();
	}
	
	private void setStatus() {
		status = new HashMap<Integer, String>();
		status.put(100, "CONTINUE");
		status.put(101, "SWITCHING PROTOCOL");
		status.put(102, "PROCESSING(WebDAV)");
		status.put(103, "EARLY HINTS");
		
		status.put(200, "OK");
		status.put(201, "CREATED");
		status.put(202, "ACCEPTED");
		status.put(203, "NON-AUTHORITATIVE INFORMATION");
		status.put(204, "NO-CONTENT");

		status.put(300, "MULTIPLE CHOICES");
		status.put(301, "MOVED PERMANENTLY");
		status.put(302, "FOUND");
		status.put(303, "SEE OTHER");
		status.put(304, "NOT MODIFIED");
		status.put(307, "TEMPORARY REDIRECT");
		
		status.put(400, "BAD REQUEST");
		status.put(401, "UNAUTHORIZED");
		status.put(403, "FORBIDDEN");
		status.put(404, "NOT FOUND");
		status.put(405, "METHOD NOT ALLOWED");
		status.put(406, "NOT ACCEPTABLE");
		status.put(412, "PRECONDITION FAILED");
		status.put(415, "UNSUPPORTED MEDIA TYPE");
		
		status.put(500, "INTERNAL SERVER ERROR");
		status.put(501, "NOT IMPLEMENTED");
	}
	
	public void setResponseCode(int responseCode) {	this.responseCode = responseCode;	}
	public void setMessage(String message) {	this.message = message;	}
	public void setContentType(String contentType) {	this.contentType = contentType;	}
	public void setCharacterEncoding(String character) {	this.character = character;	}
	public void setBeauty(boolean beauty) {	this.beauty = beauty;	}
	
	public int getResponseCode() {	return this.responseCode;	}
	public String getMessage() {	return this.message;	}
	public String getContentType() {	return this.contentType;	}
	public String getCharacterEncoding() {	return this.character;	}
	public boolean getBeauty() {	return this.beauty;	}
}
