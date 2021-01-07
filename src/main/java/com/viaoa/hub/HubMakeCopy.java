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
 * Make a copy of objects in hubMaster to hubCopy hubMaster.onNewList event.
 * <p>
 * ex: have the objects in one hub (hubMaster) populate a model.hubMultiSelect
 */
public class HubMakeCopy<T extends OAObject> {

	private Hub<T> hubMaster, hubCopy;

	public HubMakeCopy(Hub<T> hubMaster, Hub<T> hubCopy) {
		this.hubMaster = hubMaster;
		this.hubCopy = hubCopy;
		setup();
	}

	protected void setup() {
		if (hubCopy == null || hubMaster == null) {
			return;
		}

		hubMaster.onNewList((e) -> {
			update();
		});
		update();
	}

	protected void update() {
		hubCopy.clear();
		hubCopy.add(hubMaster);
	}
}
