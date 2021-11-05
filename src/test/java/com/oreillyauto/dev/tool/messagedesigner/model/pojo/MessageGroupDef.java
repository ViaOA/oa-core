package com.oreillyauto.dev.tool.messagedesigner.model.pojo;

import java.util.ArrayList;

public class MessageGroupDef implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	protected int id;
	protected String name;

	// References to other objects.
	protected ArrayList<MessageRecordDef> alMessageRecordDefs;

	public MessageGroupDef() {
	}

	public MessageGroupDef(int id) {
		this();
		setId(id);
	}

	public int getId() {
		return id;
	}

	public void setId(int newValue) {
		this.id = newValue;
	}

	public String getName() {
		return name;
	}

	public void setName(String newValue) {
		this.name = newValue;
	}

	public ArrayList<MessageRecordDef> getMessageRecordDefs() {
		if (alMessageRecordDefs == null) {
			alMessageRecordDefs = new ArrayList<MessageRecordDef>();
		}
		return alMessageRecordDefs;
	}

}
