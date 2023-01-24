package com.viaoa.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class PojoLinkMany implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	// References to other objects
	// PojoLink
	protected volatile PojoLink pojoLink;

	public PojoLinkMany() {
	}

	@JsonIgnore
	public PojoLink getPojoLink() {
		return pojoLink;
	}

	public void setPojoLink(PojoLink newValue) {
		this.pojoLink = newValue;
	}

	@Override
	public String toString() {
		return "PojoLinkMany [" +
				"]";
	}
}
