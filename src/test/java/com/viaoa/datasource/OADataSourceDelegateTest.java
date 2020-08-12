package com.viaoa.datasource;

import org.junit.Test;
import static org.junit.Assert.*;

import com.viaoa.OAUnitTest;
import com.viaoa.datasource.OADataSourceDelegate;
import com.viaoa.datasource.jdbc.OADataSourceJDBC;

import test.xice.tsac3.model.oa.*;
import test.xice.tsam.datasource.DataSource;

public class OADataSourceDelegateTest extends OAUnitTest {

    @Test
    public void getJDBCDataSourceTest() throws Exception {
        assertNull(OADataSourceDelegate.getJDBCDataSource());
        
        DataSource ds = new DataSource();
        ds.open();
        
        assertNotNull(OADataSourceDelegate.getJDBCDataSource());
        
        assertEquals(ds.getOADataSource(), OADataSourceDelegate.getJDBCDataSource());
        
        ds.close();
        assertNull(OADataSourceDelegate.getJDBCDataSource());
    }
    
    
    
}
