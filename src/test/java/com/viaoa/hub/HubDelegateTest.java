package com.viaoa.hub;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.viaoa.OAUnitTest;

import test.hifive.model.oa.Employee;
import test.hifive.model.oa.Location;
import test.hifive.model.oa.Program;

public class HubDelegateTest extends OAUnitTest {

	@Test
	public void getCurrentStateTest() {
		reset();

		Hub<Program> hubProgram = new Hub<Program>(Program.class);
		Hub<Location> hubLocation = hubProgram.getDetailHub(Program.P_Locations);

		HubDelegate.HubCurrentStateEnum hcs = HubDelegate.getCurrentState(hubLocation, null, null);

		assertEquals(HubDelegate.HubCurrentStateEnum.InSync, hcs);

		Program prog = new Program();
		hubProgram.add(prog);
		hubProgram.setPos(0);

		hcs = HubDelegate.getCurrentState(hubLocation, null, null);
		assertEquals(HubDelegate.HubCurrentStateEnum.InSync, hcs);

		hubProgram.setPos(-1);

		hcs = HubDelegate.getCurrentState(hubLocation, null, null);
		assertEquals(HubDelegate.HubCurrentStateEnum.InSync, hcs);

		hubProgram.setPos(0);
		hcs = HubDelegate.getCurrentState(hubLocation, null, null);
		assertEquals(HubDelegate.HubCurrentStateEnum.InSync, hcs);

		Program prog2 = new Program();
		hubProgram.add(prog2);
		hubProgram.setAO(prog2);

		HubDetailDelegate.setMasterObject(hubLocation, prog);

		hcs = HubDelegate.getCurrentState(hubLocation, null, null);
		assertEquals(HubDelegate.HubCurrentStateEnum.DetailHubNotSameAsMasterObject, hcs);

		Hub<Location> hubLocation2 = new Hub<>(Location.class);

		hcs = HubDelegate.getCurrentState(hubLocation, null, null);
		assertEquals(HubDelegate.HubCurrentStateEnum.DetailHubNotSameAsMasterObject, hcs);
	}

	@Test
	public void getCurrentStateTest2() {
		reset();

		Hub<Program> hubProgram = new Hub<Program>(Program.class);
		Hub<Location> hubLocation = new Hub<>(Location.class);

		HubMerger<Program, Location> hm = new HubMerger<>(hubProgram, hubLocation, "locations");

		HubDelegate.HubCurrentStateEnum hcs = HubDelegate.getCurrentState(hubLocation, null, null);
		assertEquals(HubDelegate.HubCurrentStateEnum.InSync, hcs);

		Program prog = new Program();
		hubProgram.add(prog);
		hubProgram.setPos(0);

		hcs = HubDelegate.getCurrentState(hubLocation, null, null);
		assertEquals(HubDelegate.HubCurrentStateEnum.InSync, hcs);

		Location loc = new Location();
		prog.getLocations().add(loc);

		hcs = HubDelegate.getCurrentState(hubLocation, null, null);
		assertEquals(HubDelegate.HubCurrentStateEnum.InSync, hcs);
	}

	@Test
	public void getCurrentStateTest3() {
		reset();

		Hub<Program> hubProgram = new Hub<Program>(Program.class);
		Hub<Location> hubLocation = hubProgram.getDetailHub(Program.P_Locations);

		Hub<Employee> hubEmployee = new Hub<>(Employee.class);

		HubMerger<Location, Employee> hm = new HubMerger<>(hubLocation, hubEmployee, "employees", true);

		HubDelegate.HubCurrentStateEnum hcs = HubDelegate.getCurrentState(hubLocation, null, null);
		assertEquals(HubDelegate.HubCurrentStateEnum.InSync, hcs);

		Program prog = new Program();
		hubProgram.add(prog);
		hubProgram.setPos(0);

		hcs = HubDelegate.getCurrentState(hubLocation, null, null);
		assertEquals(HubDelegate.HubCurrentStateEnum.InSync, hcs);

		Location loc = new Location();
		prog.getLocations().add(loc);

		Employee emp = new Employee();
		emp.setLastName("test");
		loc.getEmployees().add(emp);

		hcs = HubDelegate.getCurrentState(hubLocation, null, null);
		assertEquals(HubDelegate.HubCurrentStateEnum.InSync, hcs);

		Program prog2 = new Program();
		hubProgram.add(prog2);
		hubProgram.setAO(prog2);

		prog2.getLocations().add(new Location());
		prog2.getLocations().getAt(0).getEmployees().add(new Employee());

		hcs = HubDelegate.getCurrentState(hubLocation, null, null);
		assertEquals(HubDelegate.HubCurrentStateEnum.InSync, hcs);

		HubDetailDelegate.setMasterObject(hubLocation, prog);
		hcs = HubDelegate.getCurrentState(hubLocation, null, null);
		assertEquals(HubDelegate.HubCurrentStateEnum.DetailHubNotSameAsMasterObject, hcs);

		hcs = HubDelegate.getCurrentState(hubEmployee, null, null);
		assertEquals(HubDelegate.HubCurrentStateEnum.HubMergerNotUpdated, hcs);

		Hub<Employee> hubx = new Hub(Employee.class);

		hcs = HubDelegate.getCurrentState(hubEmployee, hubx, null);
		assertEquals(HubDelegate.HubCurrentStateEnum.HubMergerNotUpdated, hcs);
	}

}
