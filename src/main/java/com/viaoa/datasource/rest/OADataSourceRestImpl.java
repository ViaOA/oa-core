package com.viaoa.datasource.rest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.viaoa.datasource.OADataSource;
import com.viaoa.datasource.OADataSourceIterator;
import com.viaoa.json.OAJson;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCacheDelegate;
import com.viaoa.object.OAObjectKey;

public class OADataSourceRestImpl implements OADataSourceRestInterface {
	private OADataSource defaultDataSource;

	private static Logger LOG = Logger.getLogger(OADataSourceRestImpl.class.getName());

	@Override
	public boolean getAssignIdOnCreate() {
		OADataSource ds = getDataSource();
		if (ds == null) {
			return false;
		}
		return ds.getAssignIdOnCreate();
	}

	@Override
	public boolean isAvailable() {
		OADataSource ds = getDataSource();
		if (ds == null) {
			return false;
		}
		return ds.isAvailable();
	}

	protected OADataSource getDataSource() {
		return getDataSource(null);
	}

	protected OADataSource getDataSource(Class c) {
		if (c != null) {
			OADataSource ds = OADataSource.getDataSource(c);
			if (ds != null) {
				return ds;
			}
		}
		if (defaultDataSource == null) {
			OADataSource[] dss = OADataSource.getDataSources();
			if (dss != null && dss.length > 0) {
				defaultDataSource = dss[0];
			}
		}
		return defaultDataSource;
	}

	@Override
	public int getMaxLength(Class clazz, String propertyName) {
		OADataSource ds = getDataSource(clazz);
		if (ds == null) {
			return 0;
		}

		int x = ds.getMaxLength(clazz, propertyName);
		return x;
	}

	@Override
	public boolean isClassSupported(Class clazz) {
		OADataSource ds = getDataSource(clazz);
		if (ds == null) {
			return false;
		}
		return ds != null;
	}

	@Override
	public void insertWithoutReferences(OAObject obj) {
		OADataSource ds = getDataSource(obj.getClass());
		if (ds == null) {
			return;
		}
		ds.insertWithoutReferences(obj);
	}

	@Override
	public void insert(OAObject obj) {
		OADataSource ds = getDataSource(obj.getClass());
		if (ds == null) {
			return;
		}
		ds.insert(obj);
	}

	@Override
	public void update(OAObject obj, String[] includeProperties, String[] excludeProperties) {
		OADataSource ds = getDataSource(obj.getClass());
		if (ds == null) {
			return;
		}
		ds.update(obj);
	}

	@Override
	public void save(OAObject obj) {
		OADataSource ds = getDataSource(obj.getClass());
		if (ds == null) {
			return;
		}
		ds.save(obj);
	}

	@Override
	public void delete(OAObject obj) {
		OADataSource ds = getDataSource(obj.getClass());
		if (ds == null) {
			return;
		}
		ds.delete(obj);
	}

	@Override
	public void deleteAll(Class c) {
		OADataSource ds = getDataSource(c);
		if (ds == null) {
			return;
		}
		ds.deleteAll(c);
	}

	@Override
	public int count(Class selectClass, String queryWhere, Object[] params, Class whereObjectClass, String whereKey,
			String propertyFromWhereObject, String extraWhere, int max) {
		OADataSource ds = getDataSource(selectClass);
		if (ds == null) {
			return 0;
		}

		//qqqqqqqqqqqqqqqqqqqqq
		//qqqqqqqq todo: get where object (if whereObjectClass != null)
		OAObject objWhere = null;

		int x = ds.count(selectClass, queryWhere, params, objWhere, propertyFromWhereObject, extraWhere, max);
		return x;
	}

	@Override
	public int countPassthru(Class selectClass, String queryWhere, int max) {
		OADataSource ds = getDataSource(selectClass);
		if (ds == null) {
			return 0;
		}
		int x = ds.countPassthru(selectClass, queryWhere, max);
		return x;
	}

	@Override
	public boolean supportsStorage() {
		OADataSource ds = getDataSource();
		if (ds == null) {
			return false;
		}
		return ds.supportsStorage();
	}

	private final AtomicInteger aiSelect = new AtomicInteger();
	private ConcurrentHashMap<Integer, Iterator> hashIterator = new ConcurrentHashMap<Integer, Iterator>(); // used to store DB

	@Override
	public int select(Class selectClass, String queryWhere, Object[] params, String queryOrderBy, Class whereObjectClass, String whereKey,
			String propertyFromWhereObject, String extraWhere, int max, boolean bDirty) {

		OADataSource ds = getDataSource(selectClass);
		if (ds == null) {
			return -1;
		}

		/*
			public abstract OADataSourceIterator select(Class selectClass,
			String queryWhere, Object[] params, String queryOrder,
			OAObject whereObject, String propertyFromWhereObject, String extraWhere,
			int max, OAFilter filter, boolean bDirty);

		*/

		OAObject objWhere = null;
		if (whereObjectClass != null && whereKey != null) {
			OAObjectKey ok = OAJson.convertJsonSinglePartIdToObjectKey(whereObjectClass, whereKey);

			objWhere = (OAObject) OAObjectCacheDelegate.get(whereObjectClass, ok);
			if (objWhere == null) {
				objWhere = (OAObject) OADataSource.getObject(whereObjectClass, ok);
			}
		}

		OADataSourceIterator iterator = ds.select(	selectClass, queryWhere, params, queryOrderBy, objWhere, propertyFromWhereObject,
													extraWhere,
													max, null, bDirty);

		int selectId = aiSelect.incrementAndGet();

		hashIterator.put(selectId, iterator);
		LOG.finer("add iterator, size=" + hashIterator.size());

		return selectId;
	}

	@Override
	public int selectPassThru(Class selectClass, String queryWhere, String queryOrder, int max, boolean bDirty) {
		OADataSource ds = getDataSource(selectClass);
		if (ds == null) {
			return -1;
		}

		OADataSourceIterator iterator = ds.select(selectClass, queryWhere, queryOrder, max, bDirty);

		int selectId = aiSelect.incrementAndGet();

		hashIterator.put(selectId, iterator);
		LOG.finer("add iterator, size=" + hashIterator.size());

		return selectId;
	}

	@Override
	public Object execute(String command) {
		OADataSource ds = getDataSource();
		if (ds == null) {
			return -1;
		}
		Object obj = ds.execute(command);
		return obj;
	}

	@Override
	public OAObject assignId(OAObject obj, Class<? extends OAObject> clazz) {
		if (obj == null) {
			return obj;
		}
		OADataSource ds = getDataSource(obj.getClass());
		if (ds == null) {
			return obj;
		}
		ds.assignId(obj);
		return obj;
	}

	@Override
	public boolean willCreatePropertyValue(OAObject object, String propertyName) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void updateMany2ManyLinks(Class masterClass, String masterId, OAObject[] adds, Class addClazz, OAObject[] removes,
			Class removeClazz, String propertyNameFromMaster) {
		// TODO Auto-generated method stub

	}

	@Override
	public OAObject[] next(int selectId, Class clazz) {
		Iterator iterator = (Iterator) hashIterator.get(selectId);
		if (iterator == null) {
			return null;
		}

		ArrayList<Object> al = new ArrayList();
		for (int i = 0; i < 500; i++) {
			if (!iterator.hasNext()) {
				break;
			}
			Object obj = iterator.next();
			al.add(obj);
			/*
			if (obj instanceof OAObject) {
				OAObject oa = (OAObject) obj;
				this.setCached(oa);
			}
			*/
		}
		int x = al.size();
		if (x == 0) {
			removeSelect(selectId);
		}
		OAObject[] objs = new OAObject[x];
		if (x > 0) {
			al.toArray(objs);
		}
		return objs;
	}

	@Override
	public void removeSelect(int selectId) {
		Iterator iterator = (Iterator) hashIterator.get(selectId);
		if (iterator == null) {
			return;
		}
		iterator.remove();
		hashIterator.remove(selectId);
		LOG.finer("remove iterator, size=" + hashIterator.size());
	}

}
