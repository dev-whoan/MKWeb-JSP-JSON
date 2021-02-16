package com.mkweb.config;

import java.io.File;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.mkweb.data.MkFtpData;
import com.mkweb.data.MkJsonData;
import com.mkweb.logger.MkLogger;

public class MkFTPConfigs {
	private HashMap<String, ArrayList<MkFtpData>> ftp_configs = new HashMap<String, ArrayList<MkFtpData>>();
	private File[] defaultFiles = null;
	private static MkFTPConfigs mfd = null;
	private long[] lastModified = null;
	private MkLogger mklogger = MkLogger.Me();
	private String TAG = "[FTP Configs]";
	private String filePrefix = null;

	public static MkFTPConfigs Me() {
		if(mfd == null)
			mfd = new MkFTPConfigs();
		return mfd;
	}
	
	public void setPrefix(String filePrefix) {
		if(this.filePrefix == null)
			this.filePrefix = filePrefix;
	}
	
	public String getPrefix() {	return this.filePrefix;	}
	
	public void setFtpConfigs(File[] ftpConfigs) {
		ftp_configs.clear();
		defaultFiles = ftpConfigs;
		ArrayList<MkFtpData> ftpJsonData = null;
		lastModified = new long[ftpConfigs.length];
		int lmi = 0;
		boolean useAbsolute = (MkConfigReader.Me().get("mkweb.ftp.absolute").contentEquals("yes") ? true : false);
		for(File defaultFile : defaultFiles)
		{
			if(defaultFile.isDirectory())
				continue;
			
			lastModified[lmi++] = defaultFile.lastModified();
			mklogger.info("=*=*=*=*=*=*=* MkWeb FTP  Configs Start*=*=*=*=*=*=*=*=");
			mklogger.info(TAG + "File: " + defaultFile.getAbsolutePath());
			mklogger.info("=            " + defaultFile.getName() +"              =");
			if(defaultFile == null || !defaultFile.exists())
			{
				mklogger.error("Config file is not exists or null");
				return;
			}

			try(FileReader reader = new FileReader(defaultFile)){
				ftpJsonData = new ArrayList<MkFtpData>();
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
					boolean serviceHashDirPrefix = false;
					
					String[] serviceAllowFileFormat = null;
					try {
						serviceId = serviceObject.get("id").toString();
						servicePath = serviceObject.get("servicepath").toString();
						Object prefix = serviceObject.get("dir");
						mklogger.debug(TAG, "prefix : " + prefix);
						if(prefix != null) {
							serviceDirPrefix = prefix.toString();
							serviceHashDirPrefix = serviceObject.get("hash_dir").toString().contentEquals("true");
						}

						if(ftpControllerPath.charAt(ftpControllerPath.length() -1) == '/') {
							servicePath = (servicePath.charAt(0) == '/' ? (ftpControllerPath.substring(0, ftpControllerPath.length()-1) + servicePath) : (ftpControllerPath + servicePath));
						}else {
							servicePath = (servicePath.charAt(0) == '/' ? (ftpControllerPath + servicePath) : (ftpControllerPath + "/" + servicePath));
						}
						
						if(!createDirectory(servicePath, useAbsolute)) {
							mklogger.error("Failed to create directory. Please check your IO permissions. [" + servicePath +"]");
							return;
						}
						
						MkJsonData mjd = new MkJsonData(serviceObject.get("format").toString());
						if(!mjd.setJsonObject()) {
							mklogger.debug(TAG, "Failed to set MkJsonObject service name : " + serviceId);
							return;
						}
						
						JSONObject serviceFormatData = mjd.getJsonObject();
						serviceAllowFileFormat = new String[serviceFormatData.size()];
						for(int j = 0; j < serviceAllowFileFormat.length; j++) {
							serviceAllowFileFormat[j] = serviceFormatData.get("" + (j+1)).toString();
						}
						
					} catch(Exception e) {
						mklogger.debug(TAG, "Failed to create ftp controller. " + e.getMessage());
						e.printStackTrace();
						return;
					}
					
					MkFtpData result = new MkFtpData();
					result.setControlName(ftpName);
					result.setPath(servicePath);
					result.setDebugLevel(ftpDebugLevel);
					result.setServiceName(serviceId);
					result.setData(serviceAllowFileFormat);
					result.setDirPrefix(serviceDirPrefix);
					result.setHashDirPrefix(serviceHashDirPrefix);
					
					ftpJsonData.add(result);
					printFTPInfo(result, "info");
				}
				
				ftp_configs.put(ftpName, ftpJsonData);
				
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
			mklogger.info("=*=*=*=*=*=*=* MkWeb FTP  Configs  Done*=*=*=*=*=*=*=*=");
		}
	}
	
	public void printFTPInfo(MkFtpData jsonData, String type) {
		String tempMsg = "\n===============================FTP  Control================================="
				+ "\n|Controller:\t" + jsonData.getControlName()
				+ "\n|FTP ID:\t" + jsonData.getServiceName()
				+ "\n|FTP Path:\t" + jsonData.getPath()
				+ "\n|FTP Prefix:\t" + jsonData.getDirPrefix()
				+ "\n|Debug Level:\t" + jsonData.getDebugLevel()
				+ "\n|File Formats:\t" + jsonData.getData()
				+ "\n============================================================================";
		
		mklogger.temp(tempMsg, false);
		mklogger.flush(type);
	}
	
	private void reloadControls() {
		for(int i = 0; i < defaultFiles.length; i++)
		{
			if(lastModified[i] != defaultFiles[i].lastModified()){
				setFtpConfigs(defaultFiles);
				mklogger.info("==============Reload FTP Config files==============");
				mklogger.info("========Caused by : different modified time========");
				mklogger.info("==============Reload FTP Config files==============");
				break;
			}
		}
	}
	
	public ArrayList<MkFtpData> getControl(String controlName) {
		reloadControls();
		return ftp_configs.get(controlName);
	}
	
	public ArrayList<MkFtpData> getControlByServiceName(String serviceName){
		reloadControls();
		
		Set<String> keys = ftp_configs.keySet();
		Iterator<String> iter  = keys.iterator();
		String resultControlName = null;
		ArrayList<MkFtpData> jsonData = null;
		while(iter.hasNext()) {
			String controlName = iter.next();
			jsonData = getControl(controlName);
			for(MkFtpData curData : jsonData) {
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
	
	private boolean createDirectory(String path, boolean useAbsolute){
		String targetDir = (useAbsolute) ? path : (filePrefix + path);
		File folder = new File(targetDir);
		boolean isDirExists = folder.exists();
		if(!isDirExists)
		{
			mklogger.info(TAG, "The directory is not exists. Creating new one...");
			try {		
				isDirExists = folder.mkdirs();
				folder.setReadable(true, false);
				folder.setExecutable(true, false);
				if(!isDirExists) {
					mklogger.error(TAG, "Failed to create path. [" + targetDir +"]");
					return false;
				}
			} catch (Exception e) {
				mklogger.error(TAG, "Failed to create path. [" + targetDir +"] " + e.getMessage());
				return false;
			}
		}
		
		if(isDirExists) {
			mklogger.info(TAG, "Success to create path. [" + targetDir +"]");
		}
		
		return isDirExists;
	}
}
