package com.viaoa.oa.api.internal.hubs;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

/**
 * Internal Hub data-array and membership access operations.
 */
public interface HubDataOps {
	
	/**
	 * Sets the object class for a Hub.
	 *
	 * @param hubDetail the Hub to update
	 * @param clazz the object class
	 */
	public <T extends OAObject> void setObjectClass(Hub<T> hubDetail, Class<T> clazz);
	/**
	 * Ensures that a Hub can hold at least the supplied number of objects.
	 *
	 * @param hub the Hub to resize
	 * @param size the required capacity
	 */
	public void ensureCapacity(Hub<?> hub, int size);
	/**
	 * Resizes Hub storage to fit the current contents.
	 *
	 * @param hub the Hub to resize
	 */
	public void resizeToFit(Hub<?> hub);
	/**
	 * Copies Hub contents into an array.
	 *
	 * @param hub the source Hub
	 * @param anArray the destination array
	 */
	public <T extends OAObject> void copyInto(Hub<T> hub, T[] anArray);
	/**
	 * Returns Hub contents as an array.
	 *
	 * @param hub the source Hub
	 * @return the Hub contents
	 */
	public <T extends OAObject> T[] toArray(Hub<T> hub);
	/**
	 * Returns the current internal storage size for a Hub.
	 *
	 * @param hub the Hub to inspect
	 * @return the current size
	 */
	public int getCurrentSize(Hub<?> hub);
	/**
	 * Returns an object from a Hub by object or key-compatible value.
	 *
	 * @param hub the Hub to search
	 * @param key the object or key-compatible value
	 * @return the matching object, or {@code null}
	 */
	public <T extends OAObject> T getObject(Hub<T> hub, Object key);
	/**
	 * Returns the object at a Hub position.
	 *
	 * @param hub the Hub to inspect
	 * @param pos the object position
	 * @return the object, or {@code null}
	 */
	public <T extends OAObject> T getObjectAt(Hub<T> hub, int pos);
	/**
	 * Returns whether a Hub contains an object or key-compatible value.
	 *
	 * @param hub the Hub to inspect
	 * @param obj the object or value to test
	 * @return {@code true} if contained
	 */
	public boolean contains(Hub<?> hub, Object obj);
	/**
	 * Returns the position of an object with internal master/link update options.
	 *
	 * @param hub the Hub to search
	 * @param object the object or key-compatible value
	 * @param adjustMaster {@code true} to adjust master/detail state
	 * @param bUpdateLink {@code true} to update linked Hub state
	 * @return the object position, or {@code -1}
	 */
	public int getPos(final Hub<?> hub, Object object, final boolean adjustMaster, final boolean bUpdateLink);
	/**
	 * Sets whether a Hub is loading all data.
	 *
	 * @param hub the Hub to update
	 * @param bIsLoading {@code true} while loading all data
	 * @return the previous loading state
	 */
	public boolean setLoadingAllData(Hub<?> hub, boolean bIsLoading);
	/**
	 * Sets all-data loading state and owning thread for a Hub.
	 *
	 * @param hub the Hub to update
	 * @param bIsLoadingAllData {@code true} while loading all data
	 * @param thread the loading thread
	 */
	public void setLoadingAllData(Hub<?> hub, boolean bIsLoadingAllData, Thread thread);
	/**
	 * Clears Hub change tracking.
	 *
	 * @param hub the Hub to update
	 */
	public void clearHubChanges(Hub<?> hub);
	/**
	 * Copies internal Hub state during clone processing.
	 *
	 * @param thisHub the source Hub
	 * @param newHub the target Hub
	 */
	public <T extends OAObject> void _clone(Hub<T> thisHub, Hub<T> newHub);

}
