/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
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

import java.awt.Color;

/**
 * Utility methods for selecting a readable foreground color (black or white)
 * based on the luminance of a background color. The methods compute the
 * perceived brightness of the background using Rec. 709 luminance weights and
 * return white for dark backgrounds and black for light backgrounds. <p>
 *
 * The utility accepts either a {@link Color} instance or raw RGB component
 * values and is useful when rendering text or icons over arbitrary background
 * colors to maintain sufficient contrast.
 */
public class OAColor {

    public static Color getForeground(Color backgroundColor) {
        if (backgroundColor == null) return Color.white;
        return getForegroundColor(backgroundColor.getRed(), backgroundColor.getGreen(), backgroundColor.getBlue());
    }
    
    public static Color getForegroundColor(Color backgroundColor) {
        if (backgroundColor == null) return Color.white;
        return getForegroundColor(backgroundColor.getRed(), backgroundColor.getGreen(), backgroundColor.getBlue());
    }

    /**
     * Get best foreground color to use with the background color r,g,b value
     */
    public static Color getForegroundColor(int r, int g, int b) {
        float f = (0.2126f*r) + (0.7152f*g) + (0.0722f*b);
        int x = (f < 140) ? (0 | 0xFFFFFF) : 0;  // pick black or white color
        return new Color(x);
    }
    
}
