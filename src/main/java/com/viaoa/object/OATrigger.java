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
package com.viaoa.object;


/**
 * Defines a method to be called from a root class, when any change is made from an object in a propertyPath
 * 
 * see OATriggerDelegate# to have a trigger created. 
 */
public class OATrigger {
    protected String name;
    protected Class rootClass;
    protected String[] propertyPaths;
    protected OATriggerListener triggerListener;
    protected final boolean bOnlyUseLoadedData; 
    protected final boolean bServerSideOnly;
    protected final boolean bUseBackgroundThread;
    protected final boolean bUseBackgroundThreadIfNeeded;
    protected OATrigger[] dependentTriggers;
    
    public OATrigger(
        String name,
        Class rootClass,
        OATriggerListener triggerListener,
        String[] propertyPaths, 
        final boolean bOnlyUseLoadedData, 
        final boolean bServerSideOnly, 
        final boolean bUseBackgroundThread,
        final boolean bUseBackgroundThreadIfNeeded)
    {
        this.name = name;
        this.rootClass = rootClass;
        this.propertyPaths = propertyPaths;
        this.triggerListener = triggerListener;
        this.bOnlyUseLoadedData = bOnlyUseLoadedData;
        this.bServerSideOnly = bServerSideOnly;
        this.bUseBackgroundThread = bUseBackgroundThread;
        this.bUseBackgroundThreadIfNeeded = bUseBackgroundThreadIfNeeded;
    }
    
    public OATrigger(
        String name,
        Class rootClass,
        OATriggerListener triggerListener,
        String propertyPath, 
        final boolean bOnlyUseLoadedData, 
        final boolean bServerSideOnly, 
        final boolean bUseBackgroundThread,
        final boolean bUseBackgroundThreadIfNeeded)
    {
        this.name = name;
        this.rootClass = rootClass;
        this.propertyPaths = new String[] {propertyPath};
        this.triggerListener = triggerListener;
        this.bOnlyUseLoadedData = bOnlyUseLoadedData;
        this.bServerSideOnly = bServerSideOnly;
        this.bUseBackgroundThread = bUseBackgroundThread;
        this.bUseBackgroundThreadIfNeeded = bUseBackgroundThreadIfNeeded;
    }

    public OATrigger[] getDependentTriggers() {
        return dependentTriggers;        
    }
    public OATriggerListener getTriggerListener() {
        return triggerListener;
    }
}
