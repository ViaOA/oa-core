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
package com.viaoa.hub.listener;

import java.util.ArrayList;

import com.viaoa.callback.OAObjectCallback;
import com.viaoa.compare.OAAnyValueObject;
import com.viaoa.compare.OACompare;
import com.viaoa.compare.OAEmptyObject;
import com.viaoa.compare.OANotEmptyObject;
import com.viaoa.compare.OANotNullObject;
import com.viaoa.compare.OANullObject;
import com.viaoa.converter.OAConv;
import com.viaoa.filter.OAFilter;
import com.viaoa.graph.OAGraphInternal;
import com.viaoa.graph.service.OAObjectService;
import com.viaoa.graph.service.object.OAObjectCallbackService;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.lang.OAArray;
import com.viaoa.lang.OAString;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;
import com.viaoa.select.OASelect;

/**
 * Rule-based, multi-hub condition monitor that aggregates checks over one or
 * more {@link Hub}s (and optional property paths) and evaluates them as a single
 * boolean via {@link #getValue()}.
 * <p>
 * Use the {@code add*} methods to compose conditions (hub validity/emptiness,
 * AO null/new, property null/empty/not-empty, object-callback enabled/visible,
 * custom {@link com.viaoa.filter.OAFilter}, etc.). The listener internals will
 * attach a shared {@link HubListener} per (hub,propertyPath) and re-use it across
 * rules to minimize overhead. On relevant {@link HubEvent}s, it recomputes and
 * invokes {@code callOnChange()} in subclasses.
 * <p>
 * Comparisons are expressed via {@link Type} or by compare-value objects
 * (including {@code OANullObject}, {@code OANotNullObject}, {@code OAEmptyObject},
 * {@code OANotEmptyObject}, {@code OAAnyValueObject}). Tooltips/failure reasons
 * are tracked to aid UI enablement/visibility logic.
 */
public abstract class HubChangeListener {
	
	/**
	 * Array of rule definitions monitored by this listener. Each entry specifies
	 * a hub, optional property path, comparison logic, and associated listener
	 * configuration.
	 */
	protected HubProp[] hubProps = new HubProp[0];
	
	/**
	 * Debug flag enabling diagnostic behavior within the listener’s evaluation
	 * and event-handling routines when set to true.
	 */
	public boolean DEBUG;
	
	/**
	 * Reference to the most recently processed HubEvent. Used to prevent
	 * duplicate processing of the same event instance.
	 */
	private HubEvent lastHubEvent;
	
	/**
	 * Holds the message describing the cause of the most recent rule failure
	 * during evaluation. Used to build tooltips and UI feedback.
	 */
	private String failureReason;

	/**
	 * Enumeration representing specific comparison types used by
	 * HubChangeListener when evaluating hub or property rules.
	 * Each enum constant carries a flag indicating whether the
	 * comparison should evaluate only the active object (AO).
	 */
	public enum Type {
		/**
		 * Comparison type with no specific rule, initialized with
		 * use-AO-only behavior enabled.
		 */
		Unknown(true),

		/**
		 * Comparison type indicating that the hub must be valid.
		 * Evaluated using active-object-only mode.
		 */
		HubValid(true),
		
		/**
		 * Comparison type requiring the hub to be not valid.
		 * Evaluated using active-object-only mode.
		 */
		HubNotValid(true),
		
		/**
		 * Comparison type requiring the hub to be valid and have
		 * zero size. Active-object-only mode is disabled.
		 */
		HubEmpty(false),
		
		/**
		 * Comparison type requiring the hub to be valid and contain
		 * one or more objects. Active-object-only mode is disabled.
		 */
		HubNotEmpty(false),
		
		/**
		 * Comparison type requiring the hub's active object to be null.
		 * Evaluated using active-object-only mode.
		 */
		AoNull(true), // hub.activeObject
		
		/**
		 * Comparison type requiring the hub's active object to be not null,
		 * evaluated using active-object-only mode.
		 */
		AoNotNull(true),
		
		/**
		 * Comparison type requiring the hub's active object to exist and be new.
		 * Active-object-only mode enabled.
		 */
		AoNew(true),
		
		/**
		 * Comparison type requiring the hub's active object to exist and not be new.
		 * Active-object-only mode enabled.
		 */
		AoNotNew(true),
		
		/**
		 * Comparison type that always evaluates to true.
		 * Active-object-only mode enabled.
		 */
		AlwaysTrue(true),
		
		/**
		 * Comparison type that always evaluates to false.
		 * Active-object-only mode enabled.
		 */
		AlwaysFalse(true),
		
		/**
		 * Comparison type that evaluates true only when
		 * OAContext.isSuperAdmin() returns true.
		 * Active-object-only mode enabled.
		 */
		OnlySuperAdmin(true), // OAContext.isSuperAdmin must be true
		
		/**
		 * Comparison type requiring the specified property of the
		 * active object to be null. Active-object-only mode enabled.
		 */
		PropertyNull(true),
		
		/**
		 * Comparison type requiring the specified property of the
		 * active object to be not null. Active-object-only mode enabled.
		 */
		PropertyNotNull(true),
		
		/**
		 * Comparison type requiring the specified property of the
		 * active object to be empty. Active-object-only mode enabled.
		 */
		PropertyEmpty(true),
		
		/**
		 * Comparison type requiring the specified property of the
		 * active object to be non-empty. Active-object-only mode enabled.
		 */
		PropertyNotEmpty(true),
		
		/**
		 * Comparison type that uses an object callback to determine
		 * whether the specified property of the active object is enabled.
		 * Active-object-only mode enabled.
		 */
		ObjectCallbackEnabled(true),
		
		/**
		 * Comparison type that uses an object callback to determine
		 * whether the specified property of the active object is visible.
		 * Active-object-only mode enabled.
		 */
		ObjectCallbackVisible(true);

		/**
		 * Flag indicating whether this comparison type should evaluate
		 * only the hub's active object rather than all objects.
		 */
		private boolean bUseAoOnly;
		
		public boolean useAoOnly() {
			return bUseAoOnly;
		}

		Type(boolean b) {
			this.bUseAoOnly = b;
		}
	}

	/**
	 * Default constructor that creates an empty listener with no initial hub or property rules.
	 */
	public HubChangeListener() {
	}

	/**
	 * Creates a listener and immediately adds a rule for the specified hub
	 * using default comparison settings.
	 *
	 * @param hub the hub to associate with the initial rule
	 */
	public HubChangeListener(Hub hub) {
		add(hub);
	}

	/**
	 * Creates a listener and immediately adds a rule for the given hub and
	 * property path using default comparison behavior.
	 *
	 * @param hub          the hub to monitor
	 * @param propertyName the property path to evaluate
	 */
	public HubChangeListener(Hub hub, String propertyName) {
		add(hub, propertyName);
	}

	/**
	 * Creates a listener and immediately adds a rule for the given hub and
	 * property path using the supplied comparison value.
	 *
	 * @param hub          the hub to monitor
	 * @param propertyName the property path to evaluate
	 * @param compareValue the value or comparison rule to use
	 */
	public HubChangeListener(Hub hub, String propertyName, Object compareValue) {
		add(hub, propertyName, compareValue);
	}

	/**
	 * Creates a listener and immediately adds a rule for the given hub using
	 * the specified comparison type.
	 *
	 * @param hub  the hub to monitor
	 * @param type the comparison type to apply
	 */
	public HubChangeListener(Hub hub, HubChangeListener.Type type) {
		add(hub, type);
	}

	/**
	 * Adds a rule for the specified hub using default comparison settings.
	 *
	 * @param hub the hub to add to this listener
	 * @return the created HubProp instance
	 */
	public HubProp add(Hub hub) {
		return add(hub, null, true, Type.HubValid, null, false, null);
	}

	/**
	 * Adds a rule for the given hub and property path. If the property path
	 * is null, this behaves like {@link #add(Hub)}.
	 *
	 * @param hub          the hub to add
	 * @param propertyPath the property path to listen to
	 * @return the created HubProp instance
	 */
	public HubProp add(Hub hub, String propertyPath) {
		if (propertyPath == null) {
			return add(hub);
		} else {
			return add(hub, propertyPath, true, Type.AlwaysTrue, null, true, null);
		}
	}

	/**
	 * Adds a rule that checks whether the hub is valid.
	 *
	 * @param hub the hub to test
	 * @return the created HubProp instance
	 */
	public HubProp addHubValid(Hub hub) {
		return add(hub, null, true, Type.HubValid);
	}

	/**
	 * Adds a rule that checks whether the hub is valid, tied to a specific
	 * property path.
	 *
	 * @param hub          the hub to test
	 * @param propertyPath the property path to listen to
	 * @return the created HubProp instance
	 */
	public HubProp addHubValid(Hub hub, String propertyPath) {
		return add(hub, propertyPath, true, Type.HubValid);
	}

	/**
	 * Adds a rule that checks whether the hub is not valid.
	 *
	 * @param hub the hub to test
	 * @return the created HubProp instance
	 */
	public HubProp addHubNotValid(Hub hub) {
		return add(hub, null, true, Type.HubNotValid);
	}

	/**
	 * Adds a rule that checks whether the hub is valid and has zero size.
	 *
	 * @param hub the hub to test
	 * @return the created HubProp instance
	 */
	public HubProp addHubEmpty(Hub hub) {
		return add(hub, null, true, Type.HubEmpty);
	}

	/**
	 * Adds a rule that checks whether the hub is valid and contains one or
	 * more objects.
	 *
	 * @param hub the hub to test
	 * @return the created HubProp instance
	 */
	public HubProp addHubNotEmpty(Hub hub) {
		return add(hub, null, true, Type.HubNotEmpty);
	}

	/**
	 * Adds a rule that checks whether the hub's active object exists and is
	 * marked as new.
	 *
	 * @param hub the hub to test
	 * @return the created HubProp instance
	 */
	public HubProp addAoNew(Hub hub) {
		return add(hub, null, true, Type.AoNew);
	}

	/**
	 * Adds a rule that checks whether the hub's active object exists and is
	 * not marked as new.
	 *
	 * @param hub the hub to test
	 * @return the created HubProp instance
	 */
	public HubProp addAoNotNew(Hub hub) {
		return add(hub, null, true, Type.AoNotNew);
	}

	/**
	 * Adds a rule that checks whether the hub's active object is null.
	 *
	 * @param hub the hub to test
	 * @return the created HubProp instance
	 */
	public HubProp addAoNull(Hub hub) {
		return add(hub, null, true, Type.AoNull);
	}

	/**
	 * Adds a rule that checks whether the hub's active object is not null.
	 *
	 * @param hub the hub to test
	 * @return the created HubProp instance
	 */
	public HubProp addAoNotNull(Hub hub) {
		return add(hub, null, true, Type.AoNotNull);
	}

	/**
	 * Adds a rule that always evaluates to true for the given hub.
	 *
	 * @param hub the hub to associate with this rule
	 * @return the created HubProp instance
	 */
	public HubProp addAlwaysTrue(Hub hub) {
		return add(hub, null, true, Type.AlwaysTrue);
	}

	/**
	 * Adds a rule that always evaluates to true without requiring a hub.
	 *
	 * @return the created HubProp instance
	 */
	public HubProp addAlwaysTrue() {
		return add(null, null, true, Type.AlwaysTrue);
	}

	/**
	 * Adds a rule that always evaluates to false for the given hub.
	 *
	 * @param hub the hub to associate with this rule
	 * @return the created HubProp instance
	 */
	public HubProp addAlwaysFalse(Hub hub) {
		return add(hub, null, true, Type.AlwaysFalse);
	}

	/**
	 * Adds a rule that always evaluates to false without requiring a hub.
	 *
	 * @return the created HubProp instance
	 */
	public HubProp addAlwaysFalse() {
		return add(null, null, true, Type.AlwaysFalse);
	}

	/**
	 * Adds a rule that evaluates to true only when the global context
	 * indicates a super-admin user.
	 *
	 * @param hub the hub to associate with this rule
	 * @return the created HubProp instance
	 */
	public HubProp addOnlySuperAdmin(Hub hub) {
		return add(hub, null, true, Type.OnlySuperAdmin);
	}

	/**
	 * Adds a rule that evaluates to true only when the global context
	 * indicates a super-admin user, without requiring a hub.
	 *
	 * @return the created HubProp instance
	 */
	public HubProp addOnlySuperAdmin() {
		return add(null, null, true, Type.OnlySuperAdmin);
	}

	/**
	 * Adds a rule that checks whether the specified property of the hub's
	 * active object is null.
	 *
	 * @param hub  the hub to test
	 * @param prop the property to evaluate
	 * @return the created HubProp instance
	 */
	public HubProp addPropertyNull(Hub hub, String prop) {
		return add(hub, prop, true, Type.PropertyNull);
	}

	/**
	 * Adds a rule that checks whether the specified property of the hub's
	 * active object is not null.
	 *
	 * @param hub  the hub to test
	 * @param prop the property to evaluate
	 * @return the created HubProp instance
	 */
	public HubProp addPropertyNotNull(Hub hub, String prop) {
		return add(hub, prop, true, Type.PropertyNotNull);
	}

	/**
	 * Adds a rule that checks whether the specified property of the hub's
	 * active object is empty.
	 *
	 * @param hub  the hub to test
	 * @param prop the property to evaluate
	 * @return the created HubProp instance
	 */
	public HubProp addPropertyEmpty(Hub hub, String prop) {
		return add(hub, prop, true, Type.PropertyEmpty);
	}

	/**
	 * Adds a rule that checks whether the specified property of the hub's
	 * active object is not empty.
	 *
	 * @param hub  the hub to test
	 * @param prop the property to evaluate
	 * @return the created HubProp instance
	 */
	public HubProp addPropertyNotEmpty(Hub hub, String prop) {
		return add(hub, prop, true, Type.PropertyNotEmpty);
	}

	/**
	 * Adds a rule that monitors changes to the specified property without
	 * performing a comparison. Used for dependency tracking.
	 *
	 * @param hub  the hub to monitor
	 * @param prop the property whose changes trigger evaluation
	 * @return the created HubProp instance
	 */
	public HubProp addPropertyChange(Hub hub, String prop) {
		return add(hub, prop);
	}

	/**
	 * Adds a rule that uses an {@link OAFilter} to determine whether adding
	 * an object to the hub is allowed, updating the failure reason from the
	 * object callback result.
	 *
	 * @param hub the hub to evaluate
	 * @return the created HubProp instance
	 */
	public HubProp addAddEnabled(final Hub hub) {
		if (hub == null) {
			return null;
		}

		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hub);
		OAFilter filter = new OAFilter() {
			@Override
			public boolean isUsed(Object obj) {
				OAObjectCallback eq = og.objectsInternal().callObjectCallbackGetAllowAddObjectCallback(hub, null, OAObjectCallback.CHECK_ALL);
				boolean b = eq.getAllowed();
				if (!b) {
					failureReason = eq.getDisplayResponse();
				}
				return b;
			}
		};
		HubProp hp = add(hub, null, false, null, filter, false, "ObjectCallback.AllowAdd");

		og.objectsInternal().callObjectCallbackAddObjectCallbackChangeListeners(hub, hub.getObjectClass(), null, null, this, true);

		Hub hx = hub.getMasterHub();
		if (hx != null) {
			add(hx, Type.AoNotNull);
			String propx = og.hubsInternal().callHubDetailGetPropertyFromMasterToDetail(hub);
			og.objectsInternal().callObjectCallbackAddObjectCallbackChangeListeners(hx, hx.getObjectClass(), propx, null, this, true);
		}
		return hp;
	}

	/**
	 * Adds a rule that determines whether creation of a new object is
	 * allowed for the hub, based on an {@link OAFilter} and callback checks.
	 *
	 * @param hub the hub to evaluate
	 * @return the created HubProp instance
	 */
	@SuppressWarnings({"unchecked","rawtypes"})
	public HubProp addNewEnabled(final Hub<?> hub) {
		if (hub == null) {
			return null;
		}

		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hub);
		OAFilter filter = new OAFilter() {
			@Override
			public boolean isUsed(Object obj) {
				OAObjectCallback eq = og.objectsInternal().callObjectCallbackGetAllowNewObjectCallback(hub);
				boolean b = eq.getAllowed();
				if (!b) {
					failureReason = eq.getDisplayResponse();
				}
				return b;
			}
		};
		HubProp hp = add(hub, null, false, null, filter, false, "ObjectCallback.AllowNew");

		Hub<OAObject> hubX = (Hub) hub;
		Class classX = hubX.getObjectClass();
		og.objectsInternal().callObjectCallbackAddObjectCallbackChangeListeners(hubX, classX, null, null, this, true);

		Hub hx = hub.getMasterHub();
		if (hx != null) {
			add(hx, Type.AoNotNull);
			String propx = og.hubsInternal().callHubDetailGetPropertyFromMasterToDetail(hub);
			og.objectsInternal().callObjectCallbackAddObjectCallbackChangeListeners(hx, hx.getObjectClass(), propx, null, this, true);
		}
		return hp;
	}

	/**
	 * Adds a rule that checks whether deletion of the specified object is
	 * allowed for the hub. The rule can be limited to the active object.
	 *
	 * @param hub     the hub to evaluate
	 * @param bAoOnly true to limit checks to the active object
	 * @return the created HubProp instance
	 */
	@SuppressWarnings({"unchecked","rawtypes"})
	public HubProp addDeleteEnabled(final Hub<?> hub, boolean bAoOnly) {
		if (hub == null) {
			return null;
		}

		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hub);
		OAFilter filter = new OAFilter() {
			@Override
			public boolean isUsed(Object obj) {
				if (!(obj instanceof OAObject)) {
					return false;
				}

				Hub<OAObject> hubX = (Hub) hub;
				
				OAObjectCallback eq = og.objectsInternal().callObjectCallbackGetAllowDeleteObjectCallback(hubX, (OAObject) obj);
				boolean b = eq.getAllowed();
				if (!b) {
					failureReason = eq.getDisplayResponse();
					if (OAString.isEmpty(failureReason)) {
						failureReason = "edit query returned false";
					}
				}
				return b;
			}
		};
		HubProp hp = add(hub, null, false, null, filter, bAoOnly, "ObjectCallback.AllowDelete");

		Hub<OAObject> hubX = (Hub) hub;
		Class classX = hubX.getObjectClass();
		
		og.objectsInternal().callObjectCallbackAddObjectCallbackChangeListeners(hubX, classX, null, null, this, true);

		Hub<?> hx = hub.getMasterHub();
		if (hx != null) {
			add(hx, Type.AoNotNull);
			String propx = og.hubsInternal().callHubDetailGetPropertyFromMasterToDetail(hub);
			hubX = (Hub) hx;
			classX = hubX.getObjectClass();
			og.objectsInternal().callObjectCallbackAddObjectCallbackChangeListeners(hubX, classX, propx, null, this, true);
		}

		return hp;
	}

	/**
	 * Adds a rule that checks whether removal of the specified object is
	 * allowed for the hub, based on an {@link OAFilter}.
	 *
	 * @param hub the hub to evaluate
	 * @return the created HubProp instance
	 */
	public HubProp addRemoveEnabled(final Hub hub) {
		if (hub == null) {
			return null;
		}

		addAoNotNull(hub);
		OAFilter filter = new OAFilter() {
			@Override
			public boolean isUsed(Object obj) {
				// 20191214
				if (!(obj instanceof OAObject)) return false;

				boolean b = hub.getAllowRemove(OAObjectCallback.CHECK_ALL, (OAObject) obj);
				/*was
				boolean b;
				if (obj instanceof OAObject) b = hub.canRemove((OAObject) obj);
				else b = hub.canRemove();
				*/
				return b;
			}
		};
		HubProp hp = add(hub, null, false, null, filter, false, "Hub.canRemove");

		Hub hx = hub.getMasterHub();
		if (hx != null) {
			add(hx, Type.AoNotNull);
			final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hub);
			String propx = og.hubsInternal().callHubDetailGetPropertyFromMasterToDetail(hub);
			og.objectsInternal().callObjectCallbackAddObjectCallbackChangeListeners(hx, hx.getObjectClass(), propx, null, this, true);
		}
		return hp;
	}

	/**
	 * Adds a rule that checks whether the active object of the hub has
	 * changes and is therefore eligible for saving.
	 *
	 * @param hub the hub to evaluate
	 * @return the created HubProp instance
	 */
	public HubProp addSaveEnabled(final Hub hub) {
		if (hub == null) {
			return null;
		}
		addAoNotNull(hub);
		HubProp hp = add(hub, OAObjectService.WORD_Changed, true);
		return hp;
	}

	/**
	 * Adds a rule that determines whether copying of the hub's active
	 * object is allowed, using an {@link OAFilter}.
	 *
	 * @param hub the hub to evaluate
	 * @return the created HubProp instance
	 */
	public HubProp addCopyEnabled(final Hub hub) {
		if (hub == null) {
			return null;
		}

		addAoNotNull(hub);
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hub);
		OAFilter filter = new OAFilter() {
			@Override
			public boolean isUsed(Object obj) {
				if (!(obj instanceof OAObject)) return false;
				OAObjectCallback eq = og.objectsInternal().callObjectCallbackGetAllowCopyObjectCallback((OAObject) obj);
				boolean b = eq.getAllowed();
				if (!b) {
					failureReason = eq.getDisplayResponse();
				}
				return b;
			}
		};
		HubProp hp = add(hub, null, false, null, filter, false, "ObjectCallback.AllowCopy");
		return hp;
	}

	/**
	 * Adds a rule that determines whether pasting is allowed for the hub,
	 * based on an {@link OAFilter} and callback checks.
	 *
	 * @param hub the hub to evaluate
	 * @return the created HubProp instance
	 */
	public HubProp addPasteEnabled(final Hub hub) {
		if (hub == null) {
			return null;
		}

		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hub);
		OAFilter filter = new OAFilter() {
			@Override
			public boolean isUsed(Object obj) {
				OAObjectCallback eq = og.objectsInternal().callObjectCallbackGetAllowEnabledObjectCallback(hub);
				boolean b = eq.getAllowed();
				if (!b) {
					failureReason = eq.getDisplayResponse();
				}
				return b;
			}
		};
		HubProp hp = add(hub, null, false, null, filter, false, "ObjectCallback.AllowPaste");
		return hp;
	}

	/**
	 * Adds a rule that checks whether the object callback for the specified
	 * property indicates the object is enabled.
	 *
	 * @param hub  the hub to evaluate
	 * @param prop the property to check
	 * @return the created HubProp instance
	 */
	public HubProp addObjectCallbackEnabled(Hub hub, String prop) {
		return addObjectCallbackEnabled(hub, prop, true);
	}

	/**
	 * Adds a rule that checks object callback enabled status for the
	 * specified property, with an option to limit evaluation to the active
	 * object.
	 *
	 * @param hub     the hub to evaluate
	 * @param prop    the property to check
	 * @param bAoOnly true to evaluate only the active object
	 * @return the created HubProp instance
	 */
	public HubProp addObjectCallbackEnabled(Hub hub, String prop, boolean bAoOnly) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hub);
		og.objectsInternal().callObjectCallbackAddObjectCallbackChangeListeners(hub, hub.getObjectClass(), prop, null, this, true);
		// include master
		Hub hx = hub.getMasterHub();
		if (hx != null) {
			OALinkInfo li = og.hubsInternal().callHubDetailGetLinkInfoFromMasterObjectToDetail(hub);
			if (li != null && li.getOwner()) {
				String propx = og.hubsInternal().callHubDetailGetPropertyFromMasterToDetail(hub);
				og.objectsInternal().callObjectCallbackAddObjectCallbackChangeListeners(hx, hx.getObjectClass(), propx, null, this, true);
			}
		}

		HubProp hp = add(hub, prop, true, Type.ObjectCallbackEnabled, null, bAoOnly, "ObjectCallbackEnabled");
		//was: return add(hub, prop, true, Type.ObjectCallbackEnabled);
		return hp;
	}

	/**
	 * Adds a rule that checks object callback enabled status using a
	 * specific class and property path prefix.
	 *
	 * @param hub      the hub to evaluate
	 * @param cz       the class used for callback lookup
	 * @param prop     the property to check
	 * @param ppPrefix the property path prefix
	 * @return the created HubProp instance
	 */
	public HubProp addObjectCallbackEnabled(Hub hub, Class cz, String prop, String ppPrefix) {
		// ?? not used
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hub);
		og.objectsInternal().callObjectCallbackAddObjectCallbackChangeListeners(hub, cz, prop, ppPrefix, this, true);
		return add(hub, prop, true, Type.ObjectCallbackEnabled);
	}

	/**
	 * Adds a rule that checks whether the specified property is visible
	 * based on object callback rules.
	 *
	 * @param hub  the hub to evaluate
	 * @param prop the property to check
	 * @return the created HubProp instance
	 */
	public HubProp addObjectCallbackVisible(Hub hub, String prop) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hub);
		og.objectsInternal().callObjectCallbackAddObjectCallbackChangeListeners(hub, hub.getObjectClass(), prop, null, this, false);
		return add(hub, prop, true, Type.ObjectCallbackVisible);
	}

	/**
	 * Adds a rule that checks property visibility using the specified class
	 * and property path prefix.
	 *
	 * @param hub      the hub to evaluate
	 * @param cz       the class used for callback lookup
	 * @param prop     the property to check
	 * @param ppPrefix the property path prefix
	 * @return the created HubProp instance
	 */
	public HubProp addObjectCallbackVisible(Hub hub, Class cz, String prop, String ppPrefix) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hub);
		og.objectsInternal().callObjectCallbackAddObjectCallbackChangeListeners(hub, cz, prop, ppPrefix, this, false);

		// include master
		Hub hx = hub.getMasterHub();
		if (hx != null) {
			OALinkInfo li = og.hubsInternal().callHubDetailGetLinkInfoFromMasterObjectToDetail(hub);
			if (li != null && li.getOwner()) {
				String propx = og.hubsInternal().callHubDetailGetPropertyFromMasterToDetail(hub);
				og.objectsInternal().callObjectCallbackAddObjectCallbackChangeListeners(hx, hx.getObjectClass(), propx, null, this, false);
			}
		}

		return add(hub, prop, true, Type.ObjectCallbackVisible);
	}

	/**
	 * Adds a rule using the specified comparison type for the given hub.
	 *
	 * @param hub  the hub to evaluate
	 * @param type the comparison type
	 * @return the created HubProp instance
	 */
	public HubProp add(Hub hub, HubChangeListener.Type type) {
		return add(hub, null, (type == null ? false : true), type, null, (type == null ? true : type.bUseAoOnly), null);
	}

	/**
	 * Adds a rule for the specified hub and property using the provided
	 * comparison type.
	 *
	 * @param hub      the hub to evaluate
	 * @param property the property to monitor
	 * @param type     the comparison type
	 * @return the created HubProp instance
	 */
	public HubProp add(Hub hub, String property, HubChangeListener.Type type) {
		return add(hub, property, type == null ? false : true, type, null, (type == null ? true : type.bUseAoOnly), null);
	}

	/**
	 * Adds a rule for the specified hub and property path, using the given
	 * comparison value.
	 *
	 * @param hub          the hub to evaluate
	 * @param propertyPath the property to monitor
	 * @param compareValue the comparison value to apply
	 * @return the created HubProp instance
	 */
	public HubProp add(Hub hub, final String propertyPath, Object compareValue) {
		return add(hub, propertyPath, true, compareValue, null, true, null);
	}

	/**
	 * Adds a rule for the given hub using a custom filter to determine
	 * whether the rule evaluates to true.
	 *
	 * @param hub    the hub to evaluate
	 * @param filter the filter used for evaluation
	 * @return the created HubProp instance
	 */
	public HubProp add(Hub hub, OAFilter filter) {
		return add(hub, null, true, null, filter, true, "filter");
	}

	/**
	 * Adds a rule that uses a custom filter without requiring a hub.
	 *
	 * @param filter the filter used for evaluation
	 * @return the created HubProp instance
	 */
	public HubProp add(OAFilter filter) {
		return add(null, null, true, null, filter, true, "filter");
	}

	/**
	 * Adds a rule for the specified hub and property path with options for
	 * enabling comparison logic and providing a comparison value.
	 *
	 * @param hub              the hub to evaluate
	 * @param propertyPath     the property path to monitor
	 * @param bUseCompareValue true to use the comparison value
	 * @param compareValue     the value or rule used for comparison
	 * @return the created HubProp instance
	 */
	public HubProp add(Hub hub, final String propertyPath, boolean bUseCompareValue, Object compareValue) {
		Type type = null;
		if (bUseCompareValue && compareValue instanceof Type) {
			type = (Type) compareValue;
		}
		return this.add(hub, propertyPath, bUseCompareValue, compareValue, null, (type == null ? true : type.bUseAoOnly), null);
	}

	/**
	 * Adds a rule with full configuration for the hub, property path,
	 * comparison logic, optional filter, active-object-only behavior, and a
	 * description.
	 *
	 * @param hub              the hub to evaluate
	 * @param propertyPath     the property path to monitor
	 * @param bUseCompareValue true to use the comparison value
	 * @param compareValue     the comparison value or rule
	 * @param filter           an optional filter for evaluation
	 * @param bAoOnly          true to evaluate only the active object
	 * @param description      a description for the rule
	 * @return the created HubProp instance
	 */
	public HubProp add(Hub hub, final String propertyPath, boolean bUseCompareValue, Object compareValue, OAFilter filter,
			final boolean bAoOnly, String description) {
		String newPropertyPath;
		String[] props;

		if (bUseCompareValue && compareValue != null) {
			if (compareValue instanceof OANullObject) {
				compareValue = Type.PropertyNull;
			} else if (compareValue instanceof OANotNullObject) {
				compareValue = Type.PropertyNotNull;
			} else if (compareValue instanceof OAEmptyObject) {
				compareValue = Type.PropertyEmpty;
			} else if (compareValue instanceof OANotEmptyObject) {
				compareValue = Type.PropertyNotEmpty;
			} else if (compareValue instanceof OAAnyValueObject) {
				compareValue = Type.AlwaysTrue;
			}
		}

		if (propertyPath != null && propertyPath.indexOf('.') >= 0) {
			newPropertyPath = propertyPath.replace('.', '_');
			props = new String[] { propertyPath };
		} else {
			newPropertyPath = propertyPath;
			props = null;
		}

		final HubProp newHubProp = new HubProp(hub, propertyPath, newPropertyPath, props, bUseCompareValue, compareValue, filter, bAoOnly,
				description);

		// see if there is a listener with same hub - and one without a propertyName used
		for (HubProp hp : hubProps) {
			if (hp.equals(newHubProp)) {
				return null;
			}
		}

		if (bUseCompareValue && compareValue == Type.ObjectCallbackEnabled) {
			for (HubProp hp : hubProps) {
				if (hp.bUseCompareValue && hp.compareValue == Type.ObjectCallbackEnabled && hub == hp.hub) {
					if (OAString.isEmpty(hp.propertyPath)) {
						hp.bIgnore = true;
					} else {
						if (OAString.isEmpty(propertyPath)) {
							return null;
						}
						if (hp.propertyPath.equalsIgnoreCase(propertyPath)) {
							hp.bIgnore = true;
						}
					}
				}
			}
		}
		if (bUseCompareValue && compareValue == Type.ObjectCallbackVisible) {
			for (HubProp hp : hubProps) {
				if (hp.bUseCompareValue && hp.compareValue == Type.ObjectCallbackVisible && hub == hp.hub) {
					if (OAString.isEmpty(hp.propertyPath)) {
						hp.bIgnore = true;
					} else {
						if (OAString.isEmpty(propertyPath)) {
							return null;
						}
						if (hp.propertyPath.equalsIgnoreCase(propertyPath)) {
							hp.bIgnore = true;
						}
					}
				}
			}
		}

		assignHubListener(newHubProp);

		hubProps = (HubProp[]) OAArray.add(HubProp.class, hubProps, newHubProp);
		callOnChange();

		// 20220615 removed, calling code needs to make additional add(..) for link hub (if needed)
		/*
			Hub h = (hub == null) ? null : hub.getLinkHub(true);
			if (h != null) {
				if (HubLinkDelegate.isLinkAutoCreated(hub, true)) {
					// need to listen for AO changes, newList, etc from the linkTo Hub
					add(h, null, OAAnyValueObject.instance);
				} else {
					addHubValid(h);
				}
			}
		*/
		return newHubProp;
	}

	/**
	 * Determines whether this listener is configured to listen to the given
	 * hub, object instance, and property.
	 *
	 * @param hub      the hub to check
	 * @param object   the object associated with the event
	 * @param property the property name being evaluated
	 * @return true if this listener monitors the specified inputs
	 */
	public boolean isListeningTo(Hub hub, Object object, String property) {
		if (hub == null || object == null || property == null) {
			return false;
		}

		for (HubProp hp : hubProps) {
			if (hp.bIgnore) {
				continue;
			}
			if (hp.hub == null) {
				continue;
			}
			if (hp.hub != hub) {
				final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hub);
				if (!og.hubsInternal().callHubShareIsUsingSameSharedHub(hp.hub, hub)) {
					continue;
				}
			}
			if (!hp.bAoOnly || object == hp.hub.getAO()) {
				if (property.equalsIgnoreCase(hp.listenToPropertyName)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Assigns or reuses a hub listener for the given rule, ensuring
	 * appropriate event monitoring for the specified hub and property path.
	 *
	 * @param newHubProp the rule requiring a listener assignment
	 */
	protected void assignHubListener(final HubProp newHubProp) {
		// see if a new hubListener is needed
		for (HubProp hp : hubProps) {
			if (hp.bIgnore) {
				continue;
			}
			if (hp.hub != newHubProp.hub) {
				continue;
			}
			if (newHubProp.propertyPath != null) {
				if (!newHubProp.propertyPath.equalsIgnoreCase(hp.propertyPath)) {
					continue;
				}
			}
			newHubProp.hubListener = hp.hubListener;
			break;
		}

		if (newHubProp.hubListener != null) {
			return;
		}

		newHubProp.hubListener = new HubListenerAdapter() {
			public void afterChangeActiveObject(HubEvent e) {
				if (e == lastHubEvent) {
					return;
				}
				lastHubEvent = e;
				callOnChange();
			}

			@Override
			public void afterPropertyChange(HubEvent e) {
				if (e == lastHubEvent) {
					return;
				}
				lastHubEvent = e;
				if (isListeningTo(e.getHub(), e.getObject(), e.getPropertyName())) {
					callOnChange();
				}
			}

			// linked to hub listener
			@Override
			public void onNewList(HubEvent e) {
				if (e == lastHubEvent) {
					return;
				}
				lastHubEvent = e;
				callOnChange();
			}

			@Override
			public void afterAdd(HubEvent e) {
				if (e == lastHubEvent) {
					return;
				}
				lastHubEvent = e;
				for (HubProp hp : hubProps) {
					if (hp.bIgnore) {
						continue;
					}
					if (hp.hub != newHubProp.hub) {
						continue;
					}
					if (!hp.bAoOnly || hp.propertyPath == null) {
						callOnChange();
						break;
					}
				}
			}

			@Override
			public void afterInsert(HubEvent e) {
				if (e == lastHubEvent) {
					return;
				}
				lastHubEvent = e;
				for (HubProp hp : hubProps) {
					if (hp.bIgnore) {
						continue;
					}
					if (hp.hub != newHubProp.hub) {
						continue;
					}
					if (!hp.bAoOnly || hp.propertyPath == null) {
						callOnChange();
						break;
					}
				}
			}

			@Override
			public void afterRemove(HubEvent e) {
				if (e == lastHubEvent) {
					return;
				}
				lastHubEvent = e;
				for (HubProp hp : hubProps) {
					if (hp.bIgnore) {
						continue;
					}
					if (hp.hub != newHubProp.hub) {
						continue;
					}
					if (!hp.bAoOnly || hp.propertyPath == null) {
						callOnChange();
						break;
					}
				}
			}
		};

		if (newHubProp.hub != null) {
			if (newHubProp.props == null) {
				if (newHubProp.propertyPath == null) {
					newHubProp.hub.addHubListener(newHubProp.hubListener);
				} else {
					newHubProp.hub.addHubListener(newHubProp.hubListener, newHubProp.listenToPropertyName, newHubProp.bAoOnly);
				}
			} else {
				newHubProp.hub.addHubListener(	newHubProp.hubListener, newHubProp.listenToPropertyName, newHubProp.props,
												newHubProp.bAoOnly);
			}
		}
	}

	/**
	 * Clears all rules, closes all listeners, and resets the internal list
	 * of HubProp instances.
	 */
	public void clear() {
		close();
		hubProps = new HubProp[0];
	}

	/**
	 * Removes all assigned hub listeners from their associated hubs and
	 * clears references to those listeners.
	 */
	public void close() {
		for (HubProp hp : hubProps) {
			if (hp.hub != null && hp.hubListener != null) {
				hp.hub.removeHubListener(hp.hubListener);
				for (HubProp hpx : hubProps) {
					if (hpx.hubListener == hp.hubListener) {
						hpx.hubListener = null;
					}
				}
			}
		}
	}

	/**
	 * Removes all rules associated with the specified hub.
	 *
	 * @param hub the hub whose rules should be removed
	 */
	public void remove(Hub hub) {
		remove(hub, null);
	}

	/**
	 * Removes the rule associated with the specified hub and property path.
	 *
	 * @param hub  the hub to modify
	 * @param prop the property path for the rule to remove
	 */
	public void remove(Hub hub, String prop) {
		if (hub == null) {
			return;
		}
		for (HubProp hp : hubProps) {
			if (hp.hub != hub) {
				continue;
			}
			if (!OAString.equals(prop, hp.propertyPath)) {
				continue;
			}
			if (hp.hubListener == null) {
				continue;
			}

			boolean b = false;
			for (HubProp hpx : hubProps) {
				if (hpx == hp) {
					continue;
				}
				if (hpx.hubListener == hp.hubListener) {
					b = true;
					break;
				}
			}
			if (!b) {
				hp.hub.removeHubListener(hp.hubListener);
			}
			hp.hubListener = null;
			break;
		}
	}

	/**
	 * Removes the specified rule and detaches its listener if needed.
	 *
	 * @param hp the HubProp rule to remove
	 */
	public void remove(HubProp hp) {
		if (hp == null) {
			return;
		}
		remove(hp.hub, hp.propertyPath);
		hubProps = (HubProp[]) OAArray.removeValue(HubProp.class, hubProps, hp);
	}

	/**
	 * Evaluates all rules in sequence and returns true only if all rules
	 * evaluate to true. Populates the failure reason when a rule fails.
	 *
	 * @return true if all rules evaluate successfully; false otherwise
	 */
	public boolean getValue() {
		failureReason = null;
		boolean b = true;
		for (HubProp hp : hubProps) {
			if (hp.bIgnore) {
				continue;
			}
			if (hp.filter != null) {
				if (hp.hub == null) {
					b = hp.filter.isUsed(null);
				} else {
					b = hp.filter.isUsed(hp.hub.getAO());
				}
			} else {
				b = hp.getValue();
			}
			if (!b) {
				if (failureReason == null) {
					failureReason = hp.failureReason;
				}
				break;
			}
		}
		return b;
	}

	/**
	 * Builds a tooltip string containing the descriptions or comparison
	 * values for all active rules, and the failure reason if applicable.
	 *
	 * @return the tooltip text representing rule statuses
	 */
	public String getToolTipText() {
		String tt = "";
		for (HubProp hp : hubProps) {
			if (hp.bIgnore) {
				continue;
			}
			String s = hp.getToolTipText();
			if (s == null) {
				s = hp.description;
			}
			if (OAString.isNotEmpty(s)) {
				tt = OAString.append(tt, s, "<br>");
			}
		}
		tt = OAString.append(tt, failureReason, "<br>Reason: ");
		return tt;
	}

	/**
	 * Returns the most recent failure reason set during rule evaluation.
	 *
	 * @return the failure reason, or null if none exists
	 */
	public String getFailureReason() {
		return failureReason;
	}

	/**
	 * Returns the first HubProp rule that evaluates to false during
	 * validation, or null if all rules succeed.
	 *
	 * @return the first failing HubProp, or null
	 */
	public HubProp getFalseValue() {
		boolean b = true;
		for (HubProp hp : hubProps) {
			if (hp.bIgnore) {
				continue;
			}
			if (hp.filter != null) {
				if (hp.hub == null) {
					b = hp.filter.isUsed(null);
				} else {
					b = hp.filter.isUsed(hp.hub.getAO());
				}
			} else {
				b = hp.getValue();
			}
			if (!b) {
				return hp;
			}
		}
		return null;
	}

	/**
	 * Represents a single rule used by {@link HubChangeListener} to evaluate
	 * hub state, property values, filters, or object-callback conditions.
	 * Each instance defines how a specific hub or property should be monitored,
	 * including comparison logic, filters, listener configuration, and tooltip
	 * metadata. Instances are also responsible for tracking failure reasons and
	 * determining when a rule should be ignored due to overrides.
	 */
	public static class HubProp {

		/**
		 * Hub associated with this rule. Determines the source of active object,
		 * validity checks, and property evaluations.
		 */
		public Hub<?> hub;
		
		/**
		 * Original property path used for evaluation. May contain dotted paths that
		 * are expanded or normalized for listener resolution.
		 */
		public String propertyPath; // original propertyPath
		
		/**
		 * Property name used by HubListeners. Dotted paths are replaced with
		 * underscores to create a listener-friendly identifier.
		 */
		public String listenToPropertyName; // name used for listener - in case property path has '.' in it, then this will replace with '_'
		
		/**
		 * Expanded array of property paths when the original propertyPath contains
		 * dotted segments. Used to attach listeners to all dependent properties.
		 */
		public String[] props;
		
		/**
		 * Listener instance attached to the associated hub for monitoring relevant
		 * HubEvents that may trigger rule evaluation.
		 */
		public HubListener hubListener;
		
		/**
		 * Comparison value or rule used to evaluate the hub or property when
		 * bUseCompareValue is true.
		 */
		public Object compareValue;
		
		/**
		 * Flag indicating whether the compareValue field should participate in
		 * evaluation logic.
		 */
		public boolean bUseCompareValue;
		
		/**
		 * Optional filter applied to evaluate the rule. If present, the filter
		 * overrides comparison logic and is evaluated instead.
		 */
		public OAFilter filter;
		
		/**
		 * When true, rule evaluation is restricted to the active object of the hub
		 * rather than iterating through all objects.
		 */
		public boolean bAoOnly;
		
		/**
		 * Marks this rule as ignored when another rule overrides it. Ignored rules
		 * are skipped during evaluation.
		 */
		public boolean bIgnore; // flag used when another rule overrides this one
		
		/**
		 * Failure message specific to this rule instance, set when evaluation does
		 * not pass. Propagated up to the parent listener.
		 */
		public String failureReason;
		
		/**
		 * Optional descriptive label used for tooltips or UI reporting of this
		 * rule’s purpose.
		 */
		public String description;

		/**
		 * Constructs a HubProp instance that defines a single rule for evaluating
		 * hub state or property values. Initializes fields including the hub,
		 * property path, listener name, comparison settings, filter, and descriptive
		 * text.
		 *
		 * @param h                 the hub associated with this rule
		 * @param propertyPath      the original property path to evaluate
		 * @param listenPropertyName the property name used by hub listeners
		 * @param props             array of property paths when expanded from a dotted path
		 * @param bUseCompareValue  true to use the compareValue during evaluation
		 * @param compareValue      the comparison rule or value
		 * @param filter            optional filter used for evaluating the rule
		 * @param bAoOnly           true to evaluate only the active object
		 * @param description       descriptive label for this rule
		 */
		public HubProp(Hub<?> h, String propertyPath, String listenPropertyName, String[] props, boolean bUseCompareValue,
				Object compareValue, OAFilter filter, boolean bAoOnly, String description) {
			this.hub = h;
			this.propertyPath = propertyPath;
			this.listenToPropertyName = listenPropertyName;
			this.props = props;
			this.bUseCompareValue = bUseCompareValue;
			this.compareValue = compareValue;
			this.filter = filter;
			this.bAoOnly = bAoOnly;
			this.description = description;
		}

		/**
		 * Evaluates this rule against the associated hub and property. Depending
		 * on the configured comparison value, filter, and flags, the method checks
		 * hub validity, active object state, property values, or object-callback
		 * results. Updates the failureReason field when evaluation fails.
		 *
		 * @return true if the rule evaluates successfully; false otherwise
		 */
		public boolean getValue() {
			failureReason = null;
			boolean bValid = hub != null && hub.isValid();
			if (bUseCompareValue && compareValue != null) {
				if (compareValue == Type.HubValid) {
					return bValid;
				}
				if (compareValue == Type.HubNotValid) {
					return !bValid;
				}
				if (compareValue == Type.HubEmpty) {
					return (bValid && hub.getSize() == 0);
				}
				if (compareValue == Type.HubNotEmpty) {
					return (bValid && hub.getSize() > 0);
				}
				if (compareValue == Type.AoNull) {
					return (bValid && hub.getAO() == null);
				}
				if (compareValue == Type.AoNotNull) {
					return (bValid && hub.getAO() != null);
				}
				if (compareValue == Type.AoNew) {
					Object objx = hub.getAO();
					return (bValid && (objx instanceof OAObject) && ((OAObject) objx).isNew());
				}
				if (compareValue == Type.AoNotNew) {
					Object objx = hub.getAO();
					return (bValid && (objx instanceof OAObject) && !((OAObject) objx).isNew());
				}
				if (compareValue == Type.AlwaysTrue) {
					return true;
				}
				if (compareValue == Type.AlwaysFalse) {
					failureReason = "always false";
					return false;
				}
				if (compareValue == Type.OnlySuperAdmin) {
					OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hub);
					if (og.context().isSuperAdmin()) {
						return true;
					}
					failureReason = "only SuperAdmin";
					return false;
				}

				if (compareValue == Type.Unknown) {
					return true;
				}
			}

			Object value = (bValid) ? hub.getAO() : null;
			// 20190203 if !bAoOnly, then check all objects
			if (compareValue == Type.ObjectCallbackEnabled) {
				if (!bValid) {
					return false;
				}
				if (value != null && !(value instanceof OAObject)) {
					return true;
				}

				boolean b = false;
				for (int i = 0;; i++) {
					if (!bAoOnly) {
						value = hub.getAt(i);
						if (!(value instanceof OAObject)) {
							break;
						}
					}

					final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hub);
					
					Hub<OAObject> hubx = (Hub) hub;
				    OAObject valuex = (OAObject) value;
					
					OAObjectCallback eq = og.objectsInternal().callObjectCallbackGetAllowEnabledObjectCallback(	OAObjectCallback.CHECK_ALL, hubx, valuex, propertyPath);
					b = eq.getAllowed();
					if (!b) {
						failureReason = eq.getDisplayResponse();
						if (OAString.isEmpty(failureReason)) {
							failureReason = "edit query returned false";
						}
						break;
					}
					if (bAoOnly) {
						break;
					}
				}
				return b;

				/*was:
				OAObjectCallback eq  = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(hub, (OAObject) value, propertyPath, true);
				boolean b = eq.getAllowed();
				if (!b) {
				    failureReason = eq.getDisplayResponse();
				    if (OAString.isEmpty(failureReason)) failureReason = "edit query returned false";
				}
				return b;
				*/
			}
			if (compareValue == Type.ObjectCallbackVisible) {
				if (!bValid) {
					return true;
				}
				if (value != null && !(value instanceof OAObject)) {
					if (hub == null) {
						return true;
					}
					return true; //qqqqqqqqqqq needs to be done
					/*qqqqqqq was:
					Class cx = hub.getObjectClass();
					if (!OAObject.class.isAssignableFrom(cx)) return true;
					OAObjectCallback eq = OAObjectCallbackDelegate.getAllowVisibleObjectCallback(cx, propertyPath);
					boolean b = eq.getAllowed();
					if (!b) failureReason = eq.getDisplayResponse();
					return b;
					*/
				}
				final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hub);
				
				Hub<OAObject> hubx = (Hub) hub;
			    OAObject valuex = (OAObject) value;

				OAObjectCallback eq = og.objectsInternal().callObjectCallbackGetAllowVisibleObjectCallback(hubx, valuex, propertyPath);
				boolean b = eq.getAllowed();
				if (!b) {
					failureReason = eq.getDisplayResponse();
					if (OAString.isEmpty(failureReason)) {
						failureReason = "edit query returned false";
					}
				}
				return b;
			}

			if (!bValid) {
				return false;
			}

			if (propertyPath != null) {
				if (value instanceof OAObject) {
					value = ((OAObject) value).getProperty(propertyPath);
				}
			}

			boolean b;
			if (bUseCompareValue && compareValue != null) {
				if (compareValue == Type.PropertyNull || (compareValue instanceof OANullObject)) {
					b = (hub != null && hub.getAO() != null && value == null);
					if (!b) {
						failureReason = "compare != null";
					}
					return b;
				}
				if (compareValue == Type.PropertyNotNull || (compareValue instanceof OANotNullObject)) {
					b = (value != null);
					if (!b) {
						failureReason = "compare == null";
					}
					return b;
				}

				//qqqqqqqqqq			qqqqqqqqqqqqqqqqq
				if (compareValue == Type.PropertyEmpty) {
					b = OAString.isEmpty(value);
					if (!b) {
						failureReason = "compare PropertyEmpty=false";
					}
					return b;
				}
				if (compareValue == Type.PropertyNotEmpty) {
					b = OAString.isNotEmpty(value);
					if (!b) {
						failureReason = "compare PropertyNotEmpty=false";
					}
					return b;
				}
			}
			if (bUseCompareValue) {
				b = OACompare.compare(compareValue, value) == 0;
			} else {
				b = OAConv.toBoolean(value);
			}
			if (!b) {
				failureReason = "compare value did not match";
			}
			return b;
		}

		/**
		 * Returns a tooltip string describing this rule, including comparison
		 * information when a non-default comparison value is used. Returns null
		 * when the rule is ignored.
		 *
		 * @return formatted tooltip text, or null if ignored
		 */
		public String getToolTipText() {
			if (bIgnore) {
				return null;
			}
			String tt = null;
			/*
			if (compareValue instanceof Type) {
			    tt = OAString.append(tt, compareValue.toString(), "<br>");
			}
			else if (compareValue == Type.ObjectCallbackEnabled) {
			    tt = OAString.append(tt, "objectCallbackEnabled", "<br>");
			}
			else if (compareValue == Type.ObjectCallbackVisible) {
			    tt = OAString.append(tt, "objectCallbackVisible", "<br>");
			}
			*/
			if (bUseCompareValue && compareValue != null && compareValue != Type.AlwaysTrue) {
				tt = OAString.append(tt, "compareValue=" + compareValue, "<br>");
			}
			//todo:  see if you can figure out if editQueries exists or not
			return tt;
		}

		/**
		 * Compares this rule with another for equality. Two rules are equal when
		 * they reference the same hub, use the same comparison settings, and have
		 * matching property paths and compare values.
		 *
		 * @param obj the object to compare with
		 * @return true if both objects represent the same rule; false otherwise
		 */
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof HubProp)) {
				return false;
			}
			HubProp hp = (HubProp) obj;
			if (this.hub != hp.hub) {
				return false;
			}
			if (this.bUseCompareValue != hp.bUseCompareValue) {
				return false;
			}

			if (this.compareValue != null) {
				if (hp.compareValue == null) {
					return false;
				}
				if (OACompare.compare(this.compareValue, hp.compareValue) != 0) {
					return false;
				}
				/*was:
				if (!this.compareValue.equals(hp.compareValue)) {
					if (!this.compareValue.equals(OAConv.convert(this.compareValue.getClass(), hp.compareValue))) {
						return false;
					}
				}
				*/
				//
			} else if (hp.compareValue != null) {
				return false;
			}

			if (this.propertyPath != null) {
				if (hp.propertyPath == null) {
					return false;
				}
				if (!this.propertyPath.equalsIgnoreCase(hp.propertyPath)) {
					return false;
				}
			} else if (hp.propertyPath != null) {
				return false;
			}
			return true;
		}

		/**
		 * Returns a hash code based on the associated hub when available.
		 *
		 * @return the hash code for this rule
		 */
		@Override
		public int hashCode() {
			if (hub == null) {
				return super.hashCode();
			}
			return hub.hashCode();
		}
	}

	/**
	 * Optional list of chained HubChangeListeners. After this listener fires
	 * its onChange callback, each chained listener is invoked in sequence.
	 */
	protected ArrayList<HubChangeListener> alHubChangeListener;

	/**
	 * Adds a chained HubChangeListener that will be invoked after this
	 * listener detects a change.
	 *
	 * @param hcl the listener to add
	 */
	public void addHubChangeListener(HubChangeListener hcl) {
		if (hcl == null) {
			return;
		}

		if (alHubChangeListener == null) {
			alHubChangeListener = new ArrayList<>();
		}
		alHubChangeListener.add(hcl);
		hcl.onChange();
	}

	/**
	 * Invokes this listener's onChange method and then cascades the
	 * notification to any chained listeners.
	 */
	protected void callOnChange() {
		onChange();
		if (alHubChangeListener == null) {
			return;
		}
		for (HubChangeListener hcl : alHubChangeListener) {
			hcl.callOnChange();
		}
	}

	/**
	 * Callback invoked whenever monitored hub or property changes trigger a
	 * reevaluation. Subclasses must implement this method.
	 */
	protected abstract void onChange();
}
