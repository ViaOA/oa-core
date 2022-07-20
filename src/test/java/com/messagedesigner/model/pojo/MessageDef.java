package com.messagedesigner.model.pojo;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.messagedesigner.model.pojo.MessageDef;
import com.messagedesigner.model.pojo.MessageGroupDef;
import com.messagedesigner.model.pojo.MessageRecordDef;

@JsonIdentityInfo(scope = MessageDef.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class MessageDef implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	protected int id;
	protected int source;
	public static final int SOURCE_Unknown = 0;
	public static final int SOURCE_Jcomm = 1;
	public static final int SOURCE_Jposnd = 2;
	public static final int SOURCE_Jtrsmt = 3;
	protected String name;
	protected String description;
	protected int commonColumnCount;

	protected ArrayList<MessageRecordDef> alMessageRecordDefs;
	protected ArrayList<MessageGroupDef> alMessageGroupDefs;

	public MessageDef() {
	}

	public MessageDef(int id) {
		this();
		setId(id);
	}

	public int getId() {
		return id;
	}

	public void setId(int newValue) {
		this.id = newValue;
	}

	public int getSource() {
		return source;
	}

	public void setSource(int newValue) {
		this.source = newValue;
	}

	public String getName() {
		return name;
	}

	public void setName(String newValue) {
		this.name = newValue;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String newValue) {
		this.description = newValue;
	}

	public int getCommonColumnCount() {
		return commonColumnCount;
	}

	public void setCommonColumnCount(int newValue) {
		this.commonColumnCount = newValue;
	}

	@JsonManagedReference
	public ArrayList<MessageRecordDef> getMessageRecordDefs() {
		if (alMessageRecordDefs == null) {
			alMessageRecordDefs = new ArrayList<MessageRecordDef>();
		}
		return alMessageRecordDefs;
	}

	@JsonManagedReference
	public ArrayList<MessageGroupDef> getMessageGroupDefs() {
		if (alMessageGroupDefs == null) {
			alMessageGroupDefs = new ArrayList<MessageGroupDef>();
		}
		return alMessageGroupDefs;
	}
}
