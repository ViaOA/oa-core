package com.viaoa.oa.api.internal.objects;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;

import com.viaoa.object.OAObject;

public interface OAObjectSerializeOps {

	public void readObject(OAObject oaObj, ObjectInputStream in) throws IOException, ClassNotFoundException;
	public Object readResolve(OAObject oaObj) throws ObjectStreamException;
	public void writeObject(OAObject oaObj, ObjectOutputStream stream) throws IOException;
	
}
