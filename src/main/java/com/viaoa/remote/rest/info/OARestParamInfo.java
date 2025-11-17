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

import java.util.ArrayList;

import com.viaoa.remote.rest.annotation.OARestParam;

/**
 * Holds metadata for a single parameter of a REST-annotated method. Instances
 * of this class are created during annotation scanning and describe how one
 * method parameter is mapped into the HTTP request.
 *
 * <h2>Parameter Metadata</h2>
 * <ul>
 *   <li>Java parameter index and reflected type information</li>
 *   <li>Parameter name (from annotation or reflection)</li>
 *   <li>Binding rule (path variable, query parameter, request body, form field)</li>
 *   <li>Property-path settings for OAObject serialization</li>
 *   <li>Formatting options and optional explicit serializer overrides</li>
 *   <li>Whether null is allowed and whether the param is required</li>
 * </ul>
 *
 * <h2>Usage During Invocation</h2>
 * At runtime this metadata is used by the REST client to:
 * <ul>
 *   <li>Insert values into the URL template.</li>
 *   <li>Append query string parameters.</li>
 *   <li>Serialize object values into JSON request bodies.</li>
 *   <li>Apply property-path scope rules for OAObject graphs.</li>
 * </ul>
 *
 * <p>
 * Application code never constructs or modifies {@code OARestParamInfo}
 * instances directly.
 * </p>
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