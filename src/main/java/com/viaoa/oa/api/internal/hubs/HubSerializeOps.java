package com.viaoa.oa.api.internal.hubs;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;

import com.viaoa.hub.Hub;

public interface HubSerializeOps {

	public void _writeObject(Hub<?> hub, ObjectOutputStream stream) throws IOException;
	public Object _readResolve(Hub<?> hub) throws ObjectStreamException;

}
