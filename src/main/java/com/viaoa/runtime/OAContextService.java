package com.viaoa.runtime;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.viaoa.object.OAObject;
import com.viaoa.runtime.context.OAContext;
import com.viaoa.runtime.context.OAContextAccess;
import com.viaoa.runtime.context.OAContextUser;

/*qqqqqqqqqqqqqqq
CODEX

MEDIUM — default context user still grants all context property checks
  src/main/java/com/viaoa/runtime/OAContextService.java:47 creates a default user whose isEnabled(path, value,
  checkSuperAdmin) always returns true. You changed isAdmin() and isSuperAdmin() to false, which helps, but object
  callback checks that use context enabled/visible property paths will still pass if no thread context user is
  installed. If this is the intended “system runtime fallback,” it needs to stay very explicit because missing user
  context becomes allow-all for those checks.

*/

public class OAContextService {
	
	private final Map<Object, OAContext> hmContext = new ConcurrentHashMap<>();
	private volatile OAContextUser<?> defaultContextUser;

	public OAContext get(Object key) {
		if (key == null) return null;
		return hmContext.get(key);
	}
	
	public void register(OAContext<?, ?> ctx) {
		if (ctx == null) return; 
		Object key = ctx.getKey();
		if (key != null) hmContext.put(key, ctx);
	}

	public void unregister(Object key) {
		if (key != null) hmContext.remove(key);
	}
	
	public OAContextUser<?> getDefaultContextUser() {
		OAContextUser<?> cu = OARuntime.thread().getThreadLocalService().getContextUser();
		if (cu == null) {
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
				
				OAContext cxt = new OAContext("oa.system", ca);
				register(cxt);
				
				defaultContextUser = new OAContextUser(cxt) {
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
				cxt.addContextUser("default.user", defaultContextUser);
			}
			cu = defaultContextUser;
		}
		return cu;
	}
	
}
