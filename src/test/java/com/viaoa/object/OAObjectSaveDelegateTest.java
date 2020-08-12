package com.viaoa.object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.viaoa.OAUnitTest;
import com.viaoa.datasource.objectcache.OADataSourceObjectCache;

import test.hifive.model.oa.AwardType;
import test.hifive.model.oa.Company;
import test.hifive.model.oa.Program;

public class OAObjectSaveDelegateTest extends OAUnitTest {
	Company company;
	Program program;
	AwardType at;

	@Test
	public void testSave() {
		reset(false);

		final AtomicInteger ai1 = new AtomicInteger();
		final AtomicInteger ai2 = new AtomicInteger();
		final AtomicInteger ai3 = new AtomicInteger();

		/* save company:
		 * 1: insert company
		   2: insertW/ORef program
		   3: insert awardType
		   4: update program
		
		*/
		dsCache = new OADataSourceObjectCache() {
			@Override
			public boolean supportsStorage() {
				return true;
			}

			@Override
			public void insertWithoutReferences(OAObject obj) {
				assertEquals(1, ai3.get()); // after company insert, before at insert 
				assertEquals(program, obj);
				ai1.incrementAndGet();
				super.insertWithoutReferences(obj);
			}

			@Override
			public void update(OAObject obj) {
				assertEquals(2, ai3.get()); // after company and at are inserted 
				assertEquals(program, obj);
				ai2.incrementAndGet();
				super.update(obj);
			}

			@Override
			public void insert(OAObject object) {
				if (ai3.get() == 0) {
					assertEquals(company, object);
				} else {
					assertEquals(at, object);
					assertEquals(1, ai3.get());
				}

				ai3.incrementAndGet();
				super.insert(object);
			}
		};

		company = new Company();
		assertTrue(company.isNew());
		assertTrue(company.isChanged());
		program = new Program();
		assertTrue(program.isNew());
		assertTrue(program.isChanged());
		at = program.getInspireAwardType();
		assertTrue(at.isNew());
		assertTrue(at.isChanged());
		company.getPrograms().add(program);

		company.save();

		assertFalse(company.isNew());
		assertFalse(company.isChanged());

		assertFalse(program.isNew());
		assertFalse(program.isChanged());

		assertFalse(at.isNew());
		assertFalse(at.isChanged());

		assertEquals(1, ai1.get());
		assertEquals(1, ai2.get());
		assertEquals(2, ai3.get());
	}

}
