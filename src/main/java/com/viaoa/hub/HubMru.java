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

import com.viaoa.object.OAObject;

/**
 * MRU "most recently used", Used to have two hubs use the same objects, where the 2nd Hub moves the AO to the top (0) position.
 */
public class HubMru<T extends OAObject> extends HubCopy<T> {

	public HubMru(Hub<T> hubMaster, Hub<T> hubCopy) {
		super(hubMaster, hubCopy, false);

		HubListener hlHubMaster = new HubListenerAdapter<T>(this, "HubMru", "") {
			@Override
			public void afterChangeActiveObject(HubEvent<T> e) {
				updateMru();
			}
		};
		hubMaster.addHubListener(hlHubMaster);
		updateMru();
	}

	protected void updateMru() {
		Object obj = hubMaster.getAO();
		if (obj == null) {
			return;
		}
		Hub h = getHub();
		if (h == null) {
			return;
		}

		int pos = h.getPos(obj);
		if (pos > 0) {
			getHub().move(pos, 0);
			getHub().setPos(0);
		}
	}

}
