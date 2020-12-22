package com.mkweb.old.can.xml;

import java.io.File;
import com.mkweb.data.SqlXmlData;

public abstract class MkSqlConfigCan extends SqlXmlData{
	public abstract Object getControlService(String serviceName);
	public abstract void setSqlConfigs(File sqlConfigs);
}
