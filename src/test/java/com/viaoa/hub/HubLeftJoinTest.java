package com.viaoa.hub;

import org.junit.Test;
import static org.junit.Assert.*;

import com.viaoa.OAUnitTest;

import test.xice.tsac3.model.oa.*;

public class HubLeftJoinTest extends OAUnitTest {

    @Test
    public void test() {
        
    }
    
}

/*
 * Combines two hubs into a new single hub to create the equivalent of
 * a database left join, where all of the "left" side objects are in the list.
 *
 * The combined Hub (see getCombinedHub) uses OAObject OALeftJoin&lt;A,B&gt;, where A is the
 * same class as the left Hub and B is the same as the right Hub.
 *
 * A property path that uses A or B will need to use casting.
 * Example:  LeftHub=hubDepartments, RightHub=hubEmployees with last name "Jones"
 *    the combined Hub A=Dept ref, B=Employee ref, can use hubCombined with properties
 *    from A or B, with casting:
 *       hubCombined, "(com.xxx.Department)A.manager.fullName"
 *         or : OAString.cpp(Departement.class, OALeftJoin.P_A, Department.P_Manager, Employee.P_FullName)
 *
 * see HubLeftJoinDetail#
 * see HubGroupBy#
 *
 * @author vvia
 */
