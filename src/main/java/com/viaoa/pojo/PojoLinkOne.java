package com.viaoa.pojo;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class PojoLinkOne implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	// References to other objects
	// PojoLink
	protected volatile PojoLink pojoLink;
	// PojoLinkUnique
	protected volatile PojoLinkUnique pojoLinkUnique;
	// PojoImportMatches
	protected volatile CopyOnWriteArrayList<PojoImportMatch> alPojoImportMatches = new CopyOnWriteArrayList<>();
	// PojoLinkFkeys
	protected volatile CopyOnWriteArrayList<PojoLinkFkey> alPojoLinkFkeys = new CopyOnWriteArrayList<>();

	public PojoLinkOne() {
	}

	@JsonIgnore
	public PojoLink getPojoLink() {
		return pojoLink;
	}

	public void setPojoLink(PojoLink newValue) {
		this.pojoLink = newValue;
	}

	// @JsonIgnore
	public PojoLinkUnique getPojoLinkUnique() {
		return pojoLinkUnique;
	}

	public void setPojoLinkUnique(PojoLinkUnique newValue) {
		this.pojoLinkUnique = newValue;
	}

	public CopyOnWriteArrayList<PojoImportMatch> getPojoImportMatches() {
		return alPojoImportMatches;
	}

	public void setPojoImportMatches(List<PojoImportMatch> list) {
		if (list == null) {
			this.alPojoImportMatches.clear();
		} else {
			this.alPojoImportMatches = new CopyOnWriteArrayList<>(list);
		}
	}

	public CopyOnWriteArrayList<PojoLinkFkey> getPojoLinkFkeys() {
		return alPojoLinkFkeys;
	}

	public void setPojoLinkFkeys(List<PojoLinkFkey> list) {
		if (list == null) {
			this.alPojoLinkFkeys.clear();
		} else {
			this.alPojoLinkFkeys = new CopyOnWriteArrayList<>(list);
		}
	}

	@Override
	public String toString() {
		return "PojoLinkOne [" +
				"]";
	}
}
