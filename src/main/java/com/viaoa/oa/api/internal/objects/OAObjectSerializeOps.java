package com.viaoa.oa.api.internal.objects;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;

import com.viaoa.object.OAObject;

/**
 * Internal serialization hooks used by OAObject custom serialization.
 */
public interface OAObjectSerializeOps {

	/**
	 * Reads serialized OAObject state.
	 *
	 * @param oaObj the object being deserialized
	 * @param in the object input stream
	 * @throws IOException if stream reading fails
	 * @throws ClassNotFoundException if a serialized class cannot be resolved
	 */
	public void readObject(OAObject oaObj, ObjectInputStream in) throws IOException, ClassNotFoundException;
	/**
	 * Resolves an OAObject after deserialization.
	 *
	 * @param oaObj the deserialized object
	 * @return the resolved object
	 * @throws ObjectStreamException if resolution fails
	 */
	public Object readResolve(OAObject oaObj) throws ObjectStreamException;
	/**
	 * Writes serialized OAObject state.
	 *
	 * @param oaObj the object being serialized
	 * @param stream the object output stream
	 * @throws IOException if stream writing fails
	 */
	public void writeObject(OAObject oaObj, ObjectOutputStream stream) throws IOException;
	
}
