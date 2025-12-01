package com.viaoa.object;

import org.junit.Test;
import static org.junit.Assert.*;

import com.viaoa.OAUnitTest;

import test.xice.tsac3.model.oa.*;

public class OAObjectSerializerTest extends OAUnitTest {

    @Test
    public void test() {
        
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
