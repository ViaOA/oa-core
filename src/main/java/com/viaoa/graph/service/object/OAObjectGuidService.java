package com.viaoa.graph.service.object;



import java.util.UUID;
import java.util.logging.Logger;

import com.viaoa.object.OAObject;

public abstract class OAObjectGuidService {
	private static final Logger LOG = Logger.getLogger(OAObjectGuidService.class.getName());

	private final OAObject.FriendAccess faObject;
	
	public OAObjectGuidService(OAObject.FriendAccess faObject) {
    	if (faObject == null) throw new IllegalArgumentException("OAObject.FriendAccess can not be null");
    	this.faObject = faObject;
	}

	/**
	 * Ensures that the specified {@link OAObject} has a valid globally unique
	 * identifier (GUID). If the object already has a non-zero GUID, the method
	 * returns immediately without modification.
	 *
	 * <p>The GUID assignment strategy depends on the object's metadata and the
	 * current client/server execution context:</p>
	 *
	 * <h3>Assignment Rules</h3>
	 * <ul>
	 *   <li><b>Local-only classes</b>  
	 *       Use a negative, decrementing counter to ensure the GUID does not
	 *       overlap with server-issued identifiers.</li>
	 *
	 *   <li><b>Client-side execution</b>  
	 *       Attempt to obtain a server-issued GUID via
	 *       {@code OAObjectCSDelegate.getGuidFromServer(obj)}.  
	 *       If the server does not provide one (returns {@code 0}), a new GUID is
	 *       generated locally using {@link #getNextGuid()}.</li>
	 *
	 *   <li><b>Server-side execution</b>  
	 *       Always generates a new positive GUID using {@link #getNextGuid()}.</li>
	 * </ul>
	 *
	 * <p>This method must be invoked before any operations that rely on the object's
	 * identity, including hashing, cache insertion, linking, or client/server sync.</p>
	 *
	 * @param obj the object requiring GUID assignment; may be {@code null}.
	 * @throws Exception 
	 */
	public synchronized void assignGuid(OAObject obj) {
		if (obj == null) return;
		
		UUID guid = faObject.getGuid(obj);
		if (guid != null) return;
		
		guid = UUID.randomUUID();
		faObject.setGuid(obj, guid);
	}

	public synchronized void assignNewGuid(OAObject obj) {
		if (obj == null) return;
		faObject.setGuid(obj, null);
		assignGuid(obj);
	}

	
	/**
	 * Returns the next available positive GUID value and increments the internal
	 * GUID counter. This method is used for generating new unique identifiers for
	 * {@link OAObject} instances created on the local JVM.
	 *
	 * <p>The method delegates to {@link OAGuid#getNextGuid()} to retrieve and
	 * increment the global counter. The value returned is always positive and
	 * monotonically increasing, ensuring uniqueness across all locally created
	 * objects and preventing collisions with previously assigned GUIDs.</p>
	 *
	 * @return the next positive GUID value.
	 */
	public long getNextGuid_NOTUSED() {
		//return guidCounter.incrementAndGet(); // cant be 0
		return -1;
	}

	
	/**
	 * Reserves the next fifty GUID values in the global GUID counter and returns the
	 * first GUID in that reserved block.
	 *
	 * <p>The method atomically adds {@code 50} to the internal counter and returns
	 * the first value in the allocated range. This is useful when a caller needs to
	 * preallocate a contiguous block of GUIDs, such as for batching or distributed
	 * assignment scenarios.</p>
	 *
	 * <p>Only the first GUID is returned. The remaining forty-nine GUIDs are
	 * implicitly reserved and may be obtained by incrementing sequentially from the
	 * returned value.</p>
	 *
	 * @return the first GUID in the next reserved block of fifty GUIDs.
	 */
	public long getNextFiftyGuids_NOTUSED() {
		//return guidCounter.getAndAdd(50) + 1;
		return -1;
	}

	/**
	 * Returns the GUID assigned to the specified {@link OAObject}. If the object is
	 * {@code null}, the method returns {@code 0}.
	 *
	 * <p>This method does not generate or assign a GUID; it only returns the
	 * object's current GUID value. GUID assignment occurs during initialization or
	 * through explicit calls to methods such as {@link #assignGuid(OAObject)} or
	 * {@link #setObjectGuid(OAObject, long)}.</p>
	 *
	 * @param obj the object whose GUID is requested; may be {@code null}.
	 * @return the object's GUID, or {@code 0} if the object is {@code null}.
	 */
	public UUID getGuid(OAObject oaObj) {
		UUID guid = faObject.getGuid(oaObj);
		return guid;
	}

	public void setGuid(OAObject oaObj, UUID guid) {
		faObject.setGuid(oaObj, guid);
	}
	
	public void setNextGuid_NOTUSEd(long x) {
		// guidCounter.set(x);
	}

	public void updateGuid_NOTUSED(long guid) {
		/*
		for (;;) {
			long g = guidCounter.get();
			if (g >= guid) {
				break;
			}
			if (guidCounter.compareAndSet(g, guid)) {
				break;
			}
		}
		*/
	}

	
	
}
