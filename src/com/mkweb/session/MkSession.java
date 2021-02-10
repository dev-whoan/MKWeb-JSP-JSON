package com.mkweb.session;

import javax.servlet.http.HttpSession;

import com.mkweb.data.MkSessionData;

public class MkSession{
	private MkSessionData data;
	private HttpSession session;
	public void MkSession(HttpSession session) {
		data = new MkSessionData();
		this.session = session;
	}
}
