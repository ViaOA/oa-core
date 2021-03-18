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
package com.viaoa.remote.rest.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Information about remote method parameters.
 * <p>
 * Important: this annotation needs to be added to the Interface, not the Impl class.
 *
 * @author vvia
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface OARestParam {

	/**
	 * default is to use the name of the param defined in the method. <br>
	 * Note: param names are lost and will be named arg#, unless compiled with option to save names.
	 */
	String name() default "";

	Class paramClass() default Void.class;

	String format() default "";

	/**
	 * property path to include when serializing this value.
	 */
	String includePropertyPath() default ""; // supported by OARestServlet

	/**
	 * property paths to include when serializing this value.
	 */
	String[] includePropertyPaths() default {}; // supported by OARestServlet

	/**
	 * property path levels to include when serializing this value.
	 */
	int includeReferenceLevelAmount() default 0; // supported by OARestServlet

	/**
	 * The type of param that this is used for.
	 */
	ParamType type() default ParamType.Unassigned;

	public static enum ParamType {
		/**
		 * default value if not used, otherwise method params are set to default value based on RestMethod.MethodType
		 */
		Unassigned,

		Ignore,

		/**
		 * use the value of this param to be the RestMethod.urlPath
		 * <p>
		 * verify: RestMethod.urlPath is empty, and value is a String
		 */
		MethodUrlPath,

		/**
		 * use the value of this param to be the Method's RestMethod.queryWhereClause.
		 * <p>
		 * This can also have ? tags to use for RestParams = QueryWhereParam, filled in from left to right.
		 * <p>
		 * verify: RestMethod.queryWhere is empty, value is a String
		 */
		MethodSearchWhere,

		/**
		 * use the value of this param to be the Method's RestMethod.queryOrderBy.
		 * <p>
		 * verify: RestMethod.queryOrderBy is empty, value is a String
		 */
		MethodSearchOrderBy,

		/**
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
		UrlPathTagValue,

		/**
		 * Use RestParam.name and param value to add to url query string.
		 * <p>
		 * requires RestParam.name<br>
		 * verify: name is not empty<br>
		 */
		UrlQueryNameValue,

		/**
		 * Use RestParam.name and the param value to add to content=type="x-www-form-urlencoded"
		 * <p>
		 * requires RestParam.name, used with POST only
		 * <p>
		 * verify: name is not empty<br>
		 * verify: does not allow BodyObject, BodyJson
		 */
		FormNameValue,

		/**
		 * defines the type of return for the method. Used when using generics, ex: List<T>
		 * <p>
		 * verify: required if RestMethod method type cant be discovered<br>
		 */
		MethodReturnClass,

		/**
		 * use the value(s) of this arg for the queryWhere inputs
		 * <p>
		 * RestParam.name is required (not case sensitive) if RestMethod.urlPath is using "{x}" style tags.<br>
		 * If RestMethod.queryWhere is using "?" tags, then they are filled in with params that have paramType=PathParam<br>
		 * <p>
		 * verify: RestMethod.queryWhere has matching tags<br>
		 */
		SearchWhereTagValue,

		/**
		 * use RestParam.name=value to add to where clause. Will skip any values that are null.<br>
		 * Will append existing RestMethod.queryWhere with "AND"<br>
		 * Any params that are array/collection will use "OR" between each value, and surround using all using "(..)"
		 * <p>
		 * requires RestParam.name<br>
		 */
		SearchWhereAddNameValue,

		/**
		 * use to mark as method argument for as the OAObject for a RestMethod.MethodType=OAObjectMethodCall
		 * <p>
		 * requires
		 * <p>
		 * verify: RestMethod.methodType=OAObjectMethodCall, Insert, Update, Delete
		 */
		OAObject,

		/**
		 * Used by OAGet
		 */
		OAObjectId,

		/**
		 * used in place of OARestMethod.methodName, when the method name needs to be dynamic
		 * <p>
		 * verify: RestMethod.methodType=OAObjectMethodCall
		 */
		OAObjectMethodName,

		/**
		 * use for method argument for RestMethod.MethodType=OAObjectMethodCall or OARemote
		 * <p>
		 * requires
		 * <p>
		 * Note: method params will be used in the same order as defined.
		 * <p>
		 * verify: RestMethod.methodType=OAObjectMethodCall
		 */
		MethodCallArg,

		/**
		 * This allows the method to supply the RestClient.invokeInfo that is used for making the HTTP call.
		 */
		OARestInvokeInfo,

		/**
		 * Convert to json and send in body.<br>
		 * If only one exists for the method, then it will be used as the body. If more than one, then a json object with properties will be
		 * created, using the param name. Can be used with BodyJson.
		 */
		BodyObject,

		/**
		 * use this json in the body. Can be a String (json) or OAJsonNode.<br>
		 * If only one exists for the method, then it will be used as the body. If more than one, then a json object with properties will be
		 * created, using the param name. Can be used with BodyObject.
		 */
		BodyJson,

		/**
		 * use as byte[] in the body.<br>
		 * content-type=application/octet-stream<br>
		 * <p>
		 * verify: must be byte[], cant have other Body* params.
		 */
		BodyByteArray,

		Header, // put value in http header

		Cookie, // put value in http cookie

		/**
		 * value is to be used as the page number. a value <= 0 is for all pages. Can be used as header or query string (controlled by
		 * client config)
		 * <p>
		 * verify: only needed when return value is array,List,Hub<br>
		 */
		PageNumber,

		/**
		 * value or arg is property path(s) to include in response. Can be String[], List<String>, or String (one)
		 */
		ResponseIncludePropertyPaths
	}

}
