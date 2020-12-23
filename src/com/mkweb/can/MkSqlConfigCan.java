package com.mkweb.can;

import java.io.File;

import com.mkweb.data.SqlJsonData;

public abstract class MkSqlConfigCan extends SqlJsonData{
	public abstract Object getControl(String controlName);
	public abstract Object getControlByServiceName(String serviceName);
	public abstract void setSqlConfigs(File[] sqlConfigs);
}
