package com.viaoa.graph.api.internal.objects;

import com.viaoa.callback.OACopyCallback;
import com.viaoa.cascade.OACascade;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;

public interface OAObjectReflectOps {

	public void setProperty(OAObject oaObj, String propName, Object value, String fmt);
	public Object getProperty(OAObject oaObj, String propName);
	public OAObject createCopy(OAObject oaObj, String[] excludeProperties);
	public void copyInto(OAObject oaObj, OAObject newObject, String[] excludeProperties, OACopyCallback copyCallback);
	public <T extends OAObject> Hub<T> getReferenceHub(final OAObject oaObj, final String linkPropertyName, String sortOrder, boolean bSequence, Hub<T> hubMatch);
	public Object getReferenceObject(OAObject oaObj, String linkPropertyName);
	public boolean isReferenceObjectNullOrEmpty(OAObject oaObj, String name);
	public byte[] getReferenceBlob(OAObject oaObj, String linkPropertyName);
	public boolean getPrimitiveNull(OAObject oaObj, String prop);
	public void setPrimitiveNull(OAObject oaObj, String prop, boolean b);
	
	public int loadAllReferences(OAObject oaObj, boolean bIncludeCalc);
	public int loadAllReferences(OAObject oaObj, boolean bOne, boolean bMany, boolean bIncludeCalc);
	public int loadAllReferences(OAObject oaObj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc);
	public int loadAllReferences(OAObject oaObj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc, int maxRefsToLoad);
	public int loadAllReferences(OAObject oaObj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc, int maxRefsToLoad, long maxEndTime);
	public int loadAllReferences(OAObject obj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc, OACascade cascade, int maxRefsToLoad);
	
	public <T extends OAObject> T getObject(Class<T> clazz, Object keyValue); //qqqqqqqqq add this to graph.objects().getObject(c, k)
	public <T extends OAObject> T createNewObject(Class<T> clazz);
	public boolean areAllReferencesLoaded(OAObject oaObj, boolean bIncludeCalc);
	public boolean isReferenceHubLoaded(OAObject oaObj, String hubPropertyName);
	public String[] getUnloadedReferences(OAObject obj, boolean bIncludeCalc, String exceptPropertyName, boolean bIncludeLarge);
	public String getPropertyPathFromMaster(OAObject oaObjParent, Hub<?> hubChild);
	public Object getProperty(Hub<?> hub, String propertyPath);
	public OAObjectKey getPropertyObjectKey(OAObject oaObj, String propertyName);
	public Object getRawReference(OAObject oaObj, String name);
	public String getPropertyPathBetweenHubs(final Hub<?> hubParent, final Hub<?> hubChild);
}
