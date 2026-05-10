package com.viaoa.serialize;

import com.viaoa.object.OAObject;
import com.viaoa.hub.Hub;

public interface OADeserializer {
	<T extends OAObject> T readObject(Class<T> type, OASerializeReader reader, OASerializeContext context);

	<T extends OAObject> Hub<T> readHub(Class<T> type, OASerializeReader reader, OASerializeContext context);
}