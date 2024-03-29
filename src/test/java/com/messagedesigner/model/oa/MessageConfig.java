// Generated by OABuilder
package com.messagedesigner.model.oa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import com.messagedesigner.model.oa.Message;
import com.messagedesigner.model.oa.MessageConfig;
import com.messagedesigner.model.oa.RpgMessage;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAColumn;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OAMethod;
import com.viaoa.annotation.OAObjCallback;
import com.viaoa.annotation.OAProperty;
import com.viaoa.annotation.OATable;
import com.viaoa.datasource.OADataSource;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCallback;
import com.viaoa.util.OADateTime;

@OAClass(lowerName = "messageConfig", pluralName = "MessageConfigs", shortName = "msc", displayName = "Message Config", displayProperty = "id")
@OATable()
public class MessageConfig extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(MessageConfig.class.getName());

	public static final String P_Id = "id";
	public static final String P_Created = "created";
	public static final String P_Console = "console";
	public static final String P_RpgMessageDefinitionFile = "rpgMessageDefinitionFile";

	public static final String M_ClearMessages = "clearMessages";
	public static final String M_LoadJPOSND = "loadJPOSND";
	public static final String M_LoadJcomm = "loadJcomm";
	public static final String M_LoadRpgMessageFile = "loadRpgMessageFile";
	protected volatile int id;
	protected volatile OADateTime created;
	protected volatile String console;
	protected volatile String rpgMessageDefinitionFile;

	public MessageConfig() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
	}

	public MessageConfig(int id) {
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

	@OAProperty(displayLength = 30, columnLength = 20)
	public String getConsole() {
		return console;
	}

	public void setConsole(String newValue) {
		String old = console;
		fireBeforePropertyChange(P_Console, old, newValue);
		this.console = newValue;
		firePropertyChange(P_Console, old, this.console);
	}

	@OAProperty(displayName = "Rpg Message Definition File", maxLength = 125, displayLength = 22, columnLength = 20, isFileName = true)
	@OAColumn(maxLength = 125)
	/**
	 * Load csv file from Doug's output.<br>
	 * <br>
	 * RECORD_CODE,COLUMN_NAME,ORDINAL_POSITION,COLUMN_HEADING,DATA_TYPE,LENGTH,NUMERIC_SCALE,STORAGE,COLUMN_TEXT
	 * <p style="margin-top:0">
	 * AC,RECCD,1,recordCode,CHAR,2,,2,
	 * </p>
	 * <p style="margin-top:0">
	 * <br>
	 * <br>
	 * </p>
	 */
	public String getRpgMessageDefinitionFile() {
		return rpgMessageDefinitionFile;
	}

	public void setRpgMessageDefinitionFile(String newValue) {
		String old = rpgMessageDefinitionFile;
		fireBeforePropertyChange(P_RpgMessageDefinitionFile, old, newValue);
		this.rpgMessageDefinitionFile = newValue;
		firePropertyChange(P_RpgMessageDefinitionFile, old, this.rpgMessageDefinitionFile);
	}

	@OAMethod(displayName = "Clear Messages")
	public void clearMessages() {
		// use this to run on server (remote)
		if (isRemoteAvailable()) {
			remote();
			return;
		}
		OADataSource ds = OADataSource.getDataSource(RpgMessage.class);
		setConsole("Deleting all RpgMessages");
		ds.deleteAll(RpgMessage.class);
		setConsole("Done deleting all RpgMessages");
		setConsole("Deleting all Messages");
		ds.deleteAll(Message.class);
		setConsole("Done deleting all Messages");
	}

	@OAMethod(displayName = "Load JPOSND")
	public void loadJPOSND() {
		// use this to run on server (remote)
		if (isRemoteAvailable()) {
			remote();
			return;
		}
	}

	@OAMethod(displayName = "Load Jcomm")
	public void loadJcomm() {
	}

	@OAMethod(displayName = "Load RPG CSV File")
	public void loadRpgMessageFile() {
		// use this to run on server (remote)
		if (isRemoteAvailable()) {
			remote();
			return;
		}
		// custom code here
		/*
		LoadMessageDefinitionCsvFile loader = new LoadMessageDefinitionCsvFile();
		try {
		    loader.load();
		} catch (Exception e) {
		    throw new RuntimeException("exception call loadRpgMessageFile", e);
		}
		*/
	}

	@OAObjCallback(enabledProperty = MessageConfig.P_RpgMessageDefinitionFile)
	public void loadRpgMessageFileCallback(OAObjectCallback cb) {
	}

	public void load(ResultSet rs, int id) throws SQLException {
		this.id = id;
		java.sql.Timestamp timestamp;
		timestamp = rs.getTimestamp(2);
		if (timestamp != null) {
			this.created = new OADateTime(timestamp);
		}
		this.rpgMessageDefinitionFile = rs.getString(3);
		if (rs.getMetaData().getColumnCount() != 3) {
			throw new SQLException("invalid number of columns for load method");
		}

		this.changedFlag = false;
		this.newFlag = false;
	}
}
