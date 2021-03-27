/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.viaoa.datasource.rest;

import com.viaoa.object.OAObject;
import com.viaoa.remote.rest.annotation.OARestClass;
import com.viaoa.remote.rest.annotation.OARestMethod;
import com.viaoa.remote.rest.annotation.OARestMethod.MethodType;
import com.viaoa.remote.rest.annotation.OARestParam;
import com.viaoa.remote.rest.annotation.OARestParam.ParamType;

/**
 * OADataSource for client apps that uses OARestClientto use OADataSource on Server with OARestServlet.
 *
 * @author vvia
 */
@OARestClass()
public interface OADataSourceRestInterface {

	@OARestMethod(methodType = MethodType.OARemote)
	boolean getAssignIdOnCreate();

	@OARestMethod(methodType = MethodType.OARemote)
	boolean isAvailable();

	@OARestMethod(methodType = MethodType.OARemote)
	int getMaxLength(Class c, String propertyName);

	@OARestMethod(methodType = MethodType.OARemote)
	boolean isClassSupported(Class clazz);

	@OARestMethod(methodType = MethodType.OARemote)
	void insertWithoutReferences(OAObject obj);

	@OARestMethod(methodType = MethodType.OARemote)
	void insert(OAObject obj);

	@OARestMethod(methodType = MethodType.OARemote)
	void update(OAObject obj, String[] includeProperties, String[] excludeProperties);

	@OARestMethod(methodType = MethodType.OARemote)
	void save(OAObject obj);

	@OARestMethod(methodType = MethodType.OARemote)
	void delete(OAObject obj);

	@OARestMethod(methodType = MethodType.OARemote)
	void deleteAll(Class c);

	@OARestMethod(methodType = MethodType.OARemote)
	int count(Class selectClass, String queryWhere, Object[] params, Class whereObjectClass, String whereKey,
			String propertyFromWhereObject, String extraWhere, int max);

	@OARestMethod(methodType = MethodType.OARemote)
	int countPassthru(Class selectClass, String queryWhere, int max);

	@OARestMethod(methodType = MethodType.OARemote)
	boolean supportsStorage();

	@OARestMethod(methodType = MethodType.OARemote)
	int select(Class selectClass,
			String queryWhere, Object[] params, String queryOrderBy,
			Class whereObjectClass, String whereKey, String propertyFromWhereObject, String extraWhere,
			int max, boolean bDirty);

	@OARestMethod(methodType = MethodType.OARemote)
	int selectPassThru(Class selectClass,
			String queryWhere, String queryOrder,
			int max, boolean bDirty);

	@OARestMethod(methodType = MethodType.OARemote)
	Object execute(String command);

	@OARestMethod(methodType = MethodType.OARemote)
	OAObject assignId(OAObject obj, @OARestParam(type = ParamType.MethodReturnClass) Class<? extends OAObject> class1);

	@OARestMethod(methodType = MethodType.OARemote)
	boolean willCreatePropertyValue(OAObject object, String propertyName);

	@OARestMethod(methodType = MethodType.OARemote)
	void updateMany2ManyLinks(Class masterClass, String masterId, OAObject[] adds, Class addClazz, OAObject[] removes, Class removeClazz,
			String propertyNameFromMaster);

	@OARestMethod(methodType = MethodType.OARemote)
	OAObject[] next(int selectId, @OARestParam(type = ParamType.MethodReturnClass) Class clazz);

	@OARestMethod(methodType = MethodType.OARemote)
	void removeSelect(int selectId);
}
