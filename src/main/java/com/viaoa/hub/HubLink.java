/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viaoa.hub;


/**
 * Declarative utility that wires two {@link Hub}s together based on reference
 * properties or positional relationships.
 *
 * <p><b>Supported modes</b>:
 * <ol>
 *   <li>Active-object to reference property link.</li>
 *   <li>Hub position (index) to numeric property link.</li>
 *   <li>Property-to-property link.</li>
 *   <li>Auto-create links: instantiate new target objects on AO changes.</li>
 * </ol>
 *
 * <p>Acts as a convenience façade over {@link Hub#setLinkHub} and related APIs.</p>
 */
public class HubLink {
    
	/**
	 * Source Hub and destination Hub participating in the link relationship.
	 */
	private Hub fromHub, toHub;

	/**
	 * Property names used to synchronize values between the source and destination Hubs.
	 */
    private String fromProperty, toProperty;
    
    /**
     * Flag indicating whether the position of the active object in the fromHub
     * should be used when updating the destination Hub.
     */
    private boolean bUseHubPosition;
    
    /**
     * Flag indicating whether a new object should be automatically created in the
     * destination Hub when the active object in the source Hub changes.
     */
    private boolean bAutoCreate;

    /**
     * Creates a link where the active object in fromHub updates a property
     * in the active object of toHub.
     *
     * @param fromHub the source Hub
     * @param toHub the destination Hub
     * @param toProperty the property in the destination Hub to update
     */
    public HubLink(Hub fromHub, Hub toHub, String toProperty) {
        if (fromHub != null && toHub != null) fromHub.setLinkHub(toHub, toProperty);
        this.fromHub = fromHub;
        this.toHub = toHub;
        this.toProperty = toProperty;
    }

    /**
     * Creates a link that uses the position of the active object in fromHub
     * to update a numeric property in the active object of toHub.
     *
     * @param fromHub the source Hub
     * @param bUseHubPosition true to use the index of the active object
     * @param toHub the destination Hub
     * @param toProperty the property in the destination Hub to update
     */
    public HubLink(Hub fromHub, boolean bUseHubPosition, Hub toHub, String toProperty) {
        if (fromHub != null && toHub != null) fromHub.setLinkHub(toHub, toProperty);
        this.fromHub = fromHub;
        this.bUseHubPosition = bUseHubPosition;
        this.toProperty = toProperty;
    }

    /**
     * Creates a link where a property of the active object in fromHub
     * updates a property of the active object in toHub.
     *
     * @param fromHub the source Hub
     * @param fromProperty the property on the source Hub's active object
     * @param toHub the destination Hub
     * @param toProperty the property on the destination Hub's active object
     */
    public HubLink(Hub fromHub, String fromProperty, Hub toHub, String toProperty) {
        if (fromHub != null && toHub != null) fromHub.setLinkHub(toHub);
        this.fromHub = fromHub;
        this.toHub = toHub;
        this.fromProperty = fromProperty;
        this.toProperty = toProperty;
    }
    
    /**
     * Creates a link using the active object in fromHub to update the
     * active object in toHub.
     *
     * @param fromHub the source Hub
     * @param toHub the destination Hub
     */
    public HubLink(Hub fromHub, Hub toHub) {
        if (fromHub != null && toHub != null) fromHub.setLinkHub(toHub);
        this.fromHub = fromHub;
        this.toHub = toHub;
    }

    /**
     * Creates a link that optionally auto-creates a new object in toHub
     * when the active object in fromHub changes.
     *
     * @param fromHub the source Hub
     * @param toHub the destination Hub
     * @param toProperty property in the destination Hub to update
     * @param bAutoCreate true to auto-create a new object in toHub
     */
    public HubLink(Hub fromHub, Hub toHub, String toProperty, boolean bAutoCreate) {
        if (fromHub != null && toHub != null) {
        	fromHub.setLinkHub(toHub, toProperty, bAutoCreate);
        }
        this.fromHub = fromHub;
        this.toHub = toHub;
        this.toProperty = toProperty;
        this.bAutoCreate = bAutoCreate;
    }

    /**
     * Removes the linkHub association from fromHub before garbage collection.
     *
     * @throws Throwable if the superclass finalize throws an exception
     */
    protected void finalize() throws Throwable {
        if (fromHub != null) fromHub.removeLinkHub();
        super.finalize();
    }
    
    /**
     * Returns the source Hub participating in this link.
     *
     * @return the source Hub
     */
    public Hub getFromHub() {
        return fromHub;
    }
    
    /**
     * Returns the destination Hub participating in this link.
     *
     * @return the destination Hub
     */
    public Hub getToHub() {
        return toHub;
    }

    /**
     * Returns the property in the source Hub used for the link.
     *
     * @return property name in the source Hub, or null if the active object itself is used
     */
    public String getFromProperty() {
        return fromProperty;
    }

    /**
     * Returns the property in the destination Hub that is updated by the link.
     *
     * @return destination property name
     */
    public String getToProperty() {
        return toProperty;
    }

    /**
     * Returns whether the position of the active object in the source Hub
     * is used to update the destination Hub.
     *
     * @return true if the active object index is used
     */
    public boolean getUseHubPosition() {
        return bUseHubPosition;
    }

    /**
     * Returns whether new objects are automatically created in the destination Hub
     * when the active object in the source Hub changes.
     *
     * @return true if auto-create mode is enabled
     */
    public boolean getAutoCreate() {
        return bAutoCreate;
    }
}

