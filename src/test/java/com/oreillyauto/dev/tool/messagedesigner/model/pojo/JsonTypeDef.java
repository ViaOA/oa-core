package com.oreillyauto.dev.tool.messagedesigner.model.pojo;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@JsonIdentityInfo(scope = JsonTypeDef.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class JsonTypeDef implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	protected int id;
	protected String name;
	protected int type;
	public static final int TYPE_Unassigned = 0;
	public static final int TYPE_String = 1;
	public static final int TYPE_Number = 2;
	public static final int TYPE_Boolean = 3;
	public static final int TYPE_Array = 4;
	public static final int TYPE_DateTime = 5;
	public static final int TYPE_Date = 6;
	public static final int TYPE_Time = 7;
	public static final int TYPE_Timestamp = 8;
	public static final int TYPE_Other = 9;

	// References to other objects.

	public JsonTypeDef() {
	}

	public JsonTypeDef(int id) {
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

	public int getType() {
		return type;
	}

	public void setType(int newValue) {
		this.type = newValue;
	}

}
