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
package com.viaoa.object;

import javax.swing.JLabel;

import com.viaoa.hub.Hub;
import com.viaoa.util.OAConv;
import com.viaoa.util.OAString;

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

	private Hub hub;
	private OAObject object;

	private Type type = Type.Unknown;
	private int checkType = CHECK_ALL;

	// level of checking to include
	public static final int CHECK_None = 0;
	public static final int CHECK_Processed = 1; // used to include checking for processed flags
	public static final int CHECK_EnabledProperty = 2;
	public static final int CHECK_UserEnabledProperty = 4;
	public static final int CHECK_CallbackMethod = 8;
	public static final int CHECK_IncludeMaster = 16; // check owner object
	public static final int CHECK_ALL = 31;
	public static final int CHECK_AllButProcessed = (CHECK_ALL ^ CHECK_Processed);

	private String confirmTitle; // allow interaction with UI to have user confirm before continuing
	private String confirmMessage; // message to use for confirming
	private String toolTip;
	private String format; // allows creating customized formatter

	private boolean allowed = true; // flag to know if the type of objectCallback is permitted

	private Object value; // depends on Type
	private JLabel label; // used for UI rendering control

	private String response; // used to give message back to caller
	private Throwable throwable; // used to tell the caller to throw this exception and not to allow further processing.

	private String propertyName;
	private Object oldValue;

	/**
	 * Flag to know that the called code has Ack'd the call.
	 */
	private boolean acknownledged;

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
	public enum Type { // properies to use based on type:
	    /**
	     * Indicates an unspecified or uninitialized callback type.
	     * Performs no owner or enabled-first evaluation.
	     */
		Unknown(false),

	    /**
	     * Determines whether a target object or property is enabled.
	     * Also invoked for types that have {@code checkEnabledFirst = true}.
	     * Includes owner checks but does not perform enabled-first evaluation.
	     */
		AllowEnabled(true, false), 

	    /**
	     * Determines whether an object, property, link, or method is visible.
	     * Performs owner checks. Does not perform enabled-first evaluation.
	     */
		AllowVisible(true),

	    /**
	     * Determines whether creation of a new object is permitted.
	     * Performs owner checks and evaluates enabled-first rules.
	     */
		AllowNew(true, true),

	    /**
	     * Determines whether an object can be added to a hub.
	     * Performs owner checks and evaluates enabled-first rules.
	     */
		AllowAdd(true, true),
		
	    /**
	     * Determines whether an object can be removed from a hub.
	     * Performs owner checks and evaluates enabled-first rules.
	     */
		AllowRemove(true, true),

	    /**
	     * Determines whether all items may be removed from a hub.
	     * Performs owner checks and evaluates enabled-first rules.
	     */
		AllowRemoveAll(true, true),

	    /**
	     * Determines whether deletion of an object is permitted.
	     * Performs owner checks and evaluates enabled-first rules.
	     */
		AllowDelete(true, true),

	    /**
	     * Determines whether saving an object is permitted.
	     * Does not evaluate owner or enabled-first rules.
	     * Saving must be allowed even if an object is disabled.
	     */
		AllowSave(false, false), // dont check parent(s) or if enabled.  Need to be able to save a disabled object

	    /**
	     * Determines whether an object copy operation is permitted.
	     * No owner or enabled-first evaluation.
	     */
		AllowCopy(false),

	    /**
	     * Validates that an object’s required values and rules are satisfied
	     * before submitting. No owner or enabled-first evaluation.
	     */
		AllowSubmit(false, false), // called to see if object is populated with correct values

	    /**
	     * Verifies whether a property change is valid.
	     * Checks new and old values, and may block the action using
	     * {@code allowed = false} or a {@code throwable}.
	     * Performs owner checks and evaluates enabled-first rules.
	     */
		VerifyPropertyChange(true, true), // use: value to get new value, name, response, throwable - set allowEnablede=false, or throwable!=null to cancel

	    /**
	     * Verifies whether adding an object to a hub is valid.
	     * Uses the callback value as the object being added.
	     * Performs owner checks and evaluates enabled-first rules.
	     */
		VerifyAdd(true, true), // use: value to get added object, allowAdd, throwable - set allowed=false, or throwable!=null to cancel

	    /**
	     * Verifies whether removing an object from a hub is valid.
	     * Uses the callback value as the object being removed.
	     * Performs owner checks and evaluates enabled-first rules.
	     */
		VerifyRemove(true, true), // use: value to get removed object, allowRemove, throwable - set allowRemove=false, or throwable!=null to cancel

	    /**
	     * Verifies whether removing all objects from a hub is valid.
	     * Performs owner checks and evaluates enabled-first rules.
	     */
		VerifyRemoveAll(true, true), // use: allowRemoveAll, response, throwable - set allowRemoveAll=false, or throwable!=null to cancel

	    /**
	     * Verifies whether deleting an object is valid.
	     * Uses the callback value as the deleted object.
	     * Performs owner checks and evaluates enabled-first rules.
	     */
		VerifyDelete(true, true), // use: value to get deleted object, allowDelete, throwable - set allowDelete=false, or throwable!=null to cancel

	    /**
	     * Verifies whether saving an object is valid.
	     * Does not perform owner or enabled-first evaluation.
	     */
		VerifySave(false, false), // dont check parent(s) or if enabled.  Need to be able to save a disabled object

	    /**
	     * Verifies whether invoking a command/method is valid.
	     * Performs owner checks and evaluates enabled-first rules.
	     */
		VerifyCommand(true, true),

	    /**
	     * Supplies copy behavior. Callback may set allowed,
	     * supply a replacement value, or allow default copy logic.
	     * No owner or enabled-first evaluation.
	     */
		GetCopy(false), // can set allowed(..), or setValue(newObj), or nothing to have OAObject.createCopy(..) called.

	    /**
	     * Invoked after an object copy has been created.
	     * The callback value contains the new object.
	     * No owner or enabled-first evaluation.
	     */
		AfterCopy(false), // value=newObject

	    /**
	     * Supplies confirmation title and message for a property change.
	     * No owner or enabled-first evaluation.
	     */
		SetConfirmForPropertyChange(false),

	    /**
	     * Supplies confirmation title and message for an add operation.
	     * No owner or enabled-first evaluation.
	     */
		SetConfirmForAdd(false),

	    /**
	     * Supplies confirmation title and message for a remove operation.
	     * No owner or enabled-first evaluation.
	     */
		SetConfirmForRemove(false),

	    /**
	     * Supplies confirmation title and message for a remove-all operation.
	     * No owner or enabled-first evaluation.
	     */
		SetConfirmForRemoveAll(false),

	    /**
	     * Supplies confirmation title and message for a delete operation.
	     * No owner or enabled-first evaluation.
	     */
		SetConfirmForDelete(false),

	    /**
	     * Supplies confirmation title and message for a save operation.
	     * No owner or enabled-first evaluation.
	     */
		SetConfirmForSave(false),

	    /**
	     * Supplies confirmation title and message for a command/method invocation.
	     * No owner or enabled-first evaluation.
	     */
		SetConfirmForCommand(false), //todo: qqqq

	    /**
	     * Supplies a tooltip string for UI presentation.
	     * Uses the callback’s toolTip field.
	     */
		GetToolTip(false), // use: toolTip

	    /**
	     * Allows callback logic to update the label used to render a UI component.
	     * Uses the callback’s label field.
	     */
		RenderLabel(false), // use: update the label used to render a component

	    /**
	     * Updates the label belonging to a UI component.
	     * Uses the callback’s label field.
	     */
		UpdateLabel(false), // update the jlabel that belongs to a component
		
	    /**
	     * Supplies a format string used to customize UI formatting.
	     * Uses the callback’s format field.
	     */
		GetFormat(false); // use: format

		protected boolean checkOwner;
		protected boolean checkEnabledFirst;
		
		public boolean isCheckOwner() {
			return checkOwner;
		}
		public boolean isCheckEnabledFirst() {
			return checkEnabledFirst;
		}
		

		Type(boolean checkOwner) {
			this.checkOwner = checkOwner;
		}

		Type(boolean checkOwner, boolean checkEnabledFirst) {
			this.checkOwner = checkOwner;
			this.checkEnabledFirst = checkEnabledFirst;
		}
	}

	private Class clazz;

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
	public Class getCalcClass() {
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
	 * Creates a callback with the specified {@link Type}.
	 * Initializes the callback with the given type and leaves all
	 * other fields at their default values.
	 *
	 * @param type the callback type to assign
	 */
	public OAObjectCallback(Type type) {
		this.type = type;
	}

	/**
	 * Creates a callback with full context information including type,
	 * check flags, hub, class, target object, property name, and value.
	 * All supplied fields are assigned directly, and the callback is
	 * initialized as allowed.
	 *
	 * @param type the callback type to assign
	 * @param checkType the bitmask of checking options
	 * @param hub the associated hub, or {@code null}
	 * @param clazz an explicit class to associate, or {@code null}
	 * @param oaObj the target {@link OAObject}, or {@code null}
	 * @param propertyName the related property name, or {@code null}
	 * @param value the callback value, interpreted according to the type
	 */
	public OAObjectCallback(Type type, int checkType, Hub hub, Class clazz, OAObject oaObj, String propertyName, Object value) {
		this.type = type;
		this.checkType = checkType;
		this.hub = hub;
		this.clazz = clazz;
		this.object = oaObj;
		this.propertyName = propertyName;
		this.value = value;
		this.allowed = true;
	}

	/**
	 * Creates a callback by copying contextual information from an
	 * existing callback. The new instance uses the specified type
	 * and check flags, while hub, class, object, property name,
	 * value, and allowed state are copied from the source.
	 *
	 * @param type the callback type to assign
	 * @param checkType the bitmask of checking options
	 * @param eq the source callback to copy values from
	 */
	public OAObjectCallback(Type type, int checkType, OAObjectCallback eq) {
		this.type = type;
		this.checkType = checkType;

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

	/*
	public OAObjectCallback(Type type, int checkType) {
	    this.type = type;
	    this.checkType = checkType;

	    this.allowed = true;
	}
	*/

	/**
	 * Sets the callback type.
	 *
	 * @param t the new callback type
	 */
	public void setType(Type t) {
		this.type = t;
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
	 * Returns the bitmask of checking options that control how the callback
	 * is evaluated.
	 *
	 * @return the current check-type bitmask
	 */
	public int getCheckType() {
		return checkType;
	}

	/**
	 * Sets the bitmask of checking options that determine how the callback
	 * should be evaluated.
	 *
	 * @param x the new check-type bitmask
	 */
	public void setCheckType(int x) {
		this.checkType = x;
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

	/*qqqq replaced with old/newValue
	public Object getValue() {
	    return value;
	}
	public void setValue(Object value) {
	    this.value = value;
	}
	*/

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
	public JLabel getLabel() {
		return label;
	}

	/**
	 * Assigns a label to this callback for UI-related operations.
	 *
	 * @param label the label to assign
	 */
	public void setLabel(JLabel label) {
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
	
	/*qqqqqqq
	public String getName() {
	    return name;
	}
	public void setName(String name) {
	    this.name = name;
	}
	*/
}
