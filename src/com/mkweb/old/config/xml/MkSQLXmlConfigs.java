package com.mkweb.old.config.xml;

import java.io.File;
import java.util.HashMap;
import org.w3c.dom.Node;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import com.mkweb.can.MkSqlConfigCan;
import com.mkweb.data.SqlXmlData;
import com.mkweb.logger.MkLogger;

public class MkSQLXmlConfigs extends MkSqlConfigCan {
	private HashMap<String, SqlXmlData> sql_configs = new HashMap<String, SqlXmlData>();
	private File defaultFile = null;
	private static MkSQLXmlConfigs sxc = null;
	private long lastModified = 0L;
	private MkLogger mklogger = MkLogger.Me();
	private String TAG = "[SQLXmlConfigs]";
	
	private String[] svc_list = {
			"id",
			"db"
	};
	private String[] svc_info = new String[svc_list.length];
	
	public static MkSQLXmlConfigs Me() {
		if(sxc == null)
			sxc = new MkSQLXmlConfigs();
		return sxc;
	}
	
	public void setSqlConfigs(File sqlConfigs) {
		sql_configs.clear();
		defaultFile = sqlConfigs;
		
		mklogger.info("=*=*=*=*=*=*=* MkWeb Sql  Configs Start*=*=*=*=*=*=*=*=");
		mklogger.info(TAG + "File: " + defaultFile.getAbsolutePath());
		if(defaultFile == null || !defaultFile.exists())
		{
			mklogger.error("Config file is not exists or null");
			return;
		}
		
		NodeList nodeList = setNodeList(defaultFile);
		
		if(nodeList != null) {
			lastModified = defaultFile.lastModified();
			
			for(int i = 0; i < nodeList.getLength(); i++)
			{
				Node node = nodeList.item(i);
				if(node.getNodeType() == Node.ELEMENT_NODE) {
					NamedNodeMap attributes = node.getAttributes();
					
					for(int sli = 0; sli < svc_list.length; sli++) {
						Node tN = attributes.getNamedItem(svc_list[sli]);
						svc_info[sli] = (tN != null ? tN.getNodeValue() : null);
					}
					
					String query = node.getTextContent();
					
					SqlXmlData xmlData = new SqlXmlData();
					
					xmlData.setControlName("MkSQL");
					//ID = 0, DB = 1
					xmlData.setServiceName(svc_info[0]);
					xmlData.setDB(svc_info[1]);
					xmlData.setData(query);

					sql_configs.put(svc_info[0], xmlData);
					
					mklogger.info("SQL ID :\t\t\t" + svc_info[0]);
					mklogger.info("SQL DB :\t\t\t" + svc_info[1]);

					query = query.trim();
					String queryMsg = "";
					
					String[] queryBuffer = query.split("\n");
					
					for (int j = 0; j < queryBuffer.length; j++) {
						String tempQuery = queryBuffer[j].trim();
						queryMsg += "\t\t\t\t\t\t\t\t" + tempQuery + "\n";
					}
					
					mklogger.info("query  :\n" + queryMsg + "\n");
					
				}
			}
		}else {
			mklogger.info(TAG + " No SQL Service has found. If you set SQL service, please check SQL config and web.xml.");
		}
		
		
		mklogger.info("=*=*=*=*=*=*=* MkWeb Sql  Configs  Done*=*=*=*=*=*=*=*=");
	}
	
	public SqlXmlData getControlService(String serviceName) {
		if(lastModified != defaultFile.lastModified()) {
			setSqlConfigs(defaultFile);
			mklogger.info("==============Reload SQL Config files==============");
			mklogger.info("========Caused by : different modified time========");
			mklogger.info("==============Reload SQL Config files==============");
		}
		
		return sql_configs.get(serviceName);
	}
}
