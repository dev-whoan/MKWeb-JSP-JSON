package com.mkweb.entity;

import java.util.ArrayList;

import javax.servlet.http.HttpServlet;

import com.mkweb.data.MkPageJsonData;

public abstract class MkReceiveFormDataEntity extends HttpServlet{
	protected abstract ArrayList<MkPageJsonData> getPageControl(String url);
}
