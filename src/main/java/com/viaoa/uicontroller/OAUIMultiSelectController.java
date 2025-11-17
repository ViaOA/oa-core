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
package com.viaoa.uicontroller;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObjectCallback;
import com.viaoa.object.OAObjectCallbackDelegate;

/**
 * Controller for UI components that support multiple selection from a
 * {@link Hub}. The controller exposes the Hub contents to the view and
 * relies on {@link OAObjectCallback} rules to determine when the control
 * is enabled or visible.
 *
 * <p>
 * This is typically used for list or HTML-select style widgets where the
 * user can choose multiple OAObjects from a backing Hub. Actual selection
 * handling is performed by the view, while this controller manages the
 * high-level enabled/visible semantics.
 * </p>
 */
public class OAUIMultiSelectController extends OAUIBaseController {

    public OAUIMultiSelectController(Hub hubSelect) {
        super(hubSelect);
    }

    @Override
    public boolean isEnabled() {
        if (!super.isEnabled()) return false;
        OAObjectCallback eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(getHub());
        return eq.getAllowed();
    }
    
    @Override
    public boolean isVisible() {
        if (!super.isVisible()) return false;
        OAObjectCallback eq = OAObjectCallbackDelegate.getAllowVisibleObjectCallback(getHub());
        return eq.getAllowed();
    }

    
    /**
     * This can be used to get the confirm message before the actual new value is known.<br>
     * This is used to send a confirm message (javascript) to browser.
     */
    public OAObjectCallback getPreConfirmMessage() {
        OAObjectCallback cb = null; 
        //qqqqqq todo: need to confirm any add/remove
        return cb;
    }

    
}
