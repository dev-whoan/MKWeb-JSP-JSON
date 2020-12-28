package com.mkweb.web;

import java.io.File;


import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.mkweb.config.MkConfigReader;
import com.mkweb.config.MkPageConfigs;
import com.mkweb.config.MkRestApiPageConfigs;
import com.mkweb.config.MkRestApiSqlConfigs;
import com.mkweb.config.MkSQLConfigs;
import com.mkweb.logger.MkLogger;;

public class MkWebContextListener implements ServletContextListener {

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		// TODO Auto-generated method stub
		String mkwebProperties = event.getServletContext().getInitParameter("MKWeb.Properties");
		String sqlConfigsUri = event.getServletContext().getInitParameter("MKWeb.SqlConfigs");
		String MkLoggerUri = event.getServletContext().getInitParameter("MKWeb.LoggerConfigs");
		String pageConfigsUri = event.getServletContext().getInitParameter("MKWeb.PageConfigs");
		String apiSqlConfigs = event.getServletContext().getInitParameter("MkWeb.ApiSqlConfigs");
		String apiPageConfigs = event.getServletContext().getInitParameter("MkWeb.ApiPageConfigs");
		
		/*
		 * Setting Mk Logger Configure
		 */
		File mkweb_logger_config = new File(new File(event.getServletContext().getRealPath("/")), MkLoggerUri);
		MkLogger ml = MkLogger.Me();
		ml.setLogConfig(mkweb_logger_config);
		
		/*
		 * Read Mk Configs
		 */
		File mkweb_properties = new File(new File(event.getServletContext().getRealPath("/")), mkwebProperties);
		MkConfigReader mcr = MkConfigReader.Me();
		mcr.setMkConfig(mkweb_properties);
		//		MkConfigReader.setMkConfig(mkweb_properties);
		
		/*
		 * Setting SQL
		 */
		File mkweb_sql_config = new File(new File(event.getServletContext().getRealPath("/")), sqlConfigsUri);
		File[] config_sqls = mkweb_sql_config.listFiles();
		
		MkSQLConfigs sxc = MkSQLConfigs.Me();
		sxc.setSqlConfigs(config_sqls);
		
		/*
		 * Setting Pages
		 */
		File mkweb_page_config = new File(new File(event.getServletContext().getRealPath("/")), pageConfigsUri);
		File[] config_pages = mkweb_page_config.listFiles();
		
		MkPageConfigs pc = MkPageConfigs.Me();
		pc.setPageConfigs(config_pages);
		
		/*
		 *  Rest Api Settings
		*/
		
		if(MkConfigReader.Me().get("mkweb.restapi.use").equals("yes")) {
			File mkweb_apisql_config = new File(new File(event.getServletContext().getRealPath("/")), apiSqlConfigs);
			File[] config_api_sqls = mkweb_apisql_config.listFiles();
			MkRestApiSqlConfigs mrasc = MkRestApiSqlConfigs.Me();
			mrasc.setSqlConfigs(config_api_sqls);
			
			File mkweb_apipage_config = new File(new File(event.getServletContext().getRealPath("/")), apiPageConfigs);
			File[] config_api_pages = mkweb_apipage_config.listFiles();
			MkRestApiPageConfigs mrac = MkRestApiPageConfigs.Me();
			mrac.setPageConfigs(config_api_pages);
		}
		
	}
}
