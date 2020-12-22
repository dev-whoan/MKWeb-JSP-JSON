package com.mkweb.dispatcher;

import java.io.IOException;



import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mkweb.logger.MkLogger;
import com.mkweb.config.MkConfigReader;
import com.mkweb.security.CheckPageInfo;

/**
 * Servlet implementation class testDefDispatcher
 */
@WebServlet(
	name="DefaultDispatcher",
	loadOnStartup=1,
	urlPatterns="/"
)
public class defaultDispatcher extends HttpServlet {
	private static final long serialVersionUID = 1L;
    private MkLogger mklogger = MkLogger.Me();
    private String TAG = "[defaultDispatcher]";
    /**
     * @see HttpServlet#HttpServlet()
     */
    public defaultDispatcher() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doPost(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		String requestURI = request.getRequestURI();	// /main
		String[] reqPage = null;
		String mkPage = null;
		
		String hostcheck = request.getRequestURL().toString().split("://")[1];
		String host = MkConfigReader.Me().get("mkweb.web.hostname") + "/";

		if(!hostcheck.equals(host))
		{
			reqPage = requestURI.split("/");
			mkPage = reqPage[reqPage.length - 1];
		}else {
			reqPage = null;
			mkPage = "";
		}

		if(!(new CheckPageInfo()).isValidPageConnection(mkPage, reqPage)) {
			//에러페이지
			response.sendError(404);
			return;
		}
		
		request.setAttribute("mkPage", mkPage);
		RequestDispatcher dispatcher = this.getServletContext().getRequestDispatcher(requestURI + ".mkw");
		dispatcher.forward(request, response);
	}
}
