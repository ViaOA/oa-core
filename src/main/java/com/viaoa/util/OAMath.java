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

import java.math.BigDecimal;


/**
 * Utility class that provides precision-safe mathematical operations using {@link BigDecimal}.
 * <p>
 * {@code OAMath} enables developers to freely perform arithmetic using {@code double} or {@link Number}
 * types while maintaining controlled rounding and consistent decimal precision. This prevents the
 * rounding drift and representation errors that occur with native floating-point arithmetic.
 * </p>
 * <p>
 * All operations coerce inputs to {@code BigDecimal}, round them before and after calculation,
 * and return a {@code double} result. {@code null} values are treated as {@code 0.0}.
 * </p>
 * <p>
 * Designed for business and financial calculations where precision and predictable rounding
 * are required, while still allowing a natural, primitive-style programming model.
 * </p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * double total = OAMath.add(10.25, 5.333, 2, BigDecimal.ROUND_HALF_UP);
 * double ratio = OAMath.divide(1, 3, 4);
 * double net   = OAMath.subtract(100.0, 15.5);
 * }</pre>
 *
 * @see BigDecimal
 */public class OAMath {

    /**
     * Rounds a double value to the specified number of decimal places using
     * the provided rounding type.
     *
     * @param d             the value to round
     * @param decimalPlaces number of digits to retain after the decimal point
     * @param roundType     rounding mode constant (e.g., {@link BigDecimal#ROUND_HALF_UP})
     * @return the rounded double value
     */
	public static double round(double d, int decimalPlaces, int roundType) {
		BigDecimal bd = BigDecimal.valueOf(d); // important NOT to use new BigDecimal(d)
		bd = bd.setScale(decimalPlaces, roundType);
		return bd.doubleValue();
	}

    /**
     * Rounds a double value to the specified number of decimal places using
     * {@link BigDecimal#ROUND_HALF_UP} rounding.
     *
     * @param d             the value to round
     * @param decimalPlaces number of digits to retain after the decimal point
     * @return the rounded double value
     */
	public static double round(double d, int decimalPlaces) {
		return round(d, decimalPlaces, BigDecimal.ROUND_HALF_UP);
	}

	
    /**
     * Adds two {@link Number} values with full rounding control.
     *
     * @param n1            first value (nullable)
     * @param n2            second value (nullable)
     * @param decimalPlaces number of digits after the decimal point, or negative to skip rounding
     * @param roundType     rounding mode constant (e.g., {@link BigDecimal#ROUND_HALF_UP})
     * @return the resulting sum
     */
	public static double add(Number n1, Number n2, int decimalPlaces, int roundType) {
		return performMathOp(MATH_OP_ADD, n1, n2, decimalPlaces, roundType);
	}

	public static double add(double d, Number n, int decimalPlaces, int roundType) {
		return performMathOp(MATH_OP_ADD, BigDecimal.valueOf(d), n, decimalPlaces, roundType);
	}
	
	public static double add(Number n1, double d1, int decimalPlaces, int roundType) {
		return performMathOp(MATH_OP_ADD, n1, BigDecimal.valueOf(d1), decimalPlaces, roundType);
	}

	public static double add(double d, double d1, int decimalPlaces, int roundType) {
		return performMathOp(MATH_OP_ADD, d, d1, decimalPlaces, roundType);
	}
	
	public static double add(Number n1, Number n2, int decimalPlaces) {
		return performMathOp(MATH_OP_ADD, n1, n2, decimalPlaces, BigDecimal.ROUND_HALF_UP);
	}

	public static double add(double d, Number n, int decimalPlaces) {
		return performMathOp(MATH_OP_ADD, BigDecimal.valueOf(d), n, decimalPlaces, BigDecimal.ROUND_HALF_UP);
	}

	public static double add(Number n1, double d1, int decimalPlaces) {
		return performMathOp(MATH_OP_ADD, n1, d1, decimalPlaces, BigDecimal.ROUND_HALF_UP);
	}

	public static double add(double d1, double d2, int decimalPlaces) {
		return performMathOp(MATH_OP_ADD, d1, d2, decimalPlaces, BigDecimal.ROUND_HALF_UP);
	}

	public static double add(Number n1, Number n2) {
		return performMathOp(MATH_OP_ADD, n1, n2, -1, -1);
	}

	public static double add(double d, Number n) {
		return performMathOp(MATH_OP_ADD, BigDecimal.valueOf(d), n, -1, -1);
	}

	public static double add(Number n1, double d1) {
		return performMathOp(MATH_OP_ADD, n1, d1, -1, -1);
	}

	public static double add(double d, double d1) {
		return performMathOp(MATH_OP_ADD, d, d1, -1, -1);
	}
	
	
    /**
     * Subtracts {@code n2} from {@code n1} with rounding control.
     */
	public static double subtract(Number n1, Number n2, int decimalPlaces, int roundType) {
		return performMathOp(MATH_OP_SUBTRACT, n1, n2, decimalPlaces, roundType);
	}

	public static double subtract(double d, Number n, int decimalPlaces, int roundType) {
		return performMathOp(MATH_OP_SUBTRACT, BigDecimal.valueOf(d), n, decimalPlaces, roundType);
	}

	public static double subtract(Number n1, double d1, int decimalPlaces, int roundType) {
		return performMathOp(MATH_OP_SUBTRACT, n1, BigDecimal.valueOf(d1), decimalPlaces, roundType);
	}

	public static double subtract(double d, double d1, int decimalPlaces, int roundType) {
		return performMathOp(MATH_OP_SUBTRACT, d, d1, decimalPlaces, roundType);
	}

	public static double subtract(Number n1, Number n2, int decimalPlaces) {
		return performMathOp(MATH_OP_SUBTRACT, n1, n2, decimalPlaces, BigDecimal.ROUND_HALF_UP);
	}

	public static double subtract(double d, Number n, int decimalPlaces) {
		return performMathOp(MATH_OP_SUBTRACT, BigDecimal.valueOf(d), n, decimalPlaces, BigDecimal.ROUND_HALF_UP);
	}

	public static double subtract(Number n1, double d1, int decimalPlaces) {
		return performMathOp(MATH_OP_SUBTRACT, n1, BigDecimal.valueOf(d1), decimalPlaces, BigDecimal.ROUND_HALF_UP);
	}

	public static double subtract(double d1, double d2, int decimalPlaces) {
		return performMathOp(MATH_OP_SUBTRACT, d1, d2, decimalPlaces, BigDecimal.ROUND_HALF_UP);
	}

	public static double subtract(Number n1, Number n2) {
		return performMathOp(MATH_OP_SUBTRACT, n1, n2, -1, -1);
	}

	public static double subtract(double d, Number n) {
		return performMathOp(MATH_OP_SUBTRACT, BigDecimal.valueOf(d), n, -1, -1);
	}

	public static double subtract(Number n1, double d1) {
		return performMathOp(MATH_OP_SUBTRACT, n1, BigDecimal.valueOf(d1), -1, -1);
	}

	public static double subtract(double d1, double d2) {
		return performMathOp(MATH_OP_SUBTRACT, d1, d2, -1, -1);
	}

	
    /**
     * Multiplies two {@link Number} values with rounding control.
     */
	public static double multiply(Number n1, Number n2, int decimalPlaces, int roundType) {
		return performMathOp(MATH_OP_MULTIPLY, n1, n2, decimalPlaces, roundType);
	}

	public static double multiply(double d, Number n, int decimalPlaces, int roundType) {
		return performMathOp(MATH_OP_MULTIPLY, BigDecimal.valueOf(d), n, decimalPlaces, roundType);
	}

	public static double multiply(Number n1, double d1, int decimalPlaces, int roundType) {
		return performMathOp(MATH_OP_MULTIPLY, n1, BigDecimal.valueOf(d1), decimalPlaces, roundType);
	}

	public static double multiply(double d, double d1, int decimalPlaces, int roundType) {
		return performMathOp(MATH_OP_MULTIPLY, d, d1, decimalPlaces, roundType);
	}

	public static double multiply(Number n1, Number n2, int decimalPlaces) {
		return performMathOp(MATH_OP_MULTIPLY, n1, n2, decimalPlaces, BigDecimal.ROUND_HALF_UP);
	}

	public static double multiply(double d, Number n, int decimalPlaces) {
		return performMathOp(MATH_OP_MULTIPLY, BigDecimal.valueOf(d), n, decimalPlaces, BigDecimal.ROUND_HALF_UP);
	}

	public static double multiply(Number n1, double d1, int decimalPlaces) {
		return performMathOp(MATH_OP_MULTIPLY, n1, BigDecimal.valueOf(d1), decimalPlaces, BigDecimal.ROUND_HALF_UP);
	}

	
	public static double multiply(double d1, double d2, int decimalPlaces) {
		return performMathOp(MATH_OP_MULTIPLY, d1, d2, decimalPlaces, BigDecimal.ROUND_HALF_UP);
	}

	public static double multiply(Number n1, Number n2) {
		return performMathOp(MATH_OP_MULTIPLY, n1, n2, -1, -1);
	}

	public static double multiply(double d, Number n) {
		return performMathOp(MATH_OP_MULTIPLY, BigDecimal.valueOf(d), n, -1, -1);
	}

	public static double multiply(Number n1, double d1) {
		return performMathOp(MATH_OP_MULTIPLY, n1, BigDecimal.valueOf(d1), -1, -1);
	}

	public static double multiply(double d, double d1) {
		return performMathOp(MATH_OP_MULTIPLY, d, d1, -1, -1);
	}

    /**
     * Divides {@code n1} by {@code n2} with rounding control.
     * Returns {@link Double#NaN} if {@code n2} is zero.
     */
	public static double divide(Number n1, Number n2, int decimalPlaces, int roundType) {
		return performMathOp(MATH_OP_DIVIDE, n1, n2, decimalPlaces, roundType);
	}

	public static double divide(double d, Number n, int decimalPlaces, int roundType) {
		return performMathOp(MATH_OP_DIVIDE, BigDecimal.valueOf(d), n, decimalPlaces, roundType);
	}

	public static double divide(Number n1, double d1, int decimalPlaces, int roundType) {
		return performMathOp(MATH_OP_DIVIDE, n1, BigDecimal.valueOf(d1), decimalPlaces, roundType);
	}

	public static double divide(double d, double d1, int decimalPlaces, int roundType) {
		return performMathOp(MATH_OP_DIVIDE, d, d1, decimalPlaces, roundType);
	}

	public static double divide(Number n1, Number n2, int decimalPlaces) {
		return performMathOp(MATH_OP_DIVIDE, n1, n2, decimalPlaces, BigDecimal.ROUND_HALF_UP);
	}

	public static double divide(double d, Number n, int decimalPlaces) {
		return performMathOp(MATH_OP_DIVIDE, BigDecimal.valueOf(d), n, decimalPlaces, BigDecimal.ROUND_HALF_UP);
	}

	public static double divide(Number n1, double d1, int decimalPlaces) {
		return performMathOp(MATH_OP_DIVIDE, n1, BigDecimal.valueOf(d1), decimalPlaces, BigDecimal.ROUND_HALF_UP);
	}

	public static double divide(double d1, double d2, int decimalPlaces) {
		return performMathOp(MATH_OP_DIVIDE, d1, d2, decimalPlaces, BigDecimal.ROUND_HALF_UP);
	}

	public static double divide(Number n1, Number n2) {
		return performMathOp(MATH_OP_DIVIDE, n1, n2, -1, -1);
	}

	public static double divide(double d, Number n) {
		return performMathOp(MATH_OP_DIVIDE, BigDecimal.valueOf(d), n, -1, -1);
	}

	public static double divide(Number n1, double d1) {
		return performMathOp(MATH_OP_DIVIDE, n1, BigDecimal.valueOf(d1), -1, -1);
	}

	public static double divide(double d, double d1) {
		return performMathOp(MATH_OP_DIVIDE, d, d1, -1, -1);
	}


	public static final int MATH_OP_MULTIPLY = 0;
	public static final int MATH_OP_DIVIDE = 1;
	public static final int MATH_OP_ADD = 2;
	public static final int MATH_OP_SUBTRACT = 3;
	
	
	
    /**
     * Bridge that allows direct use of primitive {@code double} values for any math operation.
     * Converts the values to {@link BigDecimal} and delegates to
     * {@link #performMathOp(int, Number, Number, int, int)}.
     *
     * @param op operation code (e.g., {@link #MATH_OP_ADD})
     * @param a  first value
     * @param b  second value
     * @param dp number of decimal places, or negative for full precision
     * @param rt rounding mode constant, or negative to use default rounding
     * @return computed double result
     */	
	private static double performMathOp(int op, double a, double b, int dp, int rt) {
	    return performMathOp(op, BigDecimal.valueOf(a), BigDecimal.valueOf(b), dp, rt);
	}	

	
    /**
     * Core implementation of all arithmetic operations. Coerces both inputs to {@link BigDecimal},
     * applies pre- and post-rounding, executes the requested operation, and returns a double result.
     * <p>
     * If division is performed and the divisor is zero, {@link Double#NaN} is returned.
     * </p>
     *
     * @param opType        operation code (see {@link #MATH_OP_ADD}, {@link #MATH_OP_SUBTRACT}, etc.)
     * @param n1            first operand (nullable)
     * @param n2            second operand (nullable)
     * @param decimalPlaces number of decimal places to retain, or negative for full precision
     * @param roundType     rounding mode constant (negative for default)
     * @return resulting double value after rounding
     */
	public static double performMathOp(int opType, Number n1, Number n2, int decimalPlaces, int roundType) {
		if (roundType < 0) roundType = BigDecimal.ROUND_HALF_UP;
		BigDecimal bd1;
		if (n1 instanceof BigDecimal) {
			bd1 = (BigDecimal) n1;
		} else {
			bd1 = BigDecimal.valueOf(n1 == null ? 0.0 : n1.doubleValue());
		}
		if (decimalPlaces >= 0) {
			bd1 = bd1.setScale(decimalPlaces, roundType);
		}

		BigDecimal bd2;
		if (n2 instanceof BigDecimal) {
			bd2 = (BigDecimal) n2;
		} else {
			bd2 = BigDecimal.valueOf(n2 == null ? 0.0 : n2.doubleValue());
		}
		if (decimalPlaces >= 0) {
			bd2 = bd2.setScale(decimalPlaces, roundType);
		}

		switch (opType) {
		case MATH_OP_MULTIPLY:
			bd1 = bd1.multiply(bd2);
			break;
		case MATH_OP_DIVIDE:
			if (bd2.compareTo(BigDecimal.ZERO) == 0) return Double.NaN;
			
			// uses hardcoded values (8, roundHalfUp) to avoid: ArithmeticException: Non-terminating decimal expansion.
			//   ex: 1/3 is repeating, need to use scale to limit/max how many decimal places.
			int x = (decimalPlaces >= 0) ? decimalPlaces : 16;
			bd1 = bd1.divide(bd2, x, BigDecimal.ROUND_HALF_UP);  
			break;
		case MATH_OP_ADD:
			bd1 = bd1.add(bd2);
			break;
		case MATH_OP_SUBTRACT:
			bd1 = bd1.subtract(bd2);
			break;
		}

		if (decimalPlaces >= 0) {
			bd1 = bd1.setScale(decimalPlaces, roundType);
		}
		return bd1.doubleValue();
	}
}
