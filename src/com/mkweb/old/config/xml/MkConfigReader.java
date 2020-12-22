package com.mkweb.old.config.xml;

import java.io.File;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.mkweb.logger.MkLogger;

public class MkConfigReader {
	private static MkConfigReader mcr = null;
	private Properties properties = null;
	private File configFile = null;
	private long lastModified = 0L;
	private MkLogger mklogger = MkLogger.Me();
	
	public static MkConfigReader Me() {
		if(mcr == null)
			mcr = new MkConfigReader();
		return mcr;
	}
	
	public void setMkConfig(File defaultFile) {
		configFile = defaultFile;
		lastModified = configFile.lastModified();
		try {
			FileInputStream reader = new FileInputStream(configFile);
			properties = new Properties();
			properties.load(reader);
			
			reader.close();
			
		}catch( IOException e) {
			mklogger.error("ERROR OCCURED" + e);
		}
	}
	
	public String get(String key) {
		if(lastModified != configFile.lastModified())
		{
			setMkConfig(configFile);
			mklogger.info("==============Reload Mk Config files==============");
			mklogger.info("========Caused by: different modified time========");
			mklogger.info("==============Reload Mk Config files==============");
		}
		if(properties != null) {
			return this.properties.getProperty(key) != null ? this.properties.getProperty(key).toString() : null;
		}else {
			mklogger.error("Mk Config didn't set!");
			return null;
		}
	}
}
