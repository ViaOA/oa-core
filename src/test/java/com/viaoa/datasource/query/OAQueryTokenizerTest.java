package com.viaoa.datasource.query;

import static org.junit.Assert.assertEquals;

import java.util.Vector;

import org.junit.Test;

import com.viaoa.OAUnitTest;
import com.viaoa.datasource.query.OAQueryToken;
import com.viaoa.datasource.query.OAQueryTokenType;
import com.viaoa.datasource.query.OAQueryTokenizer;

public class OAQueryTokenizerTest extends OAUnitTest {

	@Test
	public void test() {

		String query = "id IN (1,2, 3, 4 )";

		OAQueryTokenizer t = new OAQueryTokenizer();
		Vector vec = t.convertToTokens(query);

		String sx = "";
		for (int i = 0; i < vec.size(); i++) {
			OAQueryToken tok = (OAQueryToken) vec.get(i);
			if (tok.type == OAQueryTokenType.EOF) {
				break;
			}
			sx += tok.value;
			System.out.print(tok.value);
		}
		System.out.println();
		assertEquals("idIN(1,2,3,4)", sx);
	}

}
