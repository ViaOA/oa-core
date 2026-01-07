package com.viaoa.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.viaoa.datasource.OADataSource;
import com.viaoa.runtime.datasource.OAJDBCDataSourceService;
import com.viaoa.util.OAFilter;


public class OADataSourceService {
	private Logger LOG = Logger.getLogger(OADataSourceService.class.getName());

	private final OARuntime runtime;

	private final OAJDBCDataSourceService srvcJdbc = new OAJDBCDataSourceService();
	
	OADataSourceService(OARuntime runtime) {
		this.runtime = runtime;
	}

	
	public OAJDBCDataSourceService jdbc() {
		return srvcJdbc;
	}
	public OAJDBCDataSourceService getJDBCDataSourceService() {
		return srvcJdbc;
	}
	
	/**
	 * List of all registered OADataSource instances. Used for global lookup,
	 * ordering, and management.
	 */
	private List<OADataSource> alDataSource = new ArrayList();

	/**
	 * Cached array of all registered data sources. Rebuilt when registration
	 * changes.
	 */
	private volatile OADataSource[] dsAll;
	
	
	/**
	 * Returns all registered DataSources. Results are cached in {@link #dsAll}
	 * until the registration changes.
	 *
	 * @return array of DataSource instances
	 */
	public OADataSource[] getDataSources() {
		if (dsAll == null) {
			synchronized (alDataSource) {
				if (dsAll == null) {
					int x = alDataSource.size();
					dsAll = new OADataSource[x];
					alDataSource.toArray(dsAll);
				}
			}
		}
		return dsAll;
	}

	/**
	 * Returns the first enabled DataSource that supports the given class.
	 *
	 * @param clazz class to evaluate
	 * @return supporting DataSource or null
	 */
	public OADataSource getDataSource(Class clazz) {
		return getDataSource(clazz, (OAFilter) null);
	}

	/**
	 * Returns a DataSource that supports the class and passes the filter.
	 * A DataSource marked as {@code bLast=true} is considered only after others.
	 *
	 * @param clazz class to evaluate
	 * @param filter optional filter used by the DataSource
	 * @return matching DataSource or null
	 */
	public OADataSource getDataSource(Class clazz, OAFilter filter) {
		OADataSource[] ds = getDataSources();
		if (ds == null) {
			return null;
		}
		int x = ds.length;
		OADataSource dsFound = null;
		for (int i = 0; ds != null && i < x; i++) {
			if (ds[i] == null) {
				continue;
			}
			if (!ds[i].getEnabled()) {
				continue;
			}

			if (dsFound == null || (dsFound.getLast() && !ds[i].getLast())) {
				if (!ds[i].isClassSupported(clazz, filter)) {
					continue;
				}
				dsFound = ds[i];
				if (!dsFound.getLast()) {
					break;
				}
			}
		}
		return dsFound;
	}

	/**
	 * Incremented whenever DataSource registration changes, allowing observers to
	 * detect configuration updates.
	 */
	protected int dataSourceChangeCnter;

	
	/** Closes all registered data sources and clears the global list. */
	public void closeAll() {
		synchronized (alDataSource) {
			dataSourceChangeCnter++;
			while (alDataSource.size() > 0) {
				((OADataSource) alDataSource.get(0)).close();
			}
			alDataSource.clear();
			dsAll = null;
		}
	}

	public void removeFromList(OADataSource ds) {
		synchronized (alDataSource) {
			alDataSource.remove(ds);
			dataSourceChangeCnter++;
			dsAll = null;
		}
	}

	
	public int getChangeCounter() {
		return dataSourceChangeCnter;
	}
	
	public void register(OADataSource ds) {
		synchronized (alDataSource) {
			if (!alDataSource.contains(ds)) {
				dsAll = null;
				alDataSource.add(ds);
				dataSourceChangeCnter++;
			}
		}

	}
	
	public void reopen(int pos, OADataSource ds) {
		synchronized (alDataSource) {
			if (!alDataSource.contains(ds)) {
				int x = alDataSource.size();
				pos = Math.max(0, Math.min(x, pos));
				alDataSource.add(pos, ds);
				dataSourceChangeCnter++;
				dsAll = null;
			}
		}
	}

	public void setPosition(int pos, OADataSource ds) {
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
			dataSourceChangeCnter++;
			alDataSource.remove(x);
			x = alDataSource.size();
			if (pos > x) {
				pos = x;
			}
			alDataSource.add(pos, ds);
			dsAll = null;
		}
	}

	public int getPosition(OADataSource ds) {
		return alDataSource.indexOf(ds);
	}

}
