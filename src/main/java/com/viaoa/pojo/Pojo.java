package com.viaoa.pojo;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Root level maps an OAObject to a POJO.
 * <p>
 * See model OABuilderPojo.obx
 * 
 * @author vvia
 */
public class Pojo implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	protected volatile String name;

	// References to other objects
	// PojoLinks
	protected volatile CopyOnWriteArrayList<PojoLink> alPojoLinks = new CopyOnWriteArrayList<>();
	// PojoRegularProperties
	protected volatile CopyOnWriteArrayList<PojoRegularProperty> alPojoRegularProperties = new CopyOnWriteArrayList<>();

	public Pojo() {
	}

	public String getName() {
		return name;
	}

	public void setName(String newValue) {
		this.name = newValue;
	}

	public CopyOnWriteArrayList<PojoLink> getPojoLinks() {
		return alPojoLinks;
	}

	public void setPojoLinks(List<PojoLink> list) {
		if (list == null) {
			this.alPojoLinks.clear();
		} else {
			this.alPojoLinks = new CopyOnWriteArrayList<>(list);
		}
	}

	public CopyOnWriteArrayList<PojoRegularProperty> getPojoRegularProperties() {
		return alPojoRegularProperties;
	}

	public void setPojoRegularProperties(List<PojoRegularProperty> list) {
		if (list == null) {
			this.alPojoRegularProperties.clear();
		} else {
			this.alPojoRegularProperties = new CopyOnWriteArrayList<>(list);
		}
	}

	@Override
	public String toString() {
		return "Pojo [" +
				"name=" + name +
				"]";
	}
}
