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
 * Declares a read-only calculated property on an {@link OAObject}, including
 * its formatting, dependent property paths, and UI characteristics.
 *
 * <p>These properties are evaluated dynamically (often via 
 * {@code getXxx()} methods containing custom logic) and have no storage
 * in the underlying datasource.</p>
 *
 * <p><b>Key Responsibilities</b>
 * <ul>
 *   <li>Define dependent properties for automatic recalculation.</li>
 *   <li>Specify formatting and display hints (lengths, tooltips, enum mappings).</li>
 *   <li>Provide type hints (email, url, xml, timestamp, currency, etc.).</li>
 *   <li>Support UI rendering: columnLength, help text, outputFormat.</li>
 * </ul>
 *
 * <p>Used by {@link OAPropertyInfo} and the OA recalculation engine.</p>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OACalculatedProperty {
	
	/**
	 * Defines the lowercase form of the calculated property name.
	 * This can be used for normalization or lookup purposes when
	 * evaluating or referencing the property.
	 */
    String lowerName() default "";

    /**
     * Specifies a human-friendly display name for the calculated
     * property, typically used in UI labels or column headers.
     */
    String displayName() default "";

    /**
     * Provides an optional descriptive text explaining the purpose
     * or meaning of the calculated property.
     */
	String description() default "";

	/**
	 * Defines a formatting pattern applied when rendering the
	 * calculated property value, such as number or date formats.
	 */
	String outputFormat() default "";

	/**
	 * Lists dependent property paths that determine when this
	 * calculated property should be recalculated.
	 */
	String[] properties() default {};

	/**
	 * Suggests the display length used by UI components when showing
	 * the calculated property's value.
	 */
	int displayLength() default 0;

	/**
	 * Defines an optional maximum column width when the property is
	 * displayed in tabular UI components.
	 */
	int columnLength() default 0;

	/**
	 * Specifies the number of decimal places to apply when formatting
	 * numeric calculated property values.
	 */
	int decimalPlaces() default 0;

	/**
	 * Indicates that the calculated property represents an email
	 * address and may require validation or email-specific UI
	 * rendering.
	 */
	boolean isEmail() default false;

	/**
	 * Identifies the calculated property as representing a URL, which
	 * can influence validation rules and hyperlink rendering.
	 */
	boolean isUrl() default false;

	/**
	 * Marks the calculated property as containing an image filename,
	 * used by UI components that display images.
	 */
	boolean isImageName() default false;

	/**
	 * Marks the calculated property as containing an icon filename,
	 * used by UI components that display icons.
	 */
	boolean isIconName() default false;

	/**
	 * Indicates that the calculated property value is XML content,
	 * allowing for specialized formatting or validation.
	 */
	boolean isXml() default false;

	/**
	 * Indicates that the calculated property contains a filename and
	 * may need corresponding file-related behavior in UI or logic.
	 */
	boolean isFileName() default false;

	/**
	 * Identifies the calculated property as an automatically
	 * generated sequence value.
	 */
	boolean isAutoSeq() default false;

	/**
	 * Indicates that the calculated property value represents a
	 * timestamp and may require time-based formatting.
	 */
	boolean isTimestamp() default false;

	/**
	 * Specifies whether comparisons involving this calculated
	 * property's value should be performed in a case-sensitive manner.
	 */
	boolean isCaseSensitive() default false;

	/**
	 * Marks the calculated property as containing a phone number,
	 * enabling phone-specific formatting or validation.
	 */
	boolean isPhone() default false;

	/**
	 * Marks the calculated property as containing a ZIP code, allowing
	 * for ZIP-specific validation or formatting.
	 */
	boolean isZipCode() default false;

	/**
	 * Indicates that the calculated property represents a currency
	 * amount, typically requiring currency formatting rules.
	 */
	boolean isCurrency() default false;

	/**
	 * Indicates that the calculated property value contains HTML
	 * content, allowing UI components to render it accordingly.
	 */
	boolean isHtml() default false;

	/**
	 * Marks the calculated property as representing an object's status,
	 * enabling specialized display or formatting in UI components.
	 */
	boolean isObjectStatus() default false;
	
	/**
	 * Defines an optional explicit column name for UI or metadata
	 * purposes when displaying the calculated property.
	 */
	String columnName() default "";

	/**
	 * Supplies tooltip text for UI components that display this
	 * calculated property, offering additional context to users.
	 */
	String toolTip() default "";

	/**
	 * Provides optional help or guidance text associated with the
	 * calculated property, used by UI frameworks to assist users.
	 */
	String help() default "";

	/**
	 * Specifies the name of the enum-based property that should be
	 * used to map or interpret the calculated property's value.
	 */
	String enumPropertyName() default "";
}
