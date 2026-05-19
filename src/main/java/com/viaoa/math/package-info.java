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
/**
 * 
 * </p>
 */
package com.viaoa.math;

/* CODEX Invariants

1. Math Utility Runtime Contracts

  MATH-RUNTIME-001 — Deterministic Numeric Results
  Contract statement: For the same operands, operation, decimal-place setting, and rounding mode, OAMath must return
  the same numeric result every time.
  Rationale: OA math helpers support business calculations where repeatability matters.
  Source locations: OAMath.round(...), OAMath.performMathOp(...), add/subtract/multiply/divide overloads.
  Known related CODEX findings: none observed.
  Suggested unit tests: mathAddIsDeterministicForSameInputs(), mathDivideIsDeterministicForSameInputs().
  Spec target section: Math Utilities / Runtime Semantics.

  MATH-RUNTIME-002 — Public Operation Codes Are Explicit
  Contract statement: performMathOp must execute only known operation constants and reject unknown operation codes.
  Rationale: Silent fallback to an operand is false success and can hide calculation bugs.
  Source locations: OAMath.performMathOp(...), MATH_OP_* constants.
  Known related CODEX findings: fixed issue where unknown operation codes silently returned operand 1.
  Suggested unit tests: mathPerformMathOpRejectsUnknownOperationCode().
  Spec target section: Math Utilities / Operation Dispatch.

  2. Numeric Conversion Contracts

  MATH-CONVERT-001 — Number Inputs Convert Through OA BigDecimal Rules
  Contract statement: All Number operands must convert to BigDecimal using OA’s precision-preserving conversion rules
  before arithmetic.
  Rationale: Consistent conversion is the foundation for predictable add/subtract/multiply/divide results.
  Source locations: OAMath.toBigDecimal(...), OAMath.performMathOp(...).
  Known related CODEX findings: fixed issue where non-BigDecimal numbers converted through doubleValue() lost large
  integral differences.
  Suggested unit tests: mathConvertsNumberOperandsConsistently(), mathSubtractLargeLongsPreservesUnitDifference().
  Spec target section: Math Utilities / Numeric Conversion Semantics.

  MATH-CONVERT-002 — Null Converts To Zero
  Contract statement: Null operands supplied to arithmetic methods must be treated as numeric zero.
  Rationale: This is part of the documented OA math contract and supports concise business arithmetic.
  Source locations: OAMath.toBigDecimal(...), all arithmetic overloads.
  Known related CODEX findings: none observed.
  Suggested unit tests: mathNullLeftOperandActsAsZero(), mathNullRightOperandActsAsZero().
  Spec target section: Math Utilities / Null Conversion.

  3. BigDecimal / Precision Contracts

  MATH-PRECISION-001 — BigDecimal Inputs Remain BigDecimal
  Contract statement: If an operand is already BigDecimal, OA math must use it directly without converting through
  double.
  Rationale: Callers using BigDecimal expect explicit decimal precision to be preserved until the final double return.
  Source locations: OAMath.toBigDecimal(...).
  Known related CODEX findings: none observed.
  Suggested unit tests: mathBigDecimalOperandPreservesDecimalValueBeforeResultConversion().
  Spec target section: Math Utilities / BigDecimal Precision.

  MATH-PRECISION-002 — Result Type Is Double By Contract
  Contract statement: OAMath arithmetic returns double; precision beyond what double can represent may be lost only at
  final return conversion, not before the operation.
  Rationale: The utility intentionally exposes primitive-style results while using BigDecimal internally.
  Source locations: OAMath.performMathOp(...), OAMath.round(...).
  Known related CODEX findings: fixed large integral operand conversion issue.
  Suggested unit tests: mathLargeIntegralOperationIsExactBeforeDoubleReturn().
  Spec target section: Math Utilities / Result Precision.

  4. Integral Number Contracts

  MATH-INTEGRAL-001 — Integral Operands Preserve Exact Value
  Contract statement: Byte, Short, Integer, Long, and BigInteger operands must convert to exact decimal values before
  arithmetic.
  Rationale: Integral identity and differences must not disappear due to floating conversion.
  Source locations: OAMath.toBigDecimal(...).
  Known related CODEX findings: fixed Long.MAX_VALUE - (Long.MAX_VALUE - 1) collapsing to zero.
  Suggested unit tests: mathSubtractLargeLongsPreservesUnitDifference(), mathBigIntegerOperandPreservesExactValue().
  Spec target section: Math Utilities / Integral Precision.

  5. Floating-Point Contracts

  MATH-FLOAT-001 — Floating Operands Use Decimal String Semantics
  Contract statement: Float and Double finite operands must convert using BigDecimal.valueOf(double) semantics, not
  new BigDecimal(double).
  Rationale: OA math should avoid binary floating representation artifacts in normal decimal business values.
  Source locations: OAMath.round(...), OAMath.toBigDecimal(...), primitive double overloads.
  Known related CODEX findings: none observed.
  Suggested unit tests: mathDoubleOperandAvoidsBinaryRepresentationArtifact(),
  mathAddPointOneAndPointTwoRoundsAsExpected().
  Spec target section: Math Utilities / Floating Conversion.

  MATH-FLOAT-002 — Non-Finite Floating Values Have Defined Behavior
  Contract statement: NaN and infinity inputs must have explicit OA behavior: either propagate consistently or fail
  with a defined exception.
  Rationale: divide returns NaN by contract, so chained math behavior must not be accidental.
  Source locations: OAMath.round(...), OAMath.toBigDecimal(...), OAMath.divide(...).
  Known related CODEX findings: divide(1,0) returns Double.NaN, but later round(NaN,...) or add(NaN,...) reaches
  BigDecimal.valueOf(Double.NaN) and throws.
  Suggested unit tests: mathRoundPropagatesDivideByZeroNaN(), mathAddPropagatesNaNOperand().
  Spec target section: Math Utilities / Non-Finite Floating Semantics.

  6. Rounding / Scale Contracts

  MATH-ROUND-001 — Rounding Mode Validation Is Consistent
  Contract statement: Negative rounding mode means OA default ROUND_HALF_UP; invalid positive rounding constants must
  be rejected consistently.
  Rationale: Rounding behavior must not depend on which public overload is called.
  Source locations: OAMath.round(...), OAMath.performMathOp(...), arithmetic overloads with roundType.
  Known related CODEX findings: fixed invalid positive roundType validation in performMathOp and round.
  Suggested unit tests: mathInvalidPositiveRoundTypeHasDefinedBehavior(),
  mathRoundInvalidPositiveRoundTypeHasDefinedBehavior().
  Spec target section: Math Utilities / Rounding Mode Semantics.

  MATH-ROUND-002 — Decimal Places Control Operand And Result Scale
  Contract statement: When decimalPlaces >= 0, operands are rounded to that scale before arithmetic and the final
  result is rounded to that scale after arithmetic.
  Rationale: This is the current OA math contract and must be stable because it affects business totals.
  Source locations: OAMath.performMathOp(...), OAMath.round(...).
  Known related CODEX findings: none observed.
  Suggested unit tests: mathRoundsOperandsBeforeOperation(), mathRoundsResultAfterOperation().
  Spec target section: Math Utilities / Scale Semantics.

  MATH-ROUND-003 — Negative Decimal Places Skip Scale Enforcement
  Contract statement: decimalPlaces < 0 means no caller-requested scale rounding, except division uses bounded scale
  to avoid non-terminating decimal expansion.
  Rationale: Callers need a way to request full practical precision while avoiding divide exceptions for repeating
  decimals.
  Source locations: OAMath.performMathOp(...).
  Known related CODEX findings: none observed.
  Suggested unit tests: mathNegativeDecimalPlacesSkipsAddRounding(),
  mathDivideWithoutDecimalPlacesUsesBoundedPrecision().
  Spec target section: Math Utilities / Scale Semantics.

  7. Arithmetic Operation Contracts

  MATH-ARITH-001 — Addition/Subtraction/Multiplication Use BigDecimal Arithmetic
  Contract statement: Add, subtract, and multiply must perform arithmetic on converted BigDecimal operands, not
  primitive doubles.
  Rationale: OA math exists to avoid primitive floating drift in normal calculations.
  Source locations: OAMath.add(...), OAMath.subtract(...), OAMath.multiply(...), OAMath.performMathOp(...).
  Known related CODEX findings: fixed conversion path for integral precision.
  Suggested unit tests: mathAddUsesDecimalArithmetic(), mathSubtractUsesDecimalArithmetic(),
  mathMultiplyUsesDecimalArithmetic().
  Spec target section: Math Utilities / Arithmetic Semantics.

  MATH-ARITH-002 — Division Uses Defined Scale And Rounding
  Contract statement: Division must use the requested scale when supplied, otherwise a defined internal scale to avoid
  non-terminating decimal exceptions.
  Rationale: Repeating decimals such as 1/3 must not throw during normal OA usage.
  Source locations: OAMath.divide(...), OAMath.performMathOp(...).
  Known related CODEX findings: none observed.
  Suggested unit tests: mathDivideOneByThreeUsesRequestedScale(), mathDivideOneByThreeWithoutScaleDoesNotThrow().
  Spec target section: Math Utilities / Division Semantics.

  8. Divide-by-Zero Contracts

  MATH-DIVIDE-001 — Divide By Zero Returns NaN
  Contract statement: Division by zero must return Double.NaN, as documented by the divide APIs.
  Rationale: Callers depend on explicit numeric failure rather than exception for zero divisors.
  Source locations: OAMath.divide(...), OAMath.performMathOp(...).
  Known related CODEX findings: non-finite follow-on behavior still needs final contract decision.
  Suggested unit tests: mathDivideByZeroReturnsNaN(), mathDivideByNullDivisorReturnsNaN().
  Spec target section: Math Utilities / Divide-By-Zero Semantics.

  9. Null Handling Contracts

  MATH-NULL-001 — Null Dividend Or Arithmetic Operand Is Zero
  Contract statement: Null operands are treated as zero in all arithmetic operations, including division numerator.
  Rationale: This behavior is documented and supports OA’s null-as-empty utility style.
  Source locations: OAMath.toBigDecimal(...), arithmetic overloads.
  Known related CODEX findings: none observed.
  Suggested unit tests: mathAddNullAndValueReturnsValue(), mathDivideNullByValueReturnsZero().
  Spec target section: Math Utilities / Null Semantics.

  MATH-NULL-002 — Null Divisor Is Zero Divisor
  Contract statement: A null divisor converts to zero and therefore follows divide-by-zero behavior.
  Rationale: Null conversion and divide-by-zero contracts must compose predictably.
  Source locations: OAMath.toBigDecimal(...), OAMath.performMathOp(...).
  Known related CODEX findings: none observed.
  Suggested unit tests: mathDivideByNullReturnsNaN().
  Spec target section: Math Utilities / Null Division Semantics.

  10. Failure / Silent Wrong-Result Contracts

  MATH-FAIL-001 — Invalid Public Parameters Must Not Produce False Success
  Contract statement: Invalid operation codes and invalid positive rounding modes must fail explicitly, not return a
  plausible numeric value.
  Rationale: Silent numeric wrong results are worse than visible failures in business calculations.
  Source locations: OAMath.performMathOp(...), OAMath.round(...).
  Known related CODEX findings: fixed unknown operation code and invalid positive roundType behavior.
  Suggested unit tests: mathPerformMathOpRejectsUnknownOperationCode(),
  mathInvalidPositiveRoundTypeHasDefinedBehavior().
  Spec target section: Math Utilities / Failure Semantics.

  MATH-FAIL-002 — Precision Loss Must Be Contracted, Not Accidental
  Contract statement: Any precision loss must be a defined result of final double return or requested rounding, not
  accidental operand conversion.
  Rationale: OA math’s value is controlled precision; hidden pre-operation loss breaks that contract.
  Source locations: OAMath.toBigDecimal(...), OAMath.performMathOp(...).
  Known related CODEX findings: fixed integral doubleValue() conversion loss.
  Suggested unit tests: mathLargeIntegralOperationIsExactBeforeDoubleReturn(),
  mathBigDecimalOperandDoesNotConvertThroughDouble().
  Spec target section: Math Utilities / Silent Wrong-Result Prevention.

  11. Test Coverage Matrix

  Math Utilities / Runtime Semantics
  Tests: mathAddIsDeterministicForSameInputs, mathDivideIsDeterministicForSameInputs.

  Math Utilities / Numeric Conversion Semantics
  Tests: mathConvertsNumberOperandsConsistently, mathNullLeftOperandActsAsZero, mathNullRightOperandActsAsZero.

  Math Utilities / BigDecimal Precision
  Tests: mathBigDecimalOperandPreservesDecimalValueBeforeResultConversion,
  mathBigDecimalOperandDoesNotConvertThroughDouble.

  Math Utilities / Integral Precision
  Tests: mathSubtractLargeLongsPreservesUnitDifference, mathBigIntegerOperandPreservesExactValue.

  Math Utilities / Floating Conversion
  Tests: mathDoubleOperandAvoidsBinaryRepresentationArtifact, mathAddPointOneAndPointTwoRoundsAsExpected,
  mathAddPropagatesNaNOperand.

  Math Utilities / Rounding / Scale
  Tests: mathInvalidPositiveRoundTypeHasDefinedBehavior, mathRoundInvalidPositiveRoundTypeHasDefinedBehavior,
  mathRoundsOperandsBeforeOperation, mathRoundsResultAfterOperation, mathNegativeDecimalPlacesSkipsAddRounding.

  Math Utilities / Arithmetic Operations
  Tests: mathAddUsesDecimalArithmetic, mathSubtractUsesDecimalArithmetic, mathMultiplyUsesDecimalArithmetic,
  mathDivideOneByThreeUsesRequestedScale.

  Math Utilities / Divide-By-Zero
  Tests: mathDivideByZeroReturnsNaN, mathDivideByNullReturnsNaN, mathRoundPropagatesDivideByZeroNaN.

  Math Utilities / Failure Semantics
  Tests: mathPerformMathOpRejectsUnknownOperationCode, mathLargeIntegralOperationIsExactBeforeDoubleReturn.

*/



