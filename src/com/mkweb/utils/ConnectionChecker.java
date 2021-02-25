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
	private static final String TAG = "[ConnectionChecker]";
	private static final MkLogger mklogger = new MkLogger(TAG);

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
				mklogger.error("There is no sql control named : " + controlName);
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

	/*
	 * 기존 : request parameter에서 parameter 앞 첨자를 구분했고, 해당 첨자들이 일치하지 않으면 취소시킴.
	 * 이제는 service단을 받아서, service에서 요청한 것들만 Ok 시킴. 이 때, service가 정의한 파라미터 수와
	 * 내가 받아들인 파라미터 수가 같은지 보면 됨
	 * 인자 5개는 new, 3개는 old
	 * */
	public String getRequestPageParameterName(HttpServletRequest request, boolean isStaticService, MkPageJsonData pageStaticData,
										String pageParameter, int pageParameterSize) {
		Enumeration<String> params = request.getParameterNames();
		String requestParams = null;
		String pageStaticParameterName = null;
		String[] pageStaticParameters = null;
		if (pageStaticData != null) {
			pageStaticParameterName = pageStaticData.getParameter();
			pageStaticParameters = pageStaticData.getData();
		} 
		
		int checked = 0;
		ArrayList<String> addedValue = new ArrayList<>();
		while(params.hasMoreElements()) {
			String name = params.nextElement().toString().trim();
			if(name.contains(".")) {
				String userRequestParameterNam = name.split("\\.")[0];
				
				if(pageParameter.contentEquals(userRequestParameterNam)) {
					if(addedValue.indexOf(name) == -1) {
						requestParams = userRequestParameterNam;
						checked++;
						addedValue.add(name);
					} else {
						mklogger.warn("Request value already checked." + name);
					}
				}
				continue;
			} else {
				//pageStatic Parameter
				if(isStaticService) {
					if(addedValue.indexOf(name) == -1) {
						checked++;
						addedValue.add(name);
					} else {
						mklogger.warn("Request value already checked." + name);
					}
				}
			}
		}
		
		if(isStaticService) {
			if(checked >= pageParameterSize) {
				requestParams = "__MKWEB_STATIC_VALUE__";
			} else {
				requestParams = null;
				mklogger.error("(func getRequestPageParameterName-StaticService) Request parameters are not valid. Need to receive more parameters.");
			}
		}else {
			if(checked != pageParameterSize) {
				requestParams = null;
				mklogger.debug("page parameter : " + pageParameter);
				mklogger.debug("received : " + checked + ", pageSize : " + pageParameterSize);
				mklogger.error("(func getRequestPageParameterName) Request parameters are not valid. May receive more parameters or less parameters than required.");
			}
		}
		
		return requestParams;
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
		mklogger.debug("params have elements : " + params.hasMoreElements());
		while (params.hasMoreElements()) {
			//need page data
			String name = params.nextElement().toString().trim();
			mklogger.debug("name: " + name);
			if (isStaticService) {
				if (name.contains(".")) {
					String userRequestParameterName = name.split("\\.")[0];
					mklogger.debug("urqn : " + userRequestParameterName);
					if (requestParams != null) {
						if (requestParams.contentEquals(userRequestParameterName) || !requestParams.contentEquals(pageStaticParameterName))
							continue; 
						mklogger.error(" (func getRequestPageParameterName) Request parameter is not valid( old: " + requestParams + " / new: " + userRequestParameterName);
						mklogger.debug(" The parameter name is not same as page static parameter name.");
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
				mklogger.debug("name : " + name + ", urpn: " + userRequestParameterName + " :: pspn " + pageStaticParameterName);
				if (requestParams != null) {
					if (requestParams.contentEquals(userRequestParameterName) || (pageStaticParameterName != null && requestParams.contentEquals(pageStaticParameterName)))
						continue; 
					mklogger.error(" (func getRequestPageParameterName) Request parameter is not valid( old: " + requestParams + " / new: " + userRequestParameterName);
					mklogger.debug(" The parameter name is not same as previous parameter name.");
					return null;
				} 
				
				if (pageStaticParameterName != null && userRequestParameterName.contentEquals(pageStaticParameterName))
					continue; 
				requestParams = userRequestParameterName;
				continue;
			} 
			if (pageStaticData == null) {
				mklogger.error(" (func getRequestPageParameterName) Request parameter is not valid(User requested with no parameter.)");
				return null;
			} 
			
		} 
		if (staticPassCount > 0 && (requestParams.contentEquals("__MKWEB_STATIC_VALUE__") || (pageStaticParameterName != null && requestParams.contentEquals(pageStaticParameterName))) && 
				staticPassCount == pageStaticParameters.length) {
			requestParams = "__MKWEB_STATIC_VALUE__";
			mklogger.warn("STATIC VALUE HAS SET");
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
		/* static value�� static ��û�� �ƴ� ��� Skip �Ѵ�. */
		/* static value�� �ƴϸ� static ��û�� �� Skip �Ѵ�. */
		/* --> Parameter�� �����͸� �޴´�. */
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
			mklogger.debug("(func setQuery): befQuery: " + aftQuery);
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
			mklogger.error(" Number of request parameters is not same as number of allowed parameters.");
			mklogger.temp(" Page value size : " + pageSize + " | request size : " + requestSize, false);
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
		mklogger.debug("device size : " + devices.size());
		int deviceIndex = -1;
		int gotcha = 0;
		for(int i = 0; i < devices.size(); i++) {
			Device currentDevice = devices.get(i);
			String currentName = currentDevice.getControlName();
			
			if(currentName.contentEquals("desktop")) {
				desktopIndex = i;
				gotcha++;
			}
			if(currentName.contentEquals(userPlatform)) {
				deviceIndex = i;
				gotcha++;
			}
			/* Found Desktop and UserPlatform */
			if(gotcha > 1)
				break;
		}
		
		Device currentDevice = null;
		if(deviceIndex != -1) {
			currentDevice = devices.get(deviceIndex);
		} else {
			currentDevice = devices.get(desktopIndex);
		}
		
		HashMap<String, String[]> currentDeviceInfo = currentDevice.getDeviceInfo();
		Set<String> key = currentDeviceInfo.keySet();
		Iterator<String> iter = key.iterator();
		while(iter.hasNext()) {
			String cService = iter.next();
			if(requestServiceURI.contentEquals(currentDeviceInfo.get(cService)[2])) {
				userDevice = currentDevice;
				if(!cService.contentEquals("default"))
					defaultLanguage = cService;
				break;
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
		

		mklogger.debug("dl 2 : " + defaultLanguage);
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
