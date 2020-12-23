package com.mkweb.security;

import java.util.ArrayList;




import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.mkweb.data.PageJsonData;
import com.mkweb.data.SqlJsonData;
import com.mkweb.logger.MkLogger;
import com.mkweb.config.MkPageConfigs;
import com.mkweb.config.MkRestApiPageConfigs;
import com.mkweb.config.MkRestApiSqlConfigs;
import com.mkweb.config.MkSQLJsonConfigs;

public class CheckPageInfo {
	private String TAG = "[CheckPageInfo]";
	private MkLogger mklogger = MkLogger.Me();

	public String regularQuery(String controlName, String serviceName, boolean isApi) {
		ArrayList<SqlJsonData> resultSqlData = null;
		if(isApi)
			resultSqlData = MkSQLJsonConfigs.Me().getControl(controlName);
		else
			resultSqlData = MkSQLJsonConfigs.Me().getControl(controlName);

		if(resultSqlData == null) {
			resultSqlData = MkSQLJsonConfigs.Me().getControlByServiceName(serviceName);
			
			if(resultSqlData == null) {
				mklogger.error(TAG, "There is no sql control named : " + controlName);
				return null;	
			}
		}
		/*
		 * n개의 SQL 파일 중 해당하는 Control 찾음!
		 */
		String[] result = null;
		for(int i = 0; i < resultSqlData.size(); i++) {
			SqlJsonData tempJsonData = resultSqlData.get(i);
			if(tempJsonData.getServiceName().contentEquals(serviceName)) {
				result = tempJsonData.getData();
				break;
			}
		}
		
		return result[0];
	}

	public String getRequestPageParameterName(HttpServletRequest request, PageJsonData pageStaticData) {
		Enumeration params = request.getParameterNames();
		String requestParams = null;
		String pageStaticParameterName = pageStaticData == null ? pageStaticData.getParameter() : null;
		String[] pageStaticParameters = null;
		int staticPassCount = 0;
		while(params.hasMoreElements()) {
			String name = params.nextElement().toString().trim();

			if(name.contains(".")) {

				String userRequestParameterName = name.split("\\.")[0];

				if(requestParams != null) {
					if(requestParams.contentEquals(userRequestParameterName)) {
						if(pageStaticParameterName != null)
							if(userRequestParameterName.contentEquals(pageStaticParameterName))
								staticPassCount++;
						continue;
					} else {
						mklogger.error(TAG, " (func getRequestPageParameterName) Request parameter is not valid( old: " + requestParams + " / new: " + userRequestParameterName);
						mklogger.debug(TAG, " The parameter name is not same as previous parameter name.");
						return null;
					}
				}else {
					if(staticPassCount > 0) {
						mklogger.error(TAG, " (func getRequestPageParameterName) User request two different service in single request.");
						mklogger.debug(TAG, " Static value checked before without parameter name, however current request include parameter name.");
						return null;
					}
					if(pageStaticParameterName != null)
						if(userRequestParameterName.contentEquals(pageStaticParameterName))
							staticPassCount++;
					
					requestParams = userRequestParameterName;
					continue;
				}
			} else {
				if(pageStaticParameterName == null) {
					mklogger.error(TAG, " (func getRequestPageParameterName) Request parameter is not valid(User requested with no parameter.)");
					return null;
				}
				
				pageStaticParameters = pageStaticData.getData();
				for(int i = 0; i < pageStaticParameters.length; i++) {
					String pageValue = pageStaticParameters[i];

					if(name.contentEquals(pageValue)) {
						staticPassCount++;
						break;
					}
				}
			}
		}
		
		if(staticPassCount > 0) {
			if(staticPassCount == pageStaticParameters.length) {
				requestParams = "__MKWEB_STATIC_VALUE__";
				mklogger.warn(TAG, "STATIC VALUE HAS SET");
			}
		}
		
		return requestParams;
	}

	public ArrayList<String> getRequestParameterValues(HttpServletRequest request, String parameter, PageJsonData pageStaticData){
		ArrayList<String> requestValues = new ArrayList<String>();
		
		Enumeration params = request.getParameterNames();

		while(params.hasMoreElements()) {
			String name = params.nextElement().toString().trim();
			if(name.contains(".")) {
				String[] nname = name.split("\\.");
				String userRequestParameter = nname[0];
				String userRequestValue = nname[1];
				
				if(!userRequestParameter.contentEquals(parameter)) {
					mklogger.error(TAG, "(func getRequestParameterValues) Request parameter is not valid. (Unvalid parameter(" + userRequestParameter + ") for " + parameter);
					return null;
				}
				requestValues.add(userRequestValue);
				continue;
			}else {
				if(parameter.contentEquals("__MKWEB_STATIC_VALUE__")) {
					String[] pageStaticParameters = null;
					pageStaticParameters = pageStaticData.getData();
					
					for(int i = 0; i < pageStaticParameters.length; i++) {
						if(name.contentEquals(pageStaticParameters[i])) {
							requestValues.add(name);
							continue;
						}
					}
				}
			}
		}

		return requestValues;
	}

	public String setQuery(String query) {
		String befQuery = query;
		if(befQuery != null) {
			mklogger.debug(TAG, "(func setQuery): befQuery: " + befQuery);
			String[] testQueryList = befQuery.split("@");
			String[] replaceTarget = null;

			if(testQueryList.length == 1)
				testQueryList = null;

			if(testQueryList != null) {
				if(testQueryList.length > 0)
				{
					replaceTarget = new String[(testQueryList.length-1)/2];
					for(int i = 0; i < replaceTarget.length; i++) {
						replaceTarget[i] = testQueryList[(i*2)+1];
					}
				}else {	return null;	}
			}

			
			
			if(replaceTarget != null) {
				for(int i = 0; i < replaceTarget.length; i++) {
					befQuery = befQuery.replaceFirst(("@" + replaceTarget[i]+ "@"), "?");
				}
			}else {
				return null;
			}
		}
		return befQuery;
	}

	public boolean comparePageValueWithRequestValue(LinkedHashMap<String, Boolean> pageValue, ArrayList<String> requestValue, PageJsonData pageStaticData, boolean isApi) {
		LinkedHashMap<String, Boolean> staticData = pageStaticData.getPageValue(); 
		/* 사용전에 null 확인하기 */
		
		int pageSize = pageValue.size();
		int requestSize = requestValue.size();
		
		/* debug */
		mklogger.debug(TAG, "pageValue : \n" + pageValue.toString());
		mklogger.debug(TAG, "requestValues : \n" + requestValue.toString());
		/* debug */
		
		if(pageSize != requestSize) {
			mklogger.error(TAG, " Number of request parameters is not same as number of allowed parameters.");
			mklogger.debug(TAG, " Page value size : " + pageSize + " | request size : " + requestSize);
			return false;
		}
		
		String pageValues = "";
		LinkedHashMap<String, Boolean> pv = null;
		
		if(pageValue != null)
			pv = new LinkedHashMap<>(pageValue);
		else
			pv = new LinkedHashMap<>();
		
		if(pv.size() > 0 && pv != null) {
			Set<String> entrySet = pv.keySet();
			Iterator iter = entrySet.iterator();
			while(iter.hasNext()) {
				String key = (String) iter.next();
				pageValues += key;
			}
		}
		
		String requestValues = "";
		if(requestValue != null) {
			for(String rv : requestValue) {
				requestValues += rv;
			}
		}

		char[] pvc = pageValues.toCharArray();
		char[] rvc = requestValues.toCharArray();
		Arrays.sort(pvc);
		Arrays.sort(rvc);
		
		pageValues = new String(pvc);
		requestValues = new String(rvc);
		
		mklogger.debug(TAG, "pvc : " + pageValues + "\nrvc : " + requestValues);
		
		return (pageValues.contentEquals(requestValues));
	}

	public boolean isValidPageConnection(String requestControlName, String[] requestDir) {
		ArrayList<PageJsonData> resultPageData = MkPageConfigs.Me().getControl(requestControlName);

		if(resultPageData == null || resultPageData.size() < 1)
			return false;
		PageJsonData jsonData = resultPageData.get(0);
		/*
		 * 오직허용: log_dir + page control name
		 * requestDir = URI / 자른거.
		 * mkPage = request page control name
		 */
		String AllowPath = jsonData.getLogicalDir();
		String userLogicalDir = "";

		if(requestDir != null) {
			for(int i = 1; i < requestDir.length-1; i++) 
				userLogicalDir += "/" + requestDir[i];
		}

		if(userLogicalDir.equals(""))
			userLogicalDir = "/";

		String c1 = userLogicalDir + requestControlName;
		String c2 = AllowPath + jsonData.getControlName();

		if(!c1.equals(c2))
			return false;

		return true;
	}

	public boolean isValidApiPageConnection(String requestControlName, String[] requestDir) {
		ArrayList<PageJsonData> resultPageData = MkRestApiPageConfigs.Me().getControl(requestControlName);

		if(resultPageData == null || resultPageData.size() < 1)
			return false;
		PageJsonData jsonData = resultPageData.get(0);
		/*
		 * 오직허용: log_dir + page control name
		 * requestDir = URI / 자른거.
		 * mkPage = request page control name
		 */
		String userLogicalDir = "";

		if(requestDir != null) {
			for(int i = 1; i < requestDir.length-1; i++) 
				userLogicalDir += "/" + requestDir[i];
		}

		if(userLogicalDir.equals(""))
			userLogicalDir = "/";

		String c1 = userLogicalDir + requestControlName;
		String c2 = "/" + jsonData.getControlName();

		if(!c1.equals(c2)){
			return false;
		}

		return true;
	}
}
