/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
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
	 * MethodInfo for this invoke.
	 */
	public OARestMethodInfo methodInfo;

	/**
	 * Arguments from method that was invoked.
	 */
	public Object[] args;

	/**
	 * Milisecond (timestamp) for start, http msg sent, and end.
	 */
	public long tsStart, tsSent, tsEnd;

	/**
	 * HTTP method used, ex: GET, POST, etc
	 */
	public String httpMethod;

	/**
	 * HTTP url path. Note: does not have to have baseUrl, or method.contextName
	 */
	public String urlPath;

	/**
	 * HTTP url query.
	 */
	public String urlQuery;

	/**
	 * Final http url used to connect to server.<br>
	 * Created using value from urlPath & urlQuery in the format: "protocol://host[:port][/oarest|contextname]/urlPath?urlQuery"
	 */
	public String finalUrl;

	/**
	 * Json object (text) for http request body
	 * <p>
	 * Depends on methodType:<br>
	 * OASearch: search param(s), if any used<br>
	 * OAObjectMethodCall: JSON array, params that are annotated with MethodCallArg<br>
	 * OARemote: JSON array, params that are annotated with MethodCallArg<br>
	 * params that are annotated with BodyObject, BodyJson<br>
	 */
	public String jsonBody;

	public String contentType;
	public String textBody;

	/**
	 * Uses http content type <br>
	 * Used for params annotated as FormNameValue.
	 */
	public String formData;
	public byte[] byteArrayBody;

	public HashMap<String, String> hsHeaderOut = new HashMap();
	public HashMap<String, String> hsCookieOut = new HashMap();

	// note: headers with multiple values are comma seperated
	public HashMap<String, String> hsHeaderIn = new HashMap();

	public Class methodReturnClass;

	public int responseCode;
	public String responseCodeMessage;
	public String responseBody;
	public Exception responseException;

	public Object returnObject;

	public OARestInvokeInfo() {

	}

}