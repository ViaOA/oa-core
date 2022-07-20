package com.messagedesigner.model.pojo;

import java.time.LocalDateTime;

public class MessageConfig implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	protected int id;
	protected LocalDateTime created;
	protected String console;
	protected String rpgMessageDefinitionFile;

	public MessageConfig() {
	}

	public MessageConfig(int id) {
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

}
