package com.viaoa.undo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Item;
import com.test.pos.model.oa.Product;
import com.test.pos.model.oa.Register;
import com.viaoa.graph.api.internal.OAGraphInternal;
import com.viaoa.graph.service.OAObjectInternalService;
import com.viaoa.hub.Hub;
import com.viaoa.runtime.OARuntime;

class OAUndoableEditTest {

    @BeforeEach
    void beforeEach() {
        clearCache();
    }

    @Test
    void createUndoableAddUsesDefaultNameAndUndoRedoHubMembership() {
        Hub<Product> hub = new Hub<>(Product.class);
        Product product = new Product(1);
        hub.add(product);

        OAUndoableEdit edit = OAUndoableEdit.createUndoableAdd(null, hub, product);

        assertEquals("Add Product", edit.getPresentationName());
        assertTrue(edit.canUndo());
        assertFalse(edit.canRedo());

        edit.undo();
        assertFalse(hub.contains(product));
        assertFalse(edit.canUndo());
        assertTrue(edit.canRedo());

        edit.redo();
        assertTrue(hub.contains(product));
        assertTrue(edit.canUndo());
        assertFalse(edit.canRedo());
    }

    @Test
    void createUndoableChangeAOUsesDefaultNameAndUndoRedoActiveObject() {
        Hub<Product> hub = new Hub<>(Product.class);
        Product first = new Product(1);
        Product second = new Product(2);
        hub.add(first);
        hub.add(second);
        hub.setAO(second);

        OAUndoableEdit edit = OAUndoableEdit.createUndoableChangeAO(null, hub, first, second);

        assertEquals("change selected Product", edit.getPresentationName());
        edit.undo();
        assertSame(first, hub.getAO());

        edit.redo();
        assertSame(second, hub.getAO());
    }

    @Test
    void createUndoableInsertUsesPositionForUndoRedo() {
        Hub<Product> hub = new Hub<>(Product.class);
        Product first = new Product(1);
        Product inserted = new Product(2);
        hub.add(first);
        hub.insert(inserted, 0);

        OAUndoableEdit edit = OAUndoableEdit.createUndoableInsert(null, hub, inserted, 0);

        assertEquals("Insert Product", edit.getPresentationName());
        edit.undo();
        assertEquals(1, hub.getSize());
        assertSame(first, hub.getAt(0));

        edit.redo();
        assertEquals(2, hub.getSize());
        assertSame(inserted, hub.getAt(0));
    }

    @Test
    void createUndoableRemoveUsesOriginalPositionForUndoRedo() {
        Hub<Product> hub = new Hub<>(Product.class);
        Product first = new Product(1);
        Product removed = new Product(2);
        Product third = new Product(3);
        hub.add(first);
        hub.add(removed);
        hub.add(third);
        hub.remove(removed);

        OAUndoableEdit edit = OAUndoableEdit.createUndoableRemove(null, hub, removed, 1);

        assertEquals("Remove Product", edit.getPresentationName());
        edit.undo();
        assertEquals(3, hub.getSize());
        assertSame(removed, hub.getAt(1));

        edit.redo();
        assertEquals(2, hub.getSize());
        assertFalse(hub.contains(removed));
    }

    @Test
    void createUndoableMoveUsesRecordedPositionsForUndoRedo() {
        Hub<Product> hub = new Hub<>(Product.class);
        Product first = new Product(1);
        Product second = new Product(2);
        Product third = new Product(3);
        hub.add(first);
        hub.add(second);
        hub.add(third);
        hub.move(0, 2);

        OAUndoableEdit edit = OAUndoableEdit.createUndoableMove(null, hub, 0, 2);

        assertEquals("Move Product", edit.getPresentationName());
        edit.undo();
        assertSame(first, hub.getAt(0));
        assertSame(second, hub.getAt(1));
        assertSame(third, hub.getAt(2));

        edit.redo();
        assertSame(second, hub.getAt(0));
        assertSame(third, hub.getAt(1));
        assertSame(first, hub.getAt(2));
    }

    @Test
    void createUndoablePropertyChangeConvenienceRestoresValues() {
        Product product = new Product(1);
        product.setSku("new");

        OAUndoableEdit edit = OAUndoableEdit.createUndoablePropertyChange(null, product, Product.P_Sku, "old", "new");

        assertEquals("Change to Product sku", edit.getPresentationName());
        edit.undo();
        assertEquals("old", product.getSku());

        edit.redo();
        assertEquals("new", product.getSku());
    }

    @Test
    void createUndoablePropertyChangeExplicitWasChangedRestoresValue() {
        Product product = new Product(1);
        product.setChanged(false);
        product.setSku("new");
        assertTrue(product.getChanged());

        OAUndoableEdit edit = OAUndoableEdit.createUndoablePropertyChange("sku", product, Product.P_Sku, "old", "new",
                false);

        edit.undo();

        assertEquals("old", product.getSku());
    }

    @Test
    void createUndoableCreatesSignificantHolderWithNoOpUndoRedo() {
        OAUndoableEdit edit = OAUndoableEdit.createUndoable("marker");

        assertEquals("marker", edit.getPresentationName());
        assertTrue(edit.isSignificant());
        assertTrue(edit.canUndo());

        edit.undo();
        assertFalse(edit.canUndo());
        assertTrue(edit.canRedo());

        edit.redo();
        assertTrue(edit.canUndo());
    }

    @Test
    void setNameAndGetNameUsePresentationName() {
        OAUndoableEdit edit = OAUndoableEdit.createUndoable("old");

        edit.setName("new");

        assertEquals("new", edit.getName());
        assertEquals("new", edit.getPresentationName());
    }

    @Test
    void setPresentationNameAndGetPresentationNameUseSameField() {
        OAUndoableEdit edit = OAUndoableEdit.createUndoable("old");

        edit.setPresentationName("new");

        assertEquals("new", edit.getPresentationName());
        assertEquals("new", edit.getName());
    }

    @Test
    void canUndoAndUndoToggleRedoState() {
        OAUndoableEdit edit = OAUndoableEdit.createUndoable("marker");

        assertTrue(edit.canUndo());
        edit.undo();

        assertFalse(edit.canUndo());
        assertTrue(edit.canRedo());
    }

    @Test
    void redoTogglesCanUndoAndCanRedo() {
        OAUndoableEdit edit = OAUndoableEdit.createUndoable("marker");
        edit.undo();

        edit.redo();

        assertTrue(edit.canUndo());
        assertFalse(edit.canRedo());
    }

    @Test
    void canRedoHonorsAllowRedoFlag() {
        OAUndoableEdit edit = OAUndoableEdit.createUndoable("marker");
        edit.undo();
        assertTrue(edit.canRedo());

        edit.setAllowRedo(false);

        assertFalse(edit.canRedo());
    }

    @Test
    void undoAndRedoPresentationNamesPrefixPresentationName() {
        OAUndoableEdit edit = OAUndoableEdit.createUndoable("Change Product");

        assertEquals("Undo Change Product", edit.getUndoPresentationName());
        assertEquals("Redo Change Product", edit.getRedoPresentationName());
    }

    @Test
    void isSignificantReturnsTrueForAllEditTypes() {
        assertTrue(OAUndoableEdit.createUndoable("holder").isSignificant());
        assertTrue(OAUndoableEdit.createUndoableAdd("add", new Hub<>(Product.class), new Product(1)).isSignificant());
    }

    @Test
    void addEditAlwaysReturnsFalse() {
        OAUndoableEdit edit = OAUndoableEdit.createUndoable("one");
        OAUndoableEdit other = OAUndoableEdit.createUndoable("two");

        assertFalse(edit.addEdit(other));
    }

    @Test
    void dieIsNoOpByCurrentContract() {
        OAUndoableEdit edit = OAUndoableEdit.createUndoable("marker");

        edit.die();

        assertEquals("marker", edit.getPresentationName());
        assertTrue(edit.canUndo());
    }

    @Test
    void replaceEditUsesExistingEditAllowReplaceFlagByCurrentContract() {
        Product product = new Product(1);
        OAUndoableEdit previous = OAUndoableEdit.createUndoablePropertyChange("old", product, Product.P_Sku, "a", "b");
        OAUndoableEdit current = OAUndoableEdit.createUndoablePropertyChange("new", product, Product.P_Sku, "b", "c");

        assertFalse(current.replaceEdit(previous));

        previous.setAllowReplace(true);

        assertTrue(current.replaceEdit(previous));
    }

    @Test
    void setAllowReplaceAndGetAllowReplaceRoundTrip() {
        OAUndoableEdit edit = OAUndoableEdit.createUndoable("marker");

        assertFalse(edit.getAllowReplace());
        edit.setAllowReplace(true);
        assertTrue(edit.getAllowReplace());
    }

    @Test
    void setAllowRedoAndGetAllowRedoRoundTrip() {
        OAUndoableEdit edit = OAUndoableEdit.createUndoable("marker");

        assertTrue(edit.getAllowRedo());
        edit.setAllowRedo(false);
        assertFalse(edit.getAllowRedo());
    }

    @Test
    void equalsUsesTypeObjectIdentityAndPropertyName() {
        Product product = new Product(1);
        OAUndoableEdit edit1 = OAUndoableEdit.createUndoablePropertyChange("one", product, Product.P_Sku, "a", "b");
        OAUndoableEdit edit2 = OAUndoableEdit.createUndoablePropertyChange("two", product, Product.P_Sku, "b", "c");
        OAUndoableEdit differentProperty = OAUndoableEdit.createUndoablePropertyChange("three", product,
                Product.P_Weight, "1", "2");
        OAUndoableEdit differentObject = OAUndoableEdit.createUndoablePropertyChange("four", new Product(2),
                Product.P_Sku, "a", "b");

        assertEquals(edit1, edit2);
        assertNotEquals(edit1, differentProperty);
        assertNotEquals(edit1, differentObject);
        assertNotEquals(edit1, null);
        assertNotEquals(edit1, "not an edit");
    }

    @Test
    void hashCodeIsConsistentWithEqualsForSameTypeAndObject() {
        Product product = new Product(1);
        OAUndoableEdit edit1 = OAUndoableEdit.createUndoablePropertyChange("one", product, Product.P_Sku, "a", "b");
        OAUndoableEdit edit2 = OAUndoableEdit.createUndoablePropertyChange("two", product, Product.P_Sku, "b", "c");
        OAUndoableEdit itemEdit = OAUndoableEdit.createUndoablePropertyChange("item", new Item(1), Item.P_Name, "a",
                "b");

        assertEquals(edit1, edit2);
        assertEquals(edit1.hashCode(), edit2.hashCode());
        assertNotEquals(edit1.hashCode(), itemEdit.hashCode());
    }

    private void clearCache() {
        OAGraphInternal og = (OAGraphInternal) OARuntime.graph(Register.class);
        OAObjectInternalService os = (OAObjectInternalService) og.objectsInternal();
        os.getOAObjectCacheService().removeAllObjects();
    }
}
