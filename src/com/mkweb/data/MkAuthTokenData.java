package com.mkweb.data;

import com.mkweb.entity.MkDefaultModelImpl;
import org.json.simple.JSONObject;

public class MkAuthTokenData extends AbsJsonData implements MkDefaultModelImpl {
    private String algorithm;
    private String secretKey;
    private JSONObject payload;
    private JSONObject authorizer;

    public String getControlName(){ return this.controlName; }
    public String getAlgorithm(){ return this.algorithm;    }
    public JSONObject getAuthorizer(){  return this.authorizer; }
    public JSONObject getPayload(){ return this.payload;    }

    public void setControlName(String controlName){ this.controlName = controlName; }
    public MkAuthTokenData setAlgorithm(String algorithm){ this.algorithm = algorithm;    return this;  }
    public MkAuthTokenData setAuthorizer(JSONObject authorizer){    this.authorizer = authorizer;   return this;    }
    public MkAuthTokenData setPayload(JSONObject payload) { this.payload = payload; return this; }
    public MkAuthTokenData setSecretKey(String secretKey){
        if(this.secretKey == null)
            this.secretKey = secretKey;

        return this;
    }
}