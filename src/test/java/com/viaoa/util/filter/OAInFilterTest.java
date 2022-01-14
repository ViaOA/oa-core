package com.viaoa.util.filter;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.viaoa.OAUnitTest;
import com.viaoa.filter.OAInFilter;

import test.hifive.model.oa.AwardType;
import test.hifive.model.oa.EmployeeAward;
import test.hifive.model.oa.Item;
import test.hifive.model.oa.Product;
import test.hifive.model.oa.propertypath.EmployeeAwardPP;

public class OAInFilterTest extends OAUnitTest {

	@Test
	public void test() throws Exception {
		reset();
		// verify that a Product is in the list of AwardType.includeItems
		EmployeeAward ea = new EmployeeAward();
		AwardType at = new AwardType();
		ea.setAwardType(at);
		OAInFilter filter = new OAInFilter(ea, EmployeeAwardPP.awardType().includeItems().products().pp);

		Item item = new Item();
		at.getIncludeItems().add(item);

		Product prod = new Product();
		item.getProducts().add(prod);

		assertTrue(filter.isUsed(prod));
	}

}
