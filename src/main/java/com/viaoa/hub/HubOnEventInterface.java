package com.viaoa.hub;

import com.viaoa.object.OAObject;

/**
 * Used to create single event listeners for Hub events.
 *  
 * @author vvia
 *
 * @param <T>
 */
public interface HubOnEventInterface<T extends OAObject> {

    void onEvent(HubEvent<T> event);
}
