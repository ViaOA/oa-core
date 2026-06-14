package com.viaoa.object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.viaoa.OAUnitTest;
import com.viaoa.datasource.OADataSource;
import com.viaoa.hub.Hub;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;

import test.xice.tsac3.model.oa.AdminUserCategory;
import test.xice.tsac3.model.oa.Server;
import test.xice.tsac3.model.oa.ServerGroup;
import test.xice.tsac3.model.oa.ServerInstall;
import test.xice.tsac3.model.oa.ServerType;
import test.xice.tsac3.model.oa.Silo;
import test.xice.tsac3.model.oa.SiloServerInfo;
import test.xice.tsac3.model.oa.SiloType;
import test.xice.tsac3.model.oa.search.ServerSearch;

public class OAObjectTest extends OAUnitTest {

	@Test
	public void constructorTest() {
		reset(false);

		assertFalse(OAThreadLocalDelegate.callThreadLocalIsLoading());
		Server server = new Server();

		assertFalse(OAThreadLocalDelegate.callThreadLocalIsLoading());
		assertTrue(server.isNew());
		assertTrue(server.isChanged());

		OAObjectInfo oi = OAObjectInfoDelegate.callInfoGetObjectInfo(Server.class);
		String[] ps = oi.getPrimitiveProperties();
		assertTrue(ps != null && ps.length == 4);
		for (String s : ps) {
			//if ("verifiedVersion".equalsIgnoreCase(s)) assertFalse(server.isNull(s));
			assertTrue(server.isNull(s));
		}
		assertTrue(server.isNull(Server.P_Id));

		// test: guid should be 1
		long x = OAObjectDelegate.getGuid(server);
		assertEquals(x, 1);

		assertEquals(server.getId(), 0);

		for (OALinkInfo li : oi.getLinkInfos()) {
			if (li.getCalculated()) {
				continue;
			}
			if (li.getPrivateMethod()) {
				continue;
			}
			if (li.getMatchProperty() != null) {
				continue;
			}

			Object objx = OAObjectPropertyDelegate.getProperty(server, li.getName(), true, true);
			assertEquals(objx, null);
		}

		// now with DS
		getDataSource();
		server = new Server();
		assertEquals(0, server.getId()); // not auto assigned

		server.save();
		assertEquals(1, server.getId()); // auto assigned

		// clean up
		reset();
	}

	@Test
	public void localGuidTest() {
		reset();
		ServerSearch serverSearch = new ServerSearch();
		assertTrue(OAObjectDelegate.getGuid(serverSearch) < 0);

		// clean up
		reset();
	}

	@Test
	public void idAndGuidTest() {
		reset(false);

		long gidNext = OAObjectDelegate.getNextGuid() + 1;

		Server server = new Server();

		// test: make sure that it is in the cache
		Server serv = (Server) OAObjectCacheDelegate.get(Server.class, 0); // should not work, Id is null
		assertEquals(null, serv);

		OAObjectKey key = new OAObjectKey(null, gidNext);
		serv = (Server) OAObjectCacheDelegate.get(Server.class, key);
		assertEquals(serv, server);

		// test: set Id, changes key, cache pos
		serv.setId(1);
		assertFalse(server.isNull(Server.P_Id));
		assertEquals(server.getId(), 1);

		try {
			serv.setId(2);
			assertEquals(server.getId(), 2);
		} catch (Exception e) {
			fail();
		}

		server.save();
		assertFalse(server.isNew());
		assertFalse(server.isChanged());

		try {
			serv.setId(1);
		} catch (Exception e) {
			fail("id can be changed, if datasource.getAllowIdChange() is true (default)"); // RDBMS datasources will be false
		}
		assertEquals(server.getId(), 1);

		// action: this will auto assign Id
		getDataSource();
		assertEquals(OADataSource.getDataSource(Server.class), dsAuto);
		Server server2 = new Server();
		assertTrue(server2.isNull(Server.P_Id));

		// test: guid should be 2
		gidNext++;
		long x = OAObjectDelegate.getGuid(server2);
		assertEquals(x, gidNext);

		assertEquals(0, server2.getId());
		server2.save();
		assertEquals(2, server2.getId());

		server2 = new Server();
		gidNext++;
		x = OAObjectDelegate.getGuid(server2);
		assertEquals(x, gidNext);
		server2.save();
		assertEquals(3, server2.getId()); // 2 was already manually assigned

		// clean up
		reset();
	}

	@Test
	public void regularPropertyChangeTest() {
		reset();
		Server server = new Server();
		server.save();
		assertFalse(server.isChanged());
		assertFalse(server.isNew());

		assertNull(server.getHostName());
		server.setHostName(null);
		assertNull(server.getHostName());
		assertFalse(server.isChanged());
		assertFalse(server.isNew());

		server.setHostName("test");
		assertEquals(server.getHostName(), "test");
		assertTrue(server.isChanged());
		assertFalse(server.isNew());

		server.setHostName(null);
		assertNull(server.getHostName());
		assertTrue(server.isChanged());
		assertFalse(server.isNew());

		server.save();
		assertFalse(server.isChanged());
		assertFalse(server.isNew());

		ServerInstall si = new ServerInstall();
		server.getServerInstalls().add(si);
		assertFalse(server.isChanged());
		assertFalse(server.isNew());
		assertFalse(server.isChanged(true)); // serverInstalls is not owned

		Silo silo = new Silo();
		silo.save();
		assertFalse(silo.isChanged(true));
		server.setHostName("");
		silo.getServers().add(server);

		assertTrue(silo.isChanged());
		assertTrue(silo.isChanged(true));

		silo.save();
		assertFalse(silo.isChanged());
		assertFalse(silo.isChanged(true));

		server.save();
		assertFalse(silo.isChanged(true));

		// clean up
		reset();
	}

	@Test
	public void uniquePropertyChangeTest() {
		reset();
		ServerType st = new ServerType();
		st.setCode("1");
		st.setCode("2");
		st.setCode("3");

		ServerType st2 = new ServerType();
		st2.setCode("1");
		try {
			st2.setCode("3");
			fail();
		} catch (Exception e) {
		}
		assertEquals(st2.getCode(), "1");

		// clean up
		reset();
	}

	@Test
	public void referenceOnePropertyChangeTest() {
		reset();
		Server server = new Server();
		server.setHostName(null);

		Object objx = OAObjectPropertyDelegate.getProperty(server, Server.P_Silo);
		assertNull(objx);

		objx = OAObjectPropertyDelegate.getProperty(server, Server.P_Silo, true, true);
		assertNull(objx);

		Silo silo = new Silo();
		server.setSilo(silo);
		assertEquals(silo, server.getSilo());

		objx = OAObjectPropertyDelegate.getProperty(server, Server.P_Silo, true, true);
		assertEquals(silo, objx);

		Silo silox = new Silo();
		server.setSilo(silox);
		assertEquals(silox, server.getSilo());
		assertTrue(silox.getServers().contains(server));

		objx = OAObjectPropertyDelegate.getProperty(server, Server.P_Silo, true, true);
		assertEquals(silox, objx);

		server.setSilo(null);
		assertNull(server.getSilo());
		assertFalse(silo.getServers().contains(server));

		objx = OAObjectPropertyDelegate.getProperty(server, Server.P_Silo, true, true);
		assertNull(objx);

		// clean up
		reset();
	}

	@Test
	public void referenceManyPropertyChangeTest() {
		reset();
		Silo silo = new Silo();
		Object objx = OAObjectPropertyDelegate.getProperty(silo, Silo.P_Servers, true, true);
		assertNull(objx);

		Hub h = silo.getServers();
		assertTrue(h != null);
		objx = OAObjectPropertyDelegate.getProperty(silo, Silo.P_Servers, true, true);
		assertTrue(objx != null);
		assertEquals(h, objx);

		// clean up
		reset();
	}

	@Test
	public void referenceManyAutoMatchPropertyChangeTest() {
		reset();
		SiloType siloType = new SiloType();

		ServerType st = new ServerType();
		siloType.getServerTypes().add(st);

		Silo silo = new Silo();
		silo.setSiloType(siloType);

		assertEquals(silo.getSiloServerInfos().getSize(), 1);

		SiloServerInfo info = silo.getSiloServerInfos().getAt(0);
		assertTrue(info != null);

		assertEquals(info.getServerType(), st);

		ServerType st2 = new ServerType();
		siloType.getServerTypes().add(st2);

		info = silo.getSiloServerInfos().getAt(1);
		assertTrue(info != null);
		assertEquals(info.getServerType(), st2);

		assertEquals(silo.getSiloServerInfos().getSize(), 2);

		siloType.getServerTypes().remove(st2);
		assertEquals(silo.getSiloServerInfos().getSize(), 1);

		// clean up
		reset();
	}

	@Test
	public void referenceManySeqPropertyChangeTest() {
		reset();
		Silo silo = new Silo();

		ServerGroup sg = new ServerGroup();
		assertEquals(sg.getSeq(), 0);
		assertTrue(sg.isNull(ServerGroup.P_Seq));
		silo.getServerGroups().add(sg);
		assertEquals(sg.getSeq(), 0);
		assertFalse(sg.isNull(ServerGroup.P_Seq));

		ServerGroup sg2 = new ServerGroup();
		assertEquals(sg2.getSeq(), 0);
		silo.getServerGroups().add(sg2);
		assertEquals(sg.getSeq(), 0);
		assertEquals(sg2.getSeq(), 1);
		assertFalse(sg2.isNull(ServerGroup.P_Seq));

		ServerGroup sg3 = new ServerGroup();
		assertEquals(sg3.getSeq(), 0);
		silo.getServerGroups().insert(sg3, 1);
		assertEquals(sg.getSeq(), 0);
		assertEquals(sg2.getSeq(), 2);
		assertEquals(sg3.getSeq(), 1);
		assertFalse(sg3.isNull(ServerGroup.P_Seq));

		silo.getServerGroups().move(2, 1);
		assertEquals(sg.getSeq(), 0);
		assertEquals(sg2.getSeq(), 1);
		assertEquals(sg3.getSeq(), 2);

		silo.getServerGroups().removeAt(1);
		assertEquals(sg.getSeq(), 0);
		assertEquals(sg2.getSeq(), 1); // removed, so value never changed
		assertEquals(sg3.getSeq(), 1);

		silo.getServerGroups().add(sg2);
		assertEquals(sg.getSeq(), 0);
		assertEquals(sg2.getSeq(), 2); // re-added
		assertEquals(sg3.getSeq(), 1);

		// clean up
		reset();
	}

	@Test
	public void recursivePropertyChangeTest() {
		reset();
		AdminUserCategory catParent = new AdminUserCategory();

		AdminUserCategory catChild1 = new AdminUserCategory();

		try {
			catParent.setParentAdminUserCategory(catParent);
			fail();
		} catch (Exception e) {
		}
		assertNull(catParent.getParentAdminUserCategory());

		catChild1.setParentAdminUserCategory(catParent);
		assertEquals(catChild1.getParentAdminUserCategory(), catParent);
		assertEquals(catParent.getAdminUserCategories().getAt(0), catChild1);

		try {
			catParent.setParentAdminUserCategory(catChild1);
			fail();
		} catch (Exception e) {
		}
		assertNull(catParent.getParentAdminUserCategory());

		AdminUserCategory catChild2 = new AdminUserCategory();
		catChild1.getAdminUserCategories().add(catChild2);
		assertEquals(catChild2.getParentAdminUserCategory(), catChild1);

		catChild2.setParentAdminUserCategory(catParent);
		assertEquals(catParent.getAdminUserCategories().getSize(), 2);
		assertTrue(catParent.getAdminUserCategories().contains(catChild1));
		assertTrue(catParent.getAdminUserCategories().contains(catChild2));
		assertEquals(catChild1.getParentAdminUserCategory(), catParent);
		assertEquals(catChild2.getParentAdminUserCategory(), catParent);

		// clean up
		reset();
	}

	//qqqqqqq create these
	//qqqqqqqq OAObjectCacheDelegate
	//  finder
	// datasource tests
	// order of finding registered one

	@Test
	public void metaDataTest() {
		OAObjectInfo oi = OAObjectInfoDelegate.callInfoGetObjectInfo(Server.class);
		String[] ps = oi.getPrimitiveProperties();

		//qqq links, etc
	}

	@Test
	public void propertyTest() {

	}

	//qqqqqqq  OAThreadLocalDelegate tests

}





/**
 * OAObject is the Base Class used for Application Data Objects. It is the central class for OA, where all other objects are designed to
 * automatically work with the OAObject class, along with the Hub collection class.
 * <p>
 * OAObjects have built-in functionality to allow it to work with other Classes. This includes other OAObjects, Hub Collections, any
 * datasource/database, JFC component, JSP component, XML, other applications (distributed) and any other Class.
 * <p>
 * &nbsp;&nbsp;&nbsp;<img src="doc-files/ObjectAutomation1.gif" alt=""> <br>
 * Subclasses of OAObject can be created that add properties and methods for building customized software applications. OAObject then
 * supplies the capability for these subclasses to automatically work with any OA Enabled Class.
 * <p>
 * This is a summary of some of the features included in OAObject.
 * <ul>
 * <li>Object Key - property values that makes this object unique.
 * <li>Reference Information - how objects are related to other object. All references use the actual objects and not the key (or foreign
 * key value). References types include one-one, one-many, many-many, recursive self references, owned and un-owned references, and more.
 * <li>Manages reference objects when working with database/datasource.
 * <li>"Moves" objects when changes are made to a reference property.
 * <li>Methods to set and get properties and convert from and to Strings.
 * <li>Store miscellaneous data in name/value pairs, where name is case insensitive.
 * <li>Initialization during creation
 * <li>Null Values - to know if a primitive property value is null
 * <li>Knows which Hub Collections that an object is a member of.
 * <li>Handles events for object, including property changes and calculated properties.
 * <li>Knows if object is "new"
 * <li>Cascading rules. Cancel, Save, Delete can be cascaded to reference objects.
 * <li>Works directly with OADataSource for storing and retrieving objects.
 * <li>Save Method
 * <li>Delete Method
 * <li>Calculated Properties - properties that rely on other properties or objects for their value.
 * <li>Serialization Support - to file/stream, other applications using RMI
 * <li>XML support - reading and writing
 * <li>Locking
 * <li>Client/Server - changes to objects can be automatically updated on other computers.
 * </ul>
 * <p>
 * This is a listing of the types of relationships that an OAObject can have with another OAObject. This information is built into the
 * object information. Relationships between objects are "two-way", meaning that both objects are related to each other.<br>
 * <ul>
 * <li>One-One relationship
 * <li>One-Many relationship
 * <li>Many-Many relationship
 * <li>Recursive - this is where an object can have many children objects of the same class and each of these children can themselves have
 * children, recursively.
 * <li>An Owned relationship is one where the children can not exist without the parent (owner) and all are treated as a single unit.
 * <li>Cascading Rules for save, delete, cancel
 * </ul>
 * <p>
 * Managing Relationships<br>
 * OAObject manages the relationships between objects, and is responsible for retrieving and populating reference objects and for managing
 * changes. An OAObject subclass does not have to have any code to handle retrieving or storing reference objects, OAObject does it
 * completely. If a reference property is changed, then OAObject manages the change so that other objects are updated correctly. <br>
 * For example, if a Department has many Employees, and an Employee has one Department: if an Employee's Department is changed, then the
 * Employee object is removed from the original Department collection and added to the new assigned Department collection. This also works
 * when an Employee is added to a different Departments Employee collection - the Employee's Department property is changed to the newly
 * assigned Department.
 * <p>
 * Working with DataSources<br>
 * OAObjects work directly with OADataSource for initializing properties, saving, deleting. This is all done so that the OAObjects are
 * independent from datasource/database.
 * <p>
 * For more information about this package, see <a href="package-summary.html#package_description">documentation</a>.
 * <p>
 * OAObjectCallback can be used to query the object and properties.<br>
 *
 * @see OAObjectCallback
 * @see Hub for observable collection class that has "linkage" features for automatically managing relationships. see OAHtmlSelect for
 *      datasource independent queries based on object and property paths.
 */













