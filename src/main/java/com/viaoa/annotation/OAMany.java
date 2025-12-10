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
package com.viaoa.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a link of type MANY on an {@link OAObject}. This represents a
 * one-to-many or many-to-many relationship as managed by OA Hubs.
 *
 * <p>The annotation specifies the target class, ownership, cascade rules,
 * ordering, match automation, calculated Hubs, server-side calculation, 
 * and dependency paths.</p>
 *
 * <p><b>Key Areas</b>
 * <ul>
 *   <li><b>Relationship structure</b>: {@code toClass}, {@code reverseName},
 *       ownership, recursion.</li>
 *   <li><b>Cascade behavior</b>: cascadeSave, cascadeDelete.</li>
 *   <li><b>Hub behavior</b>: seqProperty, cacheSize, sortProperty, sortAsc.</li>
 *   <li><b>Auto-matching</b>: matchHub, matchProperty, autoCreateProperty.</li>
 *   <li><b>Calculated Hubs</b>: isCalculated, isServerSideCalc, dependent properties.</li>
 *   <li><b>Merge logic</b>: mergerPropertyPath.</li>
 *   <li><b>Delete rules</b>: mustBeEmptyForDelete.</li>
 * </ul>
 *
 * <p>This annotation drives how OA creates detail Hubs, wiring, linking,
 * and merge/sync behavior across the object graph.</p>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OAMany {

	/**
	 * Specifies the target class for this MANY relationship.
	 * Defines the object type contained within the generated Hub.
	 */
	Class toClass() default Object.class;

	/**
	 * Provides the relationship name with the first letter in lowercase,
	 * used for normalized lookups and metadata resolution.
	 */
    String lowerName() default "";
	
    /**
     * Human-readable name for UI components that present this
     * relationship.
     */
	String displayName() default "";

	/**
	 * Describes the purpose or meaning of this MANY relationship,
	 * primarily for UI or documentation use.
	 */
	String description() default "";

	/**
	 * Indicates whether this class owns the MANY relationship.
	 * Ownership affects cascading save/delete behavior.
	 */
	boolean owner() default false;

	/**
	 * Identifies whether this relationship is recursive, meaning the
	 * MANY side contains objects of the same type as the parent.
	 */
	boolean recursive() default false;

	/**
	 * Specifies the property name on the target class that refers back
	 * to this class, forming the reverse link of the relationship.
	 */
	String reverseName() default "";

	/**
	 * Determines whether saving this object will automatically save
	 * the objects contained in the MANY Hub.
	 */
	boolean cascadeSave() default false;

	/**
	 * Determines whether deleting this object will automatically delete
	 * the MANY objects associated with it.
	 */
	boolean cascadeDelete() default false;

	/**
	 * Specifies a property on the MANY objects used to maintain their
	 * ordering within the Hub.
	 */
	String seqProperty() default "";

	/**
	 * Tooltip text for UI components displaying this MANY relationship.
	 */
	String toolTip() default "";

	/**
	 * Optional help text that explains this relationship to end users
	 * in UI contexts.
	 */
	String help() default "";

	/**
	 * Indicates that custom code exists for this MANY relationship,
	 * informing generators or tools not to overwrite it.
	 */
	boolean hasCustomCode() default false;

	/**
	 * Configures the number of objects retained in the Hub’s cache,
	 * optimizing performance for large collections.
	 */
	int cacheSize() default 0;

	/**
	 * Specifies whether a convenience create-method should be generated
	 * for adding items to this MANY Hub.
	 */
	boolean createMethod() default true;

	/**
	 * Defines the property path to a Hub used for auto-creating objects
	 * based on matching logic.
	 */
	String matchHub() default "";

	/**
	 * Works with {@code matchHub}; identifies the property used to
	 * determine whether an object in the matchHub corresponds to one
	 * that should be auto-created in this Hub.
	 */
	String matchProperty() default ""; // property that matchHub will use

	/**
	 * Optional property used to stop match-based auto-creation logic
	 * once a condition is met.
	 */
	String matchStopProperty() default "";
	
	/**
	 * Indicates whether this MANY Hub must contain zero objects before
	 * the master object can be deleted.
	 */
	boolean mustBeEmptyForDelete() default false;

	/**
	 * Identifies the Hub as a calculated Hub whose contents are
	 * derived dynamically rather than stored.
	 */
	boolean isCalculated() default false;

	/**
	 * Indicates that the calculated Hub should be evaluated only on the
	 * server side rather than on the client.
	 */
	boolean isServerSideCalc() default false;

	/**
	 * Identifies a property on the MANY objects used to ensure that
	 * each object appears only once within the Hub.
	 */
	String uniqueProperty() default "";

	/**
	 * Specifies a property used to sort the MANY objects inside the
	 * Hub.
	 */
	String sortProperty() default "";

	/**
	 * Determines whether sorting of the MANY Hub is ascending (true)
	 * or descending (false).
	 */
	boolean sortAsc() default true;

	/**
	 * Lists property paths whose changes should trigger recalculation
	 * of this calculated Hub.
	 */
	String[] calcDependentProperties() default {};

	/**
	 * Defines the property path used by merge logic when combining or
	 * reconciling objects within the MANY Hub.
	 */
	String mergerPropertyPath() default "";

	/**
	 * Indicates that this MANY Hub may contain a large number of
	 * objects, allowing tools to optimize performance or memory usage.
	 */
	boolean couldBeLarge() default false;

	/**
	 * Marks this MANY relationship as processed, requiring a user to
	 * have {@code editProcessed=true} to modify it.
	 */
	boolean isProcessed() default false;

	/**
	 * Specifies a Name/Value (enum) property used for automatically
	 * creating objects in the Hub when matching criteria are met.
	 */
	String autoCreateProperty() default "";

	/**
	 * Property path used to compare objects for equality when adding or
	 * matching objects in this MANY Hub.
	 */
	String equalPropertyPath() default "";

	/**
	 * Property path to a Hub from which objects may be selected as
	 * candidates for inclusion in this MANY Hub.
	 */
	String selectFromPropertyPath() default "";
}

