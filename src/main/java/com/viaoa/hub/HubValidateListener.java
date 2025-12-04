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
 * Base validation listener for intercepting Hub operations before execution.
 * <p>
 * Subclasses can override `beforeAdd`, `beforeRemove`, etc., and throw exceptions
 * to veto the operation.
 *
 * <p>Provides a lightweight alternative to external validation frameworks.</p>
 */
public class HubValidateListener extends HubListenerAdapter {
    
	/**
	 * Called before an object is added to the Hub. Subclasses may override
	 * this method to validate or veto the add operation by throwing an
	 * exception.
	 *
	 * @param e the event describing the pending add
	 */
    @Override
    public void beforeAdd(HubEvent e) {
    }

    /**
     * Called before an object is inserted at a specific position in the Hub.
     * Subclasses can override to enforce validation rules or cancel the
     * insertion by throwing an exception.
     *
     * @param e the event describing the pending insert
     */
    @Override
    public void beforeInsert(HubEvent e) {
    }

    /**
     * Called before an object is permanently deleted from the Hub. Override
     * to perform confirmation checks or block deletion.
     *
     * @param e the event describing the pending delete
     */
    @Override
    public void beforeDelete(HubEvent e) {
    }
    
    /**
     * Called before an object is removed from the Hub. Subclasses may use
     * this to validate removal conditions or veto the action.
     *
     * @param e the event describing the pending removal
     */
    @Override
    public void beforeRemove(HubEvent e) {
    }
    
    /**
     * Called before all objects are removed from the Hub. Provides a hook to
     * validate bulk operations or restrict clearing the Hub entirely.
     *
     * @param e the event describing the pending remove-all
     */
    @Override
    public void beforeRemoveAll(HubEvent e) {
    }

    /**
     * Called before a property on an object within the Hub is changed.
     * Override to validate property updates or prevent the change by
     * throwing an exception.
     *
     * @param e the event describing the pending property change
     */
    @Override
    public void beforePropertyChange(HubEvent e) {
    }
}
