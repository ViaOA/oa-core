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
package com.viaoa.remote.rest.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Describes how a method parameter is used by {@link com.viaoa.remote.rest.OARestClient}
 * when constructing an HTTP request for a remoted interface method.
 * This annotation must be applied to parameters of a remote interface, not the
 * implementation class.
 *
 * <h2>Usage</h2>
 * <p>
 * Each parameter is classified by a {@link ParamType}, which determines whether
 * the value is used as a path variable, query parameter, form value, search
 * clause input, OAObjectGraph identifier, JSON body fragment, byte-array body,
 * HTTP header, cookie, or response include rule.
 * </p>
 *
 * <h2>Include Rules</h2>
 * <p>
 * The {@code includePropertyPath(s)} and {@code includeReferenceLevelAmount}
 * fields allow fine-grained control over how OAObjects are serialized on the
 * server when this parameter is transmitted as part of the request.
 * </p>
 *
 * <h2>Name Resolution</h2>
 * <p>
 * If the Java compiler does not preserve parameter names, the {@code name}
 * attribute should be supplied explicitly for any parameter used in URL or
 * query composition.
 * </p>
 *
 * @see com.viaoa.remote.rest.annotation.OARestMethod
 * @author vvia
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface OARestParam {

	/**
	 * Specifies the parameter name used for URL and query composition.
	 * If omitted, the Java parameter name is used (when available).
	 *
	 * @return the explicit parameter name or empty string if none is supplied
	 */
	String name() default "";

	/**
	 * Overrides the Java type of the parameter when needed for coercion,
	 * return-class resolution, or special handling rules.
	 *
	 * @return the explicitly defined parameter class
	 */
	Class paramClass() default Void.class;

	/**
	 * Optional formatting rule for date/time parameters or other values
	 * requiring special serialization.
	 *
	 * @return the format string or empty if none is defined
	 */
	String format() default "";

	/**
	 * Single property path to include when serializing this parameter’s value
	 * for OAObjectGraph operations.
	 *
	 * @return the property path to include
	 */
	String includePropertyPath() default ""; // supported by OARestServlet

	/**
	 * Multiple property paths to include when serializing this value for
	 * OAObjectGraph operations.
	 *
	 * @return an array of property paths
	 */
	String[] includePropertyPaths() default {}; // supported by OARestServlet

	/**
	 * Indicates how many reference levels to include when serializing this value.
	 *
	 * @return the number of reference levels to include
	 */
	int includeReferenceLevelAmount() default 0; // supported by OARestServlet

	/**
	 * Defines how this parameter contributes to the REST request. Determines
	 * whether the parameter is used as a URL value, query parameter, form field,
	 * body content, OAObject identifier, paging parameter, etc.
	 *
	 * @return the classification of the parameter
	 */
	ParamType type() default ParamType.Unassigned;

	/**
	 * Enumerates the allowed behaviors for annotated parameters, defining how
	 * each contributes to request construction in {@code OARestClient}.
	 */
	public static enum ParamType {
		/**
		 * Default type applied when no explicit behavior is declared. Behavior will
		 * be inferred based on method type, or verification will fail.
		 */
		Unassigned,

		/**
		 * Indicates that this parameter should be ignored during request construction.
		 */
		Ignore,

		/*
		 * use the value of this param to be the RestMethod.urlPath
		 * <p>
		 * verify: RestMethod.urlPath is empty, and value is a String
		 */
		/**
		 * Uses the parameter value as the full URL path for the REST method.
		 * Requires the method’s {@code urlPath} to be empty.
		 */
		MethodUrlPath,

		/*
		 * use the value of this param to be the Method's RestMethod.queryWhereClause.
		 * <p>
		 * This can also have ? tags to use for RestParams = QueryWhereParam, filled in from left to right.
		 * <p>
		 * verify: RestMethod.queryWhere is empty, value is a String
		 */
		/**
		 * Uses this parameter’s value as the method’s searchWhere clause for
		 * {@code OASearch}. Supports {@code ?} tags for SearchWhereTagValue params.
		 */
		MethodSearchWhere,

		/*
		 * use the value of this param to be the Method's RestMethod.queryOrderBy.
		 * <p>
		 * verify: RestMethod.queryOrderBy is empty, value is a String
		 */
		/**
		 * Uses this parameter’s value as the method’s searchOrderBy clause for
		 * {@code OASearch}.
		 */
		MethodSearchOrderBy,

		/*
		 * value is used in the url path.
		 * <p>
		 * See RestMethod.urlPath template.
		 * <p>
		 * RestParam.name is required (not case sensitive) if RestMethod.urlPath is using "{x}" style tags.<br>
		 * If RestMethod.urlPath is using "?" tags, then they are filled in with params that have paramType=PathParam<br>
		 * <p>
		 * verify: if RestMethod.urlPath uses {} tags, that method name is in urlPath template (not case sensitive)<br>
		 * verify: method name is in urlPath template (not case sensitive)<br>
		 */
		/**
		 * Contributes a value to the URL path template. Requires {@code name} when
		 * using named tags such as {@code {id}}.
		 */
		UrlPathTagValue,

		/*
		 * Use RestParam.name and param value to add to url query string.
		 * <p>
		 * requires RestParam.name<br>
		 * verify: name is not empty<br>
		 */
		/**
		 * Adds this parameter as a name/value pair in the URL query string.
		 * Requires {@code name} to be explicitly defined.
		 */
		UrlQueryNameValue,

		/*
		 * Use RestParam.name and the param value to add to content=type="x-www-form-urlencoded"
		 * <p>
		 * requires RestParam.name, used with POST only
		 * <p>
		 * verify: name is not empty<br>
		 * verify: does not allow BodyObject, BodyJson
		 */
		/**
		 * Adds this parameter as a form name/value pair when posting URL-encoded
		 * form data. Requires {@code name}. Not allowed with BodyObject/BodyJson.
		 */
		FormNameValue,

		/*
		 * defines the type of return for the method. Used when using generics, ex: List<T>
		 * <p>
		 * verify: required if RestMethod method type cant be discovered<br>
		 */
		/**
		 * Supplies the concrete return class when generics or reflection cannot
		 * determine it automatically.
		 */
		MethodReturnClass,

		/*
		 * use the value(s) of this arg for the queryWhere inputs
		 * <p>
		 * RestParam.name is required (not case sensitive) if RestMethod.urlPath is using "{x}" style tags.<br>
		 * If RestMethod.queryWhere is using "?" tags, then they are filled in with params that have paramType=PathParam<br>
		 * <p>
		 * verify: RestMethod.queryWhere has matching tags<br>
		 */
		/**
		 * Provides a value to replace {@code ?} tags in the method’s searchWhere
		 * template for {@code OASearch}.
		 */
		SearchWhereTagValue,

		/*
		 * use RestParam.name=value to add to where clause. Will skip any values that are null.<br>
		 * Will append existing RestMethod.queryWhere with "AND"<br>
		 * Any params that are array/collection will use "OR" between each value, and surround using all using "(..)"
		 * <p>
		 * requires RestParam.name<br>
		 */
		/**
		 * Adds a name=value clause into the searchWhere expression, combining with
		 * AND/OR rules depending on whether the argument is a collection.
		 */
		SearchWhereAddNameValue,

		/*
		 * use to mark as method argument for as the OAObject for a RestMethod.MethodType=OAObjectMethodCall
		 * <p>
		 * requires
		 * <p>
		 * verify: RestMethod.methodType=OAObjectMethodCall, Insert, Update, Delete
		 */
		/**
		 * Indicates that this argument is the OAObject to be inserted, updated,
		 * deleted, or used as the target of an OAObjectMethodCall.
		 */
		OAObject,

		/*
		 * Used by OAGet
		 */
		/**
		 * Marks this parameter as the OAObject identifier for {@code OAGet} calls.
		 * The value is used to construct the derived URL path.
		 */
		OAObjectId,

		/*
		 * used in place of OARestMethod.methodName, when the method name needs to be dynamic
		 * <p>
		 * verify: RestMethod.methodType=OAObjectMethodCall
		 */
		/**
		 * Supplies a dynamic remote method name for {@code OAObjectMethodCall}.
		 * Overrides the annotation’s {@code methodName} value.
		 */
		OAObjectMethodName,

		/*
		 * use for method argument for RestMethod.MethodType=OAObjectMethodCall or OARemote
		 * <p>
		 * requires
		 * <p>
		 * Note: method params will be used in the same order as defined.
		 * <p>
		 * verify: RestMethod.methodType=OAObjectMethodCall
		 */
		/**
		 * Marks this parameter as an argument to be passed to a remote OAObject
		 * method call, preserving positional order.
		 */
		MethodCallArg,

		/*
		 * This allows the method to supply the RestClient.invokeInfo that is used for making the HTTP call.
		 */
		/**
		 * Enables caller-supplied {@link com.viaoa.remote.rest.info.OARestInvokeInfo}
		 * to control or observe details of the HTTP invocation.
		 */
		OARestInvokeInfo,

		/*
		 * Convert to json and send in body.<br>
		 * If only one exists for the method, then it will be used as the body. If more than one, then a json object with properties will be
		 * created, using the param name. Can be used with BodyJson.
		 */
		/**
		 * Serializes this parameter as JSON and uses it as (part of) the HTTP body.
		 * If multiple BodyObject/BodyJson parameters exist, a composite JSON object
		 * is created.
		 */
		BodyObject,

		/*
		 * use this json in the body. Can be a String (json) or OAJsonNode.<br>
		 * If only one exists for the method, then it will be used as the body. If more than one, then a json object with properties will be
		 * created, using the param name. Can be used with BodyObject.
		 */
		/**
		 * Sends raw JSON or an OAJsonNode as the request body. Can be combined with
		 * BodyObject; multiple entries produce a composite JSON object.
		 */
		BodyJson,

		/*
		 * use as byte[] in the body.<br>
		 * content-type=application/octet-stream<br>
		 * <p>
		 * verify: must be byte[], cant have other Body* params.
		 */
		/**
		 * Sends a byte array as the request body using the content type
		 * {@code application/octet-stream}. Must be the only Body* parameter.
		 */
		BodyByteArray,

		/**
		 * Adds this parameter’s value to the HTTP request headers under the name
		 * specified by {@code name()}.
		 */
		Header, // put value in http header

		/**
		 * Adds this parameter’s value to the HTTP request cookies.
		 */
		Cookie, // put value in http cookie

		/*
		 * value is to be used as the page number. a value <= 0 is for all pages. Can be used as header or query string (controlled by
		 * client config)
		 * <p>
		 * verify: only needed when return value is array,List,Hub<br>
		 */
		/**
		 * Supplies the page number for paginated requests. Values ≤ 0 request all
		 * pages. Can be sent either as a header or query parameter.
		 */
		PageNumber,

		/*
		 * value or arg is property path(s) to include in response. Can be String[], List<String>, or String (one)
		 */
		/**
		 * Specifies property paths that should be included in the response payload.
		 * Accepts String, String[], or List&lt;String&gt; values.
		 */
		ResponseIncludePropertyPaths
	}

}
