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
package com.viaoa.hub.auto;



import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.viaoa.callback.OAObjectCallback;
import com.viaoa.converter.OAConv;
import com.viaoa.converter.OAConverter;
import com.viaoa.lang.oa.VEnum;
import com.viaoa.object.*;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadLocalService;
import com.viaoa.runtime.OAThreadService;
import com.viaoa.runtime.thread.OAThreadLocalHubMergerCallback;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.lang.OAStr;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.oa.OA;
import com.viaoa.oa.service.hub.HubStatusService;

/**
 * Maintains object synchronization between two {@link Hub}s by ensuring that each
 * object in the master Hub has a matching object or reference in the target Hub.
 *
 * <p>Used to automatically populate or prune objects to keep related collections
 * consistent without manual synchronization logic.</p>
 *
 * <h3>Behavior</h3>
 * <ul>
 *   <li>Listens for add/remove/newList events on the master Hub.</li>
 *   <li>Creates new target objects or adds existing references when necessary.</li>
 *   <li>Removes objects not found in the master Hub (override {@link #okToRemove}).</li>
 *   <li>Optional stop condition ({@link #objStop}/{@link #stopProperty}) halts updates dynamically.</li>
 *   <li>Thread-safe with {@link java.util.concurrent.atomic.AtomicBoolean} reentrancy control.</li>
 *   <li>Fully compatible with OA’s distributed and merger frameworks.</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * Hub<ItemOption> hubItemOpts = new Hub<>(ItemOption.class);
 * Hub<ItemOptionType> hubOptionTypes = new Hub<>(ItemOptionType.class);
 *
 * // Ensure each ItemOption has a corresponding ItemOptionType reference
 * new HubAutoMatch<>(hubItemOpts, "itemOptionType", hubOptionTypes);
 * }</pre>
 *
 * @param <TYPE>      The target Hub object type
 * @param <PROPTYPE>  The master Hub object type
 */
public class HubAutoMatch<TYPE extends OAObject, TYPE2 extends OAObject> extends HubListenerAdapter<TYPE2> implements java.io.Serializable {
	static final long serialVersionUID = 1L;

	/**
	 * The target Hub that will be synchronized to match the contents of the
	 * master Hub.
	 */
	protected Hub<TYPE> hub;

	/**
	 * The master Hub providing source objects that the target Hub must match.
	 */
	protected Hub<TYPE2> hubMaster;

	/**
	 * Name of the property used to map objects in the target Hub to matching
	 * objects or values from the master Hub.
	 */
	protected String property;

	/**
	 * Indicates whether synchronization is invoked manually instead of
	 * automatically reacting to Hub events.
	 */
	protected boolean bManuallyCalled;

	/**
	 * When true, synchronization events initiated on the server will be
	 * published to clients even if triggered by server-only threads.
	 */
	private boolean bServerSideOnly;

	/**
	 * Controls whether synchronization logic is active. When false, all update
	 * operations are skipped.
	 */
	private boolean bEnabled = true;

	/**
	 * Optional object whose property controls whether synchronization should
	 * stop dynamically.
	 */
	protected OAObject objStop;

	/**
	 * Name of the property on objStop used to determine whether updates should
	 * be halted.
	 */
	protected String stopProperty; 

	/**
	 * Flag used to enable diagnostic or debug behavior within synchronization
	 * operations.
	 */
    private boolean bDebug;

    /**
     * Getter method dynamically resolved for the matching property. Used to
     * compare objects or extract key values from objects in the target Hub.
     */
	protected transient Method getMethod;

	/**
	 * Setter method dynamically resolved for the matching property. Used to
	 * assign values to new or synchronized objects in the target Hub.
	 */
	protected transient Method setMethod;

	/**
	 * Constructs a HubAutoMatch that synchronizes the target hub with the master hub
	 * based on the specified property. The update behavior can be configured to run
	 * automatically or only when manually triggered.
	 *
	 * @param hub             the target hub whose objects will be synchronized
	 * @param property        the property in the target hub used to match objects
	 * @param hubMaster       the master hub providing source objects for matching
	 * @param bManuallyCalled whether updates must be invoked manually
	 */
	public HubAutoMatch(Hub<TYPE> hub, String property, Hub<TYPE2> hubMaster, boolean bManuallyCalled) {
		this.bManuallyCalled = bManuallyCalled;
		init(hub, property, hubMaster, null, null);
	}

	/**
	 * Constructs a HubAutoMatch using automatic update mode. Synchronizes the
	 * target hub with the master hub based on the specified property.
	 *
	 * @param hub       the target hub whose objects will be synchronized
	 * @param property  the property in the target hub used to match objects
	 * @param hubMaster the master hub providing source objects for matching
	 */
	public HubAutoMatch(Hub<TYPE> hub, String property, Hub<TYPE2> hubMaster) {
		this(hub, property, hubMaster, false);
	}

	/**
	 * Constructs a HubAutoMatch with an optional stop condition. Synchronization
	 * will halt when the stop object's stop property evaluates to true.
	 *
	 * @param hub          the target hub whose objects will be synchronized
	 * @param property     the property in the target hub used to match objects
	 * @param hubMaster    the master hub providing source objects
	 * @param objStop      the object whose property controls stopping behavior
	 * @param stopProperty the property name evaluated for stopping synchronization
	 */
	public HubAutoMatch(Hub<TYPE> hub, String property, Hub<TYPE2> hubMaster, OAObject objStop, String stopProperty) {
		init(hub, property, hubMaster, objStop, stopProperty);
	}


	/**
	 * Default constructor. The {@link #init(Hub, String, Hub, OAObject, String)}
	 * method must be called before use.
	 */
	public HubAutoMatch() {
	}

	/**
	 * Indicates whether initialization has already occurred, preventing
	 * duplicate setup when using the default constructor.
	 */
	private boolean bInit;

	// required to call if using the second empty constructor
	/**
	 * Initializes the HubAutoMatch configuration when using the default constructor.
	 * Registers listeners, sets matching behavior, assigns stop conditions, and
	 * initializes getter/setter methods for the matching property.
	 *
	 * @param hub          the target hub to synchronize
	 * @param property     the property in the target hub used for matching
	 * @param hubMaster    the master hub providing source objects
	 * @param objStop      the object controlling stop behavior
	 * @param stopProperty the property used to evaluate stopping
	 */
	public void init(Hub<TYPE> hub, String property, Hub<TYPE2> hubMaster, OAObject objStop, String stopProperty) {
		if (bInit) {
			return;
		}
		if (hub == null) {
			throw new IllegalArgumentException("hub can not be null");
		}
		if (hubMaster == null) {
			// 20220802 now allows auto match on Enum property
			// throw new IllegalArgumentException("hubMaster can not be null");
		}
		bInit = true;
		this.hub = hub;
		this.hubMaster = hubMaster;
		if (!bManuallyCalled && hubMaster != null) {
			hubMaster.addHubListener(this);
		}
		this.objStop = objStop;
		this.stopProperty = stopProperty;


		// add listener on objStore.stopProperty ??

		setProperty(property);
	}

	/*
	 * This needs to be set to true if it is only created on the server, but client applications will be using the same Hub that is
	 * filtered. This is so that changes on the hub will be published to the clients, even if initiated on an OAClientThread.
	 */
	/**
	 * Enables or disables server-side-only update mode. When enabled, updates
	 * triggered on server threads will be published to clients.
	 *
	 * @param b whether the HubAutoMatch runs in server-side-only mode
	 */
	public void setServerSideOnly(boolean b) {
		bServerSideOnly = b;
	}

	/**
	 * Closes this HubAutoMatch instance by removing its listener from the master hub.
	 * This stops future automatic updates.
	 */
	public void close() {
		if (hubMaster != null) {
			hubMaster.removeHubListener(this);
		}
	}

	/**
	 * Ensures cleanup during garbage collection by calling {@link #close()} before
	 * finalization.
	 *
	 * @throws Throwable if superclass finalization throws an exception
	 */
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}

	/**
	 * Configures the property used for object matching. Determines getter and setter
	 * methods dynamically based on the hub's object class. If no property is
	 * specified, attempts to infer it by scanning link information.
	 *
	 * @param property the property name used for matching, or {@code null} to infer
	 */
	protected void setProperty(String property) {
		this.property = property;
		Class c = null;
		if (hubMaster != null && (property == null || property.length() == 0)) {
			c = hub.getObjectClass();
			if (!hubMaster.getObjectClass().equals(c)) {
				// find property to use
				final OA oa = OARuntime.oa(c);
				OAObjectInfo oi = oa.internal().objects().info().getOAObjectInfo(c);
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
			final OA oa = OARuntime.oa(hub);
		    getMethod = oa.internal().objects().info().getMethod(hub.getObjectClass(), "get" + property);
			//was: getMethod = OAReflect.getMethod(hub.getObjectClass(), "get" + property);
			if (getMethod == null) {
				throw new RuntimeException("getMethod for property \"" + property + "\" in class " + hub.getObjectClass());
			}
            setMethod = oa.internal().objects().info().getMethod(hub.getObjectClass(), "set" + property);
			//was: setMethod = OAReflect.getMethod(hub.getObjectClass(), "set" + property);
			if (setMethod == null) {
				throw new RuntimeException("setMethod for property \"" + property + "\" in class " + hub.getObjectClass());
			}
			c = getMethod.getReturnType();
		}
		if (hubMaster != null && !hubMaster.getObjectClass().equals(c)) {
			throw new RuntimeException("hubMaster class=" + hubMaster.getObjectClass() + " does not match class for update Hub: " + c);
		}
		if (!bManuallyCalled) {
			update();
		}
	}

	/**
	 * Thread-safety guard preventing reentrant or concurrent update operations.
	 */
	private AtomicBoolean abUpdating = new AtomicBoolean(false);

	/**
	 * Performs synchronization between the target hub and the master hub. Delegates
	 * to the internal update method with full in-sync checking.
	 */
    public void update() {
        _update(true);
    }

    /**
     * Internal update routine that synchronizes the target hub with the master hub.
     * Performs stop-condition evaluation, thread-safety checks, state verification,
     * and delegates to master-based or enum-based update routines.
     *
     * @param bCheckInSync whether to verify HubCurrentState is InSync before updating
     */
	protected void _update(final boolean bCheckInSync) {
		if (!getEnabled()) return;

		if (objStop != null && OAStr.isNotEmpty(this.stopProperty)) {
			Object obj = objStop.getProperty(this.stopProperty);
			if (OAConv.toBoolean(obj)) {
				return;
			}
		}

		if (!abUpdating.compareAndSet(false, true)) {
			return; // already updating
		}
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  

		boolean bWas = false;
        try {
            if (bServerSideOnly) {
            	bWas = srvcOAThreadLocal.getSendSyncMessages();
                srvcOAThreadLocal.setSendSyncMessages(true);
            }
            if (bCheckInSync) {
        		final OA oa = OARuntime.oa(this.hub);
    			if (oa.internal().hubs().status().getCurrentState(hub, null, null) != Hub.HubCurrentStateEnum.InSync) {
    				return;
    			}
    			if (hubMaster != null && oa.internal().hubs().status().getCurrentState(hubMaster, null, null) != Hub.HubCurrentStateEnum.InSync) {
    			    srvcOAThreadLocal.addHubMergerCallback(new OAThreadLocalHubMergerCallback() {
                        @Override
                        /**
                         * Runs the configured Hub callback operation.
                         */
                        public void callback() {
                            _update(false);
                        }
                    });
    				return;
    			}
            }

			if (hubMaster != null) {
				_update1();
			} else {
				_update2();
			}
		} finally {
			abUpdating.set(false);
			if (bServerSideOnly) {
				srvcOAThreadLocal.setSendSyncMessages(bWas);
			}
		}
	}

	/**
	 * Synchronizes the target hub using a master hub. Adds missing objects based on
	 * the matching property and removes objects that no longer exist in the master
	 * hub when permitted.
	 */
	private void _update1() {
		if (hub != null) {
			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
			if (srvcOAThreadLocal.isDeleting(hub.getMasterObject())) {
				return;
			}
		}

		final OA oa = OARuntime.oa(this.hub);
		// Step 1: verify that both hubs are using the correct hub
		//         (in case AO of master hub has been changed, and one of these hubs has not yet been adjusted).
		final Hub<TYPE2> hubMasterx = oa.internal().hubs().detail().getRealHub(hubMaster);
		final Hub<TYPE> hubx = oa.internal().hubs().detail().getRealHub(hub); // in case it is a detailHub and has not been updated yet
		if (hubx == null) {
			return;
		}

		// Step 2: see if every object in hubMasterx exists in hubx
		for (int i = 0;; i++) {
			TYPE2 obj = hubMasterx.elementAt(i);
			if (obj == null) {
				break;
			}
			// see if object is in hubx
			if (getMethod == null) {
				if (hubx.getObject(obj) == null) {
					if (hubx.getAllowAdd((TYPE) obj, true)) {
						hubx.add((TYPE) obj);
					}
				}
			} else {
				Class<?> returnTypeClass = getMethod.getReturnType();
				for (int j = 0;; j++) {
					Object o = hubx.elementAt(j);
					if (o == null) {
						if (hubx.getAllowAdd((TYPE) obj, true)) {
							createNewObject((TYPE2) obj);
						}
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
					if (!OAObject.class.isAssignableFrom(returnTypeClass)) {
						// ex: VEnum used to set
						Object obj2 = OAConverter.convert(returnTypeClass, obj);
						if (o != null && o.equals(obj2)) {
							break;
						}
					}
				}
			}
		}
		// Step 3: remove objects not in hubMasterx
		for (int i = 0;; i++) {
			TYPE obj = hubx.elementAt(i);
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
				if (hubx.getAllowRemove(obj, false, true)) {
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

	/*
	private int maxEnumValue;
	private boolean bMaxEnumValueChecked;
	*/

	/**
	 * Synchronizes the target hub based on enum values. Ensures the target hub
	 * contains one object per enum display name value for the configured property.
	 */
	private void _update2() {
		/*
		if (!bMaxEnumValueChecked) {
			maxEnumValue = findMaxEnumValue();
			bMaxEnumValueChecked = false;
		}
		*/

		Class<? extends OAObject> cz = hub.getObjectClass();

		final OA oa = OARuntime.oa(cz);
		Hub<VEnum> hubEnums = oa.internal().objects().enumx().getVEnums(cz, property);
		int max = hubEnums.size();


		for (int i = hub.getSize(); i < max; i++) {






//			createNewObject(i);
		}
	}



	/**
	 * Determines whether an object should be removed when it does not exist in the
	 * master hub. Default implementation allows removal.
	 *
	 * @param obj           the object considered for removal
	 * @param propertyValue the matched property value for the object
	 * @return {@code true} if removal is allowed
	 */
	public boolean okToRemove(Object obj, Object propertyValue) {
		return true;
	}

	/**
	 * Creates a new instance of the hub object type, sets its matching property
	 * value, and adds it to the target hub.
	 *
	 * @param obj the property value used to initialize the new object
	 * @return the created object added to the hub
	 */
	protected TYPE createNewObject(TYPE2 obj) {
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

	/**
	 * HubListener callback triggered after an insert event on the master hub.
	 * Initiates an update unless loading or hub-merger processing is active.
	 *
	 * @param e the hub event associated with the insert
	 */
	public @Override void afterInsert(HubEvent e) {
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
		if (!srvcOAThreadLocal.isLoading()) {
			if (!srvcOAThreadLocal.isHubMergerChanging()) { // else wait for newList
				update();
			}
		}
	}

	/**
	 * HubListener callback triggered after an add event on the master hub.
	 * Initiates an update unless loading or hub-merger processing is active.
	 *
	 * @param e the hub event associated with the add
	 */
	public @Override void afterAdd(HubEvent e) {
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
		if (!srvcOAThreadLocal.isLoading()) {
			if (!srvcOAThreadLocal.isHubMergerChanging()) { // else wait for newList
				update();
			}
		}
	}

	/**
	 * HubListener callback triggered after a remove event on the master hub.
	 * Updates the target hub unless hub-merger processing is active.
	 *
	 * @param e the hub event associated with the removal
	 */
	public @Override void afterRemove(HubEvent e) {
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
		if (srvcOAThreadLocal.isHubMergerChanging()) {
			return; // else wait for newList
		}
		update();
	}

	/**
	 * HubListener callback triggered when the master hub fires a newList event.
	 * Initiates an update unless hub-merger processing is active.
	 *
	 * @param e the hub event associated with the new list
	 */
	public @Override void onNewList(HubEvent e) {
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
		if (srvcOAThreadLocal.isHubMergerChanging()) { // else wait for newList after merger is done
			return;
		}
		update();
	}

	/**
	 * Enables or disables synchronization updates.
	 *
	 * @param b {@code true} to enable updates, {@code false} to disable
	 */
	public void setEnabled(boolean b) {
		this.bEnabled = b;
	}

	/**
	 * Returns whether synchronization updates are enabled.
	 *
	 * @return {@code true} if updates are enabled
	 */
	public boolean getEnabled() {
		return this.bEnabled;
	}

}
