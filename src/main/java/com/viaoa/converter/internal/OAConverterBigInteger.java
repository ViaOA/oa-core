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
package com.viaoa.converter.internal;

import java.math.BigInteger;

/*qqqqqqqqqq
CODEX


*/

/**
 * 
 */
public class OAConverterBigInteger implements OAConverterInterface<BigInteger> {

	private final OAConverterNumber numConv = new OAConverterNumber();
	
    @Override
    public BigInteger convert(Class<BigInteger> clazz, Object fromValue, String fmt) {
        if (fromValue == null) return BigInteger.ZERO;
        if (fromValue instanceof BigInteger) return (BigInteger) fromValue;

        // Delegate to Number converter once
        Number n = numConv.convert(Number.class, fromValue, fmt);
        if (n == null) return null;

        if (n instanceof BigInteger) return (BigInteger) n;
        if (n instanceof Long || n instanceof Integer || n instanceof Short || n instanceof Byte) {
            return BigInteger.valueOf(n.longValue());
        }
        // Float/Double path: use valueOf(double) (string-based) to avoid binary artifacts
        return BigInteger.valueOf(n.longValue());
    }
    
    /**
     * Formats a {@link java.math.BigDecimal} into a {@link String}. Alignment
     * masks and numeric patterns are supported as described in
     * {@link OAConverterNumber}.
     *
     * @param value the decimal value; may be {@code null}
     * @param fmt   optional formatting/alignment mask
     * @return formatted string, never {@code null}
     */
    @Override
    public String convertToString(BigInteger fromValue, String fmt) {
    	return numConv.convertToString(fromValue, fmt);
    }
}

