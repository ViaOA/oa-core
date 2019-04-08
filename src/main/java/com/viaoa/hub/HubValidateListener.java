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


/**
 * Validation listener that can be used for beforeXxx events.
 * Throw an exception to have the hub not perform the operation.
 * @author vvia
 *
 */
public class HubValidateListener extends HubListenerAdapter {
    
    @Override
    public void beforeAdd(HubEvent e) {
    }
    @Override
    public void beforeInsert(HubEvent e) {
    }
    @Override
    public void beforeDelete(HubEvent e) {
    }
    @Override
    public void beforeRemove(HubEvent e) {
    }
    @Override
    public void beforeRemoveAll(HubEvent e) {
    }
    @Override
    public void beforePropertyChange(HubEvent e) {
    }
    
    
}
