package com.mkweb.config;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import com.mkweb.data.MkSqlJsonData;
import com.mkweb.utils.MkUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.mkweb.data.MkAuthTokenData;
import com.mkweb.logger.MkLogger;

public class MkAuthTokenConfigs {
	private HashMap<String, MkAuthTokenData> authToken_configs = new HashMap<String, MkAuthTokenData>();
	private File defaultFile = null;
	private static MkAuthTokenConfigs matd = null;
	private long lastModified = -1L;
	private static final String TAG = "[AuthToken Configs]";
	private static final MkLogger mklogger = new MkLogger(TAG);
	private static final HashMap<String, String> SUPPORT_ALGORITHM = new HashMap<>();

	public static MkAuthTokenConfigs Me() {
		if(matd == null) 
			matd = new MkAuthTokenConfigs();

		return matd;
	}

	private static void initializeAlgorithm(){
		SUPPORT_ALGORITHM.put("HMACSHA256", "HS256");
	}

	public void setAuthTokenConfigs(File authTokenConfigs) {
		if(SUPPORT_ALGORITHM.size() == 0){
			initializeAlgorithm();
		}
		authToken_configs.clear();
		defaultFile = authTokenConfigs;
		MkAuthTokenData authTokenJsonData = null;
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
			authTokenJsonData = new MkAuthTokenData();
			JSONParser jsonParser = new JSONParser();
			JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
			JSONObject tokenObject = (JSONObject) jsonObject.get("Controller");

			String authName = MkConfigReader.Me().get("mkweb.auth.controller.name");
			String authDebugLevel = tokenObject.get("level").toString();

			String algo = tokenObject.get("algorithm").toString();
			String authAlgorithm = SUPPORT_ALGORITHM.get(algo);

			if(authAlgorithm == null){
				mklogger.warn("NoSupportAlgorithmException: " + algo + " is not supported. Default algorithm will be HS256");
				authAlgorithm = "HS256";
			}

			String secretKey = MkConfigReader.Me().get("mkweb.auth.secretkey");
			if(secretKey == null)	{
				secretKey = String.valueOf(System.currentTimeMillis());
				mklogger.warn("SecretKeyNullException: Secretkey is not set. Default secretkey will be current milli seconds: " + secretKey);
			}
			//now payload and databases
			JSONObject authObject = (JSONObject) tokenObject.get("auths");

			if(!isAuthsValid(authObject)){
				mklogger.error("InvalidAuthObjectException: \"auths\" field is not valid.");
				return;
			}

			JSONObject payloadObject = (JSONObject) tokenObject.get("payload");
			JSONObject payload = new JSONObject();

			List<String> keys = MkUtils.keyGetter(payloadObject);
			for(String key : keys){
				payload.put(key, payloadObject.get(key));
			}

			authTokenJsonData.setAlgorithm(authAlgorithm)
					.setSecretKey(secretKey)
					.setAuthorizer(authObject)
					.setPayload(payload)
					.setControlName(authName);

			printAuthTokenInfo(authTokenJsonData, "info");
			authToken_configs.put(authName, authTokenJsonData);
		} catch (IOException | ParseException e) {
			mklogger.error(e.getMessage());
			e.printStackTrace();
		}
		mklogger.info("=*=*=*=*=*=*=* MkWeb Auth Configs  Done*=*=*=*=*=*=*=*=");
	}

	private boolean isAuthsValid(JSONObject object){
		JSONObject sql;
		JSONObject parameter;
		try{
			sql = (JSONObject) object.get("sql");
		} catch (Exception e){
			mklogger.error("NullSQLControllerException: No sql set for \"auths\" field. 1");
			return false;
		}

		String controller = null;
		try{
			controller = sql.get("controller").toString();
		}catch (NullPointerException e){
			mklogger.debug(sql.toString());
			mklogger.error("NullSQLControllerException: No controller set for \"auths\" field. 2");
			return false;
		}
		ArrayList<MkSqlJsonData> sqls = MkSQLConfigs.Me().getControl(controller);
		if( sqls == null ){
			mklogger.error("NoSQLControllerFoundException: " + controller);
			return false;
		}

		String targetService = null;
		try{
			targetService = sql.get("service").toString();
			if(targetService == null){
				mklogger.error("NullSQLServiceException: No service set for \"auths\" field. 1");
				return false;
			}
		} catch (NullPointerException e){
			mklogger.debug(sql.toString());
			mklogger.error("NullSQLServiceException: No service set for \"auths\" field. 2");
			return false;
		}

		for(MkSqlJsonData temp : sqls){
			if (temp.getServiceName().contentEquals(targetService)) {
				sql.put("service", targetService);
			}
		}

		if(sql.get("service") == null){
			mklogger.error("NoSQLServiceFoundException: " + targetService);
			return false;
		}

		try{
			parameter = (JSONObject) object.get("parameter");
			if(parameter.size() == 0){
				mklogger.error("NoJWTParameterSetException: No parameter for \"auths\" field. 1");
				return false;
			}
		} catch (Exception e){
			mklogger.error("NullSQLControllerException: No parameter for \"auths\" field. 2");
			return false;
		}

		return true;
	}

	public void printAuthTokenInfo(MkAuthTokenData jsonData, String type) {
		String tempMsg = "\n==========================MkAuthToken  Control=============================="
				+ "\n|Controller:\t" + jsonData.getControlName()
				+ "\n|Algorithm:\t" + jsonData.getAlgorithm()
				+ "\n|Auths:\t" + jsonData.getAuthorizer().toString()
				+ "\n|Payloads:\t" + jsonData.getPayload().toString()
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

	public MkAuthTokenData getControl(String controlName) {
		reloadControls();
		return authToken_configs.get(controlName);
	}
}
