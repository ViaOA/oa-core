// Generated by OABuilder
package com.oreillyauto.dev.tool.messagedesigner.model.oa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import com.oreillyauto.dev.tool.messagedesigner.delegate.oa.MessageTypeDelegate;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.filter.MessageTypeChangedFilter;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.filter.MessageTypeNotVerifiedFilter;
import com.viaoa.annotation.OACalculatedProperty;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAColumn;
import com.viaoa.annotation.OAFkey;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OAIndex;
import com.viaoa.annotation.OAIndexColumn;
import com.viaoa.annotation.OAMany;
import com.viaoa.annotation.OAMethod;
import com.viaoa.annotation.OAOne;
import com.viaoa.annotation.OAProperty;
import com.viaoa.annotation.OATable;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectKey;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAString;

@OAClass(lowerName = "messageType", pluralName = "MessageTypes", shortName = "mst", displayName = "Message Type", displayProperty = "display", sortProperty = "display", filterClasses = {
		MessageTypeChangedFilter.class, MessageTypeNotVerifiedFilter.class })
@OATable(indexes = {
		@OAIndex(name = "MessageTypeMessageSource", fkey = true, columns = { @OAIndexColumn(name = "MessageSourceId") })
})
public class MessageType extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(MessageType.class.getName());

	public static final String P_Id = "id";
	public static final String P_Created = "created";
	public static final String P_Name = "name";
	public static final String P_Description = "description";
	public static final String P_CommonColumnCount = "commonColumnCount";
	public static final String P_Note = "note";
	public static final String P_FollowUp = "followUp";
	public static final String P_Verified = "verified";
	public static final String P_Seq = "seq";

	public static final String P_HasChanges = "hasChanges";
	public static final String P_IsValid = "isValid";
	public static final String P_Display = "display";
	public static final String P_RecordCodes = "recordCodes";
	public static final String P_PojoCode = "pojoCode";
	public static final String P_CalcApiName = "calcApiName";

	public static final String P_MessageGroups = "messageGroups";
	public static final String P_MessageRecords = "messageRecords";
	public static final String P_Messages = "messages";
	public static final String P_MessageSource = "messageSource";
	public static final String P_MessageTypeChanges = "messageTypeChanges";

	public static final String M_VerifyCommand = "verifyCommand";
	protected volatile int id;
	protected volatile OADateTime created;
	protected volatile String name;
	protected volatile String description;
	protected volatile int commonColumnCount;
	protected volatile String note;
	protected volatile boolean followUp;
	protected volatile OADateTime verified;
	protected volatile int seq;

	// Links to other objects.
	protected transient Hub<MessageGroup> hubMessageGroups;
	protected transient Hub<MessageRecord> hubMessageRecords;
	protected volatile transient MessageSource messageSource;
	protected transient Hub<MessageTypeChange> hubMessageTypeChanges;

	public MessageType() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
	}

	public MessageType(int id) {
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

	@OAProperty(maxLength = 55, displayLength = 12)
	@OAColumn(maxLength = 55)
	public String getName() {
		return name;
	}

	public void setName(String newValue) {
		String old = name;
		fireBeforePropertyChange(P_Name, old, newValue);
		this.name = newValue;
		firePropertyChange(P_Name, old, this.name);
	}

	@OAProperty(maxLength = 250, displayLength = 20)
	@OAColumn(maxLength = 250)
	public String getDescription() {
		return description;
	}

	public void setDescription(String newValue) {
		String old = description;
		fireBeforePropertyChange(P_Description, old, newValue);
		this.description = newValue;
		firePropertyChange(P_Description, old, this.description);
	}

	@OAProperty(displayName = "Common Column Count", description = "number of leading columns that are the same in each record type", displayLength = 3, columnLength = 19)
	@OAColumn(sqlType = java.sql.Types.INTEGER)
	/**
	 * number of leading columns that are the same in each record type
	 */
	public int getCommonColumnCount() {
		return commonColumnCount;
	}

	public void setCommonColumnCount(int newValue) {
		int old = commonColumnCount;
		fireBeforePropertyChange(P_CommonColumnCount, old, newValue);
		this.commonColumnCount = newValue;
		firePropertyChange(P_CommonColumnCount, old, this.commonColumnCount);
	}

	@OAProperty(displayLength = 30, columnLength = 20, isHtml = true)
	@OAColumn(sqlType = java.sql.Types.CLOB)
	public String getNote() {
		return note;
	}

	public void setNote(String newValue) {
		String old = note;
		fireBeforePropertyChange(P_Note, old, newValue);
		this.note = newValue;
		firePropertyChange(P_Note, old, this.note);
	}

	@OAProperty(displayName = "Follow Up", displayLength = 5, columnLength = 9)
	@OAColumn(sqlType = java.sql.Types.BOOLEAN)
	public boolean getFollowUp() {
		return followUp;
	}

	public boolean isFollowUp() {
		return getFollowUp();
	}

	public void setFollowUp(boolean newValue) {
		boolean old = followUp;
		fireBeforePropertyChange(P_FollowUp, old, newValue);
		this.followUp = newValue;
		firePropertyChange(P_FollowUp, old, this.followUp);
	}

	@OAProperty(trackPrimitiveNull = false, displayLength = 15, isProcessed = true)
	@OAColumn(sqlType = java.sql.Types.TIMESTAMP)
	public OADateTime getVerified() {
		return verified;
	}

	public void setVerified(OADateTime newValue) {
		OADateTime old = verified;
		fireBeforePropertyChange(P_Verified, old, newValue);
		this.verified = newValue;
		firePropertyChange(P_Verified, old, this.verified);
	}

	@OAProperty(displayLength = 6, isAutoSeq = true)
	@OAColumn(sqlType = java.sql.Types.INTEGER)
	public int getSeq() {
		return seq;
	}

	public void setSeq(int newValue) {
		int old = seq;
		fireBeforePropertyChange(P_Seq, old, newValue);
		this.seq = newValue;
		firePropertyChange(P_Seq, old, this.seq);
	}

	@OACalculatedProperty(displayName = "Has Changes", displayLength = 5, columnLength = 11, properties = { P_MessageTypeChanges })
	public boolean getHasChanges() {
		boolean calcChanged;
		Hub<MessageTypeChange> hub = this.getMessageTypeChanges();
		return hub.getSize() > 0;
	}

	@OACalculatedProperty(displayName = "Is Valid", displayLength = 20)
	public String getIsValid() {
		// todo: qqqqqqqqq
		return null;
	}

	@OACalculatedProperty(displayLength = 15, properties = { P_Name,
			P_MessageRecords + "." + MessageRecord.P_MessageTypeRecord + "." + MessageTypeRecord.P_Code })
	public String getDisplay() {
		String display = "";
		String name = this.getName();
		display = OAString.concat(display, name, " ");
		display += "(";

		String codes = null;
		String code = null;
		for (MessageRecord messageRecord : getMessageRecords()) {
			MessageTypeRecord messageTypeRecord = messageRecord.getMessageTypeRecord();
			code = messageTypeRecord.getCode();
			codes = OAString.concat(codes, code, ", ");
		}
		if (codes != null) {
			display += codes;
		}
		display += ")";
		return display;
	}

	@OACalculatedProperty(displayName = "Record Codes", displayLength = 20, properties = {
			P_MessageRecords + "." + MessageRecord.P_MessageTypeRecord + "." + MessageTypeRecord.P_Code })
	public String getRecordCodes() {
		String recordCodes = "";
		String code = null;
		for (MessageRecord messageRecord : getMessageRecords()) {
			MessageTypeRecord messageTypeRecord = messageRecord.getMessageTypeRecord();
			code = messageTypeRecord.getCode();
			recordCodes = OAString.concat(recordCodes, code, " ");
		}

		return recordCodes;
	}

	@OACalculatedProperty(displayName = "Pojo Code", displayLength = 30, columnLength = 20, properties = { P_Name,
			P_MessageRecords + "." + MessageRecord.P_RelationshipType,
			P_MessageRecords + "." + MessageRecord.P_MessageTypeRecord + "." + MessageTypeRecord.P_PojoCode })
	public String getPojoCode() {
		String pojo = MessageTypeDelegate.getPojoCode(this);
		return pojo;
	}

	@OACalculatedProperty(displayName = "API Name", displayLength = 14, properties = { P_Name,
			P_MessageRecords + "." + MessageRecord.P_RelationshipType })
	public String getCalcApiName() {
		String s = OAString.mfcu(getName());
		if (OAString.isNotEmpty(s)) {
			return s;
		}

		if (getMessageRecords().size() != 1) {
			return null;
		}

		MessageRecord mr = getMessageRecords().getAt(0);
		MessageTypeRecord rec = mr.getMessageTypeRecord();
		if (mr.getRelationshipType() == MessageRecord.RELATIONSHIPTYPE_One) {
			return rec.getName();
		}
		return null;
	}

	@OAMany(displayName = "Message Groups", toClass = MessageGroup.class, owner = true, reverseName = MessageGroup.P_MessageType, cascadeSave = true, cascadeDelete = true)
	public Hub<MessageGroup> getMessageGroups() {
		if (hubMessageGroups == null) {
			hubMessageGroups = (Hub<MessageGroup>) getHub(P_MessageGroups);
		}
		return hubMessageGroups;
	}

	@OAMany(displayName = "Message Records", toClass = MessageRecord.class, owner = true, reverseName = MessageRecord.P_MessageType, cascadeSave = true, cascadeDelete = true, seqProperty = MessageRecord.P_Seq, sortProperty = MessageRecord.P_Seq)
	public Hub<MessageRecord> getMessageRecords() {
		if (hubMessageRecords == null) {
			hubMessageRecords = (Hub<MessageRecord>) getHub(P_MessageRecords);
		}
		return hubMessageRecords;
	}

	@OAMany(toClass = Message.class, reverseName = Message.P_MessageType, createMethod = false)
	private Hub<Message> getMessages() {
		// oamodel has createMethod set to false, this method exists only for annotations.
		return null;
	}

	@OAOne(displayName = "Message Source", reverseName = MessageSource.P_MessageTypes, required = true, allowCreateNew = false)
	@OAFkey(columns = { "MessageSourceId" })
	public MessageSource getMessageSource() {
		if (messageSource == null) {
			messageSource = (MessageSource) getObject(P_MessageSource);
		}
		return messageSource;
	}

	public void setMessageSource(MessageSource newValue) {
		MessageSource old = this.messageSource;
		fireBeforePropertyChange(P_MessageSource, old, newValue);
		this.messageSource = newValue;
		firePropertyChange(P_MessageSource, old, this.messageSource);
	}

	@OAMany(displayName = "Message Type Changes", toClass = MessageTypeChange.class, reverseName = MessageTypeChange.P_MessageType, isProcessed = true)
	public Hub<MessageTypeChange> getMessageTypeChanges() {
		if (hubMessageTypeChanges == null) {
			hubMessageTypeChanges = (Hub<MessageTypeChange>) getHub(P_MessageTypeChanges);
		}
		return hubMessageTypeChanges;
	}

	@OAMethod(displayName = "Verify Command")
	public void verifyCommand() {
		getMessageTypeChanges().deleteAll();
		setVerified(new OADateTime());
	}

	public void load(ResultSet rs, int id) throws SQLException {
		this.id = id;
		java.sql.Timestamp timestamp;
		timestamp = rs.getTimestamp(2);
		if (timestamp != null) {
			this.created = new OADateTime(timestamp);
		}
		this.name = rs.getString(3);
		this.description = rs.getString(4);
		this.commonColumnCount = (int) rs.getInt(5);
		if (rs.wasNull()) {
			OAObjectInfoDelegate.setPrimitiveNull(this, MessageType.P_CommonColumnCount, true);
		}
		this.note = rs.getString(6);
		this.followUp = rs.getBoolean(7);
		if (rs.wasNull()) {
			OAObjectInfoDelegate.setPrimitiveNull(this, MessageType.P_FollowUp, true);
		}
		timestamp = rs.getTimestamp(8);
		if (timestamp != null) {
			this.verified = new OADateTime(timestamp);
		}
		this.seq = (int) rs.getInt(9);
		if (rs.wasNull()) {
			OAObjectInfoDelegate.setPrimitiveNull(this, MessageType.P_Seq, true);
		}
		int messageSourceFkey = rs.getInt(10);
		if (!rs.wasNull() && messageSourceFkey > 0) {
			setProperty(P_MessageSource, new OAObjectKey(messageSourceFkey));
		}
		if (rs.getMetaData().getColumnCount() != 10) {
			throw new SQLException("invalid number of columns for load method");
		}

		this.changedFlag = false;
		this.newFlag = false;
	}
}