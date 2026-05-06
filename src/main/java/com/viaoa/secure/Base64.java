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

/**
 * Provides Base64 encoding and decoding utilities for raw bytes and Strings.
 * <p>
 * Encoding converts groups of three bytes into four Base64 characters using a
 * fixed 64-character alphabet. Decoding reverses the process and restores the
 * original byte array. Padding is handled using the '=' character. <p>
 *
 * This implementation is self-contained and does not rely on
 * {@link java.util.Base64}. The methods are stateless and thread-safe. When
 * encoding a String, the platform's default character encoding is used for
 * converting the String to bytes. <p>
 *
 * Decoding tolerates non-Base64 characters by skipping them and reconstructs
 * bytes using a bit accumulator. The output length is verified to ensure the
 * encoded data is well-formed.
 */
public class Base64 {

	/**
	 * Encodes a {@link String} using Base64 encoding.
	 * <p>
	 * The input string is converted to a byte array using the platform’s
	 * default character encoding, then encoded using Base64 rules.
	 * A {@code null} input returns {@code null}.
	 *
	 * @param str the string to encode
	 * @return {@code null} if {@code str} is {@code null}, otherwise a Base64-encoded string
	 */
	static public String encode(String str) {
		if (str == null) {
			return null;
		}
		return new String(encode(str.getBytes()));
	}

	/**
	 * Encodes a byte array using Base64 encoding.
	 * <p>
	 * The input data is processed in 3-byte groups and converted into
	 * 4-character Base64 blocks. Padding characters ('=') are added
	 * as required to ensure the output length is a multiple of four.
	 *
	 * @param data the byte array to encode
	 * @return a character array containing the Base64-encoded representation
	 */
	static public char[] encode(byte[] data) {
		char[] out = new char[((data.length + 2) / 3) * 4];

		//
		// 3 bytes encode to 4 chars.  Output is always an even
		// multiple of 4 characters.
		//
		for (int i = 0, index = 0; i < data.length; i += 3, index += 4) {
			boolean quad = false;
			boolean trip = false;

			int val = (0xFF & (int) data[i]);
			val <<= 8;
			if ((i + 1) < data.length) {
				val |= (0xFF & (int) data[i + 1]);
				trip = true;
			}
			val <<= 8;
			if ((i + 2) < data.length) {
				val |= (0xFF & (int) data[i + 2]);
				quad = true;
			}
			out[index + 3] = alphabet[(quad ? (val & 0x3F) : 64)];
			val >>= 6;
			out[index + 2] = alphabet[(trip ? (val & 0x3F) : 64)];
			val >>= 6;
			out[index + 1] = alphabet[val & 0x3F];
			val >>= 6;
			out[index + 0] = alphabet[val & 0x3F];
		}
		return out;
	}

	/**
	 * Decodes a Base64-encoded {@link String} back into its original form.
	 * <p>
	 * The input string is first converted to a character array and then
	 * decoded into raw bytes using Base64 decoding rules. A {@code null}
	 * input returns {@code null}.
	 *
	 * @param s the Base64-encoded string to decode
	 * @return {@code null} if {@code s} is {@code null}, otherwise the decoded string
	 */
	static public String decode(String s) {
		if (s == null) {
			return null;
		}
		char[] c = new char[s.length()];
		s.getChars(0, s.length(), c, 0);

		return new String(decode(c));
	}

	/**
	 * Decodes a Base64-encoded character array into its original byte array.
	 * <p>
	 * Non-Base64 characters are ignored. Padding characters are handled
	 * according to Base64 specifications. The decoded byte count is verified
	 * to ensure the encoded input is well-formed.
	 *
	 * @param data the Base64-encoded character array
	 * @return the decoded byte array
	 * @throws Error if the calculated output length does not match the decoded length
	 */
	static public byte[] decode(char[] data) {
		int len = ((data.length + 3) / 4) * 3;
		if (data.length > 0 && data[data.length - 1] == '=') {
			--len;
		}
		if (data.length > 1 && data[data.length - 2] == '=') {
			--len;
		}
		byte[] out = new byte[len];

		int shift = 0; // # of excess bits stored in accum
		int accum = 0; // excess bits
		int index = 0;

		for (int ix = 0; ix < data.length; ix++) {
			int value = codes[data[ix] & 0xFF]; // ignore high byte of char
			if (value >= 0) { // skip over non-code
				accum <<= 6; // bits shift up by 6 each time thru
				shift += 6; // loop, with new bits being put in
				accum |= value; // at the bottom.
				if (shift >= 8) { // whenever there are 8 or more shifted in,
					shift -= 8; // write them out (from the top, leaving any
					out[index++] = // excess at the bottom for next iteration.
							(byte) ((accum >> shift) & 0xff);
				}
			}
		}
		if (index != out.length) {
			throw new Error("miscalculated data length!");
		}

		return out;
	}

	/**
	 * Base64 alphabet used for encoding values in the range 0–63.
	 * <p>
	 * This character set follows the standard Base64 specification
	 * and is used to map 6-bit values to encoded characters.
	 */
	static private char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=".toCharArray();
	
	/**
	 * Lookup table for decoding Base64 characters into their numeric values.
	 * <p>
	 * Each index corresponds to a character’s unsigned byte value.
	 * Entries contain values in the range 0–63 for valid Base64 characters
	 * or {@code -1} for invalid characters.
	 */
	static private byte[] codes = new byte[256];
	
	/**
	 * Static initializer that populates the Base64 decoding lookup table.
	 * <p>
	 * This initializes mappings for uppercase letters, lowercase letters,
	 * digits, and the '+' and '/' characters, marking all other entries
	 * as invalid.
	 */
	static {
		for (int i = 0; i < 256; i++) {
			codes[i] = -1;
		}
		for (int i = 'A'; i <= 'Z'; i++) {
			codes[i] = (byte) (i - 'A');
		}
		for (int i = 'a'; i <= 'z'; i++) {
			codes[i] = (byte) (26 + i - 'a');
		}
		for (int i = '0'; i <= '9'; i++) {
			codes[i] = (byte) (52 + i - '0');
		}
		codes['+'] = 62;
		codes['/'] = 63;
	}


}
