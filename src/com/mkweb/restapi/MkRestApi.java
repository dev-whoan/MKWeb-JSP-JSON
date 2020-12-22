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

	private boolean checkMethod(HttpServletRequest request, String rqMethod, String mkPage) {

		ArrayList<PageJsonData> apiPageInfo = null;
		if(MkRestApiPageConfigs.Me().getControl(mkPage) != null) {
			apiPageInfo = MkRestApiPageConfigs.Me().getControl(mkPage);
		}

		if(apiPageInfo == null) {
			mklogger.error(TAG, " api page info null");
			return false; 
		}

		if(!apiPageInfo.get(0).isMethodAllowed(rqMethod)) { 
			mklogger.error(TAG, " The request method is not allowed : " + rqMethod);
			return false;
		}

		PageJsonData pageInfo = null;
		for(int i = 0; i < apiPageInfo.size(); i++) {
			if(apiPageInfo.get(i).getSql()[2].equals(rqMethod)) {
				pageInfo = apiPageInfo.get(i);
				break;
			}
		}

		if(pageInfo == null) {
			mklogger.error(TAG, " No Service is allowed for request method : " + rqMethod);
			return false;
		}
		return true;	
	}

	private boolean isKeyValid(String key, String mkPage) {
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

		final String MKWEB_URL_PATTERN = MkConfigReader.Me().get("mkweb.restapi.urlpattern");
		final String MKWEB_API_ID = MkConfigReader.Me().get("mkweb.restapi.request.id");
		mklogger.debug(TAG," 이거 찾아야해 " + MKWEB_API_ID);
		final String MKWEB_SEARCH_KEY = MkConfigReader.Me().get("mkweb.restapi.searchkey.exp");
		String reqApiData = null;

		String requestURI = request.getRequestURI();
		String[] reqPage = null;
		String mkPage = null;

		boolean requireKey = MkConfigReader.Me().get("mkweb.restapi.usekey").contentEquals("yes") ? true : false;
		String searchKey = null;

		reqPage = requestURI.split("/" + MKWEB_URL_PATTERN + "/");

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

		mklogger.debug(TAG, "mkPage : " + mkPage);

		if(request.getAttribute("api-method").toString().contentEquals("get")) {
			//
			reqApiData = "{";
			
			int size = requestURI.split("/").length;
			mklogger.debug(TAG, "URI : " + requestURI + " || size : " + size);

			if(size == 3) {
				//쿼리스트링
				Enumeration params = request.getParameterNames();
				String requestParams = null;
				int i = 0;
				mklogger.debug(TAG, "start reqApiData:" + reqApiData);
				while(params.hasMoreElements()) {
					if(i++ != 0)
						reqApiData += ", ";
					
					String key = params.nextElement().toString().trim();
					mklogger.debug(TAG, "key: " + key);
					String value = request.getParameter(key);
					mklogger.debug(TAG, "value : " + value);
					if(key.contentEquals(MKWEB_API_ID)) {
						reqApiData += "\"" + value + "\"";
					}else {
						reqApiData += "\"" + key + "\":" + "\"" + value + "\"";
					}
					
				}
				// i 가 0이면 전체조회
				/*
				if(i == 1) {
					mklogger.debug(TAG, " you also get here" );
					if(requireKey) {
						String key = request.getParameter(MKWEB_SEARCH_KEY);
						String value = request.getParameter(key);
						mklogger.debug(TAG, " also key: " + key + ", value : " + value );
						reqApiData += "\"" + key + "\":" + "\"" + value + "\"";
						mklogger.debug(TAG, "also reqApiData: " + reqApiData);
					}else {
						reqApiData = "";
					}
				}
				*/
			}else if(size > 3) {
				// `/~/~/~`
				String[] urlPattern = requestURI.split(MKWEB_URL_PATTERN);
				searchKey = request.getParameter(MKWEB_SEARCH_KEY);
				if(urlPattern != null && urlPattern.length == 2) {
					String reqApiURI = urlPattern[1];
					String[] reqApiArray = reqApiURI.split(mkPage);
					if(reqApiArray != null && reqApiArray.length == 2) {
						String[] reqApiRequestData = reqApiArray[1].split("/");;

						for(int i = 1; i < reqApiRequestData.length; i++) {
							if(i % 2 == 1) {
								reqApiData += "\"" + reqApiRequestData[i] + "\"" + ":";
							}else {
								reqApiData += "\"" + reqApiRequestData[i] + "\"";

								if(i < reqApiRequestData.length -1) {
									reqApiData += ", ";
								}
							}
						}
					}else{
						mklogger.debug(TAG, "요청 URI에 mkPage가 없거나 reqApiArray 사이즈가 2가 아님");
					}
				}else {
					mklogger.debug(TAG, "urlPattern 배열 확인 필요");
				}

			}else {
				// 예외
			}
			reqApiData += "}";
		}else{
			reqApiData = request.getParameter(MKWEB_API_ID);
		}

		String reqToJson = null;
		boolean isDataRequestedAsJsonObject = true;
		MkRestApiData mkJsonObject = new MkRestApiData();
		JSONObject jsonObject = null;

		if(reqApiData != null) {
			mkJsonObject.setData(reqApiData);

			if(!mkJsonObject.setJsonObject()) {
				mklogger.error(TAG, " Failed to create JsonObject.");
				isDataRequestedAsJsonObject = false;
			}

			if(!isDataRequestedAsJsonObject) {
				reqToJson = mkJsonObject.stringToJsonString(reqApiData);
				mkJsonObject.setData(reqToJson);
				if(mkJsonObject.setJsonObject()) {
					jsonObject = mkJsonObject.getJsonObject();
				}else {
					mklogger.error(TAG, " Failed to create MkJsonObject. :: " + reqToJson);
				}

			}else {
				jsonObject = mkJsonObject.getJsonObject();
			}
		}

		if(jsonObject == null) {
			StringBuilder stringBuilder = new StringBuilder();

			BufferedReader br = null;
			try {
				InputStream inputStream = request.getInputStream();
				br = new BufferedReader(new InputStreamReader(inputStream));
				char[] charBuffer = new char[128];
				int bytesRead = -1;
				while((bytesRead = br.read(charBuffer)) > 0 ) {
					stringBuilder.append(charBuffer, 0, bytesRead);
				}
			}catch (IOException ex) {
				throw ex;
			}finally {
				if(br != null) {
					try {
						br.close();
					}catch(IOException ex) {
						throw ex;
					}
				}
			}
			boolean stringPass = false;
			if(stringBuilder.length() == 0) {
				stringPass = true;
			}
			if(!stringPass) {
				String rbPostData = stringBuilder.toString();

				String rbpds[] = rbPostData.split("&");

				for(int i = 0; i < rbpds.length; i++) {
					if(rbpds[i].contains(MKWEB_API_ID)) {
						reqApiData = URLDecoder.decode(rbpds[i].split("=")[1], "UTF-8");
						break;
					}
				}

				mkJsonObject.setData(reqApiData);
				if(!mkJsonObject.setJsonObject()) {
					mklogger.error(TAG, " Failed to create JsonObject.");
					isDataRequestedAsJsonObject = false;
				}

				if(!isDataRequestedAsJsonObject) {
					reqToJson = mkJsonObject.stringToJsonString(reqApiData);
					mkJsonObject.setData(reqToJson);
					if(mkJsonObject.setJsonObject()) {
						jsonObject = mkJsonObject.getJsonObject();
					}else {
						mklogger.error(TAG, " Failed to create MkJsonObject. :: " + reqToJson);
						return;
					}
				}else {
					jsonObject = mkJsonObject.getJsonObject();
				}
			}
		}
		
		if(requireKey && searchKey == null) {
			if(jsonObject != null) {
				searchKey = jsonObject.get(MKWEB_SEARCH_KEY).toString();
			}else {
				//예외
				mklogger.debug(TAG, "401 here 1");
				response.sendError(401);
				return;
			}

			if(searchKey == null) {
				//예외
				mklogger.debug(TAG, "401 here 2");
				response.sendError(401);
				return;
			}
		}
/*
 * func isValidDataForJson) ParseException:: Unexpected token RIGHT BRACE(}) at position 8. Given data is not valid for JSONObject.
{"name":}
/users/name
/users/u_class
{"name":"dev.whoan","u_class":}
/users/name/dev.whoan/u_class
jsonObject null
 */
		Object mAttributes = request.getAttribute("api-method");
		String method = null;

		//페이지 유효성 검사
		//String requestURI = request.getRequestURI();

		if(mAttributes != null)
			method = mAttributes.toString().toLowerCase();

		if(method == null) {
			//예외
			return;
		}
		String[] noUrlPattern = new String[reqPage.length-1];
		for(int i = 1; i < reqPage.length; i++) {
			noUrlPattern[i-1] = reqPage[i];
		}
		if(!cpi.isValidApiPageConnection(mkPage, noUrlPattern)) {
			//예외
			mklogger.debug(TAG, "401 here 4");
			response.sendError(401);
			return;
		}
		if(requireKey && !isKeyValid(searchKey, mkPage)) {
			//예외
			mklogger.debug(TAG, "401 here 5");
			response.sendError(401);
			return;
		}
		if(!checkMethod(request, method, mkPage)) {
			//예외
			response.sendError(400);
			return;
		}
		if(!method.contentEquals("get")) {
			if(!request.getHeader("Content-Type").toLowerCase().contains("application/json")) {
				//예외
				response.sendError(415);
				return;
			}
		}
		//리턴은 무조건 json이다.
		//ApiSql에서 Allow_Single 확인
		ArrayList<PageJsonData> apiPageInfo = MkRestApiPageConfigs.Me().getControl(mkPage);

		if(apiPageInfo == null) {
			mklogger.error(TAG, " api page info null");
			return; 
		}
		int target = -1;
		for(int i = 0; i < apiPageInfo.size(); i++) {

			if(apiPageInfo.get(i).getSql()[2].contentEquals(method)) {
				target = i;
				mklogger.debug(TAG, "target : " + target);
				break;
			}
		}
		if(target == -1) {
			//예외
			return;
		}
		if(method.contentEquals("options")) {

		}else if(method.contentEquals("head")) {

		}

		PageJsonData pxData = apiPageInfo.get(target);
		
		SqlJsonData sqlData = MkRestApiSqlConfigs.Me().getControlService(pxData.getServiceName());

		if(sqlData == null) {
			//예외
			mklogger.debug(TAG, "TnSQDn: pxData.getControlName(): " + pxData.getControlName());
			mklogger.error(TAG, "There is no SQL Data named : " + pxData.getServiceName());
			return;
		}

		//
		//		if(!cpi.comparePageValueWithRequest(pxData.getData(), sqlKey, pxData.getPageStaticParams(), true)) {
		//			//예외
		//			mklogger.error(TAG, " Request Value is not authorized. Please check page config.");
		//			return;
		//		}
		//여기까지는 모든 메서드 중복되는 행위
		//조회? 생성? 삭제?

		ArrayList<MkRestApiResponse> lmrap = null;
		MkRestApiResponse mrap = null;
		Object mro = request.getAttribute("mrap");
		Object mraHash = request.getAttribute("mraHas");
		String Hash = null;
		boolean isDone = false;
		if(mraHash != null) {
			Hash = mraHash.toString();
		}
		JSONObject resultObject = null;

		if(mro != null) {
			lmrap = (ArrayList<MkRestApiResponse>) mro;
			for(MkRestApiResponse ar : lmrap) {
				if(Hash.contentEquals(ar.getHashData())) {
					if(!ar.needUpdate()) {
						mrap = ar;
						resultObject = ar.getData();
						isDone = true;
						break;
					}
				}
			}
		} else {
			lmrap = new ArrayList<>();
		}

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
		//Create ResponseData with resultObject.
		MkRestApiData mkApiDData = new MkRestApiData();
		request.setAttribute("mrap", lmrap);
		request.setAttribute("mraHash", Hash);
		//리스폰스(최초 응답, 이전, 다음 응답 기록)
		response.setStatus(200);
		response.setContentType("application/json;charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();

		String roString = resultObject.toJSONString();
		String result = "";
		int size = roString.length();
		int tSize = -1;
		int tabCount = 0;
		boolean shouldTab = false;
		while(++tSize < size) {
			result += roString.charAt(tSize);
			switch(roString.charAt(tSize)) {
			case '{':
				tabCount++;
				result += "\n";
				shouldTab = true;
				break;
			case '}':
			{
				tabCount--;
				char rChar = result.charAt(result.length()-2);
				switch(rChar) {
				case '\"':
				{
					result = result.substring(0, result.length()-2) + "\"\n";
					for(int i = 0; i < tabCount; i++) {	result += "\t";	}
					result += "}";
					break;
				}
				}
				result += "\n";
				shouldTab = true;
				break;
			}
			case ']':
				result = result.substring(0, result.length()-3) + "]\n";
				shouldTab = true;
				break;
			case ',':
				result += "\n";
				shouldTab = true;
				break;
			}
			if(shouldTab) {
				for(int i = 0; i < tabCount; i++) {	result += "\t";	}
				shouldTab = false;
			}
		}

		result = result.substring(0, result.length()-3) + "}";

		out.print("테스트\n");
		out.print(result);
		out.print("\n기본\n");
		out.print(resultObject.toJSONString());		
		out.flush();
	}

	private JSONObject doTaskGet(PageJsonData pxData, SqlJsonData sqlData, JSONObject jsonObject, String mkPage) {
		JSONObject resultObject = null;
		MkDbAccessor DA = new MkDbAccessor();

		String service = pxData.getServiceName();
		String befQuery = cpi.regularQuery(service, true); 
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
		String befQuery = cpi.regularQuery(service, true); 
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

		String service = pxData.getServiceName().split("\\.")[1];

		String befQuery = cpi.regularQuery(service, true); 
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

		String service = pxData.getServiceName().split("\\.")[1];

		String befQuery = cpi.regularQuery(service, true); 
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
