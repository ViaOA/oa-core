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
class HubDetail implements java.io.Serializable {
    static final long serialVersionUID = 1L;  // used for object serialization

    /** types of values. */
    public final static int ARRAY = 0;
    public final static int HUB = 1;
    public final static int OAOBJECT = 2;
    public final static int OBJECT = 3;
    public final static int OAOBJECTARRAY = 4;
    public final static int OBJECTARRAY = 5;
    public final static int HUBMERGER = 6;


    /** type of detail Hub, see static list above. */
    protected int type;

    protected String path; // added for use when using a HubMerger

    /**
        true if the property value is a Hub, and the detail hub should use the same active object
        as the Hub that it is sharing.  This is used to remember what object was active for the
        detail Hub.
    */
    protected boolean bShareActiveObject;

    /** number of references to this HubDetail. */
    protected int referenceCount;

    /** Information about the reference, from master to detail. */
    protected transient OALinkInfo liMasterToDetail;

    protected Hub hubMaster;
	protected Hub hubDetail;
    
    
    /**
        Used by Hub.getDetail() to create new Hub Detail.
        @param hub is master hub.
        @param linkInfo is from master object to detail property
        @param type of value in property.
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
	    Used by HubMerger
	*/
    HubDetail(String path, Hub hubDetail) {
        this.hubDetail = hubDetail;
        this.path = path;
        this.type = HUBMERGER;
        this.referenceCount = 0;
    }
    
    boolean bIgnoreUpdate;

    /** 20150119, 20160204
     *  this is for master.detail that are recursive, in cases where the detail hub could be
     *  pointing (shared) to a child hub, which leaves it disconnected from the masterHub.
     *  This is used by HubDetailDelegate.updateDetail(..), where it will be reconnected to the masterHub.
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
}
	
