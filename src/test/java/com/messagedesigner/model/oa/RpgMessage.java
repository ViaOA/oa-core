// Generated by OABuilder
package com.messagedesigner.model.oa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import com.messagedesigner.model.oa.filter.RpgMessageErrorFilter;
import com.messagedesigner.model.oa.filter.RpgMessageOpenFilter;
import com.messagedesigner.model.oa.Message;
import com.messagedesigner.model.oa.MessageTypeRecord;
import com.messagedesigner.model.oa.RpgMessage;
import com.viaoa.annotation.OACalculatedProperty;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAColumn;
import com.viaoa.annotation.OAFkey;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OAIndex;
import com.viaoa.annotation.OAIndexColumn;
import com.viaoa.annotation.OAMethod;
import com.viaoa.annotation.OAOne;
import com.viaoa.annotation.OAProperty;
import com.viaoa.annotation.OATable;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAString;

@OAClass(lowerName = "rpgMessage", pluralName = "RpgMessages", shortName = "rpm", displayName = "Rpg Message", isProcessed = true, displayProperty = "messageTypeRecord", filterClasses = {
		RpgMessageOpenFilter.class, RpgMessageErrorFilter.class })
@OATable(indexes = {
		@OAIndex(name = "RpgMessageMessage", fkey = true, columns = { @OAIndexColumn(name = "MessageId") }),
		@OAIndex(name = "RpgMessageMessageTypeRecord", fkey = true, columns = { @OAIndexColumn(name = "MessageTypeRecordId") })
})
public class RpgMessage extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(RpgMessage.class.getName());

	public static final String P_Id = "id";
	public static final String P_Created = "created";
	public static final String P_Code = "code";
	public static final String P_Binary = "binary";
	public static final String P_Json = "json";
	public static final String P_Processed = "processed";
	public static final String P_Cancelled = "cancelled";
	public static final String P_Error = "error";

	public static final String P_Status = "status";
	public static final String P_BinaryDisplay = "binaryDisplay";

	public static final String P_Message = "message";
	public static final String P_MessageTypeRecord = "messageTypeRecord";

	public static final String M_Convert = "convert";
	protected volatile int id;
	protected volatile OADateTime created;
	protected volatile String code;
	protected volatile transient byte[] binary;
	protected volatile String json;
	protected volatile OADateTime processed;
	protected volatile OADateTime cancelled;
	protected volatile String error;

	// Links to other objects.
	protected volatile transient Message message;
	protected volatile transient MessageTypeRecord messageTypeRecord;

	public RpgMessage() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
	}

	public RpgMessage(int id) {
		this();
		setId(id);
	}

	@OAProperty(isUnique = true, trackPrimitiveNull = false, displayLength = 6)
	@OAId()
	@OAColumn(sqlType = java.sql.Types.INTEGER)
	public int getId() {
		return id;
	}

	public void setId(int newValue) {
		int old = id;
		fireBeforePropertyChange(P_Id, old, newValue);
		this.id = newValue;
		firePropertyChange(P_Id, old, this.id);
	}

	@OAProperty(defaultValue = "new OADateTime()", displayLength = 15, isProcessed = true)
	@OAColumn(sqlType = java.sql.Types.TIMESTAMP)
	public OADateTime getCreated() {
		return created;
	}

	public void setCreated(OADateTime newValue) {
		OADateTime old = created;
		fireBeforePropertyChange(P_Created, old, newValue);
		this.created = newValue;
		firePropertyChange(P_Created, old, this.created);
	}

	@OAProperty(maxLength = 4, displayLength = 4)
	@OAColumn(maxLength = 4)
	public String getCode() {
		return code;
	}

	public void setCode(String newValue) {
		String old = code;
		fireBeforePropertyChange(P_Code, old, newValue);
		this.code = newValue;
		firePropertyChange(P_Code, old, this.code);
	}

	@OAProperty(isBlob = true, columnLength = 6)
	@OAColumn(sqlType = java.sql.Types.BLOB)
	public byte[] getBinary() {
		if (binary == null) {
			binary = getBlob(P_Binary);
		}
		return binary;
	}

	public void setBinary(byte[] newValue) {
		byte[] old = binary;
		fireBeforePropertyChange(P_Binary, old, newValue);
		this.binary = newValue;
		firePropertyChange(P_Binary, old, this.binary);
	}

	@OAProperty(displayLength = 30, columnLength = 20)
	@OAColumn(sqlType = java.sql.Types.CLOB)
	public String getJson() {
		return json;
	}

	public void setJson(String newValue) {
		String old = json;
		fireBeforePropertyChange(P_Json, old, newValue);
		this.json = newValue;
		firePropertyChange(P_Json, old, this.json);
	}

	@OAProperty(displayLength = 15)
	@OAColumn(sqlType = java.sql.Types.TIMESTAMP)
	public OADateTime getProcessed() {
		return processed;
	}

	public void setProcessed(OADateTime newValue) {
		OADateTime old = processed;
		fireBeforePropertyChange(P_Processed, old, newValue);
		this.processed = newValue;
		firePropertyChange(P_Processed, old, this.processed);
	}

	@OAProperty(displayLength = 15)
	@OAColumn(sqlType = java.sql.Types.TIMESTAMP)
	public OADateTime getCancelled() {
		return cancelled;
	}

	public void setCancelled(OADateTime newValue) {
		OADateTime old = cancelled;
		fireBeforePropertyChange(P_Cancelled, old, newValue);
		this.cancelled = newValue;
		firePropertyChange(P_Cancelled, old, this.cancelled);
	}

	@OAProperty(maxLength = 250, displayLength = 25, columnLength = 20)
	@OAColumn(maxLength = 250)
	public String getError() {
		return error;
	}

	public void setError(String newValue) {
		String old = error;
		fireBeforePropertyChange(P_Error, old, newValue);
		this.error = newValue;
		firePropertyChange(P_Error, old, this.error);
	}

	@OACalculatedProperty(displayLength = 15, columnLength = 12, properties = { P_Processed, P_Error, P_Binary, P_MessageTypeRecord })
	public String getStatus() {
		if (getProcessed() != null) {
			return "Processed";
		}
		if (OAString.isNotEmpty(getError())) {
			return "Error";
		}
		if (getBinary() == null) {
			return "Binary is null";
		}
		if (getMessageTypeRecord() == null) {
			return "message type record not found";
		}
		return "Ready to process";
	}

	@OACalculatedProperty(displayName = "Binary Display", displayLength = 30, columnLength = 20, isHtml = true, properties = { P_Binary })
	public String getBinaryDisplay() {
		byte[] bs = this.getBinary();

		StringBuilder sb = new StringBuilder(600);
		sb.append("<html><tt><pre>");

		int max = bs == null ? 128 : bs.length;

		sb.append(OAString.getVerticalNumberLines(1, max));
		sb.append("</pre>");
		sb.append("<b><pre>");
		if (bs == null) {
			sb.append("binary is null");
		} else {
			sb.append(OAString.getVerticalHex(bs));
		}

		sb.append("\n\n");
		/*
		String s = RpgJsonConverter.getString(bs, 0, bs.length);
		int x = s.length();
		for (int i = 0; i < x; i++) {
		    char c = s.charAt(i);
		    int ic = (int) c;
		    if (c < 32) {
		        sb.append('?');
		    } else if (c > 127) {
		        sb.append('?');
		    } else if (c == ' ') {
		        sb.append('_');
		    } else {
		        sb.append(c);
		    }
		}
		*/
		// sb.append("[end]");
		sb.append("</pre></b></tt>");
		return sb.toString();
	}

	@OAOne(reverseName = Message.P_RpgMessages, isProcessed = true, allowCreateNew = false, allowAddExisting = false)
	@OAFkey(columns = { "MessageId" })
	public Message getMessage() {
		if (message == null) {
			message = (Message) getObject(P_Message);
		}
		return message;
	}

	public void setMessage(Message newValue) {
		Message old = this.message;
		fireBeforePropertyChange(P_Message, old, newValue);
		this.message = newValue;
		firePropertyChange(P_Message, old, this.message);
	}

	@OAOne(displayName = "Message Type Record", reverseName = MessageTypeRecord.P_RpgMessages2, allowCreateNew = false, isOneAndOnlyOne = true)
	@OAFkey(columns = { "MessageTypeRecordId" })
	public MessageTypeRecord getMessageTypeRecord() {
		if (messageTypeRecord == null) {
			messageTypeRecord = (MessageTypeRecord) getObject(P_MessageTypeRecord);
		}
		return messageTypeRecord;
	}

	public void setMessageTypeRecord(MessageTypeRecord newValue) {
		MessageTypeRecord old = this.messageTypeRecord;
		fireBeforePropertyChange(P_MessageTypeRecord, old, newValue);
		this.messageTypeRecord = newValue;
		firePropertyChange(P_MessageTypeRecord, old, this.messageTypeRecord);
	}

	@OAMethod(displayName = "Convert")
	public void convert() {
		byte[] bs = getBinary();
		if (bs == null) {
			return;
		}

		/*
		RpgJsonConverter conv = new RpgJsonConverter();
		String json = conv.convertToJson(bs);
		if (OAString.isEmpty(json)) {
			json = "could not convert RPG to JSON";
		}
		setJson(json);
		*/
	}

	public void load(ResultSet rs, int id) throws SQLException {
		this.id = id;
		java.sql.Timestamp timestamp;
		timestamp = rs.getTimestamp(2);
		if (timestamp != null) {
			this.created = new OADateTime(timestamp);
		}
		this.code = rs.getString(3);
		this.json = rs.getString(4);
		timestamp = rs.getTimestamp(5);
		if (timestamp != null) {
			this.processed = new OADateTime(timestamp);
		}
		timestamp = rs.getTimestamp(6);
		if (timestamp != null) {
			this.cancelled = new OADateTime(timestamp);
		}
		this.error = rs.getString(7);
		int messageFkey = rs.getInt(8);
		if (!rs.wasNull() && messageFkey > 0) {
			setProperty(P_Message, new OAObjectKey(messageFkey));
		}
		int messageTypeRecordFkey = rs.getInt(9);
		if (!rs.wasNull() && messageTypeRecordFkey > 0) {
			setProperty(P_MessageTypeRecord, new OAObjectKey(messageTypeRecordFkey));
		}
		if (rs.getMetaData().getColumnCount() != 9) {
			throw new SQLException("invalid number of columns for load method");
		}

		this.changedFlag = false;
		this.newFlag = false;
	}
}