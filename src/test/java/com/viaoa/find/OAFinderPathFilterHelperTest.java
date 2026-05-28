package com.viaoa.find;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAFinderPathFilterHelperTest {

    public static class Root extends OAObject {
        private final Hub<Child> children = new Hub<>(Child.class);

        public Hub<Child> getChildren() {
            return children;
        }
    }

    public static class Child extends OAObject {
        private String code;
        private Integer amount;
        private Boolean active;

        public Child() {
        }

        public Child(String code, Integer amount, Boolean active) {
            this.code = code;
            this.amount = amount;
            this.active = active;
        }

        public String getCode() {
            return code;
        }

        public Integer getAmount() {
            return amount;
        }

        public Boolean getActive() {
            return active;
        }
    }

    private static Root root() {
        Root root = new Root();
        root.getChildren().add(new Child("A", 10, true));
        root.getChildren().add(new Child("B", 20, false));
        root.getChildren().add(new Child("C", 30, true));
        root.getChildren().add(new Child("", 0, null));
        return root;
    }

    @Test
    void addNullAndNotNullFiltersUseTerminalPropertyPath() {
        Root root = root();

        OAFinder<Root, Child> isNull = new OAFinder<>("children");
        isNull.addNullFilter("active");
        assertEquals(1, isNull.find(root).size());

        OAFinder<Root, Child> notNull = new OAFinder<>("children");
        notNull.addNotNullFilter("active");
        assertEquals(3, notNull.find(root).size());
    }

    @Test
    void addEmptyAndNotEmptyFiltersUseTerminalPropertyPath() {
        Root root = root();

        OAFinder<Root, Child> empty = new OAFinder<>("children");
        empty.addEmptyFilter("code");
        assertEquals(1, empty.find(root).size());
        assertEquals("", empty.findFirst(root).getCode());

        OAFinder<Root, Child> notEmpty = new OAFinder<>("children");
        notEmpty.addNotEmptyFilter("code");
        assertEquals(3, notEmpty.find(root).size());
    }

    @Test
    void addBetweenAndBetweenOrEqualFiltersUseTerminalPropertyPath() {
        Root root = root();

        OAFinder<Root, Child> between = new OAFinder<>("children");
        between.addBetweenFilter("amount", 10, 30);
        List<Child> strict = between.find(root);
        assertEquals(1, strict.size());
        assertEquals("B", strict.get(0).getCode());

        OAFinder<Root, Child> inclusive = new OAFinder<>("children");
        inclusive.addBetweenOrEqualFilter("amount", 10, 30);
        List<Child> inc = inclusive.find(root);
        assertEquals(3, inc.size());
        assertEquals("A", inc.get(0).getCode());
        assertEquals("C", inc.get(2).getCode());
    }

    @Test
    void addBooleanTrueFalseFiltersUseTerminalPropertyPath() {
        Root root = root();

        OAFinder<Root, Child> trueFilter = new OAFinder<>("children");
        trueFilter.addTrueFilter("active");
        assertEquals(2, trueFilter.find(root).size());

        OAFinder<Root, Child> falseFilter = new OAFinder<>("children");
        falseFilter.addFalseFilter("active");
        assertEquals(1, falseFilter.find(root).size());
        assertEquals("B", falseFilter.findFirst(root).getCode());
    }

    @Test
    void addInFilterUsesCandidateMembershipValuesIfSupported() {
        Root root = root();

        OAFinder<Root, Child> finder = new OAFinder<>("children");
        finder.addInFilter("code", new String[] { "A", "C" });

        List<Child> result = finder.find(root);

        assertEquals(2, result.size());
        assertEquals("A", result.get(0).getCode());
        assertEquals("C", result.get(1).getCode());
    }
}
