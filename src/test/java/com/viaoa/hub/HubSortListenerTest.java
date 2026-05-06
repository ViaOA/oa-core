package com.viaoa.hub;

import org.junit.Test;
import static org.junit.Assert.*;

import com.viaoa.OAUnitTest;
import com.viaoa.compare.OAComparator;

import test.xice.tsac3.model.oa.*;

public class HubSortListenerTest extends OAUnitTest {

    @Test
    public void test() {
        
    }
    
}
/**
HubSortListener is used to keep a Hub sorted by the Hubs sort/select order.  Used internally by
Hub.sort method.

Note:
For oa.cs, each client will maintain their own sorting.  If a sort property is changed, then each client will resort,
without any messages going to/from server.   
<p>
For more information about this package, see <a href="package-summary.html#package_description">documentation</a>.
@see Hub#sort(String,boolean) Hub.sort
@see OAComparator that is created based on propertyPaths 
*/
