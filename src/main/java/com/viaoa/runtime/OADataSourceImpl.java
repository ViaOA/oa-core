package com.viaoa.runtime;

import com.viaoa.runtime.datasource.OADataSourceService;

public class OADataSourceImpl implements OADataSource {
	private OADataSourceService dataSourceService;
	
	
	
	public OADataSourceService getDataSourceService() {
		return dataSourceService;
	}



	@Override
	public com.viaoa.datasource.OADataSource[] getDataSources() {
		return dataSourceService.getDataSources();
	}



	@Override
	public com.viaoa.datasource.OADataSource getDataSource(Class c) {
		return dataSourceService.getDataSource(c);
	}
	
}
