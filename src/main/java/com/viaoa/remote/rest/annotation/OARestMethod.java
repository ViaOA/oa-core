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
 * Defines how a remote interface method is mapped to an HTTP(S) request when
 * invoked through {@link com.viaoa.remote.rest.OARestClient}. The annotation
 * configures the HTTP method, URL template, query parameters, OAObjectGraph
 * behavior, paging, return type resolution, and server-side include rules.
 *
 * <p>
 * This annotation must be applied to methods on the <b>interface</b> that will
 * be remoted. It is not used on implementation classes.
 * </p>
 *
 * <h2>URL and Query Mapping</h2>
 * <ul>
 *   <li>{@code urlPath} supports named template tags (e.g. "/emp/{id}") or
 *       positional tags ("/emp/?/?").</li>
 *   <li>{@code urlQuery} defines static or template-based query parameters.</li>
 *   <li>Parameters annotated with {@link OARestParam} fill in path and query
 *       variables.</li>
 * </ul>
 *
 * <h2>OAObjectGraph Support</h2>
 * <p>
 * For {@code MethodType} values such as {@code OAGet}, {@code OASearch},
 * {@code OAInsert}, {@code OAUpdate}, and {@code OADelete}, the method
 * dispatch is routed through {@code OARestServlet}. Additional metadata is
 * available:
 * </p>
 * <ul>
 *   <li>{@code includePropertyPath(s)}</li>
 *   <li>{@code includeReferenceLevelAmount}</li>
 *   <li>{@code returnClass}</li>
 * </ul>
 *
 * <h2>Paging</h2>
 * <p>
 * {@code pageSize} controls server-side pagination for collection results.
 * A {@code PageNumber} parameter may also be supplied using
 * {@link OARestParam.ParamType#PageNumber}.
 * </p>
 *
 * <h2>MethodType</h2>
 * <p>
 * The {@link MethodType} enum defines how the method maps to REST behavior.
 * Some types derive URL paths automatically when OAObjectGraph operations are
 * involved.
 * </p>
 *
 * @see com.viaoa.remote.rest.OARestClient
 * @see com.viaoa.remote.rest.annotation.OARestParam
 * @author vvia
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OARestMethod {
	MethodType methodType() default MethodType.Unassigned;

	/**
	 * Optional alternate name for this REST method. If supplied, it overrides the
	 * default method name when constructing metadata.
	 *
	 * @return the REST-visible method name
	 */
	String name() default "";

	/*
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
	/**
	 * Defines the URL path template used for the HTTP request. Supports:
	 * <ul>
	 *   <li>named tags such as {@code /emp/{id}}</li>
	 *   <li>positional tags such as {@code /emp/?/?}</li>
	 * </ul>
	 * Tags must match parameter annotations of type {@code UrlPathTagValue}.
	 *
	 * @return the URL path template
	 */
	String urlPath() default "";

	/*
	 * http query string.
	 * <p>
	 * verify: ignores leading '?'
	 */
	/**
	 * Defines the static query string to append to the request URL. Leading
	 * question marks are ignored during verification.
	 *
	 * @return the query portion of the request URL
	 */
	String urlQuery() default "";

	/**
	 * Defines the search filter expression used when {@link MethodType#OASearch}
	 * is applied. May contain positional {@code ?} tags that map to parameters
	 * annotated as {@code SearchWhereTagValue}.
	 *
	 * @return the search filter expression
	 */
	String searchWhere() default "";

	/*
	 * Search Query OrderBy to use.
	 * <p>
	 * only used when methodType=OASearch
	 * <p>
	 * verify: RestMethod.queryWhere is not empty, and param type=MethodQueryOrderBy is not empty
	 */
	/**
	 * Specifies the ORDER BY clause used with {@link MethodType#OASearch}. Requires
	 * that {@code searchWhere} is non-empty when used.
	 *
	 * @return the ordering expression for the search
	 */
	String searchOrderBy() default "";

	/*
	 * PropertyPath to include in OAGraph result.
	 * <p>
	 * only used when methodType=OA*
	 */
	/**
	 * Single property path to include in OAObjectGraph results for OA* method types.
	 *
	 * @return the property path to include
	 */
	String includePropertyPath() default "";

	/*
	 * PropertyPath to include in OAGraph result.
	 * <p>
	 * only used when methodType=OA*
	 */
	/**
	 * Multiple property paths to include in OAObjectGraph results for OA* method
	 * types. Combined with {@link #includePropertyPath()}.
	 *
	 * @return an array of property paths to include
	 */
	String[] includePropertyPaths() default {}; // PP to include in result, supported by OARestServlet

	/*
	 * Number of reference levels to include in OAGraph result.
	 * <p>
	 * only used when methodType=OA*
	 */
	/**
	 * Determines how many reference levels should be expanded in OAObjectGraph
	 * results for OA* method types.
	 *
	 * @return number of reference levels to include
	 */
	int includeReferenceLevelAmount() default 0;

	/*
	 * The method name for remote method calls.
	 * <p>
	 * only used by methodType=OAObjectMethodCall, or methodType=OARemote
	 */
	/**
	 * Specifies the remote method name when invoking {@link MethodType#OAObjectMethodCall}
	 * or {@link MethodType#OARemote}. Required for remote object method calls.
	 *
	 * @return the remote method name
	 */
	String methodName() default "";

	/*
	 * value used as the page size for returns that are collection of values (zero or more).<br>
	 * A value <= 0 will use the servers default pagesize.
	 * <p>
	 * This will be added as an http query value "pageSize"
	 * <p>
	 * verify: only needed when return value is array,List,Hub<br>
	 * verify: pageNumber is required if pageSize>0<br>
	 */
	/**
	 * Defines the number of items returned per page when the method returns a
	 * collection type. A value ≤ 0 indicates that the server’s default page size
	 * should be used.
	 *
	 * @return the configured page size
	 */
	int pageSize() default 0;

	/*
	 * Type of return class. This is used/needed when it can't be determined what the actual return class is.<br>
	 * For example: if using a List or Hub and the actual objects can not be discovered using generics.
	 * <p>
	 * Note: generics are able to be discovered for return values (not affected by generics erasure).<br>
	 * Note: calling OARestMethodInfo.verify() will check to see if returnClass is needed or not.<br>
	 * <p>
	 */
	/**
	 * Declares the concrete class of objects returned by methods whose return type
	 * cannot be inferred from generics (e.g., raw List or Hub types).
	 *
	 * @return the explicitly defined return class
	 */
	Class returnClass() default Void.class;

	/*
	 * The type of method, that define how it will use HTTP(S) to call the remote server.
	 */
	/**
	 * Enumerates the different REST and OAObjectGraph operation types supported
	 * by {@code OARestMethod}. Each value defines how URL paths, HTTP methods,
	 * and parameter annotations are interpreted.
	 */
	public static enum MethodType {
		/*
		 * Unassigned/not set, which will produce an error.
		 * <p>
		 * verify: it should throw an exception<br>
		 */
		/**
		 * Indicates that no method type has been defined. Verification should fail
		 * if this value remains assigned.
		 */
		Unassigned,

		/*
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
		/**
		 * Maps the method to an HTTP GET operation. Supports URL path templates and
		 * query construction. Accepts URL path, query, and paging annotations.
		 */
		GET(),

		/*
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
		/**
		 * Uses HTTP GET to retrieve an OAObject by ID through OARestServlet. The
		 * URL path is derived automatically from the object type and key values.
		 */
		OAGet(false),

		/*
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
		/**
		 * Uses HTTP POST to perform an OAObjectGraph search. Supports query templates,
		 * paging, search tags, additional where clauses, and return-class resolution.
		 */
		OASearch(false),

		/*
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
		/**
		 * Maps the method to an HTTP POST operation. Body content may come from
		 * form parameters, JSON, or annotated body parameters.
		 */
		POST,

		/*
		 * Use http PUT
		 * <p>
		 * see: POST
		 */
		/**
		 * Maps the method to an HTTP PUT operation. Behaves similarly to POST, with
		 * body construction rules defined by parameter annotations.
		 */
		PUT,

		/*
		 * Use http PATCH
		 * <p>
		 * see: POST
		 */
		/**
		 * Maps the method to an HTTP PATCH operation. Behavior mirrors POST, but using
		 * the PATCH verb for partial updates.
		 */
		PATCH,

		/*
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
		/**
		 * Uses HTTP POST to invoke a method on a remote OAObject through OARestServlet.
		 * Requires a parameter annotated as OAObject and a {@code methodName} value.
		 * URL path and required query parameters are derived automatically.
		 */
		OAObjectMethodCall(false),

		/*
		 * Used internally when calling methods on a remote object, that get invoked on server running OARestServlet.
		 * <p>
		 * required:<br>
		 * OARestServlet on server.<br>
		 * registered object on server, using OARestServlet.register(). This is the implementation of the Java interface that was called on
		 * the client computer.
		 * <p>
		 */
		/**
		 * Used internally for calling methods on a remote object registered with
		 * OARestServlet. Requires a server-side implementation of the proxied
		 * interface.
		 */
		OARemote(false),

		/*
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
		/**
		 * Uses HTTP PUT to insert a new OAObject through OARestServlet. The URL path
		 * is derived from the OAObject type. Requires a parameter annotated as OAObject.
		 */
		OAInsert(false),

		/*
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
		/**
		 * Uses HTTP POST to update an existing OAObject through OARestServlet.
		 * The URL path is derived from the OAObject type. Requires a parameter
		 * annotated as OAObject.
		 */
		OAUpdate(false),

		/*
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
		/**
		 * Uses HTTP DELETE to remove an OAObject from OAGraph through OARestServlet.
		 * Requires a parameter annotated as OAObject. URL path is derived from the
		 * object's type and key.
		 */
		OADelete(false);

		/**
		 * Indicates whether this method type requires an explicit {@code urlPath}
		 * annotation value. Some OA* types derive the path automatically.
		 */
		protected boolean requiresUrlPath = true;
		
		/**
		 * Returns whether the method type requires an explicitly defined URL path.
		 *
		 * @return {@code true} if a URL path must be supplied, otherwise {@code false}
		 */
		public boolean requiresUrlPath() {
			return requiresUrlPath;
		}

		/**
		 * Default constructor that leaves {@code requiresUrlPath} set to true.
		 */
		MethodType() {
		}

		/**
		 * Constructor that sets the requirement flag indicating whether the method
		 * type must explicitly define a URL path.
		 *
		 * @param requiresUrlPath whether a URL path is required
		 */
		MethodType(boolean requiresUrlPath) {
			this.requiresUrlPath = requiresUrlPath;
		}

		/**
		 * Determines whether this method type represents a generic "OA" category.
		 * Returns true only if the enum name is exactly {@code "OA"}.
		 *
		 * @return {@code true} if the enum name equals "OA"; otherwise {@code false}
		 */
		boolean isOA() {
			String s = this.toString();
			return "OA".equals(s);
		}
	}
}
