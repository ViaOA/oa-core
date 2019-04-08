/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.viaoa.hub;

import com.viaoa.object.*;


/**
 * This works similar to a HubDetail, except that it will first check to see if the data is loaded.
 * If not, it will load using swingWorker thread before setting the detail Hub
 * @author vvia
 */
public class HubJfcDetail {
    private Hub hubMaster;
    private Hub hubDetail;
    private String prop;
    private OALinkInfo li;

    public HubJfcDetail(Hub hubMaster, Hub hubDetail, String prop) {
        this.hubMaster = hubMaster;
        this.hubDetail = hubDetail;
        this.prop = prop;
        setup();
    }

    protected void setup() {
        OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(hubMaster.getObjectClass());
        this.li = oi.getLinkInfo(prop);
        this.li = this.li.getReverseLinkInfo();
        HubListener hl = new HubListenerAdapter() {
            @Override
            public void afterChangeActiveObject(HubEvent e) {
                update();
            }
        };
        hubMaster.addListener(hl);
        update();
    }

    protected void update() {
        final OAObject obj = (OAObject) hubMaster.getAO();
     
        if (obj == null) {
            hubDetail.setSharedHub(null);
            hubDetail.datam.setMasterHub(hubMaster);
            hubDetail.datam.liDetailToMaster = li;
            return;
        }
        if (obj.isLoaded(prop)) {
            hubDetail.setSharedHub(((Hub) obj.getProperty(prop)), false);
            hubDetail.datam.setMasterHub(hubMaster);
            return;
        }
        hubDetail.setSharedHub(null);

        javax.swing.SwingWorker<Void, Void> sw = new javax.swing.SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                obj.getProperty(prop);
                return null;
            }

            @Override
            protected void done() {
                if (obj != hubMaster.getAO()) return;
                Hub h = (Hub) obj.getProperty(prop);
                hubDetail.setSharedHub(h, false);
                hubDetail.datam.setMasterHub(hubMaster);
                hubDetail.datam.liDetailToMaster = li;
            }
        };
        sw.execute();
    }

}
