package com.mkweb.web;

import java.io.IOException;



import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.servlet.RequestDispatcher;
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
    private PageJsonData pxData = null;
    
    private ArrayList<PageJsonData> pi = null;
    private boolean isPiSet = false;
	boolean isSet = false;
	ArrayList<String> pageStaticParams = null;
	String pageStaticParamsName = null;
	ArrayList<String> requestServiceName = null;
	ArrayList<String> pageParameter = null;
	ArrayList<String[]> pageSqlInfo = null;
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
		
		requestParams = cpi.getRequestPageParameterName(request, pageStaticParams, pageStaticParamsName);
		
		ArrayList<PageJsonData> pal = MkPageConfigs.Me().getControl(mkPage);
		for(PageJsonData px : pal) {
			if(px.getParameter().equals(requestParams)) {
				pxData = px;
				break;
			}
		}
		
		mklogger.debug(TAG, " method: " + pxData.getSql()[2]);
		
		if(!pxData.getSql()[2].toLowerCase().contentEquals(rqMethod)) {
			return false;
		}
		
		requestValues = cpi.getRequestParameterValues(request, pxData.getParameter(), pageStaticParams, pageStaticParamsName);

    	return (pxData != null ? true : false);
    }
    
    private void doTask(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	MkDbAccessor DA = new MkDbAccessor();
		
		if(!cpi.comparePageValueWithRequestValue(pxData.getPageValue(), requestValues, pxData.getPageStaticParams(), false)) {
			mklogger.error(TAG, " Request Value is not authorized. Please check page config.");
			response.sendError(400);
			return;
		}
		
		String service = pxData.getServiceName();
		String befQuery = cpi.regularQuery(service, false);
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
		
		mklogger.debug(TAG, " isPiSet : " + isPiSet);
		
		if(isPiSet) {
			pageStaticParams = new ArrayList<>();
			pageStaticParamsName = "";
			pageParameter = new ArrayList<>();
			pageSqlInfo = new ArrayList<>();
			pageValue = new ArrayList<>();
			requestServiceName = new ArrayList<>();
			//pageConfig Parameters
			for(int i = 0; i < pi.size(); i++) {
				pageParameter.add(pi.get(i).getParameter());
				pageSqlInfo.add(pi.get(i).getSql());
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
		
		mklogger.debug(TAG, " isPiSet : " + isPiSet);
		
		if(isPiSet) {
			pageStaticParams = new ArrayList<>();
			pageStaticParamsName = "";
			pageParameter = new ArrayList<>();
			pageSqlInfo = new ArrayList<>();
			pageValue = new ArrayList<>();
			requestServiceName = new ArrayList<>();
			//pageConfig Parameters
			for(int i = 0; i < pi.size(); i++) {
				pageParameter.add(pi.get(i).getParameter());
				pageSqlInfo.add(pi.get(i).getSql());
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
