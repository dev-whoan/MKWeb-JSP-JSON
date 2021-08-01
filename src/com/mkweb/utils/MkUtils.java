package com.mkweb.utils;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.mkweb.logger.MkLogger;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

public class MkUtils {
	private static final String TAG = "[MkUtils]";
	private static final MkLogger mklogger = new MkLogger(TAG);
	private MkUtils(){ }

	public static String base64urlEncoding(String value){
		return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}

	public static String base64urlDecoding(String value){
		return new String(Base64.getUrlDecoder().decode(value));
	}

	public static List<Object> keyGetter(Map<Object, Object> map){
		ArrayList<Object> result = null;
		if(map == null) {
			mklogger.error("func keyGetter(): map must not null");
			return null;
		}
		if(map.size() > 0) {
			result = new ArrayList<>();
			
			Set<Object> keys = map.keySet();
			Iterator<Object> iter = keys.iterator();
			
			while(iter.hasNext())
				result.add(iter.next());
		}
		
		return result;
	}
	
	public static List<Object> valueGetter(Map<Object, Object> map, List<Object> keys){
		ArrayList<Object> result = null;
		if(map == null || keys == null) {
			mklogger.error("func valueGetter(): map and keys must not null");
			return null;
		}
		if(map.size() != keys.size()) {
			mklogger.error("func valueGetter(): map and key list size are not same.");
			return null;
		}
		if(map.size() > 0 && map.size() == keys.size()) {
			result = new ArrayList<>();
			for(Object key : keys)
				result.add(map.get(key));
		}
		
		return result;
	}
	
	public static Map<Object, Object> mapGenerator(List<Object> key, List<Object> value){
		LinkedHashMap<Object, Object> result = null;
		if(key == null || value == null) {
			mklogger.error("func mapGenerator(): key and value must not null");
			return null;
		}
		if(key.size() < value.size()) {
			mklogger.error("func mapGenerator(): key size must be bigger than value size");
			return null;
		}
		result = new LinkedHashMap<>();
		for(int i = 0; i < key.size(); i++) {
			result.put(key.get(i), value.get(i));
		}
		return result;
	}

	public static Map<String, String> stringToMap(String[] strArray) {
		Map<String, String> result = new HashMap<String, String>();
		for (String str : strArray) {
			String tempParameter = str.split("\\=")[0];
			String tempValue = str.split("\\=")[1];
			result.put(tempParameter, tempValue);
		}

		return result;
	}

	public static JSONObject getPOSTJsonData(HttpServletRequest request){
		StringBuilder stringBuilder = new StringBuilder(); // String Builder
		BufferedReader bufferedReader = null;
		try(InputStream inputStream = request.getInputStream()){
			if(inputStream != null){
				bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
				char[] charBuffer = new char[256];
				int bytesRead = -1;
				while ((bytesRead = bufferedReader.read(charBuffer)) > 0){
					stringBuilder.append(charBuffer, 0, bytesRead);
				}
			}
		} catch (IOException e){
			e.printStackTrace();
		} finally {
			if(bufferedReader != null){
				try{
					bufferedReader.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		}

		mklogger.debug("stringBuilder:" +  stringBuilder.toString());
		JSONObject result = null;

		mklogger.debug("1");
		result = MkJsonData.createJsonObject(stringBuilder.toString());
		mklogger.debug("result: " + result);
		if(result == null)
			result = MkJsonData.createJsonObject(MkJsonData.stringToJsonString(stringBuilder.toString()));
		mklogger.debug("final result: " + result);
		return result;
	}

	public static Map<String, Object[]> getQueryParameters(String queryString) throws UnsupportedEncodingException {
		Map<String, Object[]> queryParameters = new HashMap<>();
//		String queryString = request.getQueryString();
		if(queryString != null && !queryString.isEmpty()) {
			queryString = URLDecoder.decode(queryString, StandardCharsets.UTF_8);
			String[] parameters = queryString.split("&");
			for (String parameter : parameters) {
				String[] keyValuePair = parameter.split("=");
				Object[] keyValues = queryParameters.get(keyValuePair[0]);
				ArrayList<Object> values = new ArrayList<>();
				if(keyValues != null)
					values = (ArrayList<Object>) Arrays.asList(queryParameters.get(keyValuePair[0]));

				if(keyValuePair.length == 1){
					values.add("");
				} else {
					String[] valuesPair = keyValuePair[1].split(",");
					Collections.addAll(values, valuesPair);
				}
				new MkLogger("[MkUtils]").debug(values);
				queryParameters.put(keyValuePair[0], values.toArray() );
			}
		}
		return queryParameters;
	}
}
