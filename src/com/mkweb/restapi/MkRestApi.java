package com.mkweb.restapi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mkweb.utils.MkUtils;
import org.json.simple.JSONObject;

import com.mkweb.config.MkConfigReader;
import com.mkweb.config.MkRestApiPageConfigs;
import com.mkweb.config.MkRestApiSqlConfigs;
import com.mkweb.utils.MkJsonData;
import com.mkweb.data.MkPageJsonData;
import com.mkweb.data.MkSqlJsonData;
import com.mkweb.database.MkDbAccessor;
import com.mkweb.logger.MkLogger;
import com.mkweb.utils.ConnectionChecker;

/**
 * Servlet implementation class MkRestApi
 */
@WebServlet(
		name = "MkRestApiServlet",
		loadOnStartup=1
)
public class MkRestApi extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String TAG = "[MkRestApi]";
	private static final MkLogger mklogger = new MkLogger(TAG);

	private static String MKWEB_URI_PATTERN = MkConfigReader.Me().get("mkweb.restapi.uri");
	private static String MKWEB_SEARCH_KEY = MkConfigReader.Me().get("mkweb.restapi.search.keyexp");
	private static boolean MKWEB_USE_KEY = MkConfigReader.Me().get("mkweb.restapi.search.usekey").contentEquals("yes");
	private static String MKWEB_SEARCH_ALL = MkConfigReader.Me().get("mkweb.restapi.search.all");
	private static String MKWEB_REFONLY_HOST = MkConfigReader.Me().get("mkweb.restapi.refonly.host");
	private static String MKWEB_PRETTY_OPT = MkConfigReader.Me().get("mkweb.restapi.search.opt.pretty.param");
	private static String MKWEB_PAGING_OPT = MkConfigReader.Me().get("mkweb.restapi.search.opt.paging.param");

	public MkRestApi() {
		super();
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

		mklogger.temp(" REST Api Key has searched : " + key + " Result: ", false);

		if (apiKeyList != null) {
			for (Object o : apiKeyList) {
				HashMap<String, Object> result = new HashMap<String, Object>();
				result = (HashMap<String, Object>) o;
				if (result.get(keyColumn).equals(key)) {
					mklogger.temp(" key is valid! (remark : " + result.get(remarkColumn) + ")", false);
					mklogger.flush("info");
					isDone = true;
					break;
				}
			}
		} else {
			mklogger.temp(" Failed to search the key! (No Key List)", false);
			mklogger.flush("warn");
		}

		if (!isDone) {
			mklogger.temp(" Failed to search the key! (Key is invalid)", false);
			mklogger.flush("warn");
		}

		return isDone;
	}

	private LinkedHashMap<String, String> getParameterValues(HttpServletRequest request){
		Enumeration<String> parameters = request.getParameterNames();

		LinkedHashMap<String, String> result = null;
		if(parameters.hasMoreElements())
			result = new LinkedHashMap<>();

		while (parameters.hasMoreElements()) {
			String parameter = parameters.nextElement();
			result.put(parameter, request.getParameter(parameter));
		}

		return result;
	}
	
	private void checkHost(MkRestApiResponse apiResponse, String previousURL, String hostcheck, String host) {
		if(previousURL == null) {
			previousURL = "null";
		} else {
			previousURL = previousURL.split("://")[1];
			if(previousURL.contains("/")) {
				previousURL = previousURL.split("/")[0] + "/";
			}
		}
		if(!previousURL.contentEquals(host)) {
			mklogger.error("User blocked by CORS policy: No 'Access-Control-Allow-Origin'.");
			apiResponse.setCode(401);
			apiResponse.setMessage("You violated CORS policy: No 'Access-Control-Allow-Origin'.");
		}
	}

	private void remvoeParameter(JSONObject jsonObject, String key) {
		if(jsonObject != null) {
			jsonObject.remove(MKWEB_SEARCH_KEY);

			if(key != null)
				jsonObject.remove(key);
		}
	}

	private void removeOptionsFromParameter(JSONObject jsonObject, String prettyParam, String pagingParam) {
		if(jsonObject != null) {
			jsonObject.remove(MKWEB_SEARCH_KEY);

			if(prettyParam != null)
				jsonObject.remove(MKWEB_PRETTY_OPT);

			if(pagingParam != null)
				jsonObject.remove(MKWEB_PAGING_OPT);
		}
	}

	private JSONObject readUriParameters(String uri, String pageName){
		int mkPageIndex = -1;
		String[] tempURI = uri.split("/");

		boolean searchAll = tempURI[(tempURI.length -1)].contentEquals(pageName);
		mkPageIndex = Arrays.asList(tempURI).indexOf(pageName);
		ArrayList<String> searchColumns = new ArrayList<>();
		ArrayList<String> searchValues = new ArrayList<>();

		if(!searchAll){
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
				mklogger.error("API Request is not valid.");
				mklogger.debug("searchColumns size != searchValues.size");
				JSONObject errorResponse = new JSONObject();
				errorResponse.put("error_code", 400);
				errorResponse.put("error_message", "You need to set all conditions to search.");
				return errorResponse;
			}
		} else {
			searchColumns.add(MKWEB_SEARCH_ALL);
			searchValues.add(MKWEB_SEARCH_ALL);
		}

		LinkedHashMap<String, String> result = new LinkedHashMap<>();

		for (int i = 0; i < searchColumns.size(); i++) {
			result.put(searchColumns.get(i), searchValues.get(i));
		}

		return MkJsonData.mapToJson(result);
	}
	
	protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setAttribute("api-method", "head");
		doTask(request, response);
	}

	protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setAttribute("api-method", "options");
		doTask(request, response);
	}

	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setAttribute("api-method", "put");
		doTask(request, response);
	}

	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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

	private void doTask(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String contentType = request.getContentType();
		String authToken = request.getHeader("Authorization");
		long START_MILLIS = System.currentTimeMillis();
		request.setCharacterEncoding("UTF-8");
		MkRestApiResponse apiResponse = new MkRestApiResponse();
		apiResponse.setCode(200);
		
		if (!MkConfigReader.Me().get("mkweb.restapi.use").equals("yes")) {
			apiResponse.setCode(404);
			apiResponse.setMessage("Not Found.");
		}
		
		
		final String REQUEST_METHOD = request.getAttribute("api-method").toString().toLowerCase();

		if((!REQUEST_METHOD.contentEquals("get")) && (contentType == null || !contentType.contains("application/json"))) {
			apiResponse.setCode(406);
			apiResponse.setMessage("Content type not supported.");
		}
		
		//		String customTable = request.getParameter(MKWEB_CUSTOM_TABLE);
		String prettyParam = null;
		String pagingParam = null;
		
		String requestURI = request.getRequestURI();
		String[] reqPage = null;
		String mkPage = null;
		String userKey = null;
		JSONObject requestParameterJson = null;
		JSONObject requestParameterJsonToModify = null;
		MkJsonData mkJsonData = new MkJsonData();
		JSONObject resultObject = null;

		if(MKWEB_REFONLY_HOST.toLowerCase().contentEquals("yes")) {
			checkHost(apiResponse, 
					request.getHeader("referer"), 
					request.getRequestURL().toString().split("://")[1], 
					MkConfigReader.Me().get("mkweb.web.hostname") + "/");
		}

		while(true) {
			if(apiResponse.getCode() != 200)
				break;

			reqPage = requestURI.split("/" + MKWEB_URI_PATTERN + "/");

			if (reqPage.length < 2) {
				apiResponse.setMessage("Please enter the conditions to search.");
				apiResponse.setCode(400);
				break;
			}
			mkPage = reqPage[1];

			if (mkPage.contains("/")) {
				mkPage = mkPage.split("/")[0];
			}

			ArrayList<MkPageJsonData> control = MkRestApiPageConfigs.Me().getControl(mkPage);
			if (control == null) {
				mklogger.error("[API] Control " + mkPage + " is not exist.");
				apiResponse.setMessage("There is no api named : " + mkPage);
				apiResponse.setCode(404);
				break;
			}

			Enumeration<String> rpn = request.getParameterNames();

			if (!checkMethod(control, REQUEST_METHOD)) {
				mklogger.error("[Control " + mkPage + "] does not allow method : " + REQUEST_METHOD);
				apiResponse.setMessage("The method you requested is not allowed.");
				apiResponse.setCode(405);
				break;
			}

			/*
			GET, HEAD, OPTIONS only allow query string
			PUT allow query string for condition, body parameter for update
			DELETE allow body parameter for deleting
			POST allow body parameter
			*/
			//Step 1 : Read URI
			requestParameterJson = readUriParameters(requestURI, mkPage);

			if(requestParameterJson.get("error_code") != null){
				apiResponse.setCode((int) requestParameterJson.get("error_code"));
				apiResponse.setMessage(requestParameterJson.get("error_message").toString());
				break;
			}

			//STEP 1 ENDED

					//Step 2 : Read Parameters will only handle text/plain. so this will skipped because MkWeb only allow application/json.

			//Step 2 : Read Body Parameter
			if(REQUEST_METHOD.contentEquals("post") || REQUEST_METHOD.contentEquals("put") || REQUEST_METHOD.contentEquals("delete")){
				StringBuilder stringBuilder = new StringBuilder();
				InputStream inputStream = request.getInputStream();
				try(BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))){
					String line;
					while((line = bufferedReader.readLine()) != null){
						stringBuilder.append(line);
					}
				} catch (IOException e){
					throw e;
				}

				String rawData = URLDecoder.decode(stringBuilder.toString(), StandardCharsets.UTF_8);
				try{
					if(rawData.charAt(0) == '"' && rawData.charAt(rawData.length()-1) == '"') {
						rawData = rawData.substring(1, rawData.length()-1);
					}

					int bslash = -1;
					while((bslash = rawData.indexOf("\\")) >= 0) {
						if(rawData.charAt(bslash+1) == '"') {
							String front = new String(rawData);
							String end = new String(rawData);
							front = front.substring(0, bslash);
							end = end.substring(bslash+2);
							rawData = front + "\"" + end;
						}
					}

					mklogger.debug("rawData : " + rawData);
					mkJsonData.setData(rawData);
				} catch (StringIndexOutOfBoundsException e){
					mklogger.error("Please check your request. There is no data to post, put or delete.");
					apiResponse.setMessage("Please check your request. There is no data to post, put or delete.");
					apiResponse.setCode(400);
					break;
				}

				mklogger.debug("jsonData: " + mkJsonData.getData());
				if (mkJsonData.setJsonObject()) {
					if(REQUEST_METHOD.contentEquals("put")) {
						requestParameterJsonToModify = mkJsonData.getJsonObject();
						mklogger.debug("rPJTM: " + requestParameterJsonToModify);
					}
					else {
						if(REQUEST_METHOD.contentEquals("delete") && requestParameterJson != null) {
							if(requestParameterJson.get(MKWEB_SEARCH_ALL) == null){
								mklogger.debug(requestParameterJson);
								mklogger.error("Delete methods only 1 way to pass the parameters. You can send data with body parameter.");
								apiResponse.setMessage("Delete methods only 1 way to pass the parameters. You can send data with body parameter.");
								apiResponse.setCode(400);
								break;
							}
						}
						requestParameterJson = mkJsonData.getJsonObject();
					}

				} else {
					if(mkJsonData.getData() != null) {
						String tempJsonString = MkJsonData.stringToJsonString(mkJsonData.getData());

						mkJsonData.setData(tempJsonString);

						if (!mkJsonData.setJsonObject()) {
							mklogger.error("API Request only allow with JSON type. Cannot convert given data into JSON Type.");
							apiResponse.setMessage("Please check your request. The entered data cannot be converted into JSON Type. You may need Key to use API.");
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

			remvoeParameter(requestParameterJson, "");

			//Step 3 : Read QueryString for Options
			Map<String, Object[]> queryParameters = MkUtils.getQueryParameters(request.getQueryString());
			mklogger.debug(queryParameters.get(MKWEB_PRETTY_OPT));
			prettyParam = queryParameters.get(MKWEB_PRETTY_OPT) != null ? queryParameters.get(MKWEB_PRETTY_OPT)[0].toString() : null;
			pagingParam = queryParameters.get(MKWEB_PAGING_OPT) != null ? queryParameters.get(MKWEB_PAGING_OPT)[0].toString() : null;
			mklogger.debug("rpj: " + requestParameterJson);
			if (MKWEB_USE_KEY){
				if(authToken != null){
					userKey = authToken.toLowerCase().split("bearer ")[1];
					if(requestParameterJson == null){
						requestParameterJson = new JSONObject();
						requestParameterJsonToModify = new JSONObject();
					}
				} else {
					Object[] queryKey = queryParameters.get(MKWEB_SEARCH_KEY);
					if(queryKey != null)
						userKey = queryKey[0].toString();

				}

				if (!isKeyValid(userKey, mkPage)) {
					apiResponse.setMessage("The key is not valid.");
					apiResponse.setCode(401);
					break;
				}
			}

			removeOptionsFromParameter(requestParameterJson, prettyParam, pagingParam);
			removeOptionsFromParameter(requestParameterJsonToModify, prettyParam, pagingParam);

			MkPageJsonData pageService = null;
			MkSqlJsonData sqlService = null;
			if(!REQUEST_METHOD.contentEquals("options")) {
				Set<String> requestKeySet = requestParameterJson.keySet();
				Iterator<String> requestIterator = requestKeySet.iterator();

				ArrayList<MkPageJsonData> pageControl = MkRestApiPageConfigs.Me().getControl(mkPage);

				for (MkPageJsonData service : pageControl) {
					if (REQUEST_METHOD.contentEquals(service.getMethod())) {
						pageService = service;
						break;
					}
				}

				if (pageService == null) {
					mklogger.error("There is no service executed by requested method.");
					apiResponse.setMessage("The method you requested is not allowed.");
					apiResponse.setCode(405);
					break;
				}

				ArrayList<MkSqlJsonData> sqlControl = MkRestApiSqlConfigs.Me().getControlByServiceName(pageService.getServiceName());
				String[] sqlConditions = sqlControl.get(0).getCondition();

				if (sqlConditions.length == 0) {
					mklogger.error("Something wrong in SQL Config. Condition is not entered. If you want to allow search whole datas, please set \"1\":\"*\"");
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
					mklogger.error("There is no SQL Service what client requested.");
					apiResponse.setMessage("The method you requested is not allowed.");
					apiResponse.setCode(405);
					break;
				}

				String[] pageValues = pageService.getData();

				requestIterator = requestKeySet.iterator();
				String[] requireParameters = sqlService.getParameters();
				List<String> tempRequireParams = null;
				if(requireParameters != null)
					tempRequireParams = new ArrayList<>(Arrays.asList(requireParameters));

				mklogger.debug(requestKeySet);
				while (requestIterator.hasNext()) {
					String requestKey = requestIterator.next().toString();
					int passed = -1;
					for (String pageValue : pageValues) {
						if (pageValue.contentEquals(requestKey) || requestKey.contentEquals(MKWEB_SEARCH_ALL)) {
							passed = 1;
							break;
						}
					}

					if (passed == 1) {
						for (String sqlCondition : sqlConditions) {
							if (sqlCondition.contentEquals(requestKey) || requestKey.contentEquals(MKWEB_SEARCH_ALL)) {
								passed = 2;
								break;
							}
						}
					}

					if (passed != 2) {
						mklogger.error("The value client requested is not allowed. " + requestKey);
						apiResponse.setMessage("The column client entered is not allowed. (" + requestKey + ")");
						apiResponse.setCode(400);
						break;
					}

					if(requireParameters != null && requireParameters.length > 0) {
						int id = tempRequireParams.indexOf(requestKey);
						if( id != -1 )
							tempRequireParams.remove(id);
					}
				}

				if(tempRequireParams != null && tempRequireParams.size() > 0) {
					mklogger.error("Client must request with essential parameters.");
					apiResponse.setCode(400);
					apiResponse.setMessage("You must request with essential parameters.");
					break;
				}

				if(apiResponse.getCode() >= 400 && apiResponse.getCode() != -1){
					break;
				}

				int page = (pagingParam != null ? Integer.parseInt(pagingParam) : -1);

				switch (REQUEST_METHOD) {
				case "get": case "head": case "options":
					//					resultObject = doTaskGet(pageService, sqlService, requestParameterJson, mkPage, MKWEB_SEARCH_ALL, apiResponse, customTable);
					resultObject = doTaskGet(pageService, sqlService, requestParameterJson, mkPage, MKWEB_SEARCH_ALL, apiResponse, page);
					break;
				case "post":
					//					resultObject = doTaskInput(pageService, sqlService, requestParameterJson, mkPage, REQUEST_METHOD, apiResponse, customTable);
					resultObject = doTaskInput(pageService, sqlService, requestParameterJson, mkPage, REQUEST_METHOD, apiResponse);
					break;
				case "put":
					mklogger.debug(" putting ... ");
					//					resultObject = doTaskPut(pageService, sqlService, requestParameterJson, requestParameterJsonToModify, mkPage, MKWEB_SEARCH_ALL, REQUEST_METHOD, apiResponse, customTable);
					resultObject = doTaskPut(pageService, sqlService, requestParameterJson, requestParameterJsonToModify, mkPage, MKWEB_SEARCH_ALL, REQUEST_METHOD, apiResponse);
					break;

				case "delete":
					//					resultObject = doTaskDelete(pageService, sqlService, requestParameterJson, mkPage, apiResponse, customTable);
					resultObject = doTaskDelete(pageService, sqlService, requestParameterJson, mkPage, apiResponse);
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
	//	response.addHeader("Life-Time", "" + apiResponse.getLifeTime());

		PrintWriter out = response.getWriter();

		String result = null;
		boolean pretty = false;
		pretty = (prettyParam != null);

		if(resultObject == null) {
			String allowMethods = "";
			if(apiResponse.getCode() < 400 && REQUEST_METHOD.contentEquals("options")) {
				if(pretty)
					allowMethods = "  \"Allow\":\"";
				else
					allowMethods = "\"Allow\":\"";
				ArrayList<MkPageJsonData> control = MkRestApiPageConfigs.Me().getControl(mkPage);
				for(int i = 0; i < control.size(); i++) {

					allowMethods += control.get(i).getMethod().toString().toUpperCase();

					if( i < control.size()-1) {
						allowMethods += ",";
					}
				}
				allowMethods += "\"";
			}
			result = apiResponse.generateResult(apiResponse.getCode(), REQUEST_METHOD, allowMethods, pretty, START_MILLIS);
			out.print(result);
		}else {
			if(pretty)
				result = MkJsonData.jsonToPretty(resultObject);
			else
				result = resultObject.toString();

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

			String temp = apiResponse.generateResult(apiResponse.getCode(), REQUEST_METHOD, result, pretty, START_MILLIS);
			out.print(temp);
		}
		out.flush();
		out.close();
	}

	private JSONObject doTaskGet(MkPageJsonData pjData, MkSqlJsonData sqlData, JSONObject jsonObject, String mkPage,
			String MKWEB_SEARCH_ALL, MkRestApiResponse mkResponse, int page) {
		JSONObject resultObject = null;

		MkDbAccessor DA = new MkDbAccessor(sqlData.getDB());

		String service = pjData.getServiceName();
		String control = sqlData.getControlName();
		String befQuery = ConnectionChecker.regularQuery(control, service, true);
		String query = null;
		String[] searchKeys = pjData.getData();
		int requestSize = jsonObject.size();
		boolean searchAll = false;

		/*
		query = (customTable == null) ? 
				createSQL("get", searchKeys, jsonObject, null, null, sqlData.getTableData().get("from").toString()) : //sqlData.getRawSql()[2]) :
				createSQL("get", searchKeys, jsonObject, null, null, customTable);
		 */
		query = createSQL("get", searchKeys, jsonObject, null, null, sqlData.getTableData().get("from").toString()); //sqlData.getRawSql()[2]) :

		Set<String> keySet = jsonObject.keySet();
		Iterator<String> iter = keySet.iterator();
		ArrayList<String> sqlKey = new ArrayList<String>();
		StringBuilder condition = new StringBuilder(" WHERE ");
		int i = 0;

		while (iter.hasNext()) {
			String key = iter.next();
			mklogger.debug("key : " + key);
			if (requestSize == 1) {
				if (key.contentEquals(MKWEB_SEARCH_ALL)) {
					searchAll = true;
					continue;
				}
			}
			condition.append(key).append(" = ?");
			String temp = jsonObject.get(key).toString();
			String decodeResult = URLDecoder.decode(temp, StandardCharsets.UTF_8);
			String encodeResult = URLEncoder.encode(decodeResult, StandardCharsets.UTF_8);

			temp = (encodeResult.contentEquals(temp) ? decodeResult : temp);

			sqlKey.add(temp);
			if (i < requestSize - 1) {
				condition.append(" AND ");
			}
			i++;
		}
		if (condition.toString().contains("?"))
			query += condition;

		mklogger.debug("condition : " + query);
		mklogger.debug("query : " + query);

		if(page > 0){
			String pageLimit = MkConfigReader.Me().get("mkweb.restapi.search.opt.paging.limit");
			if(pageLimit == null)	pageLimit = "100";
			int pl = Integer.parseInt(pageLimit);
			int curStart = (pl * (page-1));
			if(curStart < 0 || page == 1)	curStart = 0;
			StringBuilder limitQuery = new StringBuilder(" LIMIT " );
			//2페이지면 앞에 100개 빼야함. (pl * page) - pl
			//pl * page - pl = pl*(page-1)
			//100 * 2 - 100
			//3페이지면 100 * 3 - 100
			limitQuery.append(String.valueOf(curStart)).append(", ").append(pl);

			query += limitQuery;
		}
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
				mklogger.error("(executeSELLike) psmt = this.dbCon.prepareStatement(" + query + ") :" + e.getMessage());
				return null;
			}
		} else {
			try {
				resultList = DA.executeSEL(true);
			} catch (SQLException e) {
				mkResponse.setCode(400);
				mkResponse.setMessage(e.getMessage());
				mklogger.error("(executeSELLike) psmt = this.dbCon.prepareStatement(" + query + ") :" + e.getMessage());
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
			String requestMethod, MkRestApiResponse mkResponse) {

		JSONObject resultObject = null;
		MkDbAccessor DA = new MkDbAccessor(sqlData.getDB());

		String service = pjData.getServiceName();
		String control = sqlData.getControlName();
		String befQuery = ConnectionChecker.regularQuery(control, service, true);
		String query = null;
		String[] inputKey = pjData.getData();

		int requestSize = jsonObject.size();

		if(inputKey.length != requestSize) {
			mklogger.error("You must eneter every column data.");
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

		/*
		query = (customTable == null) ? 
				createSQL("post", inputKey, null, inputValues, null, sqlData.getTableData().get("from").toString()) : //sqlData.getRawSql()[2]) :
				createSQL("post", inputKey, null, inputValues, null, customTable);
		 */
		query = createSQL("post", inputKey, null, inputValues, null, sqlData.getTableData().get("from").toString()); //sqlData.getRawSql()[2]) :
		DA.setRequestValue(inputValues);
		query = ConnectionChecker.setQuery(befQuery);
		mklogger.debug("�׷��� ������ : " + query);
		if(query == null) {
			mkResponse.setCode(500);
			mkResponse.setMessage("Server Error. Please contact Admin.");
			mklogger.error("Query is null. Please check API SQL configs");
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

		mklogger.debug("resultObject: " + resultObject);
		return resultObject;
	}

	private JSONObject doTaskPut(MkPageJsonData pjData, MkSqlJsonData sqlData, JSONObject jsonObject, JSONObject modifyObject, String mkPage,
			String MKWEB_SEARCH_ALL, String requestMethod, MkRestApiResponse mkResponse) {
		if(modifyObject == null) {
			mklogger.error("No modify data received.");
			mkResponse.setMessage("No modify data received.");
			mkResponse.setCode(400);
			return null;
		}
		JSONObject resultObject = null;
		String[] inputKey = pjData.getData();

		//		JSONObject getResult = doTaskGet(pjData, sqlData, jsonObject, mkPage, MKWEB_SEARCH_ALL, mkResponse, customTable);
		JSONObject getResult = doTaskGet(pjData, sqlData, jsonObject, mkPage, MKWEB_SEARCH_ALL, mkResponse, -1);
		MkDbAccessor DA = new MkDbAccessor(sqlData.getDB());
		String service = pjData.getServiceName();
		String control = sqlData.getControlName();
		String query = null;
//		String[] modifyValues = new String[modifyObject.size()];
//		int mvIterator = 0;
//		for(int i = 0; i < pjData.getData().length; i++) {
//			Object tik = modifyObject.get(inputKey[i]);
//			if(tik != null) {
//				modifyValues[mvIterator++] = tik.toString();
//			}
//		}
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

		/*
		query = (customTable == null) ?
				(createSQL(tempCrud, searchKey, jsonObject, modifyKey, modifyObject, sqlData.getTableData().get("from").toString())) : //sqlData.getRawSql()[2])):
				(createSQL(tempCrud, searchKey, jsonObject, modifyKey, modifyObject, customTable));
		 */
		query = (createSQL(tempCrud, searchKey, jsonObject, modifyKey, modifyObject, sqlData.getTableData().get("from").toString())); //sqlData.getRawSql()[2])): 
		if(query == null) {
			mkResponse.setCode(500);
			mkResponse.setMessage("Server Error. Please contact Admin.");
			mklogger.error("Query is null. Please check API SQL configs");
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

	private JSONObject doTaskDelete(MkPageJsonData pxData, MkSqlJsonData sqlData, JSONObject jsonObject, String mkPage, MkRestApiResponse mkResponse) {
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

		/*
		query = (customTable == null) ? 
						createSQL("delete", searchKeys, jsonObject, null, null, sqlData.getTableData().get("from").toString()) : //sqlData.getRawSql()[2]) :
						createSQL("delete", searchKeys, jsonObject, null, null, customTable);
		 */
		query = createSQL("delete", searchKeys, jsonObject, null, null, sqlData.getTableData().get("from").toString());  //sqlData.getRawSql()[2]) :

		mklogger.debug("delete query: " + query);
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
					String decodeResult = URLDecoder.decode(temp, StandardCharsets.UTF_8);
					String encodeResult = URLEncoder.encode(decodeResult, StandardCharsets.UTF_8);

					temp = (encodeResult.contentEquals(temp) ? decodeResult : temp);
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
				String decodeResult = URLDecoder.decode(temp, StandardCharsets.UTF_8);
				String encodeResult = URLEncoder.encode(decodeResult, StandardCharsets.UTF_8);

				temp = (encodeResult.contentEquals(temp) ? decodeResult : temp);

				targetColumns += temp;
				if(i < searchKey.length - 1)
					targetColumns += ", ";
			}
			for(int i = 0; i < modifyKey.length; i++) {
				String temp = modifyKey[i];
				String decodeResult = URLDecoder.decode(temp, StandardCharsets.UTF_8);
				String encodeResult = URLEncoder.encode(decodeResult, StandardCharsets.UTF_8);

				temp = (encodeResult.contentEquals(temp) ? decodeResult : temp);

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
				String decodeResult = URLDecoder.decode(temp, StandardCharsets.UTF_8);
				String encodeResult = URLEncoder.encode(decodeResult, StandardCharsets.UTF_8);

				temp = (encodeResult.contentEquals(temp) ? decodeResult : temp);

				targetColumns += temp;

				temp = modifyObject.get(modifyKey[i]).toString();
				decodeResult = URLDecoder.decode(temp, StandardCharsets.UTF_8);
				encodeResult = URLEncoder.encode(decodeResult, StandardCharsets.UTF_8);

				temp = (encodeResult.contentEquals(temp) ? decodeResult : temp);
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