package com.viaoa.util;

import org.junit.Test;
import static org.junit.Assert.*;

import com.viaoa.OAUnitTest;

import test.xice.tsac3.model.oa.*;

public class OAHtmlTest extends OAUnitTest {

    @Test
    public void test() {
        
    }

    public static void main(String[] args) throws Exception {
        String s = "<html><body><p><i>A</i><div class='joe'>12345<b><i>6789<br>ABCD</b> XYZ</div></body></html>";
        OAHtml h = new OAHtml(s);
        s = h.substring(0, 5);
        System.out.println("==> " + s);
    }
    
}
