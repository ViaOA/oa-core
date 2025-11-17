package com.viaoa.datasource;

import org.junit.Test;

import static org.junit.Assert.*;

import com.viaoa.OAUnitTest;
import com.viaoa.datasource.OASelect;
import com.viaoa.object.OAFinder;
import com.viaoa.util.OAFilter;

import test.xice.tsac3.model.Model;
import test.xice.tsac3.Tsac3DataGenerator;
import test.xice.tsac3.model.oa.Server;
import test.xice.tsac3.model.oa.Site;
import test.xice.tsac3.model.oa.propertypath.SitePP;

public class OASelectTest extends OAUnitTest {

    @Test
    public void selectTest() {
        reset();

        OASelect<Site> selSite = new OASelect<Site>(Site.class);
        assertFalse(selSite.getDirty());
        selSite.setDirty(true);
        assertTrue(selSite.getDirty());
        selSite.setDirty(false);
        assertFalse(selSite.getDirty());
        
        assertNull(selSite.getOrder());
        selSite.setOrder("xxx");
        assertEquals(selSite.getOrder(), "xxx");
        selSite.setOrder(null);
        assertNull(selSite.getOrder());
        
        
        // specific tests
        Model modelTsac = new Model();
        Tsac3DataGenerator data = new Tsac3DataGenerator(modelTsac);
        data.createSampleData();

        selSite = new OASelect<Site>(Site.class);
        selSite.select();
        assertFalse(selSite.hasMore());
        selSite.cancel();
        
        
        selSite = new OASelect<Site>(Site.class);
        selSite.setSearchHub(modelTsac.getSites());
        selSite.select();
        assertTrue(selSite.hasMore());
        for ( ;;) {
            assertNotNull(selSite.next());
            if (!selSite.hasMore()) break;
        }
        selSite.reset();
        selSite.select();
        assertTrue(selSite.hasMore());
        for ( ;;) {
            assertNotNull(selSite.next());
            if (!selSite.hasMore()) break;
        }
        
        selSite.reset();
        // add filter that wont return any matches
        selSite.setFilter(new OAFilter<Site>() {
            @Override
            public boolean isUsed(Site obj) {
                return false;
            }
        });
        assertFalse(selSite.hasMore());
        assertFalse(selSite.isCancelled());

        selSite.reset();
        assertNotNull(selSite.getFilter());
        selSite.setFilter(null);
        assertNull(selSite.getFilter());
        selSite.select();
        assertTrue(selSite.hasMore());
        assertFalse(selSite.isCancelled());
        selSite.cancel();
        assertFalse(selSite.hasMore());
        assertTrue(selSite.isCancelled());
        
        
        OASelect<Server> selServer = new OASelect<Server>(Server.class);
        OAFinder<Site, Server> finder = new OAFinder<Site, Server>(modelTsac.getSites(), SitePP.environments().silos().servers().pp);
        selServer.setFinder(finder);
        selServer.setFilter(new OAFilter<Server>() {
            @Override
            public boolean isUsed(Server obj) {
                return obj != null && obj.getId() == 5;
            }
        });
        selServer.select();
        assertTrue(selServer.hasMore());
        Server serx = selServer.next();
        assertNotNull(serx);
        assertEquals(serx.getId(), 5);
        assertFalse(selSite.hasMore());
        
        reset();
    }
    
}

/**
 * Helper Class used for submitting and managing queries for any OADataSource. This is used by Hub.select() methods. All queries are based
 * on object names, property names, and property paths.
 * <p>
 * A <b>property path</b> is a dot (".") separated list of property names that are used to navigate from a root Class to a property value.
 * To go from object to object, reference property names are used.
 * <p>
 * An OAFinder can be used to act as the datasource.
 * <p>
 * An OAFilter can be used to further filter the results.
 * <p>
 * Queries
 * <ul>
 * <li>All property names and connectors names are case insensitive.
 * <li>Can use the following connectors "AND", "&amp;&amp;", "||", "OR", "(", ")"
 * <li>Can use "=", "==", "!=", "&lt;", "&lt;=", "&gt;", "&gt;=", "LIKE", "%" (wildcard), "null" (any case)
 * <li>use "PASS[" to begin a passthru part of the query, and "]THRU" to end it.
 * <li>"ASC" ascending, "DESC" descending can be used with Order By properties.
 * </ul>
 *
 * <pre>
 * OASelect select = new OASelect();
 * String query = OAConverter.toDataSourceString("dept", dept); // converts to dept.Id = 'MIS'
 * String fname = "John";
 * query += " &amp;&amp; (dept.manager.lastName like 'Jones%'";
 * query += " || (dept.manager.firstName == " + OAConvert.toDataSourceString(fname) + ")";
 * select.setWhere(query);
 * select.setOrder("dept.name, Emp.LastName DESC, emp.firstName");
 * select.setPassthru(false); // needs to be converted to native query language
 * select.setCountFirst(false); // dont need count
 * select.setMax(250); // only select first 250 objects.  (default=0 ALL)
 * select.setFetchAmount(40); // amount of objects to read at a time (default=45)
 *
 * // or use params for where query
 * query = "dept = ? &amp;&amp; dept.manager.lastName like ? || dept.manager.firstname = ?";
 * Object[] params = new Object[] { dept, "Jones%", fname };
 * select.setWhere(query);
 * select.setParams(params);
 * </pre>
 * <p>
 * For more information about this package, see <a href="package-summary.html#package_description">documentation</a>.
 */
