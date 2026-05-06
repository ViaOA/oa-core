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
package com.viaoa.query;

/**
 * Defines the integer constants representing the possible token types
 * recognized by the {@link OAQueryTokenizer} and {@link OAQueryTokenManager}.
 * <p>
 * Each constant corresponds to a logical element in the query language
 * (operators, parentheses, string literals, keywords, etc.).
 *
 * <h2>Purpose</h2>
 * <ul>
 *   <li>Provide a stable, shared classification scheme for query parsing.</li>
 *   <li>Ensure type-safe, fast comparisons during lexical analysis.</li>
 *   <li>Support token categories for arithmetic, logical, and structural constructs.</li>
 * </ul>
 *
 * <h2>Common Tokens</h2>
 * <ul>
 *   <li>{@link #EQUAL}, {@link #NOTEQUAL}, {@link #GT}, {@link #LT},
 *       {@link #GE}, {@link #LE} — comparison operators</li>
 *   <li>{@link #AND}, {@link #OR} — logical operators</li>
 *   <li>{@link #LIKE}, {@link #IN} — pattern and set membership</li>
 *   <li>{@link #STRINGSQ}, {@link #STRINGDQ}, {@link #STRINGESC} — literal string forms</li>
 * </ul>
 *
 * @see OAQueryTokenizer
 * @see OAQueryToken
 * @see OAQueryTokenManager
 */
public interface OAQueryTokenType {

	/**
	 * Token type indicating the end of the input stream.
	 * Used by the tokenizer to signal that no more characters remain.
	 */
	public static final int EOF = 1;

	/**
	 * Token type representing a numeric literal.
	 * Identifies integer or decimal values encountered in the query.
	 */
	public static final int NUMBER = 2;
    
	/**
	 * Token type used for generic operators that do not have a
	 * more specific classification.
	 */
	public static final int OPERATOR = 3;
    
	/**
	 * Token type marking the beginning separator of an expression block.
	 * Represents the '(' character.
	 */
	public static final int SEPERATORBEGIN = 4;  // "("  block
    
	/**
	 * Token type marking the ending separator of an expression block.
	 * Represents the ')' character.
	 */
	public static final int SEPERATOREND = 5;    // ")"  block
    
	/**
	 * Token type representing a variable reference within the query.
	 * Typically corresponds to property or field names.
	 */
	public static final int VARIABLE = 7;
    
	/**
	 * Token type representing the '>' (greater than) comparison operator.
	 */
	public static final int GT = 8;
    
	/**
	 * Token type representing the '>=' (greater than or equal) operator.
	 */
	public static final int GE = 9;
    
	/**
	 * Token type representing the '<' (less than) comparison operator.
	 */
	public static final int LT = 10;
    
	/**
	 * Token type representing the '<=' (less than or equal) operator.
	 */
	public static final int LE = 11;
    
	/**
	 * Token type representing the '=' equality comparison operator.
	 */
	public static final int EQUAL = 12;
    
	/**
	 * Token type representing the '<>' or '!=' inequality operator.
	 */
	public static final int NOTEQUAL = 13;
    
	/**
	 * Token type representing the logical AND operator.
	 */
	public static final int AND = 14;
    
	/**
	 * Token type representing the logical OR operator.
	 */
	public static final int OR = 15;
    
	/**
	 * Token type representing the LIKE operator used for pattern matching.
	 */
	public static final int LIKE = 17;
    
	/**
	 * Token type representing the NOT LIKE operator for negated pattern matching.
	 */
	public static final int NOTLIKE = 18;
    
	/**
	 * Token type representing the NULL keyword within queries.
	 */
	public static final int NULL = 19;
    
	/**
	 * Token type representing a single-quoted string literal.
	 */
	public static final int STRINGSQ = 20; // single quote
    
	/**
	 * Token type representing a double-quoted string literal.
	 */
	public static final int STRINGDQ = 21; // double quote
    
	/**
	 * Token type representing an escaped string literal using bracket syntax.
	 */
	public static final int STRINGESC = 22; // escape bracket "{"
    
	/**
	 * Token type representing the boolean literal TRUE.
	 */
	public static final int TRUE = 23;
    
	/**
	 * Token type representing the boolean literal FALSE.
	 */
	public static final int FALSE = 24;
    
	/**
	 * Token type used for a PASS[xxx]THRU construct, allowing sections
	 * of the query to bypass normal parsing rules.
	 */
	public static final int PASSTHRU = 25;  // PASS[xxx]THRU
    
	/**
	 * Token type representing the '?' placeholder symbol.
	 * Often used for parameterized query values.
	 */
	public static final int QUESTION = 26;  // question mark "?"
    
	/**
	 * Token type marking the beginning parenthesis of a SQL function call.
	 */
	public static final int FUNCTIONBEGIN = 27;  // the '(' for a sql function, ex: lower(lastName)
    
	/**
	 * Token type marking the ending parenthesis of a SQL function call.
	 */
	public static final int FUNCTIONEND = 28;    // the ')' for a sql function, ex: lower(lastName)
    
	/**
	 * Token type representing the IN operator for set membership evaluation.
	 */
	public static final int IN = 29; // operator IN(..,..)
    
	/**
	 * Token type representing a comma separator used in lists,
	 * such as within function arguments or IN(...) sets.
	 */
	public static final int COMMA = 30; // separator

}
