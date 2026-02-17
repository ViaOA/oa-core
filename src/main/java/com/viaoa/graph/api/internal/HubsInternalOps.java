package com.viaoa.graph.api.internal;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.Comparator;

import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubAutoSequence;
import com.viaoa.hub.HubDataMaster;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubSortListener;
import com.viaoa.object.OACascade;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.util.OAFilter;
import com.viaoa.xml.OAXMLWriter;


/**
 * 
 *  methods needed internally by OA and OA tools.  Used by OG.Hubs (HubService)
 *  
 *  
 */

public interface HubsInternalOps {

	// Hub
 	public HubAutoSequence callHubGetAutoSequence(Hub<?> hub);	
	public int callHubGetSize(Hub<?> hub);
	public int callHubGetLoadedSize(Hub<?> hub);
	public void callHubSetAutoSequence(Hub<?> hub, String property, int startNumber, boolean bKeepSeq);
	public void callHubResequence(Hub<?> hub);
	public <T> HubCurrentStateEnum callHubGetCurrentState(Hub<T> thisHub, Hub<T> hubNew, ArrayList<T> alNew);
	public void callHubSetObjectClass(Hub<?> hubDetail, Class<?> clazz);
	public void callHubSetAutoMatch(Hub<?> hub, String property, Hub<?> hubMaster, boolean bServerSideOnly);
	public void callHubSetAutoMatch(Hub<?> hub, String property, Hub<?> hubMaster, boolean bServerSideOnly, OAObject objStop, String stopProperty);
	public boolean callHubIsValid(Hub<?> hub);
	public boolean callHubGetChanged(Hub<?> thisHub, int iCascadeRule, OACascade cascade); 
	public void callHubSetProperty(Hub<?> hub, String name, Object obj);
	public Object callHubGetProperty(Hub<?> hub, String name);
	public void callHubRemoveProperty(Hub<?> hub, String name);
	public void callHubSetUniqueProperty(Hub<?> hub, String propertyName);

	/**
	 * Enumeration describing the synchronization state of a hub during updates.
	 *
	 * <ul>
	 *   <li>{@code InSync} – the hub is correctly aligned with its master or linked
	 *       state.</li>
	 *   <li>{@code DetailDisconnectedFromMaster} – the detail hub does not match its
	 *       expected master state.</li>
	 *   <li>{@code DetailHubNotSameAsMasterObject} – the detail hub contains a
	 *       different object than the master hub’s active object.</li>
	 *   <li>{@code HubMergerNotUpdated} – a hub merger is not in sync with its
	 *       source hubs.</li>
	 * </ul>
	 */
	public static enum HubCurrentStateEnum {
		InSync,
		DetailDisconnectedFromMaster,
		DetailHubNotSameAsMasterObject, // caused when object/hubs are in flux (hub event that is calling listeners and changing linkages)
		HubMergerNotUpdated
	}
	
 	// AddRemove
	public <T> boolean callHubAddRemoveAdd(Hub<T> hub, T obj);
	public void callHubAddRemoveSwap(Hub<?> hub, int pos1, int pos2);
	public void callHubAddRemoveMove(Hub<?> hub, int posFrom, int posTo);
	public <T> boolean callHubAddRemoveInsert(Hub<T> hub, T obj, int pos);
	public void callHubAddRemoveClear(Hub<?> hub);
	public <T> boolean callHubAddRemoveCanAdd(Hub<T> hub, T object);
	public <T> String callHubAddRemoveCanAddMsg(Hub<T> hub, T obj);
	public String callHubAddRemoveGetCantRemoveAllMessage(Hub<?> hub, int checkType);
	public <T> void callHubAddRemoveAdd(Hub<T> hub, T obj, boolean bAlreadyCalledContains);
	public void callHubAddRemoveClear(Hub<?> thisHub, boolean bSetAOtoNull, boolean bSendNewList);
	
	public boolean callHubAddRemoveRemove(Hub<?> hub, Object obj);
	public <T> T callHubAddRemoveRemove(Hub<T> hub, int pos);
	public void callHubAddRemoveRemove(Hub<?> thisHub, Object obj, boolean bForce, boolean bSendEvent, boolean bDeleting, boolean bSetAO, boolean bSetPropToMaster, boolean bIsRemovingAll);
	
	public <T> void callHubAddRemoveSortMove(Hub<T> hub, T object);
	public <T> void callHubAddRemoveRefresh(Hub<T> hub, Hub<T> hubNew);

 	
	// AO
	public <T> T callHubAOSetActiveObject(Hub<T> hub, int pos);
	public void callHubAOSetActiveObject(Hub<?> hub, Object obj);
	public void callHubAOSetActiveObjectForce(Hub<?> hub, Object obj);

	// CS
	public void callHubCSSendRefresh(Hub<?> hub);
	public boolean callHubCSIsServer(Hub<?> hub);
	
	
	// Data
	public void callHubDataEnsureCapacity(Hub<?> hub, int size);
	public void callHubDataResizeToFit(Hub<?> hub);
	public void callHubDataSetChanged(Hub<?> hub, boolean bIsChanged);
	public <T> void callHubDataCopyInto(Hub<T> hub, T[] anArray);
	public <T> T[] callHubDataToArray(Hub<T> hub);
	public int callHubDataGetCurrentSize(Hub<?> hub);
	public void callHubDataClone(Hub<?> hub, Hub<?> hubNew);
	public <T> T callHubDataGetObject(Hub<T> hub, Object key);
	public <T> T callHubDataGetObjectAt(Hub<T> hub, int pos);
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
	
	
	public Hub<? extends OAObject> callHubDetailGetDetailHub(Hub<?> hub, String path);
	public Hub<? extends OAObject> callHubDetailGetDetailHub(Hub<?> hub, String path, boolean bShareActive, String selectOrder);
	public Hub<? extends OAObject> callHubDetailGetDetailHub(Hub<?> hub, String path, boolean bShareActive);
	public Hub<? extends OAObject> callHubDetailGetDetailHub(Hub<?> hub, String path, String selectOrder);
	public <T extends OAObject> Hub<T> callHubDetailGetDetailHub(Hub<?> hub, String path, Class<T> objectClass, boolean bShareActive);
	public <T extends OAObject> Hub<T> callHubDetailGetDetailHub(Hub<?> hub, Class<T> clazz, boolean bShareActive, String selectOrder);
	public Hub<? extends OAObject> callHubDetailGetDetailHub(Hub<?> hub, Class<? extends OAObject>[] classes);
	
	
	public void callHubDetailSetMasterHub(Hub<?> thisHub, Hub<? extends OAObject> masterHub, String path, boolean bShared, String selectOrder);
	public Hub<? extends OAObject> callHubDetailGetMasterHub(Hub<?> hub);
	public OAObject callHubDetailGetMasterObject(Hub<?> hub);
	public Class<? extends OAObject> callHubDetailGetMasterClass(Hub<?> hub);
	public boolean callHubDetailRemoveDetailHub(Hub<?> hub, Hub<? extends OAObject> hubDetail);
	public OALinkInfo callHubDetailGetLinkInfoFromDetailToMaster(Hub<?> hub);
	public Hub<?> callHubDetailGetRealHub(Hub<?> hub);
	public String callHubDetailGetPropertyFromMasterToDetail(Hub<?> hub);
	public String callHubDetailGetPropertyFromDetailToMaster(Hub<?> hub);
	public OALinkInfo callHubDetailGetLinkInfoFromMasterToDetail(Hub<?> hub);
	

 	// Event
	public void callHubEventFireOnNewListEvent(Hub<?> hub, boolean bAll);
	public void callHubEventAddHubListener(Hub<?> hub, HubListener<?> hl, String property);
	public void callHubEventAddHubListener(Hub<?> hub, HubListener<?> hl, String property, boolean bActiveObjectOnly);
	public void callHubEventAddHubListener(Hub<?> hub, HubListener<?> hl, boolean bActiveObjectOnly);
	public void callHubEventAddHubListener(Hub<?> hub, HubListener<?> hl, String property, String[] dependentPropertyPaths);
	public void callHubEventAddHubListener(Hub<?> hub, HubListener<?> hl, String property, String[] dependentPropertyPaths, boolean bActiveObjectOnly);
	public void callHubEventAddHubListener(Hub<?> hub, HubListener<?> hl, String property, String[] dependentPropertyPaths, boolean bActiveObjectOnly, boolean bUseBackgroundThread);
	public void callHubEventFireCalcPropertyChange(Hub<?> hub, OAObject obj, String property);
	public void callHubEventAddHubListener(Hub<?> hub, HubListener<?> hl);
	public void callHubEventRemoveHubListener(Hub<?> hub, HubListener<?> hl);
	public void callHubEventFireCalcPropertyChange(Hub<?> hub, Object obj, String propertyName);

	
	// Find
	public <T> T callHubFindFindFirst(Hub<T> hub, String propertyPath, Object findValue, boolean bSetAO, T lastFoundObject);
	
	// Link
	public Hub<?> callHubLinkGetHubWithLink(Hub<?> hub, boolean bIncludeCopiedHubs);
	public void callHubLinkSetLinkHub(Hub<?> thisHub, String propertyFrom, Hub<?> linkToHub, String propertyTo, boolean linkPosFlag, boolean bAutoCreate, boolean bAutoCreateAllowDups);
	public String callHubLinkGetLinkHubPath(Hub<?> hub, boolean bIncludeCopiedHubs);
	public void callHubLinkUpdateLinkedToHub(Hub<?> hub, Hub<?> linkToHub, Object obj);
	public void callHubLinkUpdateLinkedToHub(Hub<?> hub, Hub<?> linkToHub, Object obj, String changedPropName);
	public Object callHubLinkGetPropertyValueInLinkedToHub(Hub<?> hub, Object linkObject);
	public boolean callHubLinkGetLinkedOnPos(Hub<?> hub);
	public String callHubLinkGetLinkToProperty(Hub<?> hub);

	// Root
	public Hub<?> callHubRootGetRootHub(Hub<?> hub);
	public void callHubRootSetRootHub(Hub<?> hub, boolean bIsRoot);
	
	// Save
	public void callHubSaveSaveAll(Hub<?> hub, int cascadeRule);
	
	// Select
	public OASelect<? extends OAObject> callHubSelectGetSelect(Hub<?> hub, boolean bCreateIfNull);
	public void callHubSelectLoadAllData(Hub<?> hub);
	public void callHubSelectCancelSelect(Hub<?> hub, boolean bRemoveSelect);
	public boolean callHubSelectIsMoreData(Hub<?> hub);
	public void callHubSelectSetSelectWhere(Hub<?> hub, String whereClause);
	public String callHubSelectGetSelectWhere(Hub<?> hub);
	public void callHubSelectSetSelectOrder(Hub<?> hub, String orderClause);
	public void callHubSelectSetSelectWhereHub(Hub<?> hub, Hub<?> hubSelect);
	public void callHubSelectSetSelectWhereHubPropertyPath(Hub<?> hub, String ppFromHub);
	public String callHubSelectGetSelectOrder(Hub<?> hub);
	public void callHubSelectSelect(Hub<?> hub, OAObject whereObject, String whereClause, Object[] whereParams, String orderByClause, boolean bAppendFlag);
	public void callHubSelectSelect(Hub<?> hub, boolean bAppendFlag);
	public void callHubSelectSelect(Hub<?> hub, OAObject whereObject, String whereClause, Object[] whereParams, String orderBy, boolean bAppendFlag, OAFilter filter);
	public void callHubSelectSelect(Hub<?> hub, OASelect<? extends OAObject> select);
	public void callHubSelectSelectPassthru(Hub<?> hub, String whereClause, String orderClause);
	public OASelect<? extends OAObject> callHubSelectGetSelect(Hub<?> hub);
	public void callHubSelectRefresh(Hub<?> hub);
	public Hub<?> callHubSelectGetSelectWhereHub(Hub<?> hub);
	public String callHubSelectGetSelectWhereHubPropertyPath(Hub<?> hub);
	
	
	
	// Serialize
	public void callHubSerializeWriteObject(Hub<?> hub, ObjectOutputStream stream) throws IOException;
	public Object callHubSerializeReadResolve(Hub<?> hub) throws ObjectStreamException;
	
	// Share
	public void callHubShareSetSharedHub(Hub<?> hub, Hub<?> sharedMasterHub, boolean shareActiveObject);
	public void callHubShareRemoveSharedHub(Hub<?> hub, Hub<?> hubToRemove);
	public <T> Hub<T> callHubShareCreateSharedHub(Hub<T> hub, boolean shareActiveObject);
	public boolean callHubShareIsUsingSameSharedHub(Hub<?> hub, Hub<?> hub2);
	public boolean callHubShareIsUsingSameSharedAO(Hub<?> hub, Hub<?> hub2);
	public Hub<?> callHubShareGetMainSharedHub(Hub<?> hub);
	
	// Sort
 	public HubSortListener callHubSortGetSortListener(Hub<?> hub);
	public void callHubSortSort(Hub<?> hub, String propertyPaths, boolean bAscending, Comparator<?> comp);
	public boolean callHubSortIsSorted(Hub<?> hub);
	public void callHubSortCancelSort(Hub<?> hub);
	public void callHubSortSort(Hub<?> hub);
	public void callHubSortResort(Hub<?> hub);
 	
	// XML
	public void callHubXMLWrite(Hub<?> hub, OAXMLWriter ow, final String tagName, boolean bKeyOnly, OACascade cascade);
}
