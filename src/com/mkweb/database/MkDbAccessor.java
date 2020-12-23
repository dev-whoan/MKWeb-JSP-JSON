package com.mkweb.database;

import java.sql.Connection;



import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.json.simple.JSONObject;

import com.mkweb.logger.MkLogger;
import com.mkweb.config.MkConfigReader;

public class MkDbAccessor {
	//로그 만들기
	private Connection dbCon = null;
	private String psmt = null;
	private MkLogger mklogger = MkLogger.Me();
	private ArrayList<String> reqValue = null;
	private String[] reqValueArr = null;
	
	private String TAG = "[MkDbAccessor]";
	
	public MkDbAccessor() {
		try {
			dbCon = connectDB();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	protected Connection getDbCon() {	return this.dbCon;	}
	public void setPreparedStatement(String qr) {
		this.psmt = qr;
		mklogger.debug(TAG, " psmt set check : " + this.psmt);
	}
	
	public void setRequestValue(ArrayList<String> arr) {
		reqValue = new ArrayList<>();
		mklogger.temp(TAG, "=====RequestValue=====", false);
		mklogger.temp(this.psmt, false);
		String s = "Values : (";
		for(int i = 0; i < arr.size(); i++) {
			reqValue.add(arr.get(i));
			
			s += (arr.get(i).length() < 20 ? arr.get(i) : arr.get(i).substring(0, 19) + "...");
			if(i < arr.size() - 1)
				s += ", ";
		}
		s += ")";
		mklogger.temp(s, false);
		mklogger.flush("info");
	}
	
	public void setRequestValue(String[] arr) {
		reqValueArr = new String[arr.length];
		mklogger.temp(TAG, "=====RequestValue=====", false);
		mklogger.temp(this.psmt, false);
		String s = "Values : (";
		for(int i = 0; i < reqValueArr.length; i++) {
			reqValueArr[i] = arr[i];
			s += (arr[i].length() < 20 ? arr[i] : arr[i].substring(0, 19) + "...");
			if(i < reqValueArr.length - 1)
				s += ", ";
		}
		s += ")";
		mklogger.temp(s, false);
		mklogger.flush("info");
	}

	public void setRequestValue(ArrayList<String> arr, JSONObject jsonObject) {
		reqValue = new ArrayList<>();
		mklogger.temp(TAG, "=====RequestValue=====", false);
		mklogger.temp(this.psmt, false);
		String s = "Values : (";
		for(int i = 0; i < arr.size(); i++) {
			reqValue.add( jsonObject.get(arr.get(i)).toString() );
			s += (jsonObject.get(arr.get(i)).toString().length() < 20 ? jsonObject.get(arr.get(i)).toString() : jsonObject.get(arr.get(i)).toString().substring(0, 19) + "...");
			if(i < arr.size() - 1)
				s += ", ";
		}
		s += ")";
		mklogger.temp(s, false);
		mklogger.flush("info");
	}
	
	private Connection connectDB() throws SQLException{
		Connection conn = null;
		try {
			try {
				Class.forName("com.mysql.cj.jdbc.Driver");
			} catch (ClassNotFoundException e) {
				mklogger.error(TAG, "(connectDB) ClassNotFoundException: " + e.getMessage());
			}
			
			String url = "jdbc:mysql://" + MkConfigReader.Me().get("mkweb.db.hostname") + ":" + MkConfigReader.Me().get("mkweb.db.port") + "/" + MkConfigReader.Me().get("mkweb.db.database")+ "?" + "characterEncoding=UTF-8&serverTimezone=UTC";
			conn = DriverManager.getConnection(url, MkConfigReader.Me().get("mkweb.db.id"), MkConfigReader.Me().get("mkweb.db.pw"));
		}catch(SQLException e){
			mklogger.error(TAG, "(connectDB) SQLException : " + e.getMessage());
		}catch(Exception e){ 
			mklogger.error(TAG, e.getMessage());
		}
		
		return conn;
	}

	public ArrayList<Object> executeSEL(boolean asJson){
		ArrayList<Object> rst = new ArrayList<Object>();
		ResultSet rs = null;
		
		if(dbCon != null)
		{
			if(this.psmt != null)
			{
				try {
					PreparedStatement prestmt;
					prestmt = dbCon.prepareStatement(this.psmt, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
					
					if(reqValue != null) {
						for(int i = 0; i < reqValue.size(); i++) {
							prestmt.setString((i+1), reqValue.get(i));
						}
					}else {
						if(reqValueArr != null) {
							for(int i = 0; i < reqValueArr.length; i++)
								prestmt.setString((i+1), reqValueArr[i]);
						}
					}

					rs = prestmt.executeQuery(); 

					ResultSetMetaData rsmd; 
					int columnCount;
					String columnNames[];
					if(!rs.next()) {
						return null;
					}else {
						rsmd = rs.getMetaData();
					    columnCount = rsmd.getColumnCount();
					    columnNames = new String[columnCount];
					    for(int i=0; i < columnCount; i++) {
					    	String tempName = rsmd.getColumnName(i+1);
					    	String tempLabel = rsmd.getColumnLabel(i+1);
					    	
					    	columnNames[i] = tempName.contentEquals(tempLabel) ? tempName : tempLabel;
					    }
					}
					LinkedHashMap<String, Object> result = null;
					rs.beforeFirst();
					
					while(rs.next()) {
						result = new LinkedHashMap<String, Object>();

						for( String name : columnNames )
						{
							if(asJson)
								result.put("\""+name+"\"", "\""+rs.getObject(name)+"\"");
							else
								result.put(name, rs.getObject(name));
						}

						rst.add(result);
					}
					
					if(dbCon != null)
						dbCon.close();
					if(prestmt != null)
						prestmt.close();
					if(rs != null)
						rs.close();
				} catch (SQLException e) {
					mklogger.error(TAG, "(executeSEL) psmt = this.dbCon.prepareStatement(" + this.psmt + ") :" + e.getMessage());
				}
			}else {
				mklogger.debug(TAG, "psmt 널임");
			}
		}
		return rst;
	}
	
	public ArrayList<Object> executeSELLike(boolean asJson){
		ArrayList<Object> rst = new ArrayList<Object>();
		ResultSet rs = null;
		
		if(dbCon != null)
		{
			if(this.psmt != null)
			{
				try {
					PreparedStatement prestmt;
					prestmt = dbCon.prepareStatement(this.psmt, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
					
					if(reqValue != null) {
						for(int i = 0; i < reqValue.size(); i++) 
							prestmt.setString((i+1), "%" + reqValue.get(i) + "%");
					}else {
						if(reqValueArr != null) {
							for(int i = 0; i < reqValueArr.length; i++)
								prestmt.setString((i+1), "%" + reqValueArr[i] + "%");
						}
					}
					
					mklogger.debug(TAG, "prestmt: \n\n\n" + prestmt.toString());
					
					rs = prestmt.executeQuery(); 
					
					ResultSetMetaData rsmd; 
					int columnCount;
					String columnNames[];
					
					if(!rs.next()) {
						return null;
					}else {
						rsmd = rs.getMetaData();
						columnCount = rsmd.getColumnCount();
					    columnNames = new String[columnCount];
					    
					    for(int i=0; i < columnCount; i++) {
					    	String tempName = rsmd.getColumnName(i+1);
					    	String tempLabel = rsmd.getColumnLabel(i+1);
					    	
					    	columnNames[i] = tempName.contentEquals(tempLabel) ? tempName : tempLabel;
					    }
					}
					LinkedHashMap<String, Object> result = null;
					rs.beforeFirst();
					
					while(rs.next()) {
						result = new LinkedHashMap<String, Object>();
						for( String name : columnNames )
						{
							if(asJson)
								result.put("\""+name+"\"", "\""+rs.getObject(name)+"\"");
							else 
								result.put(name, rs.getObject(name));
						}
						
						rst.add(result);
					}
					
					if(dbCon != null)
						dbCon.close();
					if(prestmt != null)
						prestmt.close();
					if(rs != null)
						rs.close();
				} catch (SQLException e) {
					mklogger.error(TAG, "(executeSELLike) psmt = this.dbCon.prepareStatement(" + this.psmt + ") :" + e.getMessage());
				}
			}
		}
		return rst;
	}
	
	public int executeDML() {
		int result = 0;
		
		if(dbCon != null)
		{
			if(this.psmt != null)
			{
				try {
					PreparedStatement prestmt;
					prestmt = dbCon.prepareStatement(this.psmt, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
					
					if(reqValue != null) {
						for(int i = 0; i < reqValue.size(); i++) {
							prestmt.setString((i+1), reqValue.get(i));
						}
					}else {
						if(reqValueArr != null) {
							for(int i = 0; i < reqValueArr.length; i++) 
								prestmt.setString((i+1), reqValueArr[i]);
						}
					}
					
					result = prestmt.executeUpdate();
					
					if(dbCon != null)
						dbCon.close();
					if(prestmt != null)
						prestmt.close();
					
				} catch (SQLException e) {
					mklogger.error(TAG, "(executeDML) psmt = this.dbCon.prepareStatement(" + this.psmt + ") :" + e.getMessage());
				}
			}
		}
		
		return result;
	}
}
