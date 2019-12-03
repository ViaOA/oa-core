package com.viaoa;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.ds.OADataSource;
import com.viaoa.ds.autonumber.NextNumber;
import com.viaoa.ds.autonumber.OADataSourceAuto;
import com.viaoa.ds.objectcache.OADataSourceObjectCache;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObjectCacheDelegate;
import com.viaoa.object.OAObjectDelegate;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.util.OALogUtil;

public class OAUnitTest {
    private static Logger LOG = Logger.getLogger(OAUnitTest.class.getName());

    protected OADataSourceAuto dsAuto;
    protected OADataSourceAuto dsCache;
    
    protected OADataSource getDataSource() {
        return getAutoDataSource();
    }
    
    protected OADataSource getAutoDataSource() {
        if (dsAuto == null) {
            dsAuto = new OADataSourceAuto();
        }
        return dsAuto;
    }
    protected OADataSource getCacheDataSource() {
        if (dsCache == null) {
            dsCache = new OADataSourceObjectCache();
        }
        return dsCache;
    }
    
    public void init() {
        reset();
    }
    protected void reset()  {
        reset(true);
    }
    protected void reset(boolean bCreateNewDS)  {
        try {
            _reset(bCreateNewDS);
        }
        catch (Exception e) {
            LOG.log(Level.WARNING, "exception calling reset", e);
        }
    }
    protected void _reset(boolean bCreateNewDS) throws Exception {
        OALogUtil.consoleOnly(Level.FINE, "com.viaoa");
        OAObjectCacheDelegate.setUnitTestMode(true);
        OAObjectCacheDelegate.resetCache();
        OAObjectCacheDelegate.setUnitTestMode(false);
        OAThreadLocalDelegate.clearSiblingHelpers();
        
        OADataSource.closeAll();
        dsCache = dsAuto = null;
        
        OAObjectDelegate.setNextGuid(0);
        OADataSourceAuto.setGlobalNextNumber(null);
        
        if (bCreateNewDS) {
            getCacheDataSource().setAssignIdOnCreate(true);
            getAutoDataSource().setAssignIdOnCreate(true);
        }
    }
    
    
    protected void reset_OLD() {
/*qqqqq        
        if (dsCache != null) {
            dsCache.close();
            dsCache.setGlobalNextNumber(null);
            dsCache = null;
        }
        if (dsAuto != null) {
            dsAuto.close();
            dsAuto.setGlobalNextNumber(null);
            dsAuto = null;
        }
        OAObjectCacheDelegate.clearCache(NextNumber.class);
        OADataSource.closeAll();
        OAObjectCacheDelegate.clearCache();
        OAObjectDelegate.setNextGuid(0);
        OAObjectCacheDelegate.removeAllSelectAllHubs();
        OAThreadLocalDelegate.clearSiblingHelpers();
*/        
    }

    public void delay() {
        delay(1);
    }
    public void delay(int ms) {
        try {
            if (ms > 0) Thread.sleep(ms);
            else Thread.yield();
        }
        catch (Exception e) {}
    }
}

