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

/**
 * Describes OAObject information.
 *
 * @author vvia
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface OAClass {

	String shortName() default "";

	String pluralName() default "";

	String lowerName() default "";

	String displayName() default "";

	String description() default "";

	boolean isLookup() default false;

	boolean isPreSelect() default false;

	/** @return flag used to determine if object can be stored to datasource. */
	boolean useDataSource() default true;

	/** @return flag to know if objects should be added to cache */
	boolean addToCache() default true;

	/** @return if true, then changes are not sent to sever. */
	boolean localOnly() default false;

	/** @return if false, then objects are not initialized on creation */
	boolean initialize() default true;

	String displayProperty() default "";

	String sortProperty() default "";

	//String[] searchProperties() default {};
	String[] viewProperties() default {};

	long estimatedTotal() default 0;

	Class[] filterClasses() default {};

	// property path from a root class to this class.
	String[] rootTreePropertyPaths() default {};

	/** flag to know if this is processed and will require User.editProcessed=true for it to be changed. */
	boolean isProcessed() default false;

	String softDeleteProperty() default "";

	String softDeleteReasonProperty() default "";

	String versionProperty() default "";

	String versionLinkProperty() default "";

	String timeSeriesProperty() default "";

    String freezeProperty() default "";
	
	boolean singleton() default false;

	boolean pojoSingleton() default false;

	boolean noPojo() default false;
	
	boolean jsonUsesCapital() default false;  // JSON names use a capital letter (titled case)
}

/* used by OAObjectInfo
    protected boolean bUseDataSource = true;
    protected boolean bLocalOnly = false;  // dont send to OAServer
    protected boolean bAddToCache = true;  // add object to Cache
    protected boolean bInitializeNewObjects = true;  // initialize object properties (used by OAObject)
    protected String displayName;



*/