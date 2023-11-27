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
package com.viaoa.object;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import com.viaoa.comm.io.IODummy;
import com.viaoa.hub.Hub;
import com.viaoa.remote.multiplexer.io.RemoteObjectInputStream;
import com.viaoa.remote.multiplexer.io.RemoteObjectOutputStream;
import com.viaoa.util.Tuple;

/**
 * OAObjectSerializer is used to control object serialization for an OAObject and its reference objects.
 * <p>
 * OAObject serialization will then call the serializer to determine which reference properties should be included in the serialization.
 * <p>
 * Note: this is final so that it can not be subclassed, which would cause serialization problems when it tries to recreate with the new
 * subclass instance - remember this is a wrapper that is serialized and transported, then unserialized (trust me, painful lessons here ha)
 * Use setCallback(..) to be able to control each object's setting as it is serialized.
 */
public final class OAObjectSerializer<TYPE> implements Serializable {
	static final long serialVersionUID = 1L;
	private static final Logger LOG = Logger.getLogger(OAObjectSerializer.class.getName());

	private int clientId; // 20171216
	private int id; // 20171216
	private Object object; // object to serialize
	private Object extraObject; // extra object to serialize

	private transient boolean bCompress;
	private transient Stack<Tuple<String[], String[]>> stack = new Stack(); // used for callback, to store properties to include and exclude
	private transient Stack stackObject = new Stack(); // used for callback, to know which objects are currently being serialized
	private transient Class[] excludedReferences;

	private transient int overflowLimit = 100; // this might need to be adjusted for handling stackOverflow

	// how many objects are currently being serialized.  Level is changed after callback.eetup(..) is called
	private transient int levelsDeep;
	private int totalObjectsWritten;

	final static String[] EmptyProperties = new String[0];

	transient String[] includeProps;
	transient String[] excludeProps;

	private transient OAObjectSerializerCallback callback;

	private static volatile int wcnter;
	private static volatile int rcnter;

	// Solution for handling deep object graphs that can cause stack overflow exceptions:
	// This is used to handle stackTraceOverflow from happening.
	//   The graph will only allow for "overflowLimit" recursive objects, and will then
	//   add additional OAObjectSerializers that will then be included.
	private transient OAObjectSerializer parentWrapper;
	private transient LinkedList<Overflow> listOverflow;

	static class Overflow implements Serializable {
		OAObject parentObject;
		String property;
		transient int levelsDeep;
		transient Object object;
		transient Stack stack;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return this.id;
	}

	public void setClientId(int id) {
		this.clientId = id;
	}

	public int getClientId() {
		return this.clientId;
	}

	/**
	 * Max number of objects to serialize.
	 */
	private transient int maxObjects;
	private transient int minExpectedAmt; // minimum expected to save

	private transient int maxSize;
	private transient HashMap<OALinkInfo, Integer> hmLinkInfoCount;

	/**
	 * @param object   root object to serialize
	 * @param callback object that will be called (setupProperties(obj)) for each object that will be serialized, used to determine which
	 *                 reference properties to include.
	 */
	public OAObjectSerializer(TYPE object, boolean bCompress, OAObjectSerializerCallback callback) {
		this.object = object;
		this.bCompress = bCompress;
		if (object instanceof Hub) {
			minExpectedAmt = ((Hub) object).getSize();
		}
		setCallback(callback);
	}

	public OAObjectSerializer(TYPE object, boolean bCompress) {
		this.object = object;
		this.bCompress = bCompress;
		if (object instanceof Hub) {
			minExpectedAmt = ((Hub) object).getSize();
		}
	}

	public OAObjectSerializer(TYPE object, Object extraObject, boolean bCompress, OAObjectSerializerCallback callback) {
		this.object = object;
		this.extraObject = extraObject;
		this.bCompress = bCompress;
		if (object instanceof Hub) {
			minExpectedAmt = ((Hub) object).getSize();
		}
		setCallback(callback);
	}

	/**
	 * Used to serialize an object.
	 * 
	 * @param object         root object to serialize.
	 * @param bCompress      use compression
	 * @param bAllReferences flag to know if all references or no references should be serialized with object. see
	 *                       #setExcludedClasses(Class[]) to include reference classes should not be serialized.
	 */
	public OAObjectSerializer(TYPE object, boolean bCompress, boolean bAllReferences) {
		this.object = object;
		this.bCompress = bCompress;
		if (bAllReferences) {
			includeAllProperties();
		} else {
			excludeAllProperties();
		}
	}

	// 20200102 include blobs
	private boolean bIncludeBlobs;

	/**
	 * OAObject blob properties default to transient. Setting this to true will have them included.
	 */
	public boolean getIncludeBlobs() {
		return bIncludeBlobs;
	}

	public void setIncludeBlobs(boolean b) {
		bIncludeBlobs = b;
	}

	/**
	 * Classes that should not be serialized.
	 */
	public void setExcludedReferences(Class... classes) {
		//LOG.finer("excludedReferences="+classes);
		this.excludedReferences = classes;
	}

	public void excludedClasses(Class... classes) {
		//LOG.finer("excludedReferences="+classes);
		this.excludedReferences = classes;
	}

	/**
	 *    
	 */
	public Object getReferenceValueToSend(Object obj) {
		if (callback != null) {
			obj = callback.getReferenceValueToSend(obj);
		}
		return obj;
	}

	/**
	 * Max number of objects to serialize.
	 */
	public void setMax(int max) {
		this.maxObjects = max;
	}

	public int getMax() {
		return this.maxObjects;
	}

	public int getTotalObjectsWritten() {
		return totalObjectsWritten;
	}

	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}

	public int getMaxSize() {
		return this.maxSize;
	}

	// private int indent;

	/**
	 * Called by OAObjectSerializeDelegate.writeObject(), before an object is serialized. If a Callback object is used, then it's
	 * setupSerializedProperties() method will be called so that it can configure the reference properties to include.
	 * 
	 * @see OAObjectSerializeCallback#setupSerializedProperties(Object, Stack)
	 */
	void beforeSerialize(OAObject oaObj) {
		/* test        
		indent++;
		String msg = "";
		for (int i=0; i<indent; i++) msg += "  ";
		System.out.println(msg+""+oaObj.getClass()+" "+oaObj.getObjectKey().getGuid());
		 */

		totalObjectsWritten++;
		if (callback != null) {
			// save and push current settings into stack
			Tuple<String[], String[]> t = new Tuple<String[], String[]>(includeProps, excludeProps);
			stack.push(t);
			callback.beforeSerialize(oaObj);
		}

		// now save the obj in stack for further embeded objects to "see" where they are in the object tree.
		stackObject.push(oaObj);

		levelsDeep++;
	}

	/**
	 * Called by OAObjectSerializeDelegate.writeObject(), after an object has been serialized.
	 */
	void afterSerialize(OAObject obj) {
		// indent--;        
		if (callback != null) {
			callback.afterSerialize(obj);
		}
		stackObject.pop();
		if (callback != null) {
			Tuple<String[], String[]> t = stack.pop();
			includeProps = t.a;
			excludeProps = t.b;
		}
		levelsDeep--;
	}

	protected void includeProperties(String[] props) {
		this.includeProps = props;
		this.excludeProps = null;
	}

	protected void excludeProperties(String[] props) {
		this.excludeProps = props;
		this.includeProps = null;
	}

	protected void includeAllProperties() {
		this.excludeProps = EmptyProperties;
		this.includeProps = null;
	}

	protected void excludeAllProperties() {
		this.includeProps = EmptyProperties;
		this.excludeProps = null;
	}

	protected int getStackSize() {
		return stackObject.size();
	}

	/**
	 * @return previous object on the stack.
	 */
	protected Object getPreviousObject() {
		return getStackObject(0);
	}

	/*
	 * Last object put on stack is 0, followed by 1,2,3,...
	 */
	protected Object getStackObject(int pos) {
		int x = stackObject.size();
		x--;
		x -= pos;
		if (x < 0) {
			return null;
		}
		return stackObject.elementAt(x);
	}

	/**
	 * Number of objects currently being serialized. first object is level 0 This is incremented by beforeSerialize() and decremented by
	 * afterSerialize()
	 * 
	 * @return
	 */
	public int getLevelsDeep() {
		return levelsDeep;
	}

	/**
	 * Called by OAObjectSerializeDelegate._writeObject() to determine if an object reference property should be included for serialization.
	 * If the levelsDeep is &gt; overflowLimit, then the reference will be added to a list to serialize once the wrapper object is
	 * serialized.
	 */
	protected boolean shouldSerializeReference(OAObject oaObj, String propertyName, Object obj) {
		return shouldSerializeReference(oaObj, propertyName, obj, null);
	}

	protected boolean shouldSerializeReference(OAObject oaObj, String propertyName, Object obj, OALinkInfo linkInfo) {
		boolean b = _shouldSerializeReference(oaObj, propertyName, obj);

		// 20141023 dont send more back then cache is setup for
		if (b && linkInfo != null && linkInfo.getType() == OALinkInfo.MANY) {
			int x = linkInfo.getCacheSize();
			if (x > 0) {
				if (hmLinkInfoCount == null) {
					hmLinkInfoCount = new HashMap<OALinkInfo, Integer>();
				}
				Object objx = hmLinkInfoCount.get(linkInfo);
				if (objx != null) {
					int x2 = ((Integer) objx).intValue();
					if (x2 > x) {
						return false;
					}
					hmLinkInfoCount.put(linkInfo, new Integer(x2 + 1));
				}
			}
		}
		if (callback != null) {
			b = callback.shouldSerializeReference(oaObj, propertyName, obj, b);
		}
		if (!b) {
			return false;
		}

		if (levelsDeep >= overflowLimit && obj != null) {
			Overflow overFlow = new Overflow();
			overFlow.parentObject = oaObj;
			overFlow.property = propertyName;
			overFlow.object = obj;
			overFlow.stack = (Stack) this.stackObject.clone();
			overFlow.levelsDeep = this.levelsDeep;
			if (listOverflow == null) {
				listOverflow = new LinkedList<Overflow>();
			}
			listOverflow.add(overFlow);
			// LOG.finer("adding to overflow, levelsDeep="+levelsDeep+", object class="+oaObj.getClass().getName()+", property="+propertyName+", overFlowSize="+listOverflow.size());            
			return false;
		}
		return true;
	}

	private boolean bReachedMax;

	// 20160502
	public boolean hasReachedMax() {
		if (bReachedMax) {
			return true;
		}
		if (maxSize > 0 && (getCompressedWritten() > maxSize)) {
			bReachedMax = true;
		} else if (maxObjects > 0 && ((totalObjectsWritten + minExpectedAmt) > maxObjects)) {
			bReachedMax = true;
		}
		return bReachedMax;
	}

	/**
	 * Used to determine if an object reference property should be include for serialization. This will used the excludedClasses, callback,
	 * or allReferences flag.
	 */
	private boolean _shouldSerializeReference(OAObject oaObj, String propertyName, Object reference) {
		if (maxSize > 0) {
			if (getCompressedWritten() > maxSize) {
				return false;
			}
		}
		if (maxObjects > 0) {
			if ((totalObjectsWritten + minExpectedAmt) > maxObjects) {
				return false; // 20141119
			}
			if (reference instanceof Hub) {
				Hub h = (Hub) reference;
				if (totalObjectsWritten + minExpectedAmt + h.getSize() > maxObjects) {
					return false; // 20141119
				}
			}
		}
		if (parentWrapper != null) {
			return parentWrapper._shouldSerializeReference(oaObj, propertyName, reference);
		}

		if (excludedReferences != null && reference != null) {
			Class clazz;
			if (reference instanceof Hub) {
				clazz = ((Hub) reference).getObjectClass();
			} else {
				clazz = reference.getClass();
			}
			for (int i = 0; excludedReferences != null && i < excludedReferences.length; i++) {
				if (clazz.equals(excludedReferences[i])) {
					return false;
				}
			}
		}

		if (excludeProps != null) { // set when beforeSerialize() is called
			for (int i = 0; i < excludeProps.length; i++) {
				if (propertyName.equalsIgnoreCase(excludeProps[i])) {
					return false;
				}
			}
			return true;
		}
		if (includeProps != null) { // set when beforeSerialize() is called
			for (int i = 0; includeProps != null && i < includeProps.length; i++) {
				if (propertyName.equalsIgnoreCase(includeProps[i])) {
					return true;
				}
			}
			return false;
		}
		return true; // default, must be true
	}

	/**
	 * Called by objectStream to serialize wrapper. This will register this wrapper with OAThreadInfo so that OAObject will then use it for
	 * serializing.
	 */
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		try {
			OAThreadLocalDelegate.setObjectSerializer(this);
			_writeObject(stream);
		} catch (Throwable e) {
			LOG.log(Level.WARNING, "OAObjectSerializer.writeObject exception", e);
			// note: this is ignored when invoked by RMI
			throw new IOException("OAObjectSerializer.writeObject exception", e);
		} finally {
			OAThreadLocalDelegate.setObjectSerializer(null);
		}
	}

	public long getCompressedWritten() {
		if (deflater == null) {
			return -1;
		}
		long x = deflater.getBytesWritten();
		return x;
	}

	private transient Deflater deflater;

	/**
	 * Note: if compression is true, then this will use a new ObjectOutputStream to save the wrapped object. Remember: each ObjectStream
	 * keeps track of the objects that it has serialized and will not duplicated them, instead it will use an internal reference.
	 */
	private void _writeObject(ObjectOutputStream stream) throws IOException {
		long ts = System.currentTimeMillis();

		stream.writeInt(getId()); // 20171216
		stream.writeBoolean(bCompress);
		String msg;
		if (bCompress) {
			deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);//BEST_SPEED BEST_COMPRESSION);
			DeflaterOutputStream dos = new DeflaterOutputStream(stream, deflater, 1024 * 6);

			RemoteObjectOutputStream roos;
			boolean bStreamIsRoos = (stream instanceof RemoteObjectOutputStream);

			if (bStreamIsRoos) {
				roos = new RemoteObjectOutputStream(dos, (RemoteObjectOutputStream) stream);
			} else {
				roos = new RemoteObjectOutputStream(dos, null);
			}

			roos.writeBoolean(object != null);
			if (object != null) {
				roos.writeObject(object);
			}

			roos.writeBoolean(extraObject != null);
			if (extraObject != null) {
				roos.writeObject(extraObject);
			}

			finishWrite(roos);

			roos.flush();
			if (!bStreamIsRoos) {
				roos.close();
			}
			dos.finish();
			dos.flush();
			if (!bStreamIsRoos) {
				dos.close();
			}

			long sizeBefore = deflater.getBytesRead();
			long sizeAfter = deflater.getBytesWritten();
			deflater.end();

			long ts2 = System.currentTimeMillis();

			msg = String.format(
								"client=%d, id=%,d, class=%s, extra=%s, uncompressed=%,d, compressed=%,d, totalObjects=%,d, %,dms",
								clientId, id, object.getClass().getSimpleName(),
								extraObject == null ? "null" : extraObject.getClass().getSimpleName(),
								sizeBefore, sizeAfter, totalObjectsWritten, (ts2 - ts));
		} else {
			stream.writeBoolean(object != null);
			if (object != null) {
				stream.writeObject(object);
			}
			stream.writeBoolean(extraObject != null);
			if (extraObject != null) {
				stream.writeObject(extraObject);
			}
			finishWrite(stream);

			long ts2 = System.currentTimeMillis();
			msg = String.format(
								"client=%d, id=%,d, class=%s, extra=%s, totalObjects=%,d, %,dms",
								clientId, id, object.getClass().getSimpleName(),
								extraObject == null ? "null" : extraObject.getClass().getSimpleName(),
								totalObjectsWritten, (ts2 - ts));
		}
		stream.writeInt(totalObjectsWritten);

		/*
		if (totalObjectsWritten > 120000 || totalObjectsWritten < 0) {
		    msg = " ALERT, totalObjectsWritten is wrong";
		    LOG.warning(msg);
		}
		*/

		wcnter++;
		LOG.fine(wcnter + ") " + msg);

		OAPerformance.LOG.fine(wcnter + ") " + msg);
		if (OAObject.getDebugMode()) {
			System.out.println("OAObjectSerializer.writeObject " + wcnter + ") " + msg);
		}
	}

	/**
	 * Called once the _writeObject has serialized the object, so that any overFlow objects can be serialized within the same wrapper.
	 */
	private void finishWrite(ObjectOutputStream stream) throws IOException {
		if (listOverflow == null) {
			stream.writeBoolean(false);
			return;
		}
		LinkedList<Overflow> listOverflowHold = listOverflow;
		this.listOverflow = null;

		stream.writeBoolean(true);
		stream.writeObject(listOverflowHold);
		int cnt = 0;
		for (Overflow overFlow : listOverflowHold) {
			LOG.finer((++cnt) + ") writing overflow object=" + overFlow.object.getClass().getName());
			OAObjectSerializer wrapper = new OAObjectSerializer(overFlow.object, false); // compress must = false, since it is still using same stream
			wrapper.parentWrapper = this;
			wrapper.stackObject = overFlow.stack;
			wrapper.levelsDeep = overFlow.levelsDeep;
			wrapper.overflowLimit += wrapper.levelsDeep;
			stream.writeObject(wrapper);
		}
	}

	public static boolean bReadId = true; // 20171218, set to false to read older data  (ex: unit test binary file) 

	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		int xDup = OAObjectSerializeDelegate.cntDup;
		int xNew = OAObjectSerializeDelegate.cntNew;
		try {
			_readObject(stream);
		} finally {
			newCount = OAObjectSerializeDelegate.cntNew - xNew;
			dupCount = OAObjectSerializeDelegate.cntDup - xDup;
		}
	}

	public transient int newCount;
	public transient int dupCount;

	/**
	 * Called by objectStream to deserialize a wrapper.
	 */
	private void _readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		long ts = System.currentTimeMillis();

		if (bReadId) {
			this.id = stream.readInt(); // 20171216
		}
		bCompress = stream.readBoolean();
		String msg;
		if (bCompress) {
			Inflater inflater = new Inflater(true);
			InflaterInputStream iis = new InflaterInputStream(stream, inflater, 1024 * 6);

			RemoteObjectInputStream rois;
			if (stream instanceof RemoteObjectInputStream) {
				rois = new RemoteObjectInputStream(iis, (RemoteObjectInputStream) stream);
			} else {
				rois = new RemoteObjectInputStream(iis, null);
			}

			boolean b = rois.readBoolean();
			if (b) {
				object = rois.readObject();
			}
			b = rois.readBoolean();
			if (b) {
				extraObject = rois.readObject();
			}
			finishRead(rois);

			//ois.close();  dont call this, it WILL affect the stream
			// iis.close();// ?? not sure
			totalObjectsWritten = stream.readInt();

			long sizeBefore = inflater.getBytesRead();
			long sizeAfter = inflater.getBytesWritten();

			long ts2 = System.currentTimeMillis();

			msg = String.format("id=%,d, class=%s, extra=%s, compressed=%,d, uncompressed=%,d, totalObjects=%,d, %,dms",
								id, object.getClass().getSimpleName(),
								extraObject == null ? "null" : extraObject.getClass().getSimpleName(),
								sizeBefore, sizeAfter, totalObjectsWritten, (ts2 - ts));
		} else {
			boolean b = stream.readBoolean();
			if (b) {
				object = stream.readObject();
			}
			b = stream.readBoolean();
			if (b) {
				extraObject = stream.readObject();
			}
			finishRead(stream);
			totalObjectsWritten = stream.readInt();

			long ts2 = System.currentTimeMillis();
			msg = String.format("id=%,d, class=%s, extra=%s, totalObjects=%,d, %,dms",
								id, object.getClass().getSimpleName(),
								extraObject == null ? "null" : extraObject.getClass().getSimpleName(),
								totalObjectsWritten, (ts2 - ts));
		}

		if (totalObjectsWritten > 100000 || totalObjectsWritten < 0) {
			msg += " totalObjectsWritten=" + totalObjectsWritten;
			// LOG.warning(rcnter+") "+msg);
		}

		rcnter++;
		LOG.fine(rcnter + ") " + msg);
		OAPerformance.LOG.fine(rcnter + ") " + msg);
		if (OAObject.getDebugMode()) {
			System.out.println("OAObjectSerializer.readObject " + rcnter + ") " + msg);
		}
	}

	/**
	 * Called once readObject has loaded the wrapped object, so that any overflow objects can be included.
	 */
	private void finishRead(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		if (!stream.readBoolean()) {
			return; //
		}

		listOverflow = (LinkedList) stream.readObject();
		int cnt = 0;
		for (Overflow overFlow : listOverflow) {
			OAObjectSerializer wrap = (OAObjectSerializer) stream.readObject();
			LOG.finer((++cnt) + ") read overflow object=" + wrap.object.getClass().getName());
			overFlow.parentObject.setProperty(overFlow.property, wrap.object);
		}
	}

	/**
	 * The object that is being wrapped.
	 */
	public TYPE getObject() {
		Object objx;
		if (parentWrapper != null) {
			objx = parentWrapper.getObject();
		} else {
			objx = object;
		}

		if (objx instanceof IODummy) {
			throw new RuntimeException("Object was not able to be read, class not found");
		}

		return (TYPE) objx;
	}

	public Object getExtraObject() {
		if (parentWrapper != null) {
			return parentWrapper.getExtraObject();
		}
		return extraObject;
	}

	// send an extra object
	public void setExtraObject(Object extraObject) {
		this.extraObject = extraObject;
	}

	/**
	 * Used to dynamically determine which objects are serialized.
	 * 
	 * @param callback
	 */
	public void setCallback(OAObjectSerializerCallback callback) {
		this.callback = callback;
		callback.setOAObjectSerializer(this);
	}

	public static void main(String[] args) throws Exception {
		String s = "com.viaoa.object.OAObjectSerializer";
		Logger log = Logger.getLogger(s);
		log.setLevel(Level.FINER);
		ConsoleHandler ch = new ConsoleHandler();
		ch.setLevel(Level.FINER);
		log.addHandler(ch);

		Object obj = new String("abcedef");
		com.viaoa.object.OAObjectSerializer wrap = new OAObjectSerializer(obj, true, false);

		ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(wrap);
		oos.flush();
		oos.close();

		bos.flush();
		byte[] bs = bos.toByteArray();

		ByteArrayInputStream bis = new ByteArrayInputStream(bs);
		ObjectInputStream ois = new ObjectInputStream(bis);
		Object objx = ois.readObject();
		int xx = 4;
		System.out.println("DONE");

		/*
		Object objz = IncludeProperties.values()[0].ordinal();
		for (IncludeProperties ip : IncludeProperties.values()) {
		}
		*/
	}
}
