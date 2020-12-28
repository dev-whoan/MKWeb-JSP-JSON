package com.mkweb.restapi;

import org.json.simple.JSONObject;

import com.mkweb.config.MkConfigReader;
import com.mkweb.logger.MkLogger;
import com.mkweb.security.MyCrypto;

public class MkRestApiResponse {
	private long responseLife = -1L;
	private JSONObject responseResult = null;
	private int responseCount = 0;
	private int responseCode = -1;
	private String hash = null;
	
	private String TAG = "[MkRestApiResponse]";
	private MkLogger mklogger = MkLogger.Me();
	
	MkRestApiResponse(JSONObject jsonObject, int code, int count, String hashData){
		mklogger.debug(TAG, " Called");
		responseResult = jsonObject;
		setLife();
		responseCount = count;
		responseCode = code;
		MyCrypto mc = new MyCrypto();
		hash = mc.MD5(hashData);
	}
	
	public JSONObject getData() {
		if(!needUpdate())
			return this.responseResult;
		else {
			return null;
		}
	}
	
	public long getLife() {
		return this.responseLife;
	}
	
	public int getCount() {
		return this.responseCount;
	}
	
	public int getCode() {
		return this.responseCode;
	}
	
	public String getHashData() {
		return this.hash;
	}
	
	public void setData(JSONObject jsonObject) {
		this.responseResult = jsonObject;
	}

	private void setLife() {
		this.responseLife = System.currentTimeMillis() + Integer.parseInt(MkConfigReader.Me().get("mkweb.restapi.lifecycle")) * 60 * 1000;
		mklogger.debug(TAG, " lifecycle : " + responseLife);
	}
	
	public void setCount(int count) {
		this.responseCount += count;
	}
	
	public boolean needUpdate() {
		return (this.responseLife >= System.currentTimeMillis());
	}
}