package com.viaoa.oa.service;

import com.viaoa.oa.OA;
import com.viaoa.oa.api.SessionUserOps;
import com.viaoa.runtime.OARuntime;
import com.viaoa.session.OASessionUser;

public class OASessionUserService implements SessionUserOps {

	private final OA oa;
	
	public OASessionUserService(OA oa) {
		this.oa = oa;
	}
	
	@Override
	public OASessionUser<?> get() {
		return OARuntime.thread().getThreadLocalService().getSessionUser();
	}

	@Override
	public void set(OASessionUser<?> su) {
		OARuntime.thread().getThreadLocalService().setSessionUser(su);
	}

}
