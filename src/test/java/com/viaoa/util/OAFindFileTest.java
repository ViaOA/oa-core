package com.viaoa.util;

import org.junit.Test;
import static org.junit.Assert.*;

import com.viaoa.OAUnitTest;

import test.xice.tsac3.model.oa.*;

public class OAFindFileTest extends OAUnitTest {

    @Test
    public void test() {
        
    }

	public static void main(String[] args) throws Exception {
		if (args == null || args.length == 0) {
			System.out.println("Usage: FindFile [fromDirectory|File] SearchFileName");
		}
		else {
			String s1;
			String s2;
			if (args.length == 1) {
				s1 = ".";
				s2 = args[0];
			}
			else {
				s1 = args[0];
				s2 = args[1];
			}
			
			OAFindFile ff = new OAFindFile();
			String[] fileNames = ff.findAll(s1, s2);
			
			for (int i=0; fileNames != null && i < fileNames.length; i++) {
				System.out.println((i+1)+") " + fileNames[i]);
			}
			System.out.println("FindFile done for " + s2+", " + (fileNames.length) + " found");
		}
	}
    
}
