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
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubSerializeDelegate;
import com.viaoa.remote.multiplexer.io.RemoteObjectInputStream;
import com.viaoa.remote.multiplexer.io.RemoteObjectOutputStream;
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
	private static final Logger LOG = Logger.getLogger(OAObjectSerializeDelegate.class.getName());

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
	protected static void _readObject(OAObject oaObj, java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		// client only needs to send the key to the server
		if (in instanceof RemoteObjectInputStream) {
			byte bx = in.readByte();
			if (bx == 1) {
				OAObjectKey ok = (OAObjectKey) in.readObject();
				oaObj.guid = ok.getGuid();
				cntDup--;
				return;
			} else if (bx == 2) {
			}
		}
		in.defaultReadObject();

		final OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj.getClass());
		final boolean bIsServer = OASyncDelegate.isServer(oaObj.getClass());

		// read properties
		for (;;) {
			Object obj = in.readObject();
			if (!(obj instanceof String)) {
				break; // flag to end
			}

			String key = (String) obj;
			Object value = in.readObject();

			if (value instanceof OANullObject) {
				value = null;
			}

			if (bIsServer) {
				// 20160206 dont read calcProps if server, they need to be recalc'ed 
				OALinkInfo li = oi.getLinkInfo(key);
				if (li != null && li.bCalculated) {
					continue;
				}

				if (value instanceof IODummy) {
					value = null;
				}
				if (value instanceof Hub) {
					Hub hx = (Hub) value;
					if (hx.getObjectClass().equals(IODummy.class)) {
						value = null;
					}
				}

			}

			// 20200102 include blobs
			if (value instanceof byte[] && oi.getHasBlobProperty()) {
				OAPropertyInfo pi = oi.getPropertyInfo(key);
				if (pi != null && pi.isBlob()) {
					byte[] bs = (byte[]) value;
					oaObj.setProperty(key, bs);
					continue;
				}
			}
			OAObjectPropertyDelegate.unsafeSetPropertyIfEmpty(oaObj, key, value); // HubSerializeDelegate._readResolve could have set this first (as weakref)
		}
		OAObjectDelegate.updateGuid(oaObj.guid);
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
	protected static Object _readResolve(final OAObject oaObjRead) throws ObjectStreamException {
		OAObject oaObjUse;

		/* 20151029 on hold
		OASyncCombinedClient cc = OASyncDelegate.getSyncCombinedClient();
		if (cc != null) {
		    oaObjNew = cc.resolveObject(oaObjOrig);
		    if (oaObjNew != null) return oaObjNew;
		}
		*/

		boolean bDup;
		if (oaObjRead.guid == 0) {
			LOG.warning("received object with guid=0, obj=" + oaObjRead + ", reassigning a new guid");
			OAObjectDelegate.assignGuid(oaObjRead);
		}

		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObjRead);
		if (oi.bAddToCache) {

			// todo? need to also check guid 		    

			oaObjUse = OAObjectCacheDelegate.add(oaObjRead, false, false, true);
			bDup = (oaObjRead != oaObjUse);
		} else {
			oaObjUse = oaObjRead;
			bDup = false;
		}

		if (!bDup) {
			cntNew++;
			return oaObjUse;
		}
		cntDup++;

		final Object[] objs = oaObjRead.properties;

		// check to see if references are needed or not
		for (int i = 0; objs != null && i < objs.length; i += 2) {
			String key = (String) objs[i];
			if (key == null) {
				continue;
			}
			Object value = objs[i + 1];

			Object localValue = OAObjectPropertyDelegate.getProperty(oaObjUse, key, true, true);

			if (localValue != OANotExist.instance) {
				if (localValue instanceof OAObjectKey && (value instanceof OAObject)) {
					OAObjectKey k1 = (OAObjectKey) localValue;
					OAObjectKey k2 = OAObjectKeyDelegate.getKey((OAObject) value);
					if (OAObjectKeyDelegate.isForSameOAObject(null, k1, k2)) {
						OAObjectPropertyDelegate.setPropertyCAS(oaObjUse, key, value, localValue);
					}
					continue;
				} else if (localValue == null && value instanceof Hub) {
					// fall through and store the oaObjNew Hub value
				} else {
					continue; // note: any other value could be from a propertyChange that happened on the server, that is in the msg que for this client
				}
			}

			OALinkInfo linkInfo = OAObjectInfoDelegate.getLinkInfo(oi, key);

			// need to replace any references to oaObjOrig with oaObjNew
			boolean b = replaceReferences(oaObjRead, oaObjUse, linkInfo, value);
			if (b) {
				if (value == null && linkInfo.getType() == linkInfo.MANY) {
					// 20150826 skip if prop is locked by another
					try {
						b = OAObjectPropertyDelegate.attemptPropertyLock(oaObjUse, key);
						if (b) {
							OAObjectPropertyDelegate.setPropertyCAS(oaObjUse, key, value, localValue, (localValue == OANotExist.instance),
																	false);
						}
					} finally {
						if (b) {
							OAObjectPropertyDelegate.releasePropertyLock(oaObjUse, linkInfo.getName());
						}
					}
				} else {
					if (value instanceof Hub && linkInfo.cacheSize > 0) {
						Hub hub = (Hub) value;
						if (OAObjectInfoDelegate.cacheHub(linkInfo, hub)) {
							value = new WeakReference(hub);
						}
					}
					OAObjectPropertyDelegate.setPropertyCAS(oaObjUse, key, value, localValue, (localValue == OANotExist.instance), false);
				}
			}
		}
		OAObjectDelegate.dontFinalize(oaObjRead);

		return oaObjUse;
	}

	public static volatile int cntDup;
	public static volatile int cntNew;
	public static volatile int cntSkip;

	/**
	 * Rewrites reverse relationships so that references pointing to {@code oaObjFrom}
	 * now reference {@code oaObjTo}. This is used when merging a deserialized
	 * duplicate object into an existing cached instance.
	 *
	 * <p>Behavior depends on the type of {@code value}:</p>
	 * <ul>
	 *   <li>{@link Hub}: replaces master-object references, iterates through elements,
	 *       and updates reverse properties or nested hubs accordingly.</li>
	 *   <li>{@link OAObject}: updates single-object reverse relationships based on the
	 *       link's reverse name.</li>
	 *   <li>{@link WeakReference}: dereferenced before processing.</li>
	 * </ul>
	 *
	 * <p>If {@code linkInfo} is {@code null}, no action is taken.</p>
	 *
	 * @param oaObjFrom the obsolete object instance being replaced
	 * @param oaObjTo the authoritative instance to redirect references to
	 * @param linkInfo metadata describing the relationship being updated
	 * @param value the relationship value being inspected or rewritten
	 * @return {@code true} if reference replacement should continue; {@code false} otherwise
	 */
	private static boolean replaceReferences(final OAObject oaObjFrom, final OAObject oaObjTo, final OALinkInfo linkInfo, Object value) {
		if (linkInfo == null) {
			return false;
		}

		if (value == null) {
			return true;
		}

		String revName = linkInfo.getReverseName();
		if (revName != null) {
			revName = revName.toUpperCase();
		}

		Object origValue = value;
		if (value instanceof WeakReference) {
			value = ((WeakReference) value).get();
		}

		if (value instanceof Hub) {
			// handles M-1, M-M
			Hub hub = (Hub) value;
			if (!HubSerializeDelegate.isResolved(hub)) {
				// not fully loaded
				return false;
			}

			// this will only replace if current masterObj = oaObjOrig
			HubSerializeDelegate.replaceMasterObject((Hub) value, oaObjFrom, oaObjTo);

			for (int i = 0; revName != null; i++) {
				OAObject objx = (OAObject) hub.getAt(i);
				if (objx == null) {
					break;
				}
				Object ref = OAObjectPropertyDelegate.getProperty(objx, revName, false, true);
				if (ref == null) {
				} else if (ref == oaObjFrom || ref instanceof OAObjectKey) {
					OAObjectPropertyDelegate.setPropertyCAS(objx, revName, oaObjTo, oaObjFrom);
				} else if (ref instanceof Hub) {
					HubSerializeDelegate.replaceObject((Hub) ref, oaObjFrom, oaObjTo);
				}
			}
		} else if (value instanceof OAObject && revName != null) {
			// handles 1-1, 1-Many
			OAObject objx = (OAObject) value;

			Object ref = OAObjectPropertyDelegate.getProperty(objx, revName, false, true);
			if (ref == null) {
				return true;
			}
			if (ref == oaObjFrom || ( (ref instanceof OAObjectKey) && OAObjectKeyDelegate.isForSameOAObject(null, (OAObjectKey)ref, OAObjectKeyDelegate.getKey(oaObjFrom))) )  {
				OAObjectPropertyDelegate.setPropertyCAS(objx, revName, oaObjTo, oaObjFrom);
			} else {
				if (ref instanceof WeakReference) {
					ref = ((WeakReference) ref).get();
				}
				if (ref instanceof Hub) {
					HubSerializeDelegate.replaceObject((Hub) ref, oaObjFrom, oaObjTo);
				}
			}
		}
		return true;
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
	protected static void _writeObject(final OAObject oaObj, java.io.ObjectOutputStream stream) throws IOException {
		//if (xxx % 1000 == 0) System.out.println((xxx)+") writeObject "+oaObj);
		if (oaObj == null) {
			return;
		}
		final OAObjectSerializer serializer = OAThreadLocalDelegate.getObjectSerializer();
		if (serializer != null) {
			serializer.beforeSerialize(oaObj);
		}
		
		final OAObjectInfo oi = OAObjectInfoDelegate.getObjectInfo(oaObj.getClass());
		final boolean bIsServer = OASyncDelegate.isServer(oaObj.getClass());
		final boolean bIsObjectOnServer = bIsServer || OASync.getSyncClient().isObjectOnServer(oaObj);

		
		if (stream instanceof RemoteObjectOutputStream) {
			if (!bIsObjectOnServer) {
				stream.writeByte((byte) 2);
			} else if (!OASyncDelegate.isServer(oaObj.getClass())) {
				// only need to send key to the server
				stream.writeByte((byte) 1);
				stream.writeObject(oaObj.getObjectKey());
				if (serializer != null) {
					serializer.afterSerialize(oaObj);
				}
				return;
			} else {
				stream.writeByte((byte) 0);
			}
		}

		stream.defaultWriteObject(); // does not write references (transient)

		_writeProperties(oi, bIsServer, oaObj, stream, serializer, bIsObjectOnServer); // this will write transient properties

		// 20200102 include blobs
		if (serializer != null && serializer.getIncludeBlobs()) {
			if (oi.getHasBlobProperty()) {
				for (OAPropertyInfo pi : oi.getPropertyInfos()) {
					if (pi.isBlob()) {
						byte[] bs = (byte[]) oaObj.getProperty(pi.getName());
						if (bs != null) {
							stream.writeObject(pi.getName());
							stream.writeObject(bs);
						}
					}
				}
			}
		}

		stream.writeObject(OAObjectDelegate.FALSE); // end of property list

		if (!bIsObjectOnServer) {
	        OASync.getSyncClient().objectSentToServer(oaObj);
		}

		// 20141124
		if (serializer != null) {
			serializer.afterSerialize(oaObj);
		}
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
	protected static void _writeProperties(final OAObjectInfo oi, final boolean bIsServer, final OAObject oaObj,
			final java.io.ObjectOutputStream stream, final OAObjectSerializer serializer, final boolean bIsObjectSentOnServer)
			throws IOException {
		// this method can not support synchronized blocks, since multiple threads could be calling it and then cause deadlock
		// default way for OAServer to send objects.  Clients always send objectKeys.
		//   this way, only the object properties are sent, no reference objects or Hubs
		if (oaObj == null) {
			return;
		}
		Object[] objs = oaObj.properties;
		if (objs == null) {
			return;
		}

		/*
		final OAObjectInfo oi = OAObjectHashDelegate.hashObjectInfo.get(oaObj.getClass());
		final boolean bIsServer = OASyncDelegate.isServer(oaObj.getClass());
		*/

		for (int i = 0; i < objs.length; i += 2) {
			String key = (String) objs[i];
			if (key == null) {
				continue;
			}
			OALinkInfo li = oi.getLinkInfo(key);

			if (li != null && li.bCalculated) {
				if (!bIsServer || !li.bServerSideCalc) {
					continue;
				}
			}

			Object obj = objs[i + 1];

			if (obj instanceof IODummy) {
				continue;
			}

			if (obj instanceof WeakReference) {
				obj = ((WeakReference) obj).get();
				if (obj == null) {
					continue;
				}
			}

			if (obj instanceof Hub) {
				if (((Hub) obj).getObjectClass().equals(IODummy.class)) {
					continue;
				}
			}

			if (obj != null && !(obj instanceof OAObject) && !(obj instanceof OAObjectKey) && !(obj instanceof Hub)
					&& !(obj instanceof byte[])) {
				stream.writeObject(key);
				stream.writeObject(obj);
				continue;
			}

			boolean bShouldSerialize = !bIsObjectSentOnServer;
			if (serializer != null && obj != null && !(obj instanceof byte[])) {
			    bShouldSerialize = serializer.shouldSerializeReference(oaObj, (String) key, obj, li);
			}

			if (bShouldSerialize) {
				if (serializer != null && obj instanceof OAObject) {
					// option to dont send oaobj if it is already on the client
					obj = serializer.getReferenceValueToSend(obj);
				}
			} else {
				// see if something can be sent
				if (obj instanceof OAObject) {
					// always send OAObjectKey to reference objects
					if (bIsServer) {
						obj = OAObjectKeyDelegate.getKey((OAObject) obj);
					}
					bShouldSerialize = true;
				} else if (obj == null || obj instanceof OAObjectKey) {
				    bShouldSerialize = true;
				} else if (obj instanceof Hub) {
					// see if a hub.size=0 can be sent

					Hub hubx = (Hub) obj;
					if (!bIsObjectSentOnServer) {
					    bShouldSerialize = true; // this is when the client is sending an object that the server does not have
					} else if (hubx.size() > 0 || hubx.getSharedHub() != null) {
					    bShouldSerialize = false; // dont send
					} else {
						// if hx.size=0
						if (!bIsServer || li == null) {
							obj = null;
							bShouldSerialize = true;
						} else {
							// server. need to make sure that autoMatch (if needed) was set up.
							String matchProperty = li.getMatchProperty();
							if (matchProperty != null && matchProperty.length() > 0) {
								if (HubDelegate.getAutoMatch(hubx) != null) {
									obj = null;
									bShouldSerialize = true;
								}
								// otherwise, need to call oaObj.getHub(..), so that it's created with an autoMatch  
							} else {
								// 20150826 this was missing (not sure why), needs to send a null for empty hub
								obj = null;
								bShouldSerialize = true;
							}
						}
					}
				}
			}

			if (bShouldSerialize) {
				stream.writeObject(key);
				if (obj == null) {
					obj = OANullObject.instance;
				}
				stream.writeObject(obj);
			}
		}
	}
}
