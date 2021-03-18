package com.viaoa.remote.rest;

import com.viaoa.remote.rest.info.OARestInvokeInfo;

/**
 * OARestClient exception, supplying OARestInvokeInfo.
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
