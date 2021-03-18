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

import java.util.logging.Logger;

import com.viaoa.object.OAObject;
import com.viaoa.sync.remote.RemoteClientImpl;

//qqqqqqqqqq this needs to be registered with OARestServlet qqqqqqqqqqq
public abstract class RemoteRestClientImpl extends RemoteClientImpl {
	private static Logger LOG = Logger.getLogger(RemoteRestClientImpl.class.getName());

	public RemoteRestClientImpl(int sessionId) {
		super(sessionId);
	}

	/**
	 * Called to add objects to a client's server side cache, so that server will not GC the object.
	 */
	public abstract void setCached(OAObject obj);
}
