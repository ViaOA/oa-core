package com.viaoa.oa.api.internal.hubs;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;

import com.viaoa.hub.Hub;

/**
 * Internal serialization hooks used by Hub custom serialization.
 */
public interface HubSerializeOps {

	/**
	 * Writes serialized Hub state.
	 *
	 * @param hub the Hub being serialized
	 * @param stream the object output stream
	 * @throws IOException if stream writing fails
	 */
	public void _writeObject(Hub<?> hub, ObjectOutputStream stream) throws IOException;
	/**
	 * Resolves a Hub after deserialization.
	 *
	 * @param hub the deserialized Hub
	 * @return the resolved Hub
	 * @throws ObjectStreamException if resolution fails
	 */
	public Object _readResolve(Hub<?> hub) throws ObjectStreamException;

}
