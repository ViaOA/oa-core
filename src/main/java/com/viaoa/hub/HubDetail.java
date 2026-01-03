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

import com.viaoa.object.*;

/**
 * Internal class that defines the link between a master {@link Hub}
 * and its corresponding detail {@link Hub}.
 *
 * <p>{@code HubDetail} is created by {@link Hub#getDetail(String)} or
 * indirectly by a {@link HubMerger}.  It holds the metadata and linkage
 * information that connects two Hubs in a master–detail relationship,
 * ensuring that the detail Hub always reflects the contents of the
 * master Hub’s active object.</p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Record the relationship metadata:
 *       <ul>
 *         <li>The {@link OALinkInfo} describing the property from master
 *             to detail.</li>
 *         <li>The {@link Hub} references for both master and detail.</li>
 *         <li>The relationship type (Hub, array, OAObject, etc.).</li>
 *         <li>Reference-count tracking for reuse by other detail Hubs.</li>
 *       </ul>
 *   </li>
 *   <li>Handle special cases for recursive one-to-many links where the
 *       detail Hub can become disconnected from its master Hub.  
 *       The {@link #setup()} method installs a listener that reconnects
 *       the master Hub’s active object to the correct parent when
 *       necessary.</li>
 *   <li>Support {@link HubMerger} usage for hierarchical or flattened
 *       Hub compositions.</li>
 * </ul>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * Hub<Department> hubDept = new Hub<>(Department.class);
 * hubDept.select();
 * Hub<Employee> hubEmp = hubDept.getDetail("employees");
 * // hubEmp automatically follows the active Department
 * }</pre>
 *
 * <h3>Design Notes</h3>
 * <ul>
 *   <li>Not serialized with its parent Hub; it is reconstructed when the
 *       Hub graph is re-initialized.</li>
 *   <li>Manages recursive Hub wiring via {@link OAObjectInfoDelegate}
 *       and property-path introspection.</li>
 *   <li>Intended solely for internal OA use; application code should
 *       call {@link Hub#getDetail(String)} instead.</li>
 * </ul>
 */
public class HubDetail implements java.io.Serializable {
    static final long serialVersionUID = 1L;  // used for object serialization

    /**
     * Constant indicating that the detail value is an array type.
     */
    public final static int ARRAY = 0;

    /**
     * Constant indicating that the detail value is a Hub.
     */
    public final static int HUB = 1;

    /**
     * Constant indicating that the detail value is an OAObject.
     */
    public final static int OAOBJECT = 2;

    /**
     * Constant indicating that the detail value is a plain Object.
     */
    public final static int OBJECT = 3;

    /**
     * Constant indicating that the detail value is an array of OAObjects.
     */
    public final static int OAOBJECTARRAY = 4;

    /**
     * Constant indicating that the detail value is an array of Objects.
     */
    public final static int OBJECTARRAY = 5;

    /**
     * Constant indicating that the detail hub originates from a HubMerger.
     */
    public final static int HUBMERGER = 6;


    /**
     * Identifies the detail type for this HubDetail, based on the
     * predefined constants (ARRAY, HUB, OAOBJECT, etc.).
     */
    protected int type;

    /**
     * Property path used when this detail is created by a HubMerger,
     * defining how the detail hub is derived.
     */
    protected String path;

    /**
     * Indicates whether the detail hub should share its active object
     * with the linked master hub.
     */
    protected boolean bShareActiveObject;

    /**
     * Tracks how many consumers refer to this HubDetail instance,
     * allowing reuse where applicable.
     */
    protected int referenceCount;

    /**
     * Link information describing the master-to-detail relationship
     * for this HubDetail.
     */
    protected transient OALinkInfo liMasterToDetail;

    /**
     * The master Hub whose active object drives the content of the
     * associated detail Hub.
     */
    protected Hub hubMaster;

    /**
     * The detail Hub that follows the master Hub’s active object and
     * reflects its related objects.
     */
    protected Hub hubDetail;

    boolean bIgnoreUpdate;
    
	/**
	 * Constructs a new HubDetail instance linking a master hub to its
	 * corresponding detail hub. Initializes relationship metadata including
	 * link information, type classification, and the property path used
	 * for HubMerger scenarios. The detail wiring is completed by invoking
	 * {@link #setup()}.
	 *
	 * @param hubMaster        the master hub in the relationship
	 * @param hubDetail        the detail hub that follows the master hub
	 * @param liMasterToDetail the link information from master object to detail
	 * @param type             the detail value type (Hub, array, OAObject, etc.)
	 * @param path             optional property path used when employed by a HubMerger
	 */
    public HubDetail(Hub hubMaster, Hub hubDetail, OALinkInfo liMasterToDetail, int type, String path) {
        this.hubMaster = hubMaster;
        this.hubDetail = hubDetail;
        this.liMasterToDetail = liMasterToDetail;
        this.type = type;
        this.referenceCount = 0;
        this.path = path;
        setup();
    }

    /**
     * Constructs a HubDetail used specifically by a HubMerger. Associates a
     * detail hub with a property path and classifies its type as
     * {@code HUBMERGER}. No master hub or link information is assigned.
     *
     * @param path      the property path used by the merger
     * @param hubDetail the detail hub produced by the merger
     */
    public HubDetail(String path, Hub hubDetail) {
        this.hubDetail = hubDetail;
        this.path = path;
        this.type = HUBMERGER;
        this.referenceCount = 0;
    }
    

    /**
     * Installs recursive master/detail correction logic when the relationship
     * involves recursive one-to-many links. A hub listener is added to the
     * detail hub that detects when the detail hub’s active object becomes
     * disconnected from its master hub. When this occurs, the master hub’s
     * active object is reset to the correct parent object, unless updates
     * are being ignored.
     *
     * <p>Only applies when:
     * <ul>
     *   <li>A master hub exists.</li>
     *   <li>A detail hub exists.</li>
     *   <li>A master-to-detail link is defined.</li>
     *   <li>A recursive link is present on the detail class.</li>
     *   <li>The reverse link is a ONE-type reference.</li>
     * </ul>
     *
     * @return none
     */
    protected void setup() {
        if (hubMaster == null) return;
        if (hubDetail == null) return;
        if (liMasterToDetail == null) return;
        
        final OALinkInfo liRecursive = OAObjectInfoDelegate.getRecursiveLinkInfo(hubDetail.data.getObjectInfo(), OALinkInfo.ONE);
        if (liRecursive == null) return;
        if (liRecursive == liMasterToDetail) return;

        final OALinkInfo liDetailToMaster = liMasterToDetail.getReverseLinkInfo();
        if (liDetailToMaster == null) return;
        
        // 20150920 only if master is a one, not many
        if (liDetailToMaster.getType() != OALinkInfo.ONE) return;
        
        hubDetail.addHubListener(new HubListenerAdapter() {
            @Override
            public void afterChangeActiveObject(HubEvent e) {
                
                Object obj = e.getObject();
                if (!(obj instanceof OAObject)) return;

                Object parent = null;
                for (;;) {
                    Object objx = OAObjectReflectDelegate.getProperty((OAObject)obj, liDetailToMaster.getName());
                    if (objx == null) break;
                    parent = objx;
                    if (!(parent instanceof OAObject)) {
                        return; 
                    }
                    if (liDetailToMaster != liRecursive) {
                        break;
                    }
                    if (hubMaster.contains(parent)) break; 
                    obj = objx;
                }
                
                if (hubMaster.getAO() == parent) return;

                try {
                    bIgnoreUpdate = true;
                    hubMaster.setAO(parent);
                }
                finally {
                    bIgnoreUpdate = false;
                }
            }
        });
    }

    
	public Hub getDetailHub() {
		return hubDetail;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public boolean getShareActiveObject() {
		return bShareActiveObject;
	}

	public void setShareActiveObject(boolean bShareActiveObject) {
		this.bShareActiveObject = bShareActiveObject;
	}

	public int getReferenceCount() {
		return referenceCount;
	}
	public void incrementReferenceCount() {
		referenceCount++;
	}
	public void decrementReferenceCount() {
		referenceCount--;
	}

	public void setReferenceCount(int referenceCount) {
		this.referenceCount = referenceCount;
	}

	public OALinkInfo getMasterToDetailLinkInfo() {
		return liMasterToDetail;
	}

	public void setMasterToDetailLinkInfo(OALinkInfo liMasterToDetail) {
		this.liMasterToDetail = liMasterToDetail;
	}

	public Hub getHubMaster() {
		return hubMaster;
	}

	public void setHubMaster(Hub hubMaster) {
		this.hubMaster = hubMaster;
	}

	public Hub getHubDetail() {
		return hubDetail;
	}

	public void setHubDetail(Hub hubDetail) {
		this.hubDetail = hubDetail;
	}

	public boolean getIgnoreUpdate() {
		return bIgnoreUpdate;
	}

	public void setIgnoreUpdate(boolean bIgnoreUpdate) {
		this.bIgnoreUpdate = bIgnoreUpdate;
	}
	
}
	
