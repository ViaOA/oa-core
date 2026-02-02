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
package com.viaoa.remote.rest;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.viaoa.graph.OAGraphImpl;
import com.viaoa.graph.service.object.OAObjectInfoService;
import com.viaoa.hub.Hub;
import com.viaoa.json.OAJson;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.remote.rest.annotation.OARestClass;
import com.viaoa.remote.rest.annotation.OARestMethod;
import com.viaoa.remote.rest.annotation.OARestMethod.MethodType;
import com.viaoa.remote.rest.annotation.OARestParam;
import com.viaoa.remote.rest.info.OARestClassInfo;
import com.viaoa.remote.rest.info.OARestInvokeInfo;
import com.viaoa.remote.rest.info.OARestMethodInfo;
import com.viaoa.remote.rest.info.OARestMethodInfo.ReturnClassType;
import com.viaoa.remote.rest.info.OARestParamInfo;
import com.viaoa.remote.rest.info.OARestParamInfo.ClassType;
import com.viaoa.runtime.OARuntime;
import com.viaoa.util.Base64;
import com.viaoa.util.OAConv;
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAHttpUtil;
import com.viaoa.util.OAString;
import com.viaoa.util.OATime;


/* Demos

GET
	query params
		query add name-values
	path template, param tag values

POST
	Form data
	json
		body

return
	json
		arrays, list, hub
		object
		moxy, jackson
		oaJsonNode
	void
	primitives


OARest for model access
	get object by Id
	selecting/query objects
	insert
	update
	delete
	security/boundries
	remote method call on oaobject

	qqq todo: wrapper to be able to use siblings


Remote Method Calls
	java interface
	register Impl on server
	send OARestInvokeInfo (Todo on server side)


oarest model objects
	object by id
		servlet/oarest/manualPurchaseOrder/{id}?pp
		with pp
		?? query params
		multipart id
			combined into 1 using "-" or "_"
			separate path params "/WIX/51515
		includePPs
	select using object query
		using filter
	extra params
		PPs to include
		orderBy
	insert
		object
	update
		object
		partial using json/map of name=value
		partial using query name=value
	delete
		id
	remote object method calls
		includePPs
		to OAObject methods
		to OA remote interface
			getDetail, sibling, etc
		to registered Impl

	oasync (Todo)
		messaging
		obj versioning
		could open endpt to stream

OARestClient CLI (Todo)
	allow getting objets and changing to send back

*/

//qqqqqqqqqqqqqqqqq

// OARestInvokeInfo for restServlet

// create remote calls for getting any data
//    create getDetail that allows siblinig data, etc

/*
	call remote method on oarestservlet registered object
		../oaremote?remoteclassname=className&remotemethodname=methodName
 */

// add hoc call method ... builder ??

// Content-Type and Content-Length

// SPEC: https://tools.ietf.org/html/rfc2616

// String response = restTemplate.getForObject(DUMMY_URL, String.class);

// response body
//    https://developer.mozilla.org/en-US/docs/Web/HTTP/Messages

// keep-alive support

// multipart support

// allow param (and annotations) to get extra data:   Map for send headers, Response for headers and return code, etc
// allow adding PP hints for additional data (store in cache ?? for additional requests)
// allow response to be like getDetail, that has wrapper object to hold object, additional prop data, and additional (sibling) objects
// create
// add CORS support

// create abstract methods:  convert string to class, convert object to/from json

/**
 * OARestClient is a client for directly accessing HTTP endpoints, and for creating Java interfaces that OARestClient.getInstance will then
 * create an implementation that will use HTTP to make distributed calls to webserver endpoints, REST API calls, OAGraph objects, or
 * Java2Java method calls when a method is invoked on the client.
 * <p>
 * To create remote method calls, a Java Interface is used with annotations to describe the behavior and interaction with the remote server.
 * OARestClient getInstance(class) can then be used to get an implementation that will automatically have the method invoke use HTTP to get
 * the method's return value.
 * <p>
 * Methods can be defined to automatically call:<br>
 * 1: web url<br>
 * 2: REST API call<br>
 * 3: OA Graph - query, persistence, method calls by working with OARestServlet that allows secure access to object model data.<br>
 * 4: Java implementation of the Java interface being remoted (java2java remote method call).<br>
 * <p>
 * Includes an annotation checker that is useful for finding any configuration errors.
 *
 * @see OARestClass, OARestMethod, OARestParam annotations
 * @author vvia
 */


/**
 * REST-based remoting client for OA. This class allows a Java interface,
 * annotated with {@code OARestClass}, {@code OARestMethod}, and
 * {@code OARestParam}, to be invoked remotely over HTTP. At runtime
 * the client creates a dynamic proxy for the interface and translates
 * each method invocation into an HTTP request, returning the result as
 * strongly typed Java objects, OAObjects, Hubs, lists, or primitive values.
 *
 * <p>
 * The client also provides direct access to OA REST-style endpoints for
 * retrieving and manipulating OAObjects on a remote server. Convenience
 * methods (e.g., {@code callOAGet}, {@code callOASelect},
 * {@code callOAInsert}, {@code callOAUpdate}, {@code callOADelete})
 * handle the full object lifecycle using JSON representations and
 * property-path based graph expansion.
 * </p>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li><b>Dynamic proxy invocation</b> – Annotated interfaces are bound at
 *       runtime and executed as remote REST calls.</li>
 *
 *   <li><b>HTTP method binding</b> – Supports GET, POST, PUT, PATCH,
 *       and DELETE using annotation metadata.</li>
 *
 *   <li><b>Typed return values</b> – Automatically converts JSON responses
 *       into Java types, including OAObjects, Hubs, Lists, arrays, and
 *       simple primitives.</li>
 *
 *   <li><b>Metadata-driven invocation</b> – Builds and caches reflection
 *       metadata for interfaces, methods, and parameters, including URL
 *       templates, query parameters, and JSON body rules.</li>
 *
 *   <li><b>Invocation diagnostics</b> – Every call produces an
 *       {@link com.viaoa.remote.rest.info.OARestInvokeInfo} instance that
 *       captures request/response details, HTTP headers, timing, errors, and
 *       payloads.</li>
 *
 *   <li><b>OAObjectGraph integration</b> – Provides a REST-compatible
 *       mechanism for selecting, retrieving, updating, inserting, and deleting
 *       OAObjects without requiring the Multiplexer remoting layer.</li>
 *
 *   <li><b>Optional relaxed SSL</b> – Includes a permissive HTTPS configuration
 *       for development environments using self-signed certificates.</li>
 * </ul>
 *
 * <h2>Intended Usage</h2>
 * <p>
 * This client is ideal when applications require:
 * </p>
 * <ul>
 *   <li>a lightweight alternative to OA's Multiplexer remoting,</li>
 *   <li>Java-to-Java REST invocation using strongly typed interfaces,</li>
 *   <li>integration with OAObjectGraph over a servlet-based HTTP layer,</li>
 *   <li>simple synchronous remote calls for microservices or mobile clients.</li>
 * </ul>
 *
 * <p>
 * Although lightweight, {@code OARestClient} fully supports OAObject graph
 * semantics, including cascading property paths and JSON-based serialization
 * via {@link com.viaoa.json.OAJson}.
 * </p>
 *
 */
public class OARestClient {

	/**
	 * Protocol scheme to use when building HTTP URLs, such as {@code "http"} or {@code "https"}.
	 */
	private String protocol; // http, https
	
	/**
	 * Base URL host and optional port for remote calls, for example {@code "www.test.com:8080"}.
	 */
	private String baseUrl; // www.test.com:8080

	/**
	 * Default servlet path used when invoking OA REST-style endpoints for {@code MethodType=OA*} methods.
	 */
	private String defaultOARestUrl = "/servlet/oarest"; // when MethodType=OA*

	/*
	 * object ID separator used for compound IDs. Note that JSON "prefers" single ID values.<br>
	 * Common values are "/", "_", "-" <br>
	 * default: "/"
	 */
	/**
	 * Default separator character used when building compound object ID values for URLs.
	 */
	private String defaultIdSeperator = "/";

	/**
	 * User identifier used for HTTP basic authentication when calling remote endpoints.
	 */
	private String userId;

	/**
	 * Password for HTTP basic authentication; marked transient so it is not serialized with the client.
	 */
 	private transient String password;
	
	/**
	 * Last HTTP cookie value returned by the server, reused on subsequent requests for session continuity.
	 */
	private String cookie;

	/**
	 * Cache of REST metadata per interface class, populated from {@link OARestClassInfo}.
	 */
	private final HashMap<Class, OARestClassInfo> hmClassInfo = new HashMap<>();

	/**
	 * Cache of REST metadata per method, mapping reflected {@link Method} instances to {@link OARestMethodInfo}.
	 */
	private final HashMap<Method, OARestMethodInfo> hmMethodInfo = new HashMap<>();

	/**
	 * Cache of dynamically created proxy instances keyed by their interface class.
	 */
	private final HashMap<Class, Object> hmRemoteObjectInstance = new HashMap<>();

	/**
	 * Flag indicating whether HTTPS trust configuration has already been initialized.
	 */
	private static boolean bSetupHttpsAccess;

	/**
	 * Creates a new {@code OARestClient} with default settings; protocol, base URL, and credentials can be configured later via setters.
	 */
	public OARestClient() {
	}

	/**
	 * Sets the user credentials to be used for HTTP basic authentication.
	 *
	 * @param userId the user identifier to send in the Authorization header
	 * @param pw     the password associated with the user identifier
	 */
	public void setUserPw(String userId, String pw) {
		this.userId = userId;
		this.password = pw;
	}

	/**
	 * Sets the base URL host (and optional port) used when constructing request URLs.
	 *
	 * @param baseUrl the base host and optional port, for example {@code "www.test.com:8080"}
	 */
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	/**
	 * Returns the configured base URL used when constructing request URLs.
	 *
	 * @return the current base URL value, or {@code null} if none has been set
	 */
	public String getBaseUrl() {
		return baseUrl;
	}

	/**
	 * Sets the protocol scheme to use when building HTTP URLs.
	 *
	 * @param p the protocol value, such as {@code "http"} or {@code "https"}
	 */
	public void setProtocol(String p) {
		this.protocol = p;
	}

	/**
	 * Returns the protocol scheme currently used when building HTTP URLs.
	 *
	 * @return the protocol value, such as {@code "http"} or {@code "https"}, or {@code null} if unset
	 */
	public String getProtocol() {
		return this.protocol;
	}

	/**
	 * Sets the default servlet path used for OA REST-style endpoints.
	 *
	 * @param defaultOARestUrl the servlet path to use when invoking {@code MethodType=OA*} methods
	 */
	public void setDefaultOARestUrl(String defaultOARestUrl) {
		this.defaultOARestUrl = defaultOARestUrl;
	}

	/**
	 * Returns the default servlet path used for OA REST-style endpoint access.
	 *
	 * @return the configured default OA REST URL
	 */
	public String getDefaultOARestUrl() {
		return defaultOARestUrl;
	}

	/**
	 * Sets the character or string used to join compound object IDs in URL paths.
	 *
	 * @param defaultIdSeperator separator used when formatting multi-part key values
	 */
	public void setDefaultIdSeperator(String defaultIdSeperator) {
		this.defaultIdSeperator = defaultIdSeperator;
	}

	/**
	 * Returns the separator used when constructing multi-part object IDs in URLs.
	 *
	 * @return the configured ID separator value
	 */
	public String getDefaultIdSeperator() {
		return defaultIdSeperator;
	}

	/*
	 * Used to create and instance of a Java interface that has been annotated using OARest* for the class, methods, and method parameters.
	 * <p>
	 * This will use a Java proxy object to manage all of the method calls.
	 * <p>
	 *
	 * @param clazz Java interface to create an instance of.
	 */
	/**
	 * Creates or returns a cached dynamic proxy instance for the given annotated Java interface.
	 * The proxy intercepts method invocations and routes them through REST metadata to remote
	 * HTTP endpoints.
	 *
	 * @param clazz the interface class to proxy
	 * @return the proxy instance implementing the interface, or {@code null} if {@code clazz} is null
	 * @throws Exception if {@code clazz} is not an interface or metadata loading fails
	 */
	public <API> API getInstance(Class<API> clazz) throws Exception {
		if (clazz == null) {
			return null;
		}

		API obj = (API) hmRemoteObjectInstance.get(clazz);
		if (obj != null) {
			return obj;
		}

		if (!clazz.isInterface()) {
			throw new Exception("Class (" + clazz + ") must be a java interface");
		}

		loadMetaData(clazz);

		InvocationHandler handler = new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				Object result;
				try {
					result = onInvoke(method, args);
				} catch (Exception e) {
					throw new RuntimeException("Invoke exception, method=" + method, e);
				}
				return result;
			}
		};

		obj = (API) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, handler);

		return obj;
	}

	/**
	 * Handles a remote method invocation by delegating to {@link #_onInvoke(Method, Object[])}
	 * and returning the remote method's resulting object.
	 *
	 * @param method the reflected method being invoked
	 * @param args   arguments passed to the proxy invocation
	 * @return the return object produced by the remote invocation
	 * @throws Throwable if the remote invocation encounters an error
	 */
	protected Object onInvoke(Method method, Object[] args) throws Throwable {
		OARestInvokeInfo ii = _onInvoke(method, args);
		return ii.returnObject;
	}

	/**
	 * Performs the full setup and execution of a remote REST invocation, including:
	 * <ul>
	 *   <li>building an {@link OARestInvokeInfo} instance,</li>
	 *   <li>triggering the HTTP call,</li>
	 *   <li>handling errors and response codes,</li>
	 *   <li>mapping returned JSON into the appropriate Java object type.</li>
	 * </ul>
	 *
	 * @param method the reflected method being invoked
	 * @param args   arguments for the invocation
	 * @return the populated {@link OARestInvokeInfo} describing the call and its result
	 * @throws Throwable if metadata, HTTP communication, or JSON conversion fails
	 */
	protected OARestInvokeInfo _onInvoke(Method method, Object[] args) throws Throwable {
		final OARestMethodInfo mi = hmMethodInfo.get(method);

		final OARestInvokeInfo invokeInfo = mi.getInvokeInfo(args, getDefaultIdSeperator());

		invokeInfo.tsStart = System.currentTimeMillis();
		invokeInfo.tsSent = 0;
		invokeInfo.tsEnd = 0;
		invokeInfo.returnObject = null;

		try {
			callHttpEndPoint(invokeInfo);
		} catch (Exception e) {
			invokeInfo.responseException = e;
			invokeInfo.tsEnd = System.currentTimeMillis();
			String s = "exception while calling endpoint, wasSent=" + (invokeInfo.tsSent != 0);
			throw new OARestClientException(invokeInfo, s, e);
		}

		Object obj = null;
		if (invokeInfo.responseCode < 200 || invokeInfo.responseCode > 299) {
			// an OAGet 404 is ok, will return null value
			if (invokeInfo.responseCode != 404 || invokeInfo.methodInfo.methodType != MethodType.OAGet) {
				String s = String.format(	"OARestClient error, Response code=%d, msg=%s",
											invokeInfo.responseCode,
											invokeInfo.responseCodeMessage);
				OARestClientException e = new OARestClientException(invokeInfo, s);
				invokeInfo.responseException = e;
				invokeInfo.tsEnd = System.currentTimeMillis();
				throw e;
			}
		} else {
			try {
				Class c;
				if (OAObject.class.equals(mi.origReturnClass) && invokeInfo.methodReturnClass != null) {
					c = invokeInfo.methodReturnClass;
				} else {
					c = mi.origReturnClass;
				}

				OAJson oaj = new OAJson();
				obj = oaj.readObject(invokeInfo.responseBody, invokeInfo.methodReturnClass, true);

				//was: obj = OAJsonMapper.convertJsonToObject(invokeInfo.responseBody, c, invokeInfo.methodReturnClass);

			} catch (Exception e) {
				String s = "exception converting JSON response to Object";
				invokeInfo.tsEnd = System.currentTimeMillis();
				OARestClientException ex = new OARestClientException(invokeInfo, s, e);
				invokeInfo.responseException = ex;
				throw ex;
			}
		}
		if (mi.returnClassType == ReturnClassType.InvokeInfo) {
			obj = invokeInfo;
		}
		invokeInfo.returnObject = obj;
		invokeInfo.tsEnd = System.currentTimeMillis();
		return invokeInfo;
	}

	/**
	 * Uses the cached {@link OARestClassInfo} for the interface to validate its annotation
	 * configuration and identify any definition errors.
	 *
	 * @param interfaceClass the interface previously used with {@link #getInstance(Class)}
	 * @return a list of configuration error messages, or {@code null} if no metadata exists
	 */
	public ArrayList<String> verify(Class interfaceClass) {
		OARestClassInfo ci = hmClassInfo.get(interfaceClass);
		if (ci == null) {
			return null;
		}
		ArrayList<String> alErrors = ci.verify();
		return alErrors;
	}

	/**
	 * Returns the {@link OARestClassInfo} associated with a previously proxied interface.
	 *
	 * @param interfaceClass the interface class used with {@link #getInstance(Class)}
	 * @return the class-level REST metadata, or {@code null} if none has been loaded
	 */
	public OARestClassInfo getRestClassInfo(Class interfaceClass) {
		if (interfaceClass == null) {
			return null;
		}
		OARestClassInfo classInfo = hmClassInfo.get(interfaceClass);
		return classInfo;
	}

	/**
	 * Gathers and caches reflection-based metadata for the annotated interface, including:
	 * <ul>
	 *   <li>class-level REST annotations,</li>
	 *   <li>method-level REST definitions,</li>
	 *   <li>parameter metadata and type mappings,</li>
	 *   <li>derived invocation configuration.</li>
	 * </ul>
	 *
	 * @param interfaceClass the annotated interface for which metadata is collected
	 * @throws Exception if metadata cannot be analyzed or initialized
	 */
	protected void loadMetaData(Class interfaceClass) throws Exception {
		if (interfaceClass == null) {
			return;
		}

		final OARestClassInfo classInfo = new OARestClassInfo(interfaceClass);
		hmClassInfo.put(interfaceClass, classInfo);

		OARestClass rc = (OARestClass) interfaceClass.getAnnotation(OARestClass.class);
		if (rc != null) {
			classInfo.contextName = rc.contextName();
		}

		Method[] methods = interfaceClass.getMethods();
		for (Method method : methods) {
			OARestMethodInfo mi = new OARestMethodInfo(method);
			mi.classInfo = classInfo;
			classInfo.alMethodInfo.add(mi);

			hmMethodInfo.put(method, mi);

			mi.name = method.getName();
			mi.origReturnClass = mi.returnClass = method.getReturnType();

			if (OARestInvokeInfo.class.equals(mi.origReturnClass)) {
				mi.returnClassType = OARestMethodInfo.ReturnClassType.InvokeInfo;
			} else if (mi.origReturnClass.isArray()) {
				mi.returnClassType = OARestMethodInfo.ReturnClassType.Array;
				mi.returnClass = mi.origReturnClass.getComponentType();
			} else if (List.class.isAssignableFrom(mi.origReturnClass)) {
				mi.returnClassType = OARestMethodInfo.ReturnClassType.List;
				Type type = method.getGenericReturnType();
				if (type instanceof ParameterizedType) {
					Type typex = ((ParameterizedType) type).getActualTypeArguments()[0];
					if (typex instanceof Class) {
						mi.returnClass = (Class) ((ParameterizedType) type).getActualTypeArguments()[0];
					}
				} else {
					mi.returnClass = null; // needs to be defined by method returnClas or param.paramType=MethodReturnClass
				}
			} else if (Hub.class.isAssignableFrom(mi.origReturnClass)) {
				mi.returnClassType = OARestMethodInfo.ReturnClassType.Hub;
				Type type = method.getGenericReturnType();
				if (type instanceof ParameterizedType) {
					Type typex = ((ParameterizedType) type).getActualTypeArguments()[0];
					if (typex instanceof Class) {
						mi.returnClass = (Class) ((ParameterizedType) type).getActualTypeArguments()[0];
					}
				} else {
					mi.returnClass = null; // needs to be defined by method paramType=MethodReturnClass
				}
			} else if (JsonNode.class.isAssignableFrom(mi.origReturnClass)) {
				mi.returnClassType = OARestMethodInfo.ReturnClassType.JsonNode;
			} else if (mi.origReturnClass.equals(String.class)) {
				mi.returnClassType = OARestMethodInfo.ReturnClassType.String;
			} else if (mi.origReturnClass.equals(void.class) || mi.origReturnClass.equals(Void.class)) {
				mi.returnClassType = OARestMethodInfo.ReturnClassType.Void;
			}

			Parameter[] parameters = method.getParameters();
			for (int i = 0; parameters != null && i < parameters.length; i++) {
				OARestParamInfo pi = new OARestParamInfo();
				mi.alParamInfo.add(pi);
				pi.paramType = OARestParam.ParamType.Unassigned;

				pi.name = parameters[i].getName();
				pi.origParamClass = pi.paramClass = parameters[i].getType();

				if (OARestInvokeInfo.class.isAssignableFrom(pi.origParamClass)) {
					pi.classType = ClassType.OARestInvokeInfo;
					pi.paramType = OARestParam.ParamType.OARestInvokeInfo;
				} else if (pi.origParamClass.isArray()) {
					pi.classType = OARestParamInfo.ClassType.Array;
					pi.paramClass = pi.origParamClass.getComponentType();
				} else if (List.class.isAssignableFrom(pi.origParamClass)) {
					pi.classType = OARestParamInfo.ClassType.Array.List;
					Type type = method.getGenericReturnType();
					if (type instanceof ParameterizedType) {
						pi.paramClass = (Class) ((ParameterizedType) type).getActualTypeArguments()[0];
					} else {
						pi.paramClass = null;
					}
				} else if (JsonNode.class.isAssignableFrom(pi.origParamClass)) {
					pi.classType = OARestParamInfo.ClassType.JsonNode;
				} else if (pi.origParamClass.equals(String.class)) {
					pi.classType = OARestParamInfo.ClassType.String;
				} else if (OADate.class.isAssignableFrom(pi.origParamClass)) {
					pi.classType = OARestParamInfo.ClassType.Date;
				} else if (OADateTime.class.isAssignableFrom(pi.origParamClass)) {
					pi.classType = OARestParamInfo.ClassType.DateTime;
				} else if (OATime.class.isAssignableFrom(pi.origParamClass)) {
					pi.classType = OARestParamInfo.ClassType.Time;
				} else if (LocalDate.class.isAssignableFrom(pi.origParamClass)) {
					pi.classType = OARestParamInfo.ClassType.Date;
				} else if (LocalDateTime.class.isAssignableFrom(pi.origParamClass)) {
					pi.classType = OARestParamInfo.ClassType.DateTime;
				} else if (Date.class.isAssignableFrom(pi.origParamClass)) {
					pi.classType = OARestParamInfo.ClassType.Date;
				} else {
					pi.classType = OARestParamInfo.ClassType.Unassigned;
				}

				OARestParam rp = (OARestParam) parameters[i].getAnnotation(OARestParam.class);
				if (rp != null) {
					if (rp.name().length() > 0) {
						pi.name = rp.name();
						pi.bNameAssigned = true;
					}

					if (rp.format().length() > 0) {
						pi.format = rp.format();
					}
					if (rp.type() != null && rp.type() != OARestParam.ParamType.Unassigned) {
						pi.paramType = rp.type();
					}
					if (!rp.paramClass().equals(Void.class)) {
						pi.rpParamClass = pi.paramClass = rp.paramClass();
					}

					if (pi.paramType == OARestParam.ParamType.MethodReturnClass) {
						mi.returnClass = null; // assigned at runtime using this param's class value
					}

					if (pi.paramType == OARestParam.ParamType.BodyByteArray) {
						pi.classType = ClassType.ByteArray;
					}

					pi.includeReferenceLevelAmount = rp.includeReferenceLevelAmount();

					pi.alIncludePropertyPaths = new ArrayList();

					String sx = rp.includePropertyPath();
					if (sx != null && sx.length() > 0) {
						pi.alIncludePropertyPaths.add(rp.includePropertyPath());
					}
					String[] ss = rp.includePropertyPaths();
					if (ss != null && ss.length > 0) {
						for (String s : rp.includePropertyPaths()) {
							if (s.length() > 0) {
								pi.alIncludePropertyPaths.add(s);
							}
						}
					}
				}

				if (OAString.isEmpty(pi.format)) {
					if (pi.classType == OARestParamInfo.ClassType.Date) {
						pi.format = OADate.JsonFormat;
					} else if (pi.classType == OARestParamInfo.ClassType.Time) {
						pi.format = OATime.JsonFormat;
					} else if (pi.classType == OARestParamInfo.ClassType.DateTime) {
						pi.format = OADateTime.JsonFormat;
					}
				}
			}

			boolean bUsesBody = false;
			for (OARestParamInfo pi : mi.alParamInfo) {
				if (pi.paramType == OARestParam.ParamType.BodyObject) {
					bUsesBody = true;
					break;
				}
			}

			// methodType & urlPath
			mi.methodType = OARestMethod.MethodType.Unassigned;

			boolean bFoundUrlPath = false;

			OARestMethod rm = (OARestMethod) mi.method.getAnnotation(OARestMethod.class);
			if (rm != null) {
				if (rm.methodName() != null && rm.methodName().length() > 0) {
					mi.objectMethodName = rm.methodName();
				}

				if (rm.name() != null && rm.name().length() > 0) {
					mi.name = rm.name();
				}

				if (rm.urlQuery() != null && rm.urlQuery().length() > 0) {
					mi.urlQuery = rm.urlQuery();
				}

				if (rm.methodType() != OARestMethod.MethodType.Unassigned) {
					mi.methodType = rm.methodType();
				}
				if (rm.urlPath().length() > 0) {
					mi.urlPath = rm.urlPath();
					bFoundUrlPath = true;
				}

				if (!rm.returnClass().equals(Void.class)) {
					mi.rmReturnClass = rm.returnClass();
				}

				mi.includeReferenceLevelAmount = rm.includeReferenceLevelAmount();

				mi.alIncludePropertyPaths = new ArrayList();

				if (rm.includePropertyPath() != null && rm.includePropertyPath().length() > 0) {
					mi.alIncludePropertyPaths.add(rm.includePropertyPath());
				}
				if (rm.includePropertyPaths() != null && rm.includePropertyPaths().length > 0) {
					for (String s : rm.includePropertyPaths()) {
						if (s.length() > 0) {
							mi.alIncludePropertyPaths.add(s);
						}
					}
				}

				if (rm.urlPath() != null && rm.urlPath().length() > 0) {
					mi.urlPath = rm.urlPath();
				}

				if (rm.searchWhere() != null && rm.searchWhere().length() > 0) {
					mi.searchWhere = rm.searchWhere();
				}
				if (rm.searchOrderBy() != null && rm.searchOrderBy().length() > 0) {
					mi.searchOrderBy = rm.searchOrderBy();
				}
			}
			mi.initialize();
		}
	}

	/*
	 * internally needed for PATCH support because the Java HttpURLConnection does not support httpMethod PATCH
	 */
	/**
	 * Reflected {@link java.lang.reflect.Field} handle for {@link HttpURLConnection} method override used to support PATCH.
	 */
	private static java.lang.reflect.Field fieldHttpURLConnectMethod;
	
	/**
	 * Reflected field used to access the HTTPS delegate connection when configuring PATCH support for {@link HttpsURLConnection}.
	 */
	private static java.lang.reflect.Field fieldHttpsURLConnectMethod1;
	
	/**
	 * Reflected field used to access the nested {@link HttpsURLConnection} instance when configuring PATCH support.
	 */
	private static java.lang.reflect.Field fieldHttpsURLConnectMethod2;

	/**
	 * Executes an HTTP request based on values configured in the supplied
	 * {@link OARestInvokeInfo}, including:
	 * <ul>
	 *   <li>building the final URL and query string,</li>
	 *   <li>setting headers and authentication,</li>
	 *   <li>writing body content,</li>
	 *   <li>reading the response and headers,</li>
	 *   <li>recording timing and error information.</li>
	 * </ul>
	 *
	 * @param invokeInfo contains all parameters and state for the HTTP invocation
	 * @throws Exception if the HTTP connection or I/O fails
	 */
	public void callHttpEndPoint(OARestInvokeInfo invokeInfo)
			throws Exception {

		if (invokeInfo == null) {
			throw new IllegalArgumentException("invokeInfo can not be null");
		}

		String httpUrl;
		if (invokeInfo.urlPath == null) {
			invokeInfo.urlPath = "";
		}

		if (invokeInfo.urlPath.indexOf("://") < 0) {
			httpUrl = getBaseUrl();
			if (httpUrl == null || httpUrl.indexOf("://") < 0) {
				if (OAString.isNotEmpty(this.protocol)) {
					httpUrl = protocol + "://" + httpUrl;
				} else {
					httpUrl = "http://" + httpUrl;
				}
			}

			if (invokeInfo.methodInfo != null && invokeInfo.methodInfo.methodType.toString().startsWith("OA")) {
				httpUrl += OAHttpUtil.updateSlashes(defaultOARestUrl, true, false);
			}
			httpUrl += OAHttpUtil.updateSlashes(invokeInfo.urlPath, true, false);
		} else {
			httpUrl = OAHttpUtil.updateSlashes(invokeInfo.urlPath, false, false);
		}

		httpUrl = OAString.append(httpUrl, invokeInfo.urlQuery, "?");
		httpUrl = OAString.convert(httpUrl, "/?", "?");

		invokeInfo.finalUrl = httpUrl;

		if (httpUrl.toLowerCase().indexOf("https:") == 0) {
			setupHttpsAccess();
		}

		URL url = new URL(httpUrl);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setUseCaches(false);
		conn.setRequestProperty("User-Agent", "OARestClient");

		if ("PATCH".equalsIgnoreCase(invokeInfo.httpMethod)) {
			// Hack for PATCH
			// https://stackoverflow.com/questions/25163131/httpurlconnection-invalid-http-method-patch

			try {
				if (fieldHttpURLConnectMethod == null) {
					java.lang.reflect.Field fld = HttpURLConnection.class.getDeclaredField("method");
					fld.setAccessible(true);
					fieldHttpURLConnectMethod = fld;
				}
				fieldHttpURLConnectMethod.set(conn, invokeInfo.httpMethod);
			} catch (Throwable t) {
				conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
				conn.setRequestMethod("POST");
			}
			if (conn instanceof HttpsURLConnection) {
				try {
					if (fieldHttpsURLConnectMethod1 == null) {
						java.lang.reflect.Field fld = HttpsURLConnection.class.getDeclaredField("delegate");
						fld.setAccessible(true);
						fieldHttpsURLConnectMethod1 = fld;
					}
					Object conx = fieldHttpsURLConnectMethod1.get(conn);
					if (conx instanceof HttpURLConnection) {
						fieldHttpURLConnectMethod.setAccessible(true);
						fieldHttpURLConnectMethod.set(conx, invokeInfo.httpMethod);
					}

					if (fieldHttpsURLConnectMethod2 == null) {
						java.lang.reflect.Field fld = conx.getClass().getDeclaredField("httpsURLConnection");
						fld.setAccessible(true);
						fieldHttpsURLConnectMethod2 = fld;
					}
					HttpsURLConnection con2 = (HttpsURLConnection) fieldHttpsURLConnectMethod2.get(conx);

					fieldHttpURLConnectMethod.set(con2, invokeInfo.httpMethod);
				} catch (Throwable t) {
					// can ignore
					// System.out.println("Error setting up HTTP PATCH for HTTPS");
					// t.printStackTrace();
				}
			}
		} else {
			conn.setRequestMethod(invokeInfo.httpMethod.toUpperCase());
		}

		conn.setDoOutput(true);

		conn.setDoInput(true);
		conn.setUseCaches(false);
		conn.setAllowUserInteraction(false);

		if (OAString.isEmpty(invokeInfo.contentType)) {
			if (invokeInfo.byteArrayBody != null) {
				conn.setRequestProperty("Content-Type", "application/octet-stream");
				conn.setRequestProperty("Content-Length", "" + invokeInfo.byteArrayBody.length);
			} else if (OAString.isNotEmpty(invokeInfo.textBody)) {
				conn.setRequestProperty("Content-Type", "text/plain;charset=UTF-8");
			} else if (OAString.isNotEmpty(invokeInfo.jsonBody)) {
				conn.setRequestProperty("Content-Type", "application/json");
			} else if (OAString.isNotEmpty(invokeInfo.formData)) {
				conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			}
		}
		conn.setRequestProperty("charset", "utf-8");
		conn.setRequestProperty("Accept", "application/json, text/*;q=0.7");

		if (OAString.isNotEmpty(cookie)) {
			conn.addRequestProperty("cookie", cookie);
		}

		if (OAString.isNotEmpty(userId)) {
			String s = userId + ":" + password;
			conn.setRequestProperty("Authorization", "Basic " + Base64.encode(s));
		}

		for (Map.Entry<String, String> me : invokeInfo.hsHeaderOut.entrySet()) {
			String key = me.getKey();
			String value = me.getValue();
			if (OAString.isNotEmpty(this.cookie) && key.equalsIgnoreCase("cookie")) {
				if (OAString.isEmpty(value)) {
					continue;
				}
				if (cookie != null && value.indexOf(cookie) < 0) {
					value += ", " + cookie;
				}
			}
			conn.setRequestProperty(key, value);
		}

		if (invokeInfo.byteArrayBody != null) {
			OutputStream out = conn.getOutputStream();
			out.write(invokeInfo.byteArrayBody);
			out.close();
		} else if (OAString.isNotEmpty(invokeInfo.jsonBody)) {
			OutputStream out = conn.getOutputStream();
			Writer writer = new OutputStreamWriter(out, "UTF-8");

			writer.write(invokeInfo.jsonBody);
			writer.close();
			out.close();
		} else if (OAString.isNotEmpty(invokeInfo.textBody)) {
			OutputStream out = conn.getOutputStream();
			Writer writer = new OutputStreamWriter(out, "UTF-8");

			writer.write(invokeInfo.textBody);
			writer.close();
			out.close();
		} else if (OAString.isNotEmpty(invokeInfo.formData)) {
			OutputStream out = conn.getOutputStream();
			Writer writer = new OutputStreamWriter(out, "UTF-8");

			writer.write(invokeInfo.formData);
			writer.close();
			out.close();
		}

		invokeInfo.tsSent = System.currentTimeMillis();

		for (Map.Entry<String, List<String>> me : conn.getHeaderFields().entrySet()) {
			String s = "";
			boolean b = false;
			for (String s2 : me.getValue()) {
				if (!b) {
					b = true;
				} else {
					s += ", ";
				}
				s += s2;
			}
			invokeInfo.hsHeaderIn.put(me.getKey(), s);
		}

		String setcookie = conn.getHeaderField("Set-Cookie");
		if (OAString.isNotEmpty(setcookie)) {
			this.cookie = OAString.field(setcookie, ";", 1);
		}

		// https://developer.mozilla.org/en-US/docs/Web/HTTP/Status
		invokeInfo.responseCode = conn.getResponseCode();
		invokeInfo.responseCodeMessage = conn.getResponseMessage();

		StringBuilder sb = new StringBuilder();
		InputStream inputStream = conn.getInputStream();

		// HTTP Response
		// https://www.w3.org/Protocols/rfc2616/rfc2616-sec6.html

		if (inputStream != null) {
			BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line + "\n");
			}
			br.close();
		}
		conn.disconnect();

		invokeInfo.responseBody = sb.toString();
	}

	/**
	 * Convenience method that creates an {@link OARestInvokeInfo}, performs a JSON-based
	 * HTTP request, and returns the raw JSON response body.
	 *
	 * @param httpMethod the HTTP method to use
	 * @param urlPath    the URL path relative to the base URL
	 * @param query      optional query string
	 * @param jsonBody   JSON text to send as the request body
	 * @return the JSON response body returned by the server
	 * @throws Exception if the HTTP request fails
	 */
	public String callJsonEndpoint(String httpMethod, String urlPath, String query, String jsonBody) throws Exception {
		OARestInvokeInfo invokeInfo = new OARestInvokeInfo();
		invokeInfo.urlPath = urlPath;
		invokeInfo.urlQuery = query;
		invokeInfo.httpMethod = httpMethod;
		invokeInfo.jsonBody = jsonBody;

		callHttpEndPoint(invokeInfo);
		return invokeInfo.responseBody;
	}

	/**
	 * Ensures HTTPS trust configuration is installed; initializes relaxed certificate
	 * and hostname verification on first invocation.
	 *
	 * @throws Exception if setup fails
	 */
	public static void setupHttpsAccess() throws Exception {
		if (bSetupHttpsAccess) {
			return;
		}
		try {
			_setupHttpsAccess();
			bSetupHttpsAccess = true;
		} catch (Exception e) {
			throw new RuntimeException("OARestClient.setupHttpsAccess failed", e);
		}
	}

	/**
	 * Installs a permissive SSL context and hostname verifier that accept all
	 * certificates and hostnames, used for development or testing environments.
	 *
	 * @throws Exception if SSL configuration fails
	 */
	protected static void _setupHttpsAccess() throws Exception {
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}
		} };

		// trust manager
		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

		// create host name verifier
		HostnameVerifier allHostsValid = new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};

		HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
	}

	/*
	 * Call an HTTP endPoint.
	 *
	 * @param ii          (optional) defines details about how to call the server.
	 * @param httpMethod  ex: GET, POST, DELETE, etc
	 * @param urlPath     url path to go with base url
	 * @param urlQuery    url query
	 * @param mapUrlQuery name/value pairs for url query
	 * @param jsonBody    JSON object(s) to send as http body
	 * @param mapFormData
	 * @return restInvokeInfo that defines the http call.
	 */
	/**
	 * Performs a general-purpose REST call using the supplied parameters to populate
	 * or update an {@link OARestInvokeInfo}. URL query parameters, form data, and JSON
	 * body content are encoded as needed before the HTTP call is executed.
	 *
	 * @param ii          optional existing invocation info to populate
	 * @param httpMethod  HTTP method to use
	 * @param urlPath     request URL path
	 * @param urlQuery    initial URL query string
	 * @param mapUrlQuery additional query name/value pairs
	 * @param jsonBody    JSON body text
	 * @param mapFormData form data name/value pairs
	 * @return the updated invocation info after executing the HTTP request
	 * @throws Exception if the HTTP call fails
	 */
	public OARestInvokeInfo callEndPoint(OARestInvokeInfo ii, String httpMethod, String urlPath, String urlQuery,
			Map<String, Object> mapUrlQuery,
			String jsonBody,
			Map<String, Object> mapFormData) throws Exception {
		if (ii == null) {
			ii = new OARestInvokeInfo();
		}

		ii.httpMethod = httpMethod;
		ii.urlPath = urlPath;

		ii.urlQuery = urlQuery;

		String s = OAHttpUtil.getUrlEncodedNameValues(mapUrlQuery);
		if (OAString.isNotEmpty(s)) {
			if (OAString.isNotEmpty(ii.urlQuery)) {
				ii.urlQuery += "&";
			} else if (ii.urlQuery == null) {
				ii.urlQuery = "";
			}
			ii.urlQuery += s;
		}

		ii.formData = OAHttpUtil.getUrlEncodedNameValues(mapFormData);

		ii.jsonBody = jsonBody;

		callHttpEndPoint(ii);

		return ii;
	}

	/*
	 * call an OARestServlet to access data in an OAGraph.
	 *
	 * @param ii            (optional) defines details about how to call the server.
	 * @param clazz         OAObject class that is being called.
	 * @param searchWhere   object query search
	 * @param searchOrderBy sort order by
	 * @param includePPs    extra property paths to include in the results.
	 * @return InvokeInfo with details (including JSON result)
	 */
	/**
	 * Issues an OA REST GET request to retrieve a set of OAObjects matching a query,
	 * optionally including additional property paths in the response.
	 *
	 * @param ii            optional invocation info to use
	 * @param clazz         OAObject class to operate on
	 * @param searchWhere   query expression
	 * @param searchOrderBy ordering expression
	 * @param includePPs    optional property paths to include in results
	 * @return the populated invocation info containing the JSON response
	 * @throws Exception if the HTTP request fails
	 */
	public OARestInvokeInfo callOASelect(OARestInvokeInfo ii, Class<? extends OAObject> clazz, String searchWhere, String searchOrderBy,
			final String... includePPs)
			throws Exception {
		if (ii == null) {
			ii = new OARestInvokeInfo();
		}

		ii.httpMethod = "GET";
		ii.urlPath = defaultOARestUrl;

		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(clazz);
		final OAObjectInfoService srvcObjectInfo = og.getOAObjectService().getOAObjectInfoService();
		OAObjectInfo oi = srvcObjectInfo.getOAObjectInfo(clazz);
		ii.urlPath += OAHttpUtil.updateSlashes(OAString.mfcl(oi.getPluralName()), true, false);

		ii.urlQuery = "";

		if (OAString.isNotEmpty(searchWhere)) {
			try {
				ii.urlQuery = "query=" + URLEncoder.encode(searchWhere, "UTF-8");
			} catch (Exception e) {
			}
		}
		if (OAString.isNotEmpty(searchOrderBy)) {
			if (OAString.isNotEmpty(ii.urlQuery)) {
				ii.urlQuery += "&";
			}
			try {
				ii.urlQuery += "orderBy=" + URLEncoder.encode(searchOrderBy, "UTF-8");
			} catch (Exception e) {
			}
		}

		if (includePPs != null) {
			if (OAString.isNotEmpty(ii.urlQuery)) {
				ii.urlQuery += "&";
			}
			for (String s : includePPs) {
				ii.urlQuery += "pp=" + URLEncoder.encode(s, "UTF-8");
			}
		}

		callHttpEndPoint(ii);
		return ii;
	}

	/**
	 * Retrieves an OAObject from an OA REST servlet by its primary key, optionally
	 * including additional property paths.
	 *
	 * @param ii         optional invocation info to use
	 * @param clazz      OAObject class to retrieve
	 * @param id         primary key value
	 * @param includePPs property paths to include
	 * @return invocation info containing the HTTP response
	 * @throws Exception if the request fails
	 */
	public OARestInvokeInfo callOAGet(OARestInvokeInfo ii, Class<? extends OAObject> clazz, Object id,
			final String... includePPs) throws Exception {
		return callOAGet(ii, clazz, id, null, includePPs);
	}

	/**
	 * Retrieves an OAObject with a two-part key from an OA REST servlet, optionally
	 * including additional property paths.
	 *
	 * @param ii         optional invocation info to use
	 * @param clazz      OAObject class to retrieve
	 * @param id         first key value
	 * @param id2        second key value
	 * @param includePPs property paths to include
	 * @return invocation info containing the HTTP response
	 * @throws Exception if the request fails
	 */
	public OARestInvokeInfo callOAGet(OARestInvokeInfo ii, Class<? extends OAObject> clazz, Object id, Object id2,
			final String... includePPs) throws Exception {
		if (ii == null) {
			ii = new OARestInvokeInfo();
		}

		ii.httpMethod = "GET";
		ii.urlPath = defaultOARestUrl;

		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(clazz);
		final OAObjectInfoService srvcObjectInfo = og.getOAObjectService().getOAObjectInfoService();
		OAObjectInfo oi = srvcObjectInfo.getOAObjectInfo(clazz);
		ii.urlPath += "/" + OAString.mfcl(clazz.getSimpleName());

		ii.urlPath += "/" + OAConv.toString(id);
		if (OAString.isNotEmpty(id2)) {
			ii.urlPath += "/" + OAConv.toString(id2);
		}

		ii.urlQuery = "";

		if (includePPs != null) {
			if (OAString.isNotEmpty(ii.urlQuery)) {
				ii.urlQuery += "&";
			}
			for (String s : includePPs) {
				ii.urlQuery += "pp=" + URLEncoder.encode(s, "UTF-8");
			}
		}

		callHttpEndPoint(ii);
		return ii;
	}

	/**
	 * Inserts a new OAObject using POST semantics, serializing the object to JSON and
	 * optionally including additional property paths in the payload.
	 *
	 * @param ii         optional invocation info to use
	 * @param obj        the object to insert
	 * @param includePPs property paths to include
	 * @return invocation info containing the HTTP response
	 * @throws Exception if the request fails
	 */
	public <T extends OAObject> OARestInvokeInfo callOAInsert(OARestInvokeInfo ii, T obj,
			final String... includePPs) throws Exception {
		if (obj == null) {
			return ii;
		}
		if (ii == null) {
			ii = new OARestInvokeInfo();
		}

		ii.httpMethod = "POST";
		ii.urlPath = defaultOARestUrl;

		Class clazz = obj.getClass();
		ii.urlPath += "/" + OAString.mfcl(clazz.getSimpleName());

		OAJson oaj = new OAJson();
		if (includePPs != null) {
			oaj.addPropertyPaths(Arrays.asList(includePPs));
		}

		String json = oaj.write(obj);

		// was: String json = OAJsonMapper.convertObjectToJson(obj, includePPs == null ? null : Arrays.asList(includePPs));

		ii.jsonBody = json;

		callHttpEndPoint(ii);
		return ii;
	}

	/**
	 * Deletes an OAObject from the OA REST servlet using DELETE semantics and the
	 * object's key value encoded in the URL.
	 *
	 * @param ii  optional invocation info to use
	 * @param obj the object to delete
	 * @return invocation info describing the HTTP response
	 * @throws Exception if the request fails
	 */
	public <T extends OAObject> OARestInvokeInfo callOADelete(OARestInvokeInfo ii, T obj) throws Exception {
		if (obj == null) {
			return ii;
		}
		if (ii == null) {
			ii = new OARestInvokeInfo();
		}

		ii.httpMethod = "DELETE";
		ii.urlPath = defaultOARestUrl;

		Class clazz = obj.getClass();
		ii.urlPath += "/" + OAString.mfcl(clazz.getSimpleName());

		ii.urlPath += "/" + OAJson.convertObjectKeyToJsonSinglePartId(obj.getObjectKey());

		callHttpEndPoint(ii);
		return ii;
	}

	/**
	 * Updates an existing OAObject using PUT semantics. The object's key is encoded
	 * into the URL, and the object is serialized to JSON for the request body. Optional
	 * property paths may be included in the JSON.
	 *
	 * @param ii         optional invocation info to use
	 * @param obj        the object to update
	 * @param includePPs property paths to include
	 * @return invocation info describing the HTTP response
	 * @throws Exception if the request fails
	 */
	public <T extends OAObject> OARestInvokeInfo callOAUpdate(OARestInvokeInfo ii, T obj,
			final String... includePPs) throws Exception {
		if (obj == null) {
			return ii;
		}
		if (ii == null) {
			ii = new OARestInvokeInfo();
		}

		ii.httpMethod = "PUT";
		ii.urlPath = defaultOARestUrl;

		Class clazz = obj.getClass();
		ii.urlPath += "/" + OAString.mfcl(clazz.getSimpleName());

		ii.urlPath += "/" + OAJson.convertObjectKeyToJsonSinglePartId(obj.getObjectKey());

		OAJson oaj = new OAJson();
		if (includePPs != null) {
			oaj.addPropertyPaths(Arrays.asList(includePPs));
		}

		String json = oaj.write(obj);

		//was: String json = OAJsonMapper.convertObjectToJson(obj, includePPs == null ? null : Arrays.asList(includePPs));

		ii.jsonBody = json;

		callHttpEndPoint(ii);
		return ii;
	}
}
