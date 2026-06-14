package com.viaoa.util.filter;

import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.viaoa.OAUnitTest;
import com.viaoa.filter.OAQueryFilter;

import test.xice.tsac3.model.oa.Server;

public class OAQueryFilterTest extends OAUnitTest {

	@Test
	public void test() {
		String query = "id IN (1,2 ,3,4)";
		OAQueryFilter<Server> f = new OAQueryFilter<>(Server.class, query);
		Server server = new Server();
		server.setId(5);
		f.isUsed(server);
	}

	@Test
	public void test2() {
		String query = "Id = 1";
		query = "id == 1 && serverFromId = 2";
		query = "(id == 1) && serverFromId = 2";
		query = "id == 1 || id = 2 && serverFromId == 3";
		query = "id == 1 && serverFromId = 2 && id == 3";
		query = "id == 1 && (serverFromId = 2 && id == 3)";

		query = "(id == '1' && (serverFromId = 2 && (id == 3))) || id = 5 && serverFromId = 9 || id in (1,2, 3, 4)";

		OAQueryFilter qf = new OAQueryFilter(Server.class, query, null);
		int xx = 4;
		xx++;
	}

	@Test
	public void parseCompoundInTest() {
		OAQueryFilter qf;

		String query = "(id) IN (1,2,3,4)";
		query = "id IN (1,2,3,4)";
		qf = new OAQueryFilter(Server.class, query, null);

		// fail
		query = "(id) IN ((1),(2),(3),(4))";
		try {
			qf = new OAQueryFilter(Server.class, query, null);
			assertFalse("should throw an exception", true);
		} catch (Exception ex) {
		}

		// fail
		query = "id IN ((1),(2),(3),(4))";
		try {
			qf = new OAQueryFilter(Server.class, query, null);
			assertFalse("should throw an exception", true);
		} catch (Exception ex) {
		}

		query = "(id, date) IN ((1, '2022/01/01'),(2, '2022/01/02'),(3, '2022/01/03'),(4, '2022/01/04'))";
		qf = new OAQueryFilter(Server.class, query, null);
	}

	@Test
	public void parseInWithArgs() {
		String query = "id IN ?";

		List<Integer> list = new ArrayList<>();
		list.add(1);
		list.add(2);
		list.add(3);
		list.add(4);
		OAQueryFilter qf = new OAQueryFilter(Server.class, query, new Object[] { list });

		int xx = 4;
		xx++;
	}

	@Test
	public void parseInCompoundKeyAndArgs() {
		/*qqqq
		String query = "id IN ?";
		
		List<OAObjectKey> list = new ArrayList<>();
		OAObjectKey ok = new OAObjectKey(1);
		list.add(ok);
		ok = new OAObjectKey(2);
		list.add(ok);
		ok = new OAObjectKey(3);
		list.add(ok);
		OAQueryFilter qf = new OAQueryFilter(Server.class, query, new Object[] { list });
		 */
	}
}
