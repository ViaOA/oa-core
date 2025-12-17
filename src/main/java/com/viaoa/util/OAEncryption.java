/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viaoa.util;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

/**
 * Basic hashing and symmetric encryption utilities used by OA. This class
 * provides SHA-256 hashing as well as DES-based encryption compatible with
 * legacy storage formats.
 *
 * <p><b>Important:</b> The DES algorithms and key generation logic used here
 * exist for backward compatibility and are not recommended for new secure
 * applications. A more modern, secure implementation will be introduced in
 * a future OA release.</p>
 *
 * <p>Primary Use Cases:</p>
 * <ul>
 *   <li>Hashing values for comparison without storing plaintext</li>
 *   <li>Simple reversible encryption for legacy data (non-sensitive)</li>
 *   <li>Compatibility with existing OA deployments</li>
 * </ul>
 *
 * <p>Future releases will provide AES-based encryption and salted password
 * hashing mechanisms while preserving backward compatibility.</p>
 */
public class OAEncryption {

	/*
	 * Generates a SHA-256 hash code base64 string for a given input. This is a one way function (irreversible). Example: used when the real
	 * password is not stored. Instead the hash is stored and is used to compare the hash of user input.
	 * 
	 * @param input the input value to hash
	 * @return the SHA-256 hash string, or null if input is null
	 */
	public static String getSHAHash(String input) {
		return getHash(input);
	}

	/**
	 * Computes a SHA-256 hash string for the given input.
	 *
	 * @param input the input value to hash
	 * @return the hash string, or null if input is null or hashing fails
	 */
	public static String getHash(String input) {
		if (input == null) {
			return null;
		}
		MessageDigest md = null;

		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			System.out.println("No SHA-256");
			return null;
		}
		try {
			md.update(input.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			System.out.println("Encoding error.");
		}

		byte raw[] = md.digest();
		String hash = new String(Base64.encode(raw));

		return hash;
	}

	/*
	 * Encrypt bytes into a new byte array.
	 * 
	 * @see #decrypt(byte[])
	 */
	/**
	 * Encrypts the given byte array using the default secret key.
	 *
	 * @param bs the byte array to encrypt
	 * @return the encrypted byte array
	 * @throws Exception if encryption fails
	 */
	public static byte[] encrypt(byte[] bs) throws Exception {
		Cipher cipher = Cipher.getInstance("DES");
		cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());

		bs = cipher.doFinal(bs);
		return bs;
	}

	/**
	 * Encrypts the given byte array using a password-based or default secret key.
	 *
	 * @param bs the byte array to encrypt
	 * @param password the password used to derive the secret key
	 * @return the encrypted byte array
	 * @throws Exception if encryption fails
	 */
	public static byte[] encrypt(byte[] bs, String password) throws Exception {
		Cipher cipher = Cipher.getInstance("DES");

		SecretKey key;
		if (OAString.isEmpty(password)) {
			key = getSecretKey();
		} else {
			key = getSecretKey(password);
		}

		cipher.init(Cipher.ENCRYPT_MODE, key);

		bs = cipher.doFinal(bs);
		return bs;
	}

	/**
	 * Creates and initializes a DES cipher for encryption using the default secret key.
	 *
	 * @return an initialized Cipher instance
	 * @throws Exception if cipher initialization fails
	 */
	public static Cipher getCipher() throws Exception {
		Cipher cipher = Cipher.getInstance("DES");
		cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());
		return cipher;
	}

	/**
	 * Decrypts the given byte array using the default secret key.
	 *
	 * @param bs the byte array to decrypt
	 * @return the decrypted byte array
	 * @throws Exception if decryption fails
	 */
	public static byte[] decrypt(byte[] bs) throws Exception {
		Cipher cipher = Cipher.getInstance("DES");
		cipher.init(Cipher.DECRYPT_MODE, getSecretKey());
		bs = cipher.doFinal(bs);
		return bs;
	}

	/**
	 * Decrypts the given byte array using a password-based or default secret key.
	 *
	 * @param bs the byte array to decrypt
	 * @param password the password used to derive the secret key
	 * @return the decrypted byte array
	 * @throws Exception if decryption fails
	 */
	public static byte[] decrypt(byte[] bs, String password) throws Exception {
		Cipher cipher = Cipher.getInstance("DES");

		SecretKey key;
		if (OAString.isEmpty(password)) {
			key = getSecretKey();
		} else {
			key = getSecretKey(password);
		}

		cipher.init(Cipher.DECRYPT_MODE, key);
		bs = cipher.doFinal(bs);
		return bs;
	}

	/**
	 * Cached default secret key used for encryption and decryption.
	 */
	private static SecretKey _secretKey;

	/**
	 * Returns the default DES secret key, creating it if necessary.
	 *
	 * @return the default secret key
	 * @throws Exception if key generation fails
	 */
	public static SecretKey getSecretKey() throws Exception {
		if (_secretKey == null) {
			byte[] bs = new byte[DESKeySpec.DES_KEY_LEN];
			for (int i = 0; i < bs.length; i++) {
				bs[i] = (byte) i;
			}
			DESKeySpec desKeySpec = new DESKeySpec(bs);
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
			_secretKey = keyFactory.generateSecret(desKeySpec);
		}
		return _secretKey;
	}

	/**
	 * Creates and returns a DES secret key derived from the given password.
	 *
	 * @param password the password used to derive the key
	 * @return the generated secret key
	 * @throws Exception if key generation fails
	 */
	public static SecretKey getSecretKey(String password) throws Exception {
		byte[] bs = new byte[DESKeySpec.DES_KEY_LEN];

		int x = password == null ? 0 : password.length();

		for (int i = 0; i < bs.length; i++) {
			if (i < x) {
				bs[i] = (byte) password.charAt(i);
			} else {
				bs[i] = (byte) i;
			}
		}
		DESKeySpec desKeySpec = new DESKeySpec(bs);
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
		SecretKey skey = keyFactory.generateSecret(desKeySpec);

		return skey;
	}

	/**
	 * Encrypts the given string and encodes the result as a Base64 string.
	 *
	 * @param input the string to encrypt
	 * @return the encrypted Base64 string
	 * @throws Exception if encryption fails
	 */
	public static String encrypt(String input) throws Exception {
		byte[] bs = encrypt(input.getBytes());
		char[] cs = Base64.encode(bs);
		String s = new String(cs);
		return s;
	}

	/**
	 * Encrypts the given string using a password-based or default secret key
	 * and encodes the result as a Base64 string.
	 *
	 * @param input the string to encrypt
	 * @param password the password used to derive the secret key
	 * @return the encrypted Base64 string
	 * @throws Exception if encryption fails
	 */
	public static String encrypt(String input, String password) throws Exception {
		byte[] bs = encrypt(input.getBytes(), password);
		char[] cs = Base64.encode(bs);
		String s = new String(cs);
		return s;
	}

	/**
	 * Decrypts a Base64-encoded string using the default secret key.
	 *
	 * @param inputBase64 the Base64-encoded string to decrypt
	 * @return the decrypted string, or null if inputBase64 is null
	 * @throws Exception if decryption fails
	 */
	public static String decrypt(String inputBase64) throws Exception {
		if (inputBase64 == null) {
			return null;
		}
		char[] cs = new char[inputBase64.length()];
		inputBase64.getChars(0, inputBase64.length(), cs, 0);

		byte[] bs = Base64.decode(cs);

		bs = decrypt(bs);

		return new String(bs);

	}

	/**
	 * Decrypts a Base64-encoded string using a password-based or default secret key.
	 *
	 * @param inputBase64 the Base64-encoded string to decrypt
	 * @param password the password used to derive the secret key
	 * @return the decrypted string
	 * @throws Exception if decryption fails
	 */
	public static String decrypt(String inputBase64, String password) throws Exception {
		char[] cs = new char[inputBase64.length()];
		inputBase64.getChars(0, inputBase64.length(), cs, 0);

		byte[] bs = Base64.decode(cs);

		bs = decrypt(bs, password);

		return new String(bs);
	}

	/**
	 * Generates an MD5 hash string for the given input.
	 *
	 * @param input the input value to hash
	 * @return the MD5 hash string
	 */
	public static String getMD5Hash(String input) {
		MessageDigest md = null;

		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("No MD5 available");
		}

		try {
			md.update(input.getBytes(), 0, input.length());
		} catch (Exception e) {
			System.out.println("Encoding error.");
		}

		byte raw[] = md.digest();

		String hash = new BigInteger(1, raw).toString(16);

		if (hash.length() == 31) {
			hash = "0" + hash;
		}
		return hash;
	}

	/**
	 * Returns a UUID string created from the given value.
	 *
	 * @param value the UUID string value
	 * @return the normalized UUID string
	 */
    public static String getUUID(String value) {
        String s = UUID.fromString(value).toString();
        return s;
    }

    /**
     * Generates and returns a random UUID string.
     *
     * @return a randomly generated UUID string
     */
    public static String getUUID() {
        String s = UUID.randomUUID().toString();
        return s;
    }
}
