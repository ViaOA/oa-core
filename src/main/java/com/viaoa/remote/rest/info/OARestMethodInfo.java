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
import com.viaoa.template.OATemplate;
import com.viaoa.util.OAConv;
import com.viaoa.util.OAHttpUtil;
import com.viaoa.util.OAReflect;
import com.viaoa.util.OAString;

/**
 * Manages information and metadata about Java remote methods that use REST API endpoints.
 *
 * @author vvia
 */
public class OARestMethodInfo {

	public OARestMethod restMethod;

	public OARestClassInfo classInfo;

	public Method method;

	public String name;
	public String urlPath;
	public String derivedUrlPath;
	public OATemplate urlPathTemplate;

	public String urlQuery;

	public ArrayList<OARestParamInfo> alParamInfo = new ArrayList();

	public String objectMethodName;

	public Class origReturnClass;
	public Class rmReturnClass;
	public Class returnClass;

	public OARestMethod.MethodType methodType;
	public ReturnClassType returnClassType;

	public ArrayList<String> alErrors;

	public static enum ReturnClassType {
		Unassigned,
		Void,
		String,
		Array,
		List,
		Hub,
		JsonNode,
		InvokeInfo
	}

	public int includeReferenceLevelAmount;
	public List<String> alIncludePropertyPaths;

	public String searchWhere;
	public String searchOrderBy;

	public OARestMethodInfo(Method method) {
		this.method = method;
		this.restMethod = method.getAnnotation(OARestMethod.class);
	}

	public List<String> verify() {
		return alErrors;
	}

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

	public boolean verifyParamType(String msgPrefix, List<String> alErrors, OARestParamInfo pi, ParamType ptCheck,
			boolean bUsesName,
			boolean bUsesParamClass,
			boolean bUsesFormat,
			boolean bUsesIncludePPs) {
		return verifyParamType(msgPrefix, alErrors, pi, ptCheck, bUsesName, bUsesParamClass, bUsesFormat, bUsesIncludePPs, false);
	}

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

	protected void verifyMethodTypePUT(String msgPrefix, List<String> alErrors) {
		if (methodType != MethodType.PUT) {
			return;
		}
		_verifyMethodTypeX(msgPrefix, alErrors);
	}

	protected void verifyMethodTypePATCH(String msgPrefix, List<String> alErrors) {
		if (methodType != MethodType.PATCH) {
			return;
		}
		_verifyMethodTypeX(msgPrefix, alErrors);
	}

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

	protected void verifyIncludePropertyPaths(String msgPrefix, List<String> alErrors) {
		if (alIncludePropertyPaths == null || alIncludePropertyPaths.size() == 0) {
			return;
		}

		if (!OAObject.class.isAssignableFrom(returnClass)) {
			String s = "includePropertyPaths not needed, since return class is not OAObject";
			alErrors.add(msgPrefix + s);
		}
	}

	protected void verifyIncludeReferenceLevelAmount(String msgPrefix, List<String> alErrors) {
		if (includeReferenceLevelAmount == 0) {
			return;
		}

		if (!OAObject.class.isAssignableFrom(returnClass)) {
			String s = "includeReferenceLevelAmount > 0, since return class is not OAObject";
			alErrors.add(msgPrefix + s);
		}
	}

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

	protected void verifyUrlPath(String msgPrefix, List<String> alErrors) {
		if (!restMethod.methodType().requiresUrlPath) {
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
				OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(this.origReturnClass);
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
				OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(this.returnClass);
				derivedUrlPath = "/" + OAString.mfcl(oi.getPluralName());
			}
		} else if (methodType == MethodType.OARemote) {
			derivedUrlPath = "/oaremote";
		}
	}

	/**
	 * Called when a method is invoked, so that all of the HTTP params can be setup.
	 * <p>
	 * Note: if a method param/argument is type OARestInvokeInfo, then it will be used instead of creating a new one. If so, then it will be
	 * updated to match the current method call and argument values.
	 * <p>
	 *
	 * @param args from method invocation
	 * @return new RestInvokeInfo with all of the HTTP information needed to make the call to the endpoint.
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
			mt = "POST";
			break;
		case OAInsert:
			mt = "POST";
			break;
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

	public OATemplate getUrlPathTemplate() {
		if (urlPathTemplate != null) {
			return urlPathTemplate;
		}
		urlPathTemplate = new OATemplate(derivedUrlPath);
		return urlPathTemplate;
	}

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
						OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo((Class) args[pos]);
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

	/*
	 *  Build http body using Json.
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
