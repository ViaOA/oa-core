package com.viaoa.util;

import org.junit.Test;
import static org.junit.Assert.*;

import com.viaoa.OAUnitTest;
import com.viaoa.secure.Base64;

import test.xice.tsac3.model.oa.*;

public class Base64Test extends OAUnitTest {

    @Test
    public void test() {
        
    }

    
    
	public static void main(String[] args) throws Exception {

		String test = "f9nIAAAw";

		byte[] bs = Base64.decode(test.toCharArray());

		int xx = 4;
		xx++;

	}

	public static void mainX(String[] args) {
		String[] ss = { "Vince", "Anthony", "Via" };
		for (int i = 0; i < ss.length; i++) {
			String s = ss[i];
			String s2 = com.viaoa.secure.Base64.encode(s);
			String s3 = com.viaoa.secure.Base64.decode(s2);
			System.out.println(s + " -> " + s2 + " -> " + s3);
		}

	}

}
