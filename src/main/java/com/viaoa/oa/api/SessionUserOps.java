package com.viaoa.oa.api;

import com.viaoa.session.OASessionUser;

public interface SessionUserOps {

	OASessionUser<?> get();
	void set(OASessionUser<?> obj);
	
}
