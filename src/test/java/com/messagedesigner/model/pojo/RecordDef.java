package com.messagedesigner.model.pojo;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.messagedesigner.model.pojo.ColumnDef;
import com.messagedesigner.model.pojo.RecordDef;

@JsonIdentityInfo(scope = RecordDef.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class RecordDef implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	protected int id;
	protected String code;
	protected String subCode;
	protected String name;
	protected int repeatingCount;

	protected ArrayList<ColumnDef> alColumnDefs;
	protected ColumnDef subCodeColumn;

	public RecordDef() {
	}

	public RecordDef(int id) {
		this();
		setId(id);
	}

	public int getId() {
		return id;
	}

	public void setId(int newValue) {
		this.id = newValue;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String newValue) {
		this.code = newValue;
	}

	public String getSubCode() {
		return subCode;
	}

	public void setSubCode(String newValue) {
		this.subCode = newValue;
	}

	public String getName() {
		return name;
	}

	public void setName(String newValue) {
		this.name = newValue;
	}

	public int getRepeatingCount() {
		return repeatingCount;
	}

	public void setRepeatingCount(int newValue) {
		this.repeatingCount = newValue;
	}

	public ArrayList<ColumnDef> getColumnDefs() {
		if (alColumnDefs == null) {
			alColumnDefs = new ArrayList<ColumnDef>();
		}
		return alColumnDefs;
	}

	public ColumnDef getSubCodeColumn() {
		return subCodeColumn;
	}

	public void setSubCodeColumn(ColumnDef newValue) {
		this.subCodeColumn = newValue;
	}

}
