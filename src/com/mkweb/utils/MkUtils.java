package com.mkweb.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.mkweb.logger.MkLogger;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

public class MkUtils {
	private MkUtils(){ }

	public static String base64urlEncoding(String value){
		return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}

	public static List<Object> keyGetter(Map<Object, Object> map){
		ArrayList<Object> result = null;
		if(map == null) {
			new MkLogger("MkUtils").error("func keyGetter(): map must not null");
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
			new MkLogger("MkUtils").error("func valueGetter(): map and keys must not null");
			return null;
		}
		if(map.size() != keys.size()) {
			new MkLogger("MkUtils").error("func valueGetter(): map and key list size are not same.");
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
			new MkLogger("MkUtils").error("func mapGenerator(): key and value must not null");
			return null;
		}
		if(key.size() < value.size()) {
			new MkLogger("MkUtils").error("func mapGenerator(): key size must be bigger than value size");
			return null;
		}
		result = new LinkedHashMap<>();
		for(int i = 0; i < key.size(); i++) {
			result.put(key.get(i), value.get(i));
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

		new MkLogger("[MkUtils]").debug("stringBuilder:" +  stringBuilder.toString());

		return MkJsonData.createJsonObject(stringBuilder.toString());
	}
}
