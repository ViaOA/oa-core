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
 * Remoting information about remote methods using HTTP(S).
 * <p>
 * Important: this annotation needs to be added to the Java Interface, not the Impl class.
 *
 * @author vvia
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OARestMethod {
	MethodType methodType() default MethodType.Unassigned;

	String name() default "";

	/**
	 * URL path for endpoint. If it does not include "://", then the OARestClient.baseUrl will be used as prefix.
	 * <p>
	 * Supports path template names, example: "/emp/{id}/{name}"<br>
	 * tags (case insensitive) are filled in from method params with paramType==UrlPathValue, and must have the @RestParam.name defined to
	 * match name used in template.
	 * <p>
	 * Used by non-OA* method types
	 * <p>
	 * Supports path template {name}, example: "/emp/{id}"<br>
	 * Supports path template ?tags, example: "/emp/?/?"<br>
	 * ? tags will use method params with paramType==UrlPathValue, to fill in from left to right.
	 * <p>
	 * verify: if tags names, that there are matching param(s) with matching name for all {x} tags<br>
	 * verify: if ?tags, that there are matching paramType==UrlPathValue<br>
	 * verify: cant mix {} and ? tags<br>
	 */
	String urlPath() default "";

	/**
	 * http query string.
	 * <p>
	 * verify: ignores leading '?'
	 */
	String urlQuery() default "";

	/**
	 * search Query to use, with ?tags from param(s) with type=SearchWhereTagValue.
	 * <p>
	 * only used when methodType=OASearch
	 * <p>
	 * verify: query ?tags having mathcing params with type=SearchWhereTagValue
	 */
	String searchWhere() default "";

	/**
	 * Search Query OrderBy to use.
	 * <p>
	 * only used when methodType=OASearch
	 * <p>
	 * verify: RestMethod.queryWhere is not empty, and param type=MethodQueryOrderBy is not empty
	 */
	String searchOrderBy() default "";

	/**
	 * PropertyPath to include in OAGraph result.
	 * <p>
	 * only used when methodType=OA*
	 */
	String includePropertyPath() default "";

	/**
	 * PropertyPath to include in OAGraph result.
	 * <p>
	 * only used when methodType=OA*
	 */
	String[] includePropertyPaths() default {}; // PP to include in result, supported by OARestServlet

	/**
	 * Number of reference levels to include in OAGraph result.
	 * <p>
	 * only used when methodType=OA*
	 */
	int includeReferenceLevelAmount() default 0;

	/**
	 * The method name for remote method calls.
	 * <p>
	 * only used by methodType=OAObjectMethodCall, or methodType=OARemote
	 */
	String methodName() default "";

	/**
	 * value used as the page size for returns that are collection of values (zero or more).<br>
	 * A value <= 0 will use the servers default pagesize.
	 * <p>
	 * This will be added as an http query value "pageSize"
	 * <p>
	 * verify: only needed when return value is array,List,Hub<br>
	 * verify: pageNumber is required if pageSize>0<br>
	 */
	int pageSize() default 0;

	/**
	 * Type of return class. This is used/needed when it can't be determined what the actual return class is.<br>
	 * For example: if using a List or Hub and the actual objects can not be discovered using generics.
	 * <p>
	 * Note: generics are able to be discovered for return values (not affected by generics erasure).<br>
	 * Note: calling OARestMethodInfo.verify() will check to see if returnClass is needed or not.<br>
	 * <p>
	 */
	Class returnClass() default Void.class;

	/**
	 * The type of method, that define how it will use HTTP(S) to call the remote server.
	 *
	 * @author vvia
	 */
	public static enum MethodType {
		/**
		 * Unassigned/not set, which will produce an error.
		 * <p>
		 * verify: it should throw an exception<br>
		 */
		Unassigned,

		/**
		 * Uses http GET.
		 * <p>
		 * required:<br>
		 * urlPath or param type=MethodUrlPath,
		 * <p>
		 * valid method annotations:<br>
		 * urlPath, urlQuery, pageSize<br>
		 * <p>
		 * valid param annotation types:<br>
		 * MethodUrlPath, UrlPathTagValue, UrlQueryNameValue, MethodReturnClass, OARestInvokeInfo, BodyObject, BodyJson, Header, Cookie,
		 * PageNumber
		 */
		GET(),

		/**
		 * Uses http GET to get an object from OAGraph using an object Id.
		 * <p>
		 * automatically adds the default url for OARestServlet.
		 * <p>
		 * required:<br>
		 * method signature must return a subclass of OAObject<br>
		 * must have a param type=OAObjectId<br>
		 * OARestServlet on server.<br>
		 * <p>
		 * derives: urlPath as "/customer/{id}[/{id2}..]"<br>
		 * using the return class name, and the value(s) from param with type=OAObjectId
		 * <p>
		 * valid method annotations:<br>
		 * includePropertyPath, includePropertyPaths, includeReferenceLevelAmount
		 * <p>
		 * valid param annotation types:<br>
		 * OAObjectId (required),<br>
		 * ResponseIncludePropertyPaths<br>
		 * OARestInvokeInfo, Header, Cookie,
		 */
		OAGet(false),

		/**
		 * Uses http POST to query OAGraph objects.
		 * <p>
		 * required:<br>
		 * method signature must return a collection (array,list,hub) of objects that are a subclass of OAObject<br>
		 * OARestServlet on server.<br>
		 * <p>
		 * Supports using "?tags" as variable holders in query, to be matched with value(s) from params with type=SearchWhereTagValue <br>
		 * Supports param(s) of type = SearchWhereAddNameValue, that will be added to query. Note: value can be an array, which will add to
		 * the search "(.. OR ..)" using the values.
		 * <p>
		 * valid method annotations:<br>
		 * searchWhere, searchOrderBy,<br>
		 * pageSize,<br>
		 * includePropertyPath(s), includeReferenceLevelAmount,<br>
		 * returnClass
		 * <p>
		 * valid param annotation types:<br>
		 * SearchWhereTagValue,<br>
		 * SearchWhereAddNameValue,<br>
		 * PageNumber, <br>
		 * ResponseIncludePropertyPaths,<br>
		 * MethodSearchWhere, MethodSearchOrderBy, <br>
		 * UrlQueryNameValue, MethodReturnClass, Header, Cookie,
		 */
		OASearch(false),

		/**
		 * Uses http POST
		 * <p>
		 * http Body will use param values of type=FormNameValue or BodyObject or BodyJson.
		 * <p>
		 * required:<br>
		 * urlPath or param type=MethodUrlPath
		 * <p>
		 * valid method annotations:<br>
		 * urlPath, urlQuery, pageSize<br>
		 * <p>
		 * valid param annotation types:<br>
		 * UrlPathTagValue, UrlQueryNameValue,<br>
		 * FormNameValue, BodyObject, BodyJson,<br>
		 * MethodUrlPath, Header, Cookie, PageNumber, OARestInvokeInfo, MethodReturnClass
		 */
		POST,

		/**
		 * Use http PUT
		 * <p>
		 * see: POST
		 */
		PUT,

		/**
		 * Use http PATCH
		 * <p>
		 * see: POST
		 */
		PATCH,

		/**
		 * Use http POST to call OARestServlet to call a method on an OAObject.
		 * <p>
		 * required:<br>
		 * methodName<br>
		 * must have a param type=OAObject<br>
		 * OARestServlet on server.<br>
		 * <p>
		 * derives: urlPath and query params required by OARestServlet to make the remote method call<br>
		 * <p>
		 * valid method annotations:<br>
		 * methodName,<br>
		 * includePropertyPath(s), includeReferenceLevelAmount, returnClass
		 * <p>
		 * valid param annotation types:<br>
		 * OAObject, MethodCallArg, <br>
		 * OARestInvokeInfo, ResponseIncludePropertyPaths, <br>
		 * Header, Cookie, PageNumber
		 */
		OAObjectMethodCall(false),

		/**
		 * Used internally when calling methods on a remote object, that get invoked on server running OARestServlet.
		 * <p>
		 * required:<br>
		 * OARestServlet on server.<br>
		 * registered object on server, using OARestServlet.register(). This is the implementation of the Java interface that was called on
		 * the client computer.
		 * <p>
		 */
		OARemote(false),

		/**
		 * Uses http PUT to call OARestServlet to insert a new OAObject.
		 * <p>
		 * required:<br>
		 * must have a param type=OAObject<br>
		 * OARestServlet on server.<br>
		 * <p>
		 * derives: urlPath<br>
		 * <p>
		 * valid method annotations:<br>
		 * includePropertyPath(s), includeReferenceLevelAmount
		 * <p>
		 * valid param annotation types:<br>
		 * OAObject (required), <br>
		 * OARestInvokeInfo, Header, Cookie, PageNumber, ResponseIncludePropertyPaths
		 */
		OAInsert(false),

		/**
		 * Uses http POST to call OARestServlet to update an existing OAObject.
		 * <p>
		 * required:<br>
		 * must have a param type=OAObject<br>
		 * OARestServlet on server.<br>
		 * <p>
		 * derives: urlPath<br>
		 * <p>
		 * valid method annotations:<br>
		 * includePropertyPath(s), includeReferenceLevelAmount
		 * <p>
		 * valid param annotation types:<br>
		 * OAObject (required), <br>
		 * OARestInvokeInfo, Header, Cookie, PageNumber, ResponseIncludePropertyPaths
		 */
		OAUpdate(false),

		/**
		 * Uses http DELETE to delete an object from OAGraph.
		 * <p>
		 * automatically adds the default url for OARestServlet.
		 * <p>
		 * required:<br>
		 * must have a param type=OAObject<br>
		 * OARestServlet on server.<br>
		 * <p>
		 * derives: urlPath<br>
		 * <p>
		 * valid method annotations:<br>
		 * includePropertyPath(s), includePropertyPaths, includeReferenceLevelAmount
		 * <p>
		 * valid param annotation types:<br>
		 * OAObject (required),<br>
		 * OARestInvokeInfo, Header, Cookie
		 */
		OADelete(false);

		public boolean requiresUrlPath = true;

		MethodType() {
		}

		MethodType(boolean requiresUrlPath) {
			this.requiresUrlPath = requiresUrlPath;
		}

		boolean isOA() {
			String s = this.toString();
			return "OA".equals(s);
		}
	}

}
