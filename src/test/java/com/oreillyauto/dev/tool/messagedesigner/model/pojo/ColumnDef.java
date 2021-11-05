package com.oreillyauto.dev.tool.messagedesigner.model.pojo;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@JsonIdentityInfo(scope = ColumnDef.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class ColumnDef implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	protected int id;
	protected String name;
	protected String rpgName;
	protected int keyPos;
	protected int fromPos;
	protected int toPos;
	protected int size;
	protected String description;
	protected int decimalPlaces;
	protected String format;
	protected boolean notUsed;

	protected int specialType;
	public static final int SPECIALTYPE_Unknown = 0;
	public static final int SPECIALTYPE_NewInvoice = 1;
	public static final int SPECIALTYPE_InvoiceChange = 2;
	public static final int SPECIALTYPE_newPurchaseOrder = 3;
	public static final int SPECIALTYPE_purchaseOrderChange = 4;

	protected int nullValueType;
	public static final int NULLVALUETYPE_Default = 0;
	public static final int NULLVALUETYPE_Zero = 1;
	public static final int NULLVALUETYPE_Empty = 2;

	// References to other objects.
	protected RpgTypeDef rpgType;

	public ColumnDef() {
	}

	public ColumnDef(int id) {
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

	public String getRpgName() {
		return rpgName;
	}

	public void setRpgName(String newValue) {
		this.rpgName = newValue;
	}

	public int getKeyPos() {
		return keyPos;
	}

	public void setKeyPos(int newValue) {
		this.keyPos = newValue;
	}

	public int getFromPos() {
		return fromPos;
	}

	public void setFromPos(int newValue) {
		this.fromPos = newValue;
	}

	public int getToPos() {
		return toPos;
	}

	public void setToPos(int newValue) {
		this.toPos = newValue;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int newValue) {
		this.size = newValue;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String newValue) {
		this.description = newValue;
	}

	public int getDecimalPlaces() {
		return decimalPlaces;
	}

	public void setDecimalPlaces(int newValue) {
		this.decimalPlaces = newValue;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String newValue) {
		this.format = newValue;
	}

	public boolean getNotUsed() {
		return notUsed;
	}

	public void setNotUsed(boolean newValue) {
		this.notUsed = newValue;
	}

	public RpgTypeDef getRpgType() {
		return rpgType;
	}

	public void setRpgType(RpgTypeDef newValue) {
		this.rpgType = newValue;
	}

	public int getNullValueType() {
		return nullValueType;
	}

	public void setNullValueType(int newValue) {
		this.nullValueType = newValue;
	}

	public int getSpecialType() {
		return specialType;
	}

	public void setSpecialType(int newValue) {
		this.specialType = newValue;
	}
}
