package com.viaoa.select;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OASelectManagerBasicTest {

    public static class Item extends OAObject {
    }

    @Test
    void setTimeLimitRejectsZeroOrNegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> OASelectManager.setTimeLimit(0));
        assertThrows(IllegalArgumentException.class, () -> OASelectManager.setTimeLimit(-1));
    }

    @Test
    void addNullIsSafeNoop() {
        assertDoesNotThrow(() -> OASelectManager.add(null));
    }

    @Test
    void removeUntrackedSelectIsSafeNoop() {
        OASelect<Item> sel = new OASelect<>(Item.class);

        assertDoesNotThrow(() -> OASelectManager.remove(sel));
    }

    @Test
    void performCleanupSkipsNeverStartedSelect() {
        OASelect<Item> sel = new OASelect<>(Item.class);
        OASelectManager.add(sel);

        assertDoesNotThrow(OASelectManager::performCleanup);
        assertFalse(sel.isCancelled());

        OASelectManager.remove(sel);
    }

    @Test
    void performCleanupRemovesCancelledSelectWithoutThrowing() {
        OASelect<Item> sel = new OASelect<>(Item.class);
        OASelectManager.add(sel);

        sel.cancel();

        assertDoesNotThrow(OASelectManager::performCleanup);
        assertTrue(sel.isCancelled());
    }

    @Test
    void managerCanTrackManySelectsWithoutIdCollision() {
        OASelect<Item> a = new OASelect<>(Item.class);
        OASelect<Item> b = new OASelect<>(Item.class);

        assertNotEquals(a.getId(), b.getId());

        assertDoesNotThrow(() -> {
            OASelectManager.add(a);
            OASelectManager.add(b);
            OASelectManager.performCleanup();
            OASelectManager.remove(a);
            OASelectManager.remove(b);
        });
    }
}
