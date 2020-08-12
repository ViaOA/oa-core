package com.viaoa.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.MathContext;

import org.junit.Test;

import com.viaoa.OAUnitTest;
import com.viaoa.json.node.OAJsonArrayNode;
import com.viaoa.json.node.OAJsonNode;
import com.viaoa.json.node.OAJsonNumberNode;
import com.viaoa.json.node.OAJsonObjectNode;
import com.viaoa.json.node.OAJsonRootNode;
import com.viaoa.json.node.OAJsonStringNode;
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAFile;

/**
 * OAJson unit tests.<br>
 * Uses sample files in resource for comparing and validating.
 *
 * @author vvia
 */
public class OAJsonTest extends OAUnitTest {

	// formatting was validated using:  https://www.freeformatter.com/json-formatter.html

	@Test
	public void json1Test() throws Exception {
		OAJson oaJson = new OAJson();

		String txt = OAFile.readTextFile(OAJson.class, "json1.txt", 1024 * 2);
		OAJsonRootNode root = oaJson.load(txt);

		String json = root.toJson(2);

		boolean b = json.equals(txt);
		assertEquals(txt, json);
	}

	@Test // add obj property
	public void json2Test() throws Exception {
		OAJson oaJson = new OAJson();

		String txt = readJsonFile(1);
		OAJsonRootNode root = oaJson.load(txt);

		assertTrue(root instanceof OAJsonObjectNode);

		root.set("middle", "M");

		String txt2 = readJsonFile(2);

		String json = root.toJson(2);

		boolean b = json.equals(txt2);
		assertEquals(txt2, json);
	}

	@Test // remove obj property
	public void json3Test() throws Exception {
		OAJson oaJson = new OAJson();

		String txt1 = readJsonFile(1);
		String txt2 = readJsonFile(2);

		OAJsonRootNode root2 = oaJson.load(txt2);

		((OAJsonObjectNode) root2).remove("middle");

		String json = root2.toJson(2);

		boolean b = json.equals(txt1);
		assertEquals(txt1, json);
	}

	@Test // add array property
	public void json4Test() throws Exception {
		OAJson oaJson = new OAJson();

		String txt2 = readJsonFile(2);
		OAJsonRootNode root2 = oaJson.load(txt2);
		assertTrue(root2 instanceof OAJsonObjectNode);

		((OAJsonObjectNode) root2).set("phones", new OAJsonArrayNode());
		String json = root2.toJson(2);

		String txt3 = readJsonFile(3);

		boolean b = json.equals(txt3);
		assertEquals(json, txt3);
	}

	@Test // add value to array
	public void json5Test() throws Exception {
		OAJson oaJson = new OAJson();

		String txt3 = readJsonFile(3);
		OAJsonRootNode root3 = oaJson.load(txt3);
		assertTrue(root3 instanceof OAJsonObjectNode);
		root3.set("phones[0].number", "1231231234");
		String json = root3.toJson(2);

		String txt4 = readJsonFile(4);

		boolean b = json.equals(txt4);
		assertEquals(json, txt4);
	}

	@Test // add 2nd value to array at new index
	public void json6Test() throws Exception {
		OAJson oaJson = new OAJson();

		String txt4 = readJsonFile(4);
		OAJsonRootNode root4 = oaJson.load(txt4);
		assertTrue(root4 instanceof OAJsonObjectNode);
		root4.set("phones[2].number", "9999999999");
		String json = root4.toJson(2);

		String txt5 = readJsonFile(5);

		boolean b = json.equals(txt5);
		assertEquals(json, txt5);
	}

	@Test
	public void json7Test() throws Exception {
		OAJson oaJson = new OAJson();

		String txt = readJsonFile(5);

		OAJsonRootNode root = oaJson.load(txt);
		assertTrue(root instanceof OAJsonObjectNode);

		// add 3rd value to array at new index, w/padding
		root.set("phones[4].number", "555555555");

		String json = root.toJson(2);

		String txtNew = readJsonFile(6);
		boolean b = json.equals(txtNew);
		assertEquals(json, txtNew);
	}

	@Test
	public void json8Test() throws Exception {
		OAJson oaJson = new OAJson();

		String txt = readJsonFile(7);
		String txtNew = readJsonFile(8);

		OAJsonRootNode root = oaJson.load(txt);
		assertTrue(root instanceof OAJsonObjectNode);

		// add 3rd value to array at new index, w/padding
		root.remove("phones[14]");
		String json = root.toJson(2);
		assertEquals(json, txt);

		root.remove("phones[5]");
		json = root.toJson(2);
		assertEquals(json, txt);

		root.remove("phones[4]");
		json = root.toJson(2);
		assertNotEquals(json, txt);

		root.remove("phones[0]");
		root.remove("phones[2]");

		json = root.toJson(2);
		boolean b = json.equals(txtNew);
		assertEquals(json, txtNew);
	}

	@Test
	public void json9Test() throws Exception {
		OAJson oaJson = new OAJson();

		String txt = readJsonFile(6);

		OAJsonRootNode root = oaJson.load(txt);
		assertTrue(root instanceof OAJsonObjectNode);

		// insert at 0 & 8
		OAJsonObjectNode jsonObjectNode = new OAJsonObjectNode();
		jsonObjectNode.set("number", "1111");

		root.insert("phones", 0, jsonObjectNode);

		jsonObjectNode = new OAJsonObjectNode();
		jsonObjectNode.set("number", "2222");
		root.insert("phones", 8, jsonObjectNode);

		String json = root.toJson(2);

		String txtNew = readJsonFile(9);

		boolean b = json.equals(txtNew);
		assertEquals(json, txtNew);
	}

	@Test
	public void json10Test() throws Exception {
		OAJson oaJson = new OAJson();

		String txt = readJsonFile(9);

		OAJsonRootNode root = oaJson.load(txt);
		assertTrue(root instanceof OAJsonObjectNode);

		// insert
		root.remove("phones", 0);
		root.remove("phones", 7);
		root.remove("phones", 6);
		root.remove("phones", 5);

		String json = root.toJson(2);

		String txtNew = readJsonFile(6);

		boolean b = json.equals(txtNew);
		assertEquals(json, txtNew);
	}

	@Test
	public void json11Test() throws Exception {
		OAJson oaJson = new OAJson();

		String txt = readJsonFile(11);

		OAJsonRootNode root = oaJson.load(txt);
		assertTrue(root instanceof OAJsonArrayNode);

		OAJsonObjectNode jsonObjectNode = new OAJsonObjectNode();
		jsonObjectNode.set("number", "2");

		root.add("", jsonObjectNode);

		String json = root.toJson(2);

		String txtNew = readJsonFile(12);

		boolean b = json.equals(txtNew);
		assertEquals(json, txtNew);
	}

	@Test
	public void json12Test() throws Exception {
		OAJson oaJson = new OAJson();

		String txt = readJsonFile(12);

		OAJsonRootNode root = oaJson.load(txt);
		assertTrue(root instanceof OAJsonArrayNode);

		OAJsonObjectNode jsonObjectNode = new OAJsonObjectNode();
		jsonObjectNode.set("number", "999");

		root.insert("", 8, jsonObjectNode);

		String json = root.toJson(2);

		String txtNew = readJsonFile(13);

		boolean b = json.equals(txtNew);
		assertEquals(json, txtNew);
	}

	@Test
	public void json13Test() throws Exception {
		OAJson oaJson = new OAJson();

		String txt = readJsonFile(13);

		OAJsonRootNode root = oaJson.load(txt);
		assertTrue(root instanceof OAJsonArrayNode);

		root.remove("", 4);
		root.remove("", 7); // "number": "999"

		OAJsonObjectNode jsonObjectNode = new OAJsonObjectNode();
		jsonObjectNode.set("number", "755");

		root.insert("", 4, jsonObjectNode);

		String json = root.toJson(2);

		String txtNew = readJsonFile(14);

		boolean b = json.equals(txtNew);
		assertEquals(json, txtNew);
	}

	@Test
	public void json14Test() throws Exception {
		OAJson oaJson = new OAJson();

		String txt = readJsonFile(14);

		OAJsonRootNode root = oaJson.load(txt);
		assertTrue(root instanceof OAJsonArrayNode);

		root.set("[1].number", "2222");
		String json = root.toJson(2);

		root.set("[2].code", 3333);
		root.set("[3].date", new OADate(2020, 1, 23));
		root.set("[4].datetime", new OADateTime(2020, 1, 23, 9, 30, 45));

		json = root.toJson(2);

		String txtNew = readJsonFile(15);

		boolean b = json.equals(txtNew);
		assertEquals(json, txtNew);
	}

	@Test
	public void json15Test() throws Exception {
		OAJson oaJson = new OAJson();

		String txt = readJsonFile(15);

		OAJsonRootNode root = oaJson.load(txt);
		assertTrue(root instanceof OAJsonArrayNode);

		root.set("[1].refObject", new OAJsonObjectNode());

		String json = root.toJson(2);

		String txtNew = readJsonFile(16);

		boolean b = json.equals(txtNew);
		assertEquals(json, txtNew);
	}

	@Test
	public void json16Test() throws Exception {
		OAJson oaJson = new OAJson();

		String txt = readJsonFile(16);

		OAJsonRootNode root = oaJson.load(txt);
		assertTrue(root instanceof OAJsonArrayNode);

		root.set("[1].refObject.firstName", "Ralph");

		String json = root.toJson(2);

		String txtNew = readJsonFile(17);

		boolean b = json.equals(txtNew);
		assertEquals(json, txtNew);
	}

	@Test
	public void json17Test() throws Exception {
		OAJson oaJson = new OAJson();

		String txt = readJsonFile(17);

		OAJsonRootNode root = oaJson.load(txt);
		assertTrue(root instanceof OAJsonArrayNode);

		root.set("[1].refObject.lastName", "Jenkons");

		String json = root.toJson(2);

		String txtNew = readJsonFile(18);

		boolean b = json.equals(txtNew);
		assertEquals(json, txtNew);
	}

	@Test
	public void json18Test() throws Exception {
		OAJson oaJson = new OAJson();

		OAJsonObjectNode objNode = new OAJsonObjectNode();
		objNode.set("firstName", "fname");
		objNode.set("lastName", "lname");
		objNode.set("age", 25);
		objNode.set("date", new OADate(2020, 0, 25));
		OADateTime dt = new OADateTime(2020, 0, 25, 14, 29, 55);
		objNode.set("dateTime", dt);
		objNode.set("cost", 1234.58);
		objNode.set("bignum", 1234567.890123);
		objNode.setNull("nullProp");
		objNode.set("isTrue", true);
		objNode.set("isFalse", false);

		assertEquals(objNode.getString("firstName"), "fname");
		assertEquals(objNode.getString("lastName"), "lname");
		assertEquals(objNode.getDate("date"), new OADate(2020, 0, 25));
		assertEquals(objNode.getDateTime("dateTime"), dt);
		assertEquals(objNode.getNumber("cost"), 1234.58);
		assertEquals(objNode.getNumber("bignum"), 1234567.890123);
		assertTrue(objNode.get("xxxx") == null);
		assertTrue(objNode.getString("nullProp") == null);
		assertTrue(objNode.getBoolean("isTrue"));
		assertFalse(objNode.getBoolean("isFalse"));

		OAJsonRootNode root = objNode;

		String json = root.toJson(2);

		String txtNew = readJsonFile(19);

		boolean b = json.equals(txtNew);
		assertEquals(json, txtNew);
	}

	@Test
	public void json19Test() throws Exception {
		OAJson oaJson = new OAJson();

		OAJsonObjectNode objNode = new OAJsonObjectNode();
		objNode.set("firstName", "fname");
		objNode.set("lastName", "lname");
		objNode.set("age", 25);
		objNode.set("date", new OADate(2020, 0, 25));
		OADateTime dt = new OADateTime(2020, 0, 25, 14, 29, 55);
		objNode.set("dateTime", dt);
		objNode.set("cost", 1234.58);
		objNode.set("bignum", 1234567.890123);
		objNode.setNull("nullProp");
		objNode.set("isTrue", true);
		objNode.set("isFalse", false);

		objNode.setNull("firstName");
		objNode.setNull("lastName");
		objNode.setNull("age");
		objNode.setNull("date");
		objNode.setNull("dateTime");
		objNode.setNull("cost");
		objNode.setNull("bignum");
		objNode.set("nullProp", "NOT null");
		objNode.setNull("isTrue");
		objNode.setNull("isFalse");

		assertEquals(objNode.getString("firstName"), null);
		assertEquals(objNode.getString("lastName"), null);
		assertEquals(objNode.getDate("date"), null);
		assertEquals(objNode.getDateTime("dateTime"), null);
		assertEquals(objNode.getNumber("cost"), null);
		assertEquals(objNode.getNumber("bignum"), null);
		assertTrue(objNode.get("xxxx") == null);
		assertTrue(objNode.getString("nullProp") != null);
		assertTrue(objNode.getBoolean("isTrue") == null);
		assertTrue(objNode.getBoolean("isFalse") == null);

		OAJsonRootNode root = objNode;

		String json = root.toJson(2);

		String txtNew = readJsonFile(20);

		boolean b = json.equals(txtNew);
		assertEquals(json, txtNew);
	}

	@Test
	public void json20Test() throws Exception {
		OAJson oaJson = new OAJson();

		OAJsonRootNode root = new OAJsonArrayNode();

		OAJsonObjectNode objNode = new OAJsonObjectNode();
		objNode.setNull("firstName");
		objNode.setNull("lastName");
		objNode.setNull("age");
		objNode.setNull("date");
		objNode.setNull("dateTime");
		objNode.setNull("cost");
		objNode.setNull("bignum");
		objNode.set("nullProp", "NOT null");
		objNode.setNull("isTrue");
		objNode.setNull("isFalse");

		root.add("", objNode);

		objNode = new OAJsonObjectNode();
		objNode.setNull("firstName");
		objNode.setNull("lastName");
		objNode.setNull("age");
		objNode.setNull("date");
		objNode.setNull("dateTime");
		objNode.setNull("cost");
		objNode.setNull("bignum");
		objNode.set("nullProp", "NOT null");
		objNode.setNull("isTrue");
		objNode.setNull("isFalse");

		root.add("", objNode);

		String json = root.toJson(2);

		String txtNew = readJsonFile(21);

		boolean b = json.equals(txtNew);
		assertEquals(json, txtNew);
	}

	@Test
	public void json21Test() throws Exception {
		OAJson oaJson = new OAJson();

		String txt = readJsonFile(19);

		OAJsonRootNode rootNode = oaJson.load(txt);
		assertTrue(rootNode instanceof OAJsonObjectNode);

		OAJsonObjectNode objNode = (OAJsonObjectNode) rootNode;

		OAJsonArrayNode arrayNode = new OAJsonArrayNode();
		arrayNode.add(objNode);

		rootNode = oaJson.load(txt);
		arrayNode.add((OAJsonObjectNode) objNode);

		String json = arrayNode.toJson(2);

		String txtNew = readJsonFile(22);

		boolean b = json.equals(txtNew);
		assertEquals(json, txtNew);
	}

	@Test
	public void json22Test() throws Exception {
		OAJson oaJson = new OAJson();

		String txt = readJsonFile(16);

		OAJsonRootNode root = oaJson.load(txt);
		assertTrue(root instanceof OAJsonArrayNode);

		root.set("[5]", "wasNull");
		root.set("[6]", 12345);
		root.set("[7]", true);

		root.set("[6]", 67890);
		String json = root.toJson(2);

		String txtNew = readJsonFile(28);

		boolean b = json.equals(txtNew);
		assertEquals(json, txtNew);
	}

	@Test
	public void json23Test() throws Exception {
		OAJson oaJson = new OAJson();

		String txt = readJsonFile(16);

		OAJsonRootNode root = oaJson.load(txt);
		assertTrue(root instanceof OAJsonArrayNode);

		BigDecimal bd = new BigDecimal("12345.67");
		root.set("[6].customers[3].balance", new OAJsonNumberNode(bd));
		root.remove("", 5);
		root.remove("[6]");

		OAJsonNode jn = root.get("[5].customers[3].balance");
		assertTrue(jn instanceof OAJsonNumberNode);
		assertEquals(bd, ((OAJsonNumberNode) jn).getValue());

		String json = root.toJson(2);

		String txtNew = readJsonFile(29);

		boolean b = json.equals(txtNew);
		assertEquals(json, txtNew);
	}

	@Test
	public void json24Test() throws Exception {
		OAJson oaJson = new OAJson();

		String txt = readJsonFile(16);

		OAJsonRootNode root = oaJson.load(txt);
		assertTrue(root instanceof OAJsonArrayNode);

		root.set("[1]", "justAstring");

		OAJsonNode jn = root.get("[1]");
		assertTrue(jn instanceof OAJsonStringNode);
		assertEquals("justAstring", ((OAJsonStringNode) jn).getValue());

		String json = root.toJson(2);

		String txtNew = readJsonFile(30);

		boolean b = json.equals(txtNew);
		assertEquals(json, txtNew);
	}

	@Test
	public void json25Test() throws Exception {
		OAJson oaJson = new OAJson();
		OAJsonArrayNode custs = new OAJsonArrayNode();

		for (int i = 0; i < 2; i++) {
			OAJsonObjectNode cust = new OAJsonObjectNode();
			cust.set("name", "Customer" + i);
			cust.set("salesPerson", "John Doe" + i);
			custs.add(cust);
			OAJsonArrayNode orders = new OAJsonArrayNode();
			cust.set("orders", orders);

			for (int ii = 0; ii < 2; ii++) {
				OAJsonObjectNode ord = new OAJsonObjectNode();
				ord.set("id", ii);
				ord.set("description", "description" + ii);
				orders.add(ord);

				for (int iii = 0; iii < 2; iii++) {
					custs.set("[" + i + "].orders[" + ii + "].orderItems[" + iii + "].item.name", "itemname");
					ord.getObject("orderItems[" + iii + "]").set("qty", iii + 1);
				}
			}

			for (int ii = 0; ii < 2; ii++) {
				custs.set("[" + i + "].counters[" + ii + "]", ii);
			}

		}

		custs.set("[0].counters[0]", "test this");

		String json = custs.toJson(2);

		String txtNew = readJsonFile(31);

		boolean b = json.equals(txtNew);
		assertEquals(json, txtNew);
	}

	protected String readJsonFile(int number) throws Exception {
		String txt = OAFile.readTextFile(OAJson.class, "json" + number + ".txt", 1024 * 2);
		return txt;
	}

	@Test
	public void testA() throws Exception {
		OAJson oaJson = new OAJson();

		String txt = readJsonFile(0);
		// String txt = OAFile.readTextFile(new File("/home/vvia/git/oa-core/src/main/java/com/viaoa/json/json.txt"), 0);

		OAJsonRootNode root = oaJson.load(txt);
		String json = root.toJson();

		OAJsonArrayNode arrayNode = root.getArray("employees");
		json = arrayNode.toJson();

		OAJsonObjectNode objNode = new OAJsonObjectNode();
		arrayNode.set("[3]", objNode);
		json = arrayNode.toJson();

		OAJsonNode node = root.get("employees[0]");
		json = node.toJson();

		root.set("employees[1].customers[1].b", new OADate());
		json = root.toJson();

		node = root.get("employees[1].weight");
		json = node.toJson();

		node = root.get("employees[1].weightXXX");
		assertNull(node);

		node = root.get("employees[1]");
		json = node.toJson();

		node = root.get("employees");
		int x = root.getArraySize("employees");

		OAJsonArrayNode na = (OAJsonArrayNode) node;
		objNode = new OAJsonObjectNode();
		objNode.set("Name", new OAJsonStringNode("Value"));
		na.insert(2, objNode);

		json = root.toJson();

		String txtNew = readJsonFile(23);

		boolean b = json.equals(txtNew);
		assertEquals(json, txtNew);
	}

	@Test
	public void test1() throws Exception {
		OAJsonRootNode node = new OAJsonObjectNode();

		node.set("firstName", new OAJsonStringNode("Vincent"));
		node.set("nickName", new OAJsonStringNode("LastName"));
		node.set("middleInitial", "A");
		node.set("lastName", "NewLastName");

		// OAJsonArrayNode arrayNode = new OAJsonArrayNode()
		String json = node.toJson();

		String txtNew = readJsonFile(24);

		boolean b = json.equals(txtNew);
		assertEquals(json, txtNew);

	}

	@Test
	public void test2() throws Exception {
		OAJsonRootNode root = new OAJsonObjectNode();

		OAJsonRootNode node = new OAJsonObjectNode();

		node.set("firstName", new OAJsonStringNode("Vincent"));
		node.set("nickName", new OAJsonStringNode("Vince"));
		node.set("middleInitial", "A");
		node.set("lastName", "Smith");
		root.set("customer", node);

		root.setNull("discount");

		root.set("cost", 123.45);

		root.set("discount", new BigDecimal("12.34", MathContext.DECIMAL64));

		String json = root.toJson();

		OAJsonNode nodex = root.get("customer");

		root.remove("customer");

		json = root.toJson();

		OAJsonArrayNode arrayNode = new OAJsonArrayNode();

		root.set("customers", arrayNode);
		arrayNode.add((OAJsonObjectNode) nodex);

		arrayNode.add((OAJsonObjectNode) nodex);
		json = root.toJson();

		nodex = root.get("customers");
		json = nodex.toJson(2);

		json = root.toJson(2);

		OAJson oaJson = new OAJson();
		OAJsonRootNode rootx = oaJson.load(json);
		OAJsonObjectNode customerNode = rootx.getObjectNode("customers[1]");
		customerNode.setNull("NullProp");
		customerNode.set("Alive", true);
		customerNode.set("StringProp", "StringPropVAlue");
		json = rootx.toJson();

		OAJsonArrayNode customers = rootx.getArrayNode("customers");
		customerNode = customers.getObject(0);
		customerNode.setNull("NullProp2");
		customerNode.set("Alive2", true);
		customerNode.set("StringProp2", "StringPropVAlue2");
		json = rootx.toJson();

		String txtNew = readJsonFile(25);

		boolean b = json.equals(txtNew);
		assertEquals(json, txtNew);
	}

	@Test
	public void test3() throws Exception {
		OAJson oaJson = new OAJson();
		OAJsonObjectNode root = new OAJsonObjectNode();

		root.set("orderItem.order.customer.company.name", "ABC Company");

		String json = root.toJson();
		String txtNew = readJsonFile(26);

		boolean b = json.equals(txtNew);
		assertEquals(json, txtNew);
	}

	@Test
	public void test4() throws Exception {
		OAJson oaJson = new OAJson();
		OAJsonObjectNode root = new OAJsonObjectNode();

		root.set("company.customers[1].name", "ABC Company");

		String json = root.toJson();

		String txtNew = readJsonFile(27);

		boolean b = json.equals(txtNew);
		assertEquals(json, txtNew);
	}

	public static void main(String[] args) throws Exception {
		OAJsonTest util = new OAJsonTest();

		util.json1Test();

		int xx = 4;
		xx++;
	}

}
