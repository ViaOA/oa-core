package com.viaoa.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class PojoLink implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	protected volatile String name;

	// References to other objects
	// Pojo
	protected volatile Pojo pojo;
	// PojoLinkMany
	protected volatile PojoLinkMany pojoLinkMany;
	// PojoLinkOne
	protected volatile PojoLinkOne pojoLinkOne;

	public PojoLink() {
	}

	public String getName() {
		return name;
	}

	public void setName(String newValue) {
		this.name = newValue;
	}

	@JsonIgnore
	public Pojo getPojo() {
		return pojo;
	}

	public void setPojo(Pojo newValue) {
		this.pojo = newValue;
	}

	//@JsonIgnore
	public PojoLinkMany getPojoLinkMany() {
		return pojoLinkMany;
	}

	public void setPojoLinkMany(PojoLinkMany newValue) {
		this.pojoLinkMany = newValue;
	}

	//@JsonIgnore
	public PojoLinkOne getPojoLinkOne() {
		return pojoLinkOne;
	}

	public void setPojoLinkOne(PojoLinkOne newValue) {
		this.pojoLinkOne = newValue;
	}

	@Override
	public String toString() {
		return "PojoLink [" +
				"name=" + name +
				"]";
	}
}
