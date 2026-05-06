package com.viaoa.query;

import java.util.Vector;

public class OAQuery {

	
	public Vector<OAQueryToken> parse(String query) {
		OAQueryTokenizer qa = new OAQueryTokenizer();
		Vector<OAQueryToken> vecToken = qa.convertToTokens(query);
		return vecToken;
	}
}
