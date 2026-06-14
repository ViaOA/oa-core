package com.viaoa.hub;

import org.junit.Test;
import static org.junit.Assert.*;

import com.viaoa.OAUnitTest;

import test.xice.tsac3.model.oa.*;

public class HubRootTest extends OAUnitTest {

    @Test
    public void test() {
        
    }
    
}

/**
 * Used for recursive Hubs, so that the hub is always using the rootHub. The default behavior when using a recursive Hub is that the hub
 * will be shared to whatever hub the AO is set to. This will allow a hubRoot to always be the top level hub. example: // get the original
 * root this.hub = hubModel.getDetailHub(Model.P_Containers).createSharedHub(); // create empty hub that will then always contain the root
 * hub objects this.hubRoot = new Hub&lt;Container&gt;(Container.class); new HubRoot(hub, hubRoot); // hubRoot will always have top level
 * hub, initially using hub ... OATreeNode node = new OATreeNode(App.P_Label, hubRoot, hub) ...
 *
 * @author vvia 20120302
 */
