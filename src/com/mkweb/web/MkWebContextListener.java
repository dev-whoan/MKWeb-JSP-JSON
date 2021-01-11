package com.mkweb.web;

import java.io.File;
import java.util.ArrayList;

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

		int size = config_pages.length;
		for(int i = 0; i < size; i++) {
			File currentFile = config_pages[i];
			if(currentFile.isDirectory()) {
				File[] oldFiles = new File[config_pages.length-1];
				File[] newFiles = currentFile.listFiles();
				int oldLength = oldFiles.length;
				int newLength = newFiles.length;
				
				if(i == 0) {
					System.arraycopy(config_pages, 1, oldFiles, 0, config_pages.length-1);
				}else {
					System.arraycopy(config_pages, 0, oldFiles, 0, i);
					System.arraycopy(config_pages, i+1, oldFiles, i, config_pages.length-(i+1));
				}
				config_pages = new File[oldLength + newLength];
				System.arraycopy(oldFiles, 0, config_pages, 0, oldLength);
				System.arraycopy(newFiles, 0, config_pages, oldLength, newLength);

				i--;
				size = config_pages.length;
			}
		}
		
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
			
			size = config_api_pages.length;
			for(int i = 0; i < size; i++) {
				File currentFile = config_api_pages[i];
				if(currentFile.isDirectory()) {
					File[] oldFiles = new File[config_api_pages.length-1];
					File[] newFiles = currentFile.listFiles();
					int oldLength = oldFiles.length;
					int newLength = newFiles.length;
					if(i == 0) {
						System.arraycopy(config_api_pages, 1, oldFiles, 0, config_api_pages.length-1);
					}else {
						System.arraycopy(config_api_pages, 0, oldFiles, 0, i);
						System.arraycopy(config_api_pages, i+1, oldFiles, i, config_api_pages.length-(i+1));
					}
					config_api_pages = new File[oldLength + newLength];
					System.arraycopy(oldFiles, 0, config_api_pages, 0, oldLength);
					System.arraycopy(newFiles, 0, config_api_pages, oldLength, newLength);

					i--;
					size = config_api_pages.length;
				}
			}
			
			MkRestApiPageConfigs mrac = MkRestApiPageConfigs.Me();
			mrac.setPageConfigs(config_api_pages);
		}
		
	}
}
