package com.mkweb.restapi;

import java.sql.Connection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import com.mkweb.config.MkConfigReader;
import com.mkweb.database.MkDbAccessor;
import com.mkweb.logger.MkLogger;

public class MkRestApiGetKey extends MkDbAccessor {
	private Connection dbCon = null;
	private String psmt = null;
	private static final String TAG = "[MkRestApiGetKey]";
	private static final MkLogger mklogger = new MkLogger(TAG);
	public MkRestApiGetKey() {
		super();
		dbCon = super.getDbCon();
	}

	public ArrayList<Object> GetKey(){
		ArrayList<Object> rst = new ArrayList<Object>();
		ResultSet rs = null;
		
		if(dbCon != null)
		{
			try {
				PreparedStatement prestmt;
				this.psmt = "SELECT * FROM " + MkConfigReader.Me().get("mkweb.restapi.key.table") + ";";
				prestmt = dbCon.prepareStatement(this.psmt, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

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
				if(prestmt != null)
					prestmt.close();
				if(rs != null)
					rs.close();
			} catch (SQLException e) {
				mklogger.error("(GetKey) psmt = this.dbCon.prepareStatement(" + psmt + ") :" + e.getMessage());
			}
		}else {
			mklogger.error(" dbCon is null");
		}
		return rst;
	}
}
