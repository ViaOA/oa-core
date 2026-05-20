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

MATH-RUNTIME-001 — Deterministic Numeric Operations
Contract statement:
For the same operands, operation code, decimal-place setting, and rounding mode, OA math operations must produce the
same result every time.
Rationale:
OA math supports business calculations, persisted numeric values, query/filter evaluation, reporting, and runtime
graph state where repeatable results are required.
Source scope:
OAMath.round(...), OAMath.performMathOp(...), OAMath.add(...), OAMath.subtract(...), OAMath.multiply(...),
OAMath.divide(...).
Related CODEX findings:
none observed.
Suggested unit tests:
mathAddIsDeterministicForSameInputs(), mathDivideIsDeterministicForSameInputs(),
mathRoundIsDeterministicForSameInputs().
Spec target section:
Math / Deterministic Numeric Runtime Semantics.

MATH-OP-001 — Explicit Operation Dispatch
Contract statement:
OAMath.performMathOp must execute only defined OA math operation constants and must fail visibly for unknown
operation codes.
Rationale:
Unknown numeric operations must not silently return a plausible value because false-success arithmetic can corrupt
business totals and runtime graph-derived values.
Source scope:
OAMath.performMathOp(...), OAMath.MATH_OP_MULTIPLY, OAMath.MATH_OP_DIVIDE, OAMath.MATH_OP_ADD,
OAMath.MATH_OP_SUBTRACT.
Related CODEX findings:
package-info historical reference to unknown operation code false success.
Suggested unit tests:
mathPerformMathOpRejectsUnknownOperationCode(), mathPerformMathOpExecutesEachKnownOperationCode().
Spec target section:
Math / Operation Dispatch Semantics.

MATH-CONVERT-001 — Precision-Preserving Number Conversion
Contract statement:
Number operands must be converted into BigDecimal using OA precision-preserving rules before arithmetic is
performed.
Rationale:
OA math exists to avoid accidental primitive floating-point drift and to preserve intended numeric value before
final result conversion.
Source scope:
OAMath.performMathOp(...), OAMath.toBigDecimal(Number), all Number-based arithmetic overloads.
Related CODEX findings:
package-info historical reference to large integral values losing unit differences through double conversion.
Suggested unit tests:
mathConvertsNumberOperandsConsistently(), mathSubtractLargeLongsPreservesUnitDifference(),
mathBigIntegerOperandPreservesExactValue().
Spec target section:
Math / Numeric Conversion Semantics.

MATH-CONVERT-002 — BigDecimal Operand Preservation
Contract statement:
BigDecimal operands must be used as decimal values directly and must not be converted through double before
arithmetic.
Rationale:
Callers that supply BigDecimal are explicitly providing decimal precision that OA math must preserve until requested
rounding or final double return.
Source scope:
OAMath.toBigDecimal(Number), OAMath.performMathOp(...).
Related CODEX findings:
none observed.
Suggested unit tests:
mathBigDecimalOperandDoesNotConvertThroughDouble(),
mathBigDecimalOperandPreservesDecimalValueBeforeResultConversion().
Spec target section:
Math / BigDecimal Precision Semantics.

MATH-CONVERT-003 — Integral Operand Exactness
Contract statement:
Byte, Short, Integer, Long, and BigInteger operands must retain exact integral value when converted for OA math
operations.
Rationale:
Object keys, counts, quantities, and persisted integral values must not lose identity or unit differences during
calculation.
Source scope:
OAMath.toBigDecimal(Number), OAMath.add(...), OAMath.subtract(...), OAMath.multiply(...), OAMath.divide(...).
Related CODEX findings:
package-info historical reference to Long.MAX_VALUE minus Long.MAX_VALUE - 1 collapsing to zero.
Suggested unit tests:
mathLongOperandsPreserveExactUnitDifference(), mathIntegerOperandsConvertExactly(),
mathBigIntegerOperandPreservesExactValue().
Spec target section:
Math / Integral Precision Semantics.

MATH-CONVERT-004 — Floating Operand Decimal Semantics
Contract statement:
Finite float and double operands must be converted using decimal string/value semantics equivalent to
BigDecimal.valueOf(double), not binary-constructor semantics.
Rationale:
Normal OA business decimals such as 0.1 and 0.2 must not inherit avoidable binary representation artifacts before OA
rounding rules are applied.
Source scope:
OAMath.round(...), primitive double overloads, OAMath.performMathOp(...), OAMath.toBigDecimal(Number).
Related CODEX findings:
none observed.
Suggested unit tests:
mathDoubleOperandAvoidsBinaryConstructorArtifact(), mathAddPointOneAndPointTwoRoundsAsExpected().
Spec target section:
Math / Floating Conversion Semantics.

MATH-NULL-001 — Null Numeric Operand Semantics
Contract statement:
Null numeric operands supplied to OA arithmetic methods must be treated as numeric zero.
Rationale:
This is part of the OA math utility contract and supports concise runtime calculations over optional object values.
Source scope:
OAMath.toBigDecimal(Number), all public arithmetic overloads.
Related CODEX findings:
none observed.
Suggested unit tests:
mathAddNullAndValueReturnsValue(), mathSubtractNullUsesZero(), mathMultiplyNullUsesZero(),
mathDivideNullNumeratorUsesZero().
Spec target section:
Math / Null Numeric Semantics.

MATH-DIVIDE-001 — Divide-By-Zero Result Semantics
Contract statement:
Division by zero, including a null divisor converted to zero, must return Double.NaN as the explicit OA math divide
failure value.
Rationale:
OA divide APIs expose numeric failure for zero divisors without throwing, and callers must be able to distinguish
this from a valid numeric result.
Source scope:
OAMath.divide(...), OAMath.performMathOp(...), OAMath.toBigDecimal(Number).
Related CODEX findings:
OAMath.java CODEX note identifies follow-on non-finite handling after divide returns NaN.
Suggested unit tests:
mathDivideByZeroReturnsNaN(), mathDivideByNullDivisorReturnsNaN(), mathPerformMathOpDivideByZeroReturnsNaN().
Spec target section:
Math / Divide-By-Zero Semantics.

MATH-NONFINITE-001 — Non-Finite Number Boundary
Contract statement:
NaN and infinity inputs or results must have explicit OA behavior: they must either propagate consistently as non-
finite numeric results or fail visibly with a defined exception; they must not produce accidental conversion
failures or silent numeric values.
Rationale:
OAMath.divide intentionally returns NaN for divide-by-zero, so downstream OA math behavior must be predictable when
non-finite values enter later operations.
Source scope:
OAMath.round(...), OAMath.add(...), OAMath.subtract(...), OAMath.multiply(...), OAMath.divide(...),
OAMath.performMathOp(...), OAMath.toBigDecimal(Number).
Related CODEX findings:
OAMath.java CODEX note: divide(1, 0) returns Double.NaN, but round(NaN, ...) or add(NaN, ...) reaches
BigDecimal.valueOf(Double.NaN).
Suggested unit tests:
mathRoundNaNHasDefinedBehavior(), mathAddNaNHasDefinedBehavior(), mathInfinityOperandHasDefinedBehavior().
Spec target section:
Math / Non-Finite Numeric Semantics.

MATH-ROUND-001 — Rounding Mode Semantics
Contract statement:
Negative rounding mode values must select OA default ROUND_HALF_UP behavior, and invalid positive rounding constants
must fail visibly.
Rationale:
Rounding behavior must be stable across overloads and must not silently fall back to an unintended mode.
Source scope:
OAMath.round(...), OAMath.performMathOp(...), arithmetic overloads with roundType.
Related CODEX findings:
package-info historical reference to invalid positive roundType handling.
Suggested unit tests:
mathNegativeRoundTypeUsesHalfUp(), mathRoundRejectsInvalidPositiveRoundType(),
mathPerformMathOpRejectsInvalidPositiveRoundType().
Spec target section:
Math / Rounding Mode Semantics.

MATH-SCALE-001 — Decimal Place Scale Semantics
Contract statement:
When decimalPlaces is non-negative, OA math must apply that scale to operands before arithmetic and to the final
result after arithmetic.
Rationale:
Pre- and post-operation scale rules materially affect business totals and must remain deterministic for persisted,
displayed, and reported values.
Source scope:
OAMath.performMathOp(...), OAMath.round(...), add/subtract/multiply/divide overloads with decimalPlaces.
Related CODEX findings:
none observed.
Suggested unit tests:
mathRoundsOperandsBeforeOperation(), mathRoundsResultAfterOperation(),
mathScaleBehaviorIsConsistentAcrossOverloads().
Spec target section:
Math / Scale and Rounding Semantics.

MATH-SCALE-002 — Negative Decimal Places Semantics
Contract statement:
Negative decimalPlaces must mean no caller-requested scale enforcement for add, subtract, multiply, and round, while
division must still use a bounded internal scale to avoid non-terminating decimal expansion failures.
Rationale:
Callers need a full-practical-precision mode that remains safe for repeating decimal division.
Source scope:
OAMath.round(...), OAMath.performMathOp(...), arithmetic overloads without decimalPlaces.
Related CODEX findings:
none observed.
Suggested unit tests:
mathNegativeDecimalPlacesSkipsAddRounding(), mathNegativeDecimalPlacesSkipsRoundScale(),
mathDivideWithoutDecimalPlacesUsesBoundedScale().
Spec target section:
Math / Scale Boundary Semantics.

MATH-ARITH-001 — BigDecimal Arithmetic Authority
Contract statement:
Addition, subtraction, multiplication, and division must perform arithmetic on converted BigDecimal operands, not
primitive doubles, before returning the final double result.
Rationale:
OA numeric correctness depends on controlled decimal arithmetic rather than native floating-point arithmetic.
Source scope:
OAMath.add(...), OAMath.subtract(...), OAMath.multiply(...), OAMath.divide(...), OAMath.performMathOp(...).
Related CODEX findings:
package-info historical reference to operand conversion precision loss.
Suggested unit tests:
mathAddUsesDecimalArithmetic(), mathSubtractUsesDecimalArithmetic(), mathMultiplyUsesDecimalArithmetic(),
mathDivideUsesDecimalArithmetic().
Spec target section:
Math / Arithmetic Semantics.

MATH-DIVIDE-002 — Division Scale and Rounding Semantics
Contract statement:
Division must use the requested decimal scale when supplied, otherwise it must use a defined internal scale and
rounding mode so repeating decimals do not fail during normal OA usage.
Rationale:
Calculations such as 1 / 3 are common in business and reporting logic and must have predictable bounded results.
Source scope:
OAMath.divide(...), OAMath.performMathOp(...).
Related CODEX findings:
none observed.
Suggested unit tests:
mathDivideOneByThreeUsesRequestedScale(), mathDivideOneByThreeWithoutRequestedScaleDoesNotThrow(),
mathDivideUsesRequestedRoundingMode().
Spec target section:
Math / Division Semantics.

MATH-RESULT-001 — Final Double Result Boundary
Contract statement:
OAMath arithmetic returns double by API contract; precision loss is permitted only as a consequence of requested
rounding or final double conversion, not from pre-operation operand conversion.
Rationale:
The package provides primitive-style numeric results while preserving decimal correctness through the operation
boundary.
Source scope:
OAMath.round(...), OAMath.performMathOp(...), all public arithmetic overloads.
Related CODEX findings:
package-info historical reference to accidental pre-operation precision loss.
Suggested unit tests:
mathPrecisionLossOnlyOccursAtFinalDoubleReturn(), mathLargeIntegralOperationIsExactBeforeDoubleReturn().
Spec target section:
Math / Result Boundary Semantics.

MATH-FAIL-001 — Visible Failure for Invalid Numeric Requests
Contract statement:
Invalid operation codes, invalid positive rounding modes, unsupported non-finite handling, and other unsafe numeric
requests must fail visibly or return an explicitly contracted failure value such as NaN.
Rationale:
Silent wrong-output prevention is required because OA math results can drive persisted state, query/filter
decisions, display values, and graph-derived calculated values.
Source scope:
OAMath.round(...), OAMath.performMathOp(...), OAMath.divide(...), OAMath.toBigDecimal(Number).
Related CODEX findings:
OAMath.java CODEX note on undefined chained NaN behavior; package-info historical references to false-success
operation and rounding cases.
Suggested unit tests:
mathInvalidOperationDoesNotReturnOperand(), mathInvalidRoundTypeFailsVisibly(),
mathUnsafeNonFiniteInputHasDefinedFailureBehavior().
Spec target section:
Math / Failure and False-Success Prevention.

MATH-BOUNDARY-001 — Math Package Boundary Responsibility
Contract statement:
com.viaoa.math is responsible for numeric calculation semantics only; text formatting, locale display, parsing from
arbitrary text, and general object conversion must remain delegated to converter/text/template/query callers unless
explicitly exposed by this package.
Rationale:
Clear package boundaries prevent numeric calculation contracts from being confused with display, parsing, or
metadata conversion contracts.
Source scope:
OAMath public API; integration boundaries with com.viaoa.converter, com.viaoa.text, com.viaoa.query,
com.viaoa.template, datasource, object, hub, graph, serialization, sync, and replication packages.
Related CODEX findings:
none observed.
Suggested unit tests:
mathDoesNotApplyLocaleFormattingRules(), mathNumericOperationsRemainIndependentOfTextFormatting().
Spec target section:
Math / Cross-Package Boundary Semantics.

MATH-THREAD-001 — Static Utility Reuse Safety
Contract statement:
OAMath operations must not retain mutable per-call numeric state across invocations; concurrent calls with
independent inputs must not affect each other.
Rationale:
OA math helpers are static runtime utilities used by object graph, query, template, reporting, and distributed
runtime code paths.
Source scope:
OAMath static methods and constants.
Related CODEX findings:
none observed.
Suggested unit tests:
mathConcurrentIndependentOperationsDoNotInterfere(), mathRepeatedCallsDoNotReusePriorScaleOrOperands().
Spec target section:
Math / Concurrent Utility Semantics.

MATH-OG-001 — Numeric Operation Success Is Not Object Graph Success
Contract statement:
A successful OAMath calculation only establishes numeric result correctness; it must not imply successful
persistence, cache update, serialization, sync, replication, validation, or Object Graph mutation.
Rationale:
OA math participates in runtime graph calculations, but semantic graph operation success belongs to the calling
runtime package.
Source scope:
OAMath public API; integration boundaries with object, hub, graph, datasource, query, template, serialization, sync,
and replication packages.
Related CODEX findings:
none observed.
Suggested unit tests:
mathResultCanBeUsedByCallerWithoutMutatingObjectGraph(), mathFailureDoesNotPublishPartialGraphSemantics().
Spec target section:
Math / Object Graph Boundary Semantics.

*/


