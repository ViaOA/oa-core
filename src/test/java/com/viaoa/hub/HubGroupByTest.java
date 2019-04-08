package com.viaoa.hub;

import java.nio.charset.Charset;
import java.util.ArrayList;

import org.junit.Test;

import static org.junit.Assert.*;

import com.viaoa.OAUnitTest;

import static org.junit.Assert.*;

import com.viaoa.object.OAFinder;
import com.viaoa.object.OAGroupBy;
import com.viaoa.util.OAString;

import test.xice.tsac.DataGenerator;
import test.xice.tsac.model.Model;
import test.xice.tsac.model.oa.*;
import test.xice.tsac.model.oa.propertypath.*;

public class HubGroupByTest extends OAUnitTest {

    @Test
    public void Test() {
        init();
        Model model = new Model();
        DataGenerator data = new DataGenerator(model);
        data.createSampleData1();

        String pp = SitePP.environments().silos().pp;
        OAFinder<Site, Silo> finder = new OAFinder<Site, Silo>(model.getSites(), pp);
        ArrayList<Silo> al = finder.find();  // all silos

        Hub<Silo> hubSilo = new Hub<Silo>();
        new HubMerger(model.getSites(), hubSilo, pp, true);
        
        pp = SiloPP.siloType().pp;
        HubGroupBy<Silo, SiloType> hgb = new HubGroupBy<Silo, SiloType>(hubSilo, model.getSiloTypes(), pp);
        
        Hub<OAGroupBy<Silo, SiloType>> hubCombined = (Hub<OAGroupBy<Silo, SiloType>>) hgb.getCombinedHub();
        
        Object[] objs = model.getSiloTypes().toArray();
        objs = hubCombined.toArray();
        
        assertEquals(hubCombined.getSize(), model.getSiloTypes().getSize());
        
        Silo silo = new Silo();
        hubSilo.add(silo);
        objs = hubCombined.toArray();
        
        assertEquals(hubCombined.getSize(), model.getSiloTypes().getSize()+1);
        OAGroupBy gb = hubCombined.getAt(3);
    }
    
    
    @Test
    public void Test2() {
        init();
        Model model = new Model();
        DataGenerator data = new DataGenerator(model);
        data.createSampleData1();

        String pp = SitePP.environments().silos().pp;
        OAFinder<Site,Silo> f = new OAFinder<Site,Silo>(pp);
        Silo silo = f.findFirst(model.getSites());

        pp = SiloPP.servers().applications().pp;
        Hub<Application> hubApplication = new Hub<Application>();
        HubMerger<Silo,Application> hm  = new HubMerger<Silo, Application>(silo, hubApplication, pp);
        
        
        
        pp = ApplicationPP.applicationGroups().pp;
        
        
        Hub<ApplicationGroup> hubApplicationGroup = silo.getApplicationGroups();
        
        HubGroupBy<Application, ApplicationGroup> hgb = new HubGroupBy<Application, ApplicationGroup>(hubApplication, hubApplicationGroup, pp);

        Hub<OAGroupBy<Application, ApplicationGroup>> hubCombined = (Hub<OAGroupBy<Application, ApplicationGroup>>) hgb.getCombinedHub();
        
        assertEquals(1, hubCombined.size());
        assertEquals(hubCombined.getAt(0).getHub().size(), hubApplication.size());
        
        ApplicationGroup appGroup = new ApplicationGroup();
        silo.getApplicationGroups().add(appGroup);
        
        assertEquals(2, hubCombined.size());
        assertEquals(appGroup, hubCombined.getAt(1).getGroupBy());
        
        assertEquals(hubApplication.size(), hubCombined.getAt(0).getHub().size());
        assertEquals(0, hubCombined.getAt(1).getHub().size());
        
        appGroup.getApplications().add(hubApplication.getAt(0));
        
        assertEquals(hubCombined.getAt(0).getHub().size(), hubApplication.size()-1);
        assertEquals(1, hubCombined.getAt(1).getHub().size());
        
        assertEquals(hubCombined.getAt(1).getHub().getAt(0), hubApplication.getAt(0));
        
        assertEquals(1, hubApplication.getAt(0).getApplicationGroups().size()); 
        assertEquals(appGroup, hubApplication.getAt(0).getApplicationGroups().getAt(0)); 

        hubApplication.getAt(0).getApplicationGroups().clear();

        assertEquals(0, hubApplication.getAt(0).getApplicationGroups().size()); 
        assertEquals(0, hubApplicationGroup.getAt(0).getApplications().size()); 
        assertEquals(0, hubCombined.getAt(1).getHub().size());
        assertEquals(hubApplication.size(), hubCombined.getAt(0).getHub().size());


        // Test: add 3
        appGroup.getApplications().add(hubApplication.getAt(0));
        appGroup.getApplications().add(hubApplication.getAt(1));
        appGroup.getApplications().add(hubApplication.getAt(2));
        
        assertEquals(hubCombined.getAt(0).getHub().size(), hubApplication.size()-3);
        assertEquals(3, hubCombined.getAt(1).getHub().size());
        
        // clear
        hubApplication.getAt(0).getApplicationGroups().clear();
        hubApplication.getAt(1).getApplicationGroups().clear();
        hubApplication.getAt(2).getApplicationGroups().clear();

        assertEquals(0, hubApplication.getAt(0).getApplicationGroups().size()); 
        assertEquals(0, hubApplicationGroup.getAt(0).getApplications().size()); 
        assertEquals(0, hubCombined.getAt(1).getHub().size());
        assertEquals(hubApplication.size(), hubCombined.getAt(0).getHub().size());
        

        // Test: add 3 again
        appGroup.getApplications().add(hubApplication.getAt(0));
        appGroup.getApplications().add(hubApplication.getAt(1));
        appGroup.getApplications().add(hubApplication.getAt(2));
        
        assertEquals(hubCombined.getAt(0).getHub().size(), hubApplication.size()-3);
        assertEquals(3, hubCombined.getAt(1).getHub().size());
        
        // clear
        appGroup.getApplications().clear();

        assertEquals(0, hubApplication.getAt(0).getApplicationGroups().size()); 
        assertEquals(0, hubApplicationGroup.getAt(0).getApplications().size()); 
        assertEquals(0, hubCombined.getAt(1).getHub().size());
        assertEquals(hubApplication.size(), hubCombined.getAt(0).getHub().size());

        // Test: add 3 again, and use remove
        appGroup.getApplications().add(hubApplication.getAt(0));
        appGroup.getApplications().add(hubApplication.getAt(1));
        appGroup.getApplications().add(hubApplication.getAt(2));
        
        assertEquals(hubCombined.getAt(0).getHub().size(), hubApplication.size()-3);
        assertEquals(3, hubCombined.getAt(1).getHub().size());
        
        // clear
        appGroup.getApplications().remove(0);
        appGroup.getApplications().remove(0);
        appGroup.getApplications().remove(0);

        assertEquals(0, hubApplication.getAt(0).getApplicationGroups().size()); 
        assertEquals(0, hubApplicationGroup.getAt(0).getApplications().size()); 
        assertEquals(0, hubCombined.getAt(1).getHub().size());
        assertEquals(hubApplication.size(), hubCombined.getAt(0).getHub().size());
    }    

    @Test
    public void TestSplit() {
        init();
        Model model = new Model();
        DataGenerator data = new DataGenerator(model);
        data.createSampleData1();

        String pp = SitePP.environments().silos().pp;
        OAFinder<Site,Silo> f = new OAFinder<Site,Silo>(pp);
        Silo silo = f.findFirst(model.getSites());

        pp = SiloPP.servers().applications().pp;
        Hub<Application> hubApplication = new Hub<Application>();
        HubMerger<Silo,Application> hm  = new HubMerger<Silo, Application>(silo, hubApplication, pp);

        pp = ApplicationPP.applicationType().applicationGroups().pp;
        
        Hub<ApplicationGroup> hubApplicationGroup = silo.getApplicationGroups();
        
        HubGroupBy<Application, ApplicationGroup> hgb = new HubGroupBy<Application, ApplicationGroup>(hubApplication, hubApplicationGroup, pp);

        Hub<OAGroupBy<Application, ApplicationGroup>> hubCombined = (Hub<OAGroupBy<Application, ApplicationGroup>>) hgb.getCombinedHub();

        assertEquals(1, hubCombined.size());
        assertEquals(5, hubCombined.getAt(0).getHub().size());
        
        OAGroupBy gb = hubCombined.getAt(0);
        for (Application app : hubApplication) {
            app.setApplicationType(null);
        }
        assertEquals(1, hubCombined.size());
        assertEquals(5, hubCombined.getAt(0).getHub().size());
        
        ApplicationType appType = model.getApplicationTypes().getAt(0);
        hubApplication.getAt(0).setApplicationType(appType);
        assertEquals(1, hubCombined.size());
        assertEquals(5, hubCombined.getAt(0).getHub().size());
        
        
        // set up AppType + AppGroup
        ApplicationGroup appGroup = new ApplicationGroup();
        hubApplicationGroup.add(appGroup);
        appGroup.getApplicationTypes().add(appType);

        assertEquals(2, hubCombined.size());
        assertEquals(1, hubCombined.getAt(1).getHub().size());
        assertEquals(4, hubCombined.getAt(0).getHub().size());
        assertNull(hubCombined.getAt(0).getGroupBy());
        
        appGroup.getApplicationTypes().remove(appType);
        assertEquals(2, hubCombined.size());
        assertEquals(5, hubCombined.getAt(0).getHub().size());
        hubApplicationGroup.remove(appGroup);
        assertEquals(1, hubCombined.size());
    }    

    @Test
    public void TestSplit2() {
        init();
        Model model = new Model();
        DataGenerator data = new DataGenerator(model);
        data.createSampleData1();

        String pp = SitePP.environments().silos().pp;
        OAFinder<Site,Silo> f = new OAFinder<Site,Silo>(pp);
        Silo silo = f.findFirst(model.getSites());

        pp = SiloPP.servers().applications().pp;
        Hub<Application> hubApplication = new Hub<Application>();
        HubMerger<Silo,Application> hm  = new HubMerger<Silo, Application>(silo, hubApplication, pp);

        pp = ApplicationPP.applicationType().applicationGroups().pp;
        
        Hub<ApplicationGroup> hubApplicationGroup = silo.getApplicationGroups();
        
        HubGroupBy<Application, ApplicationGroup> hgb = new HubGroupBy<Application, ApplicationGroup>(hubApplication, hubApplicationGroup, pp);

        Hub<OAGroupBy<Application, ApplicationGroup>> hubCombined = (Hub<OAGroupBy<Application, ApplicationGroup>>) hgb.getCombinedHub();

        assertEquals(1, hubCombined.size());
        assertEquals(5, hubCombined.getAt(0).getHub().size());
        assertNull(hubCombined.getAt(0).getGroupBy());

        
        int max = 10;
        for (int i=0; i<max; i++) {
            ApplicationGroup appGroup = new ApplicationGroup();
            hubApplicationGroup.add(appGroup);
            assertEquals(2+i, hubCombined.size());
        }
        
        for (int i=max; i>0; i--) {
            hubApplicationGroup.removeAt(0);
            assertEquals(i, hubCombined.size());
        }
        assertEquals(1, hubCombined.size());
        assertEquals(5, hubCombined.getAt(0).getHub().size());
        
        for (int i=0; i<max; i++) {
            ApplicationGroup appGroup = new ApplicationGroup();
            hubApplicationGroup.add(appGroup);
            assertEquals(2+i, hubCombined.size());
        }
        assertEquals(5, hubCombined.getAt(0).getHub().size());
        hubApplicationGroup.clear();
        assertEquals(0, hubApplicationGroup.size());
        assertEquals(1, hubCombined.size());
        assertEquals(5, hubCombined.getAt(0).getHub().size());

        
        hubApplication.clear();
        for (OAGroupBy gb : hubCombined) {
            int x  = gb.getHub().size();
            assertEquals(0, x);
        }
        
        hubApplicationGroup.clear();
        assertEquals(1, hubCombined.size());
        assertEquals(0, hubCombined.getAt(0).getHub().size());
        
        ApplicationGroup appGroup = new ApplicationGroup();
        hubApplicationGroup.add(appGroup);
        assertEquals(2, hubCombined.size());
        assertEquals(0, hubCombined.getAt(0).getHub().size());
        assertEquals(0, hubCombined.getAt(1).getHub().size());
        
        Application app = new Application();
        hubApplication.add(app);
        assertEquals(2, hubCombined.size());
        assertEquals(1, hubCombined.getAt(0).getHub().size());
        assertEquals(0, hubCombined.getAt(1).getHub().size());

        ApplicationType appType = new ApplicationType();
        appGroup.getApplicationTypes().add(appType);
        assertEquals(2, hubCombined.size());
        assertEquals(1, hubCombined.getAt(0).getHub().size());
        assertEquals(0, hubCombined.getAt(1).getHub().size());

        appGroup.getApplicationTypes().add(appType);
        assertEquals(2, hubCombined.size());
        assertEquals(1, hubCombined.getAt(0).getHub().size());
        assertEquals(0, hubCombined.getAt(1).getHub().size());
        
        app.setApplicationType(appType);
        assertEquals(2, hubCombined.size());
        assertEquals(0, hubCombined.getAt(0).getHub().size());
        assertEquals(1, hubCombined.getAt(1).getHub().size());
        
        app.setApplicationType(null);
        assertEquals(2, hubCombined.size());
        assertEquals(1, hubCombined.getAt(0).getHub().size());
        assertEquals(0, hubCombined.getAt(1).getHub().size());
        
        hubApplication.removeAll();
        //hubApplication.remove(0);
        assertEquals(2, hubCombined.size());
        assertEquals(0, hubCombined.getAt(0).getHub().size());
        assertEquals(0, hubCombined.getAt(1).getHub().size());
        
        hubApplication.add(app);
        assertEquals(2, hubCombined.size());
        assertEquals(1, hubCombined.getAt(0).getHub().size());
        assertEquals(0, hubCombined.getAt(1).getHub().size());
        
        app.setApplicationType(appType);
        assertEquals(2, hubCombined.size());
        assertEquals(0, hubCombined.getAt(0).getHub().size());
        assertEquals(1, hubCombined.getAt(1).getHub().size());
        
        appGroup.getApplicationTypes().removeAt(0);
        // appGroup.getApplicationTypes().clear();
        assertEquals(1, hubApplicationGroup.size());
        assertEquals(2, hubCombined.size());
        assertEquals(1, hubCombined.getAt(0).getHub().size());
        assertEquals(0, hubCombined.getAt(1).getHub().size());

        hubApplicationGroup.clear();
        assertEquals(0, hubApplicationGroup.size());
        assertEquals(1, hubCombined.size());
        assertEquals(1, hubCombined.getAt(0).getHub().size());
        
        hubApplication.clear();
        assertEquals(1, hubCombined.size());
        assertEquals(0, hubCombined.getAt(0).getHub().size());
        
        
        for (int i=0; i<max; i++) {
            appGroup = new ApplicationGroup();
            hubApplicationGroup.add(appGroup);
            assertEquals(i+1, hubApplicationGroup.size());
            assertEquals(i+2, hubCombined.size());
        }        

        hubApplication.add(app);
        assertEquals(max+1, hubCombined.size());
        assertEquals(1, hubCombined.getAt(0).getHub().size());

        app.setApplicationType(appType);
        assertEquals(max+1, hubCombined.size());
        assertEquals(1, hubCombined.getAt(0).getHub().size());
        for (OAGroupBy gb : hubCombined) {
            if (gb.getGroupBy() == null) assertEquals(1, gb.getHub().size());
            else assertEquals(0, gb.getHub().size());
        }

        app.setApplicationType(null);
        assertEquals(max+1, hubCombined.size());
        assertEquals(1, hubCombined.getAt(0).getHub().size());
        for (OAGroupBy gb : hubCombined) {
            if (gb.getGroupBy() == null) assertEquals(1, gb.getHub().size());
            else assertEquals(0, gb.getHub().size());
        }
        
        // clear all
        hubApplication.clear();
        hubApplicationGroup.clear();
        assertEquals(1, hubCombined.size());
        assertEquals(0, hubCombined.getAt(0).getHub().size());

                
        for (int i=0; i<max; i++) {
            appGroup = new ApplicationGroup();
            hubApplicationGroup.add(appGroup);
            assertEquals(2+i, hubCombined.size());

            appType = new ApplicationType();
            appGroup.getApplicationTypes().add(appType);
            assertEquals(2+i, hubCombined.size());
            
            app = new Application();
            hubApplication.add(app);
            assertEquals(1, hubCombined.getAt(0).getHub().size());
            
            app.setApplicationType(appType);
            assertEquals(0, hubCombined.getAt(0).getHub().size());
            
            assertEquals(1, hubCombined.getAt(i+1).getHub().size());
        }

        assertEquals(max+1, hubCombined.size());
        for (OAGroupBy gb : hubCombined) {
            if (gb.getGroupBy() == null) assertEquals(0, gb.getHub().size());
            else assertEquals(1, gb.getHub().size());
        }
        
        hubApplication.clear();
        assertEquals(max+1, hubCombined.size());
        for (OAGroupBy gb : hubCombined) {
            if (gb.getGroupBy() == null) assertEquals(0, gb.getHub().size());
            else assertEquals(0, gb.getHub().size());
        }
        
        hubApplication.clear();
        assertEquals(max+1, hubCombined.size());

        for (OAGroupBy gb : hubCombined) {
            int x  = gb.getHub().size();
            assertEquals(0, x);
        }
        
        for (int i=hubApplicationGroup.size(); i>0; i--) {
            hubApplicationGroup.remove(0);
            assertEquals(i, hubCombined.size());
            int xx = 4;
            xx++;
        }
    }    

    @Test
    public void TestSplit3() {
        init();
        String pp;
        
        Hub<ApplicationType> hubApplicationType = new Hub<ApplicationType>(ApplicationType.class);
        Hub<Application> hubApplication = new Hub<Application>(Application.class);
        Hub<ApplicationGroup> hubApplicationGroup = new Hub<ApplicationGroup>(ApplicationGroup.class);
        
        pp = ApplicationPP.applicationType().applicationGroups().pp;
        HubGroupBy<Application, ApplicationGroup> hgb = new HubGroupBy<Application, ApplicationGroup>(hubApplication, hubApplicationGroup, pp);

        Hub<OAGroupBy<Application, ApplicationGroup>> hubCombined = (Hub<OAGroupBy<Application, ApplicationGroup>>) hgb.getCombinedHub();
        
        // 1: add GB objects should show in combined
        int max = 10;
        for (int i=0; i<max; i++) {
            ApplicationGroup appGroup = new ApplicationGroup();
            hubApplicationGroup.add(appGroup);
            assertEquals(1+i, hubCombined.size());
        }
        
        // 2: clear GB objects should clear combined
        hubApplicationGroup.clear();
        assertEquals(0, hubCombined.size());


        // 3: add source objects should add to combined.null
        for (int i=0; i<max; i++) {
            Application app = new Application();
            hubApplication.add(app);
            assertEquals(1, hubCombined.size());
            assertEquals(i+1, hubCombined.getAt(0).getHub().size());
        }
        
        // 4: remove source objects should remove GB combined.null
        hubApplication.clear();
        assertEquals(1, hubCombined.size());
        assertNull(hubCombined.getAt(0).getGroupBy());
        assertEquals(0, hubCombined.getAt(0).getHub().size());
        
        // 5: add appTypes should do nothing to GB
        for (int i=0; i<max; i++) {
            ApplicationType appType = new ApplicationType();
            hubApplicationType.add(appType);
            assertEquals(1, hubCombined.size());
        }
        
        // 6: add GB objects, add source objects should then be added to combined.null
        for (int i=0; i<max; i++) {
            ApplicationGroup appGroup = new ApplicationGroup();
            hubApplicationGroup.add(appGroup);
            assertEquals(i+2, hubCombined.size());
        }
        assertEquals(max+1, hubCombined.size());
        for (int i=0; i<max; i++) {
            Application app = new Application();
            hubApplication.add(app);
            assertEquals(max+1, hubCombined.size());
            assertEquals(i+1, hubCombined.getAt(0).getHub().size());
            assertNull(hubCombined.getAt(0).getGroupBy());
            for (int j=0; j<max; j++) {
                assertEquals(hubApplicationGroup.getAt(j), hubCombined.getAt(j+1).getGroupBy());
            }
        }
        
        
        ApplicationType appType = hubApplicationType.getAt(0);
        
        // 7: assign appType to all source, should have not affect on combined
        for (Application app : hubApplication) {
            app.setApplicationType(appType);
            for (int j=0; j<max; j++) {
                assertEquals(hubApplicationGroup.getAt(j), hubCombined.getAt(j+1).getGroupBy());
                assertEquals(max, hubCombined.getAt(0).getHub().size());  // null hub
            }
        }
        
        // 8: assign GB to appType, all Apps will go to that combined.hub
        hubApplicationGroup.getAt(0).getApplicationTypes().add(hubApplicationType.getAt(0));
        
        assertEquals(0, hubCombined.getAt(0).getHub().size()); // null hub
        assertEquals(max, hubCombined.getAt(1).getHub().size());  // GB that has all of apps w/ appType
        
        // 10: delete the appType, all apps should go to null hub
        assertEquals(0, hubCombined.getAt(0).getHub().size()); // null hub
        assertEquals(10, hubCombined.getAt(1).getHub().size());
        appType.delete();
        
        assertEquals(10, hubCombined.getAt(0).getHub().size()); // null hub
        assertEquals(0, hubCombined.getAt(1).getHub().size());
        for (int j=0; j<max; j++) {
            hubApplication.getAt(j).setApplicationType(null);
            assertEquals(hubApplicationGroup.getAt(j), hubCombined.getAt(j+1).getGroupBy());
            assertEquals(0, hubCombined.getAt(j+1).getHub().size());  // null hub
        }
        
        // 10: remove AppGroup should remove from combined
        hubApplicationGroup.removeAt(0);
        assertEquals(max, hubCombined.getSize()); 
        assertEquals(10, hubCombined.getAt(0).getHub().size()); // null hub
        
        // set back removed/deleted
        appType = new ApplicationType();
        hubApplicationType.add(appType);
        hubApplicationGroup.add(new ApplicationGroup());
        
        // 11: set all appGroup.appType to all appTypes
        for (int i=0; i<max; i++) {
            for (int j=0; j<max; j++) {
                hubApplicationGroup.getAt(i).getApplicationTypes().add(hubApplicationType.getAt(j));
            }
        }
        assertEquals(10, hubCombined.getAt(0).getHub().size());
        for (int i=0; i<max; i++) {
            assertEquals(0, hubCombined.getAt(i+1).getHub().size());
        }
        
        
        // 12: set all app.appType
        for (int i=0; i<max; i++) {
            hubApplication.getAt(i).setApplicationType(hubApplicationType.getAt(i));
        }
        assertEquals(0, hubCombined.getAt(0).getHub().size()); // null hub
        for (int i=0; i<max; i++) {
            assertEquals(10, hubCombined.getAt(i+1).getHub().size());
            for (int j=0; j<max; j++) {
                assertTrue(hubCombined.getAt(i+1).getHub().contains(hubApplication.getAt(i)));
            }
        }
        // ... each app has one of the appTypes

        // 13: set app.appType=null
        for (int i=0; i<max; i++) {
            hubApplication.getAt(i).setApplicationType(null);
            assertEquals(i+1, hubCombined.getAt(0).getHub().size()); // null hub
            for (int j=0; j<max; j++) {
                assertFalse(hubCombined.getAt(i+1).getHub().contains(hubApplication.getAt(i)));
            }
            for (int j=0; j<max; j++) {
                assertEquals(max-(i+1), hubCombined.getAt(j+1).getHub().size());
            }
        }
        // ... each app.appType=null, each appGroup has all appTypes
        
        // 14: assign all apps to same appType, should show for each appGroup
        appType = hubApplicationType.getAt(0);
        for (int i=0; i<max; i++) {
            hubApplication.getAt(i).setApplicationType(appType);
            assertEquals(max-(i+1), hubCombined.getAt(0).getHub().size()); // null hub
            for (int j=0; j<max; j++) {
                assertTrue(hubCombined.getAt(i+1).getHub().contains(hubApplication.getAt(i)));
                assertEquals(i+1, hubCombined.getAt(j+1).getHub().size());
            }
        }        
        // ... all apps=appType, all appGrps have all appTypes, combined gb=each appGrp, gb.hub each have all app
        
        // 15: remove appType from each appGroup, combined will have all apps in null hub
        for (int i=0; i<max; i++) {
            assertEquals(0, hubCombined.getAt(0).getHub().size()); // null hub wont be changed until done
            hubApplicationGroup.getAt(i).getApplicationTypes().remove(appType);
            assertEquals(0, hubCombined.getAt(i+1).getHub().size());
        }
        // all appGrp dont have appType, all apps are in null hub
        for (int i=0; i<max; i++) {
            assertEquals(0, hubCombined.getAt(i+1).getHub().size());
        }
        assertEquals(10, hubCombined.getAt(0).getHub().size()); // null hub should now have all objects
        
        // 16: add appType to one appGroup => null hub=0, hubCombined.appGrp.hub=10 
        hubApplicationGroup.getAt(0).getApplicationTypes().add(appType);
        assertEquals(0, hubCombined.getAt(0).getHub().size());  // null hub
        assertEquals(10, hubCombined.getAt(1).getHub().size());
        for (int i=1; i<max; i++) {
            assertEquals(0, hubCombined.getAt(i+1).getHub().size());
        }
        assertEquals(0, hubCombined.getAt(0).getHub().size()); // null hub should now have all objects
        // ... all apps are under one appGrp
        
        // remove other appGrps => should remove from combined
        for (int i=1; i<max; i++) {
            hubApplicationGroup.removeAt(1);
            assertEquals(max-(i-1), hubCombined.size());
        }
        assertEquals(2, hubCombined.size());
        assertEquals(0, hubCombined.getAt(0).getHub().size());
        assertEquals(10, hubCombined.getAt(1).getHub().size());
        
        // 17: remove appGrp.appType => combined null will have all apps
        hubApplicationGroup.getAt(0).getApplicationTypes().remove(appType);
        assertEquals(10, hubCombined.getAt(0).getHub().size());
        assertEquals(0, hubCombined.getAt(1).getHub().size());
        
        // 18: remove appGrp
        hubApplicationGroup.clear();
        assertEquals(1, hubCombined.size());
        assertEquals(10, hubCombined.getAt(0).getHub().size());
    }    
    
    @Test
    public void TestSplit4() {
        init();
        String pp;
        
        Hub<ApplicationGroup> hubApplicationGroup = new Hub<ApplicationGroup>(ApplicationGroup.class);
        Hub<Application> hubApplication = new Hub<Application>(Application.class);
        pp = ApplicationPP.applicationType().applicationGroups().pp;
        HubGroupBy<Application, ApplicationGroup> hgb = new HubGroupBy<Application, ApplicationGroup>(hubApplication, hubApplicationGroup, pp);

        int max = 10;
        for (int i=0; i<max; i++) {
            ApplicationGroup appGroup = new ApplicationGroup();
            hubApplicationGroup.add(appGroup);
        }
        for (int i=0; i<max; i++) {
            Application app = new Application();
            hubApplication.add(app);
        }
        for (int i=0; i<max; i++) {
            hubApplicationGroup.getAt(0).getApplications().add(hubApplication.getAt(i));
        }

        ApplicationType appType = new ApplicationType();
        for (int i=0; i<max; i++) {
            hubApplication.getAt(i).setApplicationType(appType);
        }
        hubApplicationGroup.getAt(0).getApplicationTypes().add(appType);
        
        Application application = hubApplication.getAt(0); 

        Hub<OAGroupBy<Application, ApplicationGroup>> hubCombined = (Hub<OAGroupBy<Application, ApplicationGroup>>) hgb.getCombinedHub();

        for (int i=0; i<max; i++) {
            hubApplication.removeAt(0);  // ==> should not be put in combined null
            assertEquals(11, hubCombined.getSize());
            assertEquals(max-(i+1), hubCombined.getAt(0).getHub().getSize());
            for (int j=1; j<11; j++) {
                assertEquals(0, hubCombined.getAt(j).getHub().getSize());
            }
        }
    
    }
    
    @Test
    public void TestMultiple() {
        init();
        String pp;
        
        Hub<Application> hubApplication = new Hub<Application>(Application.class);
        Hub<ApplicationGroup> hubApplicationGroup = new Hub<ApplicationGroup>(ApplicationGroup.class);
        
        pp = ApplicationPP.applicationType().applicationGroups().pp;
        HubGroupBy<Application, ApplicationGroup> hgb = new HubGroupBy<Application, ApplicationGroup>(hubApplication, hubApplicationGroup, pp);

        String pp2 = ApplicationPP.applicationGroups().pp;
        HubGroupBy<Application, ApplicationGroup> hgb2 = new HubGroupBy<Application, ApplicationGroup>(hgb, pp2);
        
        Hub<OAGroupBy<Application, ApplicationGroup>> hubCombined = (Hub<OAGroupBy<Application, ApplicationGroup>>) hgb2.getCombinedHub();
        
        // 1: add GB objects should show in combined
        int max = 10;
        for (int i=0; i<max; i++) {
            ApplicationGroup appGroup = new ApplicationGroup();
            hubApplicationGroup.add(appGroup);
            assertEquals(i+1, hubCombined.size());
        }
        
        // 2: create apps => all in combined null
        for (int i=0; i<max; i++) {
            Application app = new Application();
            hubApplication.add(app);
            assertEquals(i+1, hubCombined.getAt(max).getHub().size());
        }
        assertEquals(max, hubCombined.getAt(max).getHub().size());
        
        // 3: assign apps to one appGrp => combined null=0, appGrp.hub=10
        for (int i=0; i<max; i++) {
            hubApplicationGroup.getAt(0).getApplications().add(hubApplication.getAt(i));
            assertEquals(max-(i+1), hubCombined.getAt(max).getHub().size());
            assertEquals(i+1, hubCombined.getAt(0).getHub().size());
        }
        
        // 4: assign all app.appTpe => no changes to combined 
        ApplicationType appType = new ApplicationType();
        for (int i=0; i<max; i++) {
            hubApplication.getAt(i).setApplicationType(appType);
            assertEquals(max, hubCombined.getAt(0).getHub().size());
            assertEquals(0, hubCombined.getAt(max-1).getHub().size());
        }

        // 5: add appType to appGrp => no change to combined, apps were already added before
        hubApplicationGroup.getAt(0).getApplicationTypes().add(appType);
        assertEquals(max, hubCombined.getAt(0).getHub().size());
        assertEquals(0, hubCombined.getAt(max-1).getHub().size());
        
        // 6: add appType to appGrp[1] => combined[1] will have all apps
        hubApplicationGroup.getAt(1).getApplicationTypes().add(appType);
        assertEquals(0, hubCombined.getAt(max-1).getHub().size());
        assertEquals(max, hubCombined.getAt(0).getHub().size());
        assertEquals(max, hubCombined.getAt(1).getHub().size());
        for (int i=2; i<max; i++) {
            assertEquals(0, hubCombined.getAt(i).getHub().size());
        }
        
        // 7: remove app => remove from combined[0,1]
        Application application = hubApplication.getAt(0); 
        hubApplication.removeAt(0);
        assertEquals(max-1, hubCombined.getAt(0).getHub().size());
        assertEquals(max-1, hubCombined.getAt(1).getHub().size());
        for (int i=2; i<max; i++) {
            assertEquals(0, hubCombined.getAt(i).getHub().size());
        }
     
        // 8: remove app from appGrp[0] => combined no change
        hubApplicationGroup.getAt(0).getApplications().remove(0);
        assertEquals(max-1, hubCombined.getAt(0).getHub().size());

        // 9: remove appType from appGrp[0] => will keep apps 
        hubApplicationGroup.getAt(0).getApplicationTypes().removeAll();
        assertEquals(max-1, hubCombined.getAt(0).getHub().size());
        
        // 10: remove all apps 
        hubApplication.clear();
        assertEquals(11, hubCombined.size());
        for (OAGroupBy gb : hubCombined) {
            assertEquals(0, gb.getHub().size());
        }
    }
    
    @Test
    public void TestMultiple2() {
        reset();
        
        Silo silo = new Silo();
        Hub<Silo> hubSilo = new Hub<Silo>(Silo.class);
        hubSilo.add(silo);
        hubSilo.setAO(silo);

        ApplicationType appType = new ApplicationType();
        ApplicationGroup appGroup = new ApplicationGroup();
        appGroup.getApplicationTypes().add(appType);
        silo.getApplicationGroups().add(appGroup);

        
        Hub<MRADClient> hubMRADClient = new Hub<MRADClient>(MRADClient.class);
        
        MRADClient mc = new MRADClient();
        hubMRADClient.add(mc);
        Application app = new Application();
        mc.setApplication(app);
        app.setApplicationType(appType);
        app.getApplicationGroups().add(appGroup);

        MRADClient mc2 = new MRADClient();
        hubMRADClient.add(mc2);
        Application app2 = new Application();
        mc2.setApplication(app2);
        app2.setApplicationType(appType);
        app2.getApplicationGroups().add(appGroup);
        
        
        Hub<ApplicationGroup> hubApplicationGroup = new Hub<ApplicationGroup>(ApplicationGroup.class);
        
        Hub h = hubSilo.getDetailHub(SiloPP.applicationGroups().pp);
        new HubCopy(h, hubApplicationGroup, false);
        
        String pp1 = MRADClientPP.application().applicationType().applicationGroups().pp;
        HubGroupBy<MRADClient, ApplicationGroup> hgb = new HubGroupBy<MRADClient, ApplicationGroup>(hubMRADClient, hubApplicationGroup, pp1);

        Hub<OAGroupBy<MRADClient, ApplicationGroup>> hubCombined = (Hub<OAGroupBy<MRADClient, ApplicationGroup>>) hgb.getCombinedHub();

        assertEquals(1, hubCombined.size());
        assertEquals(2, hubCombined.getAt(0).getHub().size());
        
        String pp2 = MRADClientPP.application().applicationGroups().pp;
        HubGroupBy<MRADClient, ApplicationGroup> hgb2 = new HubGroupBy<MRADClient, ApplicationGroup>(hgb, pp2);
        
        hubCombined = (Hub<OAGroupBy<MRADClient, ApplicationGroup>>) hgb2.getCombinedHub();
        
        assertEquals(1, hubCombined.size());
        assertEquals(2, hubCombined.getAt(0).getHub().size());
    }
    
    @Test
    public void TestMultiple3() {
        reset();
        
        Silo silo = new Silo();
        Hub<Silo> hubSilo = new Hub<Silo>(Silo.class);
        hubSilo.add(silo);
        hubSilo.setAO(silo);

        ApplicationType appType = new ApplicationType();
        ApplicationGroup appGroup = new ApplicationGroup();
        appGroup.getApplicationTypes().add(appType);
        silo.getApplicationGroups().add(appGroup);

        
        Hub<MRADClient> hubMRADClient = new Hub<MRADClient>(MRADClient.class);
        
        MRADClient mc = new MRADClient();
        hubMRADClient.add(mc);
        Application app = new Application();
        mc.setApplication(app);
        app.setApplicationType(appType);
        app.getApplicationGroups().add(appGroup);

        MRADClient mc2 = new MRADClient();
        hubMRADClient.add(mc2);
        Application app2 = new Application();
        mc2.setApplication(app2);
        app2.setApplicationType(appType);
        app2.getApplicationGroups().add(appGroup);
        
        
        Hub<ApplicationGroup> hubApplicationGroup = new Hub<ApplicationGroup>(ApplicationGroup.class);
        
        Hub h = hubSilo.getDetailHub(SiloPP.applicationGroups().pp);
        new HubCopy(h, hubApplicationGroup, false);
        
        String pp1 = MRADClientPP.application().applicationType().applicationGroups().pp;
        HubGroupBy<MRADClient, ApplicationGroup> hgb = new HubGroupBy<MRADClient, ApplicationGroup>(hubMRADClient, hubApplicationGroup, pp1, false);

        Hub<OAGroupBy<MRADClient, ApplicationGroup>> hubCombined = (Hub<OAGroupBy<MRADClient, ApplicationGroup>>) hgb.getCombinedHub();

        assertEquals(1, hubCombined.size());
        assertEquals(2, hubCombined.getAt(0).getHub().size());
        
        String pp2 = MRADClientPP.application().applicationGroups().pp;
        HubGroupBy<MRADClient, ApplicationGroup> hgb2 = new HubGroupBy<MRADClient, ApplicationGroup>(hgb, pp2, false);
        
        hubCombined = (Hub<OAGroupBy<MRADClient, ApplicationGroup>>) hgb2.getCombinedHub();
        
        assertEquals(1, hubCombined.size());
        assertEquals(2, hubCombined.getAt(0).getHub().size());
        
        // should not have a null gb
        
        for (int i=0; i<5; i++) {
            appGroup = new ApplicationGroup();
            appType = new ApplicationType();
            appGroup.getApplicationTypes().add(appType);
            silo.getApplicationGroups().add(appGroup);
            
            assertEquals(i+2, hubCombined.size());
            for (int i2=0; i2<3; i2++) {
                mc = new MRADClient();
                hubMRADClient.add(mc);
                app.setApplicationType(appType);
                app.getApplicationGroups().add(appGroup);
                app = new Application();
                mc.setApplication(app);
            }
            assertEquals(i+2, hubCombined.size());
            for (int i2=0; i2<3; i2++) {
                mc = new MRADClient();
                app.setApplicationType(appType);
                app.getApplicationGroups().add(appGroup);
                app = new Application();
                mc.setApplication(app);
                hubMRADClient.add(mc);
            }
            assertEquals(i+2, hubCombined.size());
            for (int i2=0; i2<3; i2++) {
                mc = new MRADClient();
                app = new Application();
                mc.setApplication(app);
                app.setApplicationType(appType);
                app.getApplicationGroups().add(appGroup);
                hubMRADClient.add(mc);
            }
            assertEquals(i+2, hubCombined.size());
            for (int i2=0; i2<3; i2++) {
                mc = new MRADClient();
                app = new Application();
                mc.setApplication(app);
                hubMRADClient.add(mc);
            }
            assertEquals(i+2, hubCombined.size());
        }
    }

    @Test
    public void TestSplit5() {
        reset();
        
        Hub<ApplicationType> hubApplicationType = new Hub<ApplicationType>(ApplicationType.class);
        Hub<ApplicationGroup> hubApplicationGroup = new Hub<ApplicationGroup>(ApplicationGroup.class);
        Hub<MRADClient> hubMRADClient = new Hub<MRADClient>(MRADClient.class);
        
        String pp1 = MRADClientPP.application().applicationType().applicationGroups().pp;

        HubGroupBy<MRADClient, ApplicationGroup> hgb = new HubGroupBy<MRADClient, ApplicationGroup>(hubMRADClient, hubApplicationGroup, pp1, false);
        
        Hub<OAGroupBy<MRADClient, ApplicationGroup>> hubGroupByApplicationGroup;    
        hubGroupByApplicationGroup = hgb.getCombinedHub();

        Hub<ApplicationGroup> hubApplicationGroup2 = new Hub<ApplicationGroup>(ApplicationGroup.class);
        Hub<MRADClient> hubMRADClient2 = new Hub<MRADClient>(MRADClient.class);
        
        
        for (int i=0; i<10; i++) {
            ApplicationType at = new ApplicationType();
            hubApplicationType.add(at);
        }
        for (int i=0; i<2; i++) {
            ApplicationGroup ag = new ApplicationGroup();
            hubApplicationGroup2.add(ag);
            ApplicationType at = hubApplicationType.getAt(i);
            ag.getApplicationTypes().add(at);
        }
        assertEquals(hubApplicationGroup.getSize(), hubGroupByApplicationGroup.getSize());

        for (int i=0; i<20; i++) {
            MRADClient mc = new MRADClient();
            hubMRADClient2.add(mc);
            Application app = new Application();
            mc.setApplication(app);
            ApplicationType at;
            if (i%2==0) at = hubApplicationType.getAt(0);
            else at = hubApplicationType.getAt(1);
            app.setApplicationType(at);
        }
        
        hubMRADClient.setSharedHub(hubMRADClient2);
        hubApplicationGroup.setSharedHub(hubApplicationGroup2);
        
        assertEquals(hubApplicationGroup.getSize(), hubGroupByApplicationGroup.getSize());
        assertEquals(10, hubGroupByApplicationGroup.getAt(0).getHub().getSize());
        assertEquals(10, hubGroupByApplicationGroup.getAt(1).getHub().getSize());
    }
}


