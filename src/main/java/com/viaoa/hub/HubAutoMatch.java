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
package com.viaoa.hub;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCallback;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.remote.OARemoteThreadDelegate;
import com.viaoa.util.OAReflect;

/**
 * Makes sure that for each object in a master hub, there exists an object with a reference to it in a second hub.
 *
 * @see Hub#setAutoMatch
 */
public class HubAutoMatch<TYPE, PROPTYPE> extends HubListenerAdapter implements java.io.Serializable {
	static final long serialVersionUID = 1L;

	protected Hub hub, hubMaster;
	protected String property;
	protected boolean bManuallyCalled;
	private boolean bServerSideOnly;

	protected transient Method getMethod, setMethod;

	/**
	 * Create new HubAutoMatch that will automatically create objects in a Hub with references that match the objects in a master hub. ex:
	 * new HubAutoMatch(hubItem, "itemOptionTypes", itemMain.getItemOptionTypes())
	 *
	 * @param hubMaster       hub that has all objects to use
	 * @param property        property in hub that has same type as objects in hubMaster.
	 * @param bManuallyCalled set to true if the update method will be manually called. This is used in cases where the hubMaster could be
	 *                        generating events that should not affect the matching. For example, if the hubMaster is controlled by a
	 *                        HubMerger and objects are added/removed.
	 */
	public HubAutoMatch(Hub<TYPE> hub, String property, Hub<PROPTYPE> hubMaster, boolean bManuallyCalled) {
		this.bManuallyCalled = bManuallyCalled;
		init(hub, property, hubMaster);
	}

	public HubAutoMatch(Hub<TYPE> hub, String property, Hub<PROPTYPE> hubMaster) {
		this(hub, property, hubMaster, false);
	}

	public HubAutoMatch() {
	}

	private boolean bInit;

	// required to call if using the second empty constructor
	public void init(Hub<TYPE> hub, String property, Hub<PROPTYPE> hubMaster) {
		if (bInit) {
			return;
		}
		bInit = true;
		if (hub == null) {
			throw new IllegalArgumentException("hub can not be null");
		}
		if (hubMaster == null) {
			throw new IllegalArgumentException("hubMaster can not be null");
		}

		this.hub = hub;
		this.hubMaster = hubMaster;
		if (!bManuallyCalled) {
			hubMaster.addHubListener(this);
		}
		setProperty(property);
	}

	/**
	 * This needs to be set to true if it is only created on the server, but client applications will be using the same Hub that is
	 * filtered. This is so that changes on the hub will be published to the clients, even if initiated on an OAClientThread.
	 */
	public void setServerSideOnly(boolean b) {
		bServerSideOnly = b;
	}

	/**
	 * Closes HubAutoMatch.
	 */
	public void close() {
		hubMaster.removeHubListener(this);
	}

	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}

	protected void setProperty(String property) {
		this.property = property;
		Class c = null;
		if (property == null || property.length() == 0) {
			c = hub.getObjectClass();
			if (!hubMaster.getObjectClass().equals(c)) {
				// find property to use
				OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(c);
				List al = oi.getLinkInfos();
				for (int i = 0; i < al.size(); i++) {
					OALinkInfo li = (OALinkInfo) al.get(i);
					if (!li.getUsed()) {
						continue;
					}
					if (li.getType() == li.ONE && hubMaster.getObjectClass().equals(li.getToClass())) {
						property = li.getName();
					}
				}
			}
		}
		if (property != null) {
			getMethod = OAReflect.getMethod(hub.getObjectClass(), "get" + property);
			if (getMethod == null) {
				throw new RuntimeException("getMethod for property \"" + property + "\" in class " + hub.getObjectClass());
			}
			setMethod = OAReflect.getMethod(hub.getObjectClass(), "set" + property);
			if (setMethod == null) {
				throw new RuntimeException("setMethod for property \"" + property + "\" in class " + hub.getObjectClass());
			}
			c = getMethod.getReturnType();
		}
		if (!hubMaster.getObjectClass().equals(c)) {
			throw new RuntimeException("hubMaster class=" + hubMaster.getObjectClass() + " does not match class for update Hub: " + c);
		}
		if (!bManuallyCalled) {
			update();
		}
	}

	private AtomicBoolean abUpdating = new AtomicBoolean(false);

	public void update() {
		if (!abUpdating.compareAndSet(false, true)) {
			return; // already updating
		}
		try {
			if (HubDelegate.getCurrentState(hub, null, null) != HubDelegate.HubCurrentStateEnum.InSync) {
				return;
			}
			if (HubDelegate.getCurrentState(hubMaster, null, null) != HubDelegate.HubCurrentStateEnum.InSync) {
				return;
			}
			if (bServerSideOnly) {
				OARemoteThreadDelegate.sendMessages(true);
			}
			_update1();
		} finally {
			abUpdating.set(false);
			if (bServerSideOnly) {
				OARemoteThreadDelegate.sendMessages(false);
			}
		}
	}

	private void _update1() {
		if (hub != null) {
			if (OAThreadLocalDelegate.isDeleting(hub.getMasterObject())) {
				return;
			}
		}

		// Step 1: verify that both hubs are using the correct hub
		//         (in case AO of master hub has been changed, and one of these hubs has not yet been adjusted).
		Hub hubMasterx = HubDetailDelegate.getRealHub(hubMaster);
		Hub hubx = HubDetailDelegate.getRealHub(hub); // in case it is a detailHub and has not been updated yet
		if (hubx == null) {
			return;
		}

		// Step 2: see if every object in hubMasterx exists in hubx
		for (int i = 0;; i++) {
			Object obj = hubMasterx.elementAt(i);
			if (obj == null) {
				break;
			}
			// see if object is in hubx
			if (getMethod == null) {
			    if (hubx.getObject(obj) == null) {
			        if (hubx.getAllowAdd(OAObjectCallback.CHECK_AllButProcessed, obj)) {
                        hubx.add(obj);
				    }
				    /* 20210514 was:
                    if (hubx.getEnabled()) {
                        hubx.add(obj);
                    }
                    */
				}
			} else {
				for (int j = 0;; j++) {
					Object o = hubx.elementAt(j);
					if (o == null) {
	                    if (hubx.getAllowAdd(OAObjectCallback.CHECK_AllButProcessed, obj)) {
                            createNewObject((PROPTYPE) obj);
	                    }
	                    /* 20210514 was:
						if (hubx.getEnabled()) {
						    createNewObject((PROPTYPE) obj);
						}
						*/
						break;
					}
					try {
						o = getMethod.invoke(o, new Object[] {});
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
					if (o != null && o.equals(obj)) {
						break;
					}
				}
			}
		}
		// Step 3: remove objects not in hubMasterx
		for (int i = 0;; i++) {
			Object obj = hubx.elementAt(i);
			if (obj == null) {
				break;
			}

			Object value;
			try {
				if (getMethod != null) {
					value = getMethod.invoke(obj, new Object[] {});
				} else {
					value = obj;
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			if (hubMasterx.getObject(value) == null) {
				if (hubx.getAllowRemove(OAObjectCallback.CHECK_AllButProcessed, obj)) {
					if (okToRemove(obj, value)) {
						hubx.remove(i);
						if (obj instanceof OAObject) {
							((OAObject) obj).delete();
						}
						i--;
					}
				}
			}
		}
	}

	/**
	 * Called before removing an object that does not have a matching value.
	 */
	public boolean okToRemove(Object obj, Object propertyValue) {
		return true;
	}

	protected TYPE createNewObject(PROPTYPE obj) {
		TYPE object;
		try {
			object = (TYPE) hub.getObjectClass().newInstance();
			if (setMethod != null) {
				setMethod.invoke(object, new Object[] { obj });
			}
			hub.add(object);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return (TYPE) object;
	}

	/** HubListener interface method, used to listen to changes to master Hub. */
	public @Override void afterInsert(HubEvent e) {
		if (!OAThreadLocalDelegate.isLoading()) {
			if (!OAThreadLocalDelegate.isHubMergerChanging()) { // else wait for newList
				update();
			}
		}
	}

	/** HubListener interface method, used to listen to changes to master Hub. */
	public @Override void afterAdd(HubEvent e) {
		if (!OAThreadLocalDelegate.isLoading()) {
			if (!OAThreadLocalDelegate.isHubMergerChanging()) { // else wait for newList
				update();
			}
		}
	}

	/** HubListener interface method, used to listen to changes to master Hub. */
	public @Override void afterRemove(HubEvent e) {
		if (OAThreadLocalDelegate.isHubMergerChanging()) {
			return; // else wait for newList
		}

		update();
	}

	/** HubListener interface method, used to listen to changes to master Hub. */
	public @Override void onNewList(HubEvent e) {
		if (OAThreadLocalDelegate.isHubMergerChanging()) { // else wait for newList after merger is done
			return;
		}
		update();
	}
}
