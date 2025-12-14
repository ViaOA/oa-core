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

import java.util.HashMap;

/**
 * Runtime information for a single REST invocation. Instances of this class
 * are created for every remote call and capture detailed diagnostics, timing
 * information, and all request/response data.
 *
 * <h2>Captured Invocation Data</h2>
 * <ul>
 *   <li>Fully constructed URL and HTTP method</li>
 *   <li>Request body (JSON or form) and outbound headers</li>
 *   <li>Response status code, message, and response body</li>
 *   <li>Timestamps for start, send, receive, and finish</li>
 *   <li>Any exception thrown during the remote call</li>
 *   <li>Deserialized return value (if successful)</li>
 *   <li>Reference to the {@link OARestMethodInfo} that initiated the call</li>
 * </ul>
 *
 * <h2>Dev & Diagnostic Usage</h2>
 * {@code OARestInvokeInfo} may be:
 * <ul>
 *   <li>Returned to application code for debugging.</li>
 *   <li>Logged or persisted for auditing.</li>
 *   <li>Inspected inside an {@code OARestClientException}.</li>
 * </ul>
 *
 * <p>
 * The object is intentionally verbose and designed for transparency: developers
 * can see exactly how a REST call was formed, how long it took, and what was
 * returned.
 * </p>
 *
 * @author vvia
 */
public class OARestInvokeInfo {

	/**
	 * Metadata describing the REST method that initiated this invocation.
	 */
	public OARestMethodInfo methodInfo;

	/**
	 * The arguments supplied to the proxied method when the remote call was made.
	 */
	public Object[] args;

	/**
	 * Timestamps in milliseconds marking when the invocation began, when the
	 * request was sent, and when the invocation completed.
	 */
	public long tsStart, tsSent, tsEnd;

	/**
	 * The HTTP method used for the request (e.g., GET, POST, PUT, PATCH).
	 */
	public String httpMethod;

	/**
	 * The URL path portion of the HTTP request, excluding base URL and context.
	 */
	public String urlPath;

	/**
	 * The query-string portion of the request URL.
	 */
	public String urlQuery;

	/**
	 * The fully constructed request URL including protocol, host, context,
	 * path, and query string.
	 */
	public String finalUrl;

	/*
	 * Json object (text) for http request body
	 * <p>
	 * Depends on methodType:<br>
	 * OASearch: search param(s), if any used<br>
	 * OAObjectMethodCall: JSON array, params that are annotated with MethodCallArg<br>
	 * OARemote: JSON array, params that are annotated with MethodCallArg<br>
	 * params that are annotated with BodyObject, BodyJson<br>
	 */
	/**
	 * JSON text used as the request body. Content varies depending on the
	 * REST method type and parameter annotations.
	 */
	public String jsonBody;

	/**
	 * Explicit content type for the request body, if assigned.
	 */
	public String contentType;

	/**
	 * Text payload used as the request body when not sending JSON or binary data.
	 */
	public String textBody;

	/*
	 * Uses http content type <br>
	 * Used for params annotated as FormNameValue.
	 */
	/**
	 * URL-encoded form data constructed from parameters annotated as
	 * {@code FormNameValue}.
	 */
	public String formData;
	
	/**
	 * Raw byte array used as the request body for binary transmissions.
	 */
	public byte[] byteArrayBody;

	/**
	 * Outbound HTTP headers included with the request. Multiple header values
	 * are concatenated into a single comma-separated string.
	 */
	public HashMap<String, String> hsHeaderOut = new HashMap();

	/**
	 * Outgoing cookies added to the request, stored as name/value pairs.
	 */
	public HashMap<String, String> hsCookieOut = new HashMap();

	// note: headers with multiple values are comma seperated
	/**
	 * Headers returned by the server in the HTTP response. Multiple values
	 * are combined as a comma-separated string.
	 */
	public HashMap<String, String> hsHeaderIn = new HashMap();

	/**
	 * The resolved return class for the method, used during JSON deserialization.
	 */
	public Class methodReturnClass;

	/**
	 * The HTTP status code returned by the server.
	 */
	public int responseCode;

	/**
	 * The HTTP status message returned by the server.
	 */
	public String responseCodeMessage;
	
	/**
	 * The full response body text returned by the server.
	 */
	public String responseBody;
	
	/**
	 * Any exception encountered during HTTP execution or response processing.
	 */
	public Exception responseException;

	/**
	 * The deserialized return value produced by the REST call, if successful.
	 */
	public Object returnObject;

	/**
	 * Constructs a new, empty {@code OARestInvokeInfo} instance for tracking
	 * the state and diagnostics of a REST invocation.
	 */
	public OARestInvokeInfo() {

	}

}