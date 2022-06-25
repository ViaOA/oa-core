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
import com.viaoa.hub.Hub;
import com.viaoa.json.OAJson;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
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
public class OARestClient {
	private String protocol; // http, https
	private String baseUrl; // www.test.com:8080

	private String defaultOARestUrl = "/servlet/oarest"; // when MethodType=OA*

	/**
	 * object ID separator used for compound IDs. Note that JSON "prefers" single ID values.<br>
	 * Common values are "/", "_", "-" <br>
	 * default: "/"
	 */
	private String defaultIdSeperator = "/";

	private String userId;
	private transient String password;
	private String cookie;

	private final HashMap<Class, OARestClassInfo> hmClassInfo = new HashMap<>();
	private final HashMap<Method, OARestMethodInfo> hmMethodInfo = new HashMap<>();

	private final HashMap<Class, Object> hmRemoteObjectInstance = new HashMap<>();

	private static boolean bSetupHttpsAccess;

	/**
	 * Create an OARestClient that can be used to call another server using HTTP.
	 */
	public OARestClient() {
	}

	public void setUserPw(String userId, String pw) {
		this.userId = userId;
		this.password = pw;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setProtocol(String p) {
		this.protocol = p;
	}

	public String getProtocol() {
		return this.protocol;
	}

	public void setDefaultOARestUrl(String defaultOARestUrl) {
		this.defaultOARestUrl = defaultOARestUrl;
	}

	public String getDefaultOARestUrl() {
		return defaultOARestUrl;
	}

	public void setDefaultIdSeperator(String defaultIdSeperator) {
		this.defaultIdSeperator = defaultIdSeperator;
	}

	public String getDefaultIdSeperator() {
		return defaultIdSeperator;
	}

	/**
	 * Used to create and instance of a Java interface that has been annotated using OARest* for the class, methods, and method parameters.
	 * <p>
	 * This will use a Java proxy object to manage all of the method calls.
	 * <p>
	 *
	 * @param clazz Java interface to create an instance of.
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
	 * Manages any method that is called from a remote object.
	 */
	protected Object onInvoke(Method method, Object[] args) throws Throwable {
		OARestInvokeInfo ii = _onInvoke(method, args);
		return ii.returnObject;
	}

	/**
	 * Creates an OARestInvokeInfo to setup and manage a remote method call over HTTP
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

	public ArrayList<String> verify(Class interfaceClass) {
		OARestClassInfo ci = hmClassInfo.get(interfaceClass);
		if (ci == null) {
			return null;
		}
		ArrayList<String> alErrors = ci.verify();
		return alErrors;
	}

	/**
	 * Get OARestClassInfo about an existing Java instance created from calling getInstance(class)
	 * <p>
	 *
	 * @see OARestClassInfo#verify() to find any issues with the interface annotations and methods.
	 */
	public OARestClassInfo getRestClassInfo(Class interfaceClass) {
		if (interfaceClass == null) {
			return null;
		}
		OARestClassInfo classInfo = hmClassInfo.get(interfaceClass);
		return classInfo;
	}

	/**
	 * Used by getInstance to gather class/method/parameter metadata to be able to make an HTTP method call for each method in the Java
	 * interface.
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

	/**
	 * internally needed for PATCH support because the Java HttpURLConnection does not support httpMethod PATCH
	 */
	private static java.lang.reflect.Field fieldHttpURLConnectMethod;
	private static java.lang.reflect.Field fieldHttpsURLConnectMethod1;
	private static java.lang.reflect.Field fieldHttpsURLConnectMethod2;

	protected void callHttpEndPoint(OARestInvokeInfo invokeInfo)
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
					fieldHttpURLConnectMethod = HttpURLConnection.class.getDeclaredField("method");
					fieldHttpURLConnectMethod.setAccessible(true);
				}
				fieldHttpURLConnectMethod.set(conn, invokeInfo.httpMethod);
			} catch (Throwable t) {
				conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
				conn.setRequestMethod("POST");
			}
			if (conn instanceof HttpsURLConnection) {
				try {
					if (fieldHttpsURLConnectMethod1 == null) {
						fieldHttpsURLConnectMethod1 = HttpsURLConnection.class.getDeclaredField("delegate");
						fieldHttpsURLConnectMethod1.setAccessible(true);
					}
					Object conx = fieldHttpsURLConnectMethod1.get(conn);
					if (conx instanceof HttpURLConnection) {
						fieldHttpURLConnectMethod.setAccessible(true);
						fieldHttpURLConnectMethod.set(conx, invokeInfo.httpMethod);
					}

					if (fieldHttpsURLConnectMethod2 == null) {
						fieldHttpsURLConnectMethod2 = conx.getClass().getDeclaredField("httpsURLConnection");
						fieldHttpsURLConnectMethod2.setAccessible(true);
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

		if (invokeInfo.byteArrayBody != null) {
			conn.setRequestProperty("Content-Type", "application/octet-stream");
			conn.setRequestProperty("Content-Length", "" + invokeInfo.byteArrayBody.length);
		} else if (OAString.isNotEmpty(invokeInfo.jsonBody)) {
			conn.setRequestProperty("Content-Type", "application/json");
		} else if (OAString.isNotEmpty(invokeInfo.formData)) {
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
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
	 * Manually call an HTTP end point using JSON and expecting JSON return value.
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
	 * Configure HTTPS support.
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

	/**
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

	/**
	 * call an OARestServlet to access data in an OAGraph.
	 *
	 * @param ii            (optional) defines details about how to call the server.
	 * @param clazz         OAObject class that is being called.
	 * @param searchWhere   object query search
	 * @param searchOrderBy sort order by
	 * @param includePPs    extra property paths to include in the results.
	 * @return InvokeInfo with details (including JSON result)
	 */
	public OARestInvokeInfo callOASelect(OARestInvokeInfo ii, Class<? extends OAObject> clazz, String searchWhere, String searchOrderBy,
			final String... includePPs)
			throws Exception {
		if (ii == null) {
			ii = new OARestInvokeInfo();
		}

		ii.httpMethod = "GET";
		ii.urlPath = defaultOARestUrl;

		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);
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
	 * call an OARestServlet to access an object from an OAGraph.
	 *
	 * @param ii         (optional) defines details about how to call the server.
	 * @param clazz      OAObject class that is being called.
	 * @param id         the object key value of object.
	 * @param includePPs extra property paths to include in the results.
	 * @return InvokeInfo with details (including JSON result)
	 */
	public OARestInvokeInfo callOAGet(OARestInvokeInfo ii, Class<? extends OAObject> clazz, Object id,
			final String... includePPs) throws Exception {
		return callOAGet(ii, clazz, id, null, includePPs);
	}

	/**
	 * call an OARestServlet to access an object with 2 part key, from an OAGraph.
	 *
	 * @param ii         (optional) defines details about how to call the server.
	 * @param clazz      OAObject class that is being called.
	 * @param id         the object key value of object.
	 * @param includePPs extra property paths to include in the results.
	 * @return InvokeInfo with details (including JSON result)
	 */
	public OARestInvokeInfo callOAGet(OARestInvokeInfo ii, Class<? extends OAObject> clazz, Object id, Object id2,
			final String... includePPs) throws Exception {
		if (ii == null) {
			ii = new OARestInvokeInfo();
		}

		ii.httpMethod = "GET";
		ii.urlPath = defaultOARestUrl;

		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);
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
	 * call an OARestServlet to insert an object.
	 *
	 * @param ii         (optional) defines details about how to call the server.
	 * @param obj        object to insert.
	 * @param includePPs extra property paths to include in the results.
	 * @return InvokeInfo with details (including JSON result)
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
	 * call an OARestServlet to insert an object.
	 *
	 * @param ii  (optional) defines details about how to call the server.
	 * @param obj object to insert.
	 * @return InvokeInfo with details (including JSON result)
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
	 * call an OARestServlet to update an object.
	 *
	 * @param ii         (optional) defines details about how to call the server.
	 * @param obj        object to update.
	 * @param includePPs extra property paths to include in the results.
	 * @return InvokeInfo with details (including JSON result)
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
