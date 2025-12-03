/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viaoa.hub;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.util.OALogger;

/**
 * Used for recursive Hubs to ensure that a Hub always remains at the root of a recursive hierarchy.
 *
 * <p>Some OAObjects (model objects) are recursive, meaning they contain a one-to-many relationship
 * to themselves. For example, a {@code Category} object may have a Hub of child {@code Category}
 * objects, which can in turn have their own children. In these cases, a Hub could otherwise become
 * shared with one of its child Hubs when navigating the recursive link.
 *
 * <p><b>HubRoot</b> prevents this from happening by keeping the Hub anchored at the top level
 * (the root) of the recursive structure. It ensures that a recursive Hub never changes its shared
 * reference to a child Hub.
 *
 * <p><b>Main responsibilities:</b>
 * <ul>
 *   <li>Identify the root Hub in recursive one-to-many relationships.</li>
 *   <li>Prevent a shared Hub from being reassigned to a child Hub.</li>
 *   <li>Maintain consistent event propagation and data references across recursive structures.</li>
 * </ul>
 *
 * <p>HubRoot objects are created automatically by OA when recursive Hubs are initialized.
 * Applications typically do not create or modify them directly.
 */
public class HubRoot {
	private static final Logger LOG = OALogger.getLogger(HubRoot.class);

	/**
	 * The root Hub that must remain anchored at the top of a recursive hierarchy.
	 * All shared or copied Hubs created by this class feed into this root.
	 */
	private final Hub hubRoot;

	/**
	 * The master Hub associated with the root Hub. Used as the source whose
	 * Active Object changes determine when the recursive structure must update.
	 */
	private Hub hubMaster;
	
	/**
	 * A copy of the current child Hub associated with the master Hub’s Active Object.
	 * Updated as recursion changes, ensuring the root Hub reflects the correct branch.
	 */
	private volatile HubCopy hubCopy;
	
	/**
	 * The property name representing the recursive one-to-many link from a master object
	 * to its child Hub. Used to retrieve the correct detail Hub when updating.
	 */
	private String propertyFromMaster;
	
	/**
	 * Listener registered on {@code hubMaster} to detect Active Object changes.
	 * Triggers updates to maintain the correct recursive root structure.
	 */
	private HubListener hubListener;

	/**
	 * Creates a HubRoot to ensure that a recursive Hub stays positioned at the root
	 * of its hierarchy and never becomes shared with a child Hub.
	 *
	 * <p>Behavior:</p>
	 * <ul>
	 *   <li>Identifies whether the Hub’s object type uses a recursive link.</li>
	 *   <li>If not recursive, sets the root Hub to share the provided Hub directly.</li>
	 *   <li>If recursive, determines the master Hub and establishes update logic.</li>
	 *   <li>Registers a listener to detect Active Object changes on the master Hub.</li>
	 *   <li>Initializes the root Hub by calling {@link #update()}.</li>
	 * </ul>
	 *
	 * @param hub     the original Hub in the recursive hierarchy
	 * @param hubRoot the Hub designated to remain the fixed root
	 */
	public HubRoot(Hub hub, Hub hubRoot) {
		this.hubRoot = hubRoot;
		if (hub == null) {
			return;
		}
		if (hubRoot == null) {
			return;
		}

		Class clazz = hub.getObjectClass();
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);
		OALinkInfo li = oi.getRecursiveLinkInfo(OALinkInfo.MANY);
		if (li == null) {
			hubRoot.setSharedHub(hub, true);
			return;
		}

		li = HubDetailDelegate.getLinkInfoFromDetailToMaster(hub);
		if (li != null) {
			li = OAObjectInfoDelegate.getReverseLinkInfo(li);
		}
		if (li == null || !li.getRecursive()) {
			hubRoot.setSharedHub(hub, false);
			return;
		}

		hubMaster = hub.getMasterHub(); // master hub of root hub - this is the 'source' to listen to.
		if (hubMaster == null) {
			Hub h = hub;
			for (;;) {
				Hub hx = h.getSharedHub();
				if (hx == null) {
					break;
				}
				h = hx;
			}
			hubCopy = new HubCopy(h, hubRoot, false);
			return;
		}

		propertyFromMaster = HubDetailDelegate.getPropertyFromMasterToDetail(hub);

		hubListener = new HubListenerAdapter() {
			@Override
			public void afterChangeActiveObject(HubEvent e) {
				if (lastAO != null) { // 20180305 ao could be the same as before
					if (lastAO == e.getObject()) {
						return;
					}
				}
				HubRoot.this.update();
			}
		};
		hubMaster.addHubListener(hubListener);

		update();
	}

	/**
	 * Tracks the last Active Object processed during recursive updates, allowing
	 * the class to avoid unnecessary refreshes when the AO has not changed.
	 */
	private Object lastAO;

	/**
	 * Guards the update process against concurrent execution. Ensures that only one
	 * update runs at a time and logs a warning if contention occurs.
	 */
	private final AtomicBoolean abUpdate = new AtomicBoolean();

	/**
	 * Ensures serialized execution of recursive updates.
	 *
	 * <p>Attempts to acquire the update lock; if another update is running,
	 * logs a warning and briefly waits before retrying. Delegates to
	 * {@link #_update()} once it gains exclusive access.</p>
	 */
	private void update() {
		try {
			for (int i = 0; i < 3; i++) {
				boolean bx = abUpdate.getAndSet(true);
				if (!bx) {
					break;
				}
				String s = "concurrent issue, where update is currently running in another thread, will wait 3ms (" + (i + 1)
						+ " of 3 times), this thread=" + Thread.currentThread();
				LOG.log(Level.WARNING, s, new Exception(s));
				Thread.sleep(3);
			}
			_update();
		} catch (Exception e) {

		} finally {
			abUpdate.set(false);
		}
	}

	/**
	 * Rebuilds the recursive root Hub to reflect the current Active Object on
	 * the master Hub.
	 *
	 * <p>Behavior:</p>
	 * <ul>
	 *   <li>Closes and clears any previous HubCopy.</li>
	 *   <li>Clears the root Hub.</li>
	 *   <li>Retrieves the Active Object from the master Hub.</li>
	 *   <li>If null, stops processing.</li>
	 *   <li>Retrieves the child Hub from the Active Object using the recursive link.</li>
	 *   <li>Creates a new HubCopy to mirror that child Hub into the root Hub.</li>
	 * </ul>
	 */
	private void _update() {
		if (hubCopy != null) {
			hubCopy.close();
			hubCopy = null;
		}

		this.hubRoot.clear();

		OAObject obj = (OAObject) hubMaster.getAO();
		lastAO = obj;
		if (obj == null) {
			return;
		}

		Hub h = (Hub) obj.getProperty(propertyFromMaster);
		hubCopy = new HubCopy(h, hubRoot, false);
	}

	/**
	 * Cleans up the HubRoot by unregistering the Active Object listener
	 * from the master Hub (if present). Prevents further recursive updates.
	 */
	public void close() {
		if (hubListener != null && hubMaster != null) {
			hubMaster.removeHubListener(hubListener);
		}
	}

}
