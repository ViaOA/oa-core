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
 * Request/response carrier used by the OA object rules engine.
 * <p>
 * An {@code OAObjectCallback} describes one model-rule question being asked by OA
 * and carries both the evaluation context and the resulting answer. It is
 * processed by {@code OAObjectRulesService} and is also the shared carrier used by
 * OAObject callback methods, Hub listeners, and UI/controller code.
 * </p>
 *
 * <h3>Core Contract</h3>
 * <ul>
 *   <li>{@link #getType() type} defines the semantic question, such as
 *       {@link Type#AllowDelete}, {@link Type#VerifySave},
 *       {@link Type#SetConfirmForDelete}, or {@link Type#GetToolTip}.</li>
 *   <li>{@link CheckType} values define which rules-engine pipeline stages are
 *       active for the request.</li>
 *   <li>{@link CategoryType} values are descriptive metadata for grouping
 *       {@link Type} values.</li>
 *   <li>{@link #getAllowed() allowed}, {@link #getResponse() response}, and
 *       {@link #getThrowable() throwable} carry the rule result.</li>
 * </ul>
 *
 * <h3>Context Fields</h3>
 * <ul>
 *   <li>{@code object} is the callback target or receiver. It is the object whose
 *       callback method can run.</li>
 *   <li>{@code propertyName} is the member being evaluated on {@code object}.</li>
 *   <li>{@code value} is the operation operand or new value.</li>
 *   <li>{@code oldValue} is the previous property value for property-change
 *       verification.</li>
 *   <li>{@code hub} supplies Hub context for Hub/listener operations.</li>
 *   <li>{@code clazz} supplies explicit class context when no object instance
 *       exists.</li>
 * </ul>
 *
 * <h3>Common Operation Shapes</h3>
 * <ul>
 *   <li>{@link Type#VerifyPropertyChange}: {@code object} is the target object,
 *       {@code propertyName} is the changed property, {@code oldValue} is the
 *       previous value, and {@code value} is the new value.</li>
 *   <li>{@link Type#AllowAdd} / {@link Type#VerifyAdd} through Hub only:
 *       {@code object} is {@code null} and {@code value} is the object being
 *       added.</li>
 *   <li>{@link Type#AllowAdd} / {@link Type#VerifyAdd} through a master
 *       reverse-link: {@code object} is the master object, {@code propertyName}
 *       is the reverse link/property, and {@code value} is the object being
 *       added.</li>
 *   <li>{@link Type#AllowRemove} / {@link Type#VerifyRemove} through Hub only:
 *       {@code object} is {@code null} and {@code value} is the object being
 *       removed.</li>
 *   <li>{@link Type#AllowDelete} / {@link Type#VerifyDelete} direct object:
 *       {@code object} is the object being deleted and {@code value} is
 *       {@code null}.</li>
 *   <li>{@link Type#AllowDelete} / {@link Type#VerifyDelete} through a master
 *       reverse-link: {@code object} is the master object, {@code propertyName}
 *       is the reverse link/property, and {@code value} is the object being
 *       deleted.</li>
 * </ul>
 *
 * @see OAObject
 * @see Hub
 * @see com.viaoa.annotation.OAObjCallback
 */
public class OAObjectCallback {
	static final long serialVersionUID = 1L;

	/**
	 * Hub context for this rule request.
	 * <p>
	 * The Hub supplies collection, active-object, detail/master, and listener
	 * context for Hub-based operations. It can be {@code null} for direct
	 * object/class requests.
	 * </p>
	 */
	private Hub<?> hub;

	/**
	 * Callback target or receiver.
	 * <p>
	 * This is the object whose callback method can run. For Hub-only add/remove
	 * requests this can be {@code null}; in those cases the operation operand is
	 * carried by {@link #value}.
	 * </p>
	 */
	private OAObject object;

	/**
	 * Semantic question being asked by the rules engine.
	 * <p>
	 * Defaults to {@link Type#Unknown}.
	 * </p>
	 */
	private Type type = Type.Unknown;

	/**
	 * Optional narrowing set for the active {@link CheckType} values.
	 * <p>
	 * When populated, only these checks are used from the {@link Type}'s default
	 * check list.
	 * </p>
	 */
	private final EnumSet<CheckType> hmOnlyCheckTypes;
	
	/**
	 * Optional confirmation title supplied by confirmation-type requests.
	 */
	private String confirmTitle; // allow interaction with UI to have user confirm before continuing

	/**
	 * Optional confirmation message supplied by confirmation-type requests.
	 */
	private String confirmMessage; // message to use for confirming

	/**
	 * Tooltip text supplied by rule processing or object callback logic.
	 */
	private String toolTip;

	/**
	 * Optional format string supplied by rule processing or object callback logic.
	 */
	private String format; // allows creating customized formatter

	/**
	 * Current allowed/denied result for this rule request.
	 * <p>
	 * The value defaults to {@code true}. Rules, callback methods, and Hub
	 * listeners can set it to {@code false} to deny the request.
	 * </p>
	 */
	private boolean allowed = true; // flag to know if the type of objectCallback is permitted

	/**
	 * Operation operand or new value.
	 * <p>
	 * The meaning depends on {@link #type}. Examples include the new property
	 * value for {@link Type#VerifyPropertyChange}, the object being added for
	 * {@link Type#AllowAdd}/{@link Type#VerifyAdd}, or the object being removed
	 * for {@link Type#AllowRemove}/{@link Type#VerifyRemove}.
	 * </p>
	 */
	private Object value; // depends on Type

	/**
	 * Optional label carrier used by UI-related callback types.
	 */
	private OACallbackLabel label; // used for UI rendering control

	/**
	 * Human-readable result or denial explanation.
	 * <p>
	 * Rules and callbacks can set this for UI/controller code to display or log.
	 * </p>
	 */
	private String response; // used to give message back to caller

	/**
	 * Exception or failure detail produced by rule processing.
	 * <p>
	 * Callers can propagate this when the rule result should fail with an
	 * exception instead of only a response message.
	 * </p>
	 */
	private Throwable throwable; // used to tell the caller to throw this exception and not to allow further processing.

	/**
	 * Member being evaluated on {@link #object}.
	 * <p>
	 * This is commonly a property, link, method, or reverse-link name depending
	 * on the callback {@link #type}.
	 * </p>
	 */
	private String propertyName;

	/**
	 * Previous property value for property-change verification.
	 */
	private Object oldValue;

	/**
	 * Indicates whether this callback has been acknowledged by the caller,
	 * allowing higher-level logic to know the request was processed.
	 */
	private boolean acknownledged;

	/**
	 * Explicit class context used when no object instance exists or when class
	 * context must be supplied directly.
	 */
	private Class<? extends OAObject> clazz;

	
	/**
	 * Creates a rule request for the supplied semantic {@link Type}.
	 * <p>
	 * Other context fields remain unset and {@link #getAllowed()} defaults to
	 * {@code true}. A {@code null} type is normalized to {@link Type#Unknown}.
	 * </p>
	 *
	 * @param type the semantic rule question to ask
	 */
	public OAObjectCallback(Type type) {
		if (type == null) type = Type.Unknown;
		this.type = type;
		this.hmOnlyCheckTypes = null;
	}

	/**
	 * Creates a rule request with full context information.
	 * <p>
	 * The supplied {@code onlyCheckTypes} narrow the default pipeline stages for
	 * the selected {@link Type}. The callback starts in the allowed state.
	 * </p>
	 *
	 * @param type the semantic rule question to ask
	 * @param onlyCheckTypes optional checks to use instead of the full default check list
	 * @param hub Hub context for Hub/listener operations, or {@code null}
	 * @param clazz explicit class context, or {@code null}
	 * @param oaObj callback target/receiver, or {@code null}
	 * @param propertyName member being evaluated on {@code oaObj}, or {@code null}
	 * @param value operation operand or new value, interpreted by {@code type}
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

	/**
	 * Creates a rule request with Hub, class, and object context.
	 *
	 * @param type the semantic rule question to ask
	 * @param hub Hub context, or {@code null}
	 * @param clazz explicit class context, or {@code null}
	 * @param oaObj callback target/receiver, or {@code null}
	 */
	public OAObjectCallback(Type type, Hub<?> hub, Class<? extends OAObject> clazz, OAObject oaObj) {
		this(type, (CheckType[]) null, hub, clazz, oaObj, null, null);
	}
	
	/**
	 * Creates a rule request with Hub, object/class, member, and operation value context.
	 *
	 * @param type the semantic rule question to ask
	 * @param hub Hub context, or {@code null}
	 * @param clazz explicit class context, or {@code null}
	 * @param oaObj callback target/receiver, or {@code null}
	 * @param propertyName member being evaluated on {@code oaObj}, or {@code null}
	 * @param value operation operand or new value, interpreted by {@code type}
	 */
	public OAObjectCallback(Type type, Hub<?> hub, Class<? extends OAObject> clazz, OAObject oaObj, String propertyName, Object value) {
		this(type, (CheckType[]) null, hub, clazz, oaObj, propertyName, value);
	}

	/**
	 * Creates a rule request constrained to one rules-engine check stage.
	 *
	 * @param type the semantic rule question to ask
	 * @param onlyCheckType optional single check to use instead of the full default check list
	 * @param hub Hub context, or {@code null}
	 * @param clazz explicit class context, or {@code null}
	 * @param oaObj callback target/receiver, or {@code null}
	 * @param propertyName member being evaluated on {@code oaObj}, or {@code null}
	 * @param value operation operand or new value, interpreted by {@code type}
	 */
	public OAObjectCallback(Type type, CheckType onlyCheckType, Hub<?> hub, Class<? extends OAObject> clazz, OAObject oaObj, String propertyName, Object value) {
		this(type, onlyCheckType == null ? null : new CheckType[] {onlyCheckType}, 
			hub, clazz, oaObj, propertyName, value);
	}
	
	/**
	 * Creates a rule request by copying context from another callback.
	 * <p>
	 * The new instance uses the supplied {@code type} and optional check narrowing,
	 * while Hub, class, object, property, value, and current allowed state are
	 * copied from {@code eq}.
	 * </p>
	 *
	 * @param type the semantic rule question to ask
	 * @param onlyCheckTypes optional checks to use instead of the full default check list
	 * @param eq source callback to copy context from, or {@code null}
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
	 * Semantic question being asked of the OA rules engine.
	 * <p>
	 * Each {@code Type} identifies one supported model interaction, such as
	 * {@link #AllowDelete}, {@link #VerifySave}, {@link #SetConfirmForDelete}, or
	 * {@link #GetToolTip}. The type also declares its default {@link CheckType}
	 * pipeline stages and descriptive {@link CategoryType} values.
	 * </p>
	 */
	public enum Type { // the policy descriptor
		
	    /**
	     * Unspecified or uninitialized rule question.
	     * <p>No rules-engine checks are active by default.</p>
	     */
		Unknown(new CheckType[] {}, new CategoryType[0]),

	    /**
	     * Asks whether a target object, property, link, or method is enabled.
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
	     * Asks whether a target object, property, link, or method is visible.
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
	     * Asks whether creation of a new object is permitted.
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
	     * Asks whether an object can be added to a Hub.
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
	     * Asks whether an object can be removed from a Hub.
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
	     * Asks whether all objects may be removed from a Hub.
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
	     * Asks whether deletion of an object is permitted.
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
	     * Asks whether saving an object is permitted.
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
	     * Asks whether an object copy operation is permitted.
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
	     * Asks whether an object can be submitted.
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
	     * <p>{@code value} is the new value and {@code oldValue} is the previous value.</p>
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
	     * Verifies whether adding an object to a Hub is valid.
	     * <p>{@code value} is the object being added.</p>
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
	     * Verifies whether removing an object from a Hub is valid.
	     * <p>{@code value} is the object being removed.</p>
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
	     * Verifies whether removing all objects from a Hub is valid.
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
	     * <p>For relationship-scoped deletes, {@code value} can be the object being deleted.</p>
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
	     * Verifies whether invoking a command or model method is valid.
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
	     * Requests copy behavior for an object or member.
	     * <p>Callback logic may set a replacement value or allow default copy handling.</p>
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
	     * Notifies callback logic after an object copy has been created.
	     * <p>{@code value} contains the copied/new object.</p>
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
	     * Requests confirmation title/message for a property change.
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
	     * Requests confirmation title/message for an add operation.
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
	     * Requests confirmation title/message for a remove operation.
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
	     * Requests confirmation title/message for a remove-all operation.
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
	     * Requests confirmation title/message for a delete operation.
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
	     * Requests confirmation title/message for a save operation.
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
	     * Requests confirmation title/message for a command or method invocation.
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

		/**
		 * Returns whether this type belongs to a descriptive category.
		 *
		 * @param flag category to test
		 * @return {@code true} if this type has the category
		 */
		public boolean has(CategoryType flag) {
		    return categoryTypes.contains(flag);
		}
		/**
		 * Returns whether this type uses a rules-engine processing stage by default.
		 *
		 * @param flag check stage to test
		 * @return {@code true} if this type includes the check stage
		 */
		public boolean has(CheckType flag) {
		    return checkTypes.contains(flag);
		}
	}

	/**
	 * Descriptive metadata used to group {@link Type} values.
	 * <p>
	 * Category values describe the kind of interaction represented by a type. They
	 * are intended for classification, filtering, documentation, and API clarity;
	 * rule processing is defined by {@link CheckType}.
	 * </p>
	 */
	public enum CategoryType {
	    /** Type answers whether an action or state is permitted. */
	    PermissionGate,
	    /** Type validates data or action integrity. */
	    ValidationGate,
	    /** Type only affects presentation or interaction data such as labels, format, or tooltip. */
	    UiOnly,
	    /** Type supplies confirmation title/message data. */
	    Confirmation,
	    /** Type participates in copy flow. */
	    CopyLifecycle,
	    /** Type permits or denies a model-changing action. */
	    MutationPermission,
	    /** Type is inherently about Hub membership or Hub-scoped operation. */
	    HubOperation,
	    /** Type concerns object lifecycle such as new, save, delete, submit, or copy. */
	    ObjectLifecycleOperation,
	    /** Type can provide a response explaining the rule result. */
	    CanSetResponse,
	}
	
	/**
	 * Rules-engine processing stage used by {@link OAObjectCallback}.
	 * <p>
	 * A {@link Type} declares the default check stages that participate in rule
	 * evaluation. A callback instance can narrow those stages with
	 * {@code onlyCheckTypes}; {@link #isUsed(CheckType)} answers whether a stage is
	 * active for a specific callback.
	 * </p>
	 */
	public enum CheckType {
	    /** Evaluate owner/master hierarchy visibility or enabled state. */
	    Owner,
	    /** Evaluate processed-state restrictions. */
	    Processed,
	    /** Evaluate session-user/session-access enabled scope. */
	    SessionEnabled, 
	    /** Evaluate model metadata enabled rules. */
	    Enabled,
	    /** Evaluate ModelUser enabled rules. */
	    UserEnabled,
	    /** Evaluate model metadata visible rules. */
	    Visible,
	    /** Evaluate ModelUser visible rules. */
	    UserVisible,
	    /** Evaluate session-user/session-access visible scope. */
	    SessionVisible, 
	    /** Invoke Hub listener participation for the rule request. */
	    HubListeners,
	    /** Apply super-admin override policy where the rule type allows it. */
	    SuperAdminOverride,
	    /** Invoke the model object's callback method for the rule request. */
	    CallbackMethod
	}	

	/**
	 * Returns whether a check stage is active for this callback instance.
	 * <p>
	 * The stage must be included by the {@link Type}. If this callback was created
	 * with a narrowed check list, the stage must also be present in that list.
	 * </p>
	 *
	 * @param ct check stage to test
	 * @return {@code true} if the stage should run for this request
	 */
	public boolean isUsed(CheckType ct) {
		if (ct == null) return false;
		if (!type.has(ct)) return false;
		if (hmOnlyCheckTypes != null && hmOnlyCheckTypes.size() > 0) {
			return hmOnlyCheckTypes.contains(ct);
		}
		return true;
	}
	
	/**
	 * Returns the active check stages for this callback.
	 *
	 * @return active check stages in type declaration order
	 */
	public CheckType[] getCheckTypes() {
		return getCheckTypesExcept(new CheckType[0]);
	}
	
	/**
	 * Returns active check stages after excluding selected stages.
	 *
	 * @param ctsExclude check stages to omit
	 * @return active check stages in type declaration order
	 */
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
	
	/**
	 * Returns a type's default check stages except {@link CheckType#Processed}.
	 *
	 * @param type rule type to inspect
	 * @return check stages, or {@code null} when {@code type} is {@code null}
	 */
	public static CheckType[] getAllCheckTypesButProcessed(Type type) {
		if (type == null) return null;
		EnumSet<CheckType> es = type.checkTypes.clone();
		es.remove(CheckType.Processed);
		return es.toArray(new CheckType[0]);
	}

	/**
	 * Returns a type's default check stages except the supplied exclusions.
	 *
	 * @param type rule type to inspect
	 * @param cts check stages to omit
	 * @return check stages, or {@code null} when {@code type} is {@code null}
	 */
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
	
	/**
	 * Returns a check list that invokes only the model callback method stage.
	 *
	 * @return callback-method-only check list
	 */
	public static CheckType[] getCallbackOnlyCheckType() {
		return new CheckType[] { CheckType.CallbackMethod };
	}

	/**
	 * Wraps a single check stage as a check list.
	 *
	 * @param ct check stage to wrap
	 * @return one-element check list, or {@code null} when {@code ct} is {@code null}
	 */
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
	 * Sets the semantic rule question for this callback.
	 *
	 * @param type the new rule type
	 */
	public void setType(Type type) {
		if (type == null) type = Type.Unknown;
		this.type = type;
	}

	/**
	 * Returns the semantic rule question for this callback.
	 *
	 * @return the current rule type
	 */
	public Type getType() {
		return this.type;
	}

	/**
	 * Assigns Hub context for this rule request.
	 *
	 * @param h Hub context, or {@code null}
	 */
	public void setHub(Hub h) {
		this.hub = h;
	}

	/**
	 * Returns Hub context for this rule request.
	 *
	 * @return assigned Hub context, or {@code null}
	 */
	public Hub getHub() {
		return hub;
	}

	/**
	 * Returns the callback target/receiver.
	 *
	 * @return target object, or {@code null}
	 */
	public OAObject getObject() {
		return object;
	}

	/**
	 * Sets the callback target/receiver.
	 *
	 * @param object target object, or {@code null}
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
	 * Returns the member being evaluated on the callback target.
	 *
	 * @return property, link, method, or reverse-link name, or {@code null}
	 */
	public String getPropertyName() {
		return propertyName;
	}

	/**
	 * Sets the member being evaluated on the callback target.
	 *
	 * @param s property, link, method, or reverse-link name, or {@code null}
	 */
	public void setPropertyName(String s) {
		this.propertyName = s;
	}

	/**
	 * Sets the previous property value for property-change verification.
	 *
	 * @param obj previous value, or {@code null}
	 */
	public void setOldValue(Object obj) {
		oldValue = obj;
	}

	/**
	 * Returns the previous property value for property-change verification.
	 *
	 * @return previous value, or {@code null}
	 */
	public Object getOldValue() {
		return oldValue;
	}

	/**
	 * Sets the operation operand or new value.
	 * <p>
	 * The meaning depends on {@link #getType()}. For example, this is the new
	 * property value for {@link Type#VerifyPropertyChange}, the added object for
	 * {@link Type#AllowAdd}/{@link Type#VerifyAdd}, and the removed object for
	 * {@link Type#AllowRemove}/{@link Type#VerifyRemove}.
	 * </p>
	 *
	 * @param obj operation operand or new value
	 */
	public void setValue(Object obj) {
		value = obj;
	}

	/**
	 * Returns the operation operand or new value.
	 *
	 * @return operation operand or new value, or {@code null}
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * Sets the human-readable result or denial explanation.
	 *
	 * @param response response text, or {@code null}
	 */
	public void setResponse(String response) {
		this.response = response;
	}

	/**
	 * Returns the human-readable result or denial explanation.
	 *
	 * @return response text, or {@code null}
	 */
	public String getResponse() {
		return this.response;
	}

	/**
	 * Returns exception/failure detail produced by rule processing.
	 *
	 * @return throwable detail, or {@code null}
	 */
	public Throwable getThrowable() {
		return throwable;
	}

	/**
	 * Sets exception/failure detail for this rule result.
	 *
	 * @param t throwable detail, or {@code null}
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
	 * Returns whether the rule request is currently allowed.
	 *
	 * @return {@code true} if allowed; otherwise {@code false}
	 */
	public boolean isAllowed() {
		return allowed;
	}

	/**
	 * Returns whether the rule request is allowed.
	 *
	 * @return {@code true} if allowed; otherwise {@code false}
	 */
	public boolean getAllowed() {
		return allowed;
	}

	/**
	 * Sets whether the rule request is allowed.
	 *
	 * @param enabled {@code true} to allow the request, {@code false} to deny it
	 */
	public void setAllowed(boolean enabled) {
		this.allowed = enabled;
	}

	/**
	 * Returns the operation value converted to a boolean.
	 *
	 * @return boolean representation of {@link #getValue()}
	 */
	public boolean getBooleanValue() {
		return OAConv.toBoolean(value);
	}

	/**
	 * Returns the operation value converted to an integer.
	 *
	 * @return integer representation of {@link #getValue()}
	 */
	public int getIntValue() {
		return OAConv.toInt(value);
	}

	/**
	 * Returns the label carrier used by UI-related rule requests.
	 *
	 * @return label carrier, or {@code null}
	 */
	public OACallbackLabel getLabel() {
		return label;
	}

	/**
	 * Sets the label carrier used by UI-related rule requests.
	 *
	 * @param label label carrier, or {@code null}
	 */
	public void setLabel(OACallbackLabel label) {
		this.label = label;
	}

	/**
	 * Returns the format string supplied by UI-related rule requests.
	 *
	 * @return format string, or {@code null}
	 */
	public String getFormat() {
		return format;
	}

	/**
	 * Sets the format string supplied by UI-related rule requests.
	 *
	 * @param format format string, or {@code null}
	 */
	public void setFormat(String format) {
		this.format = format;
	}
}
