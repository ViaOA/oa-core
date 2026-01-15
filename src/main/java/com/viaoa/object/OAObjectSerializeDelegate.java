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

import java.io.IOException;
import java.io.ObjectStreamException;
import java.lang.ref.WeakReference;
import java.util.logging.Logger;

import com.viaoa.comm.io.IODummy;
import com.viaoa.graph.OAGraph;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubSerializeDelegate;
import com.viaoa.remote.multiplexer.io.RemoteObjectInputStream;
import com.viaoa.remote.multiplexer.io.RemoteObjectOutputStream;
import com.viaoa.runtime.OARuntime;
import com.viaoa.sync.*;
import com.viaoa.util.OANotExist;
import com.viaoa.util.OANullObject;

/**
 * Delegate supporting binary serialization and deserialization of OAObject
 * instances for caching, messaging, and distributed synchronization.
 *
 * <p>This class transmits minimal identity and current state only, without
 * forcing graph materialization. Relationships are represented using
 * OAObjectKey or empty hubs as appropriate, and full Objects are only
 * sent when required based on the client/server role.</p>
 *
 * <p>Upon deserialization, identity is reconciled with the runtime cache so
 * that only a single authoritative OAObject exists for any given GUID. For
 * duplicates, references are merged and reverse relationships are rewritten
 * to preserve Object Graph consistency.</p>
 *
 * <p>No metadata or Graph structure is changed during serialization;
 * correctness is driven entirely by OAObjectInfo, OALinkInfo, and the
 * distributed identity model.</p>
 *
 * @see OAObject
 * @see OAObjectKey
 * @see OAObjectSerializerCallback
 * @see OAObjectCacheDelegate
 * @see OAObjectInfo
 */
public class OAObjectSerializeDelegate {
	public static final Logger LOG = Logger.getLogger(OAObjectSerializeDelegate.class.getName());

	/*
	OAGraph g = getGraph(null, oaObj);
	if (g == null) return;
	g.objects().getOAObjectPropertyService().??(oaObj);
    */
	
	static OAGraph getGraph(Hub hub, OAObject obj) {
		Class c = null;
		if (hub != null) c = hub.getObjectClass();
		if (c == null && obj != null) c = obj.getClass();
		// if (c == null) return null;
		OAGraph g = OARuntime.get().graph(c);
		return g;
	}
	

	/**
	 * Reads serialized data into the supplied {@link OAObject}. This method handles
	 * both standard Java deserialization and OA-specific transport formats used by
	 * {@link RemoteObjectInputStream}.
	 *
	 * <p>If the input stream is a {@code RemoteObjectInputStream}, the method first
	 * reads a control byte:</p>
	 * <ul>
	 *   <li>{@code 1}: only an {@link OAObjectKey} is transmitted; the object's GUID
	 *       is updated and no additional data is read.</li>
	 *   <li>{@code 2}: reserved value; fall through to default handling.</li>
	 * </ul>
	 *
	 * <p>For non-key-only transfers, {@code defaultReadObject()} is invoked to load
	 * non-transient state. The method then iteratively reads property name/value
	 * pairs until a non-String flag is encountered.</p>
	 *
	 * <p>Special handling includes:</p>
	 * <ul>
	 *   <li>Converting {@link OANullObject} to {@code null}.</li>
	 *   <li>On servers, skipping calculated properties and stripping dummy or
	 *       unresolved hub values.</li>
	 *   <li>Blob properties are assigned directly using {@link OAObject#setProperty}.</li>
	 *   <li>All other values are assigned via
	 *       {@link OAObjectPropertyDelegate#unsafeSetPropertyIfEmpty}.</li>
	 * </ul>
	 *
	 * <p>Finally, the object's GUID is registered using
	 * {@link OAObjectDelegate#updateGuid(long)}.</p>
	 *
	 * @param oaObj the target object receiving deserialized state
	 * @param in the stream providing serialized data
	 * @throws IOException if the stream cannot be read
	 * @throws ClassNotFoundException if a property value refers to an unknown type
	 */
	public static void _readObject(OAObject oaObj, java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectSerializeService()._readObject(oaObj, in);
	}

	/**
	 * Resolves the deserialized {@link OAObject} to the authoritative instance in
	 * the runtime cache. This prevents duplicate object instances after
	 * deserialization.
	 *
	 * <p>The method determines whether the object should be added to the cache based
	 * on {@link OAObjectInfo#bAddToCache}. If a matching instance already exists,
	 * property and relationship references are merged to preserve graph consistency.</p>
	 *
	 * <p>Key behaviors:</p>
	 * <ul>
	 *   <li>Reassigns missing GUIDs (value {@code 0}).</li>
	 *   <li>Adds the object to {@link OAObjectCacheDelegate}; detects duplicates.</li>
	 *   <li>For duplicates, iterates through stored properties and merges references
	 *       using {@link #replaceReferences} and conditional CAS updates.</li>
	 *   <li>Adjusts hubs and cached hub references, wrapping them in
	 *       {@link WeakReference} when required.</li>
	 *   <li>Prevents finalization of the discarded instance via
	 *       {@link OAObjectDelegate#dontFinalize}.</li>
	 * </ul>
	 *
	 * <p>Updates global counters {@code cntNew} and {@code cntDup} accordingly.</p>
	 *
	 * @param oaObjRead the deserialized object instance
	 * @return the resolved object to use within the application
	 * @throws ObjectStreamException if resolution fails
	 */
	public static Object _readResolve(final OAObject oaObjRead) throws ObjectStreamException {
		OAGraph g = getGraph(null, oaObjRead);
		if (g == null) return null;
		return g.objects().getOAObjectSerializeService()._readResolve(oaObjRead);
	}


	/**
	 * Serializes the supplied {@link OAObject} into the given output stream. Handles
	 * both standard Java serialization and OA's optimized remote-transfer formats.
	 *
	 * <p>Processing steps:</p>
	 * <ul>
	 *   <li>Invokes {@link OAObjectSerializer#beforeSerialize} if an object serializer is active.</li>
	 *   <li>Determines server/client role to decide whether to send full object data
	 *       or only an {@link OAObjectKey}.</li>
	 *   <li>For {@link RemoteObjectOutputStream}, writes a control byte indicating
	 *       whether a key-only transmission is used.</li>
	 *   <li>Performs {@code defaultWriteObject()}, then serializes transient
	 *       properties via {@link #_writeProperties}.</li>
	 *   <li>Optionally writes blob properties if the serializer is configured to
	 *       include them.</li>
	 *   <li>Marks end-of-properties using {@link OAObjectDelegate#FALSE}.</li>
	 *   <li>Notifies the sync client when an object is sent to the server.</li>
	 *   <li>Invokes {@link OAObjectSerializer#afterSerialize} if available.</li>
	 * </ul>
	 *
	 * @param oaObj the object to serialize
	 * @param stream the output stream receiving serialized data
	 * @throws IOException if the object cannot be written
	 */
	public static void _writeObject(final OAObject oaObj, java.io.ObjectOutputStream stream) throws IOException {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectSerializeService()._writeObject(oaObj, stream);
	}

	/**
	 * Writes all serializable properties of the given {@link OAObject} to the output
	 * stream. This includes primitive values, object keys, hubs, and reference
	 * objects, depending on the sync role and serializer settings.
	 *
	 * <p>Key behaviors:</p>
	 * <ul>
	 *   <li>Iterates through the internal {@code oaObj.properties} array in key/value pairs.</li>
	 *   <li>Skips calculated link properties unless the server requires them.</li>
	 *   <li>Ignores {@link IODummy} values and unresolved hubs.</li>
	 *   <li>Dereferences {@link WeakReference} wrappers when present.</li>
	 *   <li>Determines whether to send full objects, keys, empty hubs, or no value at
	 *       all based on:</li>
	 *       <ul>
	 *         <li>server/client role</li>
	 *         <li>whether the object has already been sent</li>
	 *         <li>{@link OAObjectSerializer#shouldSerializeReference}</li>
	 *         <li>hub size, match-property rules, and autoMatch state</li>
	 *       </ul>
	 *   <li>Writes property name followed by value, substituting
	 *       {@link OANullObject#instance} for {@code null}.</li>
	 * </ul>
	 *
	 * @param oi metadata describing the object's properties
	 * @param bIsServer whether the current runtime is operating as the server
	 * @param oaObj the object whose properties are being serialized
	 * @param stream the target output stream
	 * @param serializer optional callback controlling reference serialization
	 * @param bIsObjectSentOnServer whether the object was already sent by the server
	 * @throws IOException if any property fails to serialize
	 */
	public static void _writeProperties(final OAObjectInfo oi, final boolean bIsServer, final OAObject oaObj,
			final java.io.ObjectOutputStream stream, final OAObjectSerializer serializer, final boolean bIsObjectSentOnServer)
			throws IOException {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectSerializeService()._writeProperties(oi, bIsServer, oaObj, stream, serializer, bIsObjectSentOnServer);
	}
}
