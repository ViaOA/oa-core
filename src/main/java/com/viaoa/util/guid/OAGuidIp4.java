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
package com.viaoa.util.guid;

import java.util.concurrent.atomic.AtomicLong;

import com.viaoa.util.OAConv;
import com.viaoa.util.OAInteger;
import com.viaoa.util.OANetwork;
import com.viaoa.util.OAString;

/**
 * Generates a 64-bit GUID whose high-order bits encode information about the
 * machine and environment, and whose low-order bits form a sequential counter.
 * <p>
 *
 * The GUID structure is:
 * <ul>
 *   <li><b>1 bit</b>: sign bit, always 0 to ensure a non-negative value</li>
 *   <li><b>8 bits</b>: the fourth octet of the machine's IPv4 address</li>
 *   <li><b>N bits</b>: optional extra feature bits based on the supplied
 *       boolean array</li>
 *   <li><b>remaining bits</b>: available for a monotonically increasing
 *       counter</li>
 * </ul>
 *
 * The number of bits required for the counter is specified by the
 * <code>bitsNeeded</code> constructor argument. During initialization the class
 * computes the maximum usable counter range, constructs a starting identifier,
 * and creates an {@link java.util.concurrent.atomic.AtomicLong} for generating
 * the sequence. <p>
 *
 * GUID generation is thread-safe. The {@link #getNextId()} method lazily
 * initializes the bit layout on first use, then returns successive values
 * within the allocated bit range. The high-order bits remain constant for the
 * lifetime of the instance. <p>
 *
 * This class is useful for distributed systems where independent servers must
 * generate unique, non-colliding identifiers without coordination while still
 * embedding node information in the identifier.
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

}
