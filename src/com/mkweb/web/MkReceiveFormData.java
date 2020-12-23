package com.mkweb.web;

import java.io.IOException;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mkweb.data.PageJsonData;
import com.mkweb.database.MkDbAccessor;
import com.mkweb.logger.MkLogger;
import com.mkweb.config.MkConfigReader;
import com.mkweb.config.MkPageConfigs;
import com.mkweb.security.CheckPageInfo;

/**
 * Servlet implementation class MkReceiveFormData
 **/

@WebServlet(
	name = "MkReceiveFormData",
	loadOnStartup=1
)

public class MkReceiveFormData extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private MkLogger mklogger = MkLogger.Me();
	private String TAG = "[MkReceiveFormData]";
    private PageJsonData pjData = null;
    
    private ArrayList<PageJsonData> pi = null;
    private boolean isPiSet = false;
	PageJsonData pageStaticData = null;
	ArrayList<String> requestServiceName = null;
	ArrayList<String> pageParameter = null;
	String pageObjectType = null;
	String pageMethod = null;
	ArrayList<LinkedHashMap<String, Boolean>> pageValue = null;
	
    private String requestParams = null;
    private ArrayList<String> requestValues = null;
    private CheckPageInfo cpi = null;
    public MkReceiveFormData() {
        super();
        cpi = new CheckPageInfo();
    }
	
	private ArrayList<PageJsonData> getPageControl(String url) {
		String[] requestUriList = url.split("/");
		String mkPage = requestUriList[requestUriList.length - 1];
		
		if(mkPage.equals(MkConfigReader.Me().get("mkweb.web.hostname"))) {
			mkPage = "";
		}
		
		mklogger.debug(TAG, "receive mkpage : " + mkPage);
		
		return MkPageConfigs.Me().getControl(mkPage);		
	}
    
    private boolean checkMethod(HttpServletRequest request, String rqMethod, String rqPageURL) {
		String hostCheck = rqPageURL.split("://")[1];
		String host = MkConfigReader.Me().get("mkweb.web.hostname");
		
    	if(host == null) {
    		mklogger.error(TAG, " Hostname is not set. You must set hostname on configs/MkWeb.conf");
    		return false;
    	}
    	host = host + "/";
    	String requestURI = rqPageURL.split(MkConfigReader.Me().get("mkweb.web.hostname"))[1];
		String[] reqPage = null;
		String mkPage = null;
	
		if(!hostCheck.equals(host))
		{
			reqPage = requestURI.split("/");
			mkPage = reqPage[reqPage.length - 1];
		}else {
			reqPage = null;
			mkPage = "";
		}

		if(!cpi.isValidPageConnection(mkPage, reqPage)) {
			mklogger.error(TAG, " checkMethod: Invalid Page Connection.");
			return false;
		}
    	
		if(pi == null || !isPiSet) {
			mklogger.error(TAG, " PageInfo is not set!");
			return false;
		}
		
		requestParams = cpi.getRequestPageParameterName(request, pageStaticData);
		
		ArrayList<PageJsonData> pal = MkPageConfigs.Me().getControl(mkPage);
		for(PageJsonData pj : pal) {
			if(pj.getParameter().equals(requestParams)) {
				pjData = pj;
				break;
			}
		}
		
		mklogger.debug(TAG, " method: " + pjData.getMethod());
		
		if(!pjData.getMethod().toLowerCase().contentEquals(rqMethod)) {
			return false;
		}

		requestValues = cpi.getRequestParameterValues(request, pjData.getParameter(), pageStaticData);

    	return (pjData != null ? true : false);
    }
    
    private void doTask(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	MkDbAccessor DA = new MkDbAccessor();
		
		if(!cpi.comparePageValueWithRequestValue(pjData.getPageValue(), requestValues, pageStaticData, false)) {
			mklogger.error(TAG, " Request Value is not authorized. Please check page config.");
			response.sendError(400);
			return;
		}
		
		String control = pjData.getControlName();
		String service = pjData.getServiceName();
		
		mklogger.debug(TAG, "control : " + control + "| service : " + service);
		
		String befQuery = cpi.regularQuery(control, service, false);
		String query = cpi.setQuery(befQuery);
		
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
			
			DA.executeDML();
		}
    }
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String refURL = request.getHeader("Referer");
		pi = getPageControl(refURL);
		isPiSet = (pi != null);
		pageStaticData = null;

		mklogger.debug(TAG, " isPiSet : " + isPiSet);
		
		if(isPiSet) {
			for(int i = 0; i < pi.size(); i++) {
				if(pi.get(i).getPageStatic()) {
					pageStaticData = pi.get(i);
					break;
				}
			}
			mklogger.debug(TAG, "pagestaticdata: " +pageStaticData);
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
			mklogger.error(TAG, " Request method is not authorized. [Tried: GET]");
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
		mklogger.debug(TAG, " isPiSet : " + isPiSet);
		
		if(isPiSet) {
			for(int i = 0; i < pi.size(); i++) {
				if(pi.get(i).getPageStatic()) {
					pageStaticData = pi.get(i);
					break;
				}
			}
			mklogger.debug(TAG, "pagestaticdata: " +pageStaticData);
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
			mklogger.error(TAG, " Request method is not authorized. [Tried: POST]");
			response.sendError(401);
			return;
		}
		doTask(request, response);
	}
}
