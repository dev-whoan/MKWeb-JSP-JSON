package com.mkweb.old.config.xml;
/*
REMOVED

import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.stream.Collectors;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mkweb.can.MkPageConfigCan;
import com.mkweb.data.PageXmlData;
import com.mkweb.logger.MkLogger;

public class MkRestApiPageConfigs_old extends MkPageConfigCan{
	private HashMap<String, ArrayList<PageXmlData>> page_configs = new HashMap<String, ArrayList<PageXmlData>>();
	private File[] defaultFiles = null;
	
	private long[] lastModified = null;
	
	private static MkRestApiPageConfigs_old mrapc = null;
	private MkLogger mklogger = MkLogger.Me();
	
	private String configName = "Rest Api Page";
	private String TAG = "[MkRestApiPageConfigs]";
	private boolean isSet = false;

	public boolean isApiPageSet() {	return this.isSet;	}
	
	public static MkRestApiPageConfigs_old Me() {
		if(mrapc == null)
			mrapc = new MkRestApiPageConfigs_old();
		return mrapc;
	}
	
	private String[] ctr_list = {
		"name",
		"debug",
		"authorized",
		"post",
		"get",
		"put",
		"delete",
		"options",
		"head"
	};
	private String[] ctr_info = new String[ctr_list.length];
	
	private String[] svc_list = {
		"obj",
		"result",
		"method"
	};
	private ArrayList<String> setPageParamToStrig(String pageParam) {
		if(pageParam == null)
			return null;
		String[] tempPageParam = pageParam.split("@set" + "\\(");
		String[] tempPageParam2 = new String[tempPageParam.length];
		if(tempPageParam.length == 1) 
			return null;
		
		for(int i = 0; i < tempPageParam.length; i++) {
			tempPageParam2[i] = tempPageParam[i].split("=")[0];
		}
		
		if(tempPageParam2.length == 1)
			return null;
		
		ArrayList<String> result = new ArrayList<String>();
		
		for(int i = 1; i < tempPageParam2.length; i++) {
			result.add(tempPageParam2[i].trim());
		}
		
		return result;
	}
	@Override
	public void setPageConfigs(File[] pageConfigs) {
		isSet = false;
		super.setClList(ctr_list);
		super.setSlList(svc_list);
		page_configs.clear();
		defaultFiles = pageConfigs;
		ArrayList<PageXmlData> xmlData = null;
		lastModified = new long[pageConfigs.length];
		int lmi = 0;
		for(File defaultFile : defaultFiles)
		{
			lastModified[lmi++] = defaultFile.lastModified();
			mklogger.info("=*=*=*=*=*=*=* MkWeb " + this.configName + " Configs  Done*=*=*=*=*=*=*=*=");
			mklogger.info(TAG + "File: " + defaultFile.getAbsolutePath());
			mklogger.info("=            " + defaultFile.getName() +"              =");
			if(defaultFile == null || !defaultFile.exists())
			{
				mklogger.error("Config file is not exists or null");
				return;
			}
			NodeList nodeList = setNodeList(defaultFile);

			if(nodeList != null) {
				for(int i = 0; i < nodeList.getLength(); i++)
				{
					Node node = nodeList.item(i);

					if(node.getNodeName().equals("Control"))
					{
						if(node.getNodeType() == Node.ELEMENT_NODE) {
							
							for(int cl = 0; cl < ctr_list.length; cl++) {
								if(node.getAttributes().getNamedItem(ctr_list[cl]) != null)
								{
									ctr_info[cl] = node.getAttributes().getNamedItem(ctr_list[cl]).getNodeValue();
								}
							}

							xmlData = new ArrayList<PageXmlData>();
							Element elem = (Element) node;
							NodeList services = elem.getElementsByTagName("Service");
							
							//새로 추가하는 부분
							NodeList pageParamNodes = elem.getElementsByTagName("PageParams");
							String pageParamsName = null;
							ArrayList<String> pageParam = null;
							if(pageParamNodes.item(0) != null) {
								Node pageParamNode = pageParamNodes.item(0);
								pageParamsName = pageParamNode.getAttributes().getNamedItem("name") != null ?
										pageParamNode.getAttributes().getNamedItem("name").getNodeValue():
											null;
								pageParam = setPageParamToStrig(pageParamNode.getTextContent());
							}else {
								pageParam = null;
							}
							
							if(services.item(0) != null) {
								for(int j = 0; j < services.getLength(); j++) {
									Node service = services.item(j);
									NodeList service_param = service.getChildNodes();

									if(service.getNodeType() == Node.ELEMENT_NODE)
									{
										String[] SQL_INFO = new String[svc_list.length];
										String PRM_NAME = null;
										String VAL_INFO = null;
										String STRUCTURE = null;
										for(int k = 0; k < service_param.getLength(); k++) {
											Node service_info = service_param.item(k);

											if(service_info.getNodeType() == Node.ELEMENT_NODE)
											{
												NamedNodeMap attributes = service_info.getAttributes();

												switch(service_info.getNodeName()) {
												case "Sql":
													for(int sli = 0; sli < svc_list.length; sli++) {
														Node tN = attributes.getNamedItem(svc_list[sli]);
														SQL_INFO[sli] = (tN != null ? tN.getNodeValue() : null);
													}
													break;
												case "returnStructure":
													STRUCTURE = attributes.getNamedItem("value").getNodeValue();
													break;
												case "Parameter":
													PRM_NAME = attributes.getNamedItem("name").getNodeValue();
													break;
												case "Value":
													VAL_INFO = service_info.getTextContent();
													VAL_INFO = VAL_INFO.trim();
													break;
												}
											}
										}

										String serviceName = service.getAttributes().getNamedItem("id") != null ? service.getAttributes().getNamedItem("id").getNodeValue() : null;
										String serviceType = service.getAttributes().getNamedItem("type").getNodeValue() + "." + SQL_INFO[0];
										
										//RestApiPageXmlData
										PageXmlData rapData = setPageXmlData(pageParamsName, pageParam, serviceName, serviceType, ctr_info, SQL_INFO, PRM_NAME, VAL_INFO, STRUCTURE);
										printPageInfo(rapData, "info");
										xmlData.add(rapData);
										page_configs.put(ctr_info[0], xmlData);
									}
								}
							}else {
								String PRM_NAME = "No Parameter";
								String VAL_INFO = "No Value";
								String STRUCTURE = "No Structure";
								
								String[] temp_sql = new String[svc_list.length];
								for(String s : temp_sql) {	s = "no data";	}
								
								ctr_info[0] = node.getAttributes().getNamedItem("name").getNodeValue();
								
								PageXmlData rapData = setPageXmlData(pageParamsName, pageParam, "No Service", "No Service", ctr_info, temp_sql, PRM_NAME, VAL_INFO, STRUCTURE);
								printPageInfo(rapData, "info");
								xmlData.add(rapData);
								page_configs.put(ctr_info[0], xmlData);
							}
							
						}
					}
				}
			}else {
				mklogger.info(TAG + " No " + this.configName + " Page Control has found. If you set Page configs, please check Page config files and web.xml.");
			}
			isSet = true;
			mklogger.info("=*=*=*=*=*=*=* MkWeb " + this.configName + " Configs  Done*=*=*=*=*=*=*=*=");
		}
	}
	@Override
	protected PageXmlData setPageXmlData(String pageStaticParamName, ArrayList<String> pageStaticParam, String serviceName, String serviceType, String[] ctr_info, String[] sqlInfo, String PRM_NAME, String VAL_INFO, String STRUCTURE) {
		PageXmlData result = new PageXmlData();
		result.setControlName(ctr_info[0]);
		result.setPageStaticParamName(pageStaticParamName);
		result.setPageStaticParams(pageStaticParam);
		result.setServiceName(serviceName);
		result.setServiceType(serviceType);

		result.setPageName(ctr_info[0]);
		result.setDebug(ctr_info[1]);
		
		result.setAuthorizedRequire(ctr_info[2]);
		result.setPost(ctr_info[3]);
		result.setGet(ctr_info[4]);
		result.setPut(ctr_info[5]);
		result.setDelete(ctr_info[6]);
		result.setOptions(ctr_info[7]);
		result.setHead(ctr_info[8]);
		
		result.setSql(sqlInfo);
		result.setParameter(PRM_NAME);
		result.setData(VAL_INFO);
		
		result.setStructure(STRUCTURE);

		LinkedHashMap<String, Boolean> PAGE_VALUE = null;
		PAGE_VALUE = pageValueToHashMap(VAL_INFO);
		result.setPageValue(PAGE_VALUE);
		return result;
	}
	
	@Override
	public void printPageInfo(PageXmlData xmlData, String type) {
		String controlName = xmlData.getControlName();
		String pageParamName = xmlData.getPageStaticParamsName();
		ArrayList<String> pageParams = xmlData.getPageStaticParams();
		String serviceName = xmlData.getServiceName();

		String pageDir = xmlData.getDir();
		String pageName = xmlData.getPageName();
		String debugLevel = xmlData.getDebug();

		String[] SQL_INFO = xmlData.getSql();
		String sql_info = "";
		for(int i = 0; i < SQL_INFO.length; i++) {
			if(i != SQL_INFO.length-1)
				sql_info += SQL_INFO[i] + "\t";
			else
				sql_info += SQL_INFO[i];
		}
		String PRM_NAME = xmlData.getParameter();
		String VAL_INFO = xmlData.getData();

		boolean authorized = xmlData.getAuthorizedRequire();
		boolean[] methods = {
				xmlData.getPost(),
				xmlData.getGet(),
				xmlData.getPut(),
				xmlData.getDelete(),
				xmlData.getOptions(),
				xmlData.getHead()
		};
		
		String valMsg = "";
		String[] valBuffer = VAL_INFO.split("\n");
		for (int ab = 0; ab < valBuffer.length; ab++) {
			String tempVal = valBuffer[ab].trim();
			if(valMsg == "")
				valMsg = tempVal;
			else
				valMsg += ("\n\t" + tempVal);
		}
		String PRM = "";
		if(pageParams != null) {
			for(int pr = 0; pr < pageParams.size(); pr++) {
				PRM += pageParams.get(pr);
				if(pr < pageParams.size() - 1)
					PRM += ", ";
			}
		}

		String tempMsg = "\n┌──────────────────────────Page Control  :  " + controlName + "────────────────────────────"
				+ "\n│View Dir:\t" + pageDir + "\t\tView Page:\t" + pageName
				+ "\n│Debug Level:\t" + debugLevel
				+ "\n│Static Param Name:\t" + pageParamName + "\t\tStatic Param Value:\t" + PRM
				+ "\n│Service Name:\t" + serviceName + "\tParameter:\t" + PRM_NAME
				+ "\n│Authorized  :\t" + authorized + "\tPost:\t" + methods[0] + "\tGet:\t" + methods[1]
				+ "\n│Put         :\t" + methods[2] + "\tDelete:\t" + methods[3]
						+ "\tOptions:\t" + methods[4] + "\tHead:\t"+ methods[5];

		if(type == "info") {
			tempMsg += "\n│SQL:\t" + sql_info
					+ "\n│Value:\t" + valMsg
					+ "\n└───────────────────────────────────────────────────────────────────────────";
		}else {
			tempMsg += "\n└───────────────────────────────────────────────────────────────────────────";
		}
		mklogger.temp(tempMsg, false);
		mklogger.flush("info");
	}
	@Override
	public ArrayList<PageXmlData> getControl(String k) {
		
		for(int i = 0; i < defaultFiles.length; i++)
		{
			if(lastModified[i] != defaultFiles[i].lastModified()){
				setPageConfigs(defaultFiles);
				mklogger.info("==============Reload " + this.configName + " Config files==============");
				mklogger.info("========Caused by  : different modified time========");
				mklogger.info("==============Reload " + this.configName + " Config files==============");
				break;
			}
		}

		if(k == null) {
			mklogger.error(TAG + " : Input String data is null");
			return null;
		}

		if(page_configs.get(k) == null)
		{
			mklogger.error(TAG + " : The control is unknown. [called control name: " + k + "]");
			return null;
		}
		return page_configs.get(k);
	}
}
*/
