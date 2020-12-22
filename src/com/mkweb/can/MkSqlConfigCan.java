package com.mkweb.can;

import java.io.File;

import com.mkweb.data.SqlJsonData;

public abstract class MkSqlConfigCan extends SqlJsonData{
	public abstract Object getControlService(String serviceName);
	public abstract void setSqlConfigs(File[] sqlConfigs);
}
