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

/*
 * Defines an OAObject relationship that is of type "Many"
 * example: @OAMany (clazz=Emp.class, owner=false, reverse=Emp.P_Dept, cascadeSave=false, cascadeDelete=false)
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OAMany {
	Class toClass() default Object.class;

	String displayName() default "";

	String description() default "";

	/** @return true if this object is the owner */
	boolean owner() default false;

	/** @return true if this is a recursive relationship. */
	boolean recursive() default false;

	/** @return name used in the toClass that refers to this class. */
	String reverseName() default "";

	/** @return true if saving this class will save the many objects */
	boolean cascadeSave() default false;

	/** @return true if deleting this class will delete the many objects */
	boolean cascadeDelete() default false;

	/** @return property name used to store the order position for each object in the Hub */
	String seqProperty() default "";

	String toolTip() default "";

	String help() default "";

	boolean hasCustomCode() default false;

	int cacheSize() default 0;

	/** @return true if there is a a method for this method. */
	boolean createMethod() default true;

	/** @return path to find another hub to use for autocreating objects in this hub. */
	String matchHub() default "";

	/** @return works with matchHub, to know what property should match the objects in the matchHub. */
	String matchProperty() default ""; // property that matchHub will use

	String matchStopProperty() default "";
	
	/** @return true if this must be empty (hub.size=0) to delete the other object */
	boolean mustBeEmptyForDelete() default false;

	/** @return true if this is a calculated Hub. */
	boolean isCalculated() default false;

	/** @return true if calc hub is to be done on server side. */
	boolean isServerSideCalc() default false;

	String uniqueProperty() default "";

	String sortProperty() default "";

	boolean sortAsc() default true;

	String[] calcDependentProperties() default {};

	String mergerPropertyPath() default "";

	boolean couldBeLarge() default false;

	/** flag to know if this is processed and will require User.editProcessed=true for it to be changed. */
	boolean isProcessed() default false;

	/**
	 * Name/Value (enum) property to automatch.
	 */
	String autoCreateProperty() default "";

	String equalPropertyPath() default "";

	String selectFromPropertyPath() default "";
}

/*  OALinkInfo

    public static final int ONE = 0;
    public static final int MANY = 1;

    String name;
    Class toClass;
    int type;
    boolean cascadeSave;  // save, delete of this object will do same with link hub
    boolean cascadeDelete;  // save, delete of this object will do same with link hub
    // property that needs to be updated in an inserted object.  same as Hub.propertyToMaster
    protected String reverseName;  // reverse property name
    boolean bOwner;  // this object is the owner of relationship
    private boolean bTransient;

    // runtime
    protected transient int cacheSize;
    protected OALinkInfo revLinkInfo;


*/