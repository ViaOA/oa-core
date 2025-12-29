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
package com.viaoa.object;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.annotation.OACalculatedProperty;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAColumn;
import com.viaoa.annotation.OAFkey;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OAIndex;
import com.viaoa.annotation.OAIndexColumn;
import com.viaoa.annotation.OALinkTable;
import com.viaoa.annotation.OAMany;
import com.viaoa.annotation.OAMethod;
import com.viaoa.annotation.OAObjCallback;
import com.viaoa.annotation.OAOne;
import com.viaoa.annotation.OAProperty;
import com.viaoa.annotation.OATable;
import com.viaoa.annotation.OATriggerMethod;
import com.viaoa.datasource.jdbc.db.Column;
import com.viaoa.datasource.jdbc.db.Database;
import com.viaoa.datasource.jdbc.db.Index;
import com.viaoa.datasource.jdbc.db.Table;
import com.viaoa.graph.OAGraph;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.runtime.OARuntime;
import com.viaoa.util.*;

//qqqqqqqqq PHASE 3: moved to OAObjectAnnotationService, OAObjectDatabaseService


/**
 * Internal delegate responsible for processing OA-specific annotations on
 * OAObject classes, properties, and links. These annotations supplement the
 * metadata defined by OABuilder, allowing code-level refinement of edit rules,
 * display attributes, and other runtime behaviors.
 *
 * <p>Annotation processing occurs during metadata initialization and prior to
 * any OAObject instances being created. This ensures that decorated metadata is
 * stable, thread-safe, and consistently applied throughout the OA Object Graph.</p>
 *
 * <p>This delegate enables a hybrid model-driven architecture: the application
 * model defines the core business schema while annotations provide convenient
 * inline overrides and refinements without requiring regeneration or extensive
 * configuration.</p>
 *
 * @see OAObjectInfo
 * @see OAPropertyInfo
 * @see OALinkInfo
 * @see OAMethodInfo
 */
public class OAAnnotationDelegate {
	private static Logger LOG = Logger.getLogger(OAAnnotationDelegate.class.getName());

	/**
	 * Updates the specified {@link OAObjectInfo} by processing all OA-related
	 * annotations declared on the given class and its superclasses (excluding
	 * {@link OAObject} itself).
	 * <p>
	 * Each annotation type is applied once per class hierarchy level, using an
	 * internal tracking set to prevent duplicate processing.
	 *
	 * @param oi    the metadata object to update
	 * @param clazz the class whose annotations will be processed
	 */
	public static void update(OAObjectInfo oi, Class clazz) {
		if (clazz == null) return;
		OAGraph og = OARuntime.get().graph(clazz);
		if (og == null) return;
		og.objects().getOAObjectAnnotationService().update(oi, clazz);
	}

	/**
	 * Performs a second-phase annotation update on the specified {@link OAObjectInfo},
	 * processing {@link OATriggerMethod} annotations declared on the class and its
	 * superclasses (excluding {@link OAObject}).
	 *
	 * @param oi    the metadata object to update
	 * @param clazz the class whose trigger annotations will be processed
	 */
	public static void update2(OAObjectInfo oi, Class clazz) {
		if (clazz == null) return;
		OAGraph og = OARuntime.get().graph(clazz);
		if (og == null) return;
		og.objects().getOAObjectAnnotationService().update2(oi, clazz);
	}



	/**
	 * Determines the OAObject class contained within a {@link Hub} return type.
	 * <p>
	 * The hub's element type is first resolved using reflection; if unavailable,
	 * the {@link OAMany#toClass()} annotation value is used when defined.
	 *
	 * @param annotation the {@link OAMany} annotation on the method, or {@code null}
	 * @param method     the accessor method returning a {@link Hub}
	 * @return the class of objects stored in the hub, or {@code null} if unresolved
	 */
	public static Class getHubObjectClass(OAMany annotation, Method method) {
		Class cx = OAObjectReflectDelegate.getHubObjectClass(method);
		if (cx == null && annotation != null) {
			Class cz = annotation.toClass();
			if (cz != null && !cz.equals(Object.class)) {
				cx = cz;
			}
		}
		return cx;
	}



	/**
	 * Updates the database metadata using annotations declared on the supplied
	 * classes. Column definitions are created first, followed by table-level
	 * updates such as foreign keys, link tables, and indexes.
	 *
	 * @param database the database metadata container to update
	 * @param classes  the classes whose annotations define table and column structure
	 * @throws Exception if required annotations are missing or inconsistent
	 */
	public static void update(Database database, Class[] classes) throws Exception {
		if (classes == null) {
			return;
		}
		
		for (Class c : classes) {
			OAGraph og = OARuntime.get().graph(c);
			if (og == null) continue;
			og.objects().getOAObjectDatabaseService().update(database, classes);
			break;
		}
	}

	/**
	 * Builds import-match property mappings for the specified {@link OAObjectInfo}.
	 * <p>
	 * All properties and links marked as import-match are collected, and recursive
	 * property paths are generated so that import operations can match incoming
	 * data to the correct object fields.
	 *
	 * @param oi the metadata object whose import-match mappings will be populated
	 */
	public static void updateImportMatches(OAObjectInfo oi) {
		if (oi == null) return;
		Class c = oi.getForClass();
		if (c == null) return;
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return;
		og.objects().getOAObjectAnnotationService().updateImportMatches(oi);
	}


	/**
	 * Extracts a property name from a getter/setter-style method name.
	 * <p>
	 * Recognizes prefixes {@code get}, {@code is}, {@code has}, and {@code set},
	 * removing the prefix and converting the first character to lowercase.
	 *
	 * @param s the method name
	 * @return the derived property name
	 */
	public static String getPropertyName(String s) {
		return getPropertyName(s, true);
	}

	/**
	 * Extracts a property name from a method name using JavaBean-style prefix rules.
	 * <p>
	 * Recognizes prefixes {@code get}, {@code is}, {@code has}, and {@code set}.
	 * If {@code bToLower} is true, the resulting name begins with a lowercase letter.
	 *
	 * @param s        the method name
	 * @param bToLower whether to lowercase the first character of the extracted name
	 * @return the derived property name
	 */
	public static String getPropertyName(String s, boolean bToLower) {
		boolean b = true;
		if (s.startsWith("get")) {
			s = s.substring(3);
		} else if (s.startsWith("is")) {
			s = s.substring(2);
		} else if (s.startsWith("has")) {
			s = s.substring(3);
		} else if (s.startsWith("set")) {
			s = s.substring(3);
		} else {
			b = false;
		}
		if (bToLower && b && s.length() > 1) {
			s = Character.toLowerCase(s.charAt(0)) + s.substring(1);
		}
		return s;
	}

	/**
	 * Resolves and updates foreign-key metadata for all ONE-type links in the
	 * specified {@link OAObjectInfo}.
	 * <p>
	 * For each link, the corresponding target-property metadata is assigned to
	 * its {@link OAFkeyInfo} entries. This must be called after all object infos
	 * have been initialized.
	 *
	 * @param oi the metadata object whose link foreign-key information will be updated
	 */
	public static void updateLinkFkeys(final OAObjectInfo oi) {
		if (oi == null) return;
		Class c = oi.getForClass();
		if (c == null) return;
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return;
		og.objects().getOAObjectAnnotationService().updateLinkFkeys(oi);
	}
}
