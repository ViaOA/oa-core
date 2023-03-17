package com.viaoa.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class PojoProperty implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	protected volatile String name;
	protected volatile String upperName;
	protected volatile String propertyPath;
	protected volatile String javaType;
	protected volatile int keyPos;

	// References to other objects
	// PojoImportMatch
	protected volatile PojoImportMatch pojoImportMatch;
	// PojoLinkFkey
	protected volatile PojoLinkFkey pojoLinkFkey;
	// PojoLinkUnique
	protected volatile PojoLinkUnique pojoLinkUnique;
	// PojoRegularProperty
	protected volatile PojoRegularProperty pojoRegularProperty;

	public PojoProperty() {
	}

	public String getName() {
		return name;
	}

	public void setName(String newValue) {
		this.name = newValue;
	}

	public String getUpperName() {
		return upperName;
	}

	public void setUpperName(String newValue) {
		this.upperName = newValue;
	}

	public String getPropertyPath() {
		return propertyPath;
	}

	public void setPropertyPath(String newValue) {
		this.propertyPath = newValue;
	}

	public String getJavaType() {
		return javaType;
	}

	public void setJavaType(String newValue) {
		this.javaType = newValue;
	}

	public int getKeyPos() {
		return keyPos;
	}

	public void setKeyPos(int newValue) {
		this.keyPos = newValue;
	}

	@JsonIgnore
	public PojoImportMatch getPojoImportMatch() {
		return pojoImportMatch;
	}

	public void setPojoImportMatch(PojoImportMatch newValue) {
		this.pojoImportMatch = newValue;
	}

	@JsonIgnore
	public PojoLinkFkey getPojoLinkFkey() {
		return pojoLinkFkey;
	}

	public void setPojoLinkFkey(PojoLinkFkey newValue) {
		this.pojoLinkFkey = newValue;
	}

	@JsonIgnore
	public PojoLinkUnique getPojoLinkUnique() {
		return pojoLinkUnique;
	}

	public void setPojoLinkUnique(PojoLinkUnique newValue) {
		this.pojoLinkUnique = newValue;
	}

	@JsonIgnore
	public PojoRegularProperty getPojoRegularProperty() {
		return pojoRegularProperty;
	}

	public void setPojoRegularProperty(PojoRegularProperty newValue) {
		this.pojoRegularProperty = newValue;
	}

	@Override
	public String toString() {
		return "PojoProperty [" +
				"name=" + name +
				", upperName=" + upperName +
				", propertyPath=" + propertyPath +
				", javaType=" + javaType +
				"]";
	}
}
