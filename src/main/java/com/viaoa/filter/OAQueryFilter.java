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
package com.viaoa.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import com.viaoa.datasource.query.OAQueryToken;
import com.viaoa.datasource.query.OAQueryTokenType;
import com.viaoa.datasource.query.OAQueryTokenizer;
import com.viaoa.graph.OAGraphImpl;
import com.viaoa.graph.service.object.OAObjectInfoService;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.runtime.OARuntime;
import com.viaoa.util.OAConv;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAPropertyPath;

/**
 * Compiles an object-query expression into an {@link OAFilter} tree that can be
 * evaluated against OAObjects or used by a Hub for in-memory filtering.  
 *
 * <p>
 * OAQueryFilter provides a lightweight OQL/SQL-style query language that
 * supports property paths, nested expressions, relational operators,
 * logical operators, LIKE matching, and both single-column and multi-column
 * IN conditions.  The resulting filter behaves exactly like any other
 * OAFilter and can be reused across Hubs or selection operations.
 * </p>
 *
 * <h3>Supported expression features</h3>
 * <ul>
 *   <li>Property names and nested {@link OAPropertyPath} expressions</li>
 *   <li>Comparison operators: 
 *       =, !=, &lt;, &lt;=, &gt;, &gt;=</li>
 *   <li>LIKE and NOTLIKE (wildcard matching)</li>
 *   <li>Logical AND and OR with correct precedence</li>
 *   <li>Parentheses for grouping</li>
 *   <li>Single-value IN lists</li>
 *   <li>Composite IN conditions such as:
 *       <pre>(a,b) IN (('x',1),('y',2))</pre></li>
 *   <li>IN (?) where the parameter is a List of values or List of OAObjectKey</li>
 *   <li>NULL and parameter substitution via {@code ?}</li>
 * </ul>
 *
 * <h3>How it works</h3>
 * The query string is tokenized by {@link OAQueryTokenizer} and parsed using
 * a recursive-descent grammar.  Each operator is translated into the
 * corresponding {@link OAFilter} implementation (e.g., {@link OAEqualFilter},
 * {@link OAGreaterFilter}, {@link OALikeFilter}, etc.).  The resulting filter
 * tree is stored internally and invoked through {@link #isUsed(Object)}.
 *
 * <p>
 * When a property path traverses a many-relationship, the parser automatically
 * constructs an {@link com.viaoa.util.OAFinder} so that the filter applies to
 * the located target object, matching the standard behavior of OAFilter
 * property-path evaluation.
 * </p>
 *
 * <h3>Usage</h3>
 * <pre>
 * OAQueryFilter&lt;Customer&gt; f =
 *     new OAQueryFilter<>(Customer.class, "lastName LIKE 'S*' AND age >= 18");
 *
 * hubCustomers.setFilter(f);
 * </pre>
 *
 * <h3>Error handling</h3>
 * Syntax errors, unmatched parentheses, missing operands, or invalid
 * parameter types produce a {@link RuntimeException} with diagnostic
 * information.  This ensures that query problems are caught at construction
 * time rather than during evaluation.
 *
 * <p>
 * This class is used extensively by OADataSourceObjectCache and other
 * components that convert query expressions into reusable filters.
 * </p>
 */
public class OAQueryFilter<T> implements OAFilter<T> {
    private static final long serialVersionUID = 1L;

    /**
     * The root class type for which the query expression is evaluated.
     * Used when creating {@link OAPropertyPath} instances.
     */
	private Class<T> clazz;

	/**
	 * The raw query expression string to be parsed into a filter tree.
	 */
	private String query;
	
	/**
	 * Optional argument array used to supply runtime parameters for
	 * placeholder tokens represented by '?' in the query.
	 */
	private Object[] args;
	
	/**
	 * Tracks the next positional argument to consume from the {@link #args}
	 * array during parsing.
	 */
	private int posArgs;

	// root filter for query
	/**
	 * The root filter generated from parsing the query expression.
	 * Invoked during evaluation via {@link #isUsed(Object)}.
	 */
	private OAFilter filter;

	/**
	 * Working stack used by the parser to construct composite filter trees
	 * based on operator precedence and grouping.
	 */
	private Stack<OAFilter> stack = new Stack<OAFilter>();
	
	/**
	 * The list of tokens produced by {@link OAQueryTokenizer} for the
	 * current query expression.
	 */
	private Vector vecToken;
	
	/**
	 * Current position in the token vector during recursive-descent parsing.
	 */
	private int posToken;

	/**
	 * Constructs a query filter using the specified class and query text,
	 * without placeholder arguments.
	 *
	 * @param clazz the class for which the filter is evaluated
	 * @param query the query expression to parse
	 */
	public OAQueryFilter(Class<T> clazz, String query) {
		this(clazz, query, null);
	}

	/**
	 * Constructs a query filter using the specified class, query text, and
	 * optional argument array for '?' placeholders. Parsing occurs
	 * immediately, and syntax problems result in a {@link RuntimeException}.
	 *
	 * @param clazz the class for which the filter is evaluated
	 * @param query the query expression to parse
	 * @param args  optional argument array for placeholder substitution
	 */
	public OAQueryFilter(Class<T> clazz, String query, Object[] args) {
		this.clazz = clazz;
		this.query = query;
		this.args = args;

		try {
			this.filter = parse();
		} catch (Exception e) {
			throw new RuntimeException("invalid query filter, query=" + query, e);
		}
		if (stack.size() != 0) {
			throw new RuntimeException("parse failed, filters not all used, remainder=" + stack.size());
		}
	}

	/**
	 * Tokenizes the query string and parses the token sequence into a
	 * corresponding filter tree.
	 *
	 * @return the root {@link OAFilter} generated from the query
	 * @throws Exception if tokenization or parsing fails
	 */
	private OAFilter parse() throws Exception {
		OAQueryTokenizer qa = new OAQueryTokenizer();
		vecToken = qa.convertToTokens(query);
		OAFilter f = parseBlock();
		return f;
	}

	/**
	 * Parses a grouped expression block beginning at the current token.
	 * Handles nested expressions and enforces that at least one filter is
	 * produced within the block.
	 *
	 * @return the filter produced from the block
	 * @throws Exception if the block is syntactically invalid
	 */
	private OAFilter parseBlock() throws Exception {
		OAQueryToken token = nextToken();
		if (token == null) {
			throw new Exception("token is null");
		}
		parseForConjuction(token);

		if (stack.size() == 0) {
			if (alInTokens != null && alInTokens.size() > 0) {
				return null;
			}
			throw new Exception("Block failed, no filter in stack");
		}
		OAFilter fi = stack.pop();
		return fi;
	}

	/**
	 * Parses logical conjunction operators by delegating to AND processing.
	 *
	 * @param token the current token
	 * @return the next token to process
	 * @throws Exception if parsing fails
	 */
	private OAQueryToken parseForConjuction(OAQueryToken token) throws Exception {
		if (token == null) {
			return null;
		}
		return parseForAnd(token);
	}

	// AND
	/**
	 * Parses AND operators and constructs corresponding {@link OAAndFilter}
	 * nodes from the filter stack. Recursively processes chained AND
	 * sequences.
	 *
	 * @param token the current token
	 * @return the next token after processing all AND operators
	 * @throws Exception if required operands are missing
	 */
	private OAQueryToken parseForAnd(OAQueryToken token) throws Exception {
		if (token == null || token.type != OAQueryTokenType.AND) {
			token = parseForOr(token);
		}
		if (token != null && token.type == OAQueryTokenType.AND) {
			if (stack.size() == 0) {
				throw new Exception("AND failed, no filter in stack");
			}
			OAFilter f1 = stack.pop();

			token = nextToken();
			token = parseForBracket(token);
			if (stack.size() == 0) {
				throw new Exception("AND failed, no filter in stack");
			}
			OAFilter f2 = stack.pop();

			OAFilter f = new OAAndFilter(f1, f2);
			stack.push(f);

			token = parseForConjuction(token);
		}
		return token;
	}

	// OR
	/**
	 * Parses OR operators and constructs {@link OAOrFilter} nodes by
	 * combining previously parsed filter expressions. Recursively processes
	 * chained OR sequences.
	 *
	 * @param token the current token
	 * @return the next token after OR processing
	 * @throws Exception if operands are missing
	 */
	private OAQueryToken parseForOr(OAQueryToken token) throws Exception {
		if (token == null || token.type != OAQueryTokenType.OR) {
			token = parseForBracket(token);
		}
		if (token != null && token.type == OAQueryTokenType.OR) {
			if (stack.size() == 0) {
				throw new Exception("OR failed, no filter in stack");
			}
			OAFilter f1 = stack.pop();

			token = nextToken();
			token = parseForBracket(token);
			if (stack.size() == 0) {
				throw new Exception("OR failed, no filter in stack");
			}
			OAFilter f2 = stack.pop();

			OAFilter f = new OAOrFilter(f1, f2);

			stack.push(f);

			token = parseForConjuction(token);
		}
		return token;
	}

	// ()
	/**
	 * Parses a bracketed subexpression "( ... )" and handles compound IN
	 * list syntax when multiple property names are enclosed.
	 *
	 * @param token the current token
	 * @return the next token following the closing bracket
	 * @throws Exception if parentheses or IN structure is invalid
	 */
	private OAQueryToken parseForBracket(OAQueryToken token) throws Exception {
		OAQueryToken nextToken;
		if (token.type != OAQueryTokenType.SEPERATORBEGIN) {
			nextToken = parseForEndBracket(token);
			return nextToken;
		}

		OAFilter fi = parseBlock();
		if (fi != null) {
			stack.push(fi);
		}

		nextToken = nextToken();

		if (alInTokens != null && alInTokens.size() > 0) {
			// IN using more then one property name in brackets, ex:  "(orderId, itemId) IN ?"

			if (nextToken == null || nextToken.type != OAQueryTokenType.IN) {
				throw new Exception("token type expected to be IN");
			}

			nextToken = parseCompoundIn(null);
			alInTokens.clear();
		}

		return nextToken;
	}

	/**
	 * Continues parsing after encountering a closing bracket, delegating to
	 * the next comparison-level parser.
	 *
	 * @param token the current token
	 * @return the next token to process
	 * @throws Exception if parsing fails
	 */
	private OAQueryToken parseForEndBracket(OAQueryToken token) throws Exception {
		if (token.type == OAQueryTokenType.SEPERATOREND) {
			return token;
		}
		OAQueryToken nextToken = parseForEqual(token);
		return nextToken;
	}

	// Operators begin

	// ==
	/**
	 * Parses equality expressions and creates an {@link OAEqualFilter}
	 * using the identified property path and comparison value. Case is
	 * ignored by default.
	 *
	 * @param token the current token representing a property name
	 * @return the next token after the equality expression
	 * @throws Exception if an operand is missing
	 */
	private OAQueryToken parseForEqual(OAQueryToken token) throws Exception {
		OAQueryToken nextToken = parseForNotEqual(token);
		if (nextToken != null && nextToken.type == OAQueryTokenType.EQUAL) {
			nextToken = nextToken();
			if (nextToken == null) {
				throw new Exception("token expected for =");
			}

			OAPropertyPath pp = new OAPropertyPath(clazz, token.value);

			OAEqualFilter f = new OAEqualFilter(pp, getValueToUse(nextToken));
			f.setIgnoreCase(true); // might want to make false, and then create a new "LIKE" operator
			stack.push(f);
			nextToken = nextToken();
		}
		return nextToken;
	}

	// get correct value
	/**
	 * Resolves the literal value associated with a token, performing
	 * placeholder substitution for '?' and mapping NULL tokens to
	 * {@code null}.
	 *
	 * @param token the token providing the value reference
	 * @return the resolved comparison value
	 */
	private Object getValueToUse(OAQueryToken token) {
		if (token == null) {
			return null;
		}
		Object val = token.value;
		if ("?".equals(val)) {
			if (args != null && posArgs < args.length) {
				val = args[posArgs++];
			}
		} else if (token.type == OAQueryToken.NULL) {
			val = null;
		}
		return val;
	}

	// !=
	/**
	 * Parses not-equal ("!=" or "<>") expressions and constructs an
	 * {@link OANotEqualFilter} using the identified property path and
	 * comparison value. Case is ignored by default.
	 *
	 * @param token the current token representing a property name
	 * @return the next token following the not-equal expression
	 * @throws Exception if required operands are missing
	 */
	private OAQueryToken parseForNotEqual(OAQueryToken token) throws Exception {
		OAQueryToken nextToken = parseForGreater(token);
		if (nextToken != null && nextToken.type == OAQueryTokenType.NOTEQUAL) {
			nextToken = nextToken();
			if (nextToken == null) {
				throw new Exception("token expected for !=");
			}
			OAPropertyPath pp = new OAPropertyPath(clazz, token.value);
			OAFilter f = new OANotEqualFilter(pp, getValueToUse(nextToken), true);
			stack.push(f);
			nextToken = nextToken();
		}
		return nextToken;
	}

	// >
	/**
	 * Parses greater-than (">") expressions and creates an
	 * {@link OAGreaterFilter} using the parsed property path and value.
	 *
	 * @param token the current property token
	 * @return the next token after the greater-than expression
	 * @throws Exception if an operand is missing
	 */
	private OAQueryToken parseForGreater(OAQueryToken token) throws Exception {
		OAQueryToken nextToken = parseForGreaterOrEqual(token);
		if (nextToken != null && nextToken.type == OAQueryTokenType.GT) {
			nextToken = nextToken();
			if (nextToken == null) {
				throw new Exception("token expected for !=");
			}
			OAPropertyPath pp = new OAPropertyPath(clazz, token.value);
			OAFilter f = new OAGreaterFilter(pp, getValueToUse(nextToken));
			stack.push(f);
			nextToken = nextToken();
		}
		return nextToken;
	}

	// >=
	/**
	 * Parses ">=" expressions and creates an {@link OAGreaterOrEqualFilter}
	 * using the identified property path and comparison value.
	 *
	 * @param token the current property token
	 * @return the next token after the ">=" expression
	 * @throws Exception if an operand is missing
	 */
	private OAQueryToken parseForGreaterOrEqual(OAQueryToken token) throws Exception {
		OAQueryToken nextToken = parseForLess(token);
		if (nextToken != null && nextToken.type == OAQueryTokenType.GE) {
			nextToken = nextToken();
			if (nextToken == null) {
				throw new Exception("token expected for !=");
			}
			OAPropertyPath pp = new OAPropertyPath(clazz, token.value);
			OAFilter f = new OAGreaterOrEqualFilter(pp, getValueToUse(nextToken));
			stack.push(f);
			nextToken = nextToken();
		}
		return nextToken;
	}

	// <
	/**
	 * Parses less-than ("<") expressions and creates an {@link OALessFilter}
	 * for the property path and comparison value.
	 *
	 * @param token the current property token
	 * @return the next token after the less-than expression
	 * @throws Exception if an operand is missing
	 */
	private OAQueryToken parseForLess(OAQueryToken token) throws Exception {
		OAQueryToken nextToken = parseForLessOrEqual(token);
		if (nextToken != null && nextToken.type == OAQueryTokenType.LT) {
			nextToken = nextToken();
			if (nextToken == null) {
				throw new Exception("token expected for !=");
			}
			OAPropertyPath pp = new OAPropertyPath(clazz, token.value);
			OAFilter f = new OALessFilter(pp, getValueToUse(nextToken));
			stack.push(f);
			nextToken = nextToken();
		}
		return nextToken;
	}

	// <=
	/**
	 * Parses "<=" expressions and constructs an
	 * {@link OALessOrEqualFilter} instance using the parsed property path
	 * and value.
	 *
	 * @param token the current property token
	 * @return the next token after the "<=" expression
	 * @throws Exception if an operand is missing
	 */
	private OAQueryToken parseForLessOrEqual(OAQueryToken token) throws Exception {
		OAQueryToken nextToken = parseForLike(token);
		if (nextToken != null && nextToken.type == OAQueryTokenType.LE) {
			nextToken = nextToken();
			if (nextToken == null) {
				throw new Exception("token expected for !=");
			}

			OAPropertyPath pp = new OAPropertyPath(clazz, token.value);
			OAFilter f = new OALessOrEqualFilter(pp, getValueToUse(nextToken));
			stack.push(f);
			nextToken = nextToken();
		}
		return nextToken;
	}

	// LIKE
	/**
	 * Parses LIKE expressions and creates an {@link OALikeFilter} using the
	 * specified property path and pattern value.
	 *
	 * @param token the current property-name token
	 * @return the next token after the LIKE expression
	 * @throws Exception if a pattern value is missing
	 */
	private OAQueryToken parseForLike(OAQueryToken token) throws Exception {
		OAQueryToken nextToken = parseForNotLike(token);
		if (nextToken != null && nextToken.type == OAQueryTokenType.LIKE) {
			nextToken = nextToken();
			if (nextToken == null) {
				throw new Exception("token expected for !=");
			}

			OAPropertyPath pp = new OAPropertyPath(clazz, token.value);
			OAFilter f = new OALikeFilter(pp, getValueToUse(nextToken));
			stack.push(f);
			nextToken = nextToken();
		}
		return nextToken;
	}

	// NOTLIKE
	/**
	 * Parses NOT LIKE expressions and creates an {@link OANotLikeFilter}
	 * using the specified property path and pattern value.
	 *
	 * @param token the current property-name token
	 * @return the next token after the NOT LIKE expression
	 * @throws Exception if a pattern value is missing
	 */
	private OAQueryToken parseForNotLike(OAQueryToken token) throws Exception {
		OAQueryToken nextToken = parseForInMutiplePropertyNames(token);
		//was: OAQueryToken nextToken = parseForIn(token);
		if (nextToken != null && nextToken.type == OAQueryTokenType.NOTLIKE) {
			nextToken = nextToken();
			if (nextToken == null) {
				throw new Exception("token expected for NotLike");
			}
			OAPropertyPath pp = new OAPropertyPath(clazz, token.value);
			OAFilter f = new OANotLikeFilter(pp, getValueToUse(nextToken));
			stack.push(f);
			nextToken = nextToken();
		}
		return nextToken;
	}

	/**
	 * Temporary list used to store property-name tokens for multi-column
	 * IN expressions.
	 */
	private ArrayList<OAQueryToken> alInTokens = new ArrayList<>();

	// Comma separated list (used by IN)
	/**
	 * Parses comma-separated property names used in multi-column IN
	 * expressions, storing each in {@link #alInTokens}.
	 *
	 * @param token the first property-name token
	 * @return the next token after the property-name list
	 * @throws Exception if syntax is invalid
	 */
	private OAQueryToken parseForInMutiplePropertyNames(OAQueryToken token) throws Exception {
		OAQueryToken nextToken = parseForIn(token);
		for (;;) {
			if (nextToken == null || nextToken.type != OAQueryTokenType.COMMA) {
				if (alInTokens.size() > 0) {
					alInTokens.add(token);
				}
				break;
			}
			alInTokens.add(token);

			nextToken = nextToken();

			if (nextToken == null) {
				throw new Exception("token expected for Comma");
			}
			if (nextToken.type != OAQueryTokenType.VARIABLE) {
				throw new Exception("token expected to be a Variable");
			}

			token = nextToken;
			nextToken = nextToken(); // next comma (optional), or end bracket ")"

			if (nextToken == null) {
				throw new Exception("token expected after Comma");
			}
		}
		return nextToken;
	}

	// IN
	/**
	 * Parses IN expressions and delegates to {@link #parseIn(OAQueryToken)}
	 * when an IN operator is detected.
	 *
	 * @param token the property-name token
	 * @return the next token after processing the IN operator
	 * @throws Exception if parsing fails
	 */
	private OAQueryToken parseForIn(OAQueryToken token) throws Exception {
		OAQueryToken nextToken = parseBottom(token);
		if (nextToken != null && nextToken.type == OAQueryTokenType.IN) {
			nextToken = parseIn(token);
		}
		return nextToken;
	}

	/**
	 * Parses a standard IN clause, constructing a chain of {@code OAEqualFilter}
	 * objects connected by {@code OAOrFilter}. Supports '?' parameter lists.
	 *
	 * @param token the property-name token
	 * @return the next token after the IN list
	 * @throws Exception if argument or value syntax is invalid
	 */
	private OAQueryToken parseIn(OAQueryToken token) throws Exception {
		OAPropertyPath pp = new OAPropertyPath(clazz, token.value);
		OAQueryToken nextToken = null;

		OAFilter f = null;
		for (int i = 0;; i++) {
			nextToken = nextToken();
			if (nextToken.type == OAQueryTokenType.SEPERATOREND) {
				break;
			}
			if (nextToken.type == OAQueryTokenType.SEPERATORBEGIN) {
				continue;
			}
			if (nextToken.type == OAQueryTokenType.COMMA) {
				continue;
			}

			if (nextToken.type == OAQueryTokenType.QUESTION) {
				Object arg = getValueToUse(nextToken);

				if (!(arg instanceof List)) {
					throw new IllegalArgumentException("Argument for ? is expected to be a List of key values");
				}
				List list = (List) arg;

				for (Object objx : list) {
					OAFilter fx = new OAEqualFilter(pp, objx);
					if (f == null) {
						f = fx;
					} else {
						f = new OAOrFilter(f, fx);
					}
				}
				break;
			}

			OAFilter fx = new OAEqualFilter(pp, getValueToUse(nextToken));
			if (f == null) {
				f = fx;
			} else {
				f = new OAOrFilter(f, fx);
			}
		}
		stack.push(f);
		nextToken = nextToken();
		return nextToken;
	}

	/**
	 * Parses a compound IN expression where multiple property names map to
	 * multiple-column value tuples, creating {@code OABlockFilter} instances
	 * and combining them with OR.
	 *
	 * @param token not used; present for signature compatibility
	 * @return the next token after the compound IN clause
	 * @throws Exception if the structure or values are invalid
	 */
	private OAQueryToken parseCompoundIn(OAQueryToken token) throws Exception {
		int bracketCount = 0;
		int commaCount = 0;
		OAQueryToken nextToken = null;
		OAFilter f = null;
		ArrayList<OAFilter> alFilter = null;

		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(clazz);
		final OAObjectInfoService srvcObjectInfo = og.getOAObjectService().getOAObjectInfoService();
		final OAObjectInfo oi = srvcObjectInfo.getOAObjectInfo(clazz);

		for (int i = 0;; i++) {
			nextToken = nextToken();
			if (nextToken.type == OAQueryTokenType.SEPERATOREND) {
				bracketCount--;
				if (bracketCount == 0) {
					break;
				}
				continue;
			}
			if (nextToken.type == OAQueryTokenType.SEPERATORBEGIN) {
				bracketCount++;
				commaCount = 0;
				continue;
			}

			if (nextToken.type == OAQueryTokenType.COMMA) {
				commaCount++;
				continue;
			}

			if (nextToken.type == OAQueryTokenType.QUESTION) {
				Object arg = getValueToUse(nextToken);

				// expects to be a List of OAObjectKey
				if (!(arg instanceof List)) {
					throw new IllegalArgumentException("Argument for ? is expected to be a List of OAObjectKey");
				}
				List list = (List) arg;

				for (Object objx : list) {
					if (!(objx instanceof OAObjectKey)) {
						throw new IllegalArgumentException("Argument for ? is expected to be a List of OAObjectKey");
					}
					OAObjectKey ok = (OAObjectKey) objx;

					alFilter = new ArrayList();
					int pos = 0;
					for (OAQueryToken qt : alInTokens) {
						// OAPropertyInfo pi = oi.getPropertyInfo(qt.value);
						OAPropertyPath pp = new OAPropertyPath(qt.value);
						OAFilter fx = new OAEqualFilter(pp, ok.getObjectIds()[pos++]);
						alFilter.add(fx);
					}

					OAFilter[] fs = new OAFilter[alInTokens.size()];
					alFilter.toArray(fs);
					alFilter = null;
					OAFilter fx = new OABlockFilter(fs);
					if (f == null) {
						f = fx;
					} else {
						f = new OAOrFilter(f, fx);
					}
				}
				break;
			}

			OAQueryToken tokx = alInTokens.get(commaCount);

			OAPropertyPath pp = new OAPropertyPath(tokx.value);

			Object objx = getValueToUse(nextToken);

			OAPropertyInfo pi = oi.getPropertyInfo(tokx.value);
			if (pi != null) {
				objx = OAConv.convert(pi.getClassType(), objx);
			}

			OAFilter fx = new OAEqualFilter(pp, objx);

			if (f == null && (alInTokens.size() == 1)) {
				f = fx;
			} else {
				if (alInTokens.size() == 1) {
					f = new OAOrFilter(f, fx);
				} else {
					if (alFilter == null) {
						alFilter = new ArrayList();
					}
					alFilter.add(fx);

					if (commaCount + 1 == alInTokens.size()) {
						OAFilter[] fs = new OAFilter[alInTokens.size()];
						alFilter.toArray(fs);
						alFilter = null;
						fx = new OABlockFilter(fs);
						if (f == null) {
							f = fx;
						} else {
							f = new OAOrFilter(f, fx);
						}
					}
				}
			}
		}
		stack.push(f);
		nextToken = nextToken();
		return nextToken;
	}

	/**
	 * Lowest-level parser that simply returns the next token.
	 *
	 * @param token ignored
	 * @return the next token
	 * @throws Exception never thrown in current implementation
	 */
	private OAQueryToken parseBottom(OAQueryToken token) throws Exception {
		return nextToken();
	}

	/**
	 * Retrieves and advances to the next token in {@link #vecToken}.
	 *
	 * @return the next {@code OAQueryToken}, or {@code null} if at end
	 */
	private OAQueryToken nextToken() {
		if (vecToken == null || posToken >= vecToken.size()) {
			return null;
		}
		OAQueryToken t = (OAQueryToken) vecToken.elementAt(posToken++);
		return t;
	}

	/**
	 * Evaluates the root filter against the supplied object.
	 *
	 * @param obj the object to evaluate
	 * @return true if the filter evaluates to true; false otherwise
	 */
	@Override
	public boolean isUsed(Object obj) {
		if (filter != null) {
			return filter.isUsed(obj);
		}
		/*was
		try {
		    if (filter != null) return filter.isUsed(obj);
		}
		catch (RuntimeException re) {
		    throw re;
		}
		catch (Exception e) {
		    System.out.println(e);
		    e.printStackTrace();
		}
		*/
		return false;
	}

	/**
	 * Test method demonstrating compound IN usage and constructor invocation.
	 *
	 * @param args not used
	 * @throws Exception if parsing fails
	 */
	public static void main2(String[] args) throws Exception {
		String query = "A = 1";
		query = "A == 1 && B = 2";
		query = "(A == 1) && B = 2";
		query = "A == 1 || B = 2 && C == 3";
		query = "A == 1 && B = 2 && C == 3";
		query = "A == 1 && (B = 2 && C == 3)";

		query = "(A == '1' && (B = 2 && (C == 3))) || X = 5 && Z = 9 || id in (1,2, 3, 4)";

		OAQueryFilter qf = new OAQueryFilter(Object.class, query, null);
		int xx = 4;
		xx++;
	}

	/**
	 * Test method demonstrating compound IN usage and constructor invocation.
	 *
	 * @param args not used
	 * @throws Exception if parsing fails
	 */
	public static void main(String[] args) throws Exception {
		OAQueryFilter qf;
		String query = "(date, store_number) in ( ('2021-12-15', 12345), ('2021-10-07', 67890) )";
		// qf = new OAQueryFilter(Object.class, query, null);

		query = "(date, store_number) in (?)";
		List list = new ArrayList();
		// list.add(new OAObjectKey());
		// list.add(new OAObjectKey());
		qf = new OAQueryFilter(Object.class, query, new Object[] { list });

		int xx = 4;
		xx++;
	}

}
