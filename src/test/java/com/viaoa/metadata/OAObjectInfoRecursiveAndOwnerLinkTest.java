package com.viaoa.metadata;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAObjectInfoRecursiveAndOwnerLinkTest {

    public static class Node extends OAObject {
    }

    public static class Owner extends OAObject {
    }

    @Test
    void recursiveLinkMetadataCanBeStoredAndRetrieved() {
        OAObjectInfo oi = new OAObjectInfo();
        OALinkInfo one = new OALinkInfo("parent", Node.class, OALinkInfo.TYPE_ONE);
        OALinkInfo many = new OALinkInfo("children", Node.class, OALinkInfo.TYPE_MANY);

        oi.setRecursiveOne(one);
        oi.setRecursiveMany(many);

        assertSame(one, oi.getRecursiveOne());
        assertSame(many, oi.getRecursiveMany());
    }

    @Test
    void recursiveSetupFlagRoundTrips() {
        OAObjectInfo oi = new OAObjectInfo();

        assertFalse(oi.getSetRecursive());

        oi.setSetRecursive(true);
        assertTrue(oi.getSetRecursive());

        oi.setSetRecursive(false);
        assertFalse(oi.getSetRecursive());
    }

    @Test
    void linkToOwnerMetadataCanBeStoredAndRetrieved() {
        OAObjectInfo oi = new OAObjectInfo();
        OALinkInfo li = new OALinkInfo("owner", Owner.class, OALinkInfo.TYPE_ONE);

        oi.setLinkToOwner(li);

        assertSame(li, oi.getLinkToOwner());
    }

    @Test
    void linkToOwnerSetupFlagRoundTrips() {
        OAObjectInfo oi = new OAObjectInfo();

        assertFalse(oi.getSetLinkToOwner());

        oi.setSetLinkToOwner(true);
        assertTrue(oi.getSetLinkToOwner());

        oi.setSetLinkToOwner(false);
        assertFalse(oi.getSetLinkToOwner());
    }

    @Test
    void recursiveLinkFlagsOnLinkInfoAreIndependentOfObjectInfoSlots() {
        OAObjectInfo oi = new OAObjectInfo();

        OALinkInfo parent = new OALinkInfo("parent", Node.class, OALinkInfo.TYPE_ONE);
        OALinkInfo children = new OALinkInfo("children", Node.class, OALinkInfo.TYPE_MANY);
        parent.setRecursive(true);

        oi.addLinkInfo(parent);
        oi.addLinkInfo(children);
        oi.setRecursiveOne(parent);

        assertTrue(oi.getLinkInfo("parent").getRecursive());
        assertFalse(oi.getLinkInfo("children").getRecursive());
        assertSame(parent, oi.getRecursiveOne());
        assertNull(oi.getRecursiveMany());
    }
}
