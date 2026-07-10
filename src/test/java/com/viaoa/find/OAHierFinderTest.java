package com.viaoa.find;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.ItemCategory;
import com.test.pos.model.oa.propertypath.ItemCategoryPP;
import com.viaoa.filter.OAEqualFilter;
import com.viaoa.filter.OAFilter;
import com.viaoa.text.OATextUtil;

class OAHierFinderTest {
    private static final AtomicInteger NEXT = new AtomicInteger(5000);
    private static final String RAW_PARENT_PATH = "parentItemCategory";
    private static final String GENERATED_PARENT_PATH = ItemCategoryPP.parentItemCategory().pp();
    private static final String TEXT_UTIL_PARENT_PATH = OATextUtil.createPath(ItemCategory.P_ParentItemCategory);

    @Test
    void constructorsIncludeOrSkipStartingObject() {
        CategoryGraph graph = categoryGraph();

        OAHierFinder<ItemCategory> includeStart = new OAHierFinder<>(ItemCategory.P_Name, GENERATED_PARENT_PATH);
        assertEquals("Child", includeStart.findFirst(graph.child));

        OAHierFinder<ItemCategory> skipStart = new OAHierFinder<>(ItemCategory.P_Name, GENERATED_PARENT_PATH, false);
        assertEquals("Parent", skipStart.findFirst(graph.child));
    }

    @Test
    void findFirstWithExplicitFilterFindsFirstMatchingHierarchyValue() {
        CategoryGraph graph = categoryGraph();
        OAHierFinder<ItemCategory> finder = new OAHierFinder<>(ItemCategory.P_Name, GENERATED_PARENT_PATH);

        assertEquals("Parent", finder.findFirst(graph.child, new OAEqualFilter("Parent")));
        assertNull(finder.findFirst(null, new OAEqualFilter("Parent")));
        assertNull(finder.findFirst(graph.child, new OAEqualFilter("Missing")));
    }

    @Test
    void findFirstUsesNotEmptyFilterByDefault() {
        CategoryGraph graph = categoryGraph();
        graph.child.setName("");
        OAHierFinder<ItemCategory> finder = new OAHierFinder<>(ItemCategory.P_Name, RAW_PARENT_PATH);

        assertEquals("Parent", finder.findFirst(graph.child));
    }

    @Test
    void findFirstNotEmptyUsesNotEmptyFilter() {
        CategoryGraph graph = categoryGraph();
        graph.child.setName(null);
        OAHierFinder<ItemCategory> finder = new OAHierFinder<>(ItemCategory.P_Name, TEXT_UTIL_PARENT_PATH);

        assertEquals("Parent", finder.findFirstNotEmpty(graph.child));
    }

    @Test
    void findFirstEmptyUsesEmptyFilter() {
        CategoryGraph graph = categoryGraph();
        graph.child.setName("");
        OAHierFinder<ItemCategory> finder = new OAHierFinder<>(ItemCategory.P_Name, GENERATED_PARENT_PATH);

        assertEquals("", finder.findFirstEmpty(graph.child));
    }

    @Test
    void findFirstNotNullUsesNotNullFilter() {
        CategoryGraph graph = categoryGraph();
        graph.child.setName(null);
        OAHierFinder<ItemCategory> finder = new OAHierFinder<>(ItemCategory.P_Name, GENERATED_PARENT_PATH);

        assertEquals("Parent", finder.findFirstNotNull(graph.child));
    }

    @Test
    void findFirstTrueUsesBooleanConversion() {
        CategoryGraph graph = categoryGraph();
        graph.child.setCode("false");
        graph.parent.setCode("true");
        OAHierFinder<ItemCategory> finder = new OAHierFinder<>(ItemCategory.P_Code, GENERATED_PARENT_PATH, false);

        assertEquals("true", finder.findFirstTrue(graph.child));
    }

    @Test
    void protectedFindFirstValueOverloadsDelegateToHierarchyTraversal() {
        CategoryGraph graph = categoryGraph();
        ExposedHierFinder finder = new ExposedHierFinder(ItemCategory.P_Name, GENERATED_PARENT_PATH);
        OAFilter<String> parentFilter = value -> "Parent".equals(value);

        finder.findFirst(graph.child, parentFilter);

        assertTrue(finder.callFindFirstValue(graph.child, parentFilter, 0));
        assertTrue(finder.callFindFirstValue(graph.child, parentFilter, 0, false));
        assertFalse(finder.callFindFirstValue(null, parentFilter, 0));
    }

    @Test
    void pathConstructionMechanismsAreEquivalentForParentCategoryPath() {
        assertEquals(ItemCategory.P_ParentItemCategory, RAW_PARENT_PATH);
        assertEquals(ItemCategory.P_ParentItemCategory, GENERATED_PARENT_PATH);
        assertEquals(ItemCategory.P_ParentItemCategory, TEXT_UTIL_PARENT_PATH);

        CategoryGraph graph = categoryGraph();
        assertEquals("Parent", new OAHierFinder<ItemCategory>(ItemCategory.P_Name, RAW_PARENT_PATH, false)
                .findFirst(graph.child));
        assertEquals("Parent", new OAHierFinder<ItemCategory>(ItemCategory.P_Name, GENERATED_PARENT_PATH, false)
                .findFirst(graph.child));
        assertEquals("Parent", new OAHierFinder<ItemCategory>(ItemCategory.P_Name, TEXT_UTIL_PARENT_PATH, false)
                .findFirst(graph.child));
    }

    private static CategoryGraph categoryGraph() {
        int base = NEXT.addAndGet(10);
        ItemCategory parent = new ItemCategory(base + 1);
        parent.setCode("PARENT");
        parent.setName("Parent");

        ItemCategory child = new ItemCategory(base + 2);
        child.setCode("CHILD");
        child.setName("Child");
        child.setParentItemCategory(parent);
        parent.getSubItemCategories().add(child);

        return new CategoryGraph(parent, child);
    }

    private static final class CategoryGraph {
        final ItemCategory parent;
        final ItemCategory child;

        CategoryGraph(ItemCategory parent, ItemCategory child) {
            this.parent = parent;
            this.child = child;
        }
    }

    private static final class ExposedHierFinder extends OAHierFinder<ItemCategory> {
        ExposedHierFinder(String propertyName, String propertyPath) {
            super(propertyName, propertyPath);
        }

        boolean callFindFirstValue(ItemCategory obj, OAFilter filter, int pos) {
            return findFirstValue(obj, filter, pos);
        }

        boolean callFindFirstValue(ItemCategory obj, OAFilter filter, int pos, boolean recursiveOnly) {
            return findFirstValue(obj, filter, pos, recursiveOnly);
        }
    }
}
