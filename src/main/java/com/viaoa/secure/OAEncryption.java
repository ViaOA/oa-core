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
package com.viaoa.secure;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import com.viaoa.lang.OAString;

/*qqqqqqqqqqqqqqqqqq
CODEX


should I create a new com.viaoa.secure package and divide these out?


• Yes. A new com.viaoa.secure package is the right move.

  Keep OAEncryption in com.viaoa.util as a compatibility facade, but move the real contracts into focused secure
  classes. That avoids breaking old OA applications while giving OA 4.0 a clean security model.

  Suggested split:

  - com.viaoa.secure.OAEncryptor
    Authenticated reversible encryption for new data. Versioned output, random IV/nonce, explicit charset.
  - com.viaoa.secure.OALegacyEncryption
    Current DES behavior only. Purpose: decrypt old values and maybe encrypt old-format values only when explicitly
    requested.
  - com.viaoa.secure.OAPasswordHash
    Salted password hashing and verification. This should not expose “decrypt” because password hashes are one-way.
  - com.viaoa.secure.OADigest
    SHA-256/MD5/general digest helpers. MD5 should be marked legacy/checksum-only.
  - com.viaoa.secure.OASecureRandom
    Optional small helper for token bytes, salts, nonces, UUID-like secure values if OA needs those centrally.

  Then com.viaoa.util.OAEncryption becomes deprecated/compatibility-oriented:


 The important design rule: version the stored strings.

  Examples:

  {OAENC1}...   legacy DES, if you decide to mark migrated values
  {OAENC2}...   new authenticated encryption
  {OAPW1}...    password hash format

  For existing raw DES Base64 values, decrypt can try:

  1. Recognized new prefix.
  2. Recognized legacy prefix.
  3. Raw legacy DES fallback for old persisted data.

  That gives you migration without breaking compatibility.

  I would not put this in com.viaoa.util anymore. Security needs stronger naming, stricter contracts, and fewer
  “helper method” ambiguities. com.viaoa.secure tells future developers: this is not formatting, not encoding, not
  string utility work; these APIs have security invariants.








OAEncryption is not just “needs tests” cleanup. It needs a clear contract split before fixes happen.

  The safest direction is to separate the meanings:

  - legacyEncrypt / legacyDecrypt: current DES behavior, explicitly compatibility-only for old persisted values.
  - encrypt / decrypt: new versioned authenticated encryption format.
  - hash: general digest/checksum use, not passwords.
  - hashPassword / verifyPassword: salted password KDF.
  - getMD5Hash: legacy checksum only, fixed padding/null behavior.

  The key cleanup is semantic: stop one class/method family from implying that DES obfuscation, SHA digesting, MD5
  checksums, reversible secret encryption, and password storage are all equivalent “encryption.”

  For OA 4.0, I’d treat this as a small security subsystem with backward compatibility gates:

  1. Preserve old DES decrypt so existing data can still load.
  2. Make new encryption produce a versioned value, for example {OAENC2}....
  3. Make password hashing produce a different versioned value, for example {OAPW1}....
  4. Keep old SHA/MD5 comparison paths only for migration/legacy verification.
  5. Add tests that prove old ciphertext still decrypts, new ciphertext is randomized, tampering fails, password
     hashes use salt, and null/charset behavior is deterministic.

  That gives OA a clean invariant: encoding is not encryption, encryption is not password hashing, and legacy
  compatibility is explicit instead of hidden.




  - Method: encrypt(byte[]), decrypt(byte[]), encrypt(byte[], String), decrypt(byte[], String), getCipher()
  - Issue: Uses Cipher.getInstance("DES").
  - Why it is a problem: In standard JCE providers, "DES" resolves to DES/ECB/PKCS5Padding. DES is
    cryptographically broken, has a 56-bit key, and ECB is deterministic and leaks repeated plaintext block
    patterns.
  - Classification: CODEX/FIXNOW
  CODEX/FIXNOW: DES/ECB is not security-grade encryption; keep only for legacy compatibility and route new
    encrypted data to an authenticated modern format.

 
Method: encrypt(byte[]), encrypt(byte[], String)
  - Issue: Encryption is deterministic; no random IV/nonce is used or stored.
  - Why it is a problem: The same plaintext with the same key always produces the same ciphertext. This leaks
    equality and enables pattern analysis for persisted values, tokens, and password-like fields.
  - Classification: CODEX/FIXNOW
  - Suggested Java comment to add:
  CODEX/FIXNOW: encryption is deterministic with no IV/nonce; new encrypted values need randomized
    authenticated encryption.
 
 - Method: encrypt(...) / decrypt(...)
  - Issue: Ciphertext is unauthenticated.
  - Why it is a problem: There is no MAC or authenticated mode, so tampering is not reliably detected. Modified
    ciphertext can produce corrupted plaintext or padding exceptions without a clear integrity contract.
  - Classification: CODEX/FIXNOW
  - Suggested Java comment to add:
    // CODEX/FIXNOW: ciphertext has no authentication/integrity check; decrypt must distinguish tamper/corruption
    from valid plaintext.
 
  - Class: OAEncryption
  - Method: getSecretKey()
  - Issue: The default key is hard-coded as bytes 0..7.
  - Why it is a problem: Anyone with the OA code can decrypt values encrypted with the default key. This is
    obfuscation, not secret encryption, yet the API name says encryption.
  - Classification: CODEX/FIXNOW
  - Suggested Java comment to add:
    // CODEX/FIXNOW: default key is public/static and only supports legacy obfuscation; do not use for protecting
    secrets.
  - Class: OAEncryption
  - Method: getSecretKey(String)
  - Issue: Password-derived DES key uses only the first 8 Java char values, cast directly to bytes.
  - Why it is a problem: Passwords sharing the same first 8 low bytes produce the same key. Characters above 255
    are truncated, and there is no charset-defined byte encoding.
  - Classification: CODEX/FIXNOW
  - Suggested Java comment to add:
    // CODEX/FIXNOW: password key derivation truncates to 8 chars/bytes with no charset; distinct passwords can map
    to the same DES key.
  - Class: OAEncryption
  - Method: getSecretKey(String)
  - Issue: No salt or iteration count is used for password-based key derivation.
  - Why it is a problem: Password-derived keys are fast to brute-force and identical passwords always produce
    identical keys across all OA deployments.
  - Classification: CODEX/FIXNOW
  - Suggested Java comment to add:
    // CODEX/FIXNOW: password-derived keys need per-value salt and KDF iterations for any security-sensitive use.
  - Class: OAEncryption
  - Method: encrypt(byte[], String), decrypt(byte[], String)
  - Issue: Empty or blank password silently falls back to the hard-coded default key.
  - Why it is a problem: A caller intending password-based encryption can accidentally get globally decryptable
    default-key encryption.
  - Classification: CODEX/CONTRACT
  - Suggested Java comment to add:
    // CODEX/CONTRACT: empty password falls back to default key; confirm this legacy behavior before preserving it
    in OA 4.0.
  - Class: OAEncryption
  - Method: encrypt(String), encrypt(String, String), decrypt(String), decrypt(String, String)
  - Issue: String encryption/decryption uses platform default charset.
  - Why it is a problem: input.getBytes() and new String(bs) can produce different encrypted bytes or decoded
    plaintext on systems with different default charsets.
  - Classification: CODEX/FIXNOW
  - Suggested Java comment to add:
    // CODEX/FIXNOW: String encryption/decryption uses default charset; persisted encrypted text needs explicit
    UTF-8 or legacy format tagging.
  - Class: OAEncryption
  - Method: decrypt(String, String)
  - Issue: Does not handle null input, unlike decrypt(String).
  - Why it is a problem: The overloads have inconsistent null contracts. Valid callers using the password overload
    can get NullPointerException where the default overload returns null.
  - Classification: CODEX/FIXNOW
  - Suggested Java comment to add:
    // CODEX/FIXNOW: null handling differs from decrypt(String); define and enforce one decrypt null contract.
  - Class: OAEncryption
  - Method: getHash(String) / getSHAHash(String)
  - Issue: Unsalted fast SHA-256 is used for password-style storage paths.
  - Why it is a problem: UI controllers use SHA hashing for password conversions. Fast unsalted hashes are
    vulnerable to offline dictionary/rainbow-table attacks.
  - Classification: CODEX/FIXNOW
  - Suggested Java comment to add:
    // CODEX/FIXNOW: SHA-256 without per-password salt/KDF is not password hashing; keep only for legacy hashes or
    non-password digests.
  - Class: OAEncryption
  - Method: getHash(String)
  - Issue: Failure is returned as null and errors are printed to stdout.
  - Why it is a problem: Hash failure is indistinguishable from null input, and stdout logging is not appropriate
    for security-sensitive failure paths.
  - Classification: CODEX/DEFER
  - Suggested Java comment to add:
    // CODEX/DEFER: hash failures return null/print to stdout; distinguish invalid input from crypto/provider
    failure.
  - Class: OAEncryption
  - Method: getMD5Hash(String)
  - Issue: MD5 is exposed as a hash helper.
  - Why it is a problem: MD5 is collision-broken and unsafe for password or integrity-sensitive use. The method
    name does not constrain it to legacy checksums.
  - Classification: CODEX/CONTRACT
  - Suggested Java comment to add:
    // CODEX/CONTRACT: MD5 is legacy/checksum-only; confirm no password/security-sensitive callers before
    preserving this API.
  - Class: OAEncryption
  - Method: getMD5Hash(String)
  - Issue: Hex output is not always padded to 32 characters.
  - Why it is a problem: The code only pads when length is exactly 31. Digests with two or more leading zero
    nibbles return shorter strings, breaking fixed-length MD5 storage/comparison contracts.
  - Classification: CODEX/FIXNOW
  - Suggested Java comment to add:
    // CODEX/FIXNOW: MD5 hex must left-pad to 32 chars, not only when length == 31.
  - Class: OAEncryption
  - Method: getMD5Hash(String)
  - Issue: null input is swallowed and hashed as the empty digest path.
  - Why it is a problem: input.getBytes() throws, the catch only prints, and md.digest() then returns the MD5 of no
    input. That makes getMD5Hash(null) look like a valid hash result.
  - Classification: CODEX/FIXNOW
  - Suggested Java comment to add:
    // CODEX/FIXNOW: null/encoding failure must not silently return the MD5 of an empty update.
  - Class: OAEncryption
  - Method: encrypt(String) / UI conversion 'E'
  - Issue: Reversible encryption is wired into password/encrypted field handling.
  - Why it is a problem: UI comments describe encrypted password handling, and controllers call
    OAEncryption.encrypt(text). If used for passwords, this stores recoverable secrets with the hard-coded DES key.
  - Classification: CODEX/FIXNOW
  - Suggested Java comment to add:
    // CODEX/FIXNOW: reversible default-key encryption must not be used for passwords; restrict to legacy encrypted
    fields and prefer password hashes for credentials.
  - Class: OAEncryption
  - Method: Encrypted string format
  - Issue: Ciphertext has no version/algorithm marker.
  - Why it is a problem: Legacy DES output and any future AES/KDF format cannot be distinguished from the stored
    value alone. Migration/decrypt fallback will be ambiguous and fragile.
  - Classification: CODEX/DEFER
  - Suggested Java comment to add:
    // CODEX/DEFER: encrypted string format has no version/algorithm marker; add one before introducing modern
    encryption.


 
 */

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
