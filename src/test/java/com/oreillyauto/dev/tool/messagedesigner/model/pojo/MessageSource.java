package com.oreillyauto.dev.tool.messagedesigner.model.pojo;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class MessageSource implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	protected int id;
	protected LocalDateTime created;
	protected int source;
	public static final int SOURCE_Unknown = 0;
	public static final int SOURCE_Jcomm = 1;
	public static final int SOURCE_Jposnd = 2;
	public static final int SOURCE_Jtrsmt = 3;
	protected String console;
	protected String rpgMessageDefinitionFile;
	protected String commonApiRootDirectory;

	// References to other objects.
	protected ArrayList<MessageTypeRecord> alMessageTypeRecords;
	protected ArrayList<MessageType> alMessageTypes;

	public MessageSource() {
	}

	public MessageSource(int id) {
		this();
		setId(id);
	}

	public int getId() {
		return id;
	}

	public void setId(int newValue) {
		this.id = newValue;
	}

	public LocalDateTime getCreated() {
		return created;
	}

	public void setCreated(LocalDateTime newValue) {
		this.created = newValue;
	}

	public int getSource() {
		return source;
	}

	public void setSource(int newValue) {
		this.source = newValue;
	}

	public String getConsole() {
		return console;
	}

	public void setConsole(String newValue) {
		this.console = newValue;
	}

	public String getRpgMessageDefinitionFile() {
		return rpgMessageDefinitionFile;
	}

	public void setRpgMessageDefinitionFile(String newValue) {
		this.rpgMessageDefinitionFile = newValue;
	}

	public String getCommonApiRootDirectory() {
		return commonApiRootDirectory;
	}

	public void setCommonApiRootDirectory(String newValue) {
		this.commonApiRootDirectory = newValue;
	}

	public ArrayList<MessageTypeRecord> getMessageTypeRecords() {
		if (alMessageTypeRecords == null) {
			alMessageTypeRecords = new ArrayList<MessageTypeRecord>();
		}
		return alMessageTypeRecords;
	}

	public ArrayList<MessageType> getMessageTypes() {
		if (alMessageTypes == null) {
			alMessageTypes = new ArrayList<MessageType>();
		}
		return alMessageTypes;
	}

}
