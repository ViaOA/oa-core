package com.viaoa.select;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import com.viaoa.find.OAFinder;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OASelectPhase3WhereObjectFinderGapTest {

    public static class Parent extends OAObject {
        private String name;
        private final Hub<Child> children = new Hub<>(Child.class);

        public Parent() {
        }

        public Parent(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public Hub<Child> getChildren() {
            return children;
        }
    }

    public static class Child extends OAObject {
        private String name;
        private Parent parent;

        public Child() {
        }

        public Child(String name, Parent parent) {
            this.name = name;
            this.parent = parent;
        }

        public String getName() {
            return name;
        }

        public Parent getParent() {
            return parent;
        }
    }

    private static List<Parent> parents() {
        Parent a = new Parent("A");
        Parent b = new Parent("B");

        a.getChildren().add(new Child("A1", a));
        a.getChildren().add(new Child("A2", a));
        b.getChildren().add(new Child("B1", b));

        return List.of(a, b);
    }

    @Test
    void finderSelectWithWhereObjectShouldConstrainToRelatedBranchDesiredContract() {
        List<Parent> parents = parents();
        Hub<Parent> roots = new Hub<>(Parent.class);
        roots.add(parents.get(0));
        roots.add(parents.get(1));

        OASelect<Child> sel = new OASelect<>(Child.class);
        sel.setFinder(new OAFinder<Parent, Child>(roots, "children"));
        sel.setWhereObject(parents.get(0), "children");

        List<String> names = readNames(sel);

        assertEquals(List.of("A1", "A2"), names,
            "whereObject/propertyPath must constrain finder select; returning B1 exposes the known CODEX gap");
    }

    @Test
    void invalidWhereObjectPathShouldFailBeforeFinderIgnoresItDesiredContract() {
        List<Parent> parents = parents();
        Hub<Parent> roots = new Hub<>(Parent.class);
        roots.add(parents.get(0));
        roots.add(parents.get(1));

        OASelect<Child> sel = new OASelect<>(Child.class);
        sel.setFinder(new OAFinder<Parent, Child>(roots, "children"));
        sel.setWhereObject(parents.get(0), "missing.path");

        assertThrows(RuntimeException.class, sel::select,
            "whereObjectPropertyPath should be metadata-validated and not silently ignored in finder mode");
    }

    @Test
    void whereHubActiveObjectShouldConstrainFinderSelectDesiredContract() {
        List<Parent> parents = parents();
        Hub<Parent> roots = new Hub<>(Parent.class);
        roots.add(parents.get(0));
        roots.add(parents.get(1));
        roots.setAO(parents.get(1));

        OASelect<Child> sel = new OASelect<>(Child.class);
        sel.setFinder(new OAFinder<Parent, Child>(roots, "children"));
        sel.setWhereHub(roots, "children");

        List<String> names = readNames(sel);

        assertEquals(List.of("B1"), names,
            "whereHub.AO/propertyPath must constrain finder select to active parent branch");
    }

    private static List<String> readNames(OASelect<Child> sel) {
        List<String> names = new ArrayList<>();
        Child c;
        while ((c = sel.next()) != null) {
            names.add(c.getName());
        }
        return names;
    }
}
