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
package com.viaoa.text;

/**
 * Utilities for generating sample text and random strings for demos, tests, and UI placeholders.
 * <p>
 * Responsibilities include:
 * <ul>
 *   <li>{@code getDummyText(..)} — variable-length Lorem ipsum paragraphs with natural word boundaries</li>
 *   <li>{@code getRandomString(..)} — configurable alphanumeric strings with optional first-cap behavior</li>
 *   <li>{@code createDigits(..)} — digit-only strings</li>
 * </ul>
 *
 * <p>The generation routines are intentionally simple and fast (using {@link Math#random()}), and
 * are suitable for non-cryptographic use such as UI scaffolding, test fixtures, and demo data.
 * All methods are null-safe and avoid breaking words when slicing Lorem ipsum samples.</p>
 *
 * <p>Part of the {@code com.viaoa.text} family in OA&nbsp;4.0.</p>
 *
 * @since OA 4.0
 */

public class OATextGenerate {

	/**
	 * Large Lorem Ipsum sample text used as the source material for generating
	 * variable-length dummy paragraphs. Methods such as {@link #getDummyText(int, int, int)}
	 * slice from this text while attempting to maintain word boundaries.
	 */
	public static final String LoremLipsum = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque nec eros pretium, dignissim est sit amet, malesuada augue. Sed pharetra ex ut nulla feugiat laoreet. Nunc finibus malesuada est, et fermentum lorem iaculis eget. Aenean pharetra augue ac elit gravida consectetur. Praesent dapibus sem quis tellus condimentum, eget finibus massa maximus. Quisque tempor a felis in consectetur. Donec a rutrum neque. Nam viverra eros ut arcu interdum facilisis.  "
			+
			"Etiam ultricies nisl id lacus vulputate mattis. Nulla condimentum et metus vitae vestibulum. Aliquam ac risus eros. Vestibulum dignissim bibendum sapien, quis feugiat sapien lacinia nec. Mauris id justo pharetra, tincidunt est vel, varius libero. Ut efficitur nulla nec malesuada efficitur. Nulla luctus purus eu metus feugiat, eu semper metus viverra. Aliquam erat volutpat. Vivamus mollis turpis augue, eget maximus lorem convallis vel. Nam sed arcu vitae diam tempus malesuada id non nisl. Phasellus scelerisque nunc ut dapibus interdum.  "
			+
			"Donec ornare elementum laoreet. Sed diam mauris, eleifend quis lacinia at, egestas eu tellus. Sed neque augue, vestibulum ut arcu non, accumsan aliquet enim. Aliquam fringilla neque a enim pellentesque hendrerit. Sed ac semper arcu, vitae porta purus. Curabitur sit amet faucibus augue. Praesent accumsan elit ut sem dictum vulputate. Praesent sed tempus mauris, ut ultrices dolor. Nunc congue, tortor sed lacinia pulvinar, mauris mi molestie lorem, at rutrum lorem est euismod magna. Suspendisse sagittis mauris in interdum gravida. Phasellus a ante hendrerit, pulvinar urna eget, scelerisque massa.";

	
	/**
	 * Generates a variable-length Lorem Ipsum paragraph. The returned text length
	 * is chosen pseudo-randomly between {@code min} and {@code max}, optionally
	 * biased toward {@code normal}.
	 * <ul>
	 *   <li>Adjusts min/max ranges using {@code normal} heuristics.</li>
	 *   <li>Repeats the Lorem Ipsum sample when required for long outputs.</li>
	 *   <li>Avoids breaking words by locating a space-aligned starting position.</li>
	 * </ul>
	 *
	 * @param normal preferred length; 0 disables biasing
	 * @param min    minimum length of the generated text
	 * @param max    maximum length of the generated text
	 * @return dummy text of variable length with natural word boundaries
	 */
	public static String getDummyText(int normal, int min, int max) {
		// adjust min/max based on normal
		if (normal > 0) {
			if (normal > max) {
				normal = max;
			}
			if (normal > min) {
				int diff = (normal - min);
				if (Math.random() < .75) {
					diff = (int) (diff * .30);
				}
				min = (int) (normal - (Math.random() * diff));
			} else {
				min = normal;
			}

			if (normal < max) {
				int diff = (max - normal);
				if (Math.random() < .9) {
					diff = (int) (diff * .20);
				}
				max = (int) (normal + (Math.random() * diff));
			}
		}
		int sampleSize = min;
		if (min < max) {
			sampleSize += (int) (Math.random() * (max - min));
		}

		StringBuilder sb = new StringBuilder(sampleSize);
		final int maxLipsum = LoremLipsum.length();

		for (; sampleSize > maxLipsum; sampleSize -= maxLipsum) {
			sb.append(LoremLipsum);
			sb.append("  ");
		}

		int beginPos = maxLipsum - sampleSize;
		beginPos = (int) (Math.random() * beginPos);
		for (; beginPos > 0 && LoremLipsum.charAt(beginPos) != ' '; beginPos--) {
			;
		}
		if (beginPos > 0) {
			beginPos++;
		}

		sb.append(LoremLipsum.substring(beginPos, beginPos + sampleSize));
		return sb.toString();
	}

	
	/**
	 * Generates a random alphanumeric string with length chosen randomly between
	 * {@code min} and {@code max}. Delegates to
	 * {@link #getRandomString(int, int, int, boolean, boolean, boolean)} with
	 * {@code normal = 0}, digits enabled, alpha enabled, and first-char capitalization enabled.
	 *
	 * @param min minimum length
	 * @param max maximum length
	 * @return a randomly generated string
	 */
	public static String getRandomString(int min, int max) {
		return getRandomString(0, min, max);
	}

	/**
	 * Generates a random alphanumeric string with length biased toward
	 * {@code normal}. Delegates to the full-parameter implementation with digits
	 * enabled, alpha enabled, and first-char capitalization enabled.
	 *
	 * @param normal preferred length; 0 disables bias adjustment
	 * @param min    minimum allowed length
	 * @param max    maximum allowed length
	 * @return a random string
	 */
	public static String getRandomString(int normal, int min, int max) {
		return getRandomString(normal, min, max, true, true, false);
	}
	
	/**
	 * Generates a random string whose characters are selected from digits and/or
	 * alphabetic characters, depending on flags. Delegates to the full-parameter
	 * {@link #getRandomString(int, int, int, boolean, boolean, boolean)} with
	 * {@code normal = 0}.
	 *
	 * @param min            minimum length
	 * @param max            maximum length
	 * @param bUseDigits     whether digits may appear
	 * @param bUseAlpha      whether alphabetic characters may appear
	 * @param bCapFirstChar  whether the first character is capitalized when alphabetic
	 * @return a random string
	 */
	public static String getRandomString(int min, int max, boolean bUseDigits, boolean bUseAlpha, boolean bCapFirstChar) {
		return getRandomString(0, min, max, bUseDigits, bUseAlpha, bCapFirstChar);
	}

	/**
	 * Generates a digit-only random string. Delegates to
	 * {@link #getRandomString(int, int, boolean, boolean, boolean)} with digits
	 * allowed, alpha disabled, and no first-character capitalization.
	 *
	 * @param min minimum length
	 * @param max maximum length
	 * @return a digit-only random string
	 */
	public static String createDigits(int min, int max) {
		return getRandomString(min, max, true, false, false);
	}

	
	/**
	 * Full-parameter random string generator.
	 * <ul>
	 *   <li>Normalizes invalid min/max inputs.</li>
	 *   <li>If {@code normal > 0}, biases random length toward {@code normal} using
	 *       heuristic contraction of min/max ranges.</li>
	 *   <li>Selects each character as alphabetic or numeric based on flags and a
	 *       random probability.</li>
	 *   <li>Optionally capitalizes the first alphabetic character.</li>
	 *   <li>Avoids ambiguous characters 'O' (replaced with 'P') and 'l' (replaced with 'm').</li>
	 *   <li>Skips '0' and '1' digits when alpha is enabled to avoid confusion.</li>
	 * </ul>
	 *
	 * @param normal         preferred string length; 0 disables bias
	 * @param min            minimum allowed length
	 * @param max            maximum allowed length
	 * @param bUseDigits     whether numeric digits may be used
	 * @param bUseAlpha      whether alphabetic characters may be used
	 * @param bCapFirstChar  whether to capitalize the first alphabetic character
	 * @return generated random string
	 */
	public static String getRandomString(int normal, int min, int max, boolean bUseDigits, boolean bUseAlpha, boolean bCapFirstChar) {
		if (min < 0) min = 0;
		if (max < 0) max = 0;
		else if (max < min) max = min;
		if (normal > 0) {
			if (normal < min) normal = min;
			else if (normal > max) normal = max;
		}
		
		// adjust min/max based on normal
		if (normal > 0) {
			if (normal > min) {
				int diff = (normal - min);
				if (Math.random() < .75) {
					diff = (int) (diff * .30);
				}
				min = (int) (normal - (Math.random() * diff));
			} else {
				min = normal;
			}

			if (normal < max) {
				int diff = (max - normal);
				if (Math.random() < .9) {
					diff = (int) (diff * .20);
				}
				max = (int) (normal + (Math.random() * diff));
			} else {
				max = normal;
			}
		
			if (max < min) {
			    int tmp = min;
			    min = max;
			    max = tmp;
			}		}
		
		int x = min;
		if (min < max) {
			x += (int) (Math.random() * (max - min + 1));
		}

		final StringBuilder sb = new StringBuilder(x);
		for (int i = 0; i < x; i++) {
			char ch;

			boolean bAlpha;
			if (bUseDigits) {
				if (!bUseAlpha) {
					bAlpha = false;
				} else if (i == 0 && bUseAlpha) {
					bAlpha = true;
				} else {
					bAlpha = Math.random() > .5;
				}
			} else {
				bAlpha = true;
			}

			if (bAlpha) {
				ch = (char) (Math.random() * 26);
				if ((i == 0 && bCapFirstChar) || Math.random() > .70) {
					ch += 'A';
					if (ch == 'O') {
						ch = 'P';
					}
				} else {
					ch += 'a';
					if (ch == 'l') {
						ch = 'm';
					}
				}
				sb.append(ch);
			} else {
				ch = (char) (Math.random() * 10);
				ch += '0';
				if (bUseAlpha && ch == '0') {
					ch = '2';
				}
				else if (bUseAlpha && ch == '1') {
					ch = '3';
				}
				sb.append(ch);
			}
		}
		return sb.toString();
	}
}
