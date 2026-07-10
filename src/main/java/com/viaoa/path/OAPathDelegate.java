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
package com.viaoa.path;

import java.util.List;

import com.viaoa.hub.Hub;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.oa.OA;
import com.viaoa.runtime.OARuntime;

/**
 * Helper methods for creating and deriving OA property paths.
 * <p>
 * This class is stateless. It delegates root-qualified path parsing to {@link OAPath} and can derive a dotted link path
 * between a Hub object class and a sequence of target classes using OA relationship metadata.
 * </p>
 */
public class OAPathDelegate {

	/**
	 * Creates a path for text that can include a leading root class qualifier such as {@code [Customer].orders}.
	 *
	 * @param sPropPath property path text to parse
	 * @param packageClass class whose package is used to resolve unqualified root class names
	 * @return parsed OA path
	 * @throws Exception if path construction fails
	 */
	public static OAPath createRootPath(String sPropPath, Class packageClass) throws Exception {
		OAPath pp = new OAPath(packageClass, sPropPath);
		return pp;
	}
	/**
	 * Builds a property path linking the hub's object class through a sequence of
	 * classes. For each class in the array, the method locates a matching link
	 * property that targets that class. If multiple matching links are found, an
	 * exception is thrown. If no matching link exists, {@code null} is returned.
	 *
	 * @param hub     the starting hub whose object class defines the first segment
	 * @param classes array of classes describing the traversal path
	 * @return a dot-delimited property path, or {@code null} if a segment cannot be
	 *         resolved
	 */
	public static String getPathforClasses(final Hub hub, Class[] classes) {
		if (hub == null || classes == null) {
			return null;
		}

		OAObjectInfo oi = hub.getOAObjectInfo();
		Class c = hub.getObjectClass();

		String path = null;
		int x = classes.length;
		for (int i = 0; i < x; i++) {
			// find property to use
			List al = oi.getLinkInfos();
			OALinkInfo liFound = null;
			for (int ii = 0; ii < al.size(); ii++) {
				OALinkInfo li = (OALinkInfo) al.get(ii);
				if (classes[i].equals(li.getToClass())) {
					if (li.getToClass() == null) {
						if (liFound != null) {
							continue;
						}
					}
					if (liFound != null) {
						throw new RuntimeException("more then one link for hubClass=" + c + ", find linkClass=" + classes[i]);
					}
					liFound = li;
					// if (li.getType() == li.ONE) break;  // try to find ONE type, but will settle on MANY
				}
			}
			if (liFound == null) {
				return null;
			}
			if (path == null) {
				path = liFound.getName();
			} else {
				path += "." + liFound.getName();
			}
			c = classes[i];
			oi = liFound.getToObjectInfo();
		}
		return path;
	}


	
	
}
