package com.viaoa.runtime;


public interface OADataSource {

	public com.viaoa.datasource.OADataSource[] getDataSources();
	
	public com.viaoa.datasource.OADataSource getDataSource(Class c);
	
	
}
