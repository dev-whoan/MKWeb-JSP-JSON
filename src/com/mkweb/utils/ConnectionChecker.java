package com.mkweb.utils;

import java.util.ArrayList;




import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import com.mkweb.data.Device;
import com.mkweb.data.MkPageJsonData;
import com.mkweb.data.MkSqlJsonData;
import com.mkweb.logger.MkLogger;
import com.mkweb.config.MkFTPConfigs;
import com.mkweb.config.MkPageConfigs;
import com.mkweb.config.MkRestApiPageConfigs;
import com.mkweb.config.MkRestApiSqlConfigs;
import com.mkweb.config.MkSQLConfigs;

public class ConnectionChecker {
	private String TAG = "[ConnectionChecker]";
	private MkLogger mklogger = MkLogger.Me();

	public String regularQuery(String controlName, String serviceName, boolean isApi) {
		ArrayList<MkSqlJsonData> resultSqlData = null;
		if(isApi)
			resultSqlData = MkRestApiSqlConfigs.Me().getControl(controlName);
		else
			resultSqlData = MkSQLConfigs.Me().getControl(controlName);

		if(resultSqlData == null) {
			if(isApi)
				resultSqlData = MkRestApiSqlConfigs.Me().getControlByServiceName(serviceName);
			else
				resultSqlData = MkSQLConfigs.Me().getControlByServiceName(serviceName);

			if(resultSqlData == null) {
				mklogger.error(TAG, "There is no sql control named : " + controlName);
				return null;	
			}
		}

		String[] result = null;
		for(int i = 0; i < resultSqlData.size(); i++) {
			MkSqlJsonData tempJsonData = resultSqlData.get(i);
			if(tempJsonData.getServiceName().contentEquals(serviceName)) {
				result = tempJsonData.getData();
				break;
			}
		}

		return result[0];
	}

	public String getRequestPageParameterName(HttpServletRequest request, boolean isStaticService, MkPageJsonData pageStaticData) {
		Enumeration<String> params = request.getParameterNames();
		String requestParams = null;
		String pageStaticParameterName = null;
		String[] pageStaticParameters = null;
		if (pageStaticData != null) {
			pageStaticParameterName = pageStaticData.getParameter();
			pageStaticParameters = pageStaticData.getData();
		} 
		int staticPassCount = 0;
		while (params.hasMoreElements()) {
			String name = params.nextElement().toString().trim();
			if (isStaticService) {
				if (name.contains(".")) {
					String userRequestParameterName = name.split("\\.")[0];
					if (requestParams != null) {
						if (requestParams.contentEquals(userRequestParameterName) || !requestParams.contentEquals(pageStaticParameterName))
							continue; 
						mklogger.error(this.TAG, " (func getRequestPageParameterName) Request parameter is not valid( old: " + requestParams + " / new: " + userRequestParameterName);
						mklogger.debug(this.TAG, " The parameter name is not same as page static parameter name.");
						return null;
					} 
					if (!userRequestParameterName.contentEquals(pageStaticParameterName))
						continue; 
					requestParams = userRequestParameterName;
				} 
				continue;
			} 
			if (name.contains(".")) {
				String userRequestParameterName = name.split("\\.")[0];
				mklogger.debug(TAG, "name : " + name + ", urpn: " + userRequestParameterName + " :: pspn " + pageStaticParameterName);
				if (requestParams != null) {
					if (requestParams.contentEquals(userRequestParameterName) || (pageStaticParameterName != null && requestParams.contentEquals(pageStaticParameterName)))
						continue; 
					mklogger.error(this.TAG, " (func getRequestPageParameterName) Request parameter is not valid( old: " + requestParams + " / new: " + userRequestParameterName);
					mklogger.debug(this.TAG, " The parameter name is not same as previous parameter name.");
					return null;
				} 
				
				if (pageStaticParameterName != null && userRequestParameterName.contentEquals(pageStaticParameterName))
					continue; 
				requestParams = userRequestParameterName;
				continue;
			} 
			if (pageStaticData == null) {
				mklogger.error(this.TAG, " (func getRequestPageParameterName) Request parameter is not valid(User requested with no parameter.)");
				return null;
			} 
		} 
		if (staticPassCount > 0 && (requestParams.contentEquals("__MKWEB_STATIC_VALUE__") || (pageStaticParameterName != null && requestParams.contentEquals(pageStaticParameterName))) && 
				staticPassCount == pageStaticParameters.length) {
			requestParams = "__MKWEB_STATIC_VALUE__";
			mklogger.warn(this.TAG, "STATIC VALUE HAS SET");
		} 
		return requestParams;
	}

	public ArrayList<String> getRequestParameterValues(HttpServletRequest request, String parameter, MkPageJsonData pageStaticData){
		ArrayList<String> requestValues = new ArrayList<String>();

		Enumeration<String> params = request.getParameterNames();
		String pageStaticParameter = null;
		String[] pageStaticParameterValues = null;
		if(pageStaticData != null) {
			pageStaticParameter = pageStaticData.getParameter();
			pageStaticParameterValues = pageStaticData.getData();
		}
		/* static value면 static 요청이 아닐 경우 Skip 한다. */
		/* static value가 아니면 static 요청일 때 Skip 한다. */
		/* --> Parameter가 같은것만 받는다. */
		while(params.hasMoreElements()) {
			String name = params.nextElement().toString().trim();
			if(name.contains(".")) {
				String[] nname = name.split("\\.");
				String userRequestParameter = nname[0];
				String userRequestValue = nname[1];

				/*
				if(!userRequestParameter.contentEquals(parameter) && !parameter.contentEquals(pageStaticData.getParameter())) {
					mklogger.error(TAG, "(func getRequestParameterValues) Request parameter is not valid. (Unvalid parameter(" + userRequestParameter + ") for " + parameter);
					return null;
				}
				 */
				if(userRequestParameter.contentEquals(parameter))
					requestValues.add(userRequestValue);

				continue;
			}else {
				if(pageStaticParameter != null) {
					if(parameter.contentEquals(pageStaticParameter)) {
						for(int i = 0; i < pageStaticParameterValues.length; i++) {
							if(name.contentEquals(pageStaticParameterValues[i])) {
								requestValues.add(name);
								continue;
							}
						}
					}
				}
			}
		}

		return requestValues;
	}

	public String setQuery(String query) {
		String aftQuery = query;
		if(aftQuery != null) {
			mklogger.debug(TAG, "(func setQuery): befQuery: " + aftQuery);
			String[] testQueryList = aftQuery.split("@");
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
					aftQuery = aftQuery.replaceFirst(("@" + replaceTarget[i]+ "@"), "?");
				}
			}else {
				return null;
			}
		}
		return aftQuery;
	}

	public String setApiQuery(String query) {
		String aftQuery = query;

		if(aftQuery != null) {

		}

		return aftQuery;
	}

	public boolean comparePageValueWithRequestValue(LinkedHashMap<String, Boolean> pageValue, ArrayList<String> requestValue, MkPageJsonData pageStaticDatas, boolean isStaticService, boolean isApi) {
		LinkedHashMap<String, Boolean> staticData = null;
		if(pageStaticDatas != null)
			staticData = pageStaticDatas.getPageValue(); 

		int pageSize = pageValue.size();
		int requestSize = requestValue.size();

		if(pageSize != requestSize) {
			mklogger.error(TAG, " Number of request parameters is not same as number of allowed parameters.");
			mklogger.temp(TAG + " Page value size : " + pageSize + " | request size : " + requestSize, false);
			mklogger.temp("pageValue : " + pageValue.toString(), false);
			mklogger.flush("debug");
			return false;
		}

		String pageValues = "";
		LinkedHashMap<String, Boolean> pv = null;

		if(pageValue != null)
			pv = new LinkedHashMap<>(pageValue);

		if(pv.size() > 0 && pv != null) {
			Set<String> entrySet = pv.keySet();
			Iterator<String> iter = entrySet.iterator();
			while(iter.hasNext()) {
				String key = iter.next();
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

		return (pageValues.contentEquals(requestValues));
	}

	public boolean isValidPageConnection(String requestControlName) {
		ArrayList<MkPageJsonData> resultPageData = MkPageConfigs.Me().getControl(requestControlName);

		if(resultPageData == null || resultPageData.size() < 1)
			return false;
		
		return true;
	}
	
	public String getRequestPageLanguage(String requestControlName, String userPlatform, String userAcceptLanguage, ArrayList<MkPageJsonData> resultPageData) {
		String defaultLanguage = null;
		if(userPlatform == null)
			userPlatform = "desktop";
		try {
			defaultLanguage = Locale.LanguageRange.parse(userAcceptLanguage)
					.stream().sorted(Comparator.comparing(Locale.LanguageRange::getWeight).reversed())
					.map(range -> new Locale(range.getRange())).collect(Collectors.toList()).get(0).toString().substring(0, 2);
		} catch (NullPointerException e) {
			return "error_404";
		}

		String lastURI = resultPageData.get(0).getLastURI();
		String requestServiceURI = (!lastURI.contentEquals("") ? requestControlName.substring(0, requestControlName.indexOf(lastURI)) : requestControlName);
		if(!requestServiceURI.contentEquals("")) {
			if(requestServiceURI.charAt(requestServiceURI.length()-1) == '/') {
				requestServiceURI = requestServiceURI.substring(0, requestServiceURI.length()-1);
			}	
		}
		
		ArrayList<Device> devices = resultPageData.get(0).getAllDevices();
		Device userDevice = null;
		int desktopIndex = -1;
		boolean isDone = false;
		for(int i = 0; i < devices.size(); i++) {
			Device currentDevice = devices.get(i);
			if(currentDevice.getControlName().contentEquals("desktop"))
				desktopIndex = i;
			
			if(currentDevice.getControlName().contentEquals(userPlatform)) {
				/* Language 선택 할 때, URI가 완벽히 일치하면 그 language 선택 */
				HashMap<String, String[]> currentDeviceInfo = currentDevice.getDeviceInfo();
				Set<String> key = currentDeviceInfo.keySet();
				Iterator<String> iter = key.iterator();
				int sameCount = 0;
				while(iter.hasNext()) {
					String cService = iter.next();
					if(requestServiceURI.contentEquals(currentDeviceInfo.get(cService)[2])) {
						userDevice = currentDevice;
						if(!cService.contentEquals("default"))
							defaultLanguage = cService;
						isDone = true;
						break;
					}
				}
				
				if(isDone)
					break;
				
				if(!isDone && userDevice != null) {
					defaultLanguage = "default";
				}
			}
		}
		
		if(userDevice == null) {
			/* Set Platform and Language	*/
			if(desktopIndex != -1) {
				userDevice = devices.get(desktopIndex);
				if(userDevice.getDeviceInfo(defaultLanguage) == null)
					defaultLanguage = "default";
			}else {
				return "error_500";
			}
		}else {
			if(userDevice.getDeviceInfo(defaultLanguage) == null)
				defaultLanguage = "default";
		}
		String[] uriInfo = userDevice.getDeviceInfo(defaultLanguage);
		
		return uriInfo[0] + "/" + uriInfo[1];
	}

	public boolean isValidApiPageConnection(String requestControlName, String[] requestDir) {
		ArrayList<MkPageJsonData> resultPageData = MkRestApiPageConfigs.Me().getControl(requestControlName);

		if(resultPageData == null || resultPageData.size() < 1)
			return false;
		MkPageJsonData jsonData = resultPageData.get(0);

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
