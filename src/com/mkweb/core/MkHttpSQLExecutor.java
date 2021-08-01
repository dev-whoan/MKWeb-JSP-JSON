package com.mkweb.core;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mkweb.config.MkSQLConfigs;
import com.mkweb.data.MkPageJsonData;
import com.mkweb.data.MkSqlJsonData;
import com.mkweb.database.MkDbAccessor;
import com.mkweb.logger.MkLogger;
import com.mkweb.utils.ConnectionChecker;
import com.mkweb.config.MkConfigReader;
import com.mkweb.config.MkPageConfigs;
import com.mkweb.utils.MkJsonData;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Servlet implementation class MkReceiveFormData
 **/

@WebServlet(
	name = "MkReceiveFormData",
	loadOnStartup=1
)

public class MkHttpSQLExecutor extends HttpServlet{
	private static final long serialVersionUID = 1L;
	
	private static final String TAG = "[MkHttpSQLExecutor]";
	private static final MkLogger mklogger = new MkLogger(TAG);
	
    private MkPageJsonData pjData = null;
    
    private ArrayList<MkPageJsonData> pi = null;
    private boolean isPiSet = false;
	MkPageJsonData pageStaticData = null;
	ArrayList<String> requestServiceName = null;
	ArrayList<String> pageParameter = null;
	String pageObjectType = null;
	String pageMethod = null;
	ArrayList<LinkedHashMap<String, Boolean>> pageValue = null;
	
    private String requestParams = null;
    private ArrayList<String> requestValues = null;
    public MkHttpSQLExecutor() {
        super();
    }
	
	private ArrayList<MkPageJsonData> getPageControl(String url) {
		String mkPage = url.split(MkConfigReader.Me().get("mkweb.web.hostname") )[1];//"/" + requestUriList[requestUriList.length - 1];

		mklogger.debug("receive mkpage : " + mkPage);
		
		return MkPageConfigs.Me().getControl(mkPage);		
	}
    
    private boolean checkMethod(HttpServletRequest request, String rqMethod, String rqPageURL) {
		String hostCheck = rqPageURL.split("://")[1];
		String host = MkConfigReader.Me().get("mkweb.web.hostname");
		
    	if(host == null) {
    		mklogger.error(" Hostname is not set. You must set hostname on configs/MkWeb.conf");
    		return false;
    	}
    	host = host + "/";
    	String requestURI = rqPageURL.split(MkConfigReader.Me().get("mkweb.web.hostname"))[1];
		String mkPage = (!hostCheck.contentEquals(host) ? requestURI : "");

		if(!ConnectionChecker.isValidPageConnection(mkPage)) {
			mklogger.error(" checkMethod: Invalid Page Connection.");
			return false;
		}
    	
		if(pi == null || !isPiSet) {
			mklogger.error(" PageInfo is not set!");
			return false;
		}
		
		requestParams = ConnectionChecker.getRequestPageParameterName(request, false, pageStaticData);
		
		ArrayList<MkPageJsonData> pal = MkPageConfigs.Me().getControl(mkPage);
		for(MkPageJsonData pj : pal) {
			if(pj.getParameter().equals(requestParams)) {
				pjData = pj;
				break;
			}
		}
		
		try {
			if(!pjData.getMethod().toLowerCase().contentEquals(rqMethod)) {
				return false;
			}
		} catch (NullPointerException e) {
			mklogger.error("There is no service for request parameter. You can ignore 'Request method is not authorized.' error.");
			mklogger.debug("Page Json Data is Null");
			return false;
		}
		
		requestValues = ConnectionChecker.getRequestParameterValues(request, pjData.getParameter(), pageStaticData);

    	return (pjData != null);
    }
    
    private void doTask(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	request.setCharacterEncoding("UTF-8");
    	MkDbAccessor DA = new MkDbAccessor();
		
		if(!ConnectionChecker.comparePageValueWithRequestValue(pjData.getPageValue(), requestValues, pageStaticData, false, false)) {
			mklogger.error(" Request Value is not authorized. Please check page config.");
			response.sendError(400);
			return;
		}
		
		String control = pjData.getControlName();
		String service = pjData.getServiceName();
		ArrayList<MkSqlJsonData> sqlServices = MkSQLConfigs.Me().getControlByServiceName(service);
		MkSqlJsonData sqlService = MkSQLConfigs.Me().getServiceInfoByServiceName(sqlServices, service);
		String serviceType = sqlService.getServiceType();
		
		mklogger.debug("control : " + control + "| service : " + service + "| type: " + serviceType);

		if(ConnectionChecker.isSqlAuthorized(request, response, sqlService)){
			String befQuery = ConnectionChecker.regularQuery(control, service, false);
			String query = ConnectionChecker.setQuery(befQuery);

			if(requestValues != null) {
				String[] reqs = new String[requestValues.size()];
				String tempValue = "";

				DA.setPreparedStatement(query);

				for(int i = 0; i < reqs.length; i++) {
					tempValue = request.getParameter(requestParams + "." + requestValues.get(i));
					reqs[i] = tempValue;
				}

				tempValue = null;
				DA.setRequestValue(reqs);
				reqs = null;

				try {
					if(serviceType.toLowerCase().contentEquals("select")){
						PrintWriter writer = response.getWriter();
						String message = "";
						int statusCode = 200;
						ArrayList<Object> dbResult = DA.executeSEL(true);
						JSONArray jsonArray = new JSONArray();
						if(dbResult == null || dbResult.size() == 0){
							statusCode = 204;
						} else {
							LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
							for(int i = 0; i < dbResult.size(); i++)
							{
								result = (LinkedHashMap<String, Object>) dbResult.get(i);
								jsonArray.add(i, MkJsonData.objectMapToJson(result));
							}
						}
						response.setStatus(statusCode);
						response.setContentType("application/json;charset=UTF-8");
						response.setCharacterEncoding("UTF-8");
						response.setHeader("Result", "HTTP/1.1 " + statusCode);
						writer.print(MkJsonData.removeQuoteFromJsonArray(jsonArray));
					} else {
						DA.executeDML();
					}
				} catch (SQLException e) {
					mklogger.error("(executeDML) psmt = this.dbCon.prepareStatement(" + query + ") :" + e.getMessage());
					response.setStatus(500);
				}
			}
		}
    }
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String refURL = request.getHeader("Referer");
		pi = getPageControl(refURL);
		isPiSet = (pi != null);
		pageStaticData = null;

		mklogger.debug(" isPiSet : " + isPiSet);
		
		if(isPiSet) {
			for(int i = 0; i < pi.size(); i++) {
				if(pi.get(i).getPageStatic()) {
					pageStaticData = pi.get(i);
					break;
				}
			}
			mklogger.debug("pagestaticdata: " +pageStaticData);
			pageParameter = new ArrayList<>();
			pageValue = new ArrayList<>();
			requestServiceName = new ArrayList<>();
			//pageConfig Parameters
			for(int i = 0; i < pi.size(); i++) {
				pageParameter.add(pi.get(i).getParameter());
				pageObjectType = pi.get(i).getObjectType();
				pageMethod = pi.get(i).getMethod();
				pageValue.add(pi.get(i).getPageValue());
				requestServiceName.add(pi.get(i).getServiceName());
			}
		}
		
		if(!checkMethod(request, "get", refURL)) {
			mklogger.error(" Request method is not authorized. [Tried: GET]");
			response.sendError(400);
			return;
		}
		
		doTask(request, response);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String refURL = request.getHeader("Referer");
		pi = getPageControl(refURL);
		isPiSet = (pi != null);
		pageStaticData = null;
		mklogger.debug(" isPiSet : " + isPiSet);
		
		if(isPiSet) {
			for(int i = 0; i < pi.size(); i++) {
				if(pi.get(i).getPageStatic()) {
					pageStaticData = pi.get(i);
					break;
				}
			}
			mklogger.debug("pagestaticdata: " +pageStaticData);
			pageParameter = new ArrayList<>();
			pageValue = new ArrayList<>();
			requestServiceName = new ArrayList<>();
			//pageConfig Parameters
			for(int i = 0; i < pi.size(); i++) {
				pageParameter.add(pi.get(i).getParameter());
				pageObjectType = pi.get(i).getObjectType();
				pageMethod = pi.get(i).getMethod();
				pageValue.add(pi.get(i).getPageValue());
				requestServiceName.add(pi.get(i).getServiceName());
			}
		}
		if(!checkMethod(request, "post", refURL)) {
			mklogger.error(" Request method is not authorized. [Tried: POST]");
			response.sendError(401);
			return;
		}
		doTask(request, response);
	}
}