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

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.*;
import com.viaoa.object.*;

/**
	Internally used by Hub.dataUnique
	for unique settings/data for this Hub, that are not shared with Shared Hubs.
*/
class HubDataUniquex implements java.io.Serializable {
    static final long serialVersionUID = 1L;  // used for object serialization
	

	/** these options are not enforced on OAObjects, they are used to flag options */
    //	boolean allowNew = true, allowDelete = true, allowEdit = true;
	
	/**
	    Position of active object to set for new list. Can be set to 0 so that first object
	    is always made the active object whenever a new list is created.  Default is -1 (set to null).
	    <p>
	    This can be set for Detail Hubs, so that the first object is active whenever a new list
	    is create - which is when the master Hub changes its active object.
	*/
	protected transient int defaultPos = -1;
	
	/** Set ActiveObject to null when active object is removed.
	    Default is false which will go to next object, unless last object in Hub
	    is being removed, which will set it to previous object.
	*/
	protected transient boolean bNullOnRemove;
	
	/**
	    Hub Listeners that receives all Hub and OAObject events.
	    @see HubListener
	    @see HubEvent
	*/
	// protected transient Vector vecListener;
	
	// 20101218 replaces vetListener
	protected transient volatile HubListenerTree listenerTree;
	
    
	
	/**
	    Detail Hubs that this Hub has.
	    @see Hub#getDetail
	*/
	protected transient volatile Vector<HubDetail> vecHubDetail;
	
	/**
	    List of listeners for calculated properties.
	    The hub will automatically listen for changes to any property that a calculated property
	    is dependent on.
	    @see Hub#addListener(HubListener,String)
	*/
// 20101218 replaced by HubListenerTree	
//	transient Vector<HubCalcEventListener> calcEventListeners;
	
	
	/**
	    "Master" Hub that this Hub is linked to.
	    <p>
	    A hub can only be linked to a property in only one "master" hub.
	    It's active object will then reflect the value of this property in the "master"
	    hub's active object.  If the active object of the link hub changes, then the
	    property in the "master" hub will be set to the new object.
	    @see hub#setLink
	 */
	protected transient Hub linkToHub;
	
	
	/**
	    This can be used to set a property in the Link Hub to the value
	    of the position of the active object in this Hub.
	    <p>
	    Example: an object can have a property that is set to 0-9.  Another
	    Hub can be created with ten objects, and linked to this property.
	    Instead of setting the property to the object, it is set to the position
	    of the object.
	*/
	protected transient boolean linkPos;
	
	/**
	    Property that this Hub is linked to.
	*/
	protected transient String linkToPropertyName;  // ex: hubDept linked to Emp on property  "dept"
	
	/** Method used to get value of link to object. */
	protected transient Method linkToGetMethod;     //     getDept()
	
	/** Method used to set value of link to object. */
	protected transient Method linkToSetMethod;     //     setDept()
	
	
	/**
	    Links can also be set up so that a property in the link Hub is used to update
	    a property in the linkedTo/Master Hub.
	    <p>
	    LinkPropertyName is the name of the property that is used in the Linked Hub.
	    <p>
	    Example:
	    A Hub that has objects of type "State" can be linked to another Hub, so that the State.name
	    is linked to a property in the Master Hub.  The linkFromPropertyName is "name".
	
	*/
	protected transient String linkFromPropertyName;
	
	/**
	    Method that gets value from linkFromPropertyName.
	*/
	protected transient Method linkFromGetMethod;
	
	
	
	/**
	    Hub Listener used to update active object when the active object in
	    the Master Hub changes, or when the link property in the Master Hub is changed.
	*/
    protected transient HubLinkEventListener hubLinkEventListener;
	
	/**
	    Hub that this Hub is a sharing with.
	    Hubs can be set up so that they use (share) the same data.  The active object is
	    not shared, unless specified otherwise.
	    <p>
	    Detail Hubs that are using properties that are Hubs will use a shared Hub that is
	    changed whenever the active object in the Master Hub is changed.
	
	    @see Hub#createSharedHub
	    @see Hub#getDetail
	*/
	protected transient Hub sharedHub;
	
	
	/**
	    List of Hubs that are sharing the same objects as this Hub.  Each of these Hubs will
	    have the same HubData object.  If the active object is also being shared, then
	    the HubDataActive object will also be the same.
	*/
//	transient Vector vecSharedHub;

	// 20120715 replaces vecSharedHubs
	protected transient volatile WeakReference<Hub>[] weakSharedHubs;	
	
	
	/**
	    Hub used to add active object to whenever active object is changed in this Hub.
	    This can be used for building a pick list type program, where a user can select
	    objects that are then added to a list.
	*/
	protected transient Hub addHub;

    /**
        Used to automatically create a new object in the LinkTo Hub whenever
        the active object in Link Hub is changed.  The new object will then
        have its link property set.
    */
    protected transient boolean bAutoCreate;
    
    /**
     * If true and bAutoCreate, then new objects will be created.
     * If false and a new object with value already exists, then a new object will not be created
     *    and the current object will be set to AO
    */
    protected transient boolean bAutoCreateAllowDups;

}
