package com.viaoa.runtime;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import com.viaoa.datasource.OADataSource;
import com.viaoa.filter.OAFilter;



/**
 * Runtime state field used by OA services for {.
 */
public class OADataSourceService {
	private Logger LOG = Logger.getLogger(OADataSourceService.class.getName());

	private final CopyOnWriteArrayList<OADataSource> alDataSource = new CopyOnWriteArrayList<OADataSource>();
	

	/**
	 * Creates the runtime service instance.
	 */
	public OADataSourceService() {
	}

	/**
	 * Registers a datasource with the runtime datasource registry.
	 * @param ds the datasource to register
	 */
	public void register(OADataSource ds) {
		if (ds != null) alDataSource.addIfAbsent(ds);
	}
	
	/**
	 * Removes a datasource from the runtime datasource registry.
	 * @param ds the datasource to remove
	 */
	public void unregister(OADataSource ds) {
		if (ds != null) alDataSource.remove(ds);
	}


	/**
	 * Returns the All value.
	 *
	 * @return the All value
	 */
	public OADataSource[] getAll() {
		return alDataSource.toArray(new OADataSource[alDataSource.size()]);
	}
	
	
	/**
	 * Returns an OA runtime instance for the supplied context.
	 * @param clazz the lookup context
	 * @return the resolved OA runtime
	 */
	public OADataSource get(Class<?> clazz) {
		return get(clazz, (OAFilter<Class<?>>) null);
	}

	/**
	 * Returns an OA runtime instance for the supplied context.
	 * @param clazz the lookup context
	 * @param filter the lookup context
	 * @return the resolved OA runtime
	 */
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
	
	/**
	 * Sets the Position value.
	 * @param pos the Position value
	 * @param ds the Position value
	 */
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

	/**
	 * Returns the Position value.
	 *
	 * @param ds the lookup context
	 *
	 * @return the Position value
	 */
	public int getPosition(OADataSource ds) {
		return alDataSource.indexOf(ds);
	}
	
	
}
