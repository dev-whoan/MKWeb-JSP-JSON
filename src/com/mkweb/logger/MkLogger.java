package com.mkweb.logger;


import java.io.BufferedWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class MkLogger{
	private HashMap<String, String> log_configs = new HashMap<String, String>();

	private File defaultFile = null;
	private String TAG = null;
	private String temp = null;

	private String logMsg = null;
	private static MkLogger ml = null;
	public static MkLogger Me() {
		if(ml == null)
			ml = new MkLogger();

		return ml;
	}

	private void Log(Object msg, String caller) {
		try {
			if(log_configs.get("print_date").toString().equals("yes"))
			{
				Date d = new Date();
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
				if(log_configs.get("print_time").toString().equals("yes"))
					dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

				TAG = "("+dateFormat.format(d)+")";
				TAG += temp;
			}

			if(log_configs.get("use_catalina").toString().equals("yes"))
				System.out.println(TAG + msg.toString());
			else {
				File logFile = new File(log_configs.get("log_location").toString());
				BufferedWriter bw = new BufferedWriter(new FileWriter(logFile, true));
				PrintWriter pw = new PrintWriter(bw, true);

				pw.println(TAG + msg.toString());
			}
		}catch (IOException e){
			System.out.println(TAG + " Logging error occured in function info():: original logging message: " + msg);
			System.out.println(TAG + " IOException: " + e.getMessage());
		}catch(Exception e) {
			System.out.println(TAG + " MKLogger::function " + caller + " has occured some errors!");
			e.printStackTrace();
		}
	}

	public void flush(String msgType) {
		if(logMsg != null)
		{
			if(msgType == "info")
				info("", logMsg);
			else if(msgType == "warn")
				warn("", logMsg);
			else if(msgType == "error")
				error("", logMsg);
			else if(msgType == "debug")
				debug("", logMsg);

			logMsg = null;
		}
	}

	public void debug(Object msg) {
		debug("", msg);
	}
	public void warn(Object msg) {
		warn("", msg);
	}

	public void error(Object msg) {
		error("", msg);
	}
	public void temp(Object msg, boolean doFlush) {
		temp("", msg, doFlush);
	}

	public void info(Object msg) {
		info("", msg);
	}

	public void temp(String TAG, Object msg, boolean doFlush) {
		if(logMsg != null) {
			if(doFlush)
			{
				warn(TAG, "--AUTO FLUSHING--: " + logMsg);
				logMsg = null;
				return;
			}

			if(msg != null)
				logMsg += "\n" + msg.toString();
		}else {
			if(msg != null)
				logMsg = TAG + " " + msg.toString();
		}
	}

	public void info(String TAG, Object msg)  {
		switch(log_configs.get("level").toString())
		{
		case "info": case "debug":
			Log("[INFO]" + TAG + " " + msg, "info");
			break;
		}
	}

	public void warn(String TAG, Object msg)  {
		switch(log_configs.get("level").toString())
		{
		case "info": case "warn": case "debug":
			Log("[*WARN*]" + TAG + " " + msg, "warn");
			break;
		}
	}

	public void error(String TAG, Object msg) {
		switch(log_configs.get("level").toString())
		{
		case "info": case "warn": case "error": case "debug":
			Log("[**ERROR**]" + TAG + " " + msg, "error");
			break;
		}
	}

	public void debug(String TAG, Object msg) {
		switch(log_configs.get("level").toString())
		{
		case "debug":
			Log("[^_^ DEBUG ^_^]\n" + TAG + " " + msg, "debug");
			break;
		}
	}

	private void createFile(){
		String absPath = log_configs.get("log_location").toString();
		File file = new File(absPath);
		if(!file.exists())
		{
			System.out.println(absPath);
			System.out.println("MkLogger File is not exists. Creating new one...");


			String[] directoryPath = absPath.split("/");

			String dirPath = "";
			for(int i = 0; i < directoryPath.length; i++) {
				if(i != (directoryPath.length-1))
					if(i != 0)
						dirPath += "/" + directoryPath[i];
					else
						dirPath += directoryPath[i];
				else
					break;
			}
			System.out.println("dirPath: " + dirPath);
			boolean isDirExists = false;
			if(!new File(dirPath).exists())
				isDirExists = new File(dirPath).mkdirs(); 
			else
				isDirExists = true;

			if(isDirExists)
			{
				try {
					file.createNewFile();
					info("MkLogger File is not exists. Creating new one...");
					info("Logger Path: " + absPath);
					info("Success to create log file!");
				} catch (IOException e) {
					System.out.println("Failed to create file: " + defaultFile.getAbsolutePath() + "\n" + e.getMessage());
				}
			}
		}
	}

	public void setLogConfig(File logConfigs) {
		log_configs.clear();
		logMsg = "";
		temp = "[MkLogger]";
		defaultFile = logConfigs;
		try {
			logMsg += ("\n[Initializing][MkLogger]" + "=*=*=*=*=*=*=* MkWeb Log Configs Start*=*=*=*=*=*=*=*=\n");
			logMsg += ("[Initializing][MkLogger]" + "=                   Start setting...                 =\n");

			if(defaultFile == null || !defaultFile.exists())
			{
				logMsg += ("[Initializing][MkLogger]" + "Config file is not exists or null\n");
				return;
			}

			String[] config_key = {"level", "print_date", "print_time", "use_catalina", "log_location"};

			
			try(FileReader reader = new FileReader(defaultFile)){
				JSONParser jsonParser = new JSONParser();
				JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
				JSONObject logObject = (JSONObject) jsonObject.get("Controller");
				
				temp = logObject.get("name").toString();
				for(int i = 0; i < config_key.length; i++) {
					log_configs.put(config_key[i], logObject.get(config_key[i]).toString());
				}
			} catch (FileNotFoundException e) {
				logMsg += ("[**ERROR**]" + e.getMessage());
				e.printStackTrace();
			} catch (IOException e) {
				logMsg += ("[**ERROR**]" + e.getMessage());
				e.printStackTrace();
			} catch (ParseException e) {
				logMsg += ("[**ERROR**]" + e.getMessage());
				e.printStackTrace();
			}

			createFile();

			info("", logMsg);
			logMsg = null;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

	}
}
