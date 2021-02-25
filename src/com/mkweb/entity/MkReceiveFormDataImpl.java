package com.mkweb.entity;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mkweb.data.MkPageJsonData;

interface MkReceiveFormDataImpl {
	ArrayList<MkPageJsonData> getPageControl(String url);
	boolean checkMethod(HttpServletRequest request, String rqMethod, String rqPageURL);
}
