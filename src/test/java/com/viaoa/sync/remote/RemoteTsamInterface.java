// Copied from OATemplate project by OABuilder 12/13/15 02:58 PM
package com.viaoa.sync.remote;

import java.util.ArrayList;

import com.viaoa.hub.Hub;
import com.viaoa.remote.multiplexer.annotation.*;
import com.viaoa.util.OAProperties;

import test.xice.tsam.model.oa.AdminUser;
import test.xice.tsam.model.oa.Command;
import test.xice.tsam.model.oa.MRADClient;
import test.xice.tsam.model.oa.MRADServerCommand;
import test.xice.tsam.model.oa.Server;

@OARemoteInterface
public interface RemoteTsamInterface {

    public final static String BindName = "RemoteTsamTest";

    
    public MRADServerCommand createMRADServerCommand(AdminUser user, Hub<MRADClient> hub, Command command);
    
    public boolean runCommand(MRADServerCommand cmd);
    
    
}
