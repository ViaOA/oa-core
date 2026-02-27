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

import java.awt.Color;


/**
 * 
 * qqqqqqq String helpers for working on programming code
 * 
 */
public class OATextCode {


	public static String getPropertyName(String s) {
		return getPropertyName(s, true);
	}
	
	public static String getPropertyName(String s, boolean bToLower) {
		boolean b = true;
		if (s.startsWith("get")) {
			s = s.substring(3);
		} else if (s.startsWith("is")) {
			s = s.substring(2);
		} else if (s.startsWith("has")) {
			s = s.substring(3);
		} else if (s.startsWith("set")) {
			s = s.substring(3);
		} else {
			b = false;
		}
		if (bToLower && b && s.length() > 1) {
			s = Character.toLowerCase(s.charAt(0)) + s.substring(1);
		}
		return s;
	}


}


