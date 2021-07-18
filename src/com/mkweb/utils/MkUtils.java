package com.mkweb.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mkweb.logger.MkLogger;

public abstract class MkUtils {
	MkLogger mklogger = new MkLogger("MkUtils");
	public List<Object> keyGetter(Map<Object, Object> map){
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
	
	public List<Object> valueGetter(Map<Object, Object> map, List<Object> keys){
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
	
	public Map<Object, Object> mapGenerator(List<Object> key, List<Object> value){
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
}
