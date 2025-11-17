package com.viaoa.hub;

import org.junit.Test;

import static org.junit.Assert.*;

import com.viaoa.OAUnitTest;

import test.xice.tsac.DataGenerator;
import test.xice.tsac.delegate.ModelDelegate;
import test.xice.tsac.model.Model;
import test.xice.tsac.model.oa.*;

public class DetailHubTest extends OAUnitTest {

	@Test
    public void testDetailHub() {
        reset();

        Model model = new Model();
        
        DataGenerator dg = new DataGenerator(model);
        
        Hub<Site> hubSite = ModelDelegate.getSites();
        hubSite.setAO(null);
        assertNull(hubSite.getAO());

        DetailHub<Environment> dhEnv = new DetailHub(hubSite, Site.P_Environments);
        assertNull(dhEnv.getAO());
        
        DetailHub<Silo> dhSilo = new DetailHub(dhEnv, Environment.P_Silos);
        assertNull(dhSilo.getAO());

        hubSite.setPos(0);
        
//qqqqqqqqqqqqq gen sample data
        
//        assertNotNull(hubSite.getAO());
        
        assertNull(dhEnv.getAO());
		
	}
	
	
	
	
}


/**
A DetailHub is a Hub that automatically contains the object(s) of a property from the active object
of another Hub (master).  This is referred as a <i>Master/Detail</i> relationship.
<p>
Whenever the active object of the master Hub is changed, the Detail Hub will automatically be updated
to include the objects of a property in the active object.
<p>
Example:<br>
A Department Class has many Employees (using a Hub).  A DetailHub can be created using a Hub of Department objects
as the master Hub.  This DetailHub will automatically contain the Employee objects for whichever Department is
currenly the active object in the master Hub.
<br>
&nbsp;&nbsp;&nbsp;<img src="doc-files/Hub3.gif" alt="">
<br>
Using the diagram, the detail Hub is populated with the Employee objects from the Department that is the
active object in the master Hub. Actually, the detail Hub is not really populated, but rather it uses
the same Data that the Dept B Employee Hub is using. If an Employee object is added to the active Department's
Employee Hub, the Detail Hub would also contain this Employee. 
<p>
In this example, a UI Component (ex: JTable) could be setup to list the Department objects and another UI Component
(ex: JTable) could list the Employee objects from the Department that is selected. If another Department is
selected, then the JTable listing the Employees will show that Departments Employee objects.
<pre>
* Hub hubDept = new Hub(Department.class);   // create new Hub for Department objects
* hubDept.select();      // select all departments from datasource
* Hub hubEmp = new HubDetail(hubDept, "Employees"); // create Hub that will automatically
*                                                   //  contain the Employee objects
*                                                   //  for the active Department
* // Or
* Hub hubEmp = new HubDetail(hubDept,"Employees", "lastName, firstName"); // sets sort order
*
</pre>
@see Hub
*/

