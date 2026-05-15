package com.viaoa.graph.api.internal;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.Comparator;

import com.viaoa.cascade.OACascade;
import com.viaoa.filter.OAFilter;
import com.viaoa.graph.service.hub.HubStatusService.HubCurrentStateEnum;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubDataMaster;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.auto.HubAutoSequence;
import com.viaoa.hub.sort.HubSortListener;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.select.OASelect;
import com.viaoa.serialize.OASerializeWriter;


/*qqqqqqq
CODEX

 #5 — boundary risk
  File/class/method: src/main/java/com/viaoa/graph/api/internal/HubsInternalOps.java:11, src/main/java/com/viaoa/
  graph/api/internal/HubsInternalOps.java:205
  Exact concern: internal API depends on HubStatusService.HubCurrentStateEnum, a nested type from a concrete service
  implementation package.
  Why it matters: API/internal should define the internal contract; it should not depend upward on a service
  implementation detail.
  Minimal fix: move the enum to internal API or a neutral graph/hub contract type.
  Suggested invariant: GRAPH_INTERNAL_API_DOES_NOT_IMPORT_SERVICE_IMPLEMENTATION_TYPES
  Suggested test coverage: import-boundary test for graph.api.internal against graph.service.*.



*/

/**
 * 
 *  methods needed internally by OA and OA tools.  Used by OG.Hubs (HubService)
 *  
 *  
 */

public interface HubsInternalOps {
	
 	// AddRemove
	public <T extends OAObject> boolean callHubAddRemoveAdd(Hub<T> hub, T obj);
	public void callHubAddRemoveSwap(Hub<?> hub, int pos1, int pos2);
	public void callHubAddRemoveMove(Hub<?> hub, int posFrom, int posTo);
	public <T extends OAObject> boolean callHubAddRemoveInsert(Hub<T> hub, T obj, int pos);
	public void callHubAddRemoveClear(Hub<?> hub);
	public <T extends OAObject> boolean callHubAddRemoveCanAdd(Hub<T> hub, T object);
	public <T extends OAObject> String callHubAddRemoveCanAddMsg(Hub<T> hub, T obj);
	public String callHubAddRemoveGetCantRemoveAllMessage(Hub<?> hub, int checkType);
	public <T extends OAObject> void callHubAddRemoveAdd(Hub<T> hub, T obj, boolean bAlreadyCalledContains);
	public void callHubAddRemoveClear(Hub<?> thisHub, boolean bSetAOtoNull, boolean bSendNewList);
	public <T extends OAObject> boolean callHubAddRemoveRemove(Hub<T> hub, T obj);
	public <T extends OAObject> T callHubAddRemoveRemove(Hub<T> hub, int pos);
	public <T extends OAObject> boolean callHubAddRemoveRemove(Hub<T> hub, Object obj);
	public <T extends OAObject> void callHubAddRemoveRemove(Hub<T> thisHub, T obj, boolean bForce, boolean bSendEvent, boolean bDeleting, boolean bSetAO, boolean bSetPropToMaster, boolean bIsRemovingAll);
	public <T extends OAObject> void callHubAddRemoveSortMove(Hub<T> hub, T object);
	public <T extends OAObject> void callHubAddRemoveRefresh(Hub<T> hub, Hub<T> hubNew);
 	
	// AO
	public <T extends OAObject> T callHubAOSetActiveObject(Hub<T> hub, int pos);
	public <T extends OAObject> void callHubAOSetActiveObject(Hub<T> hub, T obj);
	public <T extends OAObject> void callHubAOSetActiveObjectForce(Hub<T> hub, T obj);
	public <T extends OAObject> T callHubAOSetActiveObject(Hub<T> hub, Object obj);

	// AutoMatch	
	public void callHubAutoMatchSetAutoMatch(Hub<?> hub, String property, Hub<?> hubMaster, boolean bServerSideOnly);
	public void callHubAutoMatchSetAutoMatch(Hub<?> hub, String property, Hub<?> hubMaster, boolean bServerSideOnly, OAObject objStop, String stopProperty);

	// CS
	public void callHubCSSendRefresh(Hub<?> hub);
	public boolean callHubCSIsServer(Hub<?> hub);
	
	// Data
	public <T extends OAObject> void callHubDataSetObjectClass(Hub<T> hubDetail, Class<T> clazz);
	public void callHubDataEnsureCapacity(Hub<?> hub, int size);
	public void callHubDataResizeToFit(Hub<?> hub);
	public void callHubStatusSetChanged(Hub<?> hub, boolean bIsChanged);
	public <T extends OAObject> void callHubDataCopyInto(Hub<T> hub, T[] anArray);
	public <T extends OAObject> T[] callHubDataToArray(Hub<T> hub);
	public int callHubDataGetCurrentSize(Hub<?> hub);
	public <T extends OAObject> void callHubDataClone(Hub<T> hub, Hub<T> hubNew);
	public <T extends OAObject> T callHubDataGetObject(Hub<T> hub, Object key);
	public <T extends OAObject> T callHubDataGetObjectAt(Hub<T> hub, int pos);
	public boolean callHubDataContains(Hub<?> hub, Object obj);
	public int callHubDataGetPos(final Hub<?> hub, Object object, final boolean adjustMaster, final boolean bUpdateLink);
	public boolean callHubDataSetLoadingAllData(Hub<?> hub, boolean bIsLoading);
	public void callHubDataSetLoadingAllData(Hub<?> hub, boolean bIsLoadingAllData, Thread thread);
	public void callHubDataClearHubChanges(Hub<?> hub);

	// Delete
	public void callHubDeleteDeleteAll(Hub<?> hub);
	public boolean callHubDeleteIsDeletingAll(Hub<?> hub);
	
 	// Detail
	public OALinkInfo callHubDetailGetLinkInfoFromMasterObjectToDetail(Hub<?> hub);	
	public OALinkInfo callHubDetailGetLinkInfoFromMasterHubToDetail(Hub<?> hub);
	public void callHubDetailSetMasterObject(Hub<?> hub, OAObject masterObject);
	public void callHubDetailSetMasterObject(Hub<?> hub, OAObject masterObject, OALinkInfo liDetailToMaster);
	public HubDataMaster callHubDetailGetDataMaster(Hub<?> hub);
	public boolean callHubDetailIsOwned(Hub<?> hub);
	public Hub<?> callHubDetailGetDetailHub(Hub<?> hub, String path);
	public Hub<?> callHubDetailGetDetailHub(Hub<?> hub, String path, boolean bShareActive, String selectOrder);
	public Hub<?> callHubDetailGetDetailHub(Hub<?> hub, String path, boolean bShareActive);
	public Hub<?> callHubDetailGetDetailHub(Hub<?> hub, String path, String selectOrder);
	public <T extends OAObject> Hub<T> callHubDetailGetDetailHub(Hub<?> hub, String path, Class<T> objectClass, boolean bShareActive);
	public <T extends OAObject> Hub<T> callHubDetailGetDetailHub(Hub<?> hub, Class<T> clazz, boolean bShareActive, String selectOrder);
	public Hub<?> callHubDetailGetDetailHub(Hub<?> hub, Class<? extends OAObject>[] classes);
	
	public void callHubDetailSetMasterHub(Hub<?> thisHub, Hub<?> masterHub, String path, boolean bShared, String selectOrder);
	public Hub<? extends OAObject> callHubDetailGetMasterHub(Hub<?> hub);
	public OAObject callHubDetailGetMasterObject(Hub<?> hub);
	public Class<? extends OAObject> callHubDetailGetMasterClass(Hub<?> hub);
	public boolean callHubDetailRemoveDetailHub(Hub<?> hub, Hub<?> hubDetail);
	public OALinkInfo callHubDetailGetLinkInfoFromDetailToMaster(Hub<?> hub);
	public <T extends OAObject> Hub<T> callHubDetailGetRealHub(Hub<T> hub);
	public String callHubDetailGetPropertyFromMasterToDetail(Hub<?> hub);
	public String callHubDetailGetPropertyFromDetailToMaster(Hub<?> hub);
	public OALinkInfo callHubDetailGetLinkInfoFromMasterToDetail(Hub<?> hub);
	
 	// Event
	public void callHubEventFireOnNewListEvent(Hub<?> hub, boolean bAll);
	public <T extends OAObject> void callHubEventAddHubListener(Hub<T> hub, HubListener<T> hl, String property);
	public <T extends OAObject> void callHubEventAddHubListener(Hub<T> hub, HubListener<T> hl, String property, boolean bActiveObjectOnly);
	public <T extends OAObject> void callHubEventAddHubListener(Hub<T> hub, HubListener<T> hl, boolean bActiveObjectOnly);
	public <T extends OAObject> void callHubEventAddHubListener(Hub<T> hub, HubListener<T> hl, String property, String[] dependentPropertyPaths);
	public <T extends OAObject> void callHubEventAddHubListener(Hub<T> hub, HubListener<T> hl, String property, String[] dependentPropertyPaths, boolean bActiveObjectOnly);
	public <T extends OAObject> void callHubEventAddHubListener(Hub<T> hub, HubListener<T> hl, String property, String[] dependentPropertyPaths, boolean bActiveObjectOnly, boolean bUseBackgroundThread);
	public <T extends OAObject> void callHubEventAddHubListener(Hub<T> hub, HubListener<T> hl);
	public <T extends OAObject> void callHubEventRemoveHubListener(Hub<T> hub, HubListener<T> hl);
	public <T extends OAObject> void callHubEventFireCalcPropertyChange(Hub<T> hub, T obj, String propertyName);

	// Find
	public <T extends OAObject> T callHubFindFindFirst(Hub<T> hub, String propertyPath, Object findValue, boolean bSetAO, T lastFoundObject);

	// Link
	public <T extends OAObject> Hub<T> callHubLinkGetHubWithLink(Hub<T> hub, boolean bIncludeCopiedHubs);
	public void callHubLinkSetLinkHub(Hub<?> thisHub, String propertyFrom, Hub<?> linkToHub, String propertyTo, boolean linkPosFlag, boolean bAutoCreate, boolean bAutoCreateAllowDups);
	public String callHubLinkGetLinkHubPath(Hub<?> hub, boolean bIncludeCopiedHubs);
	public <T extends OAObject> void callHubLinkUpdateLinkedToHub(Hub<T> hub, Hub<?> linkToHub, T obj);
	public <T extends OAObject> void callHubLinkUpdateLinkedToHub(Hub<T> hub, Hub<?> linkToHub, T obj, String changedPropName);
	public <T extends OAObject, U extends OAObject> Object callHubLinkGetPropertyValueInLinkedToHub(Hub<T> hub, U linkObject); // returns OAOject, null, or int (position)
	public boolean callHubLinkGetLinkedOnPos(Hub<?> hub);
	public String callHubLinkGetLinkToProperty(Hub<?> hub);

	// Property
	public void callHubPropertySetProperty(Hub<?> hub, String name, Object obj);
	public Object callHubPropertyGetProperty(Hub<?> hub, String name);
	public void callHubPropertyRemoveProperty(Hub<?> hub, String name);
	public void callHubPropertySetUniqueProperty(Hub<?> hub, String propertyName);
	
	// Root
	public <T extends OAObject> Hub<T> callHubRootGetRootHub(Hub<T> hub);
	public void callHubRootSetRootHub(Hub<?> hub, boolean bIsRoot);
	
	// Save
	public void callHubSaveSaveAll(Hub<?> hub, int cascadeRule);
	
	// Select
	public <T extends OAObject> OASelect<T> callHubSelectGetSelect(Hub<T> hub, boolean bCreateIfNull);
	public void callHubSelectLoadAllData(Hub<?> hub);
	public void callHubSelectCancelSelect(Hub<?> hub, boolean bRemoveSelect);
	public boolean callHubSelectIsMoreData(Hub<?> hub);
	public void callHubSelectSetSelectWhere(Hub<?> hub, String whereClause);
	public String callHubSelectGetSelectWhere(Hub<?> hub);
	public void callHubSelectSetSelectOrder(Hub<?> hub, String orderClause);
	public <T extends OAObject> void callHubSelectSetSelectWhereHub(Hub<T> hub, Hub<T> hubSelect);
	public void callHubSelectSetSelectWhereHubPropertyPath(Hub<?> hub, String ppFromHub);
	public String callHubSelectGetSelectOrder(Hub<?> hub);
	public void callHubSelectSelect(Hub<?> hub, OAObject whereObject, String whereClause, Object[] whereParams, String orderByClause, boolean bAppendFlag);
	public void callHubSelectSelect(Hub<?> hub, boolean bAppendFlag);
	public <T extends OAObject>  void callHubSelectSelect(Hub<T> hub, OAObject whereObject, String whereClause, Object[] whereParams, String orderBy, boolean bAppendFlag, OAFilter<T> filter);
	public <T extends OAObject> void callHubSelectSelect(Hub<T> hub, OASelect<T> select);
	public void callHubSelectSelectPassthru(Hub<?> hub, String whereClause, String orderClause);
	public <T extends OAObject> OASelect<T> callHubSelectGetSelect(Hub<T> hub);
	public void callHubSelectRefresh(Hub<?> hub);
	public <T extends OAObject> Hub<T> callHubSelectGetSelectWhereHub(Hub<T> hub);
	public String callHubSelectGetSelectWhereHubPropertyPath(Hub<?> hub);
	
	// Sequence	
 	public HubAutoSequence callHubSequenceGetAutoSequence(Hub<?> hub);	
	public void callHubSequenceSetAutoSequence(Hub<?> hub, String property, int startNumber, boolean bKeepSeq);
	public void callHubSequenceResequence(Hub<?> hub);

	// Java Serialize
	public void callHubSerializeWriteObject(Hub<?> hub, ObjectOutputStream stream) throws IOException;
	public Object callHubSerializeReadResolve(Hub<?> hub) throws ObjectStreamException;

	// Other Serialize
//	public void callHubSerializeWrite(Hub<?> hub, OASerializeWriter ow, final String tagName, boolean bKeyOnly, OACascade cascade);
	
	// Share
	public <T extends OAObject> void callHubShareSetSharedHub(Hub<T> hub, Hub<T> sharedMasterHub, boolean shareActiveObject);
	public <T extends OAObject> void callHubShareRemoveSharedHub(Hub<T> hub, Hub<T> hubToRemove);
	public <T extends OAObject> Hub<T> callHubShareCreateSharedHub(Hub<T> hub, boolean shareActiveObject);
	public boolean callHubShareIsUsingSameSharedHub(Hub<?> hub, Hub<?> hub2);
	public boolean callHubShareIsUsingSameSharedAO(Hub<?> hub, Hub<?> hub2);
	public <T extends OAObject> Hub<T> callHubShareGetMainSharedHub(Hub<T> hub);
	
	// Size
	public int callHubSizeGetSize(Hub<?> hub);
	public int callHubSizeGetLoadedSize(Hub<?> hub);
	
	
	// Sort
 	public HubSortListener callHubSortGetSortListener(Hub<?> hub);
	public void callHubSortSort(Hub<?> hub, String propertyPaths, boolean bAscending, Comparator<?> comp);
	public boolean callHubSortIsSorted(Hub<?> hub);
	public void callHubSortCancelSort(Hub<?> hub);
	public void callHubSortSort(Hub<?> hub);
	public void callHubSortResort(Hub<?> hub);

	// Status
	public boolean callHubStatusIsValid(Hub<?> hub);
	public boolean callHubStatusGetChanged(Hub<?> thisHub, int iCascadeRule, OACascade cascade); 
	public <T extends OAObject> HubCurrentStateEnum callHubStatusGetCurrentState(Hub<T> thisHub, Hub<T> hubNew, ArrayList<T> alNew);
}
