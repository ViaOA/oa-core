package com.viaoa.remote.rest.info;

import java.util.ArrayList;

import com.viaoa.remote.rest.annotation.OARestParam;

/**
 * Method parameter information for remote http method calls.
 *
 * @author vvia
 */
public class OARestParamInfo {

	public String name;
	public boolean bNameAssigned;

	public Class rpParamClass;
	public Class origParamClass; // could be array or list, etc

	public Class paramClass; // could be null

	public String format;

	public ClassType classType;

	public ArrayList<String> alIncludePropertyPaths;
	public int includeReferenceLevelAmount;

	public OARestParam.ParamType paramType;

	public static enum ClassType {
		Unassigned,
		String,
		Date,
		DateTime,
		Time,
		Array,
		List,
		JsonNode,
		OARestInvokeInfo,
		ByteArray
	}

}