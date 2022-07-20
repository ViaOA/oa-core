package com.messagedesigner.model.pojo;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.messagedesigner.model.pojo.JsonTypeDef;
import com.messagedesigner.model.pojo.RpgTypeDef;

@JsonIdentityInfo(scope = RpgTypeDef.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIdentityReference(alwaysAsId = false)
public class RpgTypeDef implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	protected int id;
	protected String name;
	protected int encodeType;
	public static final int ENCODETYPE_None = 0;
	public static final int ENCODETYPE_PackedDecimal = 1;
	public static final int ENCODETYPE_ZonedDecimal = 2;
	public static final int ENCODETYPE_Integer = 3;
	public static final int ENCODETYPE_Float = 4;
	protected int defaultSize;
	protected String defaultFormat;

	protected int nullValueType;
	public static final int NULLVALUETYPE_Default = 0;
	public static final int NULLVALUETYPE_Zero = 1;
	public static final int NULLVALUETYPE_Empty = 2;

	// References to other objects.
	protected JsonTypeDef jsonType;

	public RpgTypeDef() {
	}

	public RpgTypeDef(int id) {
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

	public int getEncodeType() {
		return encodeType;
	}

	public void setEncodeType(int newValue) {
		this.encodeType = newValue;
	}

	public int getNullValueType() {
		return nullValueType;
	}

	public void setNullValueType(int newValue) {
		this.nullValueType = newValue;
	}

	public int getDefaultSize() {
		return defaultSize;
	}

	public void setDefaultSize(int newValue) {
		this.defaultSize = newValue;
	}

	public String getDefaultFormat() {
		return defaultFormat;
	}

	public void setDefaultFormat(String newValue) {
		this.defaultFormat = newValue;
	}

	public JsonTypeDef getJsonType() {
		return jsonType;
	}

	public void setJsonType(JsonTypeDef newValue) {
		this.jsonType = newValue;
	}

}
