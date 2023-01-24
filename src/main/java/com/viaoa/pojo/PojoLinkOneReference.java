package com.viaoa.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class PojoLinkOneReference implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	protected volatile String name;

	// References to other objects
	// PojoImportMatch
	protected volatile PojoImportMatch pojoImportMatch;
	// PojoLinkOne
	protected volatile PojoLinkOne pojoLinkOne;
	// PojoLinkUnique
	protected volatile PojoLinkUnique pojoLinkUnique;

	public PojoLinkOneReference() {
	}

	public String getName() {
		return name;
	}

	public void setName(String newValue) {
		this.name = newValue;
	}

	@JsonIgnore
	public PojoImportMatch getPojoImportMatch() {
		return pojoImportMatch;
	}

	public void setPojoImportMatch(PojoImportMatch newValue) {
		this.pojoImportMatch = newValue;
	}

	// @JsonIgnore
	public PojoLinkOne getPojoLinkOne() {
		return pojoLinkOne;
	}

	public void setPojoLinkOne(PojoLinkOne newValue) {
		this.pojoLinkOne = newValue;
	}

	@JsonIgnore
	public PojoLinkUnique getPojoLinkUnique() {
		return pojoLinkUnique;
	}

	public void setPojoLinkUnique(PojoLinkUnique newValue) {
		this.pojoLinkUnique = newValue;
	}

	@Override
	public String toString() {
		return "PojoLinkOneReference [" +
				"name=" + name +
				"]";
	}
}
