package com.mkweb.restapi;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
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
 * 1. ���� ���� 2. ��ü ��ȸ 3. ���� ���� ( �ڵ�, ������ Ÿ��, ���� ���� ��. )
 */

public class MkRestApi extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private MkLogger mklogger = MkLogger.Me();
	private String TAG = "[MkRestApi]";
	private CheckPageInfo cpi = null;
	private String[] methods = { "post", "get", "put", "delete", "options", "head" };

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public MkRestApi() {
		super();
		cpi = new CheckPageInfo();
	}

	private boolean checkMethod(ArrayList<PageJsonData> pageJsonData, String requestMethod) {
		for (PageJsonData pjd : pageJsonData) {
			if (pjd.getMethod().toString().toLowerCase().contentEquals(requestMethod)) {
				return true;
			}
		}

		return false;
	}

	private boolean isKeyValid(String key, String mkPage) {
		boolean isDone = false;
		MkRestApiGetKey mra = new MkRestApiGetKey();
		ArrayList<Object> apiKeyList = mra.GetKey();

		mklogger.temp(TAG, " REST Api Key has searched : " + key + " Result: ", false);

		if (apiKeyList != null) {
			for (int i = 0; i < apiKeyList.size(); i++) {
				HashMap<String, Object> result = new HashMap<String, Object>();
				result = (HashMap<String, Object>) apiKeyList.get(i);
				if (result.get("api_key").equals(key)) {
					mklogger.temp(TAG, " key is valid! (user_id : " + result.get("user_id") + ")", false);
					mklogger.flush("info");
					isDone = true;
					break;
				}
			}
		} else {
			mklogger.temp(TAG, " Failed to search the key! (No Key List)", false);
			mklogger.flush("warn");
		}

		if (!isDone) {
			mklogger.temp(TAG, " Failed to search the key! (Key is invalid)", false);
			mklogger.flush("warn");
		}

		return isDone;
	}

	private Map<String, String> stringToMap(String[] strArray) {
		Map<String, String> result = new HashMap<String, String>();
		for (int i = 0; i < strArray.length; i++) {
			String tempParameter = strArray[i].split("\\=")[0];
			String tempValue = strArray[i].split("\\=")[1];
			result.put(tempParameter, tempValue);
		}

		return result;
	}

	protected void doHead(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		request.setAttribute("api-method", "head");
		doTask(request, response);
	}

	protected void doOptions(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		request.setAttribute("api-method", "options");
		doTask(request, response);
	}

	protected void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		request.setAttribute("api-method", "put");
		doTask(request, response);
	}
	
	protected void doDelete(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		request.setAttribute("api-method", "delete");
		doTask(request, response);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		request.setAttribute("api-method", "get");
		doTask(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		request.setAttribute("api-method", "post");
		doTask(request, response);
	}

	private void doTask(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		if (!MkConfigReader.Me().get("mkweb.restapi.use").equals("yes")) {
			response.sendError(404);
			return;
		}
		
		final String MKWEB_URI_PATTERN = MkConfigReader.Me().get("mkweb.restapi.uripattern");
		final String MKWEB_API_ID = MkConfigReader.Me().get("mkweb.restapi.request.id");
		final String MKWEB_SEARCH_KEY = MkConfigReader.Me().get("mkweb.restapi.searchkey.exp");
		final boolean MKWEB_USE_KEY = MkConfigReader.Me().get("mkweb.restapi.usekey").contentEquals("yes") ? true
				: false;
		final String REQUEST_METHOD = request.getAttribute("api-method").toString().toLowerCase();
		final String MKWEB_SEARCH_ALL = MkConfigReader.Me().get("mkweb.restapi.search.all");

		String requestURI = request.getRequestURI();
		String[] reqPage = null;
		String mkPage = null;
		String userKey = null;
		JSONObject requestParameterJson = null;
		MkJsonData mkJsonData = new MkJsonData();
		
		MkRestApiResponse apiResponse = new MkRestApiResponse();
		JSONObject resultObject = null;
		apiResponse.setCode(200);
		
		while(true) {
			reqPage = requestURI.split("/" + MKWEB_URI_PATTERN + "/");
			
			if (reqPage.length < 2) {
				// ����
				apiResponse.setMessage("Please enter the conditions to search.");
				apiResponse.setCode(401);
				break;
			}
		
			mkPage = reqPage[1];

			if (mkPage.contains("/")) {
				mkPage = mkPage.split("/")[0];
			}

			mklogger.debug(TAG, "Request MKPage : " + mkPage + "|Method : " + REQUEST_METHOD);

			ArrayList<PageJsonData> control = MkRestApiPageConfigs.Me().getControl(mkPage);

			if (control == null) {
				mklogger.error(TAG, "[API] Control " + mkPage + " is not exist.");
				apiResponse.setMessage("There is no api named : " + mkPage);
				apiResponse.setCode(404);
				break;
			}

			Enumeration<String> rpn = request.getParameterNames();

			if (!checkMethod(control, REQUEST_METHOD)) {
				mklogger.error(TAG, "[Control " + mkPage + "] does not allow method : " + REQUEST_METHOD);
				apiResponse.setMessage("The method you requested is not allowed.");
				apiResponse.setCode(404);
				break;
			}

			if (REQUEST_METHOD.contentEquals("get")) {
				mkJsonData.setData(request.getParameter(MKWEB_API_ID));
				if (mkJsonData.setJsonObject()) {
					requestParameterJson = mkJsonData.getJsonObject();
					userKey = requestParameterJson.get(MKWEB_SEARCH_KEY).toString();
				} else {
					String[] tempURI = requestURI.split("/");
					int shouldCheckQuery = -1;
					int mkPageIndex = -1;
					/*
					 * -1 : do Nothing 0 : ��ü ��ȸ 1 : String check
					 */
					if (tempURI[(tempURI.length - 1)].contentEquals(mkPage)) {
						if (request.getParameterMap().size() == 1) {
							String element = rpn.nextElement();
							String elementValue = null;
							if (element.contentEquals(MKWEB_API_ID)) {
								elementValue = request.getParameter(element);
								if (elementValue.contains("=") && elementValue.contains(MKWEB_SEARCH_KEY)) {
									// mklogger.debug(TAG, "��� 1�� ����(/mk_api_key/users ó��) 1 ");
									shouldCheckQuery = 0;
								} else if (!elementValue.contains(MKWEB_SEARCH_KEY)) {
									// mklogger.debug(TAG, "(1) ��� 2 - /mk_api_key/users?queryString~");
									shouldCheckQuery = 1;
								}
							} else {
								if (!element.contentEquals(MKWEB_SEARCH_KEY)) {
									// mklogger.debug(TAG, "(1) ��� 2 - /mk_api_key/users?queryString~");
									shouldCheckQuery = 1;
								} else {
									// mklogger.debug(TAG, "��� 1�� ���� (/mk_api_key/users ó��) 2 ");
									shouldCheckQuery = 0;
								}
							}
						} else {
							shouldCheckQuery = 1;
						}
					} else {
						shouldCheckQuery = -1;
						for (int i = 0; i < tempURI.length; i++) {
							if (tempURI[i].contentEquals(mkPage)) {
								mkPageIndex = i;
								break;
							}
						}
					}

					if (shouldCheckQuery == 1) {
						mklogger.warn(TAG, "Given data is not valid to cast JSONObject.");
						mklogger.warn(TAG, "Try to convert into MkJsonObject...");

						String tempAPIID = request.getParameter(MKWEB_API_ID);
						LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
						mklogger.debug(TAG, "APIData�� ���� �ϴ°�? " + request.getParameter(MKWEB_API_ID));

						if (tempAPIID != null) {
							String[] tempArr = null;
							if (tempAPIID.contains("&")) {
								mklogger.debug(TAG, " & ���� ");
								tempArr = tempAPIID.split("&");
								mklogger.debug(TAG, "tempArr: " + tempArr);
								for (int i = 0; i < tempArr.length; i++) {
									mklogger.debug(TAG, "tempArr i : " + tempArr[i]);
								}
								result = (LinkedHashMap<String, String>) stringToMap(tempArr);
							} else {
								tempArr = new String[1];
								tempArr[0] = tempAPIID;
								result = (LinkedHashMap<String, String>) stringToMap(tempArr);
							}
						}

						Enumeration<String> parameters = request.getParameterNames();
						while (parameters.hasMoreElements()) {
							String parameter = parameters.nextElement();
							if (!parameter.contentEquals(MKWEB_API_ID))
								result.put(parameter, request.getParameter(parameter));
						}

						requestParameterJson = mkJsonData.mapToJson(result);

						if (requestParameterJson == null) {
							mklogger.error(TAG,
									"API Request only allow with JSON type. Cannot convert given data into JSON Type.");
							apiResponse.setMessage("Please check your request. The entered data cannot be converted into JSON Type.");
							apiResponse.setCode(404);
							break;
						}
					} else if (shouldCheckQuery == -1) {
						/*
						 * 1. /users/u_class/5 1. /users/name/dev.whoan/
						 * 
						 * 2. /users/name/dev.whoan/u_class/5 2. /users/u_class/5/name/gildong
						 * 
						 * 3. /users/name/dev.whoan/class/5/seq/1
						 * 
						 * 4. /users/u_class/5 4. /users/name/dev.whoan/class
						 * 
						 * Ȧ�������� Column ¦�������� ������
						 */
						// mkPageIndex = users�� ã��
						ArrayList<String> searchColumns = new ArrayList<>();
						ArrayList<String> searchValues = new ArrayList<>();
						boolean isColumn = true;
						for (int i = mkPageIndex + 1; i < tempURI.length; i++) {
							if (isColumn) {
								isColumn = false;
								searchColumns.add(tempURI[i]);
							} else {
								isColumn = true;
								searchValues.add(tempURI[i]);
							}
						}

						if (searchColumns.size() == searchValues.size() + 1) {
							searchValues.add(MKWEB_SEARCH_ALL);
						}

						if (searchColumns.size() != searchValues.size()) {
							mklogger.error(TAG, "API Request is not valid.");
							mklogger.debug(TAG, "searchColumns size != searchValues.size");
							apiResponse.setMessage("�κ� ���ǿ� ���� ��ü�˻��� ������ �� �����ϴ�.(���� ���ߵ��� ���� ���)");
							apiResponse.setCode(400);
							break;
						}
						LinkedHashMap<String, String> result = new LinkedHashMap<>();

						for (int i = 0; i < searchColumns.size(); i++) {
							result.put(searchColumns.get(i), searchValues.get(i));
						}
						requestParameterJson = mkJsonData.mapToJson(result);

						if (requestParameterJson == null) {
							mklogger.error(TAG,
									"API Request only allow with JSON type. Cannot convert given data into JSON Type.");
							apiResponse.setMessage("Please check your request. The entered data cannot be converted into JSON Type.");
							apiResponse.setCode(400);
							break;
						}
					} else if (shouldCheckQuery == 0) {
						LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
						result.put(MKWEB_SEARCH_ALL, MKWEB_SEARCH_ALL);
						requestParameterJson = mkJsonData.mapToJson(result);
						
						if(requestParameterJson == null) {
							mklogger.error(TAG,
									"API Request only allow with JSON type. Cannot convert given data into JSON Type.");
							apiResponse.setMessage("Please check your request. The entered data cannot be converted into JSON Type.");
							apiResponse.setCode(400);
						}
					}
				}
			} else {
				mkJsonData.setData(request.getParameter(MKWEB_API_ID));
				if (mkJsonData.setJsonObject()) {
					requestParameterJson = mkJsonData.getJsonObject();
				} else {
					String tempJsonString = mkJsonData.stringToJsonString(mkJsonData.getData());

					mklogger.debug(TAG, "stringToJsonString : " + tempJsonString);

					mkJsonData.setData(tempJsonString);

					if (!mkJsonData.setJsonObject()) {
						mklogger.error(TAG,
								"API Request only allow with JSON type. Cannot convert given data into JSON Type.");
						apiResponse.setMessage("Please check your request. The entered data cannot be converted into JSON Type.");
						apiResponse.setCode(400);
						break;
					}

					requestParameterJson = mkJsonData.getJsonObject();
				}
			}

			if(userKey == null) {
				if(requestParameterJson.get(MKWEB_SEARCH_KEY) != null) {
					userKey = requestParameterJson.get(MKWEB_SEARCH_KEY).toString();
				}else {
					userKey = request.getParameter(MKWEB_SEARCH_KEY);
				}
			}
			
			if (MKWEB_USE_KEY) {
				if (!isKeyValid(userKey, mkPage)) {
					apiResponse.setMessage("The key is not valid.");
					apiResponse.setCode(401);
					break;
				}
				
				requestParameterJson.remove(MKWEB_SEARCH_KEY);
			}

			Set requestKeySet = requestParameterJson.keySet();
			Iterator requestIterator = requestKeySet.iterator();

			ArrayList<PageJsonData> pageControl = MkRestApiPageConfigs.Me().getControl(mkPage);
			PageJsonData pageService = null;

			for (PageJsonData service : pageControl) {
				// mklogger.debug(TAG, " service method : " + service.getMethod());
				if (REQUEST_METHOD.contentEquals(service.getMethod())) {
					pageService = service;
					break;
				}
			}

			if (pageService == null) {
				mklogger.error(TAG, "��û�� ���񽺴� �������� �ʽ��ϴ�. (Method�� ���� Service �� null��)");
				apiResponse.setMessage("The method you requested is not allowed.");
				apiResponse.setCode(405);
				break;
			}

			ArrayList<SqlJsonData> sqlControl = MkRestApiSqlConfigs.Me()
					.getControlByServiceName(pageService.getServiceName());
			SqlJsonData sqlService = null;
			String[] sqlConditions = sqlControl.get(0).getCondition();

			if (sqlConditions.length == 0) {
				mklogger.error(TAG, "SQL Config ������ �߸��ƽ��ϴ�. condition�� ����ֽ��ϴ�. ��ü��ȸ�� ����� ��� \"1\":\"*\"�� �߰��� �ּ���.");
				apiResponse.setMessage("SERVER ERROR. Please contact admin.");
				apiResponse.setCode(500);
				break;
			}

			for (SqlJsonData sqlServiceData : sqlControl) {
				if (sqlServiceData.getServiceName().contentEquals(pageService.getServiceName())) {
					sqlService = sqlServiceData;
					break;
				}
			}

			if (sqlService == null) {
				mklogger.error("��û�� SQL Service�� �����ϴ�.");
				apiResponse.setMessage("The method you requested is not allowed.");
				apiResponse.setCode(405);
				break;
			}

			String[] pageValues = pageService.getData();

			requestIterator = requestKeySet.iterator();

			while (requestIterator.hasNext()) {
				String requestKey = requestIterator.next().toString();
				int passed = -1;
				for (int i = 0; i < pageValues.length; i++) {
					if (pageValues[i].contentEquals(requestKey) || requestKey.contentEquals(MKWEB_SEARCH_ALL)) {
						passed = 1;
						break;
					}
				}

				if (passed == 1) {
					for (int i = 0; i < sqlConditions.length; i++) {
						if (sqlConditions[i].contentEquals(requestKey) || requestKey.contentEquals(MKWEB_SEARCH_ALL)) {
							passed = 2;
							break;
						}
					}
				}

				if (passed != 2) {
					mklogger.error(TAG, "��û�� Value�� ���� �˻��� �Ұ����մϴ�. " + requestKey);
					apiResponse.setMessage("The column you entered is not allowed. (" + requestKey + ")");
					apiResponse.setCode(400);
					break;
				}
			}
			
			switch (REQUEST_METHOD) {
			case "get":
				resultObject = doTaskGet(pageService, sqlService, requestParameterJson, mkPage, MKWEB_SEARCH_ALL, apiResponse);
				break;
			case "post":
				mklogger.debug(TAG, "DO TASK POST");
				resultObject = doTaskPost(pageService, sqlService, requestParameterJson, mkPage, apiResponse);
				break;

			case "put":

				break;

			case "delete":

				break;
			}
			
			break;
		}

		response.setStatus(apiResponse.getCode());
		response.setContentType("application/json;charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();

		String result = mkJsonData.jsonToPretty(resultObject);
		if(resultObject == null) {
			result = "{" +
								"\"response\":\"HTTP 1.1 " + apiResponse.getCode() + " " + apiResponse.getStatus() + "\"," +
								"\"error\":{" +
									"\"message\":\"" + apiResponse.getMessage() + "\"," +
									"\"code\":\"" + apiResponse.getCode() + "\"," +
									"\"status\":\"" + apiResponse.getStatus() + "\"," +
									"\"info\":\"" + apiResponse.getDocs() + "\"" +
								"}" +
							"}";
			mkJsonData.setData(result);
			mkJsonData.setJsonObject();
			JSONObject error = mkJsonData.getJsonObject();
			result = mkJsonData.jsonToPretty(error);
			
			out.print(result);
		}else {
			result = result.substring(1, result.length()-1);
			String okTop = "{\"response\":\"HTTP 1.1 " + apiResponse.getCode() + " " + apiResponse.getStatus() +"\"," + result + "}";
			mkJsonData.setData(okTop);
			mkJsonData.setJsonObject();
			JSONObject ok = mkJsonData.getJsonObject();
			okTop = mkJsonData.jsonToPretty(ok);
			out.print(okTop);
		}
		out.flush();
	}

	private JSONObject doTaskGet(PageJsonData pjData, SqlJsonData sqlData, JSONObject jsonObject, String mkPage,
			String MKWEB_SEARCH_ALL, MkRestApiResponse mkResponse) {
		JSONObject resultObject = null;
		MkDbAccessor DA = new MkDbAccessor();

		String service = pjData.getServiceName();
		String control = sqlData.getControlName();
		String befQuery = cpi.regularQuery(control, service, true);
		String query = null;

		int requestSize = jsonObject.size();
		boolean searchAll = false;
		// �ƿ� ��ü��ȸ�� Ư���� String�� �ʿ��� jsonObject { "Ư����Ű":"true" }
		// �ؿ��� ������ȸ
		// value�� ""�� �ش������� ��ü��ȸ. �� value�� �����ص� ��
		query = cpi.setApiQuery(befQuery).split("WHERE")[0];

		Set<String> keySet = jsonObject.keySet();
		Iterator iter = keySet.iterator();
		ArrayList<String> sqlKey = new ArrayList<String>();
		String condition = "WHERE ";
		int i = 0;

		while (iter.hasNext()) {
			String key = iter.next().toString();
			if (requestSize == 1) {
				if (key.contentEquals(MKWEB_SEARCH_ALL)) {
					searchAll = true;
					continue;
				}
			}
			condition += key + " = ?";
			sqlKey.add(jsonObject.get(key).toString());
			if (i < requestSize - 1) {
				condition += " AND ";
			}

			i++;
		}
		if (condition.contains("?")) {
			query += condition;
		}
		DA.setPreparedStatement(query);

		if (!searchAll) {
			DA.setApiRequestValue(sqlKey);
		}

		ArrayList<Object> resultList = null;

		if (sqlData.getAllowLike()) {
			try {
				resultList = DA.executeSELLike(true);
			} catch (SQLException e) {
				mkResponse.setCode(400);
				mkResponse.setMessage(e.getMessage());
				mklogger.error(TAG, "(executeSELLike) psmt = this.dbCon.prepareStatement(" + query + ") :" + e.getMessage());
			}
		} else {
			try {
				resultList = DA.executeSEL(true);
			} catch (SQLException e) {
				mkResponse.setCode(400);
				mkResponse.setMessage(e.getMessage());
				mklogger.error(TAG, "(executeSELLike) psmt = this.dbCon.prepareStatement(" + query + ") :" + e.getMessage());
			}
		}

		String test = "{\"" + mkPage + "\":[";
		if (resultList != null) {
			for (int l = 0; l < resultList.size(); l++) {
				String damn = resultList.get(l).toString();
				damn = damn.replaceAll("=", ":");
				test += damn;

				if (l < resultList.size() - 1)
					test += ", ";
			}
			test += "]}";

			MkRestApiData tttt = new MkRestApiData(test);

			if (tttt.setJsonObject()) {
				resultObject = tttt.getJsonObject();
			}
		}

		return resultObject;
	}

	private JSONObject doTaskPost(PageJsonData pjData, SqlJsonData sqlData, JSONObject jsonObject, String mkPage, MkRestApiResponse mkResponse) {
		JSONObject resultObject = null;
		MkDbAccessor DA = new MkDbAccessor();

		String service = pjData.getServiceName();
		String control = sqlData.getControlName();
		String befQuery = cpi.regularQuery(control, service, true);
		String query = null;
		String[] inputKey = pjData.getData();
		
		int requestSize = jsonObject.size();
		
		if(inputKey.length != requestSize) {
			mklogger.error(TAG, "��ǲ�� �ٸ��ϴ�.");
			mkResponse.setCode(400);
			mkResponse.setMessage("You must enter every column data.");
			return null;
		}
		String[] inputValues = new String[inputKey.length];
		
		for(int i = 0; i < inputKey.length; i++) {
			if(jsonObject.get(inputKey[i]) == null) {
				mkResponse.setCode(400);
				mkResponse.setMessage("You must enter every column data.");
				return null;
			}
			inputValues[i] = jsonObject.get(inputKey[i]).toString();
		}
		
		DA.setRequestValue(inputValues);
		query = cpi.setQuery(befQuery);
		if(query == null) {
			mkResponse.setCode(500);
			mkResponse.setMessage("Server Error. Please contact Admin.");
			mklogger.error(TAG, "Query is null. Please check API SQL configs");
			return null;
		}
		DA.setPreparedStatement(query);
		
		int result;
		try {
			result = DA.executeDML();
		} catch (SQLException e) {
			mkResponse.setCode(400);
			mkResponse.setMessage(e.getMessage());
			
			return null;
		} 

		if(result > 0) {
			resultObject = new JSONObject();
			resultObject.put(mkPage, jsonObject);
		}
		
		mklogger.debug(TAG, "resultObject: " + resultObject);
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

		

		return resultObject;
	}
}
