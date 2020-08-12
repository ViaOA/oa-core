package com.viaoa.datasource.query;

import org.junit.Test;

import com.viaoa.OAUnitTest;
import com.viaoa.datasource.query.OAQueryToken;
import com.viaoa.datasource.query.OAQueryTokenManager;
import com.viaoa.datasource.query.OAQueryTokenType;

public class OAQueryTokenManagerTest extends OAUnitTest {

	@Test
	public void test() {

		testQuery("id = 4");
		testQuery("id = '4'");
		testQuery("id = 'abcXYZ'");

		testQuery("id IN (1,2,3,4)");

	}

	protected void testQuery(String query) {
		OAQueryTokenManager tm = new OAQueryTokenManager();
		tm.setQuery(query);
		for (;;) {
			OAQueryToken tok = tm.getNext();
			if (tok == null) {
				break;
			}
			if (tok.type == OAQueryTokenType.EOF) {
				break;
			}
			System.out.print(tok.value);
		}
		System.out.println();
	}

}
