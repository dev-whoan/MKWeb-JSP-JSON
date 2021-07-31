package com.mkweb.auths;

import com.mkweb.config.MkConfigReader;
import com.mkweb.data.MkAuthTokenData;
import com.mkweb.data.MkJWTData;
import com.mkweb.logger.MkLogger;
import com.mkweb.utils.MkJsonData;
import org.json.simple.JSONObject;

import java.util.Base64;

public class MkAuthToken {
	//Header Authorization
	//Attribute Token
	//includes API token
	private MkAuthTokenData tokenData;
	private MkJWTData jwtData;
	private String TAG = "[MkAuthToken]";
	private MkLogger mklogger = new MkLogger(TAG);

	private static long lifetime = Long.parseLong(MkConfigReader.Me().get("mkweb.auth.lifetime")) * 1000L;

	public MkAuthToken generateToken(String jsonString){
		MkJsonData mkJsonData = new MkJsonData(jsonString);
		mkJsonData.setJsonObject();

		jwtData = new MkJWTData(mkJsonData.getJsonObject());

		return this;
	}

	public static boolean verify(String token){
		String header, payload, signature;
		try{
			String[] splitToken = token.split("\\.");
			header = splitToken[0];
			payload = splitToken[1];
			signature = splitToken[2];
		} catch (Exception e){
			return false;
		}

		String orgHeader, orgPayload;
		try{
			orgHeader = new String(Base64.getUrlDecoder().decode(header));
			orgPayload = new String(Base64.getUrlDecoder().decode(payload));
		} catch (Exception e){
			return false;
		}

		MkJsonData mkJsonData = new MkJsonData(orgPayload);
		mkJsonData.setJsonObject();
		JSONObject jsonObject = mkJsonData.getJsonObject();
		long tempTimestamp = Long.parseLong(jsonObject.get("timestamp").toString());

		try{
			MkJWTData givenToken = new MkJWTData(jsonObject, tempTimestamp);
			if(!lifecheck(givenToken.IssuedAt()))
				return false;

			return (givenToken.getSignature().contentEquals(signature));
		} catch (Exception e){
			return false;
		}
	}

	public static boolean lifecheck(long tokenTime){
		return System.currentTimeMillis() - tokenTime <= lifetime;
	}

	public String getToken(){
		return jwtData.getToken();
	}
}
