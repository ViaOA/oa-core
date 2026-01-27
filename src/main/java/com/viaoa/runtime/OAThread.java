package com.viaoa.runtime;

import com.viaoa.transaction.OATransaction;

public interface OAThread {

	public Object getContext();
	
	public void setContext(Object context);	
	
	public boolean isAdmin();

	public String getAllStackTraces();
	
	public OATransaction getTransaction();
	
	public boolean isRefreshing();
	
	
// lock, unlock, islocked	
	
}
