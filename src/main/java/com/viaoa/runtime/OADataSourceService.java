package com.viaoa.runtime;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import com.viaoa.datasource.OADataSource;
import com.viaoa.filter.OAFilter;

/* qqqqqqqqqqqqq
CODEX

#6 — concurrency risk
  File/class/method: src/main/java/com/viaoa/runtime/OADataSourceService.java:19
  Concern: setPosition synchronizes on alDataSource, but register and unregister do not use the same lock.
  CopyOnWriteArrayList keeps individual operations safe, but the remove/add reorder in setPosition is not atomic
  relative to concurrent register/unregister.
  Why it matters: datasource priority is runtime behavior. A concurrent registration/removal can produce unexpected
  ordering or make setPosition operate against a stale list state.
  Severity: invariant risk
  Minimal fix: use a dedicated registry lock for register, unregister, and setPosition, or document that datasource
  registration order is single-threaded startup-only.
  Suggested invariant: DATASOURCE_REGISTRY_ORDER_CHANGES_ARE_SERIALIZED
  Suggested test coverage: concurrent register/unregister/setPosition preserves deterministic final registry order.

 #7 — public API risk
  File/class/method: src/main/java/com/viaoa/runtime/OADataSourceService.java:28, src/main/java/com/viaoa/runtime/
  OADataSourceService.java:19
  Concern: datasource registry has no explicit lifecycle/reset API. This overlaps with the previously noted runtime
  reset problem, but specifically the datasource service has no local way to clear or replace registered
  datasources.
  Why it matters: OA 4.0 tests and modular bootstraps need deterministic datasource ownership. Without a clear
  lifecycle, stale datasource registrations can leak between runtime contexts.
  Severity: invariant risk
  Minimal fix: add package/runtime-scoped clearForTest() or runtime reset integration; keep it guarded if needed.
  Suggested invariant: DATASOURCE_REGISTRY_HAS_EXPLICIT_RUNTIME_LIFECYCLE
  Suggested test coverage: register multiple datasources, reset runtime, verify registry is empty or intentionally
  preserved.



*/


public class OADataSourceService {
	private Logger LOG = Logger.getLogger(OADataSourceService.class.getName());

	private final CopyOnWriteArrayList<OADataSource> alDataSource = new CopyOnWriteArrayList<OADataSource>();
	

	public OADataSourceService() {
	}

	public void register(OADataSource ds) {
		if (ds != null) alDataSource.addIfAbsent(ds);
	}
	
	public void unregister(OADataSource ds) {
		if (ds != null) alDataSource.remove(ds);
	}


	public OADataSource[] getAll() {
		return alDataSource.toArray(new OADataSource[alDataSource.size()]);
	}
	
	
	public OADataSource get(Class<?> clazz) {
		return get(clazz, (OAFilter<Class<?>>) null);
	}

	public OADataSource get(Class<?> clazz, OAFilter<?> filter) {
		// todo: create mru 
		OADataSource dsFound = null;
		for (OADataSource ds : getAll()) {
			if (!ds.getEnabled()) {
				continue;
			}

			if (dsFound == null || (dsFound.getLast() && !ds.getLast())) {
				if (!ds.isClassSupported(clazz, filter)) {
					continue;
				}
				dsFound = ds;
				if (!dsFound.getLast()) {
					break;
				}
			}
		}
		return dsFound;
	}
	
	public void setPosition(int pos, OADataSource ds) {
		if (ds == null) return;
		synchronized (alDataSource) {
			if (pos < 0) {
				pos = 0;
			}
			int x = alDataSource.indexOf(ds);
			if (x < 0) {
				return;
			}
			if (x == pos) {
				return;
			}
			alDataSource.remove(x);
			x = alDataSource.size();
			if (pos > x) {
				pos = x;
			}
			alDataSource.add(pos, ds);
		}
	}

	public int getPosition(OADataSource ds) {
		return alDataSource.indexOf(ds);
	}
	
	
}
