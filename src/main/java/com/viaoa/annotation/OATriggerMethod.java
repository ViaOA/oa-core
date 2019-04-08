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
package com.viaoa.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.viaoa.hub.HubEvent;

/**
 * Describes an OAObject trigger method.
 * 
 * This is used to avoid creating "far reaching" property path listeners, where a hub is listening to a propertyPath of objects.
 * Instead, a change to the end of a property path will then loop through and invoke the trigger for all objects that
 * are in the reversing of the property path.
 * Example:  an order discount is dependent on the emp.dep.company.discount
 * 
 * instead of each order listening to the propPath, oa will loop through all emps of a company if the discount is changed.
 * 
 * 
 * method signature will be:  
 *      public void nameTrigger(HubEvent hubEvent) 
 *      
 *  example:   
 *     &#64;OATriggerMethod(onlyUseLoadedData=true, runOnServer=true, runInBackgroundThread=true, properties= {"test"})
 *
 *  Note: the trigger method is not called if hub.isfetching, or oaobj.isLoading
 *
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME) 
public @interface OATriggerMethod {

    /** 
     * @return Property paths that will automatically call this method when the propPath is changed.
     */
    String[] properties() default {};
    
    
    /**
     * @return if true (default), then triggers are only made on objects that are in memory.
     */
    boolean onlyUseLoadedData() default true;
    
    /**
     * @return if true (default), then only run on the server.
     */
    boolean runOnServer() default true;
    
    /**
     * @return If true, then this will be ran in another thread. Otherwise false (default), run in the current thread.
     */
    boolean runInBackgroundThread() default false;
}
