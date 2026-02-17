package com.viaoa.graph.service.object;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.lang.ref.WeakReference;
import java.util.UUID;
import java.util.logging.Logger;

import com.viaoa.annotation.OAParentProvided;
import com.viaoa.comm.io.IODummy;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubAutoMatch;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAObjectSerializer;
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.remote.multiplexer.io.RemoteObjectInputStream;
import com.viaoa.remote.multiplexer.io.RemoteObjectOutputStream;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadImpl;
import com.viaoa.runtime.thread.OAThreadLocalService;
import com.viaoa.util.OANotExist;
import com.viaoa.util.OANullObject;

public abstract class OAObjectSerializeService {
	private static final Logger LOG = Logger.getLogger(OAObjectSerializeService.class.getName());

	private final OAObjectSerializer.FriendAccess faObjectSerializer;
	
    public OAObjectSerializeService(OAObjectSerializer.FriendAccess faObjectSerializer) {
    	if (faObjectSerializer == null) throw new IllegalArgumentException("OAObjectSerializer.FriendAccess can not be null");
    	this.faObjectSerializer = faObjectSerializer;
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
	public void _readObject(OAObject oaObj, java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		//qqqqqqqq method was protected
		// client only needs to send the key to the server
		if (in instanceof RemoteObjectInputStream) {
			byte bx = in.readByte();
			if (bx == 1) {
				OAObjectKey ok = (OAObjectKey) in.readObject();
				callGuidSetGuid(oaObj, ok.getGuid());

				final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
				final OAObjectSerializer serializer = srvcOAThreadLocal.getCurrentObjectSerializer();
				if (serializer != null) serializer.dupCount--;
				return;
			} else if (bx == 2) {
			}
		}
		in.defaultReadObject();
		
		final OAObjectInfo oi = callInfoGetObjectInfo(oaObj.getClass());
		final boolean bIsServer = callCSIsServer();

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
				if (li != null && li.getCalculated()) {
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
			callPropertyUnsafeSetPropertyIfEmpty(oaObj, key, value); // HubSerializeDelegate._readResolve could have set this first (as weakref)
		}
		//was:  srvcObject.getOAObjectGuidService().updateGuid(srvcObject.getOAObjectGuidService().getGuid(oaObj));
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
	public Object _readResolve(final OAObject oaObjRead) throws ObjectStreamException {
		//qqqqqqqqq method was protected
		OAObject oaObjUse;

		/* 20151029 on hold
		OASyncCombinedClient cc = OASyncDelegate.getSyncCombinedClient();
		if (cc != null) {
		    oaObjNew = cc.resolveObject(oaObjOrig);
		    if (oaObjNew != null) return oaObjNew;
		}
		*/

		boolean bDup;
		if (callGuidGetGuid(oaObjRead) == null) {
			LOG.warning("received object with guid=null, obj=" + oaObjRead + ", reassigning a new guid");
			callGuiAssignGuid(oaObjRead);
		}

		OAObjectInfo oi = callInfoGetObjectInfo(oaObjRead);
		if (oi.getAddToCache()) {
			oaObjUse = callCacheAdd(oaObjRead, false, false, true);
			bDup = (oaObjRead != oaObjUse);
		} else {
			oaObjUse = oaObjRead;
			bDup = false;
		}

		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
		final OAObjectSerializer serializer = srvcOAThreadLocal.getCurrentObjectSerializer();
		if (!bDup) {
			if (serializer != null) serializer.newCount++;
			return oaObjUse;
		}
		if (serializer != null) serializer.dupCount++;

		final Object[] objs = callGetProperties(oaObjRead);

		// check to see if references are needed or not
		for (int i = 0; objs != null && i < objs.length; i += 2) {
			String key = (String) objs[i];
			if (key == null) {
				continue;
			}
			Object value = objs[i + 1];

			Object localValue = callPropertyGetProperty(oaObjUse, key, true, true);

			if (localValue != OANotExist.instance) {
				if (localValue instanceof OAObjectKey && (value instanceof OAObject)) {
					OAObjectKey k1 = (OAObjectKey) localValue;
					OAObjectKey k2 = callKeyGetKey((OAObject) value);
					if (callKeyIsForSameOAObject(null, k1, k2)) {
						callPropertySetPropertyCAS(oaObjUse, key, value, localValue);
					}
					continue;
				} else if (localValue == null && value instanceof Hub) {
					// fall through and store the oaObjNew Hub value
				} else {
					continue; // note: any other value could be from a propertyChange that happened on the server, that is in the msg que for this client
				}
			}

			OALinkInfo linkInfo = callInfoGetLinkInfo(oi, key);

			// need to replace any references to oaObjOrig with oaObjNew
			boolean b = replaceReferences(oaObjRead, oaObjUse, linkInfo, value);
			if (b) {
				if (value == null && linkInfo.getType() == linkInfo.MANY) {
					// 20150826 skip if prop is locked by another
					try {
						b = callPropertyAttemptPropertyLock(oaObjUse, key);
						if (b) {
							callPropertySetPropertyCAS(oaObjUse, key, value, localValue, (localValue == OANotExist.instance), false);
						}
					} finally {
						if (b) {
							callPropertyReleasePropertyLock(oaObjUse, linkInfo.getName());
						}
					}
				} else {
					if (value instanceof Hub && linkInfo.getCacheSize() > 0) {
						Hub hub = (Hub) value;
						if (callInfoCacheHub(linkInfo, hub)) {
							value = new WeakReference(hub);
						}
					}
					callPropertySetPropertyCAS(oaObjUse, key, value, localValue, (localValue == OANotExist.instance), false);
				}
			}
		}
		//qqqqqqqqqqqq make sure other code looks for guid=0, and ignore default cleanup (cached, etc)
		callGuidSetGuid(oaObjRead, null);
		//qqqqqqqq was: OAObjectDelegate.dontFinalize(oaObjRead);

		return oaObjUse;
	}

/*qqqqqqq removing	
	public volatile int cntDup; //qqqqq make atomic
	public volatile int cntNew;
	public volatile int cntSkip; //qqqq is this used?
*/	

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
	private boolean replaceReferences(final OAObject oaObjFrom, final OAObject oaObjTo, final OALinkInfo linkInfo, Object value) {
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
			if (!callHubSerializeIsResolved(hub)) {
				// not fully loaded
				return false;
			}

			// this will only replace if current masterObj = oaObjOrig
			callHubSerializeReplaceMasterObject((Hub) value, oaObjFrom, oaObjTo);

			for (int i = 0; revName != null; i++) {
				OAObject objx = (OAObject) hub.getAt(i);
				if (objx == null) {
					break;
				}
				Object ref = callPropertyGetProperty(objx, revName, false, true);
				if (ref == null) {
				} else if (ref == oaObjFrom || ref instanceof OAObjectKey) {
					callPropertySetPropertyCAS(objx, revName, oaObjTo, oaObjFrom);
				} else if (ref instanceof Hub) {
					callHubSerializeReplaceObject((Hub) ref, oaObjFrom, oaObjTo);
				}
			}
		} else if (value instanceof OAObject && revName != null) {
			// handles 1-1, 1-Many
			OAObject objx = (OAObject) value;

			Object ref = callPropertyGetProperty(objx, revName, false, true);
			if (ref == null) {
				return true;
			}
			if (ref == oaObjFrom || ( (ref instanceof OAObjectKey) && callKeyIsForSameOAObject(null, (OAObjectKey)ref, callKeyGetKey(oaObjFrom))) )  {
				callPropertySetPropertyCAS(objx, revName, oaObjTo, oaObjFrom);
			} else {
				if (ref instanceof WeakReference) {
					ref = ((WeakReference) ref).get();
				}
				if (ref instanceof Hub) {
					callHubSerializeReplaceObject((Hub) ref, oaObjFrom, oaObjTo);
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
	public void _writeObject(final OAObject oaObj, java.io.ObjectOutputStream stream) throws IOException {
		//qqqqqqqq method was protected
		//if (xxx % 1000 == 0) System.out.println((xxx)+") writeObject "+oaObj);
		if (oaObj == null) {
			return;
		}
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
		final OAObjectSerializer serializer = srvcOAThreadLocal.getCurrentObjectSerializer();
		if (serializer != null) {
			faObjectSerializer.beforeSerialize(oaObj, serializer);
		}
		
		final OAObjectInfo oi = callInfoGetObjectInfo(oaObj);
		final boolean bIsServer = callCSIsServer();
		final boolean bIsObjectOnServer = bIsServer || callSyncClientIsObjectOnServer(oaObj);

		
		if (stream instanceof RemoteObjectOutputStream) {
			if (!bIsObjectOnServer) {
				stream.writeByte((byte) 2);
			} else if (!callCSIsServer()) {
				// only need to send key to the server
				stream.writeByte((byte) 1);
				stream.writeObject(oaObj.getObjectKey());
				if (serializer != null) {
					faObjectSerializer.afterSerialize(oaObj, serializer);
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

		stream.writeObject(Boolean.FALSE); // end of property list

		if (!bIsObjectOnServer) {
			callSyncClientObjectSentToServer(oaObj);
		}

		// 20141124
		if (serializer != null) {
			faObjectSerializer.afterSerialize(oaObj, serializer);
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
	public void _writeProperties(final OAObjectInfo oi, final boolean bIsServer, final OAObject oaObj,
			final java.io.ObjectOutputStream stream, final OAObjectSerializer serializer, final boolean bIsObjectSentOnServer)
			throws IOException {
		//qqqqqqq method was protected
		// this method can not support synchronized blocks, since multiple threads could be calling it and then cause deadlock
		// default way for OAServer to send objects.  Clients always send objectKeys.
		//   this way, only the object properties are sent, no reference objects or Hubs
		if (oaObj == null) {
			return;
		}
		
		Object[] objs = callGetProperties(oaObj);
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

			if (li != null && li.getCalculated()) {
				if (!bIsServer || !li.getServerSideCalc()) {
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
			    bShouldSerialize = faObjectSerializer.shouldSerializeReference(serializer, oaObj, (String) key, obj, li);
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
						obj = callKeyGetKey((OAObject) obj);
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
								if (callHubGetAutoMatch(hubx) != null) {
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

	@OAParentProvided (example = "srvcObject.getOAObjectGuidService().setGuid")
	public abstract void callGuidSetGuid(OAObject oaObj, UUID guid);    

	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getOAObjectInfo")
	public abstract OAObjectInfo callInfoGetObjectInfo(Class clazz);

	@OAParentProvided (example = "srvcObject.getOAObjectPropertyService().unsafeSetPropertyIfEmpty")
	public abstract void callPropertyUnsafeSetPropertyIfEmpty(OAObject oaObj, String name, Object value);

	@OAParentProvided (example = "srvcObject.getOAObjectGuidService().getGuid")
	public abstract UUID callGuidGetGuid(OAObject oaObj);
	
	@OAParentProvided (example = "srvcObject.getOAObjectGuidService().assignGuid")
	public abstract void callGuiAssignGuid(OAObject obj);
	
	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getOAObjectInfo")
	public abstract OAObjectInfo callInfoGetObjectInfo(OAObject obj); 
	
	@OAParentProvided (example = "srvcObject.getOAObjectCacheService().add")
	public abstract OAObject callCacheAdd(OAObject obj, boolean bErrorIfExists, boolean bAddToSelectAll, boolean bSendAddEventInAnotherThread);

	@OAParentProvided (example = "srvcObject.getProperties")
	public abstract Object[] callGetProperties(OAObject obj);

	@OAParentProvided (example = "srvcObject.getOAObjectPropertyService().getProperty")
	public abstract Object callPropertyGetProperty(OAObject oaObj, String name, boolean bReturnNotExist, boolean bConvertWeakRef); 

	@OAParentProvided (example = "srvcObject.getOAObjectKeyService().getKey")
	public abstract OAObjectKey callKeyGetKey(OAObject oaObj);

	@OAParentProvided (example = "srvcObject.getOAObjectKeyService().isForSameOAObject")
	public abstract boolean callKeyIsForSameOAObject(final Class<? extends OAObject> clazz, final OAObjectKey ok1, final OAObjectKey ok2);

	@OAParentProvided (example = "srvcObject.getOAObjectPropertyService().setPropertyCAS")
	public abstract Object callPropertySetPropertyCAS(OAObject oaObj, String name, Object newValue, Object matchValue); 
	
	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getLinkInfo")
	public abstract OALinkInfo callInfoGetLinkInfo(OAObjectInfo oi, String propertyName);

	@OAParentProvided (example = "srvcObject.getOAObjectPropertyService().attemptPropertyLock")
	public abstract boolean callPropertyAttemptPropertyLock(OAObject oaObj, String name);

	@OAParentProvided (example = "srvcObject.getOAObjectPropertyService().setPropertyCAS")
	public abstract Object callPropertySetPropertyCAS(OAObject oaObj, String name, Object newValue, Object matchValue, boolean bMustNotExist, boolean bReturnNotExist); 

	@OAParentProvided (example = "srvcObject.getOAObjectPropertyService().releasePropertyLock")
	public abstract void callPropertyReleasePropertyLock(OAObject oaObj, String name);

	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().cacheHub")
	public abstract boolean callInfoCacheHub(OALinkInfo li, final Hub hub);

	@OAParentProvided (example = "srvcObject.getOAObjectCSService().isServer")
	public abstract boolean callCSIsServer();
	
	
	@OAParentProvided (example = "srvcHub.getHubSerializeService().replaceObject")
	public abstract int callHubSerializeReplaceObject(Hub thisHub, OAObject objFrom, OAObject objTo);
	
	@OAParentProvided (example = "srvcHub.getHubSerializeService().isResolved")
	public abstract boolean callHubSerializeIsResolved(Hub thisHub); 

	@OAParentProvided (example = "srvcHub.getHubSerializeService().replaceMasterObject")
	public abstract void callHubSerializeReplaceMasterObject(Hub thisHub, OAObject objFrom, OAObject objTo);
	
	@OAParentProvided (example = "srvcHub.getAutoMatch")
	public abstract HubAutoMatch callHubGetAutoMatch(Hub thisHub);
	
	
	@OAParentProvided (example = "srvcSync.getSyncClient().isObjectOnServer(..)")
	public abstract boolean callSyncClientIsObjectOnServer(OAObject obj);

	@OAParentProvided (example = "srvcSync.getSyncClient().objectSentToServer")
	public abstract void callSyncClientObjectSentToServer(OAObject obj);
	
}
