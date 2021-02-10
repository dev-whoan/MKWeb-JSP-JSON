package com.mkweb.data;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class MkSessionData {
	private HashMap<String, Object> data;
	
	public MkSessionData() {
		data = new LinkedHashMap<>();
	}
	
	public void add(String key, Object element) {	data.put(key, element);	}
	public Object get(String key) {	return this.data.get(key);	}
	public void clear() {	this.data.clear();}
}
