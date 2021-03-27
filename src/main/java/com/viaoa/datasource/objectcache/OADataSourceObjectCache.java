/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.viaoa.datasource.objectcache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import com.viaoa.comm.io.OAObjectInputStream;
import com.viaoa.datasource.OADataSource;
import com.viaoa.datasource.OADataSourceEmptyIterator;
import com.viaoa.datasource.OADataSourceIterator;
import com.viaoa.datasource.autonumber.OADataSourceAuto;
import com.viaoa.filter.OAAndFilter;
import com.viaoa.filter.OAEqualFilter;
import com.viaoa.filter.OAQueryFilter;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCacheDelegate;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectPropertyDelegate;
import com.viaoa.object.OAObjectSerializer;
import com.viaoa.util.OAArray;
import com.viaoa.util.OACompare;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OALogger;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;

// 20140124
/**
 * OADataSource for OAObjectCache.
 * <p>
 * Uses OAFinder to find objects. This will use OAObjectCache.selectAllHubs along with any OAObject.OAClass.rootTreePropertyPaths ex:
 * "[Router]."+Router.P_UserLogins+"."+UserLogin.P_User to find all of the objects available. subclassed to allow initializeObject(..) to
 * auto assign Object Ids
 */
public class OADataSourceObjectCache extends OADataSourceAuto {
	private static final Logger LOG = OALogger.getLogger(OADataSourceObjectCache.class);

	private final ConcurrentHashMap<Class, ArrayList> hmClassList = new ConcurrentHashMap();

	public OADataSourceObjectCache() {
		this(true);
	}

	public OADataSourceObjectCache(boolean bRegister) {
		super(false);
		if (!bRegister) {
			removeFromList();
		}
	}

	//TODO:  this does not sort

	@Override
	public OADataSourceIterator select(final Class selectClass,
			String queryWhere, Object[] params, String queryOrder,
			OAObject whereObject, String propertyFromWhereObject, String extraWhere,
			int max, OAFilter filterx, boolean bDirty) {

		if (extraWhere != null && OAString.isNotEmpty(extraWhere.trim())) {
			try {
				OAFilter filter2 = new OAQueryFilter(selectClass, extraWhere, null);
				if (filterx == null) {
					filterx = filter2;
				} else {
					filterx = new OAAndFilter(filterx, filter2);
				}
			} catch (Exception e) {
				throw new RuntimeException("query parsing failed", e);
			}
		}

		if (!OAString.isEmpty(queryWhere)) {
			try {
				OAFilter filter2 = new OAQueryFilter(selectClass, queryWhere, params);
				if (filterx == null) {
					filterx = filter2;
				} else {
					filterx = new OAAndFilter(filterx, filter2);
				}
			} catch (Exception e) {
				throw new RuntimeException("query parsing failed", e);
			}
		} else if (whereObject != null && propertyFromWhereObject != null) {
			OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(whereObject.getClass());
			OALinkInfo li = oi.getLinkInfo(propertyFromWhereObject);
			if (li != null) {
				li = li.getReverseLinkInfo();
			} else {
				// 20200219 check to see if it's a propertyPath.  If so, then add to the query and re-select
				OAPropertyPath pp = new OAPropertyPath(whereObject.getClass(), propertyFromWhereObject);

				OALinkInfo[] lis = pp.getLinkInfos();
				if (lis != null) {
					for (int i = 0; i < lis.length; i++) {
						if (lis[i].getType() != OALinkInfo.ONE) {
							break;
						}
						Object objx = lis[i].getValue(whereObject);
						whereObject = (OAObject) objx;

						if (whereObject == null) {
							return new OADataSourceEmptyIterator();
						}
						// shorten pp
						int pos = propertyFromWhereObject.indexOf('.');
						int pos2 = propertyFromWhereObject.indexOf(')');
						if (pos < pos2) {
							pos = propertyFromWhereObject.indexOf('.', pos2);
						}
						propertyFromWhereObject = propertyFromWhereObject.substring(pos + 1);
						pp = new OAPropertyPath(whereObject.getClass(), propertyFromWhereObject);
					}
				}
				pp = pp.getReversePropertyPath();
				if (pp == null) {
					return new OADataSourceEmptyIterator();
				}

				if (OAString.isNotEmpty(queryWhere)) {
					queryWhere += " AND ";
				} else if (queryWhere == null) {
					queryWhere = "";
				}
				queryWhere += pp.getPropertyPath() + " == ?";
				params = OAArray.add(Object.class, params, whereObject);
				return select(selectClass, queryWhere, params, queryOrder, null, null, extraWhere, max, filterx, bDirty);
			}

			if (li != null) {
				final OAObject whereObjectx = whereObject;
				final OALinkInfo lix = li;
				OAFilter filter2 = new OAEqualFilter(li.getName(), whereObject) {
					public boolean isUsed(Object obj) {
						boolean b;
						if (obj instanceof OAObject) {
							Object objx = OAObjectPropertyDelegate.getProperty((OAObject) obj, lix.getName());
							b = OACompare.isEqual(objx, whereObjectx);
						} else {
							b = super.isUsed(obj);
						}
						return b;
					}
				};
				if (filterx == null) {
					filterx = filter2;
				} else {
					filterx = new OAAndFilter(filterx, filter2);
				}
			} else {
				throw new RuntimeException("whereObject's propertyFromWhereObject is not a valid link, whereObject=" + whereObject
						+ ", propertyFromWhereObject=" + propertyFromWhereObject);
			}
		} else {
			filterx = new OAFilter() {
				@Override
				public boolean isUsed(Object obj) {
					return true;
				}
			};
		}

		ObjectCacheIterator itx = new ObjectCacheIterator(selectClass, filterx);
		itx.setMax(max);
		return itx;
	}

	@Override
	public OADataSourceIterator selectPassthru(Class selectClass,
			String queryWhere, String queryOrder,
			int max, OAFilter filter, boolean bDirty) {
		if (!OAString.isEmpty(queryWhere)) {
			filter = new OAFilter() {
				@Override
				public boolean isUsed(Object obj) {
					return false;
				}
			};
		}
		return new ObjectCacheIterator(selectClass, filter);
	}

	public @Override void assignId(OAObject obj) {
		super.assignId(obj); // have autonumber handle this
	}

	public boolean getSupportsPreCount() {
		return false;
	}

	protected boolean isOtherDataSource() {
		OADataSource[] dss = OADataSource.getDataSources();
		return dss != null && dss.length > 1;
	}

	@Override
	public boolean isClassSupported(Class clazz, OAFilter filter) {
		if (filter == null) {
			if (isOtherDataSource()) {
				return false;
			}
			return super.isClassSupported(clazz, filter);
		}
		// only if all objects are loaded, or no other DS
		if (!isOtherDataSource()) {
			return true;
		}

		if (OAObjectCacheDelegate.getSelectAllHub(clazz) != null) {
			return true;
		}
		return false;
	}

	@Override
	public void insert(OAObject object) {
		super.insert(object);
		if (object == null) {
			return;
		}
		List al = getList(object.getClass());
		al.add(object);
	}

	public void saveToStorageFile(File file) throws Exception {
		LOG.fine("saving to storage file=" + file);
		if (file == null) {
			return;
		}
		FileOutputStream fos = new FileOutputStream(file);

		Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
		DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(fos, deflater, 1024 * 16);

		ObjectOutputStream oos = new ObjectOutputStream(deflaterOutputStream);

		for (Entry<Class, ArrayList> entry : hmClassList.entrySet()) {
			oos.writeBoolean(true);

			Class c = entry.getKey();
			oos.writeObject(c);

			List al = entry.getValue();
			OAObjectSerializer wrap = new OAObjectSerializer(al, false, false);
			wrap.setIncludeBlobs(true);
			oos.writeObject(wrap);
		}
		oos.writeBoolean(false);
		oos.close();

		deflaterOutputStream.finish();
		deflaterOutputStream.close();
		fos.close();
		LOG.fine("saved to storage file=" + file);
	}

	public boolean loadFromStorageFile(final File file) throws Exception {
		LOG.fine("loading to storage file=" + file);
		if (file == null) {
			return false;
		}
		if (!file.exists()) {
			LOG.fine("storage file=" + file + " does not exist");
			return false;
		}

		FileInputStream fis = new FileInputStream(file);
		Inflater inflater = new Inflater();
		InflaterInputStream inflaterInputStream = new InflaterInputStream(fis, inflater, 1024 * 16);

		OAObjectInputStream ois = new OAObjectInputStream(inflaterInputStream);

		int cnt = 0;
		for (;;) {
			boolean b = ois.readBoolean();
			if (!b) {
				break;
			}
			cnt++;
			Class c = (Class) ois.readObject();
			OAObjectSerializer wrap = (OAObjectSerializer) ois.readObject();
			ArrayList al = (ArrayList) wrap.getObject();
			hmClassList.put(c, al);
		}

		ois.close();
		fis.close();
		LOG.fine("loaded storage file=" + file);
		return (cnt > 0);
	}

	private ArrayList getList(Class c) {
		if (c == null) {
			return null;
		}
		ArrayList al = hmClassList.get(c);
		if (al == null) {
			synchronized (hmClassList) {
				al = new ArrayList();
				hmClassList.put(c, al);
			}
		}
		return al;
	}

	@Override
	public void insertWithoutReferences(OAObject obj) {
		super.insertWithoutReferences(obj);
		if (obj == null) {
			return;
		}
		List al = getList(obj.getClass());
		if (!al.contains(obj)) {
			al.add(obj);
		}
	}

	@Override
	public void delete(OAObject obj) {
		super.delete(obj);
		if (obj == null) {
			return;
		}
		final Class c = obj.getClass();
		ArrayList al = (ArrayList) hmClassList.get(c);
		if (al != null) {
			al.remove(obj);
		}
	}

	@Override
	public void deleteAll(Class c) {
		super.deleteAll(c);
		if (c == null) {
			return;
		}
		ArrayList al = (ArrayList) hmClassList.get(c);
		if (al != null) {
			al.clear();
		}
		OAObjectCacheDelegate.removeAllObjects(c);
	}

	public void addToStorage(OAObject obj, boolean bIsLoading) {
		if (obj == null) {
			return;
		}
		ArrayList al = getList(obj.getClass());
		if (!bIsLoading || !al.contains(obj)) {
			al.add(obj);
		}
	}
}
