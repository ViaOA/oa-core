package com.viaoa.hub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.viaoa.OAUnitTest;
import com.viaoa.object.OAObjectHubDelegate;

import test.hifive.model.oa.Address;
import test.hifive.model.oa.AwardType;
import test.hifive.model.oa.Employee;
import test.hifive.model.oa.EmployeeAward;
import test.hifive.model.oa.Location;
import test.hifive.model.oa.PointsAwardLevel;
import test.hifive.model.oa.Value;
import test.hifive.model.oa.propertypath.EmployeePP;
import test.hifive.model.oa.propertypath.PointsAwardLevelPP;

public class HubFilterTest extends OAUnitTest {

	@Test
	public void testA() {
		// dependents with and w/o "."

		Hub<Employee> hubMaster = new Hub<Employee>(Employee.class);
		Hub<Employee> hubFiltered = new Hub<Employee>(Employee.class);

		HubFilter<Employee> hf = new HubFilter<Employee>(hubMaster, hubFiltered);
		hf.addDependentProperty(Employee.P_LastName);
		hf.addDependentProperty(Employee.P_FirstName);

		// make sure that a hubMerger is not created
		HubListener[] hls = HubEventDelegate.getAllListeners(hubMaster);
		assertNotNull(hls);
		assertEquals(1, hls.length);

		hf.addDependentProperty(Employee.P_Location + "." + Location.P_Name);
		HubListener[] hls2 = HubEventDelegate.getAllListeners(hubMaster);
		assertNotNull(hls2);
		assertTrue(hls2.length > 1);
		assertEquals(hls[0], hls2[0]);
	}

	@Test
	public void test() {
		init();

		Hub<PointsAwardLevel> hubMaster = new Hub<PointsAwardLevel>(PointsAwardLevel.class);

		for (int i = 0; i < 200; i++) {
			PointsAwardLevel pal = new PointsAwardLevel();
			hubMaster.add(pal);
		}

		hubMaster.saveAll();

		_test(hubMaster);

		for (int i = 0; i < 10; i++) {
			System.gc();
		}

		PointsAwardLevel pal = new PointsAwardLevel();
		hubMaster.add(pal);
		// should have caused hubFilters to be closed
	}

	public void _test(final Hub<PointsAwardLevel> hubMasterMain) {
		System.out.println("HubFilterTest, thread=" + Thread.currentThread().getName());
		for (int i = 0; i < 50; i++) {
			final Hub<PointsAwardLevel> hubMaster = hubMasterMain.createSharedHub();
			final Hub<PointsAwardLevel> hubFiltered = new Hub<PointsAwardLevel>(PointsAwardLevel.class);
			//hubMaster.copyInto(hubFiltered);

			final AtomicInteger aiCnt = new AtomicInteger();
			HubFilter<PointsAwardLevel> hf = new HubFilter<PointsAwardLevel>(hubMaster, hubFiltered) {
				public boolean isUsed(PointsAwardLevel level) {
					return true;
				}

				@Override
				protected void addObject(PointsAwardLevel obj, boolean bIsInitialzing) {
					aiCnt.incrementAndGet();
					super.addObject(obj, bIsInitialzing);
				}
			};
			hf.addDependentProperty("id");
			if (i % 5 == 0) {
				hf.addDependentProperty(PointsAwardLevelPP.location().id());
			}
			int x = hubFiltered.getSize();
			if (i % 5 == 0) {
				for (int j = 0; j < 1; j++) {
					System.gc();
				}
			}

			//System.out.println("i="+i+", hubFiltered.getSize="+hubFiltered.getSize());

			assertEquals(200, hubFiltered.getSize());
			hf.close();

			x = HubEventDelegate.getListenerCount(hubMaster);
		}
	}

	@Test
	public void test2() {
		OAObjectHubDelegate.ShowWarnings = false;
		final int max = 5;

		final Hub<PointsAwardLevel> hubMaster1 = new Hub<PointsAwardLevel>(PointsAwardLevel.class);
		for (int i = 0; i < 200; i++) {
			PointsAwardLevel pal = new PointsAwardLevel();
			hubMaster1.add(pal);
		}
		final Hub<PointsAwardLevel> hubMaster2 = new Hub<PointsAwardLevel>(PointsAwardLevel.class);
		for (int i = 0; i < 200; i++) {
			PointsAwardLevel pal = new PointsAwardLevel();
			hubMaster2.add(pal);
		}

		final CyclicBarrier barrier = new CyclicBarrier(max);
		final CountDownLatch countDownLatch = new CountDownLatch(max);
		final AtomicInteger aiDone = new AtomicInteger();
		final AtomicInteger aiError = new AtomicInteger();

		for (int i = 0; i < max; i++) {
			final int id = i;
			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						barrier.await();
						Hub<PointsAwardLevel> hub = (id % 2 == 0) ? hubMaster1 : hubMaster2;
						aiError.getAndIncrement();
						_test(hub);
						aiError.getAndDecrement();
					} catch (Throwable e) {
						System.out.println("HubFilterTest error: " + e);
						e.printStackTrace();
					} finally {
						aiDone.getAndIncrement();
						countDownLatch.countDown();
					}
				}
			}, "ThreadX." + i);
			t.start();
		}

		for (int i = 0;; i++) {
			try {
				countDownLatch.await(1, TimeUnit.SECONDS);
				if (aiDone.get() == max) {
					break;
				}
				hubMaster1.setPos(i % hubMaster1.getSize());
				hubMaster2.setPos(i % hubMaster2.getSize());
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		OAObjectHubDelegate.ShowWarnings = true;
		assertEquals("error from threads, errorCnt=" + aiError.get(), 0, aiError.get());
	}

	@Test
	public void test3() {
		Hub<Employee> hub = new Hub<Employee>(Employee.class);
		for (int i = 0; i < 20; i++) {
			Employee emp = new Employee();
			emp.setId(i);
			hub.add(emp);
		}
		Hub<Employee> hubFiltered = new Hub<Employee>(Employee.class);

		final AtomicInteger aiCnt = new AtomicInteger();
		hub.onAdd(onEvent -> {
			aiCnt.incrementAndGet();
		});

		final AtomicInteger aiCnt2 = new AtomicInteger();
		hubFiltered.onAdd(onEvent -> {
			aiCnt2.incrementAndGet();
		});

		HubFilter<Employee> hf = new HubFilter<Employee>(hub, hubFiltered) {
			@Override
			public boolean isUsed(Employee emp) {
				return emp.getId() % 5 == 0;
			}
		};
		hf.addDependentProperty("id");

		assertEquals(4, hubFiltered.size());

		Employee emp = hub.getAt(0);
		emp.setId(99);
		assertEquals(3, hubFiltered.size());
		emp.setId(0);
		assertEquals(4, hubFiltered.size());
		hub.remove(emp);
		assertEquals(3, hubFiltered.size());
		hub.add(emp);
		assertEquals(4, hubFiltered.size());

		assertEquals(1, aiCnt.get());
		assertEquals(2, aiCnt2.get());
	}

	@Test
	public void test4() {
		reset();

		Location location = new Location();
		location.setId(0);

		Hub<Employee> hub = new Hub<Employee>(Employee.class);
		for (int i = 0; i < 20; i++) {
			Employee emp = new Employee();
			emp.setId(i);
			emp.setFirstName("fn" + i);
			emp.setLastName("ln" + i);
			emp.setLocation(location);
			hub.add(emp);
		}
		Hub<Employee> hubFiltered = new Hub<Employee>(Employee.class);

		HubFilter<Employee> hf = new HubFilter<Employee>(hub, hubFiltered) {
			@Override
			public boolean isUsed(Employee emp) {
				if (emp.getId() % 5 != 0) {
					return false;
				}
				Location loc = emp.getLocation();
				if (loc == null || loc.getId() != 0) {
					return false;
				}

				String s = "fn" + emp.getId() + " ln" + emp.getId();
				return s.equals(emp.getFullName());
			}
		};
		hf.addDependentProperty(Employee.P_FullName);
		hf.addDependentProperty(Employee.P_Id);

		assertEquals(4, hubFiltered.size());

		Employee emp = hub.getAt(0);
		emp.setId(99);
		assertEquals(3, hubFiltered.size());
		emp.setId(0);
		assertEquals(4, hubFiltered.size());
		hub.remove(emp);
		assertEquals(3, hubFiltered.size());
		hub.add(emp);
		assertEquals(4, hubFiltered.size());

		location.setId(1);
		assertEquals(4, hubFiltered.size());
		location.setId(0);
		assertEquals(4, hubFiltered.size());

		hf.addDependentProperty(EmployeePP.location().id());

		assertEquals(4, hubFiltered.size());

		emp.setId(99);
		assertEquals(3, hubFiltered.size());
		emp.setId(0);
		assertEquals(4, hubFiltered.size());
		hub.remove(emp);
		assertEquals(3, hubFiltered.size());
		hub.add(emp);
		assertEquals(4, hubFiltered.size());

		location.setId(1);
		assertEquals(0, hubFiltered.size());
		location.setId(0);
		assertEquals(4, hubFiltered.size());

		emp.setFirstName("");
		assertEquals(3, hubFiltered.size());
		emp.setFirstName("fn0");
		assertEquals(4, hubFiltered.size());
	}

	@Test
	public void testC() {
		Hub<Value> hubValue = new Hub<Value>(Value.class);
		hubValue.add(new Value());

		Hub<Value> hubValueFiltered = new Hub<Value>(Value.class);
		final Hub<EmployeeAward> hubEmployeeAward = new Hub<EmployeeAward>(EmployeeAward.class);

		EmployeeAward ea = new EmployeeAward();
		AwardType at = new AwardType();
		at.setValue(50);
		ea.setAwardType(at);

		Hub<AwardType> hubAwardType = new Hub<AwardType>();
		hubAwardType.add(at);

		hubEmployeeAward.add(ea);
		hubEmployeeAward.setPos(0);

		final AtomicInteger ai = new AtomicInteger();
		final HubFilter<Value> hfCardValue = new HubFilter<Value>(hubValue, hubValueFiltered, true) {
			public boolean isUsed(Value object) {
				ai.incrementAndGet();
				return true;
			}
		};
		// {P_AwardType+"."+AwardType.P_Value, P_AwardCardOrders+"."+AwardCardOrder.P_Value, P_IsOpen, P_InternationalVisaAmount, P_AddOnProductSelectedDate, P_EmployeeAwardCharities+"."+EmployeeAwardCharity.P_Value})

		hfCardValue.addDependentProperty(hubEmployeeAward, EmployeeAward.P_Balance, true);

		//qqqqqqqq test closing HubFilter to see if it is cleaned up

		ai.set(0);
		at.setValue(51);

		assertEquals(1, ai.get());
	}

	public static void XXmain(String[] args) throws Exception {
		/*
		System.out.println("first of two 30 second count down");
		for (int i=0; i<120; i++) {
		    if (i%4==0) System.out.println("countdown "+((120-i)/4));
		    Thread.sleep(250);
		}
		
		ArrayList<PointsAwardLevel> al = new ArrayList<PointsAwardLevel>();
		for (int i=0; i<100000; i++) {
		    PointsAwardLevel pal = new PointsAwardLevel();
		    al.add(pal);
		}
		
		System.out.println("second of two 30 second count down");
		for (int i=0; i<120; i++) {
		    if (i%4==0) System.out.println("countdown "+((120-i)/4));
		    Thread.sleep(250);
		}
		*/
		HubFilterTest test = new HubFilterTest();
		//        test.test();
		System.out.println("test is done");
		/*
		for (;;) {
		    Thread.sleep(30 * 1000);
		}
		*/
	}

	@Test
	public void testD() {
		// dependents with and w/o "."

		Hub<Employee> hubMaster = new Hub<Employee>(Employee.class);
		Hub<Employee> hubFiltered = new Hub<Employee>(Employee.class);

		HubFilter<Employee> hf = new HubFilter<Employee>(hubMaster, hubFiltered) {
			@Override
			public boolean isUsed(Employee emp) {
				return (emp.getLocation() != null);
			}
		};
		hf.addDependentProperty(Employee.P_Location);

		assertEquals(0, hubFiltered.size());
		Employee emp = new Employee();
		hubMaster.add(emp);
		assertEquals(0, hubFiltered.size());
		emp.setLocation(new Location());
		assertEquals(1, hubFiltered.size());

	}

	@Test
	public void testE() {
		Hub<Employee> hub = new Hub<Employee>(Employee.class);
		for (int i = 100; i < 120; i++) {
			Employee emp = new Employee();
			emp.setId(i);
			hub.add(emp);
		}
		Hub<Employee> hubFiltered = new Hub<Employee>(Employee.class);

		final AtomicInteger aiAdd = new AtomicInteger();
		hub.onAdd(onEvent -> {
			aiAdd.incrementAndGet();
		});
		final AtomicInteger aiRemove = new AtomicInteger();
		hub.onRemove(onEvent -> {
			aiRemove.incrementAndGet();
		});

		final AtomicInteger aiAdd2 = new AtomicInteger();
		hubFiltered.onAdd(onEvent -> {
			aiAdd2.incrementAndGet();
		});
		final AtomicInteger aiRemove2 = new AtomicInteger();
		hubFiltered.onRemove(onEvent -> {
			aiRemove2.incrementAndGet();
		});

		HubFilter<Employee> hf = new HubFilter<Employee>(hub, hubFiltered) {
			@Override
			public boolean isUsed(Employee emp) {
				return emp.getId() % 5 == 0;
			}
		};
		hf.addDependentProperty("id");

		assertEquals(4, hubFiltered.size());

		Employee emp = hub.getAt(0);
		emp.setId(199);
		assertEquals(3, hubFiltered.size());
		emp.setId(100);
		assertEquals(4, hubFiltered.size());
		assertEquals(0, aiRemove.get());
		hub.remove(emp);
		assertEquals(3, hubFiltered.size());
		hub.add(emp);
		assertEquals(4, hubFiltered.size());

		assertEquals(1, aiAdd.get());
		assertEquals(2, aiAdd2.get());

		assertEquals(1, aiRemove.get());
		assertEquals(2, aiRemove2.get());
	}

	@Test
	public void testF() {
		Hub<Employee> hub = new Hub<Employee>(Employee.class);
		for (int i = 200; i < 220; i++) {
			Employee emp = new Employee();
			emp.setId(i);
			hub.add(emp);
		}
		Hub<Employee> hubFiltered = new Hub<Employee>(Employee.class);

		final AtomicInteger aiAdd = new AtomicInteger();
		hub.onAdd(onEvent -> {
			aiAdd.incrementAndGet();
		});
		final AtomicInteger aiRemove = new AtomicInteger();
		hub.onRemove(onEvent -> {
			aiRemove.incrementAndGet();
		});
		final AtomicInteger aiRemoveAll = new AtomicInteger();
		hub.addHubListener(new HubListenerAdapter<Employee>() {
			@Override
			public void afterRemoveAll(HubEvent<Employee> e) {
				aiRemoveAll.incrementAndGet();
			}
		});

		final AtomicInteger aiAdd2 = new AtomicInteger();
		hubFiltered.onAdd(onEvent -> {
			aiAdd2.incrementAndGet();
		});
		final AtomicInteger aiRemove2 = new AtomicInteger();
		hubFiltered.onRemove(onEvent -> {
			aiRemove2.incrementAndGet();
		});
		final AtomicInteger aiRemoveAll2 = new AtomicInteger();
		hubFiltered.addHubListener(new HubListenerAdapter<Employee>() {
			@Override
			public void afterRemoveAll(HubEvent<Employee> e) {
				aiRemoveAll2.incrementAndGet();
			}
		});

		HubFilter<Employee> hf = new HubFilter<Employee>(hub, hubFiltered) {
			@Override
			public boolean isUsed(Employee emp) {
				return emp.getId() % 5 == 0;
			}
		};
		hf.addDependentProperty("id");

		Employee emp = new Employee();
		emp.setId(230);
		hubFiltered.add(emp);

		assertEquals(1, aiAdd.get());
		assertEquals(1, aiAdd2.get());

		assertEquals(0, aiRemove.get());
		assertEquals(0, aiRemove2.get());

		hubFiltered.clear();
		assertEquals(0, aiRemoveAll.get());
		assertEquals(2, aiRemoveAll2.get());
	}

	@Test
	public void testG() {
		Hub<Employee> hub = new Hub<Employee>(Employee.class);
		for (int i = 300; i < 320; i++) {
			Employee emp = new Employee();
			emp.setId(i);
			hub.add(emp);
			emp.getAddresses().add(new Address());
		}

		Hub<Address> hubAddressDetail = hub.getDetailHub(Employee.P_Addresses);

		final AtomicInteger aiAdd = new AtomicInteger();
		hubAddressDetail.onAdd(onEvent -> {
			aiAdd.incrementAndGet();
		});
		final AtomicInteger aiRemove = new AtomicInteger();
		hubAddressDetail.onRemove(onEvent -> {
			aiRemove.incrementAndGet();
		});

		Hub<Address> hubAddress = new Hub<Address>(Address.class);

		final AtomicInteger aiAdd2 = new AtomicInteger();
		hubAddress.onAdd(onEvent -> {
			aiAdd2.incrementAndGet();
		});
		final AtomicInteger aiRemove2 = new AtomicInteger();
		hubAddress.onRemove(onEvent -> {
			aiRemove2.incrementAndGet();
		});

		HubFilter<Address> hf = new HubFilter<Address>(hubAddressDetail, hubAddress) {
			@Override
			public boolean isUsed(Address address) {
				return true;
			}
		};

		assertEquals(0, aiAdd.get());
		assertEquals(0, aiAdd2.get());
		assertEquals(0, aiRemove.get());
		assertEquals(0, aiRemove2.get());

		hub.setPos(0);

		assertEquals(0, aiAdd.get());
		assertEquals(0, aiAdd2.get());
		assertEquals(0, aiRemove.get());
		assertEquals(0, aiRemove2.get());

		hub.get(0).getAddresses().add(new Address());

		assertEquals(1, aiAdd.get());
		assertEquals(1, aiAdd2.get());
		assertEquals(0, aiRemove.get());
		assertEquals(0, aiRemove2.get());

		hubAddress.clear();

		assertEquals(1, aiAdd.get());
		assertEquals(1, aiAdd2.get());
		assertEquals(0, aiRemove.get());
		assertEquals(0, aiRemove2.get());
	}
}
