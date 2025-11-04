package com.viaoa.text;

public class OATextGenerate {

	public static final String LoremLipsum = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque nec eros pretium, dignissim est sit amet, malesuada augue. Sed pharetra ex ut nulla feugiat laoreet. Nunc finibus malesuada est, et fermentum lorem iaculis eget. Aenean pharetra augue ac elit gravida consectetur. Praesent dapibus sem quis tellus condimentum, eget finibus massa maximus. Quisque tempor a felis in consectetur. Donec a rutrum neque. Nam viverra eros ut arcu interdum facilisis.  "
			+
			"Etiam ultricies nisl id lacus vulputate mattis. Nulla condimentum et metus vitae vestibulum. Aliquam ac risus eros. Vestibulum dignissim bibendum sapien, quis feugiat sapien lacinia nec. Mauris id justo pharetra, tincidunt est vel, varius libero. Ut efficitur nulla nec malesuada efficitur. Nulla luctus purus eu metus feugiat, eu semper metus viverra. Aliquam erat volutpat. Vivamus mollis turpis augue, eget maximus lorem convallis vel. Nam sed arcu vitae diam tempus malesuada id non nisl. Phasellus scelerisque nunc ut dapibus interdum.  "
			+
			"Donec ornare elementum laoreet. Sed diam mauris, eleifend quis lacinia at, egestas eu tellus. Sed neque augue, vestibulum ut arcu non, accumsan aliquet enim. Aliquam fringilla neque a enim pellentesque hendrerit. Sed ac semper arcu, vitae porta purus. Curabitur sit amet faucibus augue. Praesent accumsan elit ut sem dictum vulputate. Praesent sed tempus mauris, ut ultrices dolor. Nunc congue, tortor sed lacinia pulvinar, mauris mi molestie lorem, at rutrum lorem est euismod magna. Suspendisse sagittis mauris in interdum gravida. Phasellus a ante hendrerit, pulvinar urna eget, scelerisque massa.";

	
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

	
	public static String getRandomString(int min, int max) {
		return getRandomString(0, min, max);
	}
	public static String getRandomString(int normal, int min, int max) {
		return getRandomString(normal, min, max, true, true, false);
	}
	
	public static String getRandomString(int min, int max, boolean bUseDigits, boolean bUseAlpha, boolean bCapFirstChar) {
		return getRandomString(0, min, max, true, true, false);
	}

	public static String createDigits(int min, int max) {
		return getRandomString(min, max, true, false, false);
	}

	
	
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
