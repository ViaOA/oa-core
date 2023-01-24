package com.viaoa.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class PojoLinkFkey implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	// References to other objects
	// PojoLinkOne
	protected volatile PojoLinkOne pojoLinkOne;
	// PojoProperty
	protected volatile PojoProperty pojoProperty;

	public PojoLinkFkey() {
	}

	@JsonIgnore
	public PojoLinkOne getPojoLinkOne() {
		return pojoLinkOne;
	}

	public void setPojoLinkOne(PojoLinkOne newValue) {
		this.pojoLinkOne = newValue;
	}

	// @JsonIgnore
	public PojoProperty getPojoProperty() {
		return pojoProperty;
	}

	public void setPojoProperty(PojoProperty newValue) {
		this.pojoProperty = newValue;
	}

	@Override
	public String toString() {
		return "PojoLinkFkey [" +
				"]";
	}
}
