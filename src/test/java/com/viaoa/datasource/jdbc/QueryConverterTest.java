package com.viaoa.datasource.jdbc;

import org.junit.Test;

import static org.junit.Assert.*;

import com.viaoa.OAUnitTest;
import com.viaoa.datasource.jdbc.OADataSourceJDBC;

import test.hifive.DataSource;
import test.xice.tsac3.model.oa.*;

public class QueryConverterTest extends OAUnitTest {

    @Test
    public void Test() throws Exception {
        init();
        // hi5 datasource
        DataSource ds = new DataSource();
        ds.open();
        OADataSourceJDBC oads = ds.getOADataSource();

        
        
        oads.close();
        reset();
    }
    
}
