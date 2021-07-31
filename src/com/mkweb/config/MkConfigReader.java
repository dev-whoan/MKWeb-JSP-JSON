package com.mkweb.config;

import java.io.File;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import com.mkweb.logger.MkLogger;
import com.mkweb.utils.MkCrypto;

public class MkConfigReader {
	private static final HashMap<String, String> urlPatterns = new HashMap<>();
	private static MkConfigReader mcr = null;
	private Properties properties = null;
	private File configFile = null;
	private long lastModified = 0L;
	private static final String TAG = "[MkConfigReader]";
	private static final MkLogger mklogger = new MkLogger(TAG);

	private String[] urlKeys = {
			"mkweb.auth.uri",
			"mkweb.ftp.uri",
			"mkweb.web.receive.uri",
			"mkweb.restapi.uri"
	};

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

			mklogger.debug("properties: " + properties.getProperty("alpha"));

			for(int i = 0; i < urlKeys.length; i++){
				String value = properties.getProperty(urlKeys[i]);
				if(value != null){
					urlPatterns.put(urlKeys[i], value);
				} else {
					urlPatterns.put(urlKeys[i], MkCrypto.SHA256(String.valueOf(System.currentTimeMillis())));
				}
			}

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
