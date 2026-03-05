package com.viaoa.util;

import org.junit.Test;
import static org.junit.Assert.*;

import com.viaoa.OAUnitTest;

import test.xice.tsac3.model.oa.*;

public class ImageResizerTest extends OAUnitTest {

    @Test
    public void test() {
        
    }

    public static void main(String args[]) {
//        if (args.length != 3) {ImageResizer.usage();}
        double factor = Double.parseDouble(args[2]);
        ImageResizer resizer = new ImageResizer();
        resizer.doResize(args[0], args[1], factor);
        System.exit(0);
    }
    
}
