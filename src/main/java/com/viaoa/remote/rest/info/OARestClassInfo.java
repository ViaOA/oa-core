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

import java.util.ArrayList;
import java.util.List;

/**
 * Holds metadata for a remote REST interface annotated with {@code @OARestClass}.
 * <p>
 * {@code OARestClassInfo} is populated during annotation scanning and
 * represents the structural definition of a remote REST API. It contains the
 * interface name, context path, base URL or routing prefix, and the full set
 * of discovered {@link OARestMethodInfo} objects for all annotated methods.
 * </p>
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Record the interface-level REST context (class name, URL root, tags).</li>
 *   <li>Maintain a list of all REST-accessible methods.</li>
 *   <li>Support lookup of methods by Java reflection method object.</li>
 *   <li>Provide a structure that the OARestClient uses when building invocation metadata.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * The OA REST client automatically builds one {@code OARestClassInfo} instance
 * per remote interface. Application code does not construct or modify instances
 * directly.
 *
 * @author vvia
 */
public class OARestClassInfo {

	/**
	 * The Java interface class that defines the remote REST API. This is the
	 * interface annotated with {@code @OARestClass}.
	 */
	public Class interfaceClass;
	
	/**
	 * List of {@link OARestMethodInfo} objects, one for each annotated method
	 * discovered on the remote interface.
	 */
	public ArrayList<OARestMethodInfo> alMethodInfo = new ArrayList();
	
	/**
	 * Optional context name supplied by {@code @OARestClass}, used as a logical
	 * grouping or routing prefix for the interface's REST methods.
	 */
	public String contextName;

	/**
	 * Creates a new metadata container for the specified remote interface.
	 *
	 * @param clazz the remote interface class annotated with {@code @OARestClass}
	 */
	public OARestClassInfo(Class clazz) {
		this.interfaceClass = clazz;
	}

	/**
	 * Verifies the configuration of all REST methods declared in this class
	 * by invoking {@link OARestMethodInfo#verify()} on each entry. Aggregates
	 * all reported errors into a single list.
	 *
	 * @return a list of configuration error messages, empty if none found
	 */
	public ArrayList<String> verify() {
		ArrayList<String> alErrors = new ArrayList();

		for (OARestMethodInfo mi : alMethodInfo) {
			List<String> al = mi.verify();
			alErrors.addAll(al);
		}
		return alErrors;
	}

}
