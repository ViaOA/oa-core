package com.viaoa.util;


import static org.junit.Assert.*;
import org.junit.Test;

import com.viaoa.OAUnitTest;

public class OAStringTest extends OAUnitTest {

    
    @Test 
    public void dcountTest() {
        String s = "A.b.c.d";
        assertEquals(OAString.dcount(s, '.'), 4);
        
        s = "A..c.d";
        assertEquals(OAString.dcount(s, '.'), 4);

        s = null;
        assertEquals(OAString.dcount(s, '.'), 0);

        s = "A.b.   .d";
        assertEquals(OAString.dcount(s, '.'), 4);
        
        s = "A.b:.'.c.d";
        assertEquals(OAString.dcount(s, '.'), 5);
        
        s = ".";
        assertEquals(OAString.dcount(s, '.'), 2);
        
        s = "1.2.3.4";
        s = OAString.field(s, ".", OAString.dcount(s, '.'));
        assertEquals(s, "4");

        s = "1.2.3.4";
        s = OAString.field(s, ".", OAString.dcount(s, '.'));
        assertEquals(s, "4");

        s = "1.2.3.4";
        s = OAString.field(s, ".", 1, OAString.dcount(s, '.')-1);
        assertEquals(s, "1.2.3");
    }
    @Test 
    public void fieldTest() {
        String s = "A.b.c.d";
        assertEquals(OAString.field(s, '.', OAString.dcount(s, '.')), "d");
        assertEquals(OAString.field(s, '.', 0), null);  // first field is 1, not 0.  0 will always return null

        s = "A.b.c.d";
        assertEquals(OAString.field(s, '.', 9), null);

        s = "A.b..d";
        assertEquals(OAString.field(s, '.', 3), "");

        s = "A.b..d";
        assertEquals(OAString.field(s, '.', -1), null);
        
        s = "A.b..d";
        assertEquals(OAString.field(s, '.', 3, 2), ".d");
        assertEquals(OAString.field(s, '.', 2, 3), "b..d");
        assertEquals(OAString.field(s, '.', 1, 99), s);
    }
    
    
    
    @Test
    public void formatTest() {
        String s = OAString.format("1234.56", "  R4,");
        assertEquals(s, "1,234.5600  ");

        s = OAString.format("1234.56", "4L");
        assertEquals(s, "1234");
    }
    
    @Test
    public void trimTest() {
        String s = OAString.trim(" a b    c  ");
        assertEquals(s, "a b c");
    }
    
    @Test
    public void convertTest() {
        String s = "abAcdEfA";
        s = OAString.convert(s, "A", "X");
        assertEquals(s, "abXcdEfX");
        s = OAString.convert(s, "X", "bb");
        assertEquals(s, "abbbcdEfbb");
        s = OAString.convert(s, "X", "bb");
        assertEquals(s, "abbbcdEfbb");
        s = OAString.convert(s, "bb", "b");
        assertEquals(s, "abbcdEfb");
        s = OAString.convert(s, "b", "");
        assertEquals(s, "acdEf");
    }
    
    @Test
    public void convertIgnoreCaseTest() {
        String s = "abAcdEfA";
        s = OAString.convertIgnoreCase(s, "A", "X");
        assertEquals(s, "XbXcdEfX");
        s = OAString.convertIgnoreCase(s, "x", "bb");
        assertEquals(s, "bbbbbcdEfbb");
        s = OAString.convertIgnoreCase(s, "X", "bb");
        assertEquals(s, "bbbbbcdEfbb");
        s = OAString.convertIgnoreCase(s, "BB", "b");
        assertEquals(s, "bbbcdEfb");
        s = OAString.convertIgnoreCase(s, "B", "");
        assertEquals(s, "cdEf");
    }
    
    @Test
    public void removeOtherCharactersTest() {
        String s = "1,234,5z67,ABC.123A4f5";
        s = OAString.removeOtherCharacters(s, "1234567890.");
        assertEquals(s, "1234567.12345");
    }

    @Test
    public void removeNonDigitsTest() {
        String s = "1,234,5z67,ABC.123A4f5";
        String sx = OAString.removeNonDigits(s);
        assertEquals(sx, "123456712345");
        sx = OAString.removeNonDigits(s, true);
        assertEquals(sx, "1234567.12345");
    }
    
    @Test
    public void pluralSingularTest() {
        String s = "Tree";
        String s2 = OAString.makePlural(s);
        assertEquals(s2, "Trees");
        s2 = OAString.makeSingular(s2);
        assertEquals(s, s2);
        
        s = "try";
        s2 = OAString.makePlural(s);
        assertEquals(s2, "tries");
        s2 = OAString.makeSingular(s2);
        assertEquals(s, s2);
    }

}

/*

//qqqqqqq MAIN MAIN MAIN testing


    public static void mainXX(String[] args) {
        String s = "Tymczak";
        System.out.println(soundex(s));
        System.out.println(soundex("Ashcraft"));
    }

    public static void mainX(String[] args) {
        long xx = (long) (1234 * 1e5);
        xx += 56789;
        xx = (xx % 7777);
        String codex = "" + xx;
        String codexx = "V";
        for (int ix = 0; ix < codex.length(); ix++) {
            if ((Math.random() * 10) > 5) {
                codexx += (char) ('a' + ((int) (Math.random() * 26.0)));
            }
            codexx += codex.charAt(ix);
            if ((Math.random() * 10) > 5) {
                codexx += (char) ('A' + ((int) (Math.random() * 26.0)));
            }
        }
        System.out.println("Codexx=" + codexx);
    }

    public static void mainXx(String[] args) {
        String s = String.format("%07d  %-10s", 12, "yyyyMMdd");
        System.out.println("========> " + s);

        s = "Item{Master";
        String s2 = escapeJSON(s);
        System.out.println(s + " => " + s2);
    }

    public static void main(String[] args) {
        String s = "this is a test for the hex converter";
        byte[] bs = s.getBytes();
        String s21 = new String(bs);

        String hex = bytesToHex(bs);

        byte[] bs2 = hexToBytes(hex);

        int x = OACompare.compare(bs, bs2);

        String s2 = new String(bs2);

        int xx = 4;
        xx++;
    }
    
    public static void mainz(String[] argv) {
        String s = OAString.fmt("1234.5678", "12R2,");
        OAString oas = new OAString();
        s = oas.fmt(argv[0], argv[1]);

        System.out.println("-------->"+s+"<------");

        // double x = OAConv.toDouble("-12345.5678");
        int x = OAConv.toInt("-12345.5678");
        System.out.println("-------->"+OAConv.toString(x, "#,###.####")+"<------");
    }



	public static void main99(String[] args) {
		String s = "123.456";
		s = format(s, "#,##0.00");

		s = "123.456";
		s = format(s, "$#,##0.00");

		OADate d = new OADate();
		s = d.toString();
		s = format(s, "MMMM, dd yyyy");
		int x = 4;
		x++;
	}

	public static void mainB(String[] args) {
		String html = "<body>adfadfdsdxxx<div style='background-image:url(oaproperty://com.tmgsc.hifive.model.oa.ImageStore/bytes?232); width:88; height:99px' colspan=4 test xyz abc=Abcde123>adfa</div>";

		int w = 0;
		int h = 0;

		String find = "background-image:url(oaproperty://com.tmgsc.hifive.model.oa.ImageStore/bytes?";

		int pos = html.toLowerCase().indexOf(find.toLowerCase());

		// need to find width:88, height:99
		if (pos >= 0) {
			int divPos = html.substring(0, pos).toLowerCase().lastIndexOf("<div ");
			if (divPos >= 0) {
				int divPos2 = html.indexOf(">", divPos);
				if (divPos2 >= 0) {
					String s = html.substring(divPos, divPos2 + 1);
					String style = null;
					Map<String, String> map = OAString.getHTMLAttributeMap(s);
					for (Map.Entry<String, String> ex : map.entrySet()) {
						String sx = ex.getKey();
						if (sx.equalsIgnoreCase("style")) {
							style = ex.getValue();
							break;
						}
					}
					if (style != null) {
						map = OAString.getCSSMap(style);
						for (Map.Entry<String, String> ex : map.entrySet()) {
							String sx = ex.getKey();
							if (sx.equalsIgnoreCase("width")) {
								String val = ex.getValue();
								w = OAString.parseInt(val);
							} else if (sx.equalsIgnoreCase("height")) {
								String val = ex.getValue();
								h = OAString.parseInt(val);
							}
						}
					}
				}
			}
		}
		int x = 4;
		x++;
	}


	public static void mainXXx(String[] args) {
		String s = "a','b,'c\'     \'x      ,'d";
		s = "'c\'     \'x      ,'d";
		s = "\'\"\"";
		String[] ss = parseLine(s, ',', true);
		ss = parseLine(s, ',', false);

		int x = 4;
		x++;
	}


	public static void main2(String[] args) {
		String s = OAString.fmt("CustomerName", "8L.");

		s = OAString.fmt("CustomerName", "28L.");

		s = "abCDe_ 1.2-34.59:5";
		String s2 = OAString.makeJavaIndentifier(s);
		System.out.println(s + " ==> " + s2);
	}

	public static void mainAZ(String[] args) {

		String sx = String.format("%.5s", "this is a test");
		sx = String.format("%5.15s", "test");

		int x = LoremLipsum.length();
		String s;
		for (int i = 0; i < 5000; i++) {
			int x1 = (int) (Math.random() * (x * 3));
			int x2 = (int) (Math.random() * x1);
			int x3 = x1 + ((int) (Math.random() * x * 2));
			s = getDummyText(x1, x2, x3);
			System.out.printf("%d) %d,%d,%d=%d => %s %n", i, x1, x2, x3, s.length(), OAString.format(s, "120l."));

		}
	}


*/




