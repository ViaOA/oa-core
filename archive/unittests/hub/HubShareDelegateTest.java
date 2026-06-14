package com.viaoa.hub;

import org.junit.Test;
import static org.junit.Assert.*;

import com.viaoa.OAUnitTest;

import test.xice.tsac3.model.oa.*;

public class HubShareDelegateTest extends OAUnitTest {

    @Test
    public void test() {

        
        
    }
    
	public static void main(String[] args) {
		Hub<String> h = new Hub<String>(String.class);
		for (int i = 0; i < 1000; i++) {
			Hub<String> hx = new Hub<String>(String.class);
			hx.setSharedHub(h);
			System.gc();
		}
		for (int i = 0; i < 100; i++) {
			System.gc();
		}
		System.out.println("Done");
	}
}
