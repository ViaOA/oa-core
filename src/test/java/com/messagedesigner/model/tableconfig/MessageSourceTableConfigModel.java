// Generated by OABuilder
package com.messagedesigner.model.tableconfig;

import java.util.logging.*;

import com.viaoa.object.*;
import com.messagedesigner.delegate.ModelDelegate;
import com.messagedesigner.model.*;
import com.messagedesigner.model.oa.*;
import com.messagedesigner.model.oa.method.*;
import com.messagedesigner.model.oa.tableconfig.*;
import com.messagedesigner.resource.Resource;
import com.viaoa.annotation.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import com.viaoa.datasource.*;

public class MessageSourceTableConfigModel {
    private static Logger LOG = Logger.getLogger(MessageSourceTableConfigModel.class.getName());
    
    // Hubs
    protected Hub<MessageSourceTableConfig> hub;
    
    // ObjectModels
    
    // object used for tableConfig data
    protected MessageSourceTableConfig tableConfig;
    
    public MessageSourceTableConfigModel() {
    }
    
    // object used to input query data, to be used by methodHub
    public MessageSourceTableConfig getTableConfig() {
        if (tableConfig == null) tableConfig = new MessageSourceTableConfig();
        return tableConfig;
    }
    
    public Hub<MessageSourceTableConfig> getHub() {
        if (hub == null) {
            hub = new Hub<MessageSourceTableConfig>(MessageSourceTableConfig.class);
            hub.add(getTableConfig());
            hub.setPos(0);
        }
        return hub;
    }
    
    
}

