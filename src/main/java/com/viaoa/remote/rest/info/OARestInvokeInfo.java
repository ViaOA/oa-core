package com.viaoa.remote.rest.info;

import java.util.HashMap;

/**
 * Runtime information for the proxy method invoke, that uses HTTP+REST to complete the task.
 * <p>
 * This info has http and lower level details. <br>
 * For access this info when a method is called, a method can can include an OARestInvokeInfo for one of the method params, or for it's
 * return value.
 * <p>
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