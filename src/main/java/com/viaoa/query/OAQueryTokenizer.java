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

import java.util.Vector;

/*qqqqqqqqqqqqqq
  CODEX
  
 #6 — FIX NOW
  Location: src/main/java/com/viaoa/datasource/query/OAQueryTokenizer.java:127
  Operator parsing recurses into evaluateA() for the right side. This makes comparison parsing permissive in odd
  ways and can accept expressions that should be structurally invalid. Needs unit tests around a = b = c, a > b and
  c, in lists, nested parentheses.
  
  
 */


/**
 * Parses OA object queries into a list of {@link OAQueryToken} elements.
 * <p>
 * {@code OAQueryTokenizer} performs recursive-descent parsing of textual
 * query expressions (e.g., {@code "id in (1,2,3) and status = 'A'"}) into
 * structured token sequences that can be later transformed into a
 * DataSource-specific native query (such as SQL, REST filter, or distributed call).
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Supports nested parentheses, AND/OR precedence, and comparison operators.</li>
 *   <li>Handles quoted string literals, escaped characters, and passthrough blocks.</li>
 *   <li>Recognizes modern SQL constructs such as {@code IN (...)} and function calls
 *       like {@code lower(name)}.</li>
 *   <li>Produces deterministic parse output for reliable translation.</li>
 * </ul>
 *
 * <h2>Design Notes</h2>
 * <ul>
 *   <li>Implements a lightweight recursive descent parser.</li>
 *   <li>Uses {@link OAQueryTokenManager} for lexical scanning.</li>
 *   <li>Returns token streams as {@link java.util.Vector} for backward compatibility.</li>
 *   <li>Throws {@link RuntimeException} on invalid query syntax.</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * OAQueryTokenizer qt = new OAQueryTokenizer();
 * Vector<OAQueryToken> tokens = qt.convertToTokens("lastName = 'Smith' and id in (1,2,3)");
 * }</pre>
 *
 * @see OAQueryToken
 * @see OAQueryTokenManager
 * @see OAQueryTokenType
 */
public class OAQueryTokenizer implements OAQueryTokenType {
	
	/**
	 * Manages lexical scanning of the query string. This instance produces
	 * {@link OAQueryToken} objects consumed by the tokenizer during parsing.
	 */
	OAQueryTokenManager tokenManager;
	
	/**
	 * token: the current token being evaluated during parsing.
	 * lastToken: the previously processed token, used for error reporting and
	 * contextual parsing decisions.
	 */
	OAQueryToken token, lastToken;
	
	/**
	 * The collection into which parsed {@link OAQueryToken} objects are stored.
	 * This vector represents the final token stream returned by the tokenizer.
	 */
	Vector vec;

	/**
	 * Converts the supplied query string into a vector of {@link OAQueryToken}
	 * objects. Initializes the token manager if needed, triggers tokenization,
	 * invokes the parse-evaluation process, and returns the resulting token list.
	 *
	 * @param query the textual query expression to tokenize
	 * @return a vector containing the parsed token sequence
	 */
	public Vector convertToTokens(String query) {
		if (tokenManager == null) {
			tokenManager = new OAQueryTokenManager();
		}
		vec = new Vector(20, 20);
		tokenManager.setQuery(query);
		nextToken();
		evaluate();
		return vec;
	}

	/**
	 * Initiates the top-level parse evaluation. Delegates to {@link #evaluateA()}
	 * and ensures the query ends with an EOF token. Throws a runtime exception
	 * for any remaining unexpected tokens.
	 */
	protected void evaluate() {
		evaluateA();
		if (token.type != OAQueryTokenType.EOF) {
			throw new RuntimeException("unexpected token \"" + token.value + "\" while parsing query " + tokenManager.query);
		}
	}

	/**
	 * Parses logical AND/OR expressions. Evaluates lower-precedence terms via
	 * {@link #evaluateB()}, then recursively processes subsequent AND/OR
	 * operators, appending tokens to the output vector.
	 */
	protected void evaluateA() {
		evaluateB();
		if (token.type == OAQueryTokenType.AND || token.type == OAQueryTokenType.OR) {
			vec.addElement(token);
			nextToken();
			evaluateA();
		}
	}

	// GT, GE, LT, LE, EQUAL, NOTEQUAL, LIKE
	/**
	 * Parses comparison expressions such as =, <> , >, >=, <, <=, LIKE.
	 * First evaluates higher-precedence terms via {@link #evaluateB2()},
	 * then checks for operator tokens and recursively processes the left-hand side.
	 */
	protected void evaluateB() {
		evaluateB2();
		if (token.isOperator()) {
			vec.addElement(token);
			nextToken();
			evaluateA();
		}
	}

	//qqqqqqqqqqqqqq 	(a, b, c, d)  OR ((a,b), (c,d))    OR (?)  OR ?

	//qqqqqq where (date, store_number) in ( ('2021-12-15', 12345), ('2021-10-07', 67890) )
	//qqqqqq where (date, store_number) in (?)
	//         where ? is oaObjKeys[]

	// must match pkey columns

	// 20171222 support for IN(1,2,3)
	// IN
	/**
	 * Parses IN expressions. Evaluates the left-side expression via
	 * {@link #evaluateC()}, then detects and handles the IN operator.
	 * Adds associated tokens to the output vector and delegates recursion.
	 */
	protected void evaluateB2() {
		evaluateC();

		if (token.type != OAQueryTokenType.IN) {
			return;
		}
		vec.addElement(token);

		//qqqqqqq new
		nextToken();
		evaluateA();

		/* was: qqqqqqqqqqqqqqqqqqq old
		for (int i = 0;; i++) {
			nextToken();
			vec.addElement(token);
			if (i == 0) {
				if (token.type != OAQueryTokenType.SEPERATORBEGIN) {
					throw new RuntimeException("IN operator expected begin '(',  query " + tokenManager.query);
				}
			} else if (i % 2 == 0) {
				if (token.type == OAQueryTokenType.SEPERATOREND) {
					break;
				}
				if (token.type != OAQueryTokenType.COMMA) {
					throw new RuntimeException("IN operator expected comma or ')',  query " + tokenManager.query);
				}
			} else {
				if (token.type != OAQueryTokenType.NUMBER) {
					throw new RuntimeException("IN operator expected number,  query " + tokenManager.query);
				}
			}
		}
		nextToken();
			*/
	}

	// () used to surround
	/**
	 * Parses parenthesized expressions. When encountering '(', recursively
	 * evaluates nested expressions while capturing commas and ensuring matching
	 * closing parentheses. Otherwise delegates to {@link #evaluateC2()}.
	 */
	protected void evaluateC() {
		if (token.type == OAQueryTokenType.SEPERATORBEGIN) {
			vec.addElement(token);

			for (;;) {
				nextToken();
				evaluateA();

				//qqqqqqqqqqq allow commas
				if (token.type != OAQueryTokenType.COMMA) {
					break;
				}
				vec.addElement(token);
			}

			if (token.type == OAQueryTokenType.SEPERATOREND) {
				vec.addElement(token);
				nextToken();
			} else {
				throw new RuntimeException("Unbalanced brackets in query " + tokenManager.query);
			}
		} else

		{
			evaluateC2();
		}
	}

	// 20090608 added C2, to allow for sql functions, ex: lower(lastName)
	// () func call
	/**
	 * Parses SQL-style function calls such as lower(name). After evaluating the
	 * function name via {@link #evaluateD()}, repurposes '(' and ')' tokens as
	 * FUNCTIONBEGIN and FUNCTIONEND markers and evaluates the argument list.
	 */
	protected void evaluateC2() {
		evaluateD();
		if (token.type == OAQueryTokenType.SEPERATORBEGIN) {
			token.type = OAQueryTokenType.FUNCTIONBEGIN;
			vec.addElement(token);
			nextToken();

			evaluateA();
			if (token.type == OAQueryTokenType.SEPERATOREND) {
				token.type = OAQueryTokenType.FUNCTIONEND;
				vec.addElement(token);
				nextToken();
			} else {
				throw new RuntimeException("Unbalanced brackets in query " + tokenManager.query);
			}
		}
	}

	// single quotes
	/**
	 * Parses single-quoted string literals. Delegates to {@link #evaluateE()},
	 * captures STRINGSQ tokens, and advances the token stream.
	 */
	protected void evaluateD() {
		evaluateE();
		if (token.type == OAQueryTokenType.STRINGSQ) {
			vec.addElement(token);
			nextToken();
		}
	}

	// Single Quote
	/**
	 * Handles SQL single-quote doubling and repeated string literal sequences.
	 * Delegates to {@link #evaluateF()} and consumes consecutive STRINGSQ tokens.
	 */
	protected void evaluateE() {
		// sql allows for single quotes to be doubled up to show a single quote in string
		evaluateF();
		while (token.type == OAQueryTokenType.STRINGSQ) {
			vec.addElement(token);
			nextToken();
		}
	}

	// VARIABLE, NUMBER, EOF, ?
	/**
	 * Parses atomic token types such as string literals, numbers, variables,
	 * NULL, passthrough blocks, and '?'. Adds them to the output token vector
	 * and advances the stream. Throws an exception on invalid tokens.
	 */
	protected void evaluateF() {
		if ((token.type == OAQueryTokenType.STRINGDQ) ||
				(token.type == OAQueryTokenType.STRINGSQ) ||
				(token.type == OAQueryTokenType.STRINGESC) ||
				(token.type == OAQueryTokenType.NUMBER) ||
				(token.type == OAQueryTokenType.NULL) ||
				(token.type == OAQueryTokenType.PASSTHRU) ||
				(token.type == OAQueryTokenType.VARIABLE) ||
				(token.type == OAQueryTokenType.QUESTION)) {
			vec.addElement(token);
			nextToken();
		} else {
			throw new RuntimeException(
					"Unexpected value in query " + tokenManager.query + " expecting variable or string, received " + token.value);
		}
	}

	/**
	 * Advances to the next token produced by the token manager, preserving the
	 * previous token for contextual reference.
	 */
	protected void nextToken() {
		lastToken = token;
		token = tokenManager.getNext();
	}

	/**
	 * Test method demonstrating tokenization of a string containing embedded
	 * quotes. Not used by the tokenizer itself.
	 *
	 * @param args ignored
	 */
	public static void main2(String[] args) {
		OAQueryTokenizer qt = new OAQueryTokenizer();
		String query = "Code = 'CT13''6\"X16HALF-COL'";
		Vector vec = qt.convertToTokens(query);
		int x = vec.size();
	}

	/**
	 * Demonstration method showing tokenization of various IN expressions and
	 * composite key patterns. Useful for regression testing and examples.
	 *
	 * @param args ignored
	 */
	public static void main(String[] args) {
		OAQueryTokenizer qt = new OAQueryTokenizer();
		String query = "(date, store_number) in ( ('2021-12-15', 12345), ('2021-10-07', 67890) )";
		Vector vec = qt.convertToTokens(query);

		query = "id in (12345, 67890)";
		vec = qt.convertToTokens(query);

		query = "id in (?)";
		vec = qt.convertToTokens(query);

		query = "(date, store_number) in (?)";
		vec = qt.convertToTokens(query);

		query = "(date, store_number) in ?";
		vec = qt.convertToTokens(query);
	}

}
