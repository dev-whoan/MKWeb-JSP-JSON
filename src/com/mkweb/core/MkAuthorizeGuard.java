package com.mkweb.core;

import com.mkweb.auths.MkAuthToken;
import com.mkweb.config.MkAuthTokenConfigs;
import com.mkweb.config.MkConfigReader;
import com.mkweb.data.MkAuthTokenData;
import com.mkweb.data.MkPageJsonData;
import com.mkweb.data.MkSqlJsonData;
import com.mkweb.database.MkDbAccessor;
import com.mkweb.logger.MkLogger;
import com.mkweb.utils.ConnectionChecker;
import com.mkweb.utils.MkJsonData;
import com.mkweb.utils.MkUtils;
import org.json.simple.JSONObject;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@WebServlet(
        name="MkAuthorizeGuard",
        loadOnStartup=1
)
public class MkAuthorizeGuard extends HttpServlet {
    MkAuthToken authToken;
    private static final long serialVersionUID = 1L;

    private static final String TAG = "[MKAuthorizeGuard]";
    private static final MkLogger mklogger = new MkLogger(TAG);

    private MkPageJsonData pjData = null;

    private MkAuthTokenData tokenConfigs = null;
    private JSONObject tokenService = null;
    private JSONObject tokenParameter = null;
    private String[] requestParams;
    private String[] requestValues;

    public MkAuthorizeGuard() {
        super();
    }

    private boolean isParameterValid(HttpServletRequest request){
        JSONObject requestObject = MkUtils.getPOSTJsonData(request);
        List<Object> keys = MkUtils.keyGetter(tokenParameter);
        List<String> values = MkUtils.valueGetter(tokenParameter, keys);

        if(requestObject == null || requestObject.size() == 0){ return false; }
        if(keys == null || keys.size() == 0){ return false;   }
        if(requestObject.size() != keys.size()){ return false; }

        mklogger.debug(keys);
        mklogger.debug(values);
        try{
            requestParams = new String[keys.size()];
            requestValues = new String[keys.size()];

            int i = 0;
            assert values != null;
            for(String param : values){
                String temp = requestObject.get(param).toString();

                String decodeResult = URLDecoder.decode(temp, StandardCharsets.UTF_8);
                String encodeResult = URLEncoder.encode(decodeResult, StandardCharsets.UTF_8);

                temp = (encodeResult.contentEquals(temp) ? decodeResult : temp);

                requestParams[i] = param;
                requestValues[i++] = temp;
            }
        } catch (NullPointerException e){
            return false;
        }

        return true;
    }

    private String preparedQuery(){
        String controlName = tokenService.get("controller").toString();
        String serviceName = tokenService.get("service").toString();
        MkSqlJsonData sqlService = null;

        return ConnectionChecker.regularQuery(controlName, serviceName, false);
    }


    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        tokenConfigs = MkAuthTokenConfigs.Me().getControl(MkConfigReader.Me().get("mkweb.auth.controller.name"));

        tokenService = (JSONObject) tokenConfigs.getAuthorizer().get("sql");
        tokenParameter = (JSONObject) tokenConfigs.getAuthorizer().get("parameter");
        /*
        1. request로부터 수신한 parameter를 검사해야 함. --> tokenParameter안의 것들과 비교
         */

        if(!isParameterValid(request)){
            mklogger.debug("Parameter invalid");
            response.setStatus(401);
            return;
        }

        /*
        1. sql을 실행시켜서 결과를 받아와야함 --> tokenService의 controller, service
        2. 받아온 결과를 바탕으로 MkAuthToken을 만들어야 함
        3. MkAuthToken을 Header에 설정해야 함
        4. 이후 MkAuthToken에서 토큰을 받을 때 마다 검사하고, 시간이 지났다면 초기화 하거나 갱신 시켜줘야 함
        5. Token이 필요한 모든 요청은 Token을 검사해야 함
        */
        doTask(request, response);
    }

    private void doTask(HttpServletRequest request, HttpServletResponse response) throws IOException{
        if(requestParams.length == 0 || requestValues.length == 0){
            response.setStatus(401);
            return;
        }

        String befQuery = preparedQuery();
        String query = ConnectionChecker.setQuery(befQuery);

        MkDbAccessor DA = new MkDbAccessor();
        DA.setPreparedStatement(query);
        DA.setRequestValue(requestValues);
        PrintWriter writer = response.getWriter();
        JSONObject responseObject = new JSONObject();
        int statusCode = 200;
        String token = null;
        try{
            ArrayList<Object> dbResult = DA.executeSEL(true);
            JSONObject jsonObject = null;
            if(dbResult == null || dbResult.size() == 0) {
                statusCode = 204;
            } else {
                LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
                result = (LinkedHashMap<String, Object>) dbResult.get(dbResult.size()-1);
                jsonObject = MkJsonData.objectMapToJson(result);
            }

            if(jsonObject == null || (jsonObject.size() == 0)) {
                response.setStatus(204);
                return;
            }

            mklogger.debug(MkJsonData.removeQuoteFromJsonObject(jsonObject));
            authToken = new MkAuthToken();
            token = authToken.generateToken(MkJsonData.removeQuoteFromJsonObject(jsonObject)).getToken();
            if(!MkAuthToken.verify(token))
                statusCode = 401;

            statusCode = 200;
        } catch (SQLException e ) {
            mklogger.error("(executeDML) psmt = this.dbCon.prepareStatement(" + query + ") :" + e.getMessage());
            statusCode = 500;
        }
        response.setStatus(statusCode);

        responseObject.put("code", statusCode);
        responseObject.put("token", token);

        writer.print(responseObject);
    }
}