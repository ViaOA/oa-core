package com.viaoa.util;

import org.junit.Test;
import static org.junit.Assert.*;

import com.viaoa.OAUnitTest;

import test.xice.tsac3.model.oa.*;

public class OAClassLoaderTest extends OAUnitTest {

    @Test
    public void test() {
        
    }
    
}

/*

// test using Jar, or directory
public static void main(String[] args) throws Exception {

	String cname = "com.viaoa.util.Test";

	OAClassLoader test = new OAClassLoader(cname);
	Class c = test.loadClass(cname);
	TestInterface t = (TestInterface) c.newInstance();
	t.test();
	System.out.println("Done");
}


*/




