package com.viaoa.hub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.viaoa.OAUnitTest;
import com.viaoa.object.OAFinder;

import test.hifive.HifiveDataGenerator;
import test.hifive.delegate.ModelDelegate;
import test.hifive.model.oa.AwardType;
import test.hifive.model.oa.Employee;
import test.hifive.model.oa.EmployeeAward;
import test.hifive.model.oa.Location;
import test.hifive.model.oa.Program;
import test.hifive.model.oa.cs.ServerRoot;
import test.hifive.model.oa.propertypath.ProgramPP;
import test.xice.tsac3.Tsac3DataGenerator;
import test.xice.tsac3.model.Model;
import test.xice.tsac3.model.oa.Environment;
import test.xice.tsac3.model.oa.Server;
import test.xice.tsac3.model.oa.ServerInstall;
import test.xice.tsac3.model.oa.ServerStatus;
import test.xice.tsac3.model.oa.ServerType;
import test.xice.tsac3.model.oa.Silo;
import test.xice.tsac3.model.oa.Site;
import test.xice.tsac3.model.oa.propertypath.SiloPP;

public class HubLinkTest extends OAUnitTest {
	test.xice.tsac3.model.Model modelTsac = new test.xice.tsac3.model.Model();

	@Test
	public void linkTest() {
		reset();
		Model modelTsac = new Model();
		Tsac3DataGenerator data = new Tsac3DataGenerator(modelTsac);
		data.createSampleData();

		Hub<ServerType> hubServerType = modelTsac.getServerTypes();
		Hub<ServerStatus> hubServerStatus = modelTsac.getServerStatuses();

		Hub<Site> hubSite = modelTsac.getSites();
		Hub<Environment> hubEnvironment = hubSite.getDetailHub(Site.P_Environments);
		Hub<Silo> hubSilo = hubEnvironment.getDetailHub(Environment.P_Silos);
		Hub<Server> hubServer = hubSilo.getDetailHub(Silo.P_Servers);

		hubServerType.setLinkHub(hubServer, Server.P_ServerType);
		hubServerStatus.setLinkHub(hubServer, Server.P_ServerStatus);

		hubSite.setPos(0);
		hubEnvironment.setPos(0);
		hubSilo.setPos(0);
		hubServer.setPos(0);

		Server server = hubServer.getAO();
		assertNotNull(server);

		assertNull(hubServerStatus.getAO());

		int cntServerStatus = hubServerStatus.getSize();
		server.setServerStatus(hubServerStatus.getAt(1));
		assertEquals(hubServerStatus.getPos(), 1);

		server.setServerStatus(hubServerStatus.getAt(2));
		assertEquals(hubServerStatus.getPos(), 2);

		server.setServerStatus(null);
		assertEquals(hubServerStatus.getPos(), -1);

		ServerStatus st = new ServerStatus();
		server.setServerStatus(st); // this will add st to the hubServerStatus

		assertNull(hubServerStatus.getAO());

		hubServer.setPos(1);
		assertNull(hubServerStatus.getAO());
		assertEquals(hubServerStatus.getSize(), cntServerStatus);

		hubServer.setPos(0);
		assertNull(hubServerStatus.getAO());
		assertNotNull(server.getServerStatus());

		// change site AO, which will set server AO to null
		hubSite.setPos(1);
		assertNull(hubServer.getAO());
		assertNull(hubServerStatus.getAO());

		hubServer.setAO(server);
		assertEquals(hubServer.getAO(), server);

		reset();
	}

	@Test
	public void autoCreateLinkTest() {

		reset();
		Model modelTsac = new Model();
		Tsac3DataGenerator data = new Tsac3DataGenerator(modelTsac);
		data.createSampleData();

		Hub<Site> hubSite = modelTsac.getSites();
		Hub<Environment> hubEnvironment = hubSite.getDetailHub(Site.P_Environments);
		Hub<Silo> hubSilo = hubEnvironment.getDetailHub(Environment.P_Silos);
		Hub<Server> hubServer = hubSilo.getDetailHub(Silo.P_Servers);

		Hub<ServerType> hubServerType = modelTsac.getServerTypes();
		hubServerType.setLinkHub(hubServer, Server.P_ServerType, true, true);

		hubSite.setPos(0);
		hubEnvironment.setPos(0);
		hubSilo.setPos(0);

		int x = hubServer.getSize();

		ServerType st = hubServerType.getAt(5);
		hubServerType.setAO(st);
		assertEquals(hubServer.getSize(), x + 1);
		hubServerType.setAO(null);
		assertEquals(hubServer.getSize(), x + 1);

		hubServerType.setLinkHub(hubServer, Server.P_ServerType, true, false);
		st = hubServerType.getAt(5);
		hubServerType.setAO(st);
		assertEquals(hubServer.getSize(), x + 1);
		hubServerType.setAO(null);
		assertEquals(hubServer.getAO().getServerType(), st);

		Silo silo = new Silo();
		hubEnvironment.getAO().getSilos().add(silo);
		hubSilo.setAO(silo);
		assertNull(hubServer.getAO());
		assertEquals(hubServer.getSize(), 0);

		hubServerType.setAO(st);
		assertNotNull(hubServer.getAO());
		assertEquals(hubServer.getSize(), 1);
		assertEquals(hubServer.getAO().getServerType(), st);

		reset();
	}

	@Test
	public void autoCreateLinkTest2() {
		reset();
		Model modelTsac = new Model();
		Tsac3DataGenerator data = new Tsac3DataGenerator(modelTsac);
		data.createSampleData();

		Hub<Site> hubSite = modelTsac.getSites();
		Hub<Environment> hubEnvironment = hubSite.getDetailHub(Site.P_Environments);
		Hub<Silo> hubSilo = hubEnvironment.getDetailHub(Environment.P_Silos);
		Hub<Server> hubServer = hubSilo.getDetailHub(Silo.P_Servers);
		assertNull(hubServer.getAO());
		assertEquals(hubServer.getSize(), 0);

		// ServerTypes for silo
		Hub<ServerType> hubServerType = new Hub<ServerType>(ServerType.class);

		HubMerger hmx = new HubMerger(hubSilo, hubServerType, SiloPP.siloType().serverTypes().pp, false);
		hubServerType.setLinkHub(hubServer, Server.P_ServerType, true, true);

		Hub<ServerType> hubServerType2 = modelTsac.getServerTypes().createShared();
		hubServerType2.setLinkHub(hubServer, Server.P_ServerType);

		hubSite.setPos(0);
		hubEnvironment.setPos(0);
		hubSilo.setPos(0);

		int x = hubServer.getSize();
		assertEquals(hubSilo.getAO().getSiloType().getServerTypes().getSize(), hubServerType.getSize());
		assertNull(hubServer.getAO());
		assertNull(hubServerType.getAO());
		assertNull(hubServerType2.getAO());

		hubServerType.setAO(null);
		assertNull(hubServer.getAO());
		assertNull(hubServerType2.getAO());

		Server server = hubServer.setPos(0);
		ServerType st = server.getServerType();
		assertNotNull(st);
		assertNull(hubServerType.getAO());
		assertEquals(hubServerType2.getAO(), st);

		// set Server.serverType
		st = modelTsac.getServerTypes().getAt(3);
		server.setServerType(st);
		assertNull(hubServerType.getAO());
		assertEquals(hubServerType2.getAO(), st);

		// change serverType2 AO
		st = hubServerType2.setPos(2);
		assertEquals(server.getServerType(), st);
		assertNull(hubServerType.getAO());

		// change serverType AO - create new server
		assertEquals(hubServer.getSize(), x);
		st = hubServerType.setPos(1);
		assertTrue(server != hubServer.getAO());
		server = hubServer.getAO();
		assertEquals(server.getServerType(), st);
		assertEquals(hubServer.getSize(), x + 1);

		// change silo and try again
		hubSite.setPos(1);
		hubEnvironment.setPos(0);
		hubSilo.setPos(0);
		assertNull(hubServer.getAO());
		assertNull(hubServerType.getAO());
		assertNull(hubServerType2.getAO());

		x = hubServer.getSize();
		st = hubServerType.setPos(2);
		assertEquals(hubServer.getSize(), x + 1);
		assertTrue(server != hubServer.getAO());
		server = hubServer.getAO();
		assertEquals(server.getServerType(), st);

		reset();
	}

	@Test
	public void linkAOTest() {
		reset();
		Model modelTsac = new Model();
		Tsac3DataGenerator data = new Tsac3DataGenerator(modelTsac);
		data.createSampleData();

		Hub<ServerType> hubServerType = modelTsac.getServerTypes();
		Hub<ServerStatus> hubServerStatus = modelTsac.getServerStatuses();

		Hub<Site> hubSite = modelTsac.getSites();
		Hub<Environment> hubEnvironment = hubSite.getDetailHub(Site.P_Environments);
		Hub<Silo> hubSilo = hubEnvironment.getDetailHub(Environment.P_Silos);
		Hub<Server> hubServer = hubSilo.getDetailHub(Silo.P_Servers);

		Hub<ServerInstall> hubServerInstall = new Hub<ServerInstall>(ServerInstall.class);

		hubServer.setLinkHub(hubServerInstall, ServerInstall.P_Server);

		assertNull(hubServer.getAO());

		Server server = hubSite.getAt(0).getEnvironments().getAt(0).getSilos().getAt(0).getServers().getAt(0);
		Server server2 = hubSite.getAt(1).getEnvironments().getAt(0).getSilos().getAt(0).getServers().getAt(0);

		ServerInstall si = new ServerInstall();
		hubServerInstall.add(si);
		assertNull(hubServer.getAO());
		hubServerInstall.setAO(si);
		assertNull(hubServer.getAO());

		si.setServer(server);
		assertEquals(server, hubServer.getAO());
		assertEquals(hubSite.getAO(), hubSite.getAt(0));
		assertNotNull(hubServer.getMasterHub());

		int pos = hubServer.getPos(server2);
		assertEquals(pos, -1);

		si.setServer(server2);

		assertNotNull(hubServer.getMasterHub());

		pos = hubServer.getPos(server2);
		assertEquals(pos, 0);

		assertEquals(server2, hubServer.getAO());
		assertEquals(hubSite.getAO(), hubSite.getAt(1));

		reset();
	}

	@Test
	public void recursiveLinkTest() {
		init();
		ModelDelegate.initialize(new ServerRoot());

		HifiveDataGenerator data = new HifiveDataGenerator();
		data.createSampleData();

		OAFinder<Program, Location> f = new OAFinder<Program, Location>(ProgramPP.locations().pp) {
			@Override
			protected void onFound(Location loc) {
				if (loc.getProgram() == null) {
					int xx = 4;
					xx++;
				}
			}
		};
		f.find(ModelDelegate.getPrograms());

		final Hub<Program> hubProgram = ModelDelegate.getPrograms().createSharedHub();
		final Hub<Location> hubLocation = hubProgram.getDetailHub(Program.P_Locations);
		final Hub<Employee> hubEmployee = hubLocation.getDetailHub(Location.P_Employees);

		Hub<EmployeeAward> hubEmployeeAward = new Hub<EmployeeAward>(EmployeeAward.class);
		hubEmployee.setLinkHub(hubEmployeeAward, EmployeeAward.P_Employee);

		Employee emp = hubProgram.getAt(0).getLocations().getAt(0).getEmployees().getAt(0);

		EmployeeAward ea = new EmployeeAward();
		hubEmployeeAward.add(ea);
		assertNull(hubEmployee.getAO());

		hubEmployeeAward.setPos(0);
		assertNull(hubEmployee.getAO());
		assertNull(hubLocation.getAO());
		assertNull(hubProgram.getAO());

		//qqqqqqqqqqqqqqqqqqqqq

		ea.setEmployee(emp);
		assertEquals(emp, hubEmployee.getAO());

		assertEquals(hubProgram.getAO(), hubProgram.getAt(0));
		assertEquals(hubLocation.getAO(), hubProgram.getAt(0).getLocations().getAt(0));

		emp = hubProgram.getAt(0).getLocations().getAt(0).getEmployees().getAt(0).getEmployees().getAt(0);
		ea.setEmployee(emp);
		assertEquals(emp, hubEmployee.getAO());
		assertEquals(hubProgram.getAO(), hubProgram.getAt(0));
		assertEquals(hubLocation.getAO(), hubProgram.getAt(0).getLocations().getAt(0));
		assertNotNull(hubEmployee.getMasterHub());

		Program prog = hubProgram.getAt(1);
		Location loc = prog.getLocations().getAt(0);
		emp = loc.getEmployees().getAt(0).getEmployees().getAt(0);

		hubEmployee.setAO(emp);
		assertEquals(emp, hubEmployee.getAO());
		assertEquals(loc, hubLocation.getAO());
		assertEquals(prog, hubProgram.getAO());

		//qqqqqqqq
		f.find(ModelDelegate.getPrograms());

		prog = hubProgram.getAt(2);
		loc = prog.getLocations().getAt(0);
		loc = loc.getLocations().getAt(0);
		emp = loc.getEmployees().getAt(0).getEmployees().getAt(0);

		hubEmployee.setAO(emp);

		if (hubEmployee.getAO() == null) {
			hubEmployee.setAO(emp);
			Object objx = hubEmployee.getAO();

			hubEmployee.setAO(emp);
			objx = hubEmployee.getAO();
			int xx = 4;
			xx++;
		}

		assertEquals(emp, hubEmployee.getAO());
		assertEquals(loc, hubLocation.getAO());
		assertEquals(prog, hubProgram.getAO());

		prog = hubProgram.getAt(1);
		loc = prog.getLocations().getAt(1);
		loc = loc.getLocations().getAt(0);
		emp = loc.getEmployees().getAt(1);
		emp = emp.getEmployees().getAt(0);

		ea.setEmployee(emp);
		assertEquals(emp, hubEmployee.getAO());
		assertEquals(loc, hubLocation.getAO());
		assertEquals(prog, hubProgram.getAO());

		reset();
	}

	@Test
	public void linkTest2() {
		reset();

		Location loc = new Location();
		AwardType at = new AwardType();
		loc.getAwardTypes().add(at);
		;

		Employee emp = new Employee();
		EmployeeAward ea = new EmployeeAward();
		ea.setAwardType(at);
		emp.getEmployeeAwards().add(ea);
		emp.getEmployeeAwards().setPos(0);

		Hub<AwardType> hubAwardType = loc.getAwardTypes().createShared();

		hubAwardType.setLinkHub(emp.getEmployeeAwards(), EmployeeAward.P_AwardType);

		assertNotNull(hubAwardType.getAO());

		AwardType at2 = new AwardType();
		ea.setAwardType(at2);

		assertNull(hubAwardType.getAO());
	}

	int xx = 4;

	public static void main(String[] args) throws Exception {
		HubLinkTest test = new HubLinkTest();
		test.recursiveLinkTest();
		System.out.println("Done ****");
	}

}
