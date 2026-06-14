package com.viaoa.hub;

import org.junit.Test;
import static org.junit.Assert.*;

import com.viaoa.OAUnitTest;

import test.xice.tsac3.model.oa.*;

public class SharedHubTest extends OAUnitTest {

    @Test
    public void test() {
        
    }
    
}

/**
Used for sharing the same objects that are in another Hub, with or without using the same active object.
As changes are made to the objects, both/all Hubs will be notified.
<p>    
Shared Hubs are useful in GUI applications where the same collection of objects are needed for different purposes.
<p>
Example:<br>
A JTable that uses a Hub of Department objects to display and maintain all of the Departments.
A shared Hub could be created to use the same objects in a drop down list to select the Department for an 
Employee. Both Hubs would be using the same objects, but for different purposes.
<pre>
Hub hubDepartment = new Hub(Department.class);
hubDepartment.select();
SharedHub hubDepartment2 = new SharedHub(hubDepartment);
</pre>

@since 2004/03/19 using methods built into Hub.  see {@link Hub#createSharedHub}
@see Hub
*/

