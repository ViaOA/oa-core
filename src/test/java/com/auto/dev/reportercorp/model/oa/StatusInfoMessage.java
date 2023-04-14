package com.auto.dev.reportercorp.model.oa;

import java.util.logging.Logger;

import com.viaoa.annotation.OACalculatedProperty;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAColumn;
import com.viaoa.annotation.OAFkey;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OALinkTable;
import com.viaoa.annotation.OAOne;
import com.viaoa.annotation.OAProperty;
import com.viaoa.object.OAObject;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAString;

@OAClass(lowerName = "statusInfoMessage", pluralName = "StatusInfoMessages", shortName = "sim", displayName = "Status Info Message", useDataSource = false, displayProperty = "id")
public class StatusInfoMessage extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(StatusInfoMessage.class.getName());

	public static final String P_Id = "id";
	public static final String P_Created = "created";
	public static final String P_Type = "type";
	public static final String P_TypeString = "typeString";
	public static final String P_TypeEnum = "typeEnum";
	public static final String P_TypeDisplay = "typeDisplay";
	public static final String P_Message = "message";
	public static final String P_Counter = "counter";
	public static final String P_Exception = "exception";

	public static final String P_StatusInfo = "statusInfo";
	public static final String P_StatusInfoId = "statusInfoId"; // fkey
	public static final String P_StatusInfoActivity = "statusInfoActivity";
	public static final String P_StatusInfoActivityId = "statusInfoActivityId"; // fkey
	public static final String P_StatusInfoAlert = "statusInfoAlert";
	public static final String P_StatusInfoAlertId = "statusInfoAlertId"; // fkey

	protected volatile int id;
	protected volatile OADateTime created;
	protected volatile int type;

	public static enum Type {
		status("Status"),
		activity("Activity"),
		alert("Alert");

		private String display;

		Type(String display) {
			this.display = display;
		}

		public String getDisplay() {
			return display;
		}
	}

	public static final int TYPE_status = 0;
	public static final int TYPE_activity = 1;
	public static final int TYPE_alert = 2;

	protected volatile String message;
	protected volatile int counter;
	protected volatile String exception;

	// Links to other objects.
	protected volatile transient StatusInfo statusInfo;

	public StatusInfoMessage() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
	}

	public StatusInfoMessage(int id) {
		this();
		setId(id);
	}

	@OAProperty(isUnique = true, trackPrimitiveNull = false, displayLength = 6, noPojo = true)
	@OAId
	@OAColumn(name = "id", sqlType = java.sql.Types.INTEGER)
	public int getId() {
		return id;
	}

	public void setId(int newValue) {
		int old = id;
		fireBeforePropertyChange(P_Id, old, newValue);
		this.id = newValue;
		firePropertyChange(P_Id, old, this.id);
	}

	@OAProperty(defaultValue = "new OADateTime()", displayLength = 15, uiColumnLength = 18, format = "MM/dd hh:mm:ss.S", isProcessed = true, ignoreTimeZone = true)
	@OAColumn(name = "created", sqlType = java.sql.Types.TIMESTAMP)
	public OADateTime getCreated() {
		return created;
	}

	public void setCreated(OADateTime newValue) {
		OADateTime old = created;
		fireBeforePropertyChange(P_Created, old, newValue);
		this.created = newValue;
		firePropertyChange(P_Created, old, this.created);
	}

	@OAProperty(trackPrimitiveNull = false, displayLength = 6, isNameValue = true)
	@OAColumn(name = "type", sqlType = java.sql.Types.INTEGER)
	public int getType() {
		return type;
	}

	public void setType(int newValue) {
		int old = type;
		fireBeforePropertyChange(P_Type, old, newValue);
		this.type = newValue;
		firePropertyChange(P_Type, old, this.type);
	}

	@OAProperty(enumPropertyName = P_Type)
	public String getTypeString() {
		Type type = getTypeEnum();
		if (type == null) {
			return null;
		}
		return type.name();
	}

	public void setTypeString(String val) {
		int x = -1;
		if (OAString.isNotEmpty(val)) {
			Type type = Type.valueOf(val);
			if (type != null) {
				x = type.ordinal();
			}
		}
		if (x < 0) {
			x = 0;
		}
		setType(x);
	}

	@OAProperty(enumPropertyName = P_Type)
	public Type getTypeEnum() {
		final int val = getType();
		if (val < 0 || val >= Type.values().length) {
			return null;
		}
		return Type.values()[val];
	}

	public void setTypeEnum(Type val) {
		if (val == null) {
			setType(0);
		} else {
			setType(val.ordinal());
		}
	}

	@OACalculatedProperty(enumPropertyName = P_Type, displayName = "Type", displayLength = 6, columnLength = 6, properties = { P_Type })
	public String getTypeDisplay() {
		Type type = getTypeEnum();
		if (type == null) {
			return null;
		}
		return type.getDisplay();
	}

	@OAProperty(maxLength = 250, displayLength = 25, uiColumnLength = 20)
	@OAColumn(name = "message", maxLength = 250)
	public String getMessage() {
		return message;
	}

	public void setMessage(String newValue) {
		String old = message;
		fireBeforePropertyChange(P_Message, old, newValue);
		this.message = newValue;
		firePropertyChange(P_Message, old, this.message);
	}

	@OAProperty(displayLength = 5, uiColumnLength = 7)
	@OAColumn(name = "counter", sqlType = java.sql.Types.INTEGER)
	public int getCounter() {
		return counter;
	}

	public void setCounter(int newValue) {
		int old = counter;
		fireBeforePropertyChange(P_Counter, old, newValue);
		this.counter = newValue;
		firePropertyChange(P_Counter, old, this.counter);
	}

	@OAProperty(displayLength = 30, uiColumnLength = 20)
	@OAColumn(name = "exception", sqlType = java.sql.Types.CLOB)
	public String getException() {
		return exception;
	}

	public void setException(String newValue) {
		String old = exception;
		fireBeforePropertyChange(P_Exception, old, newValue);
		this.exception = newValue;
		firePropertyChange(P_Exception, old, this.exception);
	}

	@OAOne(displayName = "Status Info", reverseName = StatusInfo.P_StatusInfoMessages, required = true, isProcessed = true, allowCreateNew = false, allowAddExisting = false, fkeys = {
			@OAFkey(fromProperty = P_StatusInfoId, toProperty = StatusInfo.P_Id) })
	public StatusInfo getStatusInfo() {
		if (statusInfo == null) {
			statusInfo = (StatusInfo) getObject(P_StatusInfo);
		}
		return statusInfo;
	}

	public void setStatusInfo(StatusInfo newValue) {
		StatusInfo old = this.statusInfo;
		fireBeforePropertyChange(P_StatusInfo, old, newValue);
		this.statusInfo = newValue;
		firePropertyChange(P_StatusInfo, old, this.statusInfo);
	}

	@OAProperty(isFkeyOnly = true)
	@OAColumn(name = "status_info_id")
	public Integer getStatusInfoId() {
		return (Integer) getFkeyProperty(P_StatusInfoId);
	}

	public void setStatusInfoId(Integer newValue) {
		this.statusInfo = null;
		setFkeyProperty(P_StatusInfoId, newValue);
	}

	@OAOne(displayName = "Status Info", reverseName = StatusInfo.P_StatusInfoActivityMessages, isProcessed = true, allowCreateNew = false, allowAddExisting = false, fkeys = {
			@OAFkey(fromProperty = P_StatusInfoActivityId, toProperty = StatusInfo.P_Id) })
	@OALinkTable(name = "status_info_status_info_message", indexName = "status_info_status_info_activity_message", columns = {
			"status_info_message_id" })
	private StatusInfo getStatusInfoActivity() {
		// oamodel has createMethod set to false, this method exists only for annotations.
		return null;
	}

	@OAOne(displayName = "Status Info", reverseName = StatusInfo.P_StatusInfoAlertMessages, isProcessed = true, allowCreateNew = false, allowAddExisting = false, fkeys = {
			@OAFkey(fromProperty = P_StatusInfoAlertId, toProperty = StatusInfo.P_Id) })
	@OALinkTable(name = "status_info_status_info_message2", indexName = "status_info_status_info_alert_message", columns = {
			"status_info_message_id" })
	private StatusInfo getStatusInfoAlert() {
		// oamodel has createMethod set to false, this method exists only for annotations.
		return null;
	}
}
