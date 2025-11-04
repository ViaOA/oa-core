package com.viaoa.util;

import org.junit.Test;
import static org.junit.Assert.*;

import com.viaoa.OAUnitTest;

import test.xice.tsac3.model.oa.*;

public class OAEncryptionTest extends OAUnitTest {

    @Test
    public void test() {
        
    }
    
}


/*


	public static void main(String[] args) throws Exception {
		String s = "";

		String pw = getHash("00001201");
		String pw2 = getMD5Hash("00001201");
		String pw3 = OAEncryption.encrypt("00001201");

		String smd = getMD5Hash("emp3364");

		smd = getMD5Hash("vince");

		for (int i = 0; args != null && i < args.length; i++) {
			s += args[i];
		}

		System.out.println("Original \"" + s + "\"");

		String s2 = encrypt(s, "password");
		System.out.println("Encrypted ==> \"" + s2 + "\"");

		String s3 = decrypt(s2, "password");
		System.out.println("Decrypted ==> \"" + s3 + "\"");

		String s4 = getHash(s);
		System.out.println("Hashed ==> \"" + s4 + "\"");
	}


*/

/*
Sun:
http://java.sun.com/javase/6/docs/technotes/guides/security/crypto/CryptoSpec.html

Sample programs:
http://www.owasp.org/index.php/Digital_Signature_Implementation_in_Java
http://www.rgagnon.com/javadetails/java-0400.html

*/



