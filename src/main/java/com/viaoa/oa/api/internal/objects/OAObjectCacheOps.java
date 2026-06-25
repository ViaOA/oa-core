package com.viaoa.oa.api.internal.objects;

import java.util.List;

import com.viaoa.cache.OAObjectCacheListener;
import com.viaoa.callback.OACallback;
import com.viaoa.filter.OAFilter;
import com.viaoa.find.OAFinder;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;

public interface OAObjectCacheOps {

	public void fireAfterLoadEvent(OAObject oaObj);
	public Class<? extends OAObject>[] getClasses();
	public <T extends OAObject> void callback(Class<T> clazz, OACallback<T> callback);
	public int getTotal(Class<? extends OAObject> clazz);
	public <T extends OAObject> void addListener(Class<T> clazz, OAObjectCacheListener<T> cachelistener);
	public <T extends OAObject> void visit(Class<T> clazz, OACallback<T> callback);
	public <T extends OAObject> void removeListener(Class<T> clazz, OAObjectCacheListener<T> cacheListener);
	public <T extends OAObject> Hub<T> getSelectAllHub(Class<T> clazz);
	public <T extends OAObject> void setSelectAllHub(Hub<T> hub);
	public <T extends OAObject> T get(Class<T> clazz, OAObjectKey objectKey);
	public <T extends OAObject> T getObject(Class<T> clazz, Object object);
	public void removeObject(OAObject oaObj);
	public void refresh(Class<? extends OAObject> clazz);
	public void removeAllObjects(Class<? extends OAObject> clazz);

	public <T extends OAObject> T find(Class<T> clazz, OAFinder<T, T> finder);
	public <T extends OAObject> T find(T fromObject, Class<T> clazz, int fetchAmount, List<T> alResults);
	public <T extends OAObject> T find(T fromObject, Class<T> clazz, OAFilter<T> filter, boolean bSkipNew, boolean bThrowException, int fetchAmount, List<T> alResults);
	
	public <T extends OAObject> T add(T oaObj, boolean bErrorIfExists, boolean bAddToSelectAll);
	public <T extends OAObject> void removeSelectAllHub(Hub<T> hub);
	public void getInfo(List<String> al);
	public OAObject getRandom(Class<? extends OAObject> clazz, int max);
}
