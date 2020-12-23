package com.mkweb.restapi;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.mkweb.config.MkConfigReader;
import com.mkweb.config.MkRestApiPageConfigs;
import com.mkweb.config.MkRestApiSqlConfigs;
import com.mkweb.data.MkJsonData;
import com.mkweb.data.PageJsonData;
import com.mkweb.data.SqlJsonData;
import com.mkweb.database.MkDbAccessor;
import com.mkweb.logger.MkLogger;
import com.mkweb.security.CheckPageInfo;

/**
 * Servlet implementation class MkRestApi
 */

/*
1. 순서 정렬
2. 전체 조회
3. 응답 정리 ( 코드, 콘텐츠 타입, 실패 사유 등. )
 */

public class MkRestApi extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private MkLogger mklogger = MkLogger.Me();
	private String TAG  = "[MkRestApi]";
	private CheckPageInfo cpi = null;
	private String[] methods = {
			"post",
			"get",
			"put",
			"delete",
			"options",
			"head"
	};
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public MkRestApi() {
		super();
		cpi = new CheckPageInfo();
	}

	private boolean checkMethod(ArrayList<PageJsonData> pageJsonData, String requestMethod) {
		for(PageJsonData pjd : pageJsonData) {
			if(pjd.getMethod().toString().toLowerCase().contentEquals(requestMethod)) {
				return true;
			}
		}
		
		return false;	
	}

	private boolean isValidKey(String key, String mkPage) {
		if(!MkConfigReader.Me().get("mkweb.restapi.use").equals("yes"))
			return false;
		if(!MkRestApiPageConfigs.Me().isApiPageSet())
			return false;

		boolean isDone = false;
		MkRestApiGetKey mra = new MkRestApiGetKey();
		ArrayList<Object> apiKeyList = mra.GetKey();

		mklogger.temp(TAG, " REST Api Key has searched : " + key + " Result: " , false);

		if(apiKeyList != null) {
			for(int i = 0; i < apiKeyList.size(); i++) {
				HashMap<String, Object> result = new HashMap<String, Object>();
				result = (HashMap<String, Object>) apiKeyList.get(i);
				if(result.get("api_key").equals(key)) {
					mklogger.temp(TAG, " key is valid! (user_id : " + result.get("user_id") +")", false);
					mklogger.flush("info");
					isDone = true;
					break;
				}
			}	
		}else {
			mklogger.temp(TAG, " Failed to search the key! (No Key List)", false);
			mklogger.flush("warn");
		}

		if(!isDone) {
			mklogger.temp(TAG, " Failed to search the key! (Key is invalid)", false);
			mklogger.flush("warn");
		}

		return isDone;
	}

	private Map<String, String> stringToMap(String[] strArray){
		Map<String, String> result = new HashMap<String, String>();
		for(int i = 0; i < strArray.length; i++) {
			String tempParameter = strArray[i].split("\\=")[0];
			String tempValue = strArray[i].split("\\=")[1];
			result.put(tempParameter, tempValue);
		}
		
		return result;
	}
	
	protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		request.setAttribute("api-method", "head");
		doTask(request, response);
	}

	protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		request.setAttribute("api-method", "options");
		doTask(request, response);
	}

	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		request.setAttribute("api-method", "put");
		doTask(request, response);
	}

	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		request.setAttribute("api-method", "delete");
		doTask(request, response);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setAttribute("api-method", "get");
		doTask(request, response);
	}
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setAttribute("api-method", "post");
		doTask(request, response);
	}

	private void doTask(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		request.setCharacterEncoding("UTF-8");
		if(!MkConfigReader.Me().get("mkweb.restapi.use").equals("yes")) {
			response.sendError(404);
			return;
		}

		final String MKWEB_URI_PATTERN = MkConfigReader.Me().get("mkweb.restapi.uripattern");
		final String MKWEB_API_ID = MkConfigReader.Me().get("mkweb.restapi.request.id");
		final String MKWEB_SEARCH_KEY = MkConfigReader.Me().get("mkweb.restapi.searchkey.exp");
		final boolean MKWEB_USE_KEY = MkConfigReader.Me().get("mkweb.restapi.usekey").contentEquals("yes") ? true : false;
		final String REQUEST_METHOD = request.getAttribute("api-method").toString().toLowerCase();
		mklogger.debug(TAG, "======API CONFIG======"
							+"\nURI Pattern\t" + MKWEB_URI_PATTERN
							+"\nAPI ID\t\t" + MKWEB_API_ID
							+"\nUSE KEY\t\t" + MKWEB_USE_KEY
							+"\nAPI KEY\t\t" + MKWEB_SEARCH_KEY);
		
		String reqApiData = null;
		String requestURI = request.getRequestURI();
		String[] reqPage = null;
		String mkPage = null;
		String searchKey = null;

		JSONObject requestParameterJson = null;
		
		MkJsonData mkJsonData = new MkJsonData();
		
		reqPage = requestURI.split("/" + MKWEB_URI_PATTERN + "/");

		if(reqPage.length < 2) {
			//예외
			mklogger.debug(TAG, "401 here 3");
			response.sendError(401);
			return;
		}
		mkPage = reqPage[1];

		if(mkPage.contains("/")) {
			mkPage = mkPage.split("/")[0];
		}

		mklogger.debug(TAG, "Request MKPage : " + mkPage + "|Method : " + REQUEST_METHOD);
		/*
		 * 컨트롤이 존재 하는가?
		 */
		
		ArrayList<PageJsonData> control = MkRestApiPageConfigs.Me().getControl(mkPage);
		
		if(control == null) {
			/*예외*/
			mklogger.error(TAG, "[API] Control " + mkPage + " is not exist.");
			response.sendError(404);
			return;
		}
		Enumeration<String> rpn = request.getParameterNames();
		/* 컨트롤 내에 method가 허용 되는가? 
		if(!checkMethod(control, REQUEST_METHOD)) {
			mklogger.error(TAG, "[API] Control " + mkPage + " does not allow method : " + REQUEST_METHOD);
			response.sendError(404);
			return;
		}
		*/
		
		mklogger.debug(TAG, "REQUEST URI : " + requestURI);
		/*
		 * Get에서 / 구분하기. 지금은 쿼리스트링 됨
		 * /mk_api_key/users/name/dev.whoan 이런거
		 * 그리고 Condition을 벗어나는 입력이 있는지
		 */
		if(REQUEST_METHOD.contentEquals("get")) {
			mkJsonData.setData(request.getParameter(MKWEB_API_ID));
			if(mkJsonData.setJsonObject()) {
				requestParameterJson = mkJsonData.getJsonObject();
			} else {
				String[] tempURI = requestURI.split("/");
				int shouldCheckQuery = -1;
				/*
				 * -1 : do Nothing
				 * 0  : 전체 조회
				 * 1  : String check 
				 */
				if(tempURI[(tempURI.length-1)].contentEquals(mkPage)) {
					//apiData가 포함되는가?
					if(request.getParameterMap().size() == 1) {
						String element = rpn.nextElement();
						String elementValue = null;
						if(element.contentEquals(MKWEB_API_ID)) {
							elementValue = request.getParameter(element);
							if(elementValue.contains("=") && elementValue.contains(MKWEB_SEARCH_KEY)) {
								mklogger.debug(TAG, "경우 1과 같음(/mk_api_key/users 처럼)");
							}else if(!elementValue.contains(MKWEB_SEARCH_KEY)) {
								mklogger.debug(TAG, "(1) 경우 2 - /mk_api_key/users?queryString~");
								shouldCheckQuery = 1;
							}
						}else {
							if(!element.contentEquals(MKWEB_SEARCH_KEY)) {
								mklogger.debug(TAG, "(1) 경우 2 - /mk_api_key/users?queryString~");
								shouldCheckQuery = 1;							
							}else {
								mklogger.debug(TAG, "경우 1과 같음 (/mk_api_key/users 처럼)");
							}
						}
					}else {
						shouldCheckQuery = 1;
					}
					
					shouldCheckQuery = 0;
				}
				
				if(shouldCheckQuery == 1){
					//String 형식 확인
					mklogger.warn(TAG, "Given data is not valid to cast JSONObject.");
					mklogger.warn(TAG, "Try to convert into MkJsonObject...");
					
					String tempAPIID = request.getParameter(MKWEB_API_ID);
					HashMap<String, String> result = new HashMap<String, String>();
					mklogger.debug(TAG, "APIData가 존재 하는가? " + request.getParameter(MKWEB_API_ID));
					
					if(tempAPIID != null) {
						//&가 존재 하는가?
						String[] tempArr = null;
						if(tempAPIID.contains("&")) {
							mklogger.debug(TAG, " & 있음 " );
							tempArr = tempAPIID.split("&");
							mklogger.debug(TAG, "tempArr: " + tempArr);
							for(int i = 0; i < tempArr.length; i++) {
								mklogger.debug(TAG, "tempArr i : " + tempArr[i]);
							}
							result = (HashMap<String, String>) stringToMap(tempArr);
						}else {
							tempArr = new String[1];
							tempArr[0] = tempAPIID;
							result = (HashMap<String, String>) stringToMap(tempArr);
						}
					}
					
					Enumeration<String> parameters =  request.getParameterNames();
					while(parameters.hasMoreElements()) {
						String parameter = parameters.nextElement();
						if(!parameter.contentEquals(MKWEB_API_ID))
							result.put(parameter, request.getParameter(parameter));
					}
					
					requestParameterJson = mkJsonData.mapToJson(result);
					
					mklogger.debug(TAG, "변환 결과 : " + requestParameterJson);
					
					if(requestParameterJson == null) {
						mklogger.error(TAG, "API Request only allow with JSON type. Cannot convert given data into JSON Type.");
						return;
					}
				}
			}
		} else {
			mkJsonData.setData(request.getParameter(MKWEB_API_ID));
			if(mkJsonData.setJsonObject()) {
				requestParameterJson = mkJsonData.getJsonObject();
				mklogger.debug(TAG, "확인: " + requestParameterJson.toString());
			} else {
				String tempJsonString = mkJsonData.stringToJsonString(mkJsonData.getData());
				
				mklogger.debug(TAG, "stringToJsonString : " + tempJsonString);
				
				mkJsonData.setData(tempJsonString);
				
				if(!mkJsonData.setJsonObject()) {
					mklogger.error(TAG, "API Request only allow with JSON type. Cannot convert given data into JSON Type.");
					return;
				}
				
				requestParameterJson = mkJsonData.getJsonObject();
			}
		}
		
/* {"name":""}
 * func isValidDataForJson) ParseException:: Unexpected token RIGHT BRACE(}) at position 8. Given data is not valid for JSONObject.
{"name":}
/users/name
/users/u_class
{"name":"dev.whoan","u_class":}
/users/name/dev.whoan/u_class
jsonObject null
 

		if(!isDone) {
			if(method.contentEquals("get")) {
				//조회
				resultObject = doTaskGet(pxData, sqlData, jsonObject, mkPage);
			}else if(method.contentEquals("post")) {
				//삽입, 갱신
				resultObject = doTaskPost(pxData, sqlData, jsonObject, mkPage);
			}else if(method.contentEquals("put")) {
				//조회, 삽입
				resultObject = doTaskPut(pxData, sqlData, jsonObject, mkPage);
			}else if(method.contentEquals("delete")) {
				//삭제
				resultObject = doTaskDelete(pxData, sqlData, jsonObject, mkPage);
			}
			Hash = pxData.getControlName() + sqlData.getServiceName() + method;
			mrap = new MkRestApiResponse(resultObject, lmrap.size()+1, 1, Hash);
		}

		//500 에러. 존재하지 않는 대상이면 500으로 넘어가는 문제
		mkJsonObject.printObject(resultObject);

		if(lmrap.size() == 0)
			lmrap.add(mrap);
		else {
			if(lmrap.size() < 3) {
				lmrap.add(mrap);
			}else {
				lmrap.remove(0);
				lmrap.add(mrap);
			}
		}
		*/
		//Create ResponseData with resultObject.
		MkRestApiData mkApiDData = new MkRestApiData();

		//리스폰스(최초 응답, 이전, 다음 응답 기록)
		response.setStatus(200);
		response.setContentType("application/json;charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();


		out.print("테스트\n");
		out.flush();
	}

	private JSONObject doTaskGet(PageJsonData pxData, SqlJsonData sqlData, JSONObject jsonObject, String mkPage) {
		JSONObject resultObject = null;
		MkDbAccessor DA = new MkDbAccessor();

		String service = pxData.getServiceName();
		String control = pxData.getControlName();
		String befQuery = cpi.regularQuery(control, service, true);
		String query = null;

		mklogger.debug(TAG, " befQuery : "+ befQuery);
		
		query = cpi.setQuery(befQuery);
		mklogger.debug(TAG, "query check : " + query);
		DA.setPreparedStatement(query);
		
		Set keySet = jsonObject.keySet();
		Iterator iter = keySet.iterator();
		ArrayList<String> sqlKey = new ArrayList<String>();
		while(iter.hasNext()) {
			String key = iter.next().toString();
			sqlKey.add(jsonObject.get(key).toString());
		}
		DA.setRequestValue(sqlKey);

		ArrayList<Object> resultTest = null;

		if(sqlData.getAllowLike()) {
			resultTest = DA.executeSELLike(true);
		}
		else {
			resultTest = DA.executeSEL(true);
		}

		String test = "{\"" + mkPage + "\":[";
		if(resultTest != null) {
			for(int l = 0; l < resultTest.size(); l++) {
				String damn = resultTest.get(l).toString();
				damn = damn.replaceAll("=", ":");
				MkRestApiData tttt = new MkRestApiData(damn);
				test += damn;

				if(l < resultTest.size() - 1)
					test += ", ";
			}
			test += "]}";

			MkRestApiData tttt = new MkRestApiData(test);

			if(tttt.setJsonObject()) {
				resultObject = tttt.getJsonObject();
			}
		}else {
			//조회 데이터 없음
		}


		return resultObject;
	}

	private JSONObject doTaskPost(PageJsonData pxData, SqlJsonData sqlData, JSONObject jsonObject, String mkPage) {
		JSONObject resultObject = null;
		MkDbAccessor DA = new MkDbAccessor();

		String service = pxData.getServiceName();
		String control = pxData.getControlName();
		String befQuery = cpi.regularQuery(control, service, true);
		String query = null;

		query = cpi.setQuery(befQuery);

		DA.setPreparedStatement(query);

		Set keySet = jsonObject.keySet();
		Iterator iter = keySet.iterator();
		ArrayList<String> sqlKey = new ArrayList<String>();
		while(iter.hasNext()) {
			String key = iter.next().toString();
			sqlKey.add(jsonObject.get(key).toString());
		}
		DA.setRequestValue(sqlKey);


		ArrayList<Object> resultTest = null;

		if(sqlData.getAllowLike()) 
			resultTest = DA.executeSELLike(true);
		else
			resultTest = DA.executeSEL(true);	


		String test = "{\"" + mkPage + "\":[";
		if(resultTest != null) {
			for(int l = 0; l < resultTest.size(); l++) {
				String damn = resultTest.get(l).toString();
				damn = damn.replaceAll("=", ":");
				MkRestApiData tttt = new MkRestApiData(damn);
				test += damn;

				if(l < resultTest.size() - 1)
					test += ", ";
			}
			test += "]}";

			MkRestApiData tttt = new MkRestApiData(test);

			if(tttt.setJsonObject()) {
				resultObject = tttt.getJsonObject();
			}
		}else {
			//조회 데이터 없음
		}

		return resultObject;
	}

	private JSONObject doTaskPut(PageJsonData pxData, SqlJsonData sqlData, JSONObject jsonObject, String mkPage) {
		JSONObject resultObject = null;
		MkDbAccessor DA = new MkDbAccessor();

		String service = pxData.getServiceName();
		String control = pxData.getControlName();
		String befQuery = cpi.regularQuery(control, service, true);
		String query = null;

		query = cpi.setQuery(befQuery);


		DA.setPreparedStatement(query);

		Set keySet = jsonObject.keySet();
		Iterator iter = keySet.iterator();
		ArrayList<String> sqlKey = new ArrayList<String>();
		while(iter.hasNext()) {
			String key = iter.next().toString();
			sqlKey.add(jsonObject.get(key).toString());
		}
		DA.setRequestValue(sqlKey);


		ArrayList<Object> resultTest = null;

		if(sqlData.getAllowLike()) 
			resultTest = DA.executeSELLike(true);
		else
			resultTest = DA.executeSEL(true);	

		String test = "{\"" + mkPage + "\":[";
		if(resultTest != null) {
			for(int l = 0; l < resultTest.size(); l++) {
				String damn = resultTest.get(l).toString();
				damn = damn.replaceAll("=", ":");
				MkRestApiData tttt = new MkRestApiData(damn);
				test += damn;

				if(l < resultTest.size() - 1)
					test += ", ";
			}
			test += "]}";
			MkRestApiData tttt = new MkRestApiData(test);

			if(tttt.setJsonObject()) {
				resultObject = tttt.getJsonObject();
			}
		}else {
			//조회 데이터 없음
		}


		return resultObject;
	}

	private JSONObject doTaskDelete(PageJsonData pxData, SqlJsonData sqlData, JSONObject jsonObject, String mkPage) {
		JSONObject resultObject = null;
		MkDbAccessor DA = new MkDbAccessor();

		String service = pxData.getServiceName();
		String control = pxData.getControlName();
		String befQuery = cpi.regularQuery(control, service, true); 
		String query = null;

		query = cpi.setQuery(befQuery);
		DA.setPreparedStatement(query);

		Set keySet = jsonObject.keySet();
		Iterator iter = keySet.iterator();
		ArrayList<String> sqlKey = new ArrayList<String>();
		while(iter.hasNext()) {
			String key = iter.next().toString();
			sqlKey.add(jsonObject.get(key).toString());
		}
		DA.setRequestValue(sqlKey);


		ArrayList<Object> resultTest = null;

		if(sqlData.getAllowLike()) 
			resultTest = DA.executeSELLike(true);
		else
			resultTest = DA.executeSEL(true);	


		String test = "{\"" + mkPage + "\":[";
		if(resultTest != null) {
			for(int l = 0; l < resultTest.size(); l++) {
				String damn = resultTest.get(l).toString();
				damn = damn.replaceAll("=", ":");
				MkRestApiData tttt = new MkRestApiData(damn);
				test += damn;

				if(l < resultTest.size() - 1)
					test += ", ";
			}
			test += "]}";

			MkRestApiData tttt = new MkRestApiData(test);

			if(tttt.setJsonObject()) {
				resultObject = tttt.getJsonObject();
			}
		}else {
			//조회 데이터 없음
		}

		return resultObject;
	}
}
