package com.mkweb.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.mkweb.data.MkJsonData;
import com.mkweb.data.MkAuthTokenData;
import com.mkweb.logger.MkLogger;

public class MkAuthTokenConfigs {
	private HashMap<String, ArrayList<MkAuthTokenData>> authToken_configs = new HashMap<String, ArrayList<MkAuthTokenData>>();
	private File defaultFile = null;
	private static MkAuthTokenConfigs matd = null;
	private long lastModified = -1L;
	private static final String TAG = "[AuthToken Configs]";
	private static final MkLogger mklogger = new MkLogger(TAG);

	public static MkAuthTokenConfigs Me() {
		if(matd == null) 
			matd = new MkAuthTokenConfigs();

		return matd;
	}

	public void setAuthTokenConfigs(File authTokenConfigs) {
		authToken_configs.clear();
		defaultFile = authTokenConfigs;
		ArrayList<MkAuthTokenData> ftpJsonData = null;
		lastModified = defaultFile.lastModified();

		mklogger.info("=*=*=*=*=*=*=* MkWeb Auth Configs Start*=*=*=*=*=*=*=*=");

		if(defaultFile == null || !defaultFile.exists())
		{
			mklogger.error("Config file is not exists or null");
			mklogger.error("Fail to setting MkAuthToken!");
			return;
		}

		mklogger.info("File: " + defaultFile.getAbsolutePath());
		mklogger.info("=            " + defaultFile.getName() +"             =");


		try(FileReader reader = new FileReader(defaultFile)){
			ftpJsonData = new ArrayList<MkAuthTokenData>();
			JSONParser jsonParser = new JSONParser();
			JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
			JSONObject ftpObject = (JSONObject) jsonObject.get("Controller");

			String ftpName = ftpObject.get("name").toString();
			String ftpControllerPath = ftpObject.get("path").toString();		

			String ftpDebugLevel = ftpObject.get("debug").toString();

			JSONArray serviceArray = (JSONArray) ftpObject.get("services");

			for(int i = 0; i < serviceArray.size(); i++) {
				JSONObject serviceObject = (JSONObject) serviceArray.get(i);
				String serviceId = null;
				String servicePath = null;
				String serviceDirPrefix = null;	//"dir"
				String serviceType = null;
				boolean serviceHashDirPrefix = false;

				String[] serviceAllowFileFormat = null;
				try {
					serviceId = serviceObject.get("id").toString();
					serviceType = serviceObject.get("type").toString();
					servicePath = serviceObject.get("servicepath").toString();
					Object prefix = serviceObject.get("dir");
					mklogger.debug("prefix : " + prefix);
					if(prefix != null) {
						serviceDirPrefix = prefix.toString();
						serviceHashDirPrefix = serviceObject.get("hash_dir").toString().contentEquals("true");
					}

					if(ftpControllerPath.charAt(ftpControllerPath.length() -1) == '/') {
						servicePath = (servicePath.charAt(0) == '/' ? (ftpControllerPath.substring(0, ftpControllerPath.length()-1) + servicePath) : (ftpControllerPath + servicePath));
					}else {
						servicePath = (servicePath.charAt(0) == '/' ? (ftpControllerPath + servicePath) : (ftpControllerPath + "/" + servicePath));
					}

					MkJsonData mjd = new MkJsonData(serviceObject.get("format").toString());
					if(!mjd.setJsonObject()) {
						mklogger.debug("Failed to set MkJsonObject service name : " + serviceId);
						return;
					}

					JSONObject serviceFormatData = mjd.getJsonObject();
					serviceAllowFileFormat = new String[serviceFormatData.size()];
					for(int j = 0; j < serviceAllowFileFormat.length; j++) {
						serviceAllowFileFormat[j] = serviceFormatData.get("" + (j+1)).toString();
					}

				} catch(Exception e) {
					mklogger.debug("Failed to create ftp controller. " + e.getMessage());
					e.printStackTrace();
					return;
				}

				MkAuthTokenData result = new MkAuthTokenData();
				/*
				result.setControlName(ftpName);
				result.setServiceType(serviceType);
				result.setPath(servicePath);
				result.setDebugLevel(ftpDebugLevel);
				result.setServiceName(serviceId);
				result.setData(serviceAllowFileFormat);
				result.setDirPrefix(serviceDirPrefix);
				result.setHashDirPrefix(serviceHashDirPrefix);
				 */
				ftpJsonData.add(result);
				printAuthTokenInfo(result, "info");
			}

			authToken_configs.put(ftpName, ftpJsonData);

		} catch (FileNotFoundException e) {
			mklogger.error(e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			mklogger.error(e.getMessage());
			e.printStackTrace();
		} catch (ParseException e) {
			mklogger.error(e.getMessage());
			e.printStackTrace();
		}
		mklogger.info("=*=*=*=*=*=*=* MkWeb Auth Configs  Done*=*=*=*=*=*=*=*=");
	}

	public void printAuthTokenInfo(MkAuthTokenData jsonData, String type) {
		String tempMsg = "\n===============================FTP  Control================================="
				+ "\n|Controller:\t" + jsonData.getControlName()
				+ "\n|FTP ID:\t" + jsonData.getServiceName() + "\t\tFTP Type:\t" + jsonData.getServiceType()
				+ "\n|FTP Path:\t" + jsonData.getPath()
				+ "\n|FTP Prefix:\t" + jsonData.getDirPrefix()
				+ "\n|Debug Level:\t" + jsonData.getDebugLevel()
				+ "\n|File Formats:\t" + jsonData.getData()
				+ "\n============================================================================";

		mklogger.temp(tempMsg, false);
		mklogger.flush(type);
	}

	private void reloadControls() {
		if(lastModified != defaultFile.lastModified()){
			mklogger.info("===========Reload AuthToken Config files===========");
			mklogger.info("========Caused by : different modified time========");
			setAuthTokenConfigs(defaultFile);
			mklogger.info("==========Reloaded AuthToken Config files==========");
		}
	}

	public ArrayList<MkAuthTokenData> getControl(String controlName) {
		reloadControls();
		return authToken_configs.get(controlName);
	}

	public ArrayList<MkAuthTokenData> getControlByServiceName(String serviceName){
		reloadControls();

		Set<String> keys = authToken_configs.keySet();
		Iterator<String> iter  = keys.iterator();
		String resultControlName = null;
		ArrayList<MkAuthTokenData> jsonData = null;
		while(iter.hasNext()) {
			String controlName = iter.next();
			jsonData = getControl(controlName);
			for(MkAuthTokenData curData : jsonData) {
				if(serviceName.contentEquals(curData.getServiceName())) {
					resultControlName = controlName;
					break;
				}
			}

			if(resultControlName != null) {
				break;
			}
			jsonData = null;
		}

		return jsonData;
	}

}
