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
 * Defines a link of type ONE on an {@link OAObject}, describing a
 * many-to-one or one-to-one relationship as interpreted by OA.
 *
 * <p>The annotation encodes ownership, cascade rules, reverse link name,
 * creation rules, default values, calculation dependencies, and link
 * constraints used by {@link OALinkInfo} at runtime.</p>
 *
 * <p><b>Key Behavioral Metadata</b>
 * <ul>
 *   <li><b>Ownership</b>: controls whether this side creates/updates the target.</li>
 *   <li><b>Cascade rules</b>: cascadeSave, cascadeDelete.</li>
 *   <li><b>Reverse link</b>: the name of the {@code @OAMany} or {@code @OAOne}
 *       property in the target class.</li>
 *   <li><b>Creation flow</b>: allowCreateNew, autoCreateNew, allowAddExisting.</li>
 *   <li><b>Validation</b>: required, mustBeEmptyForDelete, defaultPropertyPath.</li>
 *   <li><b>Calculated link</b>: isCalculated, calcDependentProperties.</li>
 *   <li><b>Import/merge</b>: importMatch, equalPropertyPath.</li>
 * </ul>
 *
 * <p>This metadata defines how OA wires Hubs for link relationships and
 * how OAObjectGraph handles cascades and updates.</p>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OAOne {
	
	/**
	 * Provides the link name with the first character in lowercase,
	 * used for normalized metadata lookup and internal resolution.
	 */
	String lowerName() default "";

	/**
	 * Human-readable name for UI components that display this ONE link.
	 */
	String displayName() default "";

	/**
	 * Describes the purpose or semantics of this ONE relationship for
	 * user interfaces or documentation.
	 */
	String description() default "";

	/**
	 * Indicates whether this object owns the ONE relationship, affecting
	 * cascade and update rules.
	 */
	boolean owner() default false;

	/**
	 * Names the property on the target class that refers back to this
	 * class, forming the reverse link of the relationship.
	 */
	String reverseName() default "";

	/**
	 * Specifies whether this ONE reference is mandatory. If true, the
	 * property must not be null.
	 */
	boolean required() default false;

	/**
	 * Indicates whether the referenced object should be validated or
	 * verified before assignment.
	 */
	boolean verify() default false;

	/**
	 * Determines whether saving this object automatically saves the
	 * referenced ONE object.
	 */
	boolean cascadeSave() default false;

	/**
	 * Determines whether deleting this object automatically deletes the
	 * referenced ONE object.
	 */
	boolean cascadeDelete() default false;

	/**
	 * Indicates whether the referenced object is transient and should
	 * not be stored in the datasource.
	 */
	boolean isTransient() default false;

	/**
	 * If false, a new referenced object cannot be created and an
	 * existing object must be selected.
	 */
	boolean allowCreateNew() default true;

	/**
	 * If true, a new referenced object will be automatically created
	 * when this ONE link is accessed and found to be null.
	 */
	boolean autoCreateNew() default false;

	/**
	 * If false, prevents using an existing object for this ONE link,
	 * requiring creation of a new instance instead.
	 */
	boolean allowAddExisting() default true;

	/**
	 * If true, the ONE link must be null before the linked object may
	 * be deleted.
	 */
	boolean mustBeEmptyForDelete() default false;

	/**
	 * Tooltip text for UI components that present this ONE link.
	 */
	String toolTip() default "";

	/**
	 * Optional help or instructional text shown in UI contexts.
	 */
	String help() default "";

	/**
	 * Indicates whether custom code exists for methods associated with
	 * this ONE relationship to prevent generator overwrite.
	 */
	boolean hasCustomCode() default false;

	/**
	 * Marks this ONE relationship as calculated, meaning its value is
	 * derived dynamically rather than stored.
	 */
	boolean isCalculated() default false;

	/**
	 * Lists property paths whose changes trigger recalculation of this
	 * calculated ONE reference.
	 */
	String[] calcDependentProperties() default {};

	/**
	 * Indicates whether this ONE relationship has been marked as
	 * processed, requiring User.editProcessed=true for changes.
	 */
	boolean isProcessed() default false;

	/**
	 * Defines a property path used to supply the default value for this
	 * ONE reference.
	 */
	String defaultPropertyPath() default "";

	/**
	 * Indicates whether the {@code defaultPropertyPath} should be
	 * interpreted as a hierarchical path when locating the default value.
	 */
	boolean defaultPropertyPathIsHierarchy() default false;

	/**
	 * Determines whether the default value obtained from
	 * {@code defaultPropertyPath} can later be modified by the user.
	 */
	boolean defaultPropertyPathCanBeChanged() default false;

	/**
	 * Identifies a property path, evaluated on the context object, that
	 * provides the default reference. A value of "." indicates this object.
	 */
	String defaultContextPropertyPath() default "";

	/**
	 * Indicates that this ONE link is only valid if all other
	 * one-and-only-one links are null, enforcing exclusivity.
	 */
	boolean isOneAndOnlyOne() default false;

	/**
	 * Indicates that this ONE link is only valid if all other
	 * one-and-only-one links are null, enforcing exclusivity.
	 */
	boolean importMatch() default false;

	/**
	 * Defines a property path used to compare objects for equality
	 * during linking, merging, or import operations.
	 */
	String equalPropertyPath() default "";

	/**
	 * Property path that locates a Hub of objects which may be selected
	 * as candidates for this ONE reference.
	 */
	String selectFromPropertyPath() default "";

	/**
	 * Declares the foreign-key mappings used by this ONE relationship.
	 * Supersedes the older {@code pojoNames} mechanism.
	 */
	OAFkey[] fkeys() default {};

	/**
	 * Deprecated: legacy mapping of POJO names used to identify foreign
	 * keys. Replaced by {@code fkeys()}.
	 */
	@Deprecated 
	String[] pojoNames() default {};
}
