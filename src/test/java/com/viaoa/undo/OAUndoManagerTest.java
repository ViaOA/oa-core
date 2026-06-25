package com.viaoa.undo;

import static org.junit.jupiter.api.Assertions.*;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Product;
import com.test.pos.model.oa.Register;
import com.viaoa.graph.OAGraph;
import com.viaoa.runtime.OARuntime;

class OAUndoManagerTest {

    @BeforeEach
    void beforeEach() {
        resetUndoManager();
        OAGraph og = OARuntime.graph(Register.class);
    }

    @AfterEach
    void afterEach() {
        resetUndoManager();
        OARuntime.graph(Register.class).close();
    }
    
    @Test
    void createUndoManagerCreatesAndReturnsSingleton() {
        OAUndoManager manager = OAUndoManager.createUndoManager();

        assertNotNull(manager);
        assertSame(manager, OAUndoManager.createUndoManager());
        assertSame(manager, OAUndoManager.getUndoManager());
    }

    @Test
    void getUndoManagerReturnsNullBeforeCreationAndManagerAfterCreation() {
        assertNull(OAUndoManager.getUndoManager());

        OAUndoManager manager = OAUndoManager.createUndoManager();

        assertSame(manager, OAUndoManager.getUndoManager());
    }

    @Test
    void setVerboseAndGetVerboseRoundTrip() {
        OAUndoManager.createUndoManager();

        OAUndoManager.setVerbose(true);
        assertTrue(OAUndoManager.getVerbose());

        OAUndoManager.setVerbose(false);
        assertFalse(OAUndoManager.getVerbose());
    }

    @Test
    void startCompoundEditCreatesDefaultCompoundWhenManagerExists() {
        OAUndoManager.createUndoManager();

        OAUndoManager.startCompoundEdit();

        assertTrue(OAUndoManager.isInCompoundEdit());
    }

    @Test
    void startCompoundEditWithNameGroupsSubsequentStaticAdds() {
        OAUndoManager manager = OAUndoManager.createUndoManager();
        Product product = new Product(1);
        product.setSku("new");

        OAUndoManager.startCompoundEdit("product update");
        OAUndoManager.add(OAUndoableEdit.createUndoablePropertyChange("sku", product, Product.P_Sku, "old", "new"));
        OAUndoManager.endCompoundEdit();

        assertTrue(manager.canUndo());
        assertEquals("Undo product update", manager.getUndoPresentationName());

        manager.undo();
        assertEquals("old", product.getSku());
    }

    @Test
    void startCompoundEditForPropertyChangesCapturesObjectPropertyChanges() {
        OAUndoManager manager = OAUndoManager.createUndoManager();
        Product product = new Product(1);

        OAUndoManager.startCompoundEditForPropertyChanges("captured sku");
        try {
            product.setSku("A");
            product.setSku("B");
        }
        finally {
            OAUndoManager.endCompoundEditForPropertyChanges();
        }

        assertTrue(manager.canUndo());
        assertEquals("Undo captured sku", manager.getUndoPresentationName());

        manager.undo();
        assertNull(product.getSku());
    }

    @Test
    void endCompoundEditWithoutActiveCompoundIsSafe() {
        OAUndoManager.createUndoManager();

        assertDoesNotThrow(OAUndoManager::endCompoundEdit);
    }

    @Test
    void isInCompoundEditReflectsActiveCompoundAndIgnoreState() {
        OAUndoManager.createUndoManager();
        OAUndoManager.startCompoundEdit("group");
        assertTrue(OAUndoManager.isInCompoundEdit());

        OAUndoManager.setIgnore(true);
        assertFalse(OAUndoManager.isInCompoundEdit());

        OAUndoManager.setIgnore(false);
        assertTrue(OAUndoManager.isInCompoundEdit());
    }

    @Test
    void cancelCompoundEditDiscardsGroupedEdits() {
        OAUndoManager manager = OAUndoManager.createUndoManager();
        Product product = new Product(1);
        product.setSku("new");

        OAUndoManager.startCompoundEdit("discard");
        OAUndoManager.add(OAUndoableEdit.createUndoablePropertyChange("sku", product, Product.P_Sku, "old", "new"));
        OAUndoManager.cancelCompoundEdit();

        assertFalse(OAUndoManager.isInCompoundEdit());
        assertFalse(manager.canUndo());
    }

    @Test
    void addSingleEditIgnoresNullAndAddsWhenManagerExists() {
        OAUndoManager manager = OAUndoManager.createUndoManager();
        Product product = new Product(1);
        product.setSku("new");

        OAUndoManager.add((UndoableEdit) null);
        assertFalse(manager.canUndo());

        OAUndoManager.add(OAUndoableEdit.createUndoablePropertyChange("sku", product, Product.P_Sku, "old", "new"));

        assertTrue(manager.canUndo());
        manager.undo();
        assertEquals("old", product.getSku());
    }

    @Test
    void addEditArrayCreatesOneCompoundUndoableAction() {
        OAUndoManager manager = OAUndoManager.createUndoManager();
        Product product = new Product(1);
        product.setSku("new");
        product.setWeight("heavy");

        OAUndoManager.add(new UndoableEdit[] {
                OAUndoableEdit.createUndoablePropertyChange("sku", product, Product.P_Sku, "old", "new"),
                OAUndoableEdit.createUndoablePropertyChange("weight", product, Product.P_Weight, "light", "heavy") });

        assertTrue(manager.canUndo());
        manager.undo();
        assertEquals("old", product.getSku());
        assertEquals("light", product.getWeight());
        assertFalse(manager.canUndo());
    }

    @Test
    void addEditReturnsFalseWhenIgnoredAndTrueWhenAccepted() {
        OAUndoManager manager = OAUndoManager.createUndoManager();
        OAUndoableEdit edit = OAUndoableEdit.createUndoable("marker");

        OAUndoManager.setIgnore(true);
        assertFalse(manager.addEdit(edit));

        OAUndoManager.setIgnore(false);
        assertTrue(manager.addEdit(edit));
    }

    @Test
    void setIgnoreBalancesCurrentThreadCounterAndResetClearsIt() {
        OAUndoManager.createUndoManager();

        assertFalse(OAUndoManager.getIgnore());
        OAUndoManager.setIgnore(true);
        assertTrue(OAUndoManager.getIgnore());

        OAUndoManager.setIgnore(false);
        assertFalse(OAUndoManager.getIgnore());

        OAUndoManager.setIgnore(true);
        OAUndoManager.setIgnore(true);
        assertTrue(OAUndoManager.getIgnore());
        OAUndoManager.setIgnore(false, true);
        assertFalse(OAUndoManager.getIgnore());
    }

    @Test
    void ignoreConvenienceEnablesCurrentThreadIgnore() {
        OAUndoManager.createUndoManager();

        OAUndoManager.ignore();

        assertTrue(OAUndoManager.getIgnore());
    }

    @Test
    void ignoreBooleanDelegatesToSetIgnore() {
        OAUndoManager.createUndoManager();

        OAUndoManager.ignore(true);
        assertTrue(OAUndoManager.getIgnore());

        OAUndoManager.ignore(false);
        assertFalse(OAUndoManager.getIgnore());
    }

    @Test
    void getIgnoreIsTrueBeforeManagerCreationAndWhenIgnoreAllIsSet() {
        assertTrue(OAUndoManager.getIgnore());

        OAUndoManager.createUndoManager();
        assertFalse(OAUndoManager.getIgnore());

        OAUndoManager.setIgnoreAll(true);
        assertTrue(OAUndoManager.getIgnore());
    }

    @Test
    void setIgnoreAllSuppressesStaticAdd() {
        OAUndoManager manager = OAUndoManager.createUndoManager();

        OAUndoManager.setIgnoreAll(true);
        OAUndoManager.add(OAUndoableEdit.createUndoable("ignored"));

        assertFalse(manager.canUndo());
    }

    @Test
    void undoSuppressesCaptureDuringUndoAndRestoresNormalIgnoreState() {
        OAUndoManager manager = OAUndoManager.createUndoManager();
        RecordingEdit edit = new RecordingEdit();
        manager.addEdit(edit);

        manager.undo();

        assertTrue(edit.ignoreDuringUndo);
        assertFalse(OAUndoManager.getIgnore());
    }

    @Test
    void undoThrowsWhenNoEditIsAvailable() {
        OAUndoManager manager = OAUndoManager.createUndoManager();

        assertThrows(CannotUndoException.class, manager::undo);
    }

    private void resetUndoManager() {
        OAUndoManager.setVerbose(false);
        OAUndoManager.setIgnoreAll(false);
        OAUndoManager.setIgnore(false, true);
        OAUndoManager.cancelCompoundEdit();
        OAUndoManager manager = OAUndoManager.getUndoManager();
        if (manager != null) {
            manager.discardAllEdits();
        }
        OAUndoManager.hmThreadCounter.clear();
        OAUndoManager.bVerbose = false;
        OAUndoManager.bIgnoreAll = false;
        OAUndoManager.undoManager = null;
    }

    private static class RecordingEdit extends AbstractUndoableEdit {
        boolean ignoreDuringUndo;

        @Override
        public void undo() {
            ignoreDuringUndo = OAUndoManager.getIgnore();
            super.undo();
        }
    }
}
