package com.viaoa.serialize;

import com.viaoa.object.OAObject;
import com.viaoa.hub.Hub;

public interface OASerializer {
    void writeObject(OAObject obj, OASerializeWriter writer, OASerializeContext context);
    void writeHub(Hub<?> hub, OASerializeWriter writer, OASerializeContext context);
}
