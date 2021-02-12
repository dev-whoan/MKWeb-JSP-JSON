package com.mkweb.restapi;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
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
import com.mkweb.data.MkPageJsonData;
import com.mkweb.data.MkSqlJsonData;
import com.mkweb.database.MkDbAccessor;
import com.mkweb.logger.MkLogger;
import com.mkweb.utils.ConnectionChecker;

/**
 * Servlet implementation class MkRestApi
 */

/*
 * 1. 순서 정렬 2. 전체 조회 3. 응답 정리 ( 코드, 콘텐츠 타입, 실패 사유 등. )
 */

public class MkRestApi extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private MkLogger mklogger = MkLogger.Me();
	private String TAG = "[MkRestApi]";
	private ConnectionChecker cpi = null;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public MkRestApi() {
		super();
		cpi = new ConnectionChecker();
	}

	private boolean checkMethod(ArrayList<MkPageJsonData> pageJsonData, String requestMethod) {
		for (MkPageJsonData pjd : pageJsonData) {
			if (pjd.getMethod().toString().toLowerCase().contentEquals(requestMethod)) {
				return true;
			}
		}

		return false;
	}

	private boolean isKeyValid(String key, String mkPage) {
		boolean isDone = false;
		MkRestApiGetKey mra = new MkRestApiGetKey();
		String keyColumn = MkConfigReader.Me().get("mkweb.restapi.key.column.name");
		String remarkColumn = MkConfigReader.Me().get("mkweb.restapi.key.column.remark");
		ArrayList<Object> apiKeyList = mra.GetKey();

		mklogger.temp(TAG, " REST Api Key has searched : " + key + " Result: ", false);

		if (apiKeyList != null) {
			for (int i = 0; i < apiKeyList.size(); i++) {
				HashMap<String, Object> result = new HashMap<String, Object>();
				result = (HashMap<String, Object>) apiKeyList.get(i);
				if (result.get(keyColumn).equals(key)) {
					mklogger.temp(TAG, " key is valid! (remark : " + result.get(remarkColumn) + ")", false);
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
		final String MKWEB_CUSTOM_TABLE = MkConfigReader.Me().get("mkweb.restapi.search.customtable");

		String customTable = request.getParameter(MKWEB_CUSTOM_TABLE);
		mklogger.debug(TAG, "customtable name : " + MKWEB_CUSTOM_TABLE);
		mklogger.debug(TAG, "requested customTable : " + customTable);
		String requestURI = request.getRequestURI();
		String[] reqPage = null;
		String mkPage = null;
		String userKey = null;
		JSONObject requestParameterJson = null;
		JSONObject requestParameterJsonToModify = null;
		MkJsonData mkJsonData = new MkJsonData();

		MkRestApiResponse apiResponse = new MkRestApiResponse();
		JSONObject resultObject = null;
		apiResponse.setCode(200);

		while(true) {
			reqPage = requestURI.split("/" + MKWEB_URI_PATTERN + "/");

			if (reqPage.length < 2) {
				// 예외
				apiResponse.setMessage("Please enter the conditions to search.");
				apiResponse.setCode(401);
				break;
			}

			mkPage = reqPage[1];

			if (mkPage.contains("/")) {
				mkPage = mkPage.split("/")[0];
			}
			
			mklogger.debug(TAG, "Request MKPage : " + mkPage + "| Method : " + REQUEST_METHOD);

			ArrayList<MkPageJsonData> control = MkRestApiPageConfigs.Me().getControl(mkPage);

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
				apiResponse.setCode(405);
				break;
			}

			if (REQUEST_METHOD.contentEquals("get") || REQUEST_METHOD.contentEquals("head") ||
					REQUEST_METHOD.contentEquals("options") || REQUEST_METHOD.contentEquals("put") ||
					REQUEST_METHOD.contentEquals("delete")) {
				mkJsonData.setData(request.getParameter(MKWEB_API_ID));
				if (mkJsonData.setJsonObject()) {
					requestParameterJson = mkJsonData.getJsonObject();
					userKey = requestParameterJson.get(MKWEB_SEARCH_KEY).toString();
				} else {
					String[] tempURI = requestURI.split("/");
					int shouldCheckQuery = -1;
					int mkPageIndex = -1;
					/*
					 * -1 : do Nothing 0 : 전체 조회 1 : String check
					 */
					if (tempURI[(tempURI.length - 1)].contentEquals(mkPage)) {
						if (request.getParameterMap().size() == 1) {
							String element = rpn.nextElement();
							String elementValue = null;
							if (element.contentEquals(MKWEB_API_ID)) {
								elementValue = request.getParameter(element);
								if (elementValue.contains("=") && elementValue.contains(MKWEB_SEARCH_KEY)) {
									// mklogger.debug(TAG, "경우 1과 같음(/mk_api_key/users 처럼) 1 ");
									shouldCheckQuery = 0;
								} else if (!elementValue.contains(MKWEB_SEARCH_KEY)) {
									// mklogger.debug(TAG, "(1) 경우 2 - /mk_api_key/users?queryString~");
									shouldCheckQuery = 1;
								}
							} else {
								if (!element.contentEquals(MKWEB_SEARCH_KEY)) {
									// mklogger.debug(TAG, "(1) 경우 2 - /mk_api_key/users?queryString~");
									shouldCheckQuery = 1;
								} else {
									// mklogger.debug(TAG, "경우 1과 같음 (/mk_api_key/users 처럼) 2 ");
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
						mklogger.debug(TAG, "APIData가 존재 하는가? " + request.getParameter(MKWEB_API_ID));

						if (tempAPIID != null) {
							String[] tempArr = null;
							if (tempAPIID.contains("&")) {
								mklogger.debug(TAG, " & 있음 ");
								tempArr = tempAPIID.split("&");
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
						mklogger.debug(TAG, "result : " + result);
						mklogger.debug(TAG, "rqj    : " + requestParameterJson);

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
						 * 홀수번에는 Column 짝수번에는 데이터
						 */
						// mkPageIndex = users를 찾음
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
						
						if (searchColumns.size() == searchValues.size() + 1) 
							searchValues.add(MKWEB_SEARCH_ALL);
						
						if (searchColumns.size() != searchValues.size()) {
							mklogger.error(TAG, "API Request is not valid.");
							mklogger.debug(TAG, "searchColumns size != searchValues.size");
							apiResponse.setMessage("부분 조건에 대해 전체검색을 수행할 수 없습니다.(아직 개발되지 않은 기능)");
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
						if(REQUEST_METHOD.contentEquals("get"))
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
			} 

			if(REQUEST_METHOD.contentEquals("post") || REQUEST_METHOD.contentEquals("put") || REQUEST_METHOD.contentEquals("delete")){

				mkJsonData.setData(request.getParameter(MKWEB_API_ID));
				if(mkJsonData.getData() == null) {
					StringBuilder stringBuilder = new StringBuilder();
					BufferedReader bufferedReader = null;
					try (InputStream inputStream = request.getInputStream()){
						if (inputStream != null) {
							bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
							String line;
							while((line = bufferedReader.readLine()) != null){
								stringBuilder.append(line);
							}
						}
					} catch (IOException ex) {
						throw ex;
					} finally {
						if (bufferedReader != null) {
							try {
								bufferedReader.close();
							} catch (IOException ex) {
								throw ex;
							}
						}
					}
					String rawData = URLDecoder.decode(stringBuilder.toString(), "UTF-8");
					mklogger.debug(TAG, "rawData : " + rawData);
					String rawDatas[] = rawData.split("\\=");

					if(rawDatas.length == 2) {
						if(rawDatas[0].contentEquals(MKWEB_API_ID)) {
							mkJsonData.setData(rawDatas[1]);
						}
					} else {
						if(requestParameterJson == null) {
							mklogger.error(TAG, "API 사용법이 틀렸습니다. MKWEB_API_ID가 포함되어야 합니다.");
							apiResponse.setMessage("API 사용법이 틀렸습니다. MKWEB_API_ID가 포함되어야 합니다.");
							apiResponse.setCode(400);
							break;
						}
					}
				}
				if (mkJsonData.setJsonObject()) {
					if(REQUEST_METHOD.contentEquals("put"))
						requestParameterJsonToModify = mkJsonData.getJsonObject();
					else {
						if(REQUEST_METHOD.contentEquals("delete") && requestParameterJson != null) {
							/*	 DELETE는 Body Parameter 혹은 URI 중 하나로만 데이터를 전달해야 한다. */
							mklogger.error(TAG, "Delete methods only 1 way to pass the parameters. You can use only URI parameter or Body parameter.");
							apiResponse.setMessage("Delete methods only 1 way to pass the parameters. You can use only URI parameter or Body parameter.");
							apiResponse.setCode(400);
							break;
						}
						requestParameterJson = mkJsonData.getJsonObject();
					}

				} else {
					if(mkJsonData.getData() != null) {
						String tempJsonString = mkJsonData.stringToJsonString(mkJsonData.getData());

						mkJsonData.setData(tempJsonString);

						if (!mkJsonData.setJsonObject()) {
							mklogger.error(TAG,
									"API Request only allow with JSON type. Cannot convert given data into JSON Type.");
							apiResponse.setMessage("Please check your request. The entered data cannot be converted into JSON Type.");
							apiResponse.setCode(400);
							break;
						}
						if(REQUEST_METHOD.contentEquals("put"))
							requestParameterJsonToModify = mkJsonData.getJsonObject();
						else
							requestParameterJson = mkJsonData.getJsonObject();
					}
				}
			}

			if(userKey == null) {
				if(requestParameterJson.get(MKWEB_SEARCH_KEY) != null) {
					userKey = requestParameterJson.get(MKWEB_SEARCH_KEY).toString();
				}else if(requestParameterJsonToModify != null) {
					if(requestParameterJsonToModify.get(MKWEB_SEARCH_KEY) != null) {
						userKey = requestParameterJsonToModify.get(MKWEB_SEARCH_KEY).toString();
					}
				}

				if(userKey == null) {
					userKey = request.getParameter(MKWEB_SEARCH_KEY);
				}
			}

			if (MKWEB_USE_KEY) {
				if (!isKeyValid(userKey, mkPage)) {
					apiResponse.setMessage("The key is not valid.");
					apiResponse.setCode(401);
					break;
				}
			}
			if(requestParameterJson != null) {
				requestParameterJson.remove(MKWEB_SEARCH_KEY);
				if(customTable != null)
					requestParameterJson.remove(MKWEB_CUSTOM_TABLE);
			}
				
			if(requestParameterJsonToModify != null) {
				requestParameterJsonToModify.remove(MKWEB_SEARCH_KEY);
				if(customTable != null)
					requestParameterJson.remove(MKWEB_CUSTOM_TABLE);
			}
				
			
			MkPageJsonData pageService = null;
			MkSqlJsonData sqlService = null;
			if(!REQUEST_METHOD.contentEquals("options")) {
				Set<String> requestKeySet = requestParameterJson.keySet();
				Iterator<String> requestIterator = requestKeySet.iterator();

				ArrayList<MkPageJsonData> pageControl = MkRestApiPageConfigs.Me().getControl(mkPage);

				for (MkPageJsonData service : pageControl) {
					// mklogger.debug(TAG, " service method : " + service.getMethod());
					if (REQUEST_METHOD.contentEquals(service.getMethod())) {
						pageService = service;
						break;
					}
				}

				if (pageService == null) {
					mklogger.error(TAG, "요청한 서비스는 존재하지 않습니다. (Method에 대한 Service 가 null임)");
					apiResponse.setMessage("The method you requested is not allowed.");
					apiResponse.setCode(405);
					break;
				}

				ArrayList<MkSqlJsonData> sqlControl = MkRestApiSqlConfigs.Me().getControlByServiceName(pageService.getServiceName());

				String[] sqlConditions = sqlControl.get(0).getCondition();

				if (sqlConditions.length == 0) {
					mklogger.error(TAG, "SQL Config 설정이 잘못됐습니다. condition이 비어있습니다. 전체조회를 희망할 경우 \"1\":\"*\"을 추가해 주세요.");
					apiResponse.setMessage("SERVER ERROR. Please contact admin.");
					apiResponse.setCode(500);
					break;
				}

				for (MkSqlJsonData sqlServiceData : sqlControl) {
					if (sqlServiceData.getServiceName().contentEquals(pageService.getServiceName())) {
						sqlService = sqlServiceData;
						break;
					}
				}
				
				if (sqlService == null) {
					mklogger.error("요청한 SQL Service가 없습니다.");
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
						mklogger.error(TAG, "요청한 Value에 대한 검색이 불가능합니다. " + requestKey);
						apiResponse.setMessage("The column client entered is not allowed. (" + requestKey + ")");
						apiResponse.setCode(400);
						break;
					}
					
					String[] requireParameters = sqlService.getParameters();
					
					if(requireParameters != null && requireParameters.length > 0) {
						List<String> tempRequireParams = Arrays.asList(requireParameters);
						if( tempRequireParams.indexOf(requestKey) == -1 ) {
							mklogger.error(TAG, "Client must request with essential parameters.");
							apiResponse.setCode(400);
							apiResponse.setMessage("You must request with essential parameters.");
							break;
						}
					}
				}

				
				
				if(apiResponse.getCode() >= 400 && apiResponse.getCode() != -1){
					break;
				}

				switch (REQUEST_METHOD) {
				case "get": case "head": case "options":
					resultObject = doTaskGet(pageService, sqlService, requestParameterJson, mkPage, MKWEB_SEARCH_ALL, apiResponse, customTable);
					break;
				case "post":
					resultObject = doTaskInput(pageService, sqlService, requestParameterJson, mkPage, REQUEST_METHOD, apiResponse, customTable);
					break;
				case "put":
					mklogger.debug(TAG, " putting ... ");
					resultObject = doTaskPut(pageService, sqlService, requestParameterJson, requestParameterJsonToModify, mkPage, MKWEB_SEARCH_ALL, REQUEST_METHOD, apiResponse, customTable);
					break;

				case "delete":
					resultObject = doTaskDelete(pageService, sqlService, requestParameterJson, mkPage, apiResponse, customTable);
					break;
				}
				break;
			}
			break;
		}

		apiResponse.setContentType("application/json;charset=UTF-8");
		response.setStatus(apiResponse.getCode());
		response.setContentType(apiResponse.getContentType());
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Result", "HTTP/1.1 " + apiResponse.getCode() + " " + apiResponse.getStatus());
		response.addHeader("Life-Time", "" + apiResponse.getLifeTime());

		PrintWriter out = response.getWriter();

		String result = null;
		if(resultObject == null) {
			String allowMethods = "";
			if(apiResponse.getCode() < 400 && REQUEST_METHOD.contentEquals("options")) {
				allowMethods = "  \"Allow\":\"";
				ArrayList<MkPageJsonData> control = MkRestApiPageConfigs.Me().getControl(mkPage);
				for(int i = 0; i < control.size(); i++) {

					allowMethods += control.get(i).getMethod().toString().toUpperCase();

					if( i < control.size()-1) {
						allowMethods += ",";
					}
				}
				allowMethods += "\"";
			}
			result = apiResponse.generateResult(apiResponse.getCode(), REQUEST_METHOD, allowMethods);
			out.print(result);
		}else {
			result = mkJsonData.jsonToPretty(resultObject);
			result = result.substring(1, result.length()-1);
			Object roPut = resultObject.get("PUT_UPDATE_DONE");
			Object roPost = resultObject.get("PUT_INSERT_DONE");
			Object roDelete = resultObject.get("DELETE_DONE");
			if(roPut != null) {
				result = "";
			}else if(roPost != null) {
				resultObject.remove("PUT_INSERT_DONE");
			}else if(roDelete != null) {
				resultObject.remove("DELETE_DONE");
			}
			apiResponse.setContentLength(resultObject.toString().length());
			
			String temp = apiResponse.generateResult(apiResponse.getCode(), REQUEST_METHOD, result);
			out.print(temp);
		}
		out.flush();
		out.close();
	}

	private JSONObject doTaskGet(MkPageJsonData pjData, MkSqlJsonData sqlData, JSONObject jsonObject, String mkPage,
			String MKWEB_SEARCH_ALL, MkRestApiResponse mkResponse, String customTable) {		
		JSONObject resultObject = null;
	
		MkDbAccessor DA = new MkDbAccessor(sqlData.getDB());

		String service = pjData.getServiceName();
		String control = sqlData.getControlName();
		String befQuery = cpi.regularQuery(control, service, true);
		String query = null;
		String[] searchKeys = pjData.getData();
		int requestSize = jsonObject.size();
		boolean searchAll = false;
		
		for(int i = 0; i < searchKeys.length; i++) {
			mklogger.debug(TAG, "searchkeys :" + searchKeys[i]);
		}
		
		query = (customTable == null) ? 
				createSQL("get", searchKeys, jsonObject, null, null, sqlData.getRawSql()[2]) :
				createSQL("get", searchKeys, jsonObject, null, null, customTable);
		
		Set<String> keySet = jsonObject.keySet();
		Iterator<String> iter = keySet.iterator();
		ArrayList<String> sqlKey = new ArrayList<String>();
		String condition = " WHERE ";
		int i = 0;

		while (iter.hasNext()) {
			String key = iter.next();
			mklogger.debug(TAG, "key : " + key);
			if (requestSize == 1) {
				if (key.contentEquals(MKWEB_SEARCH_ALL)) {
					searchAll = true;
					continue;
				}
			}
			condition += key + " = ?";
			String temp = jsonObject.get(key).toString();
			try {
				String decodeResult = URLDecoder.decode(temp, "UTF-8");
				String encodeResult = URLEncoder.encode(decodeResult, "UTF-8");
				
				temp = (encodeResult.contentEquals(temp) ? decodeResult : temp);
			} catch (UnsupportedEncodingException e) {
				//
				mklogger.error(TAG, "(doTaskGet - jsonObject key) given data (" + temp + ") is invalid! " + e.getMessage());
				mkResponse.setCode(400);
				mkResponse.setMessage(e.getMessage());
				return null;
			}
			
			sqlKey.add(temp);
			if (i < requestSize - 1) {
				condition += " AND ";
			}
			i++;
		}
		if (condition.contains("?"))
			query += condition;

		mklogger.debug(TAG, "condition : " + query);
		mklogger.debug(TAG, "query : " + query);
		
		DA.setPreparedStatement(query);
		if (!searchAll)
			DA.setApiRequestValue(sqlKey);

		ArrayList<Object> resultList = null;
		if (sqlData.getAllowLike()) {
			try {
				resultList = DA.executeSELLike(true);
			} catch (SQLException e) {
				mkResponse.setCode(400);
				mkResponse.setMessage(e.getMessage());
				mklogger.error(TAG, "(executeSELLike) psmt = this.dbCon.prepareStatement(" + query + ") :" + e.getMessage());
				return null;
			}
		} else {
			try {
				resultList = DA.executeSEL(true);
			} catch (SQLException e) {
				mkResponse.setCode(400);
				mkResponse.setMessage(e.getMessage());
				mklogger.error(TAG, "(executeSELLike) psmt = this.dbCon.prepareStatement(" + query + ") :" + e.getMessage());
				return null;
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

		if(resultObject == null)
			mkResponse.setCode(204);

		return resultObject;
	}

	private JSONObject doTaskInput(MkPageJsonData pjData, MkSqlJsonData sqlData, JSONObject jsonObject, String mkPage,
			String requestMethod, MkRestApiResponse mkResponse, String customTable) {

		JSONObject resultObject = null;
		MkDbAccessor DA = new MkDbAccessor(sqlData.getDB());

		String service = pjData.getServiceName();
		String control = sqlData.getControlName();
		String befQuery = cpi.regularQuery(control, service, true);
		String query = null;
		String[] inputKey = pjData.getData();

		int requestSize = jsonObject.size();

		if(inputKey.length != requestSize) {
			mklogger.error(TAG, "인풋이 다릅니다.");
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
		
		query = (customTable == null) ? 
				createSQL("post", inputKey, null, inputValues, null, sqlData.getRawSql()[2]) :
				createSQL("post", inputKey, null, inputValues, null, customTable);
		
		DA.setRequestValue(inputValues);
		query = cpi.setQuery(befQuery);
		mklogger.debug(TAG, "그래서 쿼리는 : " + query);
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
			mkResponse.setCode(201);
		}

		mklogger.debug(TAG, "resultObject: " + resultObject);
		return resultObject;
	}

	private JSONObject doTaskPut(MkPageJsonData pjData, MkSqlJsonData sqlData, JSONObject jsonObject, JSONObject modifyObject, String mkPage,
			String MKWEB_SEARCH_ALL, String requestMethod, MkRestApiResponse mkResponse, String customTable) {
		if(modifyObject == null) {
			mklogger.error(TAG, "PUT method should wrapped by " + MkConfigReader.Me().get("mkweb.restapi.request.id") + ".");
			mkResponse.setMessage("PUT method should wrapped by " + MkConfigReader.Me().get("mkweb.restapi.request.id") + ".");
			mkResponse.setCode(400);
			return null;
		}
		JSONObject resultObject = null;
		String[] inputKey = pjData.getData();

		JSONObject getResult = doTaskGet(pjData, sqlData, jsonObject, mkPage, MKWEB_SEARCH_ALL, mkResponse, customTable);
		MkDbAccessor DA = new MkDbAccessor(sqlData.getDB());
		String service = pjData.getServiceName();
		String control = sqlData.getControlName();
		String query = null;
		String[] modifyValues = new String[modifyObject.size()];
		int mvIterator = 0;
		for(int i = 0; i < pjData.getData().length; i++) {
			Object tik = modifyObject.get(inputKey[i]);
			if(tik != null) {					
				modifyValues[mvIterator++] = tik.toString();					
			}
		}
		String[] searchKey = new String[jsonObject.size()];
		String[] modifyKey = new String[modifyObject.size()];
		Set<String> jSet = jsonObject.keySet();
		Iterator<String> jKey = jSet.iterator();
		int i = 0;
		while(jKey.hasNext()) {
			searchKey[i++] = jKey.next();
		}
		i = 0;
		jSet = modifyObject.keySet();
		jKey = jSet.iterator();
		while(jKey.hasNext()) {
			modifyKey[i++] = jKey.next();
		}
		i = 0;

		String tempCrud = (getResult == null ? "insert" : "update");

		query = (customTable == null) ?
				(createSQL(tempCrud, searchKey, jsonObject, modifyKey, modifyObject, sqlData.getRawSql()[2])):
				(createSQL(tempCrud, searchKey, jsonObject, modifyKey, modifyObject, customTable));

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
			if(tempCrud.contentEquals("insert")) {
				resultObject.put("PUT_INSERT_DONE", "true");
				resultObject.put(mkPage, jsonObject);
				mkResponse.setCode(201);
			}else {
				resultObject.put("PUT_UPDATE_DONE", "true");
				mkResponse.setCode(200);
			}
		}	

		return resultObject;
	}

	private JSONObject doTaskDelete(MkPageJsonData pxData, MkSqlJsonData sqlData, JSONObject jsonObject, String mkPage, MkRestApiResponse mkResponse, String customTable) {
		JSONObject resultObject = null;
		MkDbAccessor DA = new MkDbAccessor(sqlData.getDB());

		String service = pxData.getServiceName();
		String control = pxData.getControlName();
		String query = null;

		String[] searchKeys = new String[jsonObject.size()];

		Set<String> jSet = jsonObject.keySet();
		Iterator<String> jKey = jSet.iterator();

		int i = 0;
		while(jKey.hasNext()) {
			searchKeys[i++] = jKey.next();
		}

		query = (customTable == null) ? 
						createSQL("delete", searchKeys, jsonObject, null, null, sqlData.getRawSql()[2]) :
						createSQL("delete", searchKeys, jsonObject, null, null, customTable);
						
		mklogger.debug(TAG, "delete query: " + query);
		DA.setPreparedStatement(query);

		int result;
		try {
			result = DA.executeDML();
		} catch (SQLException e) {
			mkResponse.setCode(400);
			mkResponse.setMessage(e.getMessage());
			return null;
		}

		resultObject = new JSONObject();
		resultObject.put("DELETE_DONE", "true");
		mkResponse.setCode(204);

		return resultObject;
	}

	private String createSQL(String crud, String[] searchKey, JSONObject searchObject, String[] modifyKey, JSONObject modifyObject, String Table) {
		String result = null;
		switch(crud.toLowerCase()) {
		case "get":
		{
			/*
			String whereClause = (searchKey.length > 0 ? " WHERE " : "");
			for(int i = 0; i < searchKey.length; i++) {
				whereClause += searchKey[i] + "=" + "'" + searchObject.get(searchKey[i]) + "'";
				if(i < searchKey.length -1) {
					whereClause += " AND ";
				}
			}
			*/
			
			String valueClause = "*";
			if(searchKey != null && searchKey.length > 0) {
				valueClause = "";
				for(int i = 0; i < searchKey.length; i++) {
					String temp = searchKey[i];
					try {
						String decodeResult = URLDecoder.decode(temp, "UTF-8");
						String encodeResult = URLEncoder.encode(decodeResult, "UTF-8");
						
						temp = (encodeResult.contentEquals(temp) ? decodeResult : temp);
					} catch (UnsupportedEncodingException e) {
						//
						mklogger.error(TAG, "(createSQL get) given data (" + temp + ") is invalid! " + e.getMessage());
						return null;
					}
					valueClause += temp;
					
					if(i < searchKey.length-1) {
						valueClause += ", ";
					}
				}
			}
	//		result = "SELECT * FROM TABLE WHERE ?";
			result = "SELECT " + valueClause + " FROM `" + Table +"`";

			break;	
		}
		case "post":
		{
			String targetColumns = "";
			String targetValues = "";
			for(int i = 0; i < searchKey.length; i++) {
				String temp = searchKey[i];
				try {
					String decodeResult = URLDecoder.decode(temp, "UTF-8");
					String encodeResult = URLEncoder.encode(decodeResult, "UTF-8");
					
					temp = (encodeResult.contentEquals(temp) ? decodeResult : temp);
				} catch (UnsupportedEncodingException e) {
					//
					mklogger.error(TAG, "(createSQL post) given searchKey (" + temp + ") is invalid! " + e.getMessage());
					return null;
				}
				
				targetColumns += temp;
				if(i < searchKey.length - 1)
					targetColumns += ", ";
			}
			for(int i = 0; i < modifyKey.length; i++) {
				String temp = modifyKey[i];
				try {
					String decodeResult = URLDecoder.decode(temp, "UTF-8");
					String encodeResult = URLEncoder.encode(decodeResult, "UTF-8");
					
					temp = (encodeResult.contentEquals(temp) ? decodeResult : temp);
				} catch (UnsupportedEncodingException e) {
					//
					mklogger.error(TAG, "(createSQL post) given modifyKey (" + temp + ") is invalid! " + e.getMessage());
					return null;
				}
				
				targetValues = "'" + temp + "'";
				if( i < modifyKey.length - 1)
					targetValues += ", ";
			}
			result = "INSERT INTO `" + Table + "` (" + targetColumns + ") VALUE(" + targetValues + ");";

			break;	
		}
		case "insert":
		{
			String targetColumns = "";
			String targetValues = "";
			for(int i = 0; i < modifyKey.length; i++) {
				String temp = modifyKey[i];
				try {
					String decodeResult = URLDecoder.decode(temp, "UTF-8");
					String encodeResult = URLEncoder.encode(decodeResult, "UTF-8");
					
					temp = (encodeResult.contentEquals(temp) ? decodeResult : temp);
				} catch (UnsupportedEncodingException e) {
					//
					mklogger.error(TAG, "(createSQL insert) given modify Key (" + temp + ") is invalid! " + e.getMessage());
					return null;
				}
				
				targetColumns += temp;
				
				temp = modifyObject.get(modifyKey[i]).toString();
				try {
					String decodeResult = URLDecoder.decode(temp, "UTF-8");
					String encodeResult = URLEncoder.encode(decodeResult, "UTF-8");
					
					temp = (encodeResult.contentEquals(temp) ? decodeResult : temp);
				} catch (UnsupportedEncodingException e) {
					//
					mklogger.error(TAG, "(createSQL insert) given modify Object (" + temp + ") is invalid! " + e.getMessage());
					return null;
				}
				targetValues += "'" + temp + "'";
				if(i < modifyKey.length-1) {
					targetColumns += ",";
					targetValues += ",";
				}
			}
			
			result = "INSERT INTO `" + Table + "` (" + targetColumns + ") VALUE(" + targetValues + ");";

			break;	
		}
		case "update":
		{
			String whereClause = (searchKey.length > 0 ? " WHERE " : "");
			for(int i = 0; i < searchKey.length; i++) {
				whereClause += searchKey[i] + "=" + "'" + searchObject.get(searchKey[i]) + "'";
				if(i < searchKey.length -1) {
					whereClause += " AND ";
				}
			}

			String dataField = "";
			for(int i = 0; i < modifyKey.length; i++) {
				dataField += modifyKey[i] + "=" + "'" + modifyObject.get(modifyKey[i]) + "'";

				if(i < modifyKey.length-1) {
					dataField += ", ";
				}
			}
			result = "UPDATE `" + Table + "` SET " + dataField + whereClause + ";";
			break;
		}
		case "delete":
		{
			if(searchKey.length < 1)
				return null;

			String whereClause = "";

			for(int i = 0; i < searchKey.length; i++) {
				whereClause += searchKey[i] + "=" + "'" + searchObject.get(searchKey[i]) + "'";
				if(i < searchKey.length -1) {
					whereClause += " AND ";
				}
			}

			result = "DELETE FROM `" + Table + "` WHERE " + whereClause + ";";
			break;
		}
		/*	Switch parentheses	*/
		}

		return result;
	}
}