package com.mkweb.data;

import com.mkweb.config.MkAuthTokenConfigs;
import com.mkweb.config.MkConfigReader;
import com.mkweb.logger.MkLogger;
import com.mkweb.utils.MkCrypto;
import com.mkweb.utils.MkUtils;
import org.json.simple.JSONObject;

import java.util.Base64;
import java.util.List;

public class MkJWTData {
    private JSONObject payloadObject;
    private JSONObject headerObject;
    private String header;
    private String payload;
    private String signature;
    private long timestamp;
    private static final String TAG = "[MkJWTData]";
    private static final MkLogger mklogger = new MkLogger(TAG);

    public MkJWTData(JSONObject payloadObject){
        this.payloadObject = payloadObject;
        this.timestamp = System.currentTimeMillis();
        createHeaderObject();
        createPayloadObject();
        generateSignature();
    }

    public MkJWTData(JSONObject payloadObject, long timestamp){
        this.payloadObject = payloadObject;
        this.timestamp = timestamp;
        createHeaderObject();
        createPreparedPayloadObject();
        generateSignature();
    }

    private void createHeaderObject(){
        MkAuthTokenData matd = MkAuthTokenConfigs.Me().getControl(MkConfigReader.Me().get("mkweb.auth.controller.name"));

        this.headerObject = new JSONObject();
        this.headerObject.put("alg", matd.getAlgorithm());
        this.headerObject.put("type", "JWT");

        this.header = Base64.getUrlEncoder().withoutPadding().encodeToString(this.headerObject.toString().getBytes());
    }
    private void createPreparedPayloadObject(){
        MkAuthTokenData matd = MkAuthTokenConfigs.Me().getControl(MkConfigReader.Me().get("mkweb.auth.controller.name"));
        JSONObject preparedPayload = matd.getPayload();
        List<Object> keys = MkUtils.keyGetter(preparedPayload);

        JSONObject tempObject = new JSONObject();
        try{
            for(int i = 0; i < keys.size(); i++){
                String value = payloadObject.get(keys.get(i)).toString();
                tempObject.put(keys.get(i), value);
            }
        } catch (NullPointerException e) {
            mklogger.error("There is no data for the prepayload.");
            return;
        }

        this.payloadObject = new JSONObject(tempObject);
        this.payloadObject.put("timestamp", this.timestamp);
        this.payload = Base64.getUrlEncoder().withoutPadding().encodeToString(this.payloadObject.toString().getBytes());
    }
    private void createPayloadObject(){
        MkAuthTokenData matd = MkAuthTokenConfigs.Me().getControl(MkConfigReader.Me().get("mkweb.auth.controller.name"));
        JSONObject preparedPayload = matd.getPayload();
        List<Object> keys = MkUtils.keyGetter(preparedPayload);
        List<String> values = MkUtils.valueGetter(preparedPayload, keys);

        JSONObject tempObject = new JSONObject();
        try{
            for(int i = 0; i < keys.size(); i++){
                String value = payloadObject.get(values.get(i)).toString();
                tempObject.put(keys.get(i), value);
            }
        } catch (NullPointerException e) {
            mklogger.error("There is no data for the payload");
            return;
        }

        this.payloadObject = new JSONObject(tempObject);
        this.payloadObject.put("timestamp", this.timestamp);
        this.payload = Base64.getUrlEncoder().withoutPadding().encodeToString(this.payloadObject.toString().getBytes());
    }

    private void generateSignature(){
        if(this.header == null || this.payload == null){
            mklogger.error("header or payload is not set for jwt signature.");
            return;
        }
        String currentData = this.header + "." + this.payload;
        String secretKey = MkConfigReader.Me().get("mkweb.auth.secretkey");
        this.signature = MkCrypto.HS256(currentData, secretKey.getBytes());
    }

    public String getToken() {
        return this.header + "." + this.payload + "." + this.signature;
    }

    public String getSignature(){   return this.signature;  }

    public long IssuedAt(){ return this.timestamp;  }
}