package com.oreillyauto.dev.tool.messagedesigner.model.pojo;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@JsonIdentityInfo(scope = MessageRecordDef.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class MessageRecordDef implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	protected int id;
	protected int relationshipType;
	public static final int RELATIONSHIPTYPE_One = 0;
	public static final int RELATIONSHIPTYPE_ZeroOrOne = 1;
	public static final int RELATIONSHIPTYPE_ZeroOrMore = 2;
	public static final int RELATIONSHIPTYPE_OneOrMore = 3;

	protected RecordDef recordDef;

	public MessageRecordDef() {
	}

	public MessageRecordDef(int id) {
		this();
		setId(id);
	}

	public int getId() {
		return id;
	}

	public void setId(int newValue) {
		this.id = newValue;
	}

	public int getRelationshipType() {
		return relationshipType;
	}

	public void setRelationshipType(int newValue) {
		this.relationshipType = newValue;
	}

	public RecordDef getRecordDef() {
		return recordDef;
	}

	public void setRecordDef(RecordDef rd) {
		this.recordDef = rd;
	}

}
