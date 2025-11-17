/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
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

	private final Hub hubRoot;
	private Hub hubMaster;
	private volatile HubCopy hubCopy;
	private String propertyFromMaster;
	private HubListener hubListener;

	/**
	 * This is used for recursive hubs, so that a Hub will stay at the root. By default, a shared hub that is recursive could change to be
	 * shared with a child hub. This class is used to make sure that the hub does not change to share a child hub.
	 *
	 * @param hubRoot Hub to use as the root, it will auto populated to always be the root hub.
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

	private Object lastAO;

	private final AtomicBoolean abUpdate = new AtomicBoolean();

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

	public void close() {
		if (hubListener != null && hubMaster != null) {
			hubMaster.removeHubListener(hubListener);
		}
	}

}
