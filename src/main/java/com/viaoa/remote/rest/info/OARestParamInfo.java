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
package com.viaoa.remote.rest.info;

import java.util.ArrayList;

import com.viaoa.remote.rest.annotation.OARestParam;

/**
 * Holds metadata for a single parameter of a REST-annotated method. Instances
 * of this class are created during annotation scanning and describe how one
 * method parameter is mapped into the HTTP request.
 *
 * <h2>Parameter Metadata</h2>
 * <ul>
 *   <li>Java parameter index and reflected type information</li>
 *   <li>Parameter name (from annotation or reflection)</li>
 *   <li>Binding rule (path variable, query parameter, request body, form field)</li>
 *   <li>Property-path settings for OAObject serialization</li>
 *   <li>Formatting options and optional explicit serializer overrides</li>
 *   <li>Whether null is allowed and whether the param is required</li>
 * </ul>
 *
 * <h2>Usage During Invocation</h2>
 * At runtime this metadata is used by the REST client to:
 * <ul>
 *   <li>Insert values into the URL template.</li>
 *   <li>Append query string parameters.</li>
 *   <li>Serialize object values into JSON request bodies.</li>
 *   <li>Apply property-path scope rules for OAObject graphs.</li>
 * </ul>
 *
 * <p>
 * Application code never constructs or modifies {@code OARestParamInfo}
 * instances directly.
 * </p>
 *
 * @author vvia
 */
public class OARestParamInfo {

	/**
	 * The parameter name used for binding within the REST request.
	 * <p>
	 * This may come from an {@link OARestParam} annotation or be
	 * assigned during parameter processing. It is used for URL path
	 * tags, query parameters, form fields, and JSON property names
	 * depending on {@link #paramType}.
	 */
	public String name;

	/**
	 * Flag indicating whether a name has been explicitly assigned.
	 * <p>
	 * When {@code true}, the {@link #name} value originated from an
	 * annotation or explicit configuration; otherwise it may still be
	 * auto-derived or unused depending on the parameter type.
	 */
	public boolean bNameAssigned;

	/**
	 * The raw parameter class associated with this metadata.
	 * <p>
	 * Represents the reflected Java class of the method parameter,
	 * and is used when determining serialization rules, type
	 * validation, and mapping decisions.
	 */
	public Class rpParamClass;
	
	/**
	 * The original declared parameter class before any type
	 * normalization.
	 * <p>
	 * May represent array or list types or other container
	 * structures. Used to identify class-type categories such as
	 * {@code Array}, {@code List}, or {@code ByteArray}.
	 */
	public Class origParamClass; // could be array or list, etc

	/**
	 * The effective parameter class after processing.
	 * <p>
	 * Can be {@code null} if the parameter does not require explicit
	 * class resolution. Used for validation and serialization
	 * routines that depend on the final argument type.
	 */
	public Class paramClass; // could be null

	/**
	 * Optional formatting string applied when converting this
	 * parameter to a URL or form value.
	 * <p>
	 * Used primarily for number and date/time parameters.
	 */
	public String format;

	/**
	 * Logical classification of the parameter's data shape.
	 * <p>
	 * Differentiates simple types (String, Date, etc.) from arrays,
	 * lists, JSON structures, and specialized types such as
	 * {@code OARestInvokeInfo} and {@code ByteArray}.
	 */
	public ClassType classType;

	/**
	 * Optional list of property-path expressions used when this
	 * parameter contributes to an OAObject JSON body.
	 * <p>
	 * When present, limits serialization to the specified paths.
	 */
	public ArrayList<String> alIncludePropertyPaths;
	
	/**
	 * Depth limit for reference expansion when serializing OAObjects
	 * provided as this parameter.
	 * <p>
	 * A value greater than zero expands referenced objects up to the
	 * given depth.
	 */
	public int includeReferenceLevelAmount;

	/**
	 * Indicates how this parameter is bound into the HTTP request.
	 * <p>
	 * Examples include URL path variables, query name/value pairs,
	 * form fields, JSON body contributions, and OA-specific roles
	 * such as {@code OAObjectId} or {@code MethodReturnClass}.
	 */
	public OARestParam.ParamType paramType;

	/**
	 * Enumeration describing the logical class category for a
	 * parameter value.
	 * <p>
	 * Used to determine how the value is interpreted, validated,
	 * and serialized during request construction.
	 */
	public static enum ClassType {
		/**
		 * Indicates that no type classification has been assigned.
		 */
		Unassigned,
		
		/**
		 * Indicates that the parameter value is a {@link String}.
		 */
		String,
		
		/**
		 * Indicates a date-only value with no time component.
		 */
		Date,
		
		/**
		 * Indicates a date-time value (date + time).
		 */
		DateTime,
		
		/**
		 * Indicates a time-only value (no date component).
		 */
		Time,
		
		/**
		 * The parameter represents an array type.
		 */
		Array,
		
		/**
		 * The parameter represents a list type.
		 */
		List,
		
		/**
		 * The parameter is represented as a JSON tree node.
		 */
		JsonNode,
		
		/**
		 * The parameter is or resolves to an {@code OARestInvokeInfo}
		 * instance used to control request construction.
		 */
		OARestInvokeInfo,
		
		/**
		 * Indicates that the parameter represents a raw {@code byte[]}
		 * value.
		 * <p>
		 * Used for request bodies that send binary data. When present,
		 * this classification affects validation rules and takes
		 * precedence over JSON or form-body generation.
		 */
		ByteArray
	}

}