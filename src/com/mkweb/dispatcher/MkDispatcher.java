package com.mkweb.dispatcher;

import java.io.IOException;


import java.util.ArrayList;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mkweb.auths.MkAuthToken;
import com.mkweb.config.MkConfigReader;
import com.mkweb.data.MkPageJsonData;
import com.mkweb.logger.MkLogger;
import com.mkweb.utils.ConnectionChecker;
import com.mkweb.config.MkPageConfigs;

/**
 * Servlet implementation class testMkDispatcher
 */
@WebServlet(
		name="MkWebDispatcher",
		loadOnStartup=1,
		urlPatterns="*.mkw"
		)
public class MkDispatcher extends HttpServlet {
	private static final long serialVersionUID = 1L;
	//	private static final String[] deviceFilter = {"win16","win32","win64","mac","macintel"};
	private static final String[] deviceFilter = {"ipad", "iphone", "ipod", "android"};
	private static final String TAG = "[MkDispatcher]";
	private static final MkLogger mklogger = new MkLogger(TAG);
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public MkDispatcher() {
		super();
		// TODO Auto-generated constructor stub
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String requestURI = request.getRequestURI();
		String clientAddress = request.getAttribute("client-host").toString();
		Object o = request.getAttribute("mkPage");
		if(o == null) {
			mklogger.error("Request URI is invalid. ( Unauthorzied connection [" + requestURI + "] )");
			response.sendError(400);
			return;
		}
		String mkPage = request.getAttribute("mkPage").toString();
		if(!ConnectionChecker.isValidPageConnection(mkPage)) {
			response.sendError(404);
			return;
		}
		ArrayList<MkPageJsonData> resultPageData = MkPageConfigs.Me().getControl(mkPage);

		String userAcceptLanguage = request.getHeader("Accept-Language");
		String userAgent = request.getHeader("User-Agent").toLowerCase();
		String userPlatform = null;

		for(int i = 0; i < deviceFilter.length; i++) {
			if(userAgent.contains(deviceFilter[i])) {
				if(i <= 2) {
					userPlatform = "ios";
				}else {
					userPlatform = "android";
				}
			}
		}
		if(userPlatform == null)
			userPlatform = "desktop";

		String targetURI = MkPageJsonData.getAbsPath() + (ConnectionChecker.getRequestPageLanguage(mkPage, userPlatform, userAcceptLanguage, resultPageData));
		if(targetURI.contains("error_")) {
			response.sendError(Integer.parseInt(targetURI.split("error_")[1]));
			return;
		}

		//  For each service, the service executor will ask authorize.

		if(!ConnectionChecker.isPageAuthorized(request, response, resultPageData)){
			if(MkConfigReader.Me().get("mkweb.auth.redirect.use").contentEquals("yes")){
				response.sendRedirect(MkConfigReader.Me().get("mkweb.auth.redirect.uri"));
			} else {
				response.sendError(401);
			}

			return;
		}
		request.setAttribute("mkPage", mkPage);

		mklogger.debug("cookie: " + request.getCookies().length);
		MkAuthToken.printCookies(request.getCookies());
		mklogger.info("Page Called by " + clientAddress);
		MkPageConfigs.Me().printPageInfo(mklogger, resultPageData.get(0), "no-sql");
		dispatch(request, response, targetURI);
	}

	private void dispatch(HttpServletRequest request, HttpServletResponse response, String URI) throws ServletException, IOException {
		RequestDispatcher dispatcher = this.getServletContext().getRequestDispatcher(URI);
		dispatcher.forward(request, response);
	}

}
