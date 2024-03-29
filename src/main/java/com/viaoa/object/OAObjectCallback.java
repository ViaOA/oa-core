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
package com.viaoa.object;

import javax.swing.JLabel;

import com.viaoa.annotation.OAObjCallback;
import com.viaoa.hub.Hub;
import com.viaoa.util.OAConv;
import com.viaoa.util.OAString;

/**
 * Used to allow interaction between OAObject callback methods and other (ex: UI) components. 
 * OAObject has a class level "callback(OAObjectCallback eq)" method and 
 * can have callbackXXX(OAObjectCallback eq)" methods defined for other props,calcs,links, and methods.
 *
 * @see OAObjectCallbackDelegate
 * @see OAObjCallback annotation for example
 * @author vvia
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
	 * Type of request being made from caller object. All can use setResponse to include a return message/
	 */
	public enum Type { // properies to use based on type:
		Unknown(false),

		//========= AllowX - to see if the command/option is available
		//     set/getAllowed(b) is used to set/get
		// set: confirmeTitle/Message to have UI interact with user
		AllowEnabled(true, false), // use: allowEnabled  NOTE: this is also called for all types that have checkEnabledFirst=true

		AllowVisible(true), // use: allowVisible

		AllowNew(true, true),
		AllowAdd(true, true),
		AllowRemove(true, true),
		AllowRemoveAll(true, true),
		AllowDelete(true, true),
		AllowSave(false, false), // dont check parent(s) or if enabled.  Need to be able to save a disabled object
		AllowCopy(false),
		AllowSubmit(false, false), // called to see if object is populated with correct values

		VerifyPropertyChange(true, true), // use: value to get new value, name, response, throwable - set allowEnablede=false, or throwable!=null to cancel
		VerifyAdd(true, true), // use: value to get added object, allowAdd, throwable - set allowed=false, or throwable!=null to cancel
		VerifyRemove(true, true), // use: value to get removed object, allowRemove, throwable - set allowRemove=false, or throwable!=null to cancel
		VerifyRemoveAll(true, true), // use: allowRemoveAll, response, throwable - set allowRemoveAll=false, or throwable!=null to cancel
		VerifyDelete(true, true), // use: value to get deleted object, allowDelete, throwable - set allowDelete=false, or throwable!=null to cancel
		VerifySave(false, false), // dont check parent(s) or if enabled.  Need to be able to save a disabled object
		VerifyCommand(true, true),

		GetCopy(false), // can set allowed(..), or setValue(newObj), or nothing to have OAObject.createCopy(..) called.
		AfterCopy(false), // value=newObject

		// set ConfirmMessage, Title
		SetConfirmForPropertyChange(false),
		SetConfirmForAdd(false),
		SetConfirmForRemove(false),
		SetConfirmForRemoveAll(false), //todo: qqqq
		SetConfirmForDelete(false),
		SetConfirmForSave(false),
		SetConfirmForCommand(false), //todo: qqqq

		GetToolTip(false), // use: toolTip
		RenderLabel(false), // use: update the label used to render a component
		UpdateLabel(false), // update the jlabel that belongs to a component
		GetFormat(false); // use: format

		public boolean checkOwner;
		public boolean checkEnabledFirst;

		Type(boolean checkOwner) {
			this.checkOwner = checkOwner;
		}

		Type(boolean checkOwner, boolean checkEnabledFirst) {
			this.checkOwner = checkOwner;
			this.checkEnabledFirst = checkEnabledFirst;
		}
	}

	private Class clazz;

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

	public OAObjectCallback(Type type) {
		this.type = type;
	}

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

	public void setType(Type t) {
		this.type = t;
	}

	/**
	 * Type of query. NOTE: Type.AllowEnabled will also be called for all types that have checkEnabledFirst=true
	 */
	public Type getType() {
		return this.type;
	}

	public int getCheckType() {
		return checkType;
	}

	public void setCheckType(int x) {
		this.checkType = x;
	}

	public void setHub(Hub h) {
		this.hub = h;
	}

	public Hub getHub() {
		return hub;
	}

	public OAObject getObject() {
		return object;
	}

	public void setObject(OAObject object) {
		this.object = object;
	}

	public void ack() {
		this.acknownledged = true;
	}

	public void setAcknownledged(boolean b) {
		this.acknownledged = b;
	}

	public boolean getAcknownledged() {
		return acknownledged;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String s) {
		this.propertyName = s;
	}

	public void setOldValue(Object obj) {
		oldValue = obj;
	}

	public Object getOldValue() {
		return oldValue;
	}

	public void setValue(Object obj) {
		value = obj;
	}

	public Object getValue() {
		return value;
	}

	// set a response to the request.
	public void setResponse(String response) {
		this.response = response;
	}

	public String getResponse() {
		return this.response;
	}

	public Throwable getThrowable() {
		return throwable;
	}

	public void setThrowable(Throwable t) {
		this.throwable = t;
	}

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

	public String getConfirmTitle() {
		return confirmTitle;
	}

	public void setConfirmTitle(String confirmTitle) {
		this.confirmTitle = confirmTitle;
	}

	public String getConfirmMessage() {
		return confirmMessage;
	}

	public void setConfirmMessage(String confirmMessage) {
		this.confirmMessage = confirmMessage;
	}

	public String getToolTip() {
		return toolTip;
	}

	public void setToolTip(String toolTip) {
		this.toolTip = toolTip;
	}

	public boolean isAllowed() {
		return allowed;
	}

	public boolean getAllowed() {
		return allowed;
	}

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
	public boolean getBooleanValue() {
		return OAConv.toBoolean(value);
	}

	public int getIntValue() {
		return OAConv.toInt(value);
	}

	public JLabel getLabel() {
		return label;
	}

	public void setLabel(JLabel label) {
		this.label = label;
	}

	public String getFormat() {
		return format;
	}

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
