package com.mkweb.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import com.mkweb.logger.MkLogger;

public class PageJsonData extends AbsJsonData {
	private String logicalDir = null;
	private String pageURI = null;
	private String pageName = null;			//페이지 네임
	private String debug = null;
	/* Page Parameters */
	private String parameter = null;
	private LinkedHashMap<String, Boolean> pageValue = null;
	/* Page Parameters */
	/* Page Static Parameters */
	private boolean isPageStatic = false;
	/* Page Static Parameters */
	private String objectType = null;
	private String method = null;
	private boolean authorizedRequire = false;		//이거 클래스 필요한거임; 지우지마셈
	private boolean post = false;
	private boolean get = false;
	private boolean put = false;
	private boolean delete = false;
	private boolean options = false;
	private boolean head = false;
	private boolean isApi = false;
	
	public void setPageName(String pageName) {	this.pageName = pageName;	}
	public void setDebug(String debug) {	this.debug = debug;	}
	public void setParameter(String param) {	this.parameter = param;	}
	public void setObjectType(String objectType) {	this.objectType = objectType;	}
	public void setMethod(String method) {	this.method = method;	}
	public void setPageURI(String dir) {	this.pageURI = dir;	}
	public void setLogicalDir(String dir) {	this.logicalDir = dir;	}
	public void setAuthorizedRequire(String ar) {	this.authorizedRequire = (ar == null || ar.equals("no") ? false : ( ar.equals("yes") ? true : false) );	}
	public void setPost(String post) {	this.post = (post == null || post.equals("no") ? false : ( post.equals("yes") ? true : false) );	}
	public void setGet(String get) {	this.get = (get == null || get.equals("no") ? false : ( get.equals("yes") ? true : false) );	}
	public void setPut(String put) {	this.put = (put == null || put.equals("no") ? false : ( put.equals("yes") ? true : false) );	}
	public void setDelete(String delete) {	this.delete = (delete == null || delete.equals("no") ? false : ( delete.equals("yes") ? true : false) );	}
	public void setOptions(String options) {	this.options = (options == null || options.equals("no") ? false : ( options.equals("yes") ? true : false) );	}
	public void setHead(String head) {	this.head = (head == null || head.equals("no") ? false : ( head.equals("yes") ? true : false) );	}
//	public void setPageStaticParamName(String ppn) {	this.pageParamsName = ppn;	}
//	public void setPageStaticParams(String[] pageParams) {	this.pageParams = pageParams;	}
	public void setPageValue(LinkedHashMap<String, Boolean> pageValue) { this.pageValue = pageValue;	}
	public void setPageStatic(boolean isPageStatic) {	this.isPageStatic = isPageStatic;	}
	public void setAPI(boolean ia) {	this.isApi = ia;	}

	public String getPageName() {	return this.pageName;	}
	public String getDebug() {	return this.debug;	}
	public String getParameter() {	return this.parameter;	}
	public String getObjectType() {	return this.objectType;	}
	public String getMethod() {	return this.method;	}
	public String getPageURI() {	return this.pageURI;	}
	public String getLogicalDir() {	return this.logicalDir;	}
	public boolean getAuthorizedRequire() {	return this.authorizedRequire;	}
	public boolean getPost() {	return this.post;	}
	public boolean getGet() {	return this.get;	}
	public boolean getPut() {	return this.put;	}
	public boolean getDelete() {	return this.delete;	}
	public boolean getOptions() {	return this.options;	}
	public boolean getHead() {	return this.head;	}
	public boolean isMethodAllowed(String method) {
		HashMap<String, Boolean> map = new HashMap<>();
		map.put("post", getPost());
		map.put("get", getGet());
		map.put("put", getPut());
		map.put("delete", getDelete());
		map.put("options", getOptions());
		map.put("head", getHead());
		
		return map.get(method);
	}
//	public String getPageStaticParamsName() {	return this.pageParamsName;	}
//	public String[] getPageStaticParams() {	return this.pageParams;	} 
	public LinkedHashMap<String, Boolean> getPageValue(){	return this.pageValue;	}
	public boolean IsApiPage() {	return this.isApi;	}
	public boolean getPageStatic() {	return this.isPageStatic;	}
	public String getMyInfo() {	return "Control: " + (this.controlName) + " | Service: " + (this.serviceName) + " | Tag: " + (getTag());	}
}