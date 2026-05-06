package com.viaoa.runtime;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import com.viaoa.datasource.OADataSource;
import com.viaoa.filter.OAFilter;

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
