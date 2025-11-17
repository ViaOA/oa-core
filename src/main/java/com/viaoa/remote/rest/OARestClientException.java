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
package com.viaoa.remote.rest;

import com.viaoa.remote.rest.info.OARestInvokeInfo;

/**
 * Exception thrown when a remote REST invocation fails. This exception
 * wraps an {@link com.viaoa.remote.rest.info.OARestInvokeInfo} instance,
 * which contains complete diagnostic information about the failed request,
 * including HTTP status code, request/response headers, JSON payloads,
 * method metadata, and timing information.
 *
 * <p>
 * {@code OARestClientException} does not generate its own message; instead,
 * callers inspect the associated {@code OARestInvokeInfo} to obtain the
 * detailed remote error description or server response.
 * </p>
 *
 * <h2>When Thrown</h2>
 * <ul>
 *   <li>HTTP status codes indicate error conditions (4xx or 5xx)</li>
 *   <li>JSON deserialization fails</li>
 *   <li>Network or connection failures occur</li>
 *   <li>A remote server returns a structured OA error</li>
 *   <li>Method return types do not match the received JSON</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>
 * Calling code may inspect:
 * </p>
 * <ul>
 *   <li>{@link #getInvokeInfo()}</li>
 *   <li>{@link #getStatus()}</li>
 *   <li>{@link #getStatusMessage()}</li>
 * </ul>
 *
 * to determine the cause of failure and construct appropriate application logic.
 *
 * @author vvia
 */
public class OARestClientException extends RuntimeException {
	private transient OARestInvokeInfo invokeInfo;

	public OARestClientException(String msg) {
		super(msg);
	}

	public OARestClientException(OARestInvokeInfo invokeInfo) {
		this.invokeInfo = invokeInfo;
	}

	public OARestClientException(OARestInvokeInfo invokeInfo, String msg) {
		super(msg);
		this.invokeInfo = invokeInfo;
	}

	public OARestClientException(OARestInvokeInfo invokeInfo, String msg, Exception e) {
		super(msg, e);
		this.invokeInfo = invokeInfo;
	}

	public OARestInvokeInfo getInvokeInfo() {
		return this.invokeInfo;
	}

	public int getHttpStatusCode() {
		if (invokeInfo == null) {
			return 200; // HttpServletResponse.SC_OK;
		}
		return invokeInfo.responseCode;
	}

	public String getHttpStatusMessage() {
		if (invokeInfo == null) {
			return null;
		}
		return invokeInfo.responseCodeMessage;
	}

}
