package com.viaoa.util.guid;

import com.viaoa.util.OAInteger;

public class OAGuidIp4Test {

	
	public static void main(String[] args) {
		OAGuidIp4 g = new OAGuidIp4(24);
		long x = g.getMaxValue();
		String sx = Long.toBinaryString(x);
		// System.out.println(sx);
		String s = OAInteger.getAsBinary(x);
		// System.out.println(s + "  " + x);

		s = OAInteger.getAsBinary(Long.MAX_VALUE);
		// System.out.println(s);

		g = new OAGuidIp4(4, true, true, true, true);
		long id = g.getNextId();
		s = OAInteger.getAsBinary(id);
		System.out.println(s + "  " + id);

		/*
		x = g.getMaxValue();
		s = OAInteger.getAsBinary(x);
		System.out.println(s + "  " + x);
		int i = s.length();
		
		long l = g.getNextId();
		*/
		int xx = 4;
		xx++;
	}

}


