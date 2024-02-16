package com.viaoa.util;

import java.awt.Color;


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
