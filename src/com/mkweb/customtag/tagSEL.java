package com.mkweb.customtag;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import com.mkweb.data.MkPageJsonData;
import com.mkweb.data.MkSqlJsonData;
import com.mkweb.database.MkDbAccessor;
import com.mkweb.logger.MkLogger;
import com.mkweb.utils.ConnectionChecker;
import com.mkweb.config.MkPageConfigs;
import com.mkweb.config.MkSQLConfigs;

public class tagSEL extends SimpleTagSupport {
	private String obj;
	private String like = "yes";
	private String name = "name";
	private String id = "id";
	private static final String TAG = "[tagSEL]";
	private static final MkLogger mklogger = new MkLogger(TAG);
	//Log ?���?
	public void setObj(String obj) {
		this.obj = obj;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setLike(String like) {
		this.like = like;
	}

	public String getName() {
		return this.name;
	}

	private ArrayList<MkPageJsonData> getPageControl(HttpServletRequest request) {
		Object o = request.getAttribute("mkPage");
		if(o == null) {	return null;	}

		String controlName = o.toString();
		return MkPageConfigs.Me().getControl(controlName);
	}

	private ArrayList<MkSqlJsonData> getSqlControl(String sqlControlName){
		return MkSQLConfigs.Me().getControl(sqlControlName);
	}

	public void doTag() throws JspException, IOException{
		MkDbAccessor DA;
		ArrayList<Object> dbResult = new ArrayList<Object>();

		HttpServletRequest request = (HttpServletRequest) ((PageContext)getJspContext()).getRequest();
		HttpServletResponse response = (HttpServletResponse) ((PageContext)getJspContext()).getResponse();
		
		request.setCharacterEncoding("UTF-8");

		String requestParams = null;
		ArrayList<String> requestValues = new ArrayList<String>();

		ArrayList<MkPageJsonData> pageInfo = getPageControl(request);
		ArrayList<MkSqlJsonData> sqlInfo = getSqlControl(this.name);

		/*
		Should check Auths
		 */
		boolean isSet = (pageInfo == null || pageInfo.size() == 0) ? false : true;
		MkPageJsonData pageStaticData = null;

		if(isSet) {
			for(int i = 0; i < pageInfo.size(); i++) {
				if(pageInfo.get(i).getPageStatic()) {
					pageStaticData = pageInfo.get(i);
					break;
				}
			}
		}
		
		int pageServiceIndex = -1;
		boolean pageServiceFound = false;
		for(MkPageJsonData pjd : pageInfo) {
			pageServiceIndex++;
			if(this.id.contentEquals(pjd.getServiceName())) {
				pageServiceFound = true;
				break;
			}
		}
		if(!pageServiceFound)
			pageServiceIndex = -1;

		int sqlControlIndex = -1;
		boolean sqlControlFound = false;
		for(MkSqlJsonData sjd : sqlInfo) {
			sqlControlIndex++;
			if(this.name.contentEquals(sjd.getControlName())) {
				sqlControlFound = true;
				break;
			}
		}
		if(!sqlControlFound)
			sqlControlIndex = -1;

		if(pageServiceIndex == -1) {
			mklogger.error(" Tag 'id(" + this.id + ")' is not matched with any page service 'type:id'.");
			//	response.sendError(500);
			return;
		}

		if(sqlControlIndex == -1) {
			mklogger.error(" Tag 'name(" + this.name + ")' is not matched with any SQL controller. Please check SQL configs.");
			return;
		}

		requestParams = ConnectionChecker.getRequestPageParameterName(request, pageInfo.get(pageServiceIndex).getPageStatic(), pageStaticData,
				pageInfo.get(pageServiceIndex).getParameter(), pageInfo.get(pageServiceIndex).getData().length);

		requestValues = ConnectionChecker.getRequestParameterValues(request, pageInfo.get(pageServiceIndex).getParameter(), pageStaticData);

		if(!ConnectionChecker.comparePageValueWithRequestValue(
				pageInfo.get(pageServiceIndex).getPageValue(),
				requestValues,
				pageStaticData,
				pageInfo.get(pageServiceIndex).getPageStatic(),
				false)
				){
			mklogger.error(" Request Value is not authorized. Please check page config.");
			//	response.sendError(500);
			return;
		}

		LinkedHashMap<String, Boolean> pvHash = pageInfo.get(pageServiceIndex).getPageValue();
		requestValues.clear();
		
		if(pvHash != null && pvHash.size() > 0) {
			Set<String> pvEntrySet = pvHash.keySet();
			Iterator<String> pvIter = pvEntrySet.iterator();

			while(pvIter.hasNext()) {
				String pvKey = pvIter.next();		
				if(requestValues == null)
					requestValues = new ArrayList<>();
				
				requestValues.add(pvKey);
			}
		}

		String befQuery = ConnectionChecker.regularQuery(sqlInfo.get(sqlControlIndex).getControlName(), pageInfo.get(pageServiceIndex).getServiceName(), false);

		String query = null;
		query = ConnectionChecker.setQuery(befQuery);
		if(query == null)
			query = befQuery;

		String targetDB = sqlInfo.get(sqlControlIndex).getDB();
		mklogger.info("=====Service[" + this.id +"]=====");
		if(this.obj == "list")
		{
			DA = new MkDbAccessor(targetDB);
			DA.setPreparedStatement(query);
			if(requestValues != null) {
				String[] reqs = new String[requestValues.size()];
				String tempValue = "";
				for(int i = 0; i < reqs.length; i++) {
					tempValue = request.getParameter(requestParams + "." + requestValues.get(i));
					if(tempValue == null)
						tempValue = request.getParameter(requestValues.get(i));

					String temp = tempValue;
					try {
						mklogger.debug("temp : " + temp + " | tempValue : " + tempValue);
						String decodeResult = URLDecoder.decode(temp, "UTF-8");
						String encodeResult = URLEncoder.encode(decodeResult, "UTF-8");
						
						tempValue = (encodeResult.contentEquals(temp) ? decodeResult : temp);
					} catch (UnsupportedEncodingException e) {
						//
						mklogger.error("(obj == list) given data (" + temp + ") is invalid! " + e.getMessage());
					}
					
					if(this.like.equals("no"))
					{
						if(tempValue.contains("%"))
							tempValue = tempValue.replaceAll("%", " ");

						reqs[i] = tempValue;
					}else {
						reqs[i] = tempValue;
					}
				}
				tempValue = null;
				DA.setRequestValue(reqs);
				reqs = null;
			}
			
			if(this.like.equals("no")) {
				try {
					dbResult = DA.executeSEL(false);
				}catch (SQLException e) {
					mklogger.error("(executeSELLike) psmt = this.dbCon.prepareStatement(" + query + ") :" + e.getMessage());
				}
			}
			else {
				try {
					dbResult = DA.executeSELLike(false);
				}catch (SQLException e) {
					mklogger.error("(executeSELLike) psmt = this.dbCon.prepareStatement(" + query + ") :" + e.getMessage());
				}	
			}
			
			LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();

			if(dbResult != null && dbResult.size() > 0)
			{
				for(int i = 0; i < dbResult.size(); i++)
				{
					result = (LinkedHashMap<String, Object>) dbResult.get(i);
					((PageContext)getJspContext()).getRequest().setAttribute("mkw", result);
					getJspBody().invoke(null);
				}
				((PageContext)getJspContext()).getRequest().removeAttribute("mkw");
			}else {
				return;
			}
		}else if(this.obj =="map") {
			DA = new MkDbAccessor();
		}

	}
}
