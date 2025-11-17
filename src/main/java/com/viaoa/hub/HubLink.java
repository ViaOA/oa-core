/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
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
    private Hub fromHub, toHub;
    private String fromProperty, toProperty;
    private boolean bUseHubPosition;
    private boolean bAutoCreate;

    /**
        Link a Hub to the active object of a property of the same Class in another Hub.
    */
    public HubLink(Hub fromHub, Hub toHub, String toProperty) {
        if (fromHub != null && toHub != null) fromHub.setLinkHub(toHub, toProperty);
        this.fromHub = fromHub;
        this.toHub = toHub;
        this.toProperty = toProperty;
    }

    /**
        Link the position of the active object in a Hub to a numeric property of the active object in another Hub.
        @param bUseHubPosition if true, then use the position of the object in the fromHub.
    */
    public HubLink(Hub fromHub, boolean bUseHubPosition, Hub toHub, String toProperty) {
        if (fromHub != null && toHub != null) fromHub.setLinkHub(toHub, toProperty);
        this.fromHub = fromHub;
        this.bUseHubPosition = bUseHubPosition;
        this.toProperty = toProperty;
    }

    /**
        Link the value of a property of the active object in a Hub to a property of the active object in another Hub.
        @param fromProperty property in fromHub to use.
        @param toProperty property in toHub to use.
    */
    public HubLink(Hub fromHub, String fromProperty, Hub toHub, String toProperty) {
        if (fromHub != null && toHub != null) fromHub.setLinkHub(toHub);
        this.fromHub = fromHub;
        this.toHub = toHub;
        this.fromProperty = fromProperty;
        this.toProperty = toProperty;
    }
    
    /**
        Link the active object in a Hub to a property of the active object in another Hub.
    */
    public HubLink(Hub fromHub, Hub toHub) {
        if (fromHub != null && toHub != null) fromHub.setLinkHub(toHub);
        this.fromHub = fromHub;
        this.toHub = toHub;
    }

    /**
        Used to automatically create a new Object in "to" Hub whenever the
        active object in "from" Hub is changed.
        @param bAutoCreate if true then a new object will be created and added to linkHub.
        @param toProperty is name of property in "to" Hub that will be set.
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
        Removes HubLink from toHub.
    */
    protected void finalize() throws Throwable {
        if (fromHub != null) fromHub.removeLinkHub();
        super.finalize();
    }
    
    /**
        Returns Hub that is linked to another Hub.
    */
    public Hub getFromHub() {
        return fromHub;
    }
    
    /**
        Returns Hub that is updated by toHub.
    */
    public Hub getToHub() {
        return toHub;
    }

    /**
        Returns the property name in fromHub that is linked to the active object of a property in the toHub.
        If null, then the actual object in the fromHub is used.
    */
    public String getFromProperty() {
        return fromProperty;
    }

    /**
        Returns property in toHub that is automatically update by linkHub.
    */
    public String getToProperty() {
        return toProperty;
    }

    /**
        Returns true if the position of the active object in the fromHub is used to update the
        property in the toHub.
    */
    public boolean getUseHubPosition() {
        return bUseHubPosition;
    }

    /**
        Returns true if a new object is created and added to the toHub when the active object in 
        the fromHub is changed.
    */
    public boolean getAutoCreate() {
        return bAutoCreate;
    }
}

