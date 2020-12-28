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
import java.util.LinkedHashMap;
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
1. ���� ����
2. ��ü ��ȸ
3. ���� ���� ( �ڵ�, ������ Ÿ��, ���� ���� ��. )
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

	private boolean isKeyValid(String key, String mkPage) {
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
			//����
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
		 * ��Ʈ���� ���� �ϴ°�?
		 */
		
		ArrayList<PageJsonData> control = MkRestApiPageConfigs.Me().getControl(mkPage);
		
		if(control == null) {
			/*����*/
			mklogger.error(TAG, "[API] Control " + mkPage + " is not exist.");
			response.sendError(404);
			return;
		}
		Enumeration<String> rpn = request.getParameterNames();
		
		if(!checkMethod(control, REQUEST_METHOD)) {
			mklogger.error(TAG, "[Control " + mkPage + "] does not allow method : " + REQUEST_METHOD);
			response.sendError(404);
			return;
		}
		
		
		if(MKWEB_USE_KEY) {
			String key = request.getParameter(MKWEB_SEARCH_KEY);
			if(!isKeyValid(key, mkPage)) {
				return;
			}
		}
		
		mklogger.debug(TAG, "REQUEST URI : " + requestURI);
		/*
		 * Get���� / �����ϱ�. ������ ������Ʈ�� ��
		 * /mk_api_key/users/name/dev.whoan �̷���
		 * �׸��� Condition�� ����� �Է��� �ִ���
		 */
		if(REQUEST_METHOD.contentEquals("get")) {
			mkJsonData.setData(request.getParameter(MKWEB_API_ID));
			if(mkJsonData.setJsonObject()) {
				requestParameterJson = mkJsonData.getJsonObject();
			} else {
				// /users/u_class/5
				String[] tempURI = requestURI.split("/");
				int shouldCheckQuery = -1;
				int mkPageIndex = -1;
				/*
				 * -1 : do Nothing
				 * 0  : ��ü ��ȸ
				 * 1  : String check 
				 */
				if(tempURI[(tempURI.length-1)].contentEquals(mkPage)) {
					//apiData�� ���ԵǴ°�?
					if(request.getParameterMap().size() == 1) {
						String element = rpn.nextElement();
						String elementValue = null;
						if(element.contentEquals(MKWEB_API_ID)) {
							elementValue = request.getParameter(element);
							if(elementValue.contains("=") && elementValue.contains(MKWEB_SEARCH_KEY)) {
							//	mklogger.debug(TAG, "��� 1�� ����(/mk_api_key/users ó��)");
							}else if(!elementValue.contains(MKWEB_SEARCH_KEY)) {
							//	mklogger.debug(TAG, "(1) ��� 2 - /mk_api_key/users?queryString~");
								shouldCheckQuery = 1;
							}
						}else {
							if(!element.contentEquals(MKWEB_SEARCH_KEY)) {
							//	mklogger.debug(TAG, "(1) ��� 2 - /mk_api_key/users?queryString~");
								shouldCheckQuery = 1;							
							}else {
							//	mklogger.debug(TAG, "��� 1�� ���� (/mk_api_key/users ó��)");
							}
						}
					}else {
						shouldCheckQuery = 1;
					}
					
					shouldCheckQuery = 0;
				}else {
					shouldCheckQuery = -1;
					for(int i = 0; i < tempURI.length; i++) {
						if(tempURI[i].contentEquals(mkPage)) {
							mkPageIndex = i;
							break;
						}
					}	
				}
				
				if(shouldCheckQuery == 1){
					//String ���� Ȯ��
					mklogger.warn(TAG, "Given data is not valid to cast JSONObject.");
					mklogger.warn(TAG, "Try to convert into MkJsonObject...");
					
					String tempAPIID = request.getParameter(MKWEB_API_ID);
					LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
					mklogger.debug(TAG, "APIData�� ���� �ϴ°�? " + request.getParameter(MKWEB_API_ID));
					
					if(tempAPIID != null) {
						//&�� ���� �ϴ°�?
						String[] tempArr = null;
						if(tempAPIID.contains("&")) {
							mklogger.debug(TAG, " & ���� " );
							tempArr = tempAPIID.split("&");
							mklogger.debug(TAG, "tempArr: " + tempArr);
							for(int i = 0; i < tempArr.length; i++) {
								mklogger.debug(TAG, "tempArr i : " + tempArr[i]);
							}
							result = (LinkedHashMap<String, String>) stringToMap(tempArr);
						}else {
							tempArr = new String[1];
							tempArr[0] = tempAPIID;
							result = (LinkedHashMap<String, String>) stringToMap(tempArr);
						}
					}
					
					Enumeration<String> parameters =  request.getParameterNames();
					while(parameters.hasMoreElements()) {
						String parameter = parameters.nextElement();
						if(!parameter.contentEquals(MKWEB_API_ID))
							result.put(parameter, request.getParameter(parameter));
					}
					
					requestParameterJson = mkJsonData.mapToJson(result);
					
					if(requestParameterJson == null) {
						mklogger.error(TAG, "API Request only allow with JSON type. Cannot convert given data into JSON Type.");
						return;
					}
				}else if(shouldCheckQuery == -1) {
					/*
					 1. /users/u_class/5
					 1. /users/name/dev.whoan/
					 
					 2. /users/name/dev.whoan/u_class/5
					 2. /users/u_class/5/name/gildong
					 
					 3. /users/name/dev.whoan/class/5/seq/1 
					 
					 4. /users/u_class/5
					 4. /users/name/dev.whoan/class
					 
					 Ȧ�������� Column
					 ¦�������� ������
					 */
					// mkPageIndex = users�� ã��
					ArrayList<String> searchColumns = new ArrayList<>();
					ArrayList<String> searchValues = new ArrayList<>();
					boolean isColumn = true;
					for(int i = mkPageIndex+1; i < tempURI.length; i++) {
						if(isColumn) {
							isColumn = false;
							searchColumns.add(tempURI[i]);
						}else {
							isColumn = true;
							searchValues.add(tempURI[i]);
						}
					}
					
					if(searchColumns.size() == searchValues.size() + 1) {
						searchValues.add("");
					}
					
					if(searchColumns.size() != searchValues.size()) {
						mklogger.error(TAG, "API Request is not valid.");
						mklogger.debug(TAG, "searchColumns size != searchValues.size");
						return;
					}
					LinkedHashMap<String, String> result = new LinkedHashMap<>();
					
					for(int i = 0; i < searchColumns.size(); i++) {
						result.put(searchColumns.get(i), searchValues.get(i));
					}
					requestParameterJson = mkJsonData.mapToJson(result);
					
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
				mklogger.debug(TAG, "Ȯ��: " + requestParameterJson.toString());
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
		mklogger.debug(TAG, "���� : " + requestParameterJson);
		Set requestKeySet = requestParameterJson.keySet();
		Iterator requestIterator = requestKeySet.iterator();
	
		/*
		 * �������� �� ��
		 * ApiPageConfig���� Method�� ���� Value Ȯ���ϱ�
		 * ����ϸ� SQL Condition ���ϱ�
		 */
		
		ArrayList<PageJsonData> pageControl = MkRestApiPageConfigs.Me().getControl(mkPage);
		PageJsonData pageService = null;
		
		for(PageJsonData service : pageControl) {
	//		mklogger.debug(TAG, " service method : " + service.getMethod());
			if(REQUEST_METHOD.contentEquals(service.getMethod())) {
				pageService = service;
				break;
			}
		}
		
		if(pageService == null) {
			mklogger.error(TAG, "��û�� ���񽺴� �������� �ʽ��ϴ�. (Method�� ���� Service �� null��)");
			return;
		}
		
		ArrayList<SqlJsonData> sqlControl = MkRestApiSqlConfigs.Me().getControlByServiceName(pageService.getServiceName());
		SqlJsonData sqlService = null;
		String[] sqlConditions = sqlControl.get(0).getCondition();
		
		if(sqlConditions.length == 0) {
			mklogger.error(TAG, "SQL Config ������ �߸��ƽ��ϴ�. condition�� ����ֽ��ϴ�. ��ü��ȸ�� ����� ��� \"1\":\"\"�� �߰��� �ּ���.");
			return;
		}
		
		for(SqlJsonData sqlServiceData : sqlControl) {
			if(sqlServiceData.getServiceName().contentEquals(pageService.getServiceName())) {
				sqlService = sqlServiceData;
				break;
			}
		}
		
		if(sqlService == null) {
			mklogger.error("��û�� SQL Service�� �����ϴ�.");
			return;
		}
		
		String[] pageValues = pageService.getData();
		
		requestIterator = requestKeySet.iterator();
		
		while(requestIterator.hasNext()) {
			String requestKey = requestIterator.next().toString();
			int passed = -1;
			for(int i = 0; i < pageValues.length; i++) {
				if(pageValues[i].contentEquals(requestKey)) {
					passed = 1;
					break;
				}
			}
			
			if(passed == 1) {
				for(int i = 0; i < sqlConditions.length; i++) {
					if(sqlConditions[i].contentEquals(requestKey)) {
						passed = 2;
						break;
					}
				}
			}
			
			if(passed != 2) {
				mklogger.error(TAG, "��û�� Value�� ���� �˻��� �Ұ����մϴ�. " + requestKey);
				return;
			}
		}
		JSONObject resultObject = null;
		switch(REQUEST_METHOD) {
		case "get":
			resultObject = doTaskGet(pageService, sqlService, requestParameterJson, mkPage);
			break;
		case "post":
			
			break;
			
		case "put":
			
			break;
			
		case "delete":
			
			break;
		}
		mklogger.debug(TAG, "resultObejct : " + resultObject.toString());
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
				//��ȸ
				resultObject = doTaskGet(pxData, sqlData, jsonObject, mkPage);
			}else if(method.contentEquals("post")) {
				//����, ����
				resultObject = doTaskPost(pxData, sqlData, jsonObject, mkPage);
			}else if(method.contentEquals("put")) {
				//��ȸ, ����
				resultObject = doTaskPut(pxData, sqlData, jsonObject, mkPage);
			}else if(method.contentEquals("delete")) {
				//����
				resultObject = doTaskDelete(pxData, sqlData, jsonObject, mkPage);
			}
			Hash = pxData.getControlName() + sqlData.getServiceName() + method;
			mrap = new MkRestApiResponse(resultObject, lmrap.size()+1, 1, Hash);
		}

		//500 ����. �������� �ʴ� ����̸� 500���� �Ѿ�� ����
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

		//��������(���� ����, ����, ���� ���� ���)
		response.setStatus(200);
		response.setContentType("application/json;charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();


		out.print("�׽�Ʈ\n");
		out.flush();
	}

	private JSONObject doTaskGet(PageJsonData pjData, SqlJsonData sqlData, JSONObject jsonObject, String mkPage) {
		JSONObject resultObject = null;
		MkDbAccessor DA = new MkDbAccessor();

		String service = pjData.getServiceName();
		String control = sqlData.getControlName();
		String befQuery = cpi.regularQuery(control, service, true);
		String query = null;
		
		//�ƿ� ��ü��ȸ�� Ư���� String�� �ʿ��� jsonObject { "Ư����Ű":"true" }
		//�ؿ��� ������ȸ
		//value�� ""�� �ش������� ��ü��ȸ. �� value�� �����ص� ��
		query = cpi.setApiQuery(befQuery).split("WHERE")[0];

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
			//��ȸ ������ ����
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
			//��ȸ ������ ����
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
			//��ȸ ������ ����
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
			//��ȸ ������ ����
		}

		return resultObject;
	}
}
