package com.viaoa.compare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OAComparator")
class OAComparatorTest {

    @Test
    @DisplayName("constructor exposes configured property paths and default direction")
    void constructorExposesConfiguredValues() {
        OAComparator comparator = new OAComparator(Person.class, "lastName, firstName", true);

        assertEquals("lastName, firstName", comparator.getPropertyPaths());
        assertTrue(comparator.getAsc());
    }

    @Test
    @DisplayName("null values sort first when ascending")
    void nullValuesSortFirstWhenAscending() {
        OAComparator comparator = new OAComparator(null, null, true);

        assertEquals(0, comparator.compare(null, null));
        assertTrue(comparator.compare(null, "x") < 0);
        assertTrue(comparator.compare("x", null) > 0);
    }

    @Test
    @DisplayName("null values sort last when descending")
    void nullValuesSortLastWhenDescending() {
        OAComparator comparator = new OAComparator(null, null, false);

        assertEquals(0, comparator.compare(null, null));
        assertTrue(comparator.compare(null, "x") > 0);
        assertTrue(comparator.compare("x", null) < 0);
    }

    @Test
    @DisplayName("property-path null values sort deterministically")
    void sortsNullsDeterministically() {
        List<Person> people = Arrays.asList(
                new Person("Smith", "Beth", 30),
                new Person(null, "Adam", 40),
                new Person("Adams", "Carl", 50));

        Collections.sort(people, new OAComparator(Person.class, "lastName", true));
        assertEquals(Arrays.asList(null, "Adams", "Smith"), Arrays.asList(
                people.get(0).getLastName(), people.get(1).getLastName(), people.get(2).getLastName()));

        Collections.sort(people, new OAComparator(Person.class, "lastName", false));
        assertEquals(Arrays.asList("Smith", "Adams", null), Arrays.asList(
                people.get(0).getLastName(), people.get(1).getLastName(), people.get(2).getLastName()));
    }

    @Test
    @DisplayName("no-property comparator compares Comparable values directly")
    void noPropertyComparatorComparesComparableValuesDirectly() {
        OAComparator ascending = new OAComparator(String.class, "", true);
        OAComparator descending = new OAComparator(String.class, "", false);

        assertTrue(ascending.compare("a", "b") < 0);
        assertTrue(ascending.compare("b", "a") > 0);
        assertEquals(0, ascending.compare("a", "a"));

        assertTrue(descending.compare("a", "b") > 0);
        assertTrue(descending.compare("b", "a") < 0);
        assertEquals(0, descending.compare("a", "a"));
    }

    @Test
    @DisplayName("no-property comparator also falls back when class and property path are null")
    void fallsBackWhenNoPropertyPathConfigured() {
        OAComparator ascending = new OAComparator(null, null, true);
        OAComparator descending = new OAComparator(null, null, false);

        assertTrue(ascending.compare("a", "b") < 0);
        assertTrue(descending.compare("a", "b") > 0);
        assertEquals(0, ascending.compare("same", "same"));
    }

    @Test
    @DisplayName("no-property comparator currently throws for incompatible Comparable types")
    void noPropertyComparatorThrowsForIncompatibleComparableTypes() {
        OAComparator comparator = new OAComparator(Object.class, "", true);

        assertThrows(ClassCastException.class, () -> comparator.compare("1", 1));
    }

    @Test
    @DisplayName("single property path sorts by reflected getter")
    void singlePropertyPathSortsByGetter() {
        List<Person> people = Arrays.asList(
                new Person("Smith", "Bob", 40),
                new Person("Jones", "Ann", 30),
                new Person("Brown", "Cat", 20));

        Collections.sort(people, new OAComparator(Person.class, "lastName", true));

        assertEquals("Brown", people.get(0).getLastName());
        assertEquals("Jones", people.get(1).getLastName());
        assertEquals("Smith", people.get(2).getLastName());
    }

    @Test
    @DisplayName("string property sorting is case-insensitive")
    void stringPropertySortingIsCaseInsensitive() {
        OAComparator comparator = new OAComparator(Person.class, "lastName", true);

        assertEquals(0, comparator.compare(new Person("smith", "Bob", 40), new Person("SMITH", "Ann", 30)));
    }

    @Test
    @DisplayName("case-insensitive string compare still orders distinct values")
    void comparesStringsCaseInsensitively() {
        OAComparator comparator = new OAComparator(Person.class, "lastName", true);

        assertEquals(0, comparator.compare(new Person("smith", "a", 1), new Person("SMITH", "b", 2)));
        assertTrue(comparator.compare(new Person("adams", "a", 1), new Person("Smith", "b", 2)) < 0);
    }

    @Test
    @DisplayName("DESC property direction reverses ordering")
    void descPropertyDirectionReversesOrdering() {
        List<Person> people = Arrays.asList(
                new Person("Smith", "Bob", 40),
                new Person("Jones", "Ann", 30),
                new Person("Brown", "Cat", 20));

        Collections.sort(people, new OAComparator(Person.class, "lastName DESC", true));

        assertEquals("Smith", people.get(0).getLastName());
        assertEquals("Jones", people.get(1).getLastName());
        assertEquals("Brown", people.get(2).getLastName());
    }

    @Test
    @DisplayName("multiple property paths apply tie-breakers in order")
    void multiplePropertyPathsApplyTieBreakersInOrder() {
        List<Person> people = Arrays.asList(
                new Person("Smith", "Bob", 40),
                new Person("Smith", "Ann", 30),
                new Person("Brown", "Cat", 20));

        Collections.sort(people, new OAComparator(Person.class, "lastName, firstName", true));

        assertEquals("Brown", people.get(0).getLastName());
        assertEquals("Ann", people.get(1).getFirstName());
        assertEquals("Bob", people.get(2).getFirstName());
    }

    @Test
    @DisplayName("mixed ASC and DESC property directions are honored for comma-separated properties")
    void mixedDirectionsAreHonored() {
        List<Person> people = Arrays.asList(
                new Person("Smith", "Ann", 30),
                new Person("Smith", "Bob", 40),
                new Person("Brown", "Cat", 20));

        Collections.sort(people, new OAComparator(Person.class, "lastName ASC, firstName DESC", true));

        assertEquals("Brown", people.get(0).getLastName());
        assertEquals("Bob", people.get(1).getFirstName());
        assertEquals("Ann", people.get(2).getFirstName());
    }

    @Test
    @DisplayName("ASC and DESC remain bound to the correct property with whitespace before commas")
    void sortsByPropertyPathWithAscDescAndWhitespace() {
        List<Person> people = Arrays.asList(
                new Person("Smith", "Beth", 30),
                new Person("Smith", "Adam", 40),
                new Person("Adams", "Carl", 50));

        Collections.sort(people, new OAComparator(Person.class, "lastName DESC, firstName DESC", true));

        assertEquals("Smith", people.get(0).getLastName());
        assertEquals("Beth", people.get(0).getFirstName());
        assertEquals("Adam", people.get(1).getFirstName());
        assertEquals("Adams", people.get(2).getLastName());
    }

    @Test
    @DisplayName("property-path comparison is deterministic across repeated calls")
    void propertyPathComparisonIsDeterministicAcrossRepeatedCalls() {
        OAComparator comparator = new OAComparator(Person.class, "lastName ASC, firstName DESC", true);
        Person a = new Person("Smith", "Ann", 30);
        Person b = new Person("Smith", "Bob", 40);

        int expected = comparator.compare(a, b);
        for (int i = 0; i < 100; i++) {
            assertEquals(expected, comparator.compare(a, b));
            assertEquals(-Integer.signum(expected), Integer.signum(comparator.compare(b, a)));
        }
    }

    public static final class Person {
        private final String lastName;
        private final String firstName;
        private final int age;

        public Person(String lastName, String firstName, int age) {
            this.lastName = lastName;
            this.firstName = firstName;
            this.age = age;
        }

        public String getLastName() {
            return lastName;
        }

        public String getFirstName() {
            return firstName;
        }

        public int getAge() {
            return age;
        }
    }
}
