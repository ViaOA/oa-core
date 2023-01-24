package com.viaoa.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class PojoRegularProperty implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	// References to other objects
	// Pojo
	protected volatile Pojo pojo;
	// PojoProperty
	protected volatile PojoProperty pojoProperty;

	public PojoRegularProperty() {
	}

	@JsonIgnore
	public Pojo getPojo() {
		return pojo;
	}

	public void setPojo(Pojo newValue) {
		this.pojo = newValue;
	}

	public PojoProperty getPojoProperty() {
		return pojoProperty;
	}

	public void setPojoProperty(PojoProperty newValue) {
		this.pojoProperty = newValue;
	}

	@Override
	public String toString() {
		return "PojoRegularProperty [" +
				"]";
	}
}
