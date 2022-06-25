package com.viaoa.util.guid;

import java.util.concurrent.atomic.AtomicLong;

import com.viaoa.util.OAConv;
import com.viaoa.util.OAInteger;
import com.viaoa.util.OANetwork;
import com.viaoa.util.OAString;

/**
 * GUID based on using last octet of IP as high order bits. <br>
 * Find the first number to use for a UUID integer that is based on the following:<br>
 * high bit = 0 (keep negatives out) <br>
 * next byte = IP address's lower (4th) octet<br>
 * next extra bit(s) from boolean[]. Example: this could be used as false for all servers on same subnet, and true if outside server.
 * <p>
 * The amount of bits needed is then used to right shift any bits not needed (which keeps the number smaller)
 *
 * @author vvia
 */
public class OAGuidIp4 {
	private final boolean[] bAddExtraBits;
	private final int bitsNeeded;
	private volatile AtomicLong nextId;
	private int bitsUsed;
	private int bitsAvailable;
	private long startId;
	private long maxValue;
	private volatile boolean bInit;

	/**
	 * Create new Guid based on IP 4th octet, and any added extra bits.
	 *
	 * @param bitsNeeded    number of bits that are needed, will be used when determining the actual bits that are used.
	 * @param bAddExtraBits that can be used.
	 */
	public OAGuidIp4(int bitsNeeded, boolean... bAddExtraBits) {
		this.bitsNeeded = bitsNeeded;
		this.bAddExtraBits = bAddExtraBits;
	}

	/**
	 * Get the next
	 *
	 * @return
	 */
	public long getNextId() {
		if (!bInit) {
			init();
		}
		long id = nextId.getAndIncrement();
		return id;
	}

	public synchronized void init() {
		if (bInit) {
			return;
		}

		String ip = OANetwork.getIPAddress();
		String ips = OAString.field(ip, ".", 4);
		long ipx = OAConv.toLong(ips);

		// shift 8 bit IP to left, but not using negative bit
		long id = ipx << (64 - 9);

		for (int i = 0; i < bAddExtraBits.length; i++) {
			if (bAddExtraBits[i]) {
				int x = (64 - (10 + i));
				id |= (1L << x);
			}
		}

		// shift right for unneeded bits
		bitsUsed = 1 + 8 + bAddExtraBits.length;
		bitsAvailable = 64 - bitsUsed;

		if (bitsAvailable > bitsNeeded) {
			int shift = bitsAvailable - bitsNeeded;
			id = id >>> shift;
			bitsAvailable = bitsNeeded;
		}

		long maxValue = id + ((long) Math.pow(2, bitsAvailable)) - 1;

		this.startId = id + 1;
		nextId = new AtomicLong(this.startId);

		maxValue = 1;
		for (int i = 0; i < bitsAvailable; i++) {
			maxValue *= 2;
		}

		maxValue |= nextId.longValue();
		bInit = true;
	}

	public int getBitsUsed() {
		init();
		return bitsUsed;
	}

	public int getBitsAvailable() {
		init();
		return bitsAvailable;
	}

	public long getMaxValue() {
		init();
		return maxValue;
	}

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
