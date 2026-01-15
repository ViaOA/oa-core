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

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.viaoa.graph.object.OAObjectInfoService;
import com.viaoa.json.OAJson;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectKey;
import com.viaoa.remote.rest.OARestClientException;
import com.viaoa.remote.rest.annotation.OARestMethod;
import com.viaoa.remote.rest.annotation.OARestMethod.MethodType;
import com.viaoa.remote.rest.annotation.OARestParam;
import com.viaoa.remote.rest.annotation.OARestParam.ParamType;
import com.viaoa.remote.rest.info.OARestParamInfo.ClassType;
import com.viaoa.runtime.OARuntime;
import com.viaoa.template.OATemplate;
import com.viaoa.util.OAConv;
import com.viaoa.util.OAHttpUtil;
import com.viaoa.util.OAReflect;
import com.viaoa.util.OAString;

/**
 * Metadata for a single REST-accessible method declared in an interface
 * annotated with {@code @OARestClass}. This object is created during annotation
 * scanning and provides all information required to turn a Java method
 * invocation into an HTTP request.
 *
 * <h2>Captures</h2>
 * <ul>
 *   <li>HTTP method (GET, POST, PUT, PATCH, DELETE)</li>
 *   <li>URL path template and query construction rules</li>
 *   <li>Return type and any wrapper or container information</li>
 *   <li>List of {@link OARestParamInfo} describing annotated parameters</li>
 *   <li>Serialization mode (JSON body, query params, form params, etc.)</li>
 *   <li>Flags controlling OAObject streaming and property-path expansion</li>
 *   <li>Optional path rewrite or custom URL composition instructions</li>
 * </ul>
 *
 * <h2>Role in Invocation</h2>
 * {@code OARestMethodInfo} acts as the complete definition of how a method
 * should be executed remotely. During the invocation:
 * <ul>
 *   <li>URL components are assembled from annotation metadata.</li>
 *   <li>Parameters are bound to the path, query string, or request body.</li>
 *   <li>The return type is used to deserialize the response JSON.</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * Instances are immutable after construction and safe for concurrent access
 * by multiple remote invocations.
 *
 * @author vvia
 */
public class OARestMethodInfo {

	/**
	 * Annotation instance defining how this Java method is exposed over REST.
	 * Populated from {@link com.viaoa.remote.rest.annotation.OARestMethod}.
	 */
	public OARestMethod restMethod;

	/**
	 * Reference to the parent {@link OARestClassInfo} that owns this method
	 * and provides interface-level REST metadata.
	 */
	public OARestClassInfo classInfo;

	/**
	 * Java reflection {@link Method} object for the interface method that this
	 * metadata instance describes.
	 */
	public Method method;

	/**
	 * Logical name of the REST method, usually the Java method name unless
	 * overridden by the annotation.
	 */
	public String name;
	
	/**
	 * Configured URL path from the {@code @OARestMethod} annotation, before any
	 * automatic derivation or template expansion is applied.
	 */
	public String urlPath;
	
	/**
	 * URL path that is derived automatically from return type, method type, or
	 * other metadata when the annotation does not supply an explicit path.
	 */
	public String derivedUrlPath;
	
	/**
	 * Template used to expand tokens (such as class name or ID) into the final
	 * URL path at invocation time.
	 */
	public OATemplate urlPathTemplate;

	/**
	 * Static query-string fragment configured on the method, before adding any
	 * parameter-based query values.
	 */
	public String urlQuery;

	/**
	 * Ordered list of parameter metadata, one {@link OARestParamInfo} per
	 * Java method parameter.
	 */
	public ArrayList<OARestParamInfo> alParamInfo = new ArrayList();

	/**
	 * Name of the OAObject method to invoke for OAObject-based remote calls,
	 * when required by the selected {@link MethodType}.
	 */
	public String objectMethodName;

	/**
	 * Raw return type from the Java method signature, before any generic or
	 * container-type resolution is applied.
	 */
	public Class origReturnClass;
	
	/**
	 * Return class explicitly supplied by the {@code @OARestMethod.returnClass}
	 * annotation, when used to override or clarify the actual element type.
	 */
	public Class rmReturnClass;
	
	/**
	 * Effective return class used for JSON deserialization, resolved from the
	 * original return type, generics, and optional annotation overrides.
	 */
	public Class returnClass;

	/**
	 * REST method type that controls HTTP verb selection, URL derivation, and
	 * valid parameter/annotation combinations.
	 */
	public OARestMethod.MethodType methodType;
	
	/**
	 * Classification of the method's return shape (void, String, array, List,
	 * Hub, JsonNode, or InvokeInfo) used during validation and deserialization.
	 */
	public ReturnClassType returnClassType;

	/**
	 * Collected configuration and verification error messages for this method.
	 * Populated by {@link #initialize()} and various verify* methods.
	 */
	public ArrayList<String> alErrors;

	/**
	 * Enumerates the high-level shapes that a method's return value can take.
	 * Used to drive validation rules and response handling behavior.
	 */
	public static enum ReturnClassType {
		/**
		 * Indicates that the return type classification has not been determined.
		 * Verification should flag this as an error.
		 */
		Unassigned,
		/**
		 * Marks methods that do not return a value (void or {@link Void}).
		 */
		Void,
		/**
		 * Marks methods that return a {@link String} value.
		 */
		String,
		Array,
		List,
		Hub,
		/**
		 * Indicates the method returns a JSON tree structure (OAJsonNode or similar),
		 * allowing arbitrary JSON content to be returned without a specific Java type.
		 */
		JsonNode,
		InvokeInfo
	}

	/**
	 * Depth of reference expansion to include when serializing OAObjects in
	 * the response.
	 * <p>
	 * A value greater than zero enables automatic traversal of referenced
	 * objects up to the configured level, and is validated in
	 * {@link #verifyIncludeReferenceLevelAmount(String, java.util.List)}
	 * against the resolved {@link #returnClass}.
	 */
	public int includeReferenceLevelAmount;

	/**
	 * List of property-path expressions to include when serializing OAObjects
	 * in the response.
	 * <p>
	 * When present, each path is appended as a {@code pp=} query parameter in
	 * {@link #getInvokeInfo(Object[], String)} and validated by
	 * {@link #verifyIncludePropertyPaths(String, java.util.List)} to ensure
	 * that it only applies to OAObject-based return types.
	 */
	public List<String> alIncludePropertyPaths;

	/**
	 * Static search filter expression applied when the method performs a
	 * search-style operation.
	 * <p>
	 * Used primarily for {@code OASearch} and related method types and
	 * validated by the corresponding verify* methods to ensure that it is
	 * only configured when supported for the current {@link #methodType}.
	 */
	public String searchWhere;

	/**
	 * Static sort expression applied to search results for this method.
	 * <p>
	 * Relevant for search-style method types and checked by the verification
	 * logic so that it is not used with method types that do not support
	 * server-side ordering.
	 */
	public String searchOrderBy;

	/**
	 * Creates a new metadata instance for a REST-accessible method.
	 *
	 * @param method the Java {@link Method} being described; used to
	 *               retrieve annotations and reflective information
	 *               required for validation and request construction.
	 *
	 * <p>
	 * The constructor stores the method reference and extracts its
	 * {@link OARestMethod} annotation, if present. All remaining values
	 * are initialized later during {@link #initialize()}.
	 */
	public OARestMethodInfo(Method method) {
		this.method = method;
		this.restMethod = method.getAnnotation(OARestMethod.class);
	}

	/**
	 * Returns the list of validation errors accumulated during
	 * {@link #initialize()}.
	 *
	 * @return list of error messages, or {@code null} if no
	 *         verification has been performed.
	 */
	public List<String> verify() {
		return alErrors;
	}

	/**
	 * Initializes and validates all metadata for this method.
	 * <p>
	 * Creates the error list, verifies required annotation state,
	 * applies default parameter settings, validates method type,
	 * URL path, query, return class, paging, parameter types,
	 * and other rules enforced by the verify* methods. Any
	 * detected errors are added to {@link #alErrors}.
	 */
	public void initialize() {
		alErrors = new ArrayList();

		if (restMethod == null) {
			alErrors.add("RestMethod annotation is missing");
			return;
		}

		String msgPrefix = String.format("method name=%s, type=%s, ", name, methodType);

		setDefaults();

		verifyMethodType(msgPrefix, alErrors);

		verifyUrlPath(msgPrefix, alErrors);
		verifyDerviedUrlPath(msgPrefix, alErrors);

		verifyUrlQuery(msgPrefix, alErrors);

		verifyIncludePropertyPaths(msgPrefix, alErrors);
		verifyIncludeReferenceLevelAmount(msgPrefix, alErrors);

		verifyMethodReturnClass(msgPrefix, alErrors);

		verifyMethodPageSize(msgPrefix, alErrors);

		verifyMethodTypeGET(msgPrefix, alErrors);
		verifyMethodTypeOAGet(msgPrefix, alErrors);
		verifyMethodTypeOASearch(msgPrefix, alErrors);
		verifyMethodTypePOST(msgPrefix, alErrors);
		verifyMethodTypePUT(msgPrefix, alErrors);
		verifyMethodTypePATCH(msgPrefix, alErrors);
		verifyMethodTypeOAObjectMethodCall(msgPrefix, alErrors);
		verifyMethodTypeOARemote(msgPrefix, alErrors);
		verifyMethodTypeOAInsert(msgPrefix, alErrors);
		verifyMethodTypeOAUpdate(msgPrefix, alErrors);
		verifyMethodTypeOADelete(msgPrefix, alErrors);

		verifyParamAmounts(msgPrefix, alErrors);
		verifyRestParams(msgPrefix, alErrors);
	}

	/**
	 * Applies default parameter-type assignments based on the
	 * configured {@link #methodType}.
	 * <p>
	 * Parameters with an undefined or unassigned type will adopt the
	 * default type for certain method categories, such as
	 * {@code MethodCallArg} for {@code OARemote}. All other fields
	 * remain unchanged.
	 */
	public void setDefaults() {
		ParamType ptDefault = null;
		switch (methodType) {
		case GET:
		case OAGet:
		case OASearch:
		case POST:
		case PUT:
		case PATCH:
		case OAObjectMethodCall:
			break;
		case OARemote:
			ptDefault = ParamType.MethodCallArg;
			break;
		case OAInsert:
		case OAUpdate:
		case OADelete:
		}

		for (OARestParamInfo pi : alParamInfo) {
			if (pi.paramType == null || pi.paramType == ParamType.Unassigned) {
				if (ptDefault != null) {
					pi.paramType = ptDefault;
				}
			}
		}
	}

	/**
	 * Performs detailed validation of each {@link OARestParamInfo} entry.
	 *
	 * @param msgPrefix prefix used when constructing error messages
	 * @param alErrors  collection to receive validation errors
	 *
	 * <p>
	 * This routine evaluates every parameter against its expected
	 * configuration rules: allowed combinations, required names,
	 * type usage, class constraints, restrictions on body and
	 * byte-array parameters, and mutual exclusion rules. Errors are
	 * appended to the supplied list.
	 */
	public void verifyRestParams(String msgPrefix, List<String> alErrors) {
		String origMsgPrefix = msgPrefix;
		for (OARestParamInfo pi : alParamInfo) {
			msgPrefix = origMsgPrefix + "paramType=" + pi.paramType + ", ";

			verifyParamType(msgPrefix, alErrors, pi, ParamType.Ignore, false, false, false, false);
			verifyParamType(msgPrefix, alErrors, pi, ParamType.MethodUrlPath, false, false, false, false, true);
			verifyParamType(msgPrefix, alErrors, pi, ParamType.MethodSearchWhere, false, false, false, false, true);
			verifyParamType(msgPrefix, alErrors, pi, ParamType.MethodSearchOrderBy, false, false, false, false, true);
			verifyParamType(msgPrefix, alErrors, pi, ParamType.UrlPathTagValue, true, false, false, false);
			verifyParamType(msgPrefix, alErrors, pi, ParamType.UrlQueryNameValue, true, false, true, false);

			if (verifyParamType(msgPrefix, alErrors, pi, ParamType.FormNameValue, true, false, false, false)) {
				if (methodType != MethodType.POST) {
					String s = "param type only be used with methodType=POST";
					alErrors.add(msgPrefix + s);
				}
				if (!pi.bNameAssigned) {
					String s = "requires param name";
					alErrors.add(msgPrefix + s);
				}
			}

			if (verifyParamType(msgPrefix, alErrors, pi, ParamType.OARestInvokeInfo, false, false, false, false)) {
				if (pi.classType != ClassType.OARestInvokeInfo) {
					String s = "param type InvokeInfo is only for param classType InvokeInfo.class";
					alErrors.add(msgPrefix + s);
				}
			} else if (pi.classType == ClassType.OARestInvokeInfo) {
				String s = "param classType InvokeInfo can only be used only for ParamType.InvokeInfo or Unassigned";
				alErrors.add(msgPrefix + s);
			}

			if (verifyParamType(msgPrefix, alErrors, pi, ParamType.MethodReturnClass, false, false, false, false)) {
				if (!Class.class.equals(pi.paramClass)) {
					String s = "type should be of type Class";
					alErrors.add(msgPrefix + s);
				}
			}

			if (verifyParamType(msgPrefix, alErrors, pi, ParamType.SearchWhereTagValue, false, false, false, false)) {
				// cant be an array
				if (pi.classType == ClassType.Array || pi.classType == ClassType.List) {
					boolean b = false;
					if (OAString.isEmpty(searchWhere)) {
						for (OARestParamInfo pix : alParamInfo) {
							if (pix.paramType == ParamType.MethodSearchWhere) {
								b = true;
								break;
							}
						}
						if (!b) {
							String s = "SearchWhereTagValues can not be Array or List";
							alErrors.add(msgPrefix + s);
						}
					}
				}
			}
			verifyParamType(msgPrefix, alErrors, pi, ParamType.SearchWhereAddNameValue, true, false, false, false);

			if (verifyParamType(msgPrefix, alErrors, pi, ParamType.OAObject, false, false, false, true)) {
				if (!OAObject.class.isAssignableFrom(pi.paramClass)) {
					String s = "type should be of class type OAObject";
					alErrors.add(msgPrefix + s);
				}
			}

			verifyParamType(msgPrefix, alErrors, pi, ParamType.OAObjectId, true, false, false, false);
			verifyParamType(msgPrefix, alErrors, pi, ParamType.MethodCallArg, false, false, false, true);

			boolean bCheckName = false;
			if (verifyParamType(msgPrefix, alErrors, pi, ParamType.BodyObject, false, false, false, true)) {
				bCheckName = true;
			}

			if (verifyParamType(msgPrefix, alErrors, pi, ParamType.BodyJson, false, false, false, true, true)) {
				bCheckName = true;
			}

			if (bCheckName) {
				int cnt = 0;
				boolean b = false;
				for (OARestParamInfo pix : alParamInfo) {
					if (pix.paramType == ParamType.BodyObject || pix.paramType == ParamType.BodyJson) {
						cnt++;
						if (!pix.bNameAssigned) {
							b = true;
						}
					}
				}
				if (b && cnt > 1) {
					String s = "more then one BodyObject/BodyJson used, must have name assigned";
					alErrors.add(msgPrefix + s);
				} else if (cnt == 1 && !b) {
					String s = "does not need name " + pi.name;
					alErrors.add(msgPrefix + s);
				}
			}

			verifyParamType(msgPrefix, alErrors, pi, ParamType.Header, false, false, false, false);
			verifyParamType(msgPrefix, alErrors, pi, ParamType.Cookie, false, false, false, false);
			if (verifyParamType(msgPrefix, alErrors, pi, ParamType.PageNumber, false, false, false, false)) {
				if (!OAReflect.isNumber(pi.paramClass)) {
					String s = "type should be of type Number";
					alErrors.add(msgPrefix + s);
				}
				if (returnClassType != ReturnClassType.Array && returnClassType != ReturnClassType.List
						&& returnClassType != ReturnClassType.Hub) {
					String s = "only used when method return type is Array, List, or Hub";
					alErrors.add(msgPrefix + s);
				}

			}
			if (verifyParamType(msgPrefix, alErrors, pi, ParamType.ResponseIncludePropertyPaths, false, false, false, false)) {
				if (!String.class.equals(pi.paramClass)) {
					String s = "type should be of class type String or String[]";
					alErrors.add(msgPrefix + s);
				}
			}

			if (verifyParamType(msgPrefix, alErrors, pi, ParamType.BodyByteArray, false, false, false, false)) {
				if (this.methodType == MethodType.OAGet || this.methodType == MethodType.OADelete || this.methodType == MethodType.OAInsert
						|| this.methodType == MethodType.OAUpdate || this.methodType == MethodType.OASearch) {
					String s = "byte[] param cant be used for this OA methodType";
					alErrors.add(msgPrefix + s);
				}
				if (!pi.origParamClass.isArray() || !pi.paramClass.equals(byte.class)) {
					String s = "type is used for byte[]";
					alErrors.add(msgPrefix + s);
				}
				for (OARestParamInfo pix : alParamInfo) {
					if (pix == pi) {
						continue;
					}
					if (pix.paramType == ParamType.BodyObject || pix.paramType == ParamType.BodyJson) {
						String s = "type cant have other Body* params with BodyByteArray";
						alErrors.add(msgPrefix + s);
					} else if (pix.paramType == ParamType.BodyByteArray) {
						String s = "can only have one param BodyByteArray";
						alErrors.add(msgPrefix + s);
					}
				}
			}
		}
	}

	/**
	 * Checks whether a parameter matches a specific {@link ParamType}
	 * and enforces its associated configuration rules.
	 *
	 * @param msgPrefix           prefix used in any error messages
	 * @param alErrors            list to append validation errors
	 * @param pi                  parameter metadata being checked
	 * @param ptCheck             the parameter type being validated
	 * @param bUsesName           whether a name is required
	 * @param bUsesParamClass     whether a parameter class is required
	 * @param bUsesFormat         whether a format string is supported
	 * @param bUsesIncludePPs     whether include-property-paths are allowed
	 *
	 * @return {@code true} if {@code pi.paramType == ptCheck}, otherwise {@code false}
	 */
	public boolean verifyParamType(String msgPrefix, List<String> alErrors, OARestParamInfo pi, ParamType ptCheck,
			boolean bUsesName,
			boolean bUsesParamClass,
			boolean bUsesFormat,
			boolean bUsesIncludePPs) {
		return verifyParamType(msgPrefix, alErrors, pi, ptCheck, bUsesName, bUsesParamClass, bUsesFormat, bUsesIncludePPs, false);
	}

	/**
	 * Variant of {@link #verifyParamType(String, List, OARestParamInfo, ParamType, boolean, boolean, boolean, boolean)}
	 * that additionally enforces a string-type requirement.
	 *
	 * @param msgPrefix       prefix used in error messages
	 * @param alErrors        list to collect validation errors
	 * @param pi              parameter metadata being inspected
	 * @param ptCheck         expected parameter type
	 * @param bUsesName       whether a name is required
	 * @param bUsesParamClass whether a parameter class is required
	 * @param bUsesFormat     whether a format string is supported
	 * @param bUsesIncludePPs whether include-property-paths are allowed
	 * @param bTypeString     whether the parameter must be a {@link String}
	 *
	 * @return {@code true} if the parameter matches {@code ptCheck}
	 */
	public boolean verifyParamType(String msgPrefix, List<String> alErrors, OARestParamInfo pi, ParamType ptCheck,
			boolean bUsesName,
			boolean bUsesParamClass,
			boolean bUsesFormat,
			boolean bUsesIncludePPs,
			boolean bTypeString) {
		if (pi.paramType != ptCheck) {
			return false;
		}

		if (bTypeString) {
			if (pi.classType == null || !String.class.equals(pi.paramClass)) {
				String s = "type needs to be String";
				alErrors.add(msgPrefix + s);
			}
		}

		if (!bUsesName && pi.bNameAssigned && OAString.isNotEmpty(pi.name)) {
			String s = "does not need name " + pi.name;
			alErrors.add(msgPrefix + s);
		}
		if (!bUsesParamClass && pi.rpParamClass != null) {
			String s = "does not need paramClass " + pi.rpParamClass.getSimpleName();
			alErrors.add(msgPrefix + s);
		}
		if (!bUsesFormat && OAString.isNotEmpty(pi.format)) {
			String s = "does not need param.format";
			alErrors.add(msgPrefix + s);
		}
		if (!bUsesIncludePPs && pi.alIncludePropertyPaths != null && pi.alIncludePropertyPaths.size() > 0) {
			String s = "does not need param.includePropertyPath(s)";
			alErrors.add(msgPrefix + s);
		}
		if (!bUsesIncludePPs && pi.includeReferenceLevelAmount > 0) {
			String s = "does not need param.includePropertyPath(s)";
			alErrors.add(msgPrefix + s);
		}
		return true;
	}

	/**
	 * Ensures that only one instance exists for parameter types that
	 * are required to be unique.
	 *
	 * @param msgPrefix prefix used for error messages
	 * @param alErrors  list to receive validation errors
	 *
	 * <p>
	 * Examines all parameters and flags any duplicates for types
	 * where only a single parameter is permitted (such as
	 * {@code MethodUrlPath}, {@code MethodSearchWhere}, and others).
	 */
	public void verifyParamAmounts(String msgPrefix, List<String> alErrors) {
		HashSet<String> hs = new HashSet();
		for (OARestParamInfo pi : alParamInfo) {
			if (
			// || pi.paramType == ParamType.Ignore
			pi.paramType == ParamType.MethodUrlPath
					|| pi.paramType == ParamType.MethodSearchWhere
					|| pi.paramType == ParamType.MethodSearchOrderBy
					// || pi.paramType == ParamType.UrlPathValue
					// || pi.paramType == ParamType.UrlQueryNameValue
					// || pi.paramType == ParamType.FormNameValue
					|| pi.paramType == ParamType.MethodReturnClass
					// || pi.paramType == ParamType.SearchWhereTagValue
					// || pi.paramType == ParamType.SearchWhereAddNameValue
					|| pi.paramType == ParamType.OAObject
					|| pi.paramType == ParamType.OAObjectId
					|| pi.paramType == ParamType.OAObjectMethodName
					|| pi.paramType == ParamType.OARestInvokeInfo
					// || pi.paramType == ParamType.OAObjectMethodCallArg
					// || pi.paramType == ParamType.BodyObject
					// || pi.paramType == ParamType.BodyJson
					// || pi.paramType == ParamType.Header
					// || pi.paramType == ParamType.Cookie
					|| pi.paramType == ParamType.PageNumber
					|| pi.paramType == ParamType.ResponseIncludePropertyPaths) {

				String s = pi.paramType.toString();
				if (hs.contains(s)) {
					s = String.format("only one paramType=%s is allowed", s);
					alErrors.add(msgPrefix + s);

				}
				hs.add(s);
			}
		}
	}

	/**
	 * Validates that {@link #methodType} is assigned and not
	 * {@code Unassigned}. Adds an error if the method type is
	 * missing or invalid.
	 *
	 * @param msgPrefix prefix for error messages
	 * @param alErrors  list to receive validation errors
	 */
	protected void verifyMethodType(String msgPrefix, List<String> alErrors) {
		if (methodType == null) {
			String s = "methodType can not be null";
			alErrors.add(msgPrefix + s);
		}
		if (methodType == MethodType.Unassigned) {
			String s = "methodType can not be 'Unassigned'";
			alErrors.add(msgPrefix + s);
		}
	}

	/**
	 * Validates that the configured page size is compatible with
	 * the method's return type.
	 *
	 * @param msgPrefix prefix for diagnostic messages
	 * @param alErrors  list to receive validation errors
	 *
	 * <p>
	 * Ensures that page size is used only with array, list, or hub
	 * return types and only when a {@code PageNumber} parameter
	 * exists.
	 */
	protected void verifyMethodPageSize(String msgPrefix, List<String> alErrors) {
		if (restMethod != null && restMethod.pageSize() > 0) {
			if (returnClassType != ReturnClassType.Array && returnClassType != ReturnClassType.List
					&& returnClassType != ReturnClassType.Hub) {
				String s = "pageSize is only used when method return type is Array, List, or Hub";
				alErrors.add(msgPrefix + s);
			}
			boolean b = false;
			for (OARestParamInfo pi : alParamInfo) {
				if (pi.paramType == OARestParam.ParamType.PageNumber) {
					b = true;
					break;
				}
			}
			if (!b) {
				String s = "pageSize is only used when param with type pageNumber is used";
				alErrors.add(msgPrefix + s);
			}
		}

	}

	/**
	 * Performs GET-specific validation rules.
	 *
	 * @param msgPrefix prefix for error messages
	 * @param alErrors  list to receive validation errors
	 *
	 * <p>
	 * Confirms compatibility of search fields, method name usage,
	 * return types, and parameter types when the method type is GET.
	 * Flags any parameter types not allowed for GET requests.
	 */
	protected void verifyMethodTypeGET(String msgPrefix, List<String> alErrors) {
		if (methodType != MethodType.GET) {
			return;
		}

		// done by verifyUrlPath
		// if (OAString.isNotEmpty(urlPath)) {

		// no validation
		// if (OAString.isNotEmpty(urlQuery)) {

		// no validation, done by verifyMethodReturnClass
		// if (!OAObject.class.isAssignableFrom(origReturnClass)) {

		if (OAString.isNotEmpty(searchWhere)) {
			String s = "searchWhere only valid for methodType=GET";
			alErrors.add(msgPrefix + s);
		}

		if (OAString.isNotEmpty(searchOrderBy)) {
			String s = "searchWhere only valid for methodType=GET";
			alErrors.add(msgPrefix + s);
		}

		if (OAString.isNotEmpty(objectMethodName)) {
			String s = "methodName only valid for methodType=OAObjectMethodCall";
			alErrors.add(msgPrefix + s);
		}

		for (OARestParamInfo pi : alParamInfo) {
			if (false
					|| pi.paramType == ParamType.Ignore
					|| pi.paramType == ParamType.OARestInvokeInfo
					|| pi.paramType == ParamType.MethodUrlPath
					// || pi.paramType == ParamType.MethodSearchWhere
					// || pi.paramType == ParamType.MethodSearchOrderBy
					|| pi.paramType == ParamType.UrlPathTagValue
					|| pi.paramType == ParamType.UrlQueryNameValue
					// || pi.paramType == ParamType.FormNameValue
					|| pi.paramType == ParamType.MethodReturnClass
					|| pi.paramType == ParamType.OARestInvokeInfo
					// || pi.paramType == ParamType.SearchWhereValue
					// || pi.paramType == ParamType.SearchWhereNameValue
					// || pi.paramType == ParamType.SearchWhereValue
					// || pi.paramType == ParamType.SearchWhereNameValue
					// || pi.paramType == ParamType.OAObject
					// || pi.paramType == ParamType.OAObjectId
					// || pi.paramType == ParamType.OAObjectMethodName
					// || pi.paramType == ParamType.OAObjectMethodCallArg
					// || pi.paramType == ParamType.BodyObject
					// || pi.paramType == ParamType.BodyJson
					// || pi.paramType == ParamType.BodyByteArray
					|| pi.paramType == ParamType.Header
					|| pi.paramType == ParamType.Cookie
					|| pi.paramType == ParamType.PageNumber
			// || pi.paramType == ParamType.ResponseIncludePropertyPaths
			) {
				// valid
			} else {
				String s = String
						.format("paramType=%s not allowed with %s",
								pi.paramType, methodType);
				alErrors.add(msgPrefix + s);
			}
		}
	}

	/**
	 * Performs POST-specific validation rules.
	 *
	 * @param msgPrefix prefix for error messages
	 * @param alErrors  list to receive validation errors
	 *
	 * <p>
	 * Validates combinations of form parameters, body parameters,
	 * search fields, and method name usage. Ensures prohibited
	 * parameter types are not used with POST.
	 */
	protected void verifyMethodTypePOST(String msgPrefix, List<String> alErrors) {
		if (methodType != MethodType.POST) {
			return;
		}
		// done by verifyUrlPath
		// if (OAString.isNotEmpty(urlPath)) {

		// no validation
		// if (OAString.isNotEmpty(urlQuery)) {

		// no validation, done by verifyMethodReturnClass
		// if (!OAObject.class.isAssignableFrom(origReturnClass)) {

		if (OAString.isNotEmpty(searchWhere)) {
			String s = "searchWhere only valid for methodType=OASearch";
			alErrors.add(msgPrefix + s);
		}

		if (OAString.isNotEmpty(searchOrderBy)) {
			String s = "searchOrderBy only valid for methodType=OASearch";
			alErrors.add(msgPrefix + s);
		}

		if (OAString.isNotEmpty(objectMethodName)) {
			String s = "methodName only valid for methodType=OAObjectMethodCall";
			alErrors.add(msgPrefix + s);
		}

		int cntFormNameValue = 0;
		int cntOther = 0;

		for (OARestParamInfo pi : alParamInfo) {
			if (pi.paramType == ParamType.FormNameValue) {
				cntFormNameValue++;
			}
			if (pi.paramType == ParamType.BodyObject || pi.paramType == ParamType.BodyJson) {
				cntOther++;
			}

			if (false
					|| pi.paramType == ParamType.Ignore
					|| pi.paramType == ParamType.OARestInvokeInfo
					|| pi.paramType == ParamType.MethodUrlPath
					// || pi.paramType == ParamType.MethodSearchWhere
					// || pi.paramType == ParamType.MethodSearchOrderBy
					|| pi.paramType == ParamType.UrlPathTagValue
					|| pi.paramType == ParamType.UrlQueryNameValue
					|| pi.paramType == ParamType.FormNameValue
					|| pi.paramType == ParamType.MethodReturnClass
					// || pi.paramType == ParamType.SearchWhereValue
					// || pi.paramType == ParamType.SearchWhereNameValue
					// || pi.paramType == ParamType.SearchWhereValue
					// || pi.paramType == ParamType.SearchWhereNameValue
					// || pi.paramType == ParamType.OAObject
					// || pi.paramType == ParamType.OAObjectId
					// || pi.paramType == ParamType.OAObjectMethodName
					// || pi.paramType == ParamType.OAObjectMethodCallArg
					|| pi.paramType == ParamType.FormNameValue
					|| pi.paramType == ParamType.BodyObject
					|| pi.paramType == ParamType.BodyJson
					|| pi.paramType == ParamType.BodyByteArray
					|| pi.paramType == ParamType.Header
					|| pi.paramType == ParamType.Cookie
					|| pi.paramType == ParamType.PageNumber
			// || pi.paramType == ParamType.ResponseIncludePropertyPaths
			) {
				// valid
			} else {
				String s = String
						.format("paramType=%s not allowed with %s",
								pi.paramType, methodType);
				alErrors.add(msgPrefix + s);
			}
		}

		if (cntFormNameValue > 0 && cntOther > 0) {
			String s = "cant mix paramType FormNameValue with BodyObject or BodyJson paramTypes";
			alErrors.add(msgPrefix + s);
		}
	}

	/**
	 * Validates rules specific to PUT method types.
	 *
	 * @param msgPrefix prefix for error messages
	 * @param alErrors  list to add validation errors
	 *
	 * <p>
	 * Delegates shared logic to {@link #_verifyMethodTypeX(String, java.util.List)}
	 * and enforces PUT-specific restrictions.
	 */
	protected void verifyMethodTypePUT(String msgPrefix, List<String> alErrors) {
		if (methodType != MethodType.PUT) {
			return;
		}
		_verifyMethodTypeX(msgPrefix, alErrors);
	}

	/**
	 * Validates PATCH-specific method rules.
	 *
	 * @param msgPrefix prefix used in error reporting
	 * @param alErrors  list to receive validation errors
	 *
	 * <p>
	 * Relies on {@link #_verifyMethodTypeX(String, java.util.List)} for
	 * common validation applicable to both PUT and PATCH operations.
	 */
	protected void verifyMethodTypePATCH(String msgPrefix, List<String> alErrors) {
		if (methodType != MethodType.PATCH) {
			return;
		}
		_verifyMethodTypeX(msgPrefix, alErrors);
	}

	/**
	 * Shared validation routine for PUT and PATCH methods.
	 *
	 * @param msgPrefix prefix used when reporting validation errors
	 * @param alErrors  list to receive validation errors
	 *
	 * <p>
	 * Confirms proper usage of search filters, object method names,
	 * body parameters, and other constraints common to PUT/PATCH
	 * semantics.
	 */
	protected void _verifyMethodTypeX(String msgPrefix, List<String> alErrors) {
		// done by verifyUrlPath
		// if (OAString.isNotEmpty(urlPath)) {

		// no validation
		// if (OAString.isNotEmpty(urlQuery)) {

		// no validation, done by verifyMethodReturnClass
		// if (!OAObject.class.isAssignableFrom(origReturnClass)) {

		if (OAString.isNotEmpty(searchWhere)) {
			String s = "searchWhere only valid for methodType=OASearch";
			alErrors.add(msgPrefix + s);
		}

		if (OAString.isNotEmpty(searchOrderBy)) {
			String s = "searchOrderBy only valid for methodType=OASearch";
			alErrors.add(msgPrefix + s);
		}

		if (OAString.isNotEmpty(objectMethodName)) {
			String s = "methodName only valid for methodType=OAObjectMethodCall";
			alErrors.add(msgPrefix + s);
		}

		for (OARestParamInfo pi : alParamInfo) {
			if (false
					|| pi.paramType == ParamType.Ignore
					|| pi.paramType == ParamType.OARestInvokeInfo
					|| pi.paramType == ParamType.MethodUrlPath
					// || pi.paramType == ParamType.MethodSearchWhere
					// || pi.paramType == ParamType.MethodSearchOrderBy
					|| pi.paramType == ParamType.UrlPathTagValue
					|| pi.paramType == ParamType.UrlQueryNameValue
					|| pi.paramType == ParamType.FormNameValue
					|| pi.paramType == ParamType.MethodReturnClass
					// || pi.paramType == ParamType.SearchWhereValue
					// || pi.paramType == ParamType.SearchWhereNameValue
					// || pi.paramType == ParamType.SearchWhereValue
					// || pi.paramType == ParamType.SearchWhereNameValue
					// || pi.paramType == ParamType.OAObject
					// || pi.paramType == ParamType.OAObjectId
					// || pi.paramType == ParamType.OAObjectMethodName
					// || pi.paramType == ParamType.OAObjectMethodCallArg
					// ||pi.paramType == ParamType.FormNameValue
					|| pi.paramType == ParamType.BodyObject
					|| pi.paramType == ParamType.BodyJson
					|| pi.paramType == ParamType.BodyByteArray
					|| pi.paramType == ParamType.Header
					|| pi.paramType == ParamType.Cookie
					|| pi.paramType == ParamType.PageNumber
			// || pi.paramType == ParamType.ResponseIncludePropertyPaths
			) {
				// valid
			} else {
				String s = String
						.format("paramType=%s not allowed with %s",
								pi.paramType, methodType);
				alErrors.add(msgPrefix + s);
			}
		}
	}

	/**
	 * Validates rules specific to the {@code OAGet} method type.
	 *
	 * @param msgPrefix prefix used when generating error messages
	 * @param alErrors  list to receive validation errors
	 *
	 * <p>
	 * Confirms that return types and parameters match the
	 * requirements for object retrieval, such as requiring
	 * {@code OAObjectId} parameters and disallowing unsupported
	 * parameter types.
	 */
	protected void verifyMethodTypeOAGet(String msgPrefix, List<String> alErrors) {
		if (methodType != MethodType.OAGet) {
			return;
		}

		// done by verifyUrlPath
		// if (OAString.isNotEmpty(urlPath)) {

		// no validation
		// if (OAString.isNotEmpty(urlQuery)) {

		// also by verifyMethodReturnClass
		if (!OAObject.class.isAssignableFrom(origReturnClass)) {
			String s = "return value must be an OAObject, which is needed to be able to derive url";
			alErrors.add(msgPrefix + s);
		}

		if (OAString.isNotEmpty(searchWhere)) {
			String s = "searchWhere only valid for methodType=OASearch";
			alErrors.add(msgPrefix + s);
		}

		if (OAString.isNotEmpty(searchOrderBy)) {
			String s = "searchOrderBy only valid for methodType=OASearch";
			alErrors.add(msgPrefix + s);
		}

		// valid:
		// lstIncludePropertyPath(s)
		// includeReferenceLevelAmount
		if (restMethod.pageSize() > 0) {
			String s = "pageSize not needed";
			alErrors.add(msgPrefix + s);
		}
		// pageNumber
		if (restMethod.pageSize() > 0) {
			String s = "pageSize not needed";
			alErrors.add(msgPrefix + s);
		}

		if (OAString.isNotEmpty(objectMethodName)) {
			String s = "methodName only valid for methodType=OAObjectMethodCall";
			alErrors.add(msgPrefix + s);
		}

		if (rmReturnClass != null) {
			String s = "returnClass not needed, uses the method return type to determine class";
			alErrors.add(msgPrefix + s);
		}

		boolean b = false;
		for (OARestParamInfo pi : alParamInfo) {
			b |= (pi.paramType == ParamType.OAObjectId);

			if (false
					|| pi.paramType == ParamType.Ignore
					|| pi.paramType == ParamType.OARestInvokeInfo
					// || pi.paramType == ParamType.MethodUrlPath
					// || pi.paramType == ParamType.MethodSearchWhere
					// || pi.paramType == ParamType.MethodSearchOrderBy
					// || pi.paramType == ParamType.UrlPathValue
					// || pi.paramType == ParamType.UrlQueryNameValue
					// || pi.paramType == ParamType.FormNameValue
					|| pi.paramType == ParamType.MethodReturnClass
					// || pi.paramType == ParamType.SearchWhereValue
					// || pi.paramType == ParamType.SearchWhereNameValue
					// || pi.paramType == ParamType.OAObject
					|| pi.paramType == ParamType.OAObjectId
					// || pi.paramType == ParamType.OAObjectMethodName
					// || pi.paramType == ParamType.OAObjectMethodCallArg
					// || pi.paramType == ParamType.BodyObject
					// || pi.paramType == ParamType.BodyJson
					// || pi.paramType == ParamType.BodyByteArray
					|| pi.paramType == ParamType.Header
					|| pi.paramType == ParamType.Cookie
					// || pi.paramType == ParamType.PageNumber
					|| pi.paramType == ParamType.ResponseIncludePropertyPaths) {
				// valid ...
				if (pi.paramType == ParamType.MethodReturnClass) {
					if (!OAObject.class.equals(origReturnClass)) {
						String s = String
								.format("paramType=%s is only allowed with %s if the return class is OAObject",
										pi.paramType, methodType);
						alErrors.add(msgPrefix + s);
					}
				}

			} else {
				String s = String
						.format("paramType=%s not allowed with %s",
								pi.paramType, methodType);
				alErrors.add(msgPrefix + s);
			}
		}
		if (!b) {
			String s = "requires param with ParamType=OAObjectId";
			alErrors.add(msgPrefix + s);
		}
	}

	/**
	 * Validates rules for the {@code OASearch} method type.
	 *
	 * @param msgPrefix prefix added to error messages
	 * @param alErrors  list collecting validation errors
	 *
	 * <p>
	 * Ensures that search filters, return types, and tag/value
	 * parameter usage follow the conventions required for server-side
	 * search operations.
	 */
	protected void verifyMethodTypeOASearch(String msgPrefix, List<String> alErrors) {
		if (methodType != MethodType.OASearch) {
			return;
		}

		/*
			urlPath - not used, done by verifyUrlPath
			urlQuery - not needed, but allowing
		    searchWhere - allowed
		    searchOrderBy - allowed
		    includePropertyPath(s) - allowed
		    includeReferenceLevelAmount - allowed
		    methodName - not used
			pageSize - allowed
		    returnClass - allowed, should use generic to determine
		*/

		// also by verifyMethodReturnClass
		if (returnClassType != ReturnClassType.Array && returnClassType != ReturnClassType.List && returnClassType != ReturnClassType.Hub) {
			String s = "returnClassType must be for (array, list, hub)";
			alErrors.add(msgPrefix + s);
		}

		if (returnClass == null || returnClass.equals(OAObject.class)) {
			boolean b = false;
			for (OARestParamInfo pi : alParamInfo) {
				if (pi.paramType == ParamType.MethodReturnClass) {
					b = true;
					break;
				}
			}
			if (!b) {
				String s = "returnClassType not known, must be for (array, list, hub) of OAObjects";
				alErrors.add(msgPrefix + s);
			}
		} else if (!OAObject.class.isAssignableFrom(returnClass)) {
			String s = "returnClassType must be for (array, list, hub) of OAObjects";
			alErrors.add(msgPrefix + s);
		}

		boolean bSearchFound = OAString.isNotEmpty(searchWhere);

		// if (OAString.isNotEmpty(searchWhere)) {

		// if (OAString.isNotEmpty(searchOrderBy)) {

		// no validation
		// lstIncludePropertyPaths

		// no validation
		// includeReferenceLevelAmount

		if (OAString.isNotEmpty(objectMethodName)) {
			String s = "methodName only valid for methodType=OAObjectMethodCall";
			alErrors.add(msgPrefix + s);
		}

		boolean b = false;
		int tagCnt = 0;
		for (OARestParamInfo pi : alParamInfo) {
			bSearchFound |= (pi.paramType == ParamType.MethodSearchWhere);
			bSearchFound |= (pi.paramType == ParamType.SearchWhereAddNameValue);

			if (pi.paramType == ParamType.SearchWhereTagValue) {
				tagCnt++;
			}

			if (false
					|| pi.paramType == ParamType.Ignore
					|| pi.paramType == ParamType.OARestInvokeInfo
					// || pi.paramType == ParamType.MethodUrlPath
					|| pi.paramType == ParamType.MethodSearchWhere
					|| pi.paramType == ParamType.MethodSearchOrderBy
					// || pi.paramType == ParamType.UrlPathValue
					|| pi.paramType == ParamType.UrlQueryNameValue
					|| pi.paramType == ParamType.FormNameValue
					|| pi.paramType == ParamType.MethodReturnClass
					|| pi.paramType == ParamType.SearchWhereTagValue
					|| pi.paramType == ParamType.SearchWhereAddNameValue
					// || pi.paramType == ParamType.OAObject
					// || pi.paramType == ParamType.OAObjectId
					// || pi.paramType == ParamType.OAObjectMethodName
					// || pi.paramType == ParamType.OAObjectMethodCallArg
					// || pi.paramType == ParamType.BodyObject
					// || pi.paramType == ParamType.BodyJson
					// || pi.paramType == ParamType.BodyByteArray
					|| pi.paramType == ParamType.Header
					|| pi.paramType == ParamType.Cookie
					|| pi.paramType == ParamType.PageNumber
					|| pi.paramType == ParamType.ResponseIncludePropertyPaths) {
				// valid
			} else {
				String s = String
						.format("paramType=%s not allowed with %s",
								pi.paramType, methodType);
				alErrors.add(msgPrefix + s);
			}
		}
		if (!bSearchFound) {
			String s = "requires SearchWhere, param methodSearchWhere, param searchWhereNameValue";
			alErrors.add(msgPrefix + s);
		}

		int x = searchWhere == null ? 0 : OAString.count(searchWhere, "?");

		b = true;
		if (x == 0) {
			for (OARestParamInfo pi : alParamInfo) {
				if (pi.paramType == ParamType.MethodSearchWhere) {
					b = false;
					break;
				}
			}
		}

		if (x != tagCnt && b) {
			String s = String.format("OASearch expected %d param(s) of type=SearchWhereTagValue, but found %d", x, tagCnt);
			alErrors.add(msgPrefix + s);
		}
	}

	/**
	 * Validates rules for the {@code OASearch} method type.
	 *
	 * @param msgPrefix prefix added to error messages
	 * @param alErrors  list collecting validation errors
	 *
	 * <p>
	 * Ensures that search filters, return types, and tag/value
	 * parameter usage follow the conventions required for server-side
	 * search operations.
	 */
	protected void verifyMethodTypeOAObjectMethodCall(String msgPrefix, List<String> alErrors) {
		if (methodType != MethodType.OAObjectMethodCall) {
			return;
		}

		// done by verifyUrlPath
		// if (OAString.isNotEmpty(urlPath)) {

		// no validation
		// if (OAString.isNotEmpty(urlQuery)) {

		// also by verifyMethodReturnClass
		// if (!OAObject.class.isAssignableFrom(origReturnClass)) {

		if (OAString.isNotEmpty(searchWhere)) {
			String s = "searchWhere only valid for methodType=OASearch";
			alErrors.add(msgPrefix + s);
		}

		if (OAString.isNotEmpty(searchOrderBy)) {
			String s = "searchOrderBy only valid for methodType=OASearch";
			alErrors.add(msgPrefix + s);
		}

		boolean bOAObjectFound = false;
		boolean bMethodNameFound = OAString.isNotEmpty(objectMethodName);

		for (OARestParamInfo pi : alParamInfo) {
			if (pi.paramType == ParamType.OAObject) {
				if (!OAObject.class.isAssignableFrom(pi.origParamClass)) {
					String s = String
							.format("paramType=%s must be for an OAObject",
									pi.paramType);
					alErrors.add(msgPrefix + s);
				}
				bOAObjectFound = true;
			}

			if (pi.paramType == ParamType.OAObjectMethodName) {
				if (bMethodNameFound) {
					String s = "only method.methodName or param OAObjectMethodName can be used, not both";
					alErrors.add(msgPrefix + s);
				}
				if (pi.classType != ClassType.String) {
					String s = "OAObjectMethodName param must be a String";
					alErrors.add(msgPrefix + s);
				}
				bMethodNameFound = true;
			}

			if (false
					|| pi.paramType == ParamType.Ignore
					|| pi.paramType == ParamType.OARestInvokeInfo
					// || pi.paramType == ParamType.MethodUrlPath
					// || pi.paramType == ParamType.MethodSearchWhere
					// || pi.paramType == ParamType.MethodSearchOrderBy
					// || pi.paramType == ParamType.UrlPathValue
					|| pi.paramType == ParamType.UrlQueryNameValue
					// || pi.paramType == ParamType.FormNameValue
					|| pi.paramType == ParamType.MethodReturnClass
					// || pi.paramType == ParamType.SearchWhereValue
					// || pi.paramType == ParamType.SearchWhereNameValue
					|| pi.paramType == ParamType.OAObject
					|| pi.paramType == ParamType.OAObjectMethodName
					// || pi.paramType == ParamType.OAObjectId
					|| pi.paramType == ParamType.MethodCallArg
					|| pi.paramType == ParamType.BodyObject
					|| pi.paramType == ParamType.BodyJson
					|| pi.paramType == ParamType.BodyByteArray
					|| pi.paramType == ParamType.Header
					|| pi.paramType == ParamType.Cookie
					|| pi.paramType == ParamType.PageNumber
					|| pi.paramType == ParamType.ResponseIncludePropertyPaths) {
				// valid
			} else {
				String s = String
						.format("paramType=%s not allowed with %s",
								pi.paramType, methodType);
				alErrors.add(msgPrefix + s);
			}
		}
		if (!bOAObjectFound) {
			String s = "requires param with ParamType=OAObject";
			alErrors.add(msgPrefix + s);
		}
		if (!bMethodNameFound) {
			String s = "method.methodName or param type=OAObjectMethodName is required";
			alErrors.add(msgPrefix + s);
		}
	}

	/**
	 * Validates rules specific to the {@code OARemote} method type.
	 *
	 * @param msgPrefix prefix applied in error messages
	 * @param alErrors  list to collect validation errors
	 *
	 * <p>
	 * Ensures compatible parameter types and disallows fields that
	 * do not apply to remote method-call forwarding.
	 */
	protected void verifyMethodTypeOARemote(String msgPrefix, List<String> alErrors) {
		if (methodType != MethodType.OARemote) {
			return;
		}

		// done by verifyUrlPath
		// if (OAString.isNotEmpty(urlPath)) {

		// no validation
		// if (OAString.isNotEmpty(urlQuery)) {

		// also by verifyMethodReturnClass
		// if (!OAObject.class.isAssignableFrom(origReturnClass)) {

		if (OAString.isNotEmpty(searchWhere)) {
			String s = "searchWhere only valid for methodType=OASearch";
			alErrors.add(msgPrefix + s);
		}

		if (OAString.isNotEmpty(searchOrderBy)) {
			String s = "searchOrderBy only valid for methodType=OASearch";
			alErrors.add(msgPrefix + s);
		}

		if (OAString.isNotEmpty(objectMethodName)) {
			String s = "methodName is only used for OAObjectMethodCall";
			alErrors.add(msgPrefix + s);
		}

		for (OARestParamInfo pi : alParamInfo) {
			if (false
					// || pi.paramType == ParamType.Unassigned
					|| pi.paramType == ParamType.Ignore
					|| pi.paramType == ParamType.OARestInvokeInfo
					// || pi.paramType == ParamType.MethodUrlPath
					// || pi.paramType == ParamType.MethodSearchWhere
					// || pi.paramType == ParamType.MethodSearchOrderBy
					// || pi.paramType == ParamType.UrlPathValue
					|| pi.paramType == ParamType.UrlQueryNameValue
					// || pi.paramType == ParamType.FormNameValue
					|| pi.paramType == ParamType.MethodReturnClass
					// || pi.paramType == ParamType.SearchWhereValue
					// || pi.paramType == ParamType.SearchWhereNameValue
					// || pi.paramType == ParamType.OAObject
					// || pi.paramType == ParamType.OAObjectId
					// || pi.paramType == ParamType.OAObjectMethodName
					|| pi.paramType == ParamType.MethodCallArg
					|| pi.paramType == ParamType.BodyObject
					|| pi.paramType == ParamType.BodyJson
					|| pi.paramType == ParamType.BodyByteArray
					|| pi.paramType == ParamType.Header
					|| pi.paramType == ParamType.Cookie
					|| pi.paramType == ParamType.PageNumber
					|| pi.paramType == ParamType.ResponseIncludePropertyPaths) {
				// valid
			} else {
				String s = String
						.format("paramType=%s not allowed with %s",
								pi.paramType, methodType);
				alErrors.add(msgPrefix + s);
			}
		}
	}

	/**
	 * Validates the {@code OAInsert} method type.
	 *
	 * @param msgPrefix prefix applied to error messages
	 * @param alErrors  list to receive validation errors
	 *
	 * <p>
	 * Confirms that an {@code OAObject} parameter exists and that only
	 * permitted parameter types are used during object insertion
	 * operations.
	 */
	protected void verifyMethodTypeOAInsert(String msgPrefix, List<String> alErrors) {
		if (methodType != MethodType.OAInsert) {
			return;
		}

		// done by verifyUrlPath
		// if (OAString.isNotEmpty(urlPath)) {

		// no validation
		// if (OAString.isNotEmpty(urlQuery)) {

		// also by verifyMethodReturnClass
		// if (!OAObject.class.isAssignableFrom(origReturnClass)) {

		if (OAString.isNotEmpty(searchWhere)) {
			String s = "searchWhere only valid for methodType=OASearch";
			alErrors.add(msgPrefix + s);
		}

		if (OAString.isNotEmpty(searchOrderBy)) {
			String s = "searchOrderBy only valid for methodType=OASearch";
			alErrors.add(msgPrefix + s);
		}

		if (OAString.isNotEmpty(objectMethodName)) {
			String s = "methodName is only used for OAObjectMethodCall";
			alErrors.add(msgPrefix + s);
		}

		boolean b = false;
		for (OARestParamInfo pi : alParamInfo) {
			if (pi.paramType == ParamType.OAObject) {
				if (!OAObject.class.isAssignableFrom(pi.origParamClass)) {
					String s = String
							.format("paramType=%s must be for an OAObject",
									pi.paramType);
					alErrors.add(msgPrefix + s);
				}
				b = true;
			}

			if (false
					|| pi.paramType == ParamType.Ignore
					|| pi.paramType == ParamType.OARestInvokeInfo
					// || pi.paramType == ParamType.MethodUrlPath
					// || pi.paramType == ParamType.MethodSearchWhere
					// || pi.paramType == ParamType.MethodSearchOrderBy
					// || pi.paramType == ParamType.UrlPathValue
					|| pi.paramType == ParamType.UrlQueryNameValue
					// || pi.paramType == ParamType.FormNameValue
					// || pi.paramType == ParamType.MethodReturnClass
					// || pi.paramType == ParamType.SearchWhereValue
					// || pi.paramType == ParamType.SearchWhereNameValue
					|| pi.paramType == ParamType.OAObject
					// || pi.paramType == ParamType.OAObjectId
					// || pi.paramType == ParamType.OAObjectMethodName
					// || pi.paramType == ParamType.OAObjectMethodCallArg
					// || pi.paramType == ParamType.BodyObject
					// || pi.paramType == ParamType.BodyJson
					// || pi.paramType == ParamType.BodyByteArray
					|| pi.paramType == ParamType.Header
					|| pi.paramType == ParamType.Cookie
					// || pi.paramType == ParamType.PageNumber
					|| pi.paramType == ParamType.ResponseIncludePropertyPaths) {
				// valid
			} else {
				String s = String
						.format("paramType=%s not allowed with %s",
								pi.paramType, methodType);
				alErrors.add(msgPrefix + s);
			}
		}
		if (!b) {
			String s = "requires param with ParamType=OAObject";
			alErrors.add(msgPrefix + s);
		}
	}

	/**
	 * Validates the {@code OAUpdate} method type.
	 *
	 * @param msgPrefix prefix to use when creating error messages
	 * @param alErrors  list that collects validation errors
	 *
	 * <p>
	 * Ensures that an {@code OAObject} parameter is supplied, checks for
	 * incompatible parameter types, and enforces update-specific rules
	 * mirroring those used for insert operations.
	 */
	protected void verifyMethodTypeOAUpdate(String msgPrefix, List<String> alErrors) {
		if (methodType != MethodType.OAUpdate) {
			return;
		}

		// done by verifyUrlPath
		// if (OAString.isNotEmpty(urlPath)) {

		// no validation
		// if (OAString.isNotEmpty(urlQuery)) {

		// also by verifyMethodReturnClass
		// if (!OAObject.class.isAssignableFrom(origReturnClass)) {

		if (OAString.isNotEmpty(searchWhere)) {
			String s = "searchWhere only valid for methodType=OASearch";
			alErrors.add(msgPrefix + s);
		}

		if (OAString.isNotEmpty(searchOrderBy)) {
			String s = "searchOrderBy only valid for methodType=OASearch";
			alErrors.add(msgPrefix + s);
		}

		if (OAString.isNotEmpty(objectMethodName)) {
			String s = "methodName is only used for OAObjectMethodCall";
			alErrors.add(msgPrefix + s);
		}

		boolean b = false;
		for (OARestParamInfo pi : alParamInfo) {
			if (pi.paramType == ParamType.OAObject) {
				if (!OAObject.class.isAssignableFrom(pi.origParamClass)) {
					String s = String
							.format("paramType=%s must be for an OAObject",
									pi.paramType);
					alErrors.add(msgPrefix + s);
				}
				b = true;
			}

			if (false
					|| pi.paramType == ParamType.Ignore
					|| pi.paramType == ParamType.OARestInvokeInfo
					// || pi.paramType == ParamType.MethodUrlPath
					// || pi.paramType == ParamType.MethodSearchWhere
					// || pi.paramType == ParamType.MethodSearchOrderBy
					// || pi.paramType == ParamType.UrlPathValue
					|| pi.paramType == ParamType.UrlQueryNameValue
					// || pi.paramType == ParamType.FormNameValue
					// || pi.paramType == ParamType.MethodReturnClass
					// || pi.paramType == ParamType.SearchWhereValue
					// || pi.paramType == ParamType.SearchWhereNameValue
					|| pi.paramType == ParamType.OAObject
					// || pi.paramType == ParamType.OAObjectId
					// || pi.paramType == ParamType.OAObjectMethodName
					// || pi.paramType == ParamType.OAObjectMethodCallArg
					// || pi.paramType == ParamType.BodyObject
					// || pi.paramType == ParamType.BodyJson
					// || pi.paramType == ParamType.BodyByteArray
					|| pi.paramType == ParamType.Header
					|| pi.paramType == ParamType.Cookie
					// || pi.paramType == ParamType.PageNumber
					|| pi.paramType == ParamType.ResponseIncludePropertyPaths) {
				// valid
			} else {
				String s = String
						.format("paramType=%s not allowed with %s",
								pi.paramType, methodType);
				alErrors.add(msgPrefix + s);
			}
		}
		if (!b) {
			String s = "requires param with ParamType=OAObject";
			alErrors.add(msgPrefix + s);
		}
	}

	/**
	 * Validates use of the {@code OADelete} method type.
	 *
	 * @param msgPrefix prefix used for error reporting
	 * @param alErrors  list to receive validation messages
	 *
	 * <p>
	 * Confirms that the method is configured with either an {@code OAObject}
	 * or {@code OAObjectId} source, disallows include-property-paths, and
	 * checks all parameter types for compatibility with delete semantics.
	 */
	protected void verifyMethodTypeOADelete(String msgPrefix, List<String> alErrors) {
		if (methodType != MethodType.OAUpdate) {
			return;
		}

		// done by verifyUrlPath
		// if (OAString.isNotEmpty(urlPath)) {

		// no validation
		// if (OAString.isNotEmpty(urlQuery)) {

		// also by verifyMethodReturnClass
		// if (!OAObject.class.isAssignableFrom(origReturnClass)) {

		if (OAString.isNotEmpty(searchWhere)) {
			String s = "searchWhere only valid for methodType=OASearch";
			alErrors.add(msgPrefix + s);
		}

		if (OAString.isNotEmpty(searchOrderBy)) {
			String s = "searchOrderBy only valid for methodType=OASearch";
			alErrors.add(msgPrefix + s);
		}

		if (alIncludePropertyPaths != null && alIncludePropertyPaths.size() > 0) {
			String s = "IncludePropertyPaths not valid for OADelete";
			alErrors.add(msgPrefix + s);
		}

		// no validation
		// includeReferenceLevelAmount

		if (OAString.isNotEmpty(objectMethodName)) {
			String s = "methodName is only used for OAObjectMethodCall";
			alErrors.add(msgPrefix + s);
		}

		boolean b = false;
		for (OARestParamInfo pi : alParamInfo) {
			if (pi.paramType == ParamType.OAObject) {
				if (!OAObject.class.isAssignableFrom(pi.origParamClass)) {
					String s = String
							.format("paramType=%s must be for an OAObject",
									pi.paramType);
					alErrors.add(msgPrefix + s);
				}
				b = true;
			}
			b |= (pi.paramType == ParamType.OAObjectId);

			if (false
					|| pi.paramType == ParamType.Ignore
					|| pi.paramType == ParamType.OARestInvokeInfo
					// || pi.paramType == ParamType.MethodUrlPath
					// || pi.paramType == ParamType.MethodSearchWhere
					// || pi.paramType == ParamType.MethodSearchOrderBy
					// || pi.paramType == ParamType.UrlPathValue
					|| pi.paramType == ParamType.UrlQueryNameValue
					// || pi.paramType == ParamType.FormNameValue
					// || pi.paramType == ParamType.MethodReturnClass
					// || pi.paramType == ParamType.SearchWhereValue
					// || pi.paramType == ParamType.SearchWhereNameValue
					|| pi.paramType == ParamType.OAObject
					|| pi.paramType == ParamType.OAObjectId
					// || pi.paramType == ParamType.OAObjectMethodName
					// || pi.paramType == ParamType.OAObjectMethodCallArg
					// || pi.paramType == ParamType.BodyObject
					// || pi.paramType == ParamType.BodyJson
					// || pi.paramType == ParamType.BodyByteArray
					|| pi.paramType == ParamType.Header
					|| pi.paramType == ParamType.Cookie
					// || pi.paramType == ParamType.PageNumber
					|| pi.paramType == ParamType.ResponseIncludePropertyPaths) {
				// valid
			} else {
				String s = String
						.format("paramType=%s not allowed with %s",
								pi.paramType, methodType);
				alErrors.add(msgPrefix + s);
			}
		}
		if (!b) {
			String s = "requires param with ParamType=OAObject";
			alErrors.add(msgPrefix + s);
		}
	}

	/**
	 * Validates all parameters assigned to {@code UrlQueryNameValue}.
	 *
	 * @param msgPrefix prefix to apply to error messages
	 * @param alErrors  list to append validation failures
	 *
	 * <p>
	 * Ensures that each query-name/value parameter defines a name and
	 * reports any violations.
	 */
	protected void verifyUrlQuery(String msgPrefix, List<String> alErrors) {
		int cnt = 0;
		for (OARestParamInfo pi : alParamInfo) {
			if (pi.paramType != ParamType.UrlQueryNameValue) {
				continue;
			}
			cnt++;
			if (!pi.bNameAssigned) {
				String s = "param type=UrlQueryNameValue needs to define a name";
				alErrors.add(msgPrefix + s);
			}
		}
	}

	/**
	 * Validates configured include-property-paths.
	 *
	 * @param msgPrefix prefix for diagnostic messages
	 * @param alErrors  list to receive validation errors
	 *
	 * <p>
	 * Ensures that property-path expansion is only used when the
	 * return class is an {@code OAObject}.
	 */
	protected void verifyIncludePropertyPaths(String msgPrefix, List<String> alErrors) {
		if (alIncludePropertyPaths == null || alIncludePropertyPaths.size() == 0) {
			return;
		}

		if (!OAObject.class.isAssignableFrom(returnClass)) {
			String s = "includePropertyPaths not needed, since return class is not OAObject";
			alErrors.add(msgPrefix + s);
		}
	}

	/**
	 * Validates the reference-level expansion depth.
	 *
	 * @param msgPrefix prefix for error messages
	 * @param alErrors  collection receiving validation errors
	 *
	 * <p>
	 * Ensures that reference-level expansion only applies when the
	 * method returns an {@code OAObject}.
	 */
	protected void verifyIncludeReferenceLevelAmount(String msgPrefix, List<String> alErrors) {
		if (includeReferenceLevelAmount == 0) {
			return;
		}

		if (!OAObject.class.isAssignableFrom(returnClass)) {
			String s = "includeReferenceLevelAmount > 0, since return class is not OAObject";
			alErrors.add(msgPrefix + s);
		}
	}

	/**
	 * Validates configuration of the return class.
	 *
	 * @param msgPrefix prefix used for error reporting
	 * @param alErrors  list to collect validation errors
	 *
	 * <p>
	 * Ensures that only one {@code MethodReturnClass} parameter exists,
	 * checks consistency with annotation-supplied return classes, verifies
	 * that the final resolved {@link #returnClass} is valid for the method
	 * type, and enforces rules for {@code InvokeInfo} return types.
	 */
	protected void verifyMethodReturnClass(String msgPrefix, List<String> alErrors) {
		boolean bFoundParam = false;
		for (OARestParamInfo pi : alParamInfo) {
			if (pi.paramType == ParamType.MethodReturnClass) {
				if (bFoundParam) {
					String s = "paramType == ParamType.MethodReturnClass, not more then one is permitted";
					alErrors.add(msgPrefix + s);
				}
				if (!pi.paramClass.equals(Class.class)) {
					String s = "paramType == ParamType.MethodReturnClass, but param class type is not Class";
					alErrors.add(msgPrefix + s);
				} else {
					bFoundParam = true;
				}
			}
		}

		if (returnClass == null && !bFoundParam) {
			String s = "returnClass is not known, need to use one of the following: array, list<generic>, return class, specify using method.returnClass, or param.methodReturnClass";
			alErrors.add(msgPrefix + s);
		}

		if (returnClass != null && bFoundParam) {
			String s = "returnClass is known, dont need to use param.methodReturnClass";
			alErrors.add(msgPrefix + s);
		}
		if (returnClass != null && rmReturnClass != null) {
			String s = "returnClass is known, dont need to use methodType.ReturnClass";
			alErrors.add(msgPrefix + s);
		}

		if (OARestInvokeInfo.class.equals(returnClass) && returnClassType != ReturnClassType.InvokeInfo) {
			String s = "returnClass is InvokeInfo.class, ReturnClassType should be InvokeInfo";
			alErrors.add(msgPrefix + s);
		}
	}

	/**
	 * Validates the configured URL path and resolves template markers.
	 *
	 * @param msgPrefix prefix used for error messages
	 * @param alErrors  list to append validation messages
	 *
	 * <p>
	 * Confirms whether URL paths are allowed or required for the method
	 * type, checks for mutually exclusive configurations, transforms
	 * placeholder syntax, and ensures that parameter counts and template
	 * tags are consistent.
	 */
	protected void verifyUrlPath(String msgPrefix, List<String> alErrors) {
		if (!restMethod.methodType().requiresUrlPath()) {
			if (OAString.isNotEmpty(urlPath)) {
				String s = "creates it's own UrlPath and should not have a urlPath defined";
				alErrors.add(msgPrefix + s);
			}
			for (OARestParamInfo pi : alParamInfo) {
				if (pi.paramType == ParamType.MethodUrlPath) {
					String s = "creates it's own UrlPath, should not have paramType=MethodUrlPath";
					alErrors.add(msgPrefix + s);
				}
			}
			for (OARestParamInfo pi : alParamInfo) {
				if (pi.paramType == ParamType.UrlPathTagValue) {
					String s = "creates it's own UrlPath, should not have paramType=UrlPathValue";
					alErrors.add(msgPrefix + s);
				}
			}
			return;
		}

		// URL path is required

		if (OAString.isEmpty(urlPath)) {
			boolean b = false;
			for (OARestParamInfo pi : alParamInfo) {
				if (pi.paramType == ParamType.MethodUrlPath) {
					b = true;
					break;
				}
			}
			if (!b) {
				String s = "urlPath is required, either: Method.urlPath, or param MethodUrlPath";
				alErrors.add(msgPrefix + s);
			}
		} else {
			for (OARestParamInfo pi : alParamInfo) {
				if (pi.paramType == ParamType.MethodUrlPath) {
					String s = "has urlPath, also has a param of type=methodUrlParam, cant have both defined";
					alErrors.add(msgPrefix + s);
				}
			}
		}

		// make sure that matching ? {} param vars
		derivedUrlPath = urlPath;
		if (derivedUrlPath != null) {
			if (derivedUrlPath.indexOf("{") < 0 && derivedUrlPath.indexOf("}") < 0) {
				// convert each ? to {name}
				for (OARestParamInfo pi : alParamInfo) {
					if (pi.paramType != ParamType.UrlPathTagValue) {
						continue;
					}
					int pos = derivedUrlPath.indexOf("?");
					if (pos < 0) {
						continue;
					}
					if (pos == 0) {
						derivedUrlPath = "{" + pi.name + "}" + derivedUrlPath.substring(1);
					} else {
						derivedUrlPath = derivedUrlPath.substring(0, pos) + "{" + pi.name + "}" + derivedUrlPath.substring(pos + 1);
					}
				}
			}

			derivedUrlPath = OAString.convert(derivedUrlPath, "{", "<%=$");
			derivedUrlPath = OAString.convert(derivedUrlPath, "}", "%>");

			int x = OAString.count(derivedUrlPath, "<%=$");
			x += OAString.count(derivedUrlPath, "?");
			int cnt = 0;
			for (OARestParamInfo pi : alParamInfo) {
				if (pi.paramType == ParamType.UrlPathTagValue) {
					cnt++;
					if (derivedUrlPath.indexOf("$" + pi.name) < 0) {
						String s = String
								.format("urlPath %s, template=%s, param path value '%s' not found in template tag(s)",
										urlPath, derivedUrlPath, pi.name);
						alErrors.add(msgPrefix + s);
					}
				}
			}
			if (x != cnt) {
				String s = String
						.format("urlPath %s, has %d tag value(s), does not match %d param(s) with paramType=urlPathValue",
								urlPath, x, cnt);
				alErrors.add(msgPrefix + s);
			}
		}
	}

	/**
	 * Determines and validates the final derived URL path template.
	 *
	 * @param msgPrefix prefix applied to all validation messages
	 * @param alErrors  list to receive validation errors
	 *
	 * <p>
	 * Creates method-type-specific paths (for example OAGet,
	 * OAObjectMethodCall, OASearch, OARemote), verifies that required
	 * parameters exist, and ensures that template variables can be
	 * resolved during invocation.
	 */
	protected void verifyDerviedUrlPath(String msgPrefix, List<String> alErrors) {
		// make sure that it can derive urlPath
		if (methodType == MethodType.OAObjectMethodCall || methodType == MethodType.OAInsert || methodType == MethodType.OAUpdate
				|| methodType == MethodType.OADelete) {
			// requires paramType=OAObject
			boolean b = false;
			for (OARestParamInfo pi : alParamInfo) {
				if (pi.paramType != ParamType.OAObject) {
					continue;
				}
				if (!OAObject.class.isAssignableFrom(pi.origParamClass)) {
					String s = "cant derive urlPath, ParamType.OAObject must be of type OAObject.class";
					alErrors.add(msgPrefix + s);
				} else {
					derivedUrlPath = "/<%=$Class%>/<%=$ID%>";
					b = true;
					break;
				}
			}
			if (!b) {
				String s = "urlPath can not be derived, needs to have a paramtType=OAObject for class type=OAObject.class";
				alErrors.add(msgPrefix + s);
			}
		} else if (methodType == MethodType.OAGet) {
			// requires return oaobject
			boolean b = false;
			for (OARestParamInfo pi : alParamInfo) {
				if (pi.paramType == ParamType.MethodReturnClass) {
					b = true;
					break;
				}
			}

			if (b) {
				derivedUrlPath = "/<%=$Class%>/<%=$ID%>";
				int cnt2 = 0;
				for (OARestParamInfo pi : alParamInfo) {
					if (pi.paramType == ParamType.OAObjectId) {
						if (pi.classType != ClassType.Array) {
							String s = "OAObjectId has to be an array type, since using MethodReturnClass, and could have more than one ID property";
							// allow this to not be an array
							// alErrors.add(msgPrefix + s);
						}
					}
				}
			} else if (this.origReturnClass == null || !OAObject.class.isAssignableFrom(this.origReturnClass)) {
				String s = "cant derive urlPath, return class must be of type OAObject.class";
				alErrors.add(msgPrefix + s);
			} else {
				derivedUrlPath = "/" + OAString.mfcl(this.origReturnClass.getSimpleName());
				derivedUrlPath += "/<%=$ID%>";
				final OAObjectInfoService srvcObjectInfo = OARuntime.get().graph(this.origReturnClass).objects().getOAObjectInfoService();
				OAObjectInfo oi = srvcObjectInfo.getOAObjectInfo(this.origReturnClass);
				int cnt = 0;
				for (String s : oi.getKeyProperties()) {
					cnt++;
				}
				// make sure that there are param=OAObjectId
				int cnt2 = 0;
				for (OARestParamInfo pi : alParamInfo) {
					if (pi.paramType == ParamType.OAObjectId) {
						cnt2++;
					}
				}
				if (cnt != cnt2) {
					String s = String.format("cant derive urlPath, needs to have %d paramType=OAObjectId", cnt);
					alErrors.add(msgPrefix + s);
				}
			}
		} else if (methodType == MethodType.OASearch) {
			// requires return oaobject collection
			boolean b = (returnClassType == ReturnClassType.Array || returnClassType == ReturnClassType.List
					|| returnClassType == ReturnClassType.Hub);

			if (!b) {
				String s = "cant derive urlPath, return class must be an array, List or Hub of type OAObject.class";
				alErrors.add(msgPrefix + s);
			} else if (this.returnClass != null && !OAObject.class.isAssignableFrom(this.returnClass)) {
				String s = "cant derive urlPath, return class must be OAObject collection using array, List or Hub";
				alErrors.add(msgPrefix + s);
			} else if (this.returnClass == null || OAObject.class.equals(this.returnClass)) {
				b = false;
				for (OARestParamInfo pi : alParamInfo) {
					if (pi.paramType == ParamType.MethodReturnClass) {
						b = true;
						break;
					}
				}
				if (b) {
					derivedUrlPath = "/<%=$PluralClass%>";
				} else {
					String s = "cant derive urlPath, return class must be OAObject collection using array,List,Hub or use param type=MethodReturnClass";
					alErrors.add(msgPrefix + s);
				}
			} else {
				final OAObjectInfoService srvcObjectInfo = OARuntime.get().graph(this.returnClass).objects().getOAObjectInfoService();
				OAObjectInfo oi = srvcObjectInfo.getOAObjectInfo(this.returnClass);
				derivedUrlPath = "/" + OAString.mfcl(oi.getPluralName());
			}
		} else if (methodType == MethodType.OARemote) {
			derivedUrlPath = "/oaremote";
		}
	}

	/*
	 * Called when a method is invoked, so that all of the HTTP params can be setup.
	 * <p>
	 * Note: if a method param/argument is type OARestInvokeInfo, then it will be used instead of creating a new one. If so, then it will be
	 * updated to match the current method call and argument values.
	 * <p>
	 *
	 * @param args from method invocation
	 * @return new RestInvokeInfo with all of the HTTP information needed to make the call to the endpoint.
	 */
	/**
	 * Builds and returns an {@link OARestInvokeInfo} instance describing
	 * all HTTP request details for a method invocation.
	 *
	 * @param args        the argument values passed to the method
	 * @param idSeparater string used to join multi-part object IDs
	 *
	 * @return a fully populated {@code OARestInvokeInfo} containing
	 *         HTTP method, URL path, query string, body content,
	 *         headers, cookies, and resolved return-class information
	 *
	 * @throws Exception if argument validation or request assembly fails
	 *
	 * <p>
	 * Detects existing {@code OARestInvokeInfo} parameters, assembles
	 * URL components, applies search filters, encodes include-property
	 * paths, resolves body/form/byte-array content, and attaches any
	 * header or cookie parameters.
	 */
	public OARestInvokeInfo getInvokeInfo(final Object[] args, final String idSeparater) throws Exception {
		OARestInvokeInfo invokeInfo = null;
		// see if one of the method params is of type OARestInvokeInfo
		int pos = -1;
		for (OARestParamInfo pi : alParamInfo) {
			pos++;
			if (pi.paramType == ParamType.OARestInvokeInfo && args[pos] instanceof OARestInvokeInfo) {
				invokeInfo = (OARestInvokeInfo) args[pos];
				break;
			}
		}
		if (invokeInfo == null) {
			invokeInfo = new OARestInvokeInfo();
		}

		invokeInfo.methodInfo = this;

		String mt;
		switch (methodType) {
		case OAGet:
		case OASearch:
			mt = "GET";
			break;
		case OARemote:
		case OAObjectMethodCall:
		case OAInsert:
		case OAUpdate:
			mt = "POST";
			break;
		case OADelete:
			mt = "DELETE";
			break;
		default:
			mt = methodType.toString();
		}
		invokeInfo.httpMethod = mt;

		invokeInfo.args = args;

		invokeInfo.urlPath = OAString.concat(classInfo.contextName, getUrlPath(args, idSeparater), "/");

		invokeInfo.urlQuery = getUrlQuery(args);

		String searchQuery = getSearchWhere(args);
		invokeInfo.urlQuery = OAString.concat(invokeInfo.urlQuery, searchQuery, "&");

		if (alIncludePropertyPaths != null) {
			for (String s : alIncludePropertyPaths) {
				invokeInfo.urlQuery = OAString.concat(invokeInfo.urlQuery, "pp=" + URLEncoder.encode(s, "UTF-8"), "&");
			}
		}

		invokeInfo.byteArrayBody = getByteArrayBody(args);
		if (invokeInfo.byteArrayBody == null) {
			invokeInfo.jsonBody = getJsonBody(args);
			invokeInfo.formData = getFormData(args);
		}

		invokeInfo.methodReturnClass = getMethodReturnClass(args);

		HashMap<String, String> hsHeader = null;
		HashMap<String, String> hsCookie = null;

		pos = -1;
		for (OARestParamInfo pi : alParamInfo) {
			pos++;
			if (pi.paramType == ParamType.Header) {
				if (hsHeader == null) {
					hsHeader = new HashMap();
				}
				hsHeader.put(pi.name.toUpperCase(), OAConv.toString(args[pos], pi.format));
			} else if (pi.paramType == ParamType.Cookie) {
				if (hsCookie == null) {
					hsCookie = new HashMap();
				}
				hsCookie.put(pi.name.toUpperCase(), OAConv.toString(args[pos], pi.format));
			}
		}
		return invokeInfo;
	}

	/**
	 * Returns the cached template for the derived URL path, creating it
	 * on first access.
	 *
	 * @return an {@link OATemplate} instance representing the
	 *         {@link #derivedUrlPath}
	 *
	 * <p>
	 * Templates are reused across invocations to substitute values such
	 * as object class names and IDs efficiently.
	 */
	public OATemplate getUrlPathTemplate() {
		if (urlPathTemplate != null) {
			return urlPathTemplate;
		}
		urlPathTemplate = new OATemplate(derivedUrlPath);
		return urlPathTemplate;
	}

	/**
	 * Produces the final URL path for a specific invocation.
	 *
	 * @param args        argument values from the method call
	 * @param idSeparater separator used when combining composite IDs
	 *
	 * @return the resolved URL path for the request
	 *
	 * <p>
	 * Applies method-type-specific resolution rules, substituting
	 * template variables (such as class name, ID, or plural name)
	 * and injecting URL-path arguments supplied by parameters of
	 * type {@code UrlPathTagValue}.
	 */
	public String getUrlPath(final Object[] args, final String idSeparater) {
		getUrlPathTemplate();

		String result = null;

		if (methodType == MethodType.OAObjectMethodCall || methodType == MethodType.OAInsert || methodType == MethodType.OAUpdate
				|| methodType == MethodType.OADelete) {
			// requires paramType=OAObject
			int pos = -1;
			for (OARestParamInfo pi : alParamInfo) {
				pos++;
				if (pi.paramType == ParamType.OAObject) {
					OAObject oaobj = (OAObject) args[pos];
					if (oaobj == null) {
						throw new OARestClientException("arg/param type=OAObject can not be null for methodType=" + methodType);
					} else {
						urlPathTemplate.setProperty("Class", OAString.mfcl(oaobj.getClass().getSimpleName()));

						OAObjectKey oakey = oaobj.getObjectKey();
						Object[] ids = oakey.getObjectIds();
						String id = "";
						if (ids != null) {
							for (Object idx : ids) {
								if (id.length() > 0) {
									id += idSeparater;
								}
								id += idx;
							}
						}
						urlPathTemplate.setProperty("ID", id);
						break;
					}
				}
			}
		} else if (methodType == MethodType.OAGet) {
			int cnt = 0;
			int pos = -1;
			String id = "";

			for (OARestParamInfo pi : alParamInfo) {
				pos++;
				if (pi.paramType == ParamType.OAObjectId) {
					if (pi.classType == OARestParamInfo.ClassType.Array) {
						int x = Array.getLength(args[pos]);
						for (int i = 0; i < x; i++) {
							Object obj = Array.get(args[pos], i);
							if (id.length() > 0) {
								id += idSeparater;
							}
							id += obj;
						}
					} else {
						if (id.length() > 0) {
							id += idSeparater;
						}
						id += args[pos];
					}
				}
				if (pi.paramType == ParamType.MethodReturnClass) {
					if (args[pos] instanceof Class) {
						urlPathTemplate.setProperty("Class", OAString.mfcl(((Class) args[pos]).getSimpleName()));
					} else {
						throw new OARestClientException(
								"arg/param type=MethodReturnClass must be of type Class for methodType=" + methodType);
					}
				}
			}
			urlPathTemplate.setProperty("ID", id);
		} else if (methodType == MethodType.OASearch) {
			result = "";
			int pos = -1;
			for (OARestParamInfo pi : alParamInfo) {
				pos++;
				if (pi.paramType == ParamType.MethodReturnClass) {
					if (args[pos] instanceof Class) {
						final OAObjectInfoService srvcObjectInfo = OARuntime.get().graph((Class) args[pos]).objects().getOAObjectInfoService();
						OAObjectInfo oi = srvcObjectInfo.getOAObjectInfo((Class) args[pos]);
						urlPathTemplate.setProperty("PluralClass", OAString.mfcl(oi.getPluralName()));
					} else {
						throw new OARestClientException(
								"arg/param type=MethodReturnClass must be of type Class for methodType=" + methodType);
					}
				}
			}
		} else if (OAString.isNotEmpty(urlPath)) {
			int pos = -1;
			for (OARestParamInfo pi : alParamInfo) {
				pos++;
				if (pi.paramType == ParamType.UrlPathTagValue) {
					urlPathTemplate.setProperty(pi.name, OAConv.toString(args[pos]));
				}
			}
		} else if (methodType == MethodType.OARemote) {
			return derivedUrlPath;
		} else {
			int i = 0;
			for (OARestParamInfo pi : alParamInfo) {
				if (pi.paramType == ParamType.MethodUrlPath) {
					result = OAConv.toString(args[i++]);
				}
			}
		}
		if (urlPathTemplate != null) {
			result = urlPathTemplate.process();
		}
		return result;
	}

	/**
	 * Constructs the URL query string for a request.
	 *
	 * @param args the method arguments from the invocation
	 *
	 * @return the composed query string, or {@code null} if none
	 *
	 * <p>
	 * Processes parameters of type {@code UrlQueryNameValue} and
	 * appends name/value pairs using each parameter's formatting
	 * rules.
	 */
	public String getUrlQuery(Object[] args) throws Exception {
		String urlQuery = this.urlQuery;
		if (urlQuery == null) {
			urlQuery = "";
		}

		if (methodType == MethodType.OARemote) {
			if (urlQuery.length() > 0) {
				urlQuery += "&";
			}
			urlQuery += String.format(	"remoteClassName=%s&remoteMethodName=%s", classInfo.interfaceClass.getSimpleName(),
										method.getName());
		} else if (methodType == MethodType.OAObjectMethodCall) {
			String s = objectMethodName;
			if (OAString.isEmpty(s)) {
				for (int argPos = 0; argPos < alParamInfo.size(); argPos++) {
					OARestParamInfo pi = alParamInfo.get(argPos);
					if (pi.paramType == ParamType.OAObjectMethodName && args[argPos] instanceof String) {
						s = (String) args[argPos];
						break;
					}
				}
			}

			if (urlQuery.length() > 0) {
				urlQuery += "&";
			}
			urlQuery += String.format("objectMethodName=%s", s);
		}

		for (int argPos = 0; argPos < alParamInfo.size(); argPos++) {
			OARestParamInfo pi = alParamInfo.get(argPos);
			final Object objArg = args[argPos];

			if (pi.paramType == OARestParam.ParamType.PageNumber) {
				int val = OAConv.toInt(objArg);
				if (urlQuery.length() > 0) {
					urlQuery += "&";
				}
				urlQuery += "pageNumber=" + val;

				if (restMethod.pageSize() > 0) {
					if (urlQuery.length() > 0) {
						urlQuery += "&";
					}
					urlQuery += "pageSize=" + restMethod.pageSize();
				}
			} else if (pi.paramType == OARestParam.ParamType.UrlQueryNameValue) {
				String s = OAHttpUtil.getUrlEncodedNameValues(pi.name, objArg, pi.format);
				if (OAString.isNotEmpty(s)) {
					if (urlQuery.length() > 0) {
						urlQuery += "&";
					}
					urlQuery += s;
				}
			} else if (pi.paramType == OARestParam.ParamType.ResponseIncludePropertyPaths) {
				String s = OAHttpUtil.getUrlEncodedNameValues("pp", objArg, null);
				if (OAString.isNotEmpty(s)) {
					if (urlQuery.length() > 0) {
						urlQuery += "&";
					}
					urlQuery += s;
				}
			}
		}
		return urlQuery;
	}

	/**
	 * Creates the search-where expression for this invocation.
	 *
	 * @param args the method arguments passed at invocation time
	 *
	 * @return the formatted search-where expression, or {@code null} if none
	 *
	 * <p>
	 * Evaluates parameters of type {@code MethodSearchWhere},
	 * {@code SearchWhereTagValue}, and {@code SearchWhereAddNameValue},
	 * substituting tag values and concatenating additional name/value
	 * expressions. Returns the complete search filter to append to the
	 * URL query string.
	 */
	public String getSearchWhere(Object[] args) throws Exception {
		String search = searchWhere;
		String orderBy = searchOrderBy;
		String searchArgs = "";

		for (int argPos = 0; argPos < alParamInfo.size(); argPos++) {
			OARestParamInfo pi = alParamInfo.get(argPos);
			final Object objArg = args[argPos];

			if (pi.paramType == OARestParam.ParamType.MethodSearchWhere) {
				if (search == null) {
					search = "";
				} else if (OAString.isNotEmpty(search)) {
					search += " AND ";
				}
				String val = OAConv.toString(objArg);
				search += val;
			} else if (pi.paramType == OARestParam.ParamType.MethodSearchOrderBy) {
				String val = OAConv.toString(objArg);
				orderBy = val;
			} else if (pi.paramType == OARestParam.ParamType.SearchWhereAddNameValue) {
				if (objArg == null) {
					continue;
				}
				if (pi.classType == OARestParamInfo.ClassType.Array) {
					int x = Array.getLength(objArg);
					for (int i = 0; i < x; i++) {
						Object obj = Array.get(objArg, i);

						if (i == 0) {
							if (search == null) {
								search = "";
							} else {
								search += " AND ";
							}
						}

						if (search.length() > 0) {
							if (i > 0) {
								search += " OR ";
							}
						}

						if (i == 0) {
							search += "(";
						}

						String val;
						if (obj instanceof OAObject) {
							val = OAJson.convertObjectKeyToJsonSinglePartId(((OAObject) obj).getObjectKey());
						} else {
							val = OAConv.toString(obj, pi.format);
							if (val == null) {
								val = "NULL";
							}
						}
						search += pi.name + "=" + val;
					}
					if (x > 0) {
						search += ")";
					}
				} else if (pi.classType == OARestParamInfo.ClassType.List) {
					final List list = (List) objArg;
					if (list.size() > 0) {
						if (urlQuery.length() > 0) {
							urlQuery += " AND ";
						}
					}
					int i = 0;
					for (Object arg : list) {
						if (i == 0) {
							if (search == null) {
								search = "";
							} else {
								search += " AND ";
							}
						}

						if (i > 0) {
							search += " OR ";
						}
						if (i++ == 0) {
							search += "(";
						}

						String val;
						if (arg instanceof OAObject) {
							val = OAJson.convertObjectKeyToJsonSinglePartId(((OAObject) arg).getObjectKey());
						} else {
							val = OAConv.toString(arg, pi.format);
							if (val == null) {
								val = "NULL";
							}
						}
						search += pi.name + "=" + val;
					}
					if (list.size() > 0) {
						search += ")";
					}
				} else {
					if (search == null) {
						search = "";
					} else {
						search += " AND ";
					}

					String val;
					if (objArg instanceof OAObject) {
						val = OAJson.convertObjectKeyToJsonSinglePartId(((OAObject) objArg).getObjectKey());
					} else {
						val = OAConv.toString(objArg, pi.format);
						if (val == null) {
							val = "NULL";
						}
					}
					search += pi.name + "=" + val;
				}

			} else if (pi.paramType == OARestParam.ParamType.SearchWhereTagValue) {
				if (pi.classType == OARestParamInfo.ClassType.Array) {
					int x = Array.getLength(objArg);
					for (int i = 0; i < x; i++) {
						Object obj = Array.get(objArg, i);

						if (searchArgs.length() > 0) {
							searchArgs += "&";
						}
						searchArgs += "queryParam=";

						String val;
						if (obj instanceof OAObject) {
							val = OAJson.convertObjectKeyToJsonSinglePartId(((OAObject) obj).getObjectKey());
						} else {
							val = OAConv.toString(obj, pi.format);
							if (val == null) {
								val = "NULL";
							}
						}
						searchArgs += URLEncoder.encode(val, "UTF-8");
					}
				} else {
					if (searchArgs.length() > 0) {
						searchArgs += "&";
					}
					searchArgs += "queryParam=";

					String val;
					if (objArg instanceof OAObject) {
						val = OAJson.convertObjectKeyToJsonSinglePartId(((OAObject) objArg).getObjectKey());
					} else {
						val = OAConv.toString(objArg, pi.format);
						if (val == null) {
							val = "NULL";
						}
					}
					searchArgs += URLEncoder.encode(val, "UTF-8");
				}
			}
		}

		if (OAString.isNotEmpty(search)) {
			search = "query=" + URLEncoder.encode(search, "UTF-8");
			if (OAString.isNotEmpty(searchArgs)) {
				search += "&" + searchArgs;
			}
		}

		if (OAString.isNotEmpty(orderBy)) {
			if (OAString.isNotEmpty(search)) {
				search += "&";
			}
			search += "orderBy=" + URLEncoder.encode(orderBy, "UTF-8");
		}

		return search;
	}

	/**
	 * Builds the JSON request body for this invocation.
	 *
	 * @param args the argument values passed to the method
	 *
	 * @return a JSON string, or {@code null} if no JSON body is required
	 *
	 * <p>
	 * Processes parameters of type {@code BodyObject} and
	 * {@code BodyJson}, serializes objects using {@link OAJson},
	 * and includes optional include-property-path information.
	 */
	public String getJsonBody(Object[] args) throws Exception {

		if (methodType == MethodType.GET) {
			// fall thru
		} else if (methodType == MethodType.OAGet) {
			// fall thru
		} else if (methodType == MethodType.OASearch) {
			return null;
		} else if (methodType == MethodType.POST) {
			// fall thru
		} else if (methodType == MethodType.PUT) {
			// fall thru
		} else if (methodType == MethodType.PATCH) {
			// fall thru
		} else if (methodType == MethodType.OAObjectMethodCall) {
			int cnt = 0;
			for (OARestParamInfo pix : alParamInfo) {
				if (pix.paramType == ParamType.MethodCallArg) {
					cnt++;
				}
			}

			int[] is = new int[alParamInfo.size() - cnt];
			List<String>[] lstIncludePropertyPaths = new ArrayList[cnt];
			Object[] args2 = new Object[cnt];

			int i = -1;
			int i2 = 0;
			int i3 = 0;
			boolean bDynamicMethodName = false;
			for (OARestParamInfo pix : alParamInfo) {
				i++;
				if (pix.paramType == ParamType.MethodCallArg) {
					args2[i2] = args[i];
					lstIncludePropertyPaths[i2] = pix.alIncludePropertyPaths;
					i2++;
				} else {
					if (pix.paramType == ParamType.OAObjectMethodName) {
						bDynamicMethodName = true;
					}
					is[i3] = i;
					i3++;
				}
			}

			if (i2 == 1 && bDynamicMethodName) {
				if (args2[0] != null && args2[0].getClass().isArray()) {
					args = (Object[]) args2[0];
					is = null;
					lstIncludePropertyPaths = null;
				}
			}

			String json = OAJson.convertMethodArgumentsToJson(method, args, lstIncludePropertyPaths, is);
			return json;
		} else if (methodType == MethodType.OARemote) {
			int cnt = 0;
			for (OARestParamInfo pix : alParamInfo) {
				if (pix.paramType == ParamType.MethodCallArg) {
					cnt++;
				}
			}

			int[] is = new int[alParamInfo.size() - cnt];
			List<String>[] lstIncludePropertyPathss = new ArrayList[cnt];
			Object[] args2 = new Object[cnt];

			int i = -1;
			int i2 = 0;
			int i3 = 0;
			for (OARestParamInfo pix : alParamInfo) {
				i++;
				if (pix.paramType == ParamType.MethodCallArg) {
					args2[i2] = args[i];
					lstIncludePropertyPathss[i2] = pix.alIncludePropertyPaths;
					i2++;
				} else {
					is[i3] = i;
					i3++;
				}
			}
			String json = OAJson.convertMethodArgumentsToJson(method, args2, lstIncludePropertyPathss, is);
			return json;
		} else if (methodType == MethodType.OAInsert || methodType == MethodType.OAUpdate || methodType == MethodType.OADelete) {
			if (args == null || args.length == 0) {
				return null;
			}

			int pos = -1;
			for (OARestParamInfo pix : alParamInfo) {
				pos++;
				if (pix.paramType == ParamType.OAObject) {
					break;
				}
			}

			OARestParamInfo pi = alParamInfo.get(pos);

			OAJson oaj = new OAJson();
			oaj.addPropertyPaths(pi.alIncludePropertyPaths);

			String json = oaj.write(args[pos]);

			return json;
		}

		final OAJson oaj = new OAJson();
		final ObjectMapper om = oaj.getObjectMapper();

		// fall thru and find all OAObject, BodyObject, BodyJson
		ObjectNode jsonNodeBody = om.createObjectNode();

		for (int argPos = 0; argPos < alParamInfo.size(); argPos++) {
			OARestParamInfo pi = alParamInfo.get(argPos);
			final Object objArg = args[argPos];

			if (pi.paramType == OARestParam.ParamType.BodyJson) {
				if (objArg instanceof String) {
					JsonNode node = om.readTree((String) objArg);
					jsonNodeBody.set(pi.name, node);
				} else if (objArg instanceof JsonNode) {
					jsonNodeBody.set(pi.name, (JsonNode) objArg);
				}
			} else if (pi.paramType == OARestParam.ParamType.BodyObject) {

				OAJson oajx = new OAJson();
				ObjectMapper omx = oajx.getObjectMapper();
				oajx.addPropertyPaths(pi.alIncludePropertyPaths);

				String jsonx = oajx.write(objArg);
				JsonNode nodex = omx.readTree(jsonx);

				jsonNodeBody.set(pi.name, nodex);
			}
		}

		String jsonBody = null;
		int x = jsonNodeBody.size();
		if (x == 1) {
			jsonBody = jsonNodeBody.get(0).asText();
		} else if (x > 1) {
			// simulate an object based on the params that are paramType.BodyObject
			jsonBody = jsonNodeBody.asText();
		}

		return jsonBody;
	}

	/**
	 * Resolves the final return class for this method invocation.
	 *
	 * @param args the invocation arguments
	 *
	 * @return the resolved return class
	 *
	 * <p>
	 * Considers annotation-supplied return classes, method-level
	 * overrides, and parameters of type {@code MethodReturnClass}.
	 */
	public Class getMethodReturnClass(Object[] args) {
		Class result = returnClass;
		if (returnClass == null) {
			for (int argPos = 0; argPos < alParamInfo.size(); argPos++) {
				OARestParamInfo pi = alParamInfo.get(argPos);
				if (pi.paramType == ParamType.MethodReturnClass) {
					final Object objArg = args[argPos];
					if (objArg instanceof Class) {
						result = (Class) objArg;
						break;
					}
				}
			}
		}
		if (OAObject.class.equals(result)) {
			if (methodType == MethodType.OAInsert || methodType == MethodType.OAUpdate) {
				for (int argPos = 0; argPos < alParamInfo.size(); argPos++) {
					OARestParamInfo pi = alParamInfo.get(argPos);
					if (pi.paramType == ParamType.OAObject) {
						if (args[argPos] instanceof OAObject) {
							result = args[argPos].getClass();
							break;
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * Creates form-encoded request body content.
	 *
	 * @param args method call arguments
	 *
	 * @return a map of form-field names to values, or {@code null}
	 *         if no form parameters are present
	 *
	 * <p>
	 * Uses parameters of type {@code FormNameValue}, applying
	 * formatting rules where specified.
	 */
	public String getFormData(Object[] args) throws Exception {
		if (methodType != MethodType.POST) {
			return null;
		}
		String formData = "";

		for (int argPos = 0; argPos < alParamInfo.size(); argPos++) {
			OARestParamInfo pi = alParamInfo.get(argPos);
			final Object objArg = args[argPos];

			if (pi.paramType != OARestParam.ParamType.FormNameValue) {
				continue;
			}

			String s = OAHttpUtil.getUrlEncodedNameValues(pi.name, objArg, pi.format);
			if (OAString.isNotEmpty(s)) {
				if (formData.length() > 0) {
					formData += "&";
				}
				formData += s;
			}
		}
		return formData;
	}

	/**
	 * Extracts the byte-array request body, if present.
	 *
	 * @param args the method invocation arguments
	 *
	 * @return the byte array supplied by a parameter of type
	 *         {@code BodyByteArray}, or {@code null} if none exists
	 *
	 * <p>
	 * Only one such parameter is permitted; if present, it takes
	 * precedence over JSON and form-data bodies.
	 */
	public byte[] getByteArrayBody(Object[] args) throws Exception {
		byte[] bs = null;
		for (int argPos = 0; argPos < alParamInfo.size(); argPos++) {
			OARestParamInfo pi = alParamInfo.get(argPos);
			final Object objArg = args[argPos];

			if (pi.paramType == OARestParam.ParamType.BodyByteArray) {
				bs = (byte[]) objArg;
				if (bs == null) {
					bs = new byte[0];
				}
				break;
			}
		}
		return bs;
	}
}
