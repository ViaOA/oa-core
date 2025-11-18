package com.viaoa.util;

import org.junit.Test;
import static org.junit.Assert.*;

import com.viaoa.OAUnitTest;

import test.xice.tsac3.model.oa.*;

public class OAIntegerTest extends OAUnitTest {

    @Test
    public void test() {
        
    }

	public static void main(String[] args) throws Exception {
		// findAllServers();
		for (int i = -5; i < 5; i++) {
			String sx = Integer.toBinaryString(i);
			String s = OAInteger.getAsBinary(i);
			System.out.println(i + " " + sx + " " + s);
		}
		int i = 4;
		i++;
	}
    
}
