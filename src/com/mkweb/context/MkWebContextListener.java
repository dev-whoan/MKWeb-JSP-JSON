package com.mkweb.context;

import java.io.File;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;

import com.mkweb.config.*;
import com.mkweb.core.MkAuthorizeGuard;
import com.mkweb.core.MkFileReceiver;
import com.mkweb.core.MkHttpSQLExecutor;
import com.mkweb.logger.MkLogger;
import com.mkweb.restapi.MkRestApi;;

public class MkWebContextListener implements ServletContextListener {

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		// TODO Auto-generated method stub
		ServletContext context = event.getServletContext();
		String mkwebProperties = context.getInitParameter("MKWeb.Properties");
		String sqlConfigsUri = context.getInitParameter("MKWeb.SqlConfigs");
		String MkLoggerUri = context.getInitParameter("MKWeb.LoggerConfigs");
		String pageConfigsUri = context.getInitParameter("MKWeb.PageConfigs");
		String ftpConfigsUri = context.getInitParameter("MKWeb.FTPConfigs");
		String apiSqlConfigs = context.getInitParameter("MKWeb.ApiSqlConfigs");
		String apiPageConfigs = context.getInitParameter("MKWeb.ApiPageConfigs");
		String authConfigsUri = context.getInitParameter("MKWeb.AuthConfigs");
		
		/*
		 * Setting Mk Logger Configure
		 */
		File mkweb_logger_config = new File(new File(context.getRealPath("/")), MkLoggerUri);
		MkLogger ml = new MkLogger(mkweb_logger_config);
		/*
		 * Read Mk Configs
		 */
		File mkweb_properties = new File(new File(context.getRealPath("/")), mkwebProperties);
		MkConfigReader mcr = MkConfigReader.Me();
		mcr.setMkConfig(mkweb_properties);
		//		MkConfigReader.setMkConfig(mkweb_properties);

		/*
		 * Setting SQL
		 */
		File mkweb_sql_config = new File(new File(context.getRealPath("/")), sqlConfigsUri);
		File[] config_sqls = mkweb_sql_config.listFiles();
		MkSQLConfigs sjc = MkSQLConfigs.Me();
		sjc.setSqlConfigs(config_sqls);
		/*
		 * Setting Pages
		 */
		File mkweb_page_config = new File(new File(context.getRealPath("/")), pageConfigsUri);
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
		 * FTP Server Settings
		 */
		if(MkConfigReader.Me().get("mkweb.ftp.use").contentEquals("yes")) {
			File mkweb_ftp_configs = new File(new File(context.getRealPath("/")), ftpConfigsUri);
			File[] config_ftps = mkweb_ftp_configs.listFiles();
			MkFTPConfigs fjc = MkFTPConfigs.Me();
			fjc.setPrefix(event.getServletContext().getRealPath("/"));
			fjc.setFtpConfigs(config_ftps);
			ServletRegistration.Dynamic registration = context.addServlet("MkFTPServlet", new MkFileReceiver());
			registration.addMapping(MkConfigReader.Me().get("mkweb.ftp.uri"));
		}

		/*
		 *  Rest Api Settings
		*/
		if(MkConfigReader.Me().get("mkweb.restapi.use").contentEquals("yes")) {
			File mkweb_apisql_config = new File(new File(context.getRealPath("/")), apiSqlConfigs);
			File[] config_api_sqls = mkweb_apisql_config.listFiles();
			MkRestApiSqlConfigs mrasc = MkRestApiSqlConfigs.Me();
			mrasc.setSqlConfigs(config_api_sqls);
			
			File mkweb_apipage_config = new File(new File(context.getRealPath("/")), apiPageConfigs);
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

			ServletRegistration.Dynamic registration = context.addServlet("MkRestApiServlet", MkRestApi.class);
			registration.addMapping("/" + MkConfigReader.Me().get("mkweb.restapi.uri") + "/*");
		}
		if(MkConfigReader.Me().get("mkweb.auth.use").contentEquals("yes")){
			File mkAuthProperties = new File(new File(context.getRealPath("/")), authConfigsUri);
			if(mkAuthProperties.exists()){
				MkAuthTokenConfigs matc = MkAuthTokenConfigs.Me();
				matc.setAuthTokenConfigs(mkAuthProperties);

				ServletRegistration.Dynamic registration = context.addServlet("MkAuthorizeGuard", MkAuthorizeGuard.class);
				registration.addMapping(MkConfigReader.Me().get("mkweb.auth.uri"));
			}

		}
		if(MkConfigReader.Me().get("mkweb.web.receive.use").contentEquals("yes")){
			ServletRegistration.Dynamic registration = context.addServlet("MkHttpSQLExecutor", MkHttpSQLExecutor.class);
			registration.addMapping(MkConfigReader.Me().get("mkweb.web.receive.uri"));
		}


	}
}
