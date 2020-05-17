package com.mkweb.database;

import java.sql.Connection;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.mkweb.logger.MkLogger;

public class MkDbAccessor {
	//로그 만들기
	
	private Connection dbCon = null;
	private PreparedStatement psmt = null;
	
	String stmt = "";
	
	public MkDbAccessor() {
		try {
			dbCon = connectDB();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private Connection connectDB() throws SQLException{
		Connection conn = null;
		
		try {
			try {
				Class.forName("com.mysql.cj.jdbc.Driver");
			} catch (ClassNotFoundException e) {
				MkLogger.error("(line: 40) ClassNotFoundException: " + e.getMessage());
			}
			
			String url = "jdbc:mysql://eugenes.iptime.org:3306/mkweb?characterEncoding=UTF-8&serverTimezone=UTC";
			conn = DriverManager.getConnection(url, "mkweb", "mkweb");
		}catch(SQLException e){
			MkLogger.error("(line: 47) SQLException : " + e.getMessage());
		}catch(Exception e){ 
			MkLogger.error(" " + e.getMessage());
		}
		
		return conn;
	}

	private void queryLog(String query) {
		query = query.trim();
		String queryMsg = "";
		
		String[] queryBuffer = query.split("\n");
		
		for (int i = 0; i < queryBuffer.length; i++) {
			String tempQuery = queryBuffer[i].trim();
			queryMsg += "\n\t\t\t\t\t" + tempQuery;
		}
		
		MkLogger.info(queryMsg);
	}
	
	//DML
	public ArrayList<Object> executeSEL(String query){
		ArrayList<Object> rst = new ArrayList<Object>();
		ResultSet rs = null;
		
		if(dbCon != null)
		{
			if(this.stmt != null)
			{
				try {
					psmt = dbCon.prepareStatement(query);
					queryLog(query);
					
					rs = psmt.executeQuery(); 
					
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
					        columnNames[i] = rsmd.getColumnName(i+1); 
					    }
					}
					HashMap<String, Object> result = null;
					rs.beforeFirst();
					
					while(rs.next()) {
						result = new HashMap<String, Object>();
						for( String name : columnNames )
						{
							result.put(name, rs.getObject(name));
						}
						
						rst.add(result);
					}
					
					if(dbCon != null)
						dbCon.close();
					if(psmt != null)
						psmt.close();
					if(rs != null)
						rs.close();
				} catch (SQLException e) {
					MkLogger.error( "(line: 96~105) psmt = this.dbCon.prepareStatement(" + this.stmt + ") :" + e.getMessage());
				}
			}
		}
		return rst;
	}
}
