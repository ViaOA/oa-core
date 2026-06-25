package com.viaoa.runtime;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.viaoa.lang.oa.VInteger;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.context.OAContext;
import com.viaoa.runtime.context.OAContextAccess;
import com.viaoa.runtime.context.OAContextUser;

public class OAContextService {
	private final Map<Object, OAContext> hmContext = new ConcurrentHashMap<>();
	private volatile OAContextUser<? extends OAObject> defaultContextUser;

	public OAContext<?, ?> get(Object key) {
		if (key == null) key = OAContext.NullKey;
		return hmContext.get(key);
	}
	
	public void register(OAContext<?, ?> ctx) {
		if (ctx == null) return; 
		Object key = ctx.getKey();
		hmContext.put(key, ctx);
	}

	public void unregister(Object key) {
		if (key != null) hmContext.remove(key);
	}
	
	
	public void setDefaultContextUser(OAContextUser<?> cu) {
		this.defaultContextUser = cu;
	}
	
	
	
	public OAContextUser<?> getDefaultContextUser() {
		OAContextUser<?> cu = OARuntime.thread().getThreadLocalService().getContextUser();
		if (cu != null) return cu;
		
		if (defaultContextUser == null) {
			OAContextAccess ca = new OAContextAccess() {
				protected boolean getEnabled(final OAObject obj, final Class cz, final String propertyName, final boolean bDefault) {
					return true;
				}
				@Override
				protected boolean getVisible(final OAObject obj, final Class cz, final String propertyName, final boolean bDefault) {
					return true;
				}
			};
			
			final OAContext<String, VInteger> cxt = new OAContext("oa.system", ca);
			register(cxt);
			
			VInteger vint = new VInteger(0);
			OAContextUser<VInteger> cux = new OAContextUser<VInteger>(cxt, vint) {
				@Override
				public boolean isEnabled(String path, boolean bEqualTo, boolean bCheckSuperAdmin) {
					return true;
				}
				@Override
				public boolean isAdmin() {
					return false;
				}
				@Override
				public boolean isSuperAdmin() {
					return false;
				}
			};
			defaultContextUser = cux;
			cxt.addContextUser("default.user", cux);
		}
		cu = defaultContextUser;

		return cu;
	}
	
}
