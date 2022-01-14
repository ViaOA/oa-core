/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.viaoa.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import com.viaoa.datasource.query.OAQueryToken;
import com.viaoa.datasource.query.OAQueryTokenType;
import com.viaoa.datasource.query.OAQueryTokenizer;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.util.OAConv;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAPropertyPath;

/**
 * Convert an Object query to an OAFilter. This can be used for Hub selects, etc. It is used by OADataSourceObjectCache.selects created
 * 20140127, expanded 201511125
 *
 * @author vvia
 */
public class OAQueryFilter<T> implements OAFilter {
	private Class<T> clazz;
	private String query;
	private Object[] args;
	private int posArgs;

	// root filter for query
	private OAFilter filter;

	private Stack<OAFilter> stack = new Stack<OAFilter>();
	private Vector vecToken;
	private int posToken;

	public OAQueryFilter(Class<T> clazz, String query) {
		this(clazz, query, null);
	}

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

	private OAFilter parse() throws Exception {
		OAQueryTokenizer qa = new OAQueryTokenizer();
		vecToken = qa.convertToTokens(query);
		OAFilter f = parseBlock();
		return f;
	}

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

	private OAQueryToken parseForConjuction(OAQueryToken token) throws Exception {
		if (token == null) {
			return null;
		}
		return parseForAnd(token);
	}

	// AND
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

	private OAQueryToken parseForEndBracket(OAQueryToken token) throws Exception {
		if (token.type == OAQueryTokenType.SEPERATOREND) {
			return token;
		}
		OAQueryToken nextToken = parseForEqual(token);
		return nextToken;
	}

	// Operators begin

	// ==
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

	private ArrayList<OAQueryToken> alInTokens = new ArrayList<>();

	// Comma separated list (used by IN)
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

	// 20171222
	// IN
	private OAQueryToken parseForIn(OAQueryToken token) throws Exception {
		OAQueryToken nextToken = parseBottom(token);
		if (nextToken != null && nextToken.type == OAQueryTokenType.IN) {
			nextToken = parseIn(token);
		}
		return nextToken;
	}

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

	private OAQueryToken parseCompoundIn(OAQueryToken token) throws Exception {
		int bracketCount = 0;
		int commaCount = 0;
		OAQueryToken nextToken = null;
		OAFilter f = null;
		ArrayList<OAFilter> alFilter = null;

		final OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);

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

	private OAQueryToken parseBottom(OAQueryToken token) throws Exception {
		return nextToken();
	}

	private OAQueryToken nextToken() {
		if (vecToken == null || posToken >= vecToken.size()) {
			return null;
		}
		OAQueryToken t = (OAQueryToken) vecToken.elementAt(posToken++);
		return t;
	}

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

	public static void main(String[] args) throws Exception {
		OAQueryFilter qf;
		String query = "(date, store_number) in ( ('2021-12-15', 12345), ('2021-10-07', 67890) )";
		// qf = new OAQueryFilter(Object.class, query, null);

		query = "(date, store_number) in (?)";
		List list = new ArrayList();
		list.add(new OAObjectKey());
		list.add(new OAObjectKey());
		qf = new OAQueryFilter(Object.class, query, new Object[] { list });

		int xx = 4;
		xx++;
	}

}
