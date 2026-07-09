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
package com.viaoa.callback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import com.viaoa.converter.OAConv;
import com.viaoa.hub.Hub;
import com.viaoa.lang.OAString;
import com.viaoa.object.OAObject;

/**
 * Carrier object for interactive edit/query callbacks on {@link OAObject} instances.
 * <p>
 * An {@code OAObjectCallback} represents a request to determine whether an action
 * is permitted, visible, or requires confirmation, or to supply additional UI data
 * such as formatting or tooltips. It is used as a shared contract between:
 * <ul>
 *   <li>OAObject callback methods (model rules)</li>
 *   <li>Hub listeners (contextual rules)</li>
 *   <li>Controller/UI code (visual and interaction behavior)</li>
 * </ul>
 *
 * <h3>Primary Responsibilities</h3>
 * <ul>
 *   <li>Report if an edit operation is <b>allowed</b> or must be <b>blocked</b></li>
 *   <li>Provide <b>confirm messages</b> and <b>tooltips</b> for UI presentation</li>
 *   <li>Propagate <b>context</b>: hub, owning object, property, new/old values</li>
 *   <li>Provide <b>optional response</b> or <b>throwable</b> when rules fail</li>
 *   <li>Support UI result control such as format or label customization</li>
 * </ul>
 *
 * <h3>Callback Types</h3>
 * Uses {@link Type} to describe the semantic request, including:
 * <ul>
 *   <li>Availability (AllowNew/Add/Delete/Visible/Enabled)</li>
 *   <li>Verification (VerifyPropertyChange/Add/Delete/Save/...)</li>
 *   <li>UI behavior (GetToolTip / RenderLabel / GetFormat)</li>
 *   <li>Copy operations (GetCopy / AfterCopy)</li>
 *   <li>Confirmation requests (SetConfirmFor*)</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * Instances are created and processed by {@link OAObjectCallbackDelegate} and never
 * directly by application code. Domain objects may implement {@code callback*}
 * methods to participate in rule enforcement and UI interaction.
 *
 * @see OAObjectCallbackDelegate
 * @see com.viaoa.annotation.OAObjCallback
 */
public class OAObjectCallback {
	static final long serialVersionUID = 1L;

	/**
	 * The Hub associated with this callback, providing contextual information
	 * about the collection or owner from which the callback originated.
	 */
	private Hub<?> hub;

	/**
	 * The target OAObject associated with this callback, representing the
	 * domain object being evaluated or modified.
	 */
	private OAObject object;

	/**
	 * The semantic callback type indicating what rule, action, or UI behavior
	 * is being evaluated. Defaults to {@link Type#Unknown}.
	 */
	private Type type = Type.Unknown;

	/**
	 * Optional list of the CheckTypes to perform, given the Type.checkType list
	 */
	private final EnumSet<CheckType> hmOnlyCheckTypes;
	
	/**
	 * Optional confirmation dialog title used when a callback type requires
	 * user confirmation before proceeding.
	 */
	private String confirmTitle; // allow interaction with UI to have user confirm before continuing

	/**
	 * Optional confirmation message presented to the user for confirmation-type
	 * callback requests.
	 */
	private String confirmMessage; // message to use for confirming

	/**
	 * Tooltip text supplied by callback logic for UI presentation.
	 */
	private String toolTip;

	/**
	 * Optional formatting string used to control UI formatting based on the
	 * callback type.
	 */
	private String format; // allows creating customized formatter

	/**
	 * Indicates whether the callback action is currently permitted. Defaults
	 * to {@code true}.
	 */
	private boolean allowed = true; // flag to know if the type of objectCallback is permitted

	/**
	 * The callback value whose meaning is determined by the {@link Type}.
	 * For example, new value, added object, removed object, or other contextual
	 * data.
	 */
	private Object value; // depends on Type

	/**
	 * Optional Swing label used for UI-related callback types that allow
	 * label configuration or rendering customization.
	 */
	private OACallbackLabel label; // used for UI rendering control

	/**
	 * Optional response message assigned by callback logic, typically returned
	 * to UI or controller code for display or further handling.
	 */
	private String response; // used to give message back to caller

	/**
	 * Optional throwable assigned when callback logic determines that an action
	 * should fail and propagate an exception back to the caller.
	 */
	private Throwable throwable; // used to tell the caller to throw this exception and not to allow further processing.

	/**
	 * The property name associated with this callback, used primarily for
	 * verification or confirmation-type callbacks.
	 */
	private String propertyName;

	/**
	 * The previous value associated with a property-change callback. Used for
	 * comparison logic in validation operations.
	 */
	private Object oldValue;

	/**
	 * Indicates whether this callback has been acknowledged by the caller,
	 * allowing higher-level logic to know the request was processed.
	 */
	private boolean acknownledged;

	/**
	 * Explicit class reference used to resolve the effective class for this
	 * callback when provided. Otherwise fallback rules apply.
	 */
	private Class<? extends OAObject> clazz;

	
	/**
	 * Creates a callback with the specified {@link Type}.
	 * Initializes the callback with the given type and leaves all
	 * other fields at their default values.
	 *
	 * @param type the callback type to assign
	 */
	public OAObjectCallback(Type type) {
		if (type == null) type = Type.Unknown;
		this.type = type;
		this.hmOnlyCheckTypes = null;
	}

	/**
	 * Creates a callback with full context information including type,
	 * check flags, hub, class, target object, property name, and value.
	 * All supplied fields are assigned directly, and the callback is
	 * initialized as allowed.
	 *
	 * @param type the callback type to assign
	 * @param onlyCheckTypes, if populated then these are the only CheckTypes to use (ignore others in Type.checkType list)
	 * @param hub the associated hub, or {@code null}
	 * @param clazz an explicit class to associate, or {@code null}
	 * @param oaObj the target {@link OAObject}, or {@code null}
	 * @param propertyName the related property name, or {@code null}
	 * @param value the callback value, interpreted according to the type
	 */
	public OAObjectCallback(Type type, CheckType[] onlyCheckTypes, Hub<?> hub, Class<? extends OAObject> clazz, OAObject oaObj, String propertyName, Object value) {
		if (type == null) type = Type.Unknown;
		this.type = type;
		this.hmOnlyCheckTypes = (onlyCheckTypes == null) ? null : EnumSet.copyOf(Arrays.asList(onlyCheckTypes));
		this.hub = hub;
		this.clazz = clazz;
		this.object = oaObj;
		this.propertyName = propertyName;
		this.value = value;
		this.allowed = true;
	}

	public OAObjectCallback(Type type, Hub<?> hub, Class<? extends OAObject> clazz, OAObject oaObj) {
		this(type, (CheckType[]) null, hub, clazz, oaObj, null, null);
	}
	
	public OAObjectCallback(Type type, Hub<?> hub, Class<? extends OAObject> clazz, OAObject oaObj, String propertyName, Object value) {
		this(type, (CheckType[]) null, hub, clazz, oaObj, propertyName, value);
	}

	public OAObjectCallback(Type type, CheckType onlyCheckType, Hub<?> hub, Class<? extends OAObject> clazz, OAObject oaObj, String propertyName, Object value) {
		this(type, onlyCheckType == null ? null : new CheckType[] {onlyCheckType}, 
			hub, clazz, oaObj, propertyName, value);
	}
	
	/**
	 * Creates a callback by copying contextual information from an
	 * existing callback. The new instance uses the specified type
	 * and check flags, while hub, class, object, property name,
	 * value, and allowed state are copied from the source.
	 *
	 * @param type the callback type to assign
	 * @param onlyCheckTypes, if populated then these are the only CheckTypes to use (ignore others in Type.checkType list)
	 * @param eq the source callback to copy values from
	 */
	public OAObjectCallback(Type type, CheckType[] onlyCheckTypes, OAObjectCallback eq) {
		if (type == null) type = Type.Unknown;
		this.type = type;
		this.hmOnlyCheckTypes = (onlyCheckTypes == null || onlyCheckTypes.length == 0) ? null : EnumSet.copyOf(Arrays.asList(onlyCheckTypes));

		if (eq == null) {
			return;
		}
		this.hub = eq.getHub();
		this.clazz = eq.getCalcClass();
		this.object = eq.getObject();
		this.propertyName = eq.getPropertyName();
		this.value = eq.getValue();
		this.allowed = eq.getAllowed();
	}

	
	/**
	 * Defines the semantic category of an {@link OAObjectCallback} request.
	 * <p>
	 * Each enum value determines what kind of rule, UI behavior, confirmation,
	 * or verification is being performed, and also encodes whether the callback
	 * should evaluate owner/parent objects and whether it should perform an
	 * “enabled-first” check prior to running other logic.
	 *
	 * <p>The constructor arguments set two internal behavioral flags:</p>
	 * <ul>
	 *   <li><b>checkOwner</b> — whether owner/parent objects should be evaluated</li>
	 *   <li><b>checkEnabledFirst</b> — whether enabled-state rules are evaluated
	 *       before invoking callback logic</li>
	 * </ul>
	 *
	 * <p>Callback categories include:</p>
	 * <ul>
	 *   <li><b>Allow*</b> — determine whether an action is permitted</li>
	 *   <li><b>Verify*</b> — validate an edit operation based on new/old values</li>
	 *   <li><b>SetConfirmFor*</b> — define confirmation messages and titles</li>
	 *   <li><b>Get*</b> — supply tooltip, format, label, or copy behavior</li>
	 * </ul>
	 *
	 * <p>Values without arguments default to {@code checkOwner = false}
	 * and {@code checkEnabledFirst = false}.</p>
	 */
	public enum Type { // the policy descriptor
		
	    /**
	     * Indicates an unspecified or uninitialized callback type.
	     * Performs no owner or enabled-first evaluation.
	     */
		Unknown(new CheckType[] {}, new CategoryType[0]),

	    /**
	     * Determines whether a target object or property is enabled.
	     * Also invoked for types that have {@code checkEnabledFirst = true}.
	     * Includes owner checks but does not perform enabled-first evaluation.
	     */
		AllowEnabled( 
			new CheckType[] { 
				CheckType.Owner, CheckType.Processed, CheckType.Enabled, CheckType.UserEnabled, 
				CheckType.SessionEnabled, CheckType.HubListeners, CheckType.SuperAdminOverride, CheckType.CallbackMethod
			},
			new CategoryType [] { 
				CategoryType.PermissionGate, 
				CategoryType.CanSetResponse
			}
		), 

	    /**
	     * Determines whether an object, property, link, or method is visible.
	     * Performs owner checks. Does not perform enabled-first evaluation.
	     */
		AllowVisible(
			new CheckType[] {
				CheckType.Owner, CheckType.Visible, CheckType.UserVisible, 
				CheckType.SessionVisible, CheckType.HubListeners, CheckType.SuperAdminOverride, CheckType.CallbackMethod
			},
			new CategoryType [] { 
				CategoryType.PermissionGate, CategoryType.CanSetResponse
			}
		),

	    /**
	     * Determines whether creation of a new object is permitted.
	     * Performs owner checks and evaluates enabled-first rules.
	     */
		AllowNew(
			new CheckType[] { 
				CheckType.Owner, CheckType.Processed, CheckType.Enabled, CheckType.UserEnabled, 
				CheckType.SessionEnabled, CheckType.HubListeners, CheckType.SuperAdminOverride, CheckType.CallbackMethod
			},
			new CategoryType [] { 
				CategoryType.PermissionGate, CategoryType.MutationPermission, CategoryType.ObjectLifecycleOperation,
				CategoryType.CanSetResponse
			}
		),

	    /**
	     * Determines whether an object can be added to a hub.
	     * Performs owner checks and evaluates enabled-first rules.
	     */
		AllowAdd( 
			new CheckType[] {
				CheckType.Owner, CheckType.Processed, CheckType.Enabled, CheckType.UserEnabled, 
				CheckType.SessionEnabled, CheckType.HubListeners, CheckType.SuperAdminOverride, CheckType.CallbackMethod
			},
			new CategoryType [] { 
				CategoryType.PermissionGate, CategoryType.MutationPermission, 
				CategoryType.HubOperation,
				CategoryType.CanSetResponse
			}
		),
		
	    /**
	     * Determines whether an object can be removed from a hub.
	     * Performs owner checks and evaluates enabled-first rules.
	     */
		AllowRemove(
			new CheckType[] {
				CheckType.Owner, CheckType.Processed, CheckType.Enabled, CheckType.UserEnabled, 
				CheckType.SessionEnabled, CheckType.HubListeners, CheckType.SuperAdminOverride, CheckType.CallbackMethod
			},
			new CategoryType [] { 
				CategoryType.PermissionGate, CategoryType.MutationPermission, 
				CategoryType.HubOperation,
				CategoryType.CanSetResponse
			}
		),

	    /**
	     * Determines whether all items may be removed from a hub.
	     * Performs owner checks and evaluates enabled-first rules.
	     */
		AllowRemoveAll( 
			new CheckType[] {
				CheckType.Owner, CheckType.Processed, CheckType.Enabled, CheckType.UserEnabled, 
				CheckType.SessionEnabled, CheckType.HubListeners, CheckType.SuperAdminOverride, CheckType.CallbackMethod
			},
			new CategoryType [] { 
				CategoryType.PermissionGate, CategoryType.MutationPermission, 
				CategoryType.HubOperation,
				CategoryType.CanSetResponse
			}
		),

	    /**
	     * Determines whether deletion of an object is permitted.
	     * Performs owner checks and evaluates enabled-first rules.
	     */
		AllowDelete( 
			new CheckType[] {
				CheckType.Owner, CheckType.Processed, CheckType.Enabled, CheckType.UserEnabled, 
				CheckType.SessionEnabled, CheckType.HubListeners, CheckType.SuperAdminOverride, CheckType.CallbackMethod
			},
			new CategoryType [] { 
				CategoryType.PermissionGate, CategoryType.MutationPermission, CategoryType.ObjectLifecycleOperation,
				CategoryType.CanSetResponse
			}
		),

	    /**
	     * Determines whether saving an object is permitted.
	     * Does not evaluate owner or enabled-first rules.
	     */
		AllowSave(
			new CheckType[] {
				CheckType.Processed, CheckType.Enabled, CheckType.UserEnabled, 
				CheckType.SessionEnabled, CheckType.HubListeners, CheckType.SuperAdminOverride, CheckType.CallbackMethod
			},
			new CategoryType [] { 
				CategoryType.PermissionGate, CategoryType.ObjectLifecycleOperation,
				CategoryType.CanSetResponse
			}
		),

	    /**
	     * Determines whether an object copy operation is permitted.
	     * No owner or enabled-first evaluation.
	     */
		AllowCopy( 
			new CheckType[] {
				CheckType.Processed, CheckType.Enabled, CheckType.UserEnabled, 
				CheckType.SuperAdminOverride, CheckType.CallbackMethod
			},
			new CategoryType [] { 
				CategoryType.PermissionGate,
				CategoryType.CopyLifecycle,
				CategoryType.CanSetResponse
			}
		),

	    /**
	     * Validates that an object’s required values and rules are satisfied
	     * before submitting. No owner or enabled-first evaluation.
	     */
		AllowSubmit( 
			new CheckType[] {
				CheckType.Owner, CheckType.HubListeners, CheckType.CallbackMethod
			},
			new CategoryType [] { 
				CategoryType.PermissionGate, CategoryType.ValidationGate, CategoryType.ObjectLifecycleOperation, CategoryType.CanSetResponse
			}
		),

		
		
	    /**
	     * Verifies whether a property change is valid.
	     * Checks new and old values, and may block the action using
	     * {@code allowed = false} or a {@code throwable}.
	     * Performs owner checks and evaluates enabled-first rules.
	     */
		VerifyPropertyChange( 
			new CheckType[] {
				CheckType.Owner, CheckType.Processed, CheckType.Enabled, CheckType.UserEnabled, 
				CheckType.SessionEnabled, CheckType.HubListeners, CheckType.SuperAdminOverride, CheckType.CallbackMethod
			},
			new CategoryType [] { 
				CategoryType.ValidationGate, CategoryType.ObjectLifecycleOperation,
				CategoryType.CanSetResponse
			}
		), 

	    /**
	     * Verifies whether adding an object to a hub is valid.
	     * Uses the callback value as the object being added.
	     * Performs owner checks and evaluates enabled-first rules.
	     */
		VerifyAdd( 
			new CheckType[] {
				CheckType.Owner, CheckType.Processed, CheckType.Enabled, CheckType.UserEnabled, 
				CheckType.SessionEnabled, CheckType.HubListeners, CheckType.SuperAdminOverride, CheckType.CallbackMethod
			},
			new CategoryType [] { 
				CategoryType.ValidationGate, CategoryType.ObjectLifecycleOperation,
				CategoryType.HubOperation,
				CategoryType.CanSetResponse
			}
		),

	    /**
	     * Verifies whether removing an object from a hub is valid.
	     * Uses the callback value as the object being removed.
	     * Performs owner checks and evaluates enabled-first rules.
	     */
		VerifyRemove(
			new CheckType[] {
				CheckType.Owner, CheckType.Processed, CheckType.Enabled, CheckType.UserEnabled, 
				CheckType.SessionEnabled, CheckType.HubListeners, CheckType.SuperAdminOverride, CheckType.CallbackMethod
			},
			new CategoryType [] { 
				CategoryType.ValidationGate, CategoryType.ObjectLifecycleOperation,
				CategoryType.HubOperation,
				CategoryType.CanSetResponse
			}
		), 

	    /**
	     * Verifies whether removing all objects from a hub is valid.
	     * Performs owner checks and evaluates enabled-first rules.
	     */
		VerifyRemoveAll( 
			new CheckType[] {
				CheckType.Owner, CheckType.Processed, CheckType.Enabled, CheckType.UserEnabled, 
				CheckType.SessionEnabled, CheckType.HubListeners, CheckType.SuperAdminOverride, CheckType.CallbackMethod
			},
			new CategoryType [] { 
				CategoryType.ValidationGate, CategoryType.ObjectLifecycleOperation,
				CategoryType.HubOperation,
				CategoryType.CanSetResponse
			}
		), 

	    /**
	     * Verifies whether deleting an object is valid.
	     * Uses the callback value as the deleted object.
	     * Performs owner checks and evaluates enabled-first rules.
	     */
		VerifyDelete(
			new CheckType[] {
				CheckType.Owner, CheckType.Processed, CheckType.Enabled, CheckType.UserEnabled, 
				CheckType.SessionEnabled, CheckType.HubListeners, CheckType.SuperAdminOverride, CheckType.CallbackMethod
			},
			new CategoryType [] { 
				CategoryType.ValidationGate, CategoryType.ObjectLifecycleOperation,
				CategoryType.HubOperation,
				CategoryType.CanSetResponse
			}
		),

	    /**
	     * Verifies whether saving an object is valid.
	     * Does not perform owner.
	     */
		VerifySave( 
			new CheckType[] {
				CheckType.Processed, CheckType.Enabled, CheckType.UserEnabled, 
				CheckType.SessionEnabled, CheckType.HubListeners, CheckType.SuperAdminOverride, CheckType.CallbackMethod
			},
			new CategoryType [] { 
				CategoryType.ValidationGate, CategoryType.ObjectLifecycleOperation,
				CategoryType.CanSetResponse
			}
		), 

	    /**
	     * Verifies whether invoking a command/method is valid.
	     * Performs owner checks and evaluates enabled-first rules.
	     */
		VerifyCommand( 
			new CheckType[] {
				CheckType.Owner, CheckType.Processed, CheckType.Enabled, CheckType.UserEnabled, 
				CheckType.SessionEnabled, CheckType.HubListeners, CheckType.SuperAdminOverride, CheckType.CallbackMethod
			},
			new CategoryType [] { 
				CategoryType.ValidationGate, 
				CategoryType.CanSetResponse
			}
		),

	    /**
	     * Supplies copy behavior. Callback may set allowed,
	     * supply a replacement value, or allow default copy logic.
	     * No owner or enabled-first evaluation.
	     */
		GetCopy( 
			new CheckType[] {
				CheckType.HubListeners, CheckType.CallbackMethod
			},
			new CategoryType [] { 
				CategoryType.CopyLifecycle
			}
		),

	    /**
	     * Invoked after an object copy has been created.
	     * The callback value contains the new object.
	     * No owner or enabled-first evaluation.
	     */
		AfterCopy(
			new CheckType[] {
				CheckType.CallbackMethod
			},
			new CategoryType [] { 
				CategoryType.CopyLifecycle
			}
		), 

	    /**
	     * Supplies confirmation title and message for a property change.
	     * No owner or enabled-first evaluation.
	     */
		SetConfirmForPropertyChange(
			new CheckType[] {
				CheckType.CallbackMethod
			},
			new CategoryType [] { 
				CategoryType.Confirmation
			}
		),

	    /**
	     * Supplies confirmation title and message for an add operation.
	     * No owner or enabled-first evaluation.
	     */
		SetConfirmForAdd(
			new CheckType[] {
				CheckType.CallbackMethod
			},
			new CategoryType [] { 
				CategoryType.Confirmation
			}
		),

	    /**
	     * Supplies confirmation title and message for a remove operation.
	     * No owner or enabled-first evaluation.
	     */
		SetConfirmForRemove(
			new CheckType[] {
				CheckType.CallbackMethod
			},
			new CategoryType [] { 
				CategoryType.Confirmation
			}
		),

	    /**
	     * Supplies confirmation title and message for a remove-all operation.
	     * No owner or enabled-first evaluation.
	     */
		SetConfirmForRemoveAll(
			new CheckType[] {
				CheckType.CallbackMethod
			},
			new CategoryType [] { 
				CategoryType.Confirmation
			}
		),

	    /**
	     * Supplies confirmation title and message for a delete operation.
	     * No owner or enabled-first evaluation.
	     */
		SetConfirmForDelete(
			new CheckType[] {
				CheckType.CallbackMethod
			},
			new CategoryType [] { 
				CategoryType.Confirmation
			}
		),

	    /**
	     * Supplies confirmation title and message for a save operation.
	     * No owner or enabled-first evaluation.
	     */
		SetConfirmForSave(
			new CheckType[] {
				CheckType.CallbackMethod
			},
			new CategoryType [] { 
				CategoryType.Confirmation
			}
		),

	    /**
	     * Supplies confirmation title and message for a command/method invocation.
	     * No owner or enabled-first evaluation.
	     */
		SetConfirmForCommand(
			new CheckType[] {
				CheckType.CallbackMethod
			},
			new CategoryType [] { 
				CategoryType.Confirmation
			}
		),

	    /**
	     * Supplies a tooltip string for UI presentation.
	     * Uses the callback’s toolTip field.
	     */
		GetToolTip(
			new CheckType[] {
				CheckType.CallbackMethod
			},
			new CategoryType [] { 
				CategoryType.UiOnly
			}
		),

	    /**
	     * Allows callback logic to update the label used to render a UI component.
	     * Uses the callback’s label field.
	     */
		RenderLabel(
			new CheckType[] {
				CheckType.CallbackMethod
			},
			new CategoryType [] { 
				CategoryType.UiOnly
			}
		),

	    /**
	     * Updates the label belonging to a UI component.
	     * Uses the callback’s label field.
	     */
		UpdateLabel(
			new CheckType[] {
				CheckType.CallbackMethod
			},
			new CategoryType [] { 
				CategoryType.UiOnly
			}
		),
		
	    /**
	     * Supplies a format string used to customize UI formatting.
	     * Uses the callback’s format field.
	     */
		GetFormat(
			new CheckType[] {
				CheckType.CallbackMethod
			},
			new CategoryType [] { 
				CategoryType.UiOnly
			}
		);
		
		private final EnumSet<CategoryType> categoryTypes;
		private final EnumSet<CheckType> checkTypes;

		Type(CheckType[] checkTypes, CategoryType... cts) {
			this.checkTypes = checkTypes.length == 0
			    ? EnumSet.noneOf(CheckType.class)
			    : EnumSet.copyOf(Arrays.asList(checkTypes));
			
			this.categoryTypes = cts.length == 0
		        ? EnumSet.noneOf(CategoryType.class)
		        : EnumSet.copyOf(Arrays.asList(cts));
		}

		public boolean has(CategoryType flag) {
		    return categoryTypes.contains(flag);
		}
		public boolean has(CheckType flag) {
		    return checkTypes.contains(flag);
		}
	}

	// helps document 
	public enum CategoryType {
	    PermissionGate, // This type answers whether an action/state is permitted. 
	    ValidationGate, // This type validates data/action integrity.
	    UiOnly, // This type only affects presentation/interaction text, label, format, tooltip, confirmation.
	    Confirmation, // This type supplies confirmation text/title.
	    CopyLifecycle, // This type participates in copy flow.
	    MutationPermission, // This permission type allows a model-changing action.
	    HubOperation, // This type is inherently about Hub membership.
	    ObjectLifecycleOperation, // This type concerns object lifecycle: new/save/delete/submit/copy.
	    CanSetResponse,  // Callback type can provide denial. Useful for UI/controller error explanation response.
	}
	
	public enum CheckType {
	    Owner,
	    Processed,
	    SessionEnabled, 
	    Enabled,
	    UserEnabled,
	    Visible,
	    UserVisible,
	    SessionVisible, 
	    HubListeners,
	    SuperAdminOverride,
	    CallbackMethod
	}	

	public boolean isUsed(CheckType ct) {
		if (ct == null) return false;
		if (!type.has(ct)) return false;
		if (hmOnlyCheckTypes != null && hmOnlyCheckTypes.size() > 0) {
			return hmOnlyCheckTypes.contains(ct);
		}
		return true;
	}
	
	public CheckType[] getCheckTypes() {
		return getCheckTypesExcept(new CheckType[0]);
	}
	
	public CheckType[] getCheckTypesExcept(CheckType ... ctsExclude) {
		List<CheckType> al = new ArrayList<>();
		List lst = ctsExclude == null || ctsExclude.length == 0 ? null : Arrays.asList(ctsExclude);
		for (CheckType ct : type.checkTypes) {
			if (!isUsed(ct)) continue;
			if (lst != null) {
				if (lst.contains(ct)) continue;
			}
			al.add(ct);
		}
		return al.toArray(new CheckType[0]);
	}
	
	public static CheckType[] getAllCheckTypesButProcessed(Type type) {
		if (type == null) return null;
		EnumSet<CheckType> es = type.checkTypes.clone();
		es.remove(CheckType.Processed);
		return es.toArray(new CheckType[0]);
	}

	public static CheckType[] getAllCheckTypesExcept(Type type, CheckType... cts) {
		if (type == null) return null;
		EnumSet<CheckType> es = type.checkTypes.clone();
		if (cts != null) {
			for (CheckType ct : cts) {
				es.remove(ct);
			}
		}
		return es.toArray(new CheckType[0]);
	}
	
	public static CheckType[] getCallbackOnlyCheckType() {
		return new CheckType[] { CheckType.CallbackMethod };
	}

	public static CheckType[] getCheckTypes(CheckType ct) {
		if (ct == null) return null;
		return new CheckType[] { ct };
	}
	
	/**
	 * Determines the effective class associated with this callback.
	 *
	 * <p>The class resolution follows this order:</p>
	 * <ol>
	 *   <li>If an explicit class has been assigned, returns it.</li>
	 *   <li>Else, if an {@link OAObject} is present, returns the object's class.</li>
	 *   <li>Else, if a {@link Hub} is present, returns the hub's object class.</li>
	 *   <li>Otherwise returns {@code null}.</li>
	 * </ol>
	 *
	 * @return the resolved class, or {@code null} if none is available
	 */
	public Class<? extends OAObject> getCalcClass() {
		if (clazz != null) {
			return clazz;
		}
		if (object != null) {
			return object.getClass();
		}
		if (hub != null) {
			return hub.getObjectClass();
		}
		return null;
	}

	/**
	 * Sets the callback type.
	 *
	 * @param t the new callback type
	 */
	public void setType(Type type) {
		if (type == null) type = Type.Unknown;
		this.type = type;
	}

	/**
	 * Returns the callback type. Note that {@link Type#AllowEnabled}
	 * may be invoked for other types that have {@code checkEnabledFirst = true}.
	 *
	 * @return the current callback type
	 */
	public Type getType() {
		return this.type;
	}

	/**
	 * Assigns the hub associated with this callback.
	 *
	 * @param h the hub to set
	 */
	public void setHub(Hub h) {
		this.hub = h;
	}

	/**
	 * Returns the hub associated with this callback.
	 *
	 * @return the assigned hub, or {@code null} if none
	 */
	public Hub getHub() {
		return hub;
	}

	/**
	 * Returns the object associated with this callback.
	 *
	 * @return the target object, or {@code null} if none
	 */
	public OAObject getObject() {
		return object;
	}

	/**
	 * Sets the object associated with this callback.
	 *
	 * @param object the target object to assign
	 */
	public void setObject(OAObject object) {
		this.object = object;
	}

	/**
	 * Marks this callback as acknowledged.
	 */
	public void ack() {
		this.acknownledged = true;
	}

	/**
	 * Sets the acknowledged flag.
	 *
	 * @param b {@code true} to mark the callback as acknowledged
	 */
	public void setAcknownledged(boolean b) {
		this.acknownledged = b;
	}

	/**
	 * Returns whether this callback has been acknowledged.
	 *
	 * @return {@code true} if acknowledged, otherwise {@code false}
	 */
	public boolean getAcknownledged() {
		return acknownledged;
	}

	/**
	 * Returns the property name associated with this callback.
	 *
	 * @return the property name, or {@code null} if none is set
	 */
	public String getPropertyName() {
		return propertyName;
	}

	/**
	 * Sets the property name associated with this callback.
	 *
	 * @param s the property name to assign
	 */
	public void setPropertyName(String s) {
		this.propertyName = s;
	}

	/**
	 * Sets the old value for this callback.
	 *
	 * @param obj the previous value to assign
	 */
	public void setOldValue(Object obj) {
		oldValue = obj;
	}

	/**
	 * Returns the old value associated with this callback.
	 *
	 * @return the old value, or {@code null} if none is set
	 */
	public Object getOldValue() {
		return oldValue;
	}

	/**
	 * Sets the value for this callback. The meaning of the value depends
	 * on the callback type.
	 *
	 * @param obj the value to assign
	 */
	public void setValue(Object obj) {
		value = obj;
	}

	/**
	 * Returns the current value associated with this callback.
	 *
	 * @return the value, or {@code null} if none is set
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * Sets the response message for this callback.
	 *
	 * @param response the message to assign
	 */
	public void setResponse(String response) {
		this.response = response;
	}

	/**
	 * Returns the response message associated with this callback.
	 *
	 * @return the response message, or {@code null} if none is set
	 */
	public String getResponse() {
		return this.response;
	}

	/**
	 * Returns the throwable assigned to this callback.
	 *
	 * @return the throwable, or {@code null} if none is set
	 */
	public Throwable getThrowable() {
		return throwable;
	}

	/**
	 * Assigns a throwable to this callback. When set, the caller may
	 * throw it and cancel further processing.
	 *
	 * @param t the throwable to assign
	 */
	public void setThrowable(Throwable t) {
		this.throwable = t;
	}

	/**
	 * Returns a displayable response string. If no explicit response
	 * is set, the message is derived from the assigned throwable,
	 * falling back to its {@code toString()} if necessary.
	 *
	 * @return a displayable response message
	 */
	public String getDisplayResponse() {
		String s = getResponse();
		Throwable t = getThrowable();
		if (OAString.isEmpty(s) && t != null) {
			if (t != null) {
				for (; t != null; t = t.getCause()) {
					s = t.getMessage();
					if (OAString.isNotEmpty(s)) {
						break;
					}
				}
				if (OAString.isEmpty(s)) {
					s = getThrowable().toString();
				}
			}
		}
		return s;
	}

	/**
	 * Returns the confirmation dialog title for this callback.
	 *
	 * @return the confirmation title, or {@code null} if none is set
	 */
	public String getConfirmTitle() {
		return confirmTitle;
	}

	/**
	 * Sets the confirmation dialog title for this callback.
	 *
	 * @param confirmTitle the title to assign
	 */
	public void setConfirmTitle(String confirmTitle) {
		this.confirmTitle = confirmTitle;
	}

	/**
	 * Returns the confirmation message for this callback.
	 *
	 * @return the confirmation message, or {@code null} if none is set
	 */
	public String getConfirmMessage() {
		return confirmMessage;
	}

	/**
	 * Sets the confirmation message for this callback.
	 *
	 * @param confirmMessage the message to assign
	 */
	public void setConfirmMessage(String confirmMessage) {
		this.confirmMessage = confirmMessage;
	}

	/**
	 * Returns the tooltip text assigned to this callback.
	 *
	 * @return the tooltip text, or {@code null} if none is set
	 */
	public String getToolTip() {
		return toolTip;
	}

	/**
	 * Sets the tooltip text for this callback.
	 *
	 * @param toolTip the tooltip text to assign
	 */
	public void setToolTip(String toolTip) {
		this.toolTip = toolTip;
	}

	/**
	 * Returns whether the callback action is currently allowed.
	 *
	 * @return {@code true} if the action is allowed; otherwise {@code false}
	 */
	public boolean isAllowed() {
		return allowed;
	}

	/**
	 * Returns whether the callback action is allowed.
	 *
	 * @return {@code true} if allowed; otherwise {@code false}
	 */
	public boolean getAllowed() {
		return allowed;
	}

	/**
	 * Sets whether the callback action is allowed.
	 *
	 * @param enabled {@code true} to allow the action, {@code false} to block it
	 */
	public void setAllowed(boolean enabled) {
		this.allowed = enabled;
	}

	/**
	 * Returns the callback value converted to a boolean.
	 *
	 * @return the boolean representation of the value
	 */
	public boolean getBooleanValue() {
		return OAConv.toBoolean(value);
	}

	/**
	 * Returns the callback value converted to an integer.
	 *
	 * @return the integer representation of the value
	 */
	public int getIntValue() {
		return OAConv.toInt(value);
	}

	/**
	 * Returns the label associated with this callback.
	 *
	 * @return the label, or {@code null} if none is set
	 */
	public OACallbackLabel getLabel() {
		return label;
	}

	/**
	 * Assigns a label to this callback for UI-related operations.
	 *
	 * @param label the label to assign
	 */
	public void setLabel(OACallbackLabel label) {
		this.label = label;
	}

	/**
	 * Returns the formatting string associated with this callback.
	 *
	 * @return the format string, or {@code null} if none is set
	 */
	public String getFormat() {
		return format;
	}

	/**
	 * Sets the formatting string for this callback. The meaning of the
	 * format depends on the callback type.
	 *
	 * @param format the format string to assign
	 */
	public void setFormat(String format) {
		this.format = format;
	}
}
