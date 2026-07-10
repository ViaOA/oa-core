package com.viaoa.oa.api.internal.hubs;

import com.viaoa.callback.OAObjectCallback;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

/**
 * Internal Hub membership operations for add, insert, remove, clear, move, refresh, and add/remove permission checks.
 */
public interface HubAddRemoveOps {

	/**
	 * Adds an object to a Hub.
	 *
	 * @param hub the Hub receiving the object
	 * @param obj the object to add
	 * @return {@code true} if the object was added
	 */
	public <T extends OAObject> boolean add(Hub<T> hub, T obj);
	/**
	 * Swaps two Hub positions.
	 *
	 * @param hub the Hub to update
	 * @param pos1 the first position
	 * @param pos2 the second position
	 */
	public void swap(Hub<?> hub, int pos1, int pos2);
	/**
	 * Moves an object from one Hub position to another.
	 *
	 * @param hub the Hub to update
	 * @param posFrom the original position
	 * @param posTo the destination position
	 */
	public void move(Hub<?> hub, int posFrom, int posTo);
	/**
	 * Inserts an object at a Hub position.
	 *
	 * @param hub the Hub receiving the object
	 * @param obj the object to insert
	 * @param pos the insertion position
	 * @return {@code true} if the object was inserted
	 */
	public <T extends OAObject> boolean insert(Hub<T> hub, T obj, int pos);
	/**
	 * Clears all objects from a Hub.
	 *
	 * @param hub the Hub to clear
	 */
	public void clear(Hub<?> hub);
	/**
	 * Returns whether an object can be added to a Hub.
	 *
	 * @param hub the Hub receiving the object
	 * @param object the object to test
	 * @return {@code true} if add is allowed
	 */
	public <T extends OAObject> boolean canAdd(Hub<T> hub, T object);
	/**
	 * Returns the denial message for adding an object, when add is not allowed.
	 *
	 * @param hub the Hub receiving the object
	 * @param obj the object to test
	 * @return the denial message, or {@code null}
	 */
	public <T extends OAObject> String canAddMsg(Hub<T> hub, T obj);
	/**
	 * Returns the denial message for removing all objects from a Hub.
	 *
	 * @param hub the Hub to check
	 * @param onlyCheckTypes optional rule checks to use
	 * @return the denial message, or {@code null}
	 */
	public String getCantRemoveAllMessage(Hub<?> hub, OAObjectCallback.CheckType[] onlyCheckTypes);
	/**
	 * Adds an object with internal control over duplicate membership checking.
	 *
	 * @param hub the Hub receiving the object
	 * @param obj the object to add
	 * @param bAlreadyCalledContains {@code true} if membership was already checked
	 */
	public <T extends OAObject> void add(Hub<T> hub, T obj, boolean bAlreadyCalledContains);
	/**
	 * Clears a Hub using internal active-object and event options.
	 *
	 * @param thisHub the Hub to clear
	 * @param bSetAOtoNull {@code true} to clear the active object
	 * @param bSendNewList {@code true} to send a new-list event
	 */
	public void clear(Hub<?> thisHub, boolean bSetAOtoNull, boolean bSendNewList);
	/**
	 * Removes an object from a Hub.
	 *
	 * @param hub the Hub losing the object
	 * @param obj the object to remove
	 * @return {@code true} if removed
	 */
	public <T extends OAObject> boolean remove(Hub<T> hub, T obj);
	/**
	 * Removes the object at a Hub position.
	 *
	 * @param hub the Hub losing the object
	 * @param pos the position to remove
	 * @return the removed object, or {@code null}
	 */
	public <T extends OAObject> T remove(Hub<T> hub, int pos);
	/**
	 * Removes an object or key-compatible value from a Hub.
	 *
	 * @param hub the Hub losing the object
	 * @param obj the object or key-compatible value to remove
	 * @return {@code true} if removed
	 */
	public <T extends OAObject> boolean remove(Hub<T> hub, Object obj);
	/**
	 * Removes an object using full internal remove options.
	 *
	 * @param thisHub the Hub losing the object
	 * @param obj the object to remove
	 * @param bForce {@code true} to force removal
	 * @param bSendEvent {@code true} to send Hub events
	 * @param bDeleting {@code true} when removal is part of delete processing
	 * @param bSetAO {@code true} to adjust the active object
	 * @param bSetPropToMaster {@code true} to update master-link state
	 * @param bIsRemovingAll {@code true} when part of remove-all processing
	 */
	public <T extends OAObject> void remove(Hub<T> thisHub, T obj, boolean bForce, boolean bSendEvent, boolean bDeleting, boolean bSetAO, boolean bSetPropToMaster, boolean bIsRemovingAll);
	/**
	 * Moves an object to its sorted position after sort-relevant state changes.
	 *
	 * @param hub the sorted Hub
	 * @param object the object to reposition
	 */
	public <T extends OAObject> void sortMove(Hub<T> hub, T object);
	/**
	 * Refreshes a Hub from another Hub.
	 *
	 * @param hub the Hub to refresh
	 * @param hubNew the Hub containing replacement contents
	 */
	public <T extends OAObject> void refresh(Hub<T> hub, Hub<T> hubNew);
	/**
	 * Returns whether the Hub allows add/remove operations.
	 *
	 * @param thisHub the Hub to inspect
	 * @return {@code true} if add/remove operations are allowed
	 */
	public boolean isAllowAddRemove(Hub<?> thisHub);	
	/**
	 * Returns whether the Hub allows remove operations.
	 *
	 * @param thisHub the Hub to inspect
	 * @return {@code true} if remove operations are allowed
	 */
	public boolean isAllowRemove(Hub<?> thisHub);	
	
}
