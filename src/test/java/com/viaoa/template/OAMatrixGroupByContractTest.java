package com.viaoa.template;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAMatrixGroupByContractTest {

    public static class Customer extends OAObject {
        private String name;
        private final Hub<Order> orders = new Hub<>(Order.class);

        public Customer() { }
        public Customer(String name) { this.name = name; }
        public String getName() { return name; }
        public Hub<Order> getOrders() { return orders; }
    }

    public static class Order extends OAObject {
        private String name;
        private Customer customer;

        public Order() { }
        public Order(String name, Customer customer) {
            this.name = name;
            this.customer = customer;
        }
        public String getName() { return name; }
        public Customer getCustomer() { return customer; }
    }

    private static Hub<Customer> customers(Customer... cs) {
        Hub<Customer> hub = new Hub<>(Customer.class);
        for (Customer c : cs) hub.add(c);
        return hub;
    }

    private static Hub<Order> orders(Order... os) {
        Hub<Order> hub = new Hub<>(Order.class);
        for (Order o : os) hub.add(o);
        return hub;
    }

    @Test
    void addGroupByColumnFromRootColumnCreatesColumn() {
        Customer a = new Customer("A");
        Order o1 = new Order("O1", a);
        Hub<Customer> customers = customers(a);
        Hub<Order> orders = orders(o1);

        OAMatrix m = new OAMatrix();
        OAMatrix.Column root = m.addColumn(customers);

        OAMatrix.Column gb = m.addGroupByColumn(root, orders, "orders", "customer");

        assertNotNull(gb);
        assertSame(root, gb.getFromColumn());
        assertEquals("orders", gb.getPropertyPath());
        assertEquals(2, m.getColumnCount());
    }

    @Test
    void groupByMatchesObjectsByMatchProperty() {
        Customer a = new Customer("A");
        Customer b = new Customer("B");
        Order a1 = new Order("A1", a);
        Order a2 = new Order("A2", a);
        Order b1 = new Order("B1", b);

        OAMatrix m = new OAMatrix();
        OAMatrix.Column root = m.addColumn(customers(a, b));
        m.addGroupByColumn(root, orders(a1, a2, b1), "orders", "customer");

        assertEquals(3, m.getRowCount());

        assertSame(a, m.getRealObject(0, 0));
        assertSame(a, m.getRealObject(1, 0));
        assertSame(b, m.getRealObject(2, 0));

        assertEquals("A1", ((Order) m.getObject(0, 1)).getName());
        assertEquals("A2", ((Order) m.getObject(1, 1)).getName());
        assertEquals("B1", ((Order) m.getObject(2, 1)).getName());
    }

    @Test
    void groupByWithNoMatchesKeepsParentRowAndBlankGroupColumn() {
        Customer a = new Customer("A");

        OAMatrix m = new OAMatrix();
        OAMatrix.Column root = m.addColumn(customers(a));
        m.addGroupByColumn(root, new Hub<>(Order.class), "orders", "customer");

        assertEquals(1, m.getRowCount());
        assertSame(a, m.getRealObject(0, 0));
        assertNull(m.getObject(0, 1));
    }

    @Test
    void groupByFromDetailColumnDoesNotThrowNullPointerDesiredContract() {
        Customer a = new Customer("A");
        a.getOrders().add(new Order("A1", a));

        OAMatrix m = new OAMatrix();
        OAMatrix.Column root = m.addColumn(customers(a));
        OAMatrix.Column detail = m.addDetailColumn(root, "orders");

        assertThrows(RuntimeException.class, () -> m.addGroupByColumn(detail, new Hub<>(Order.class), "orders", "customer"),
            "detail-column group-by should be validated against correct root/path or fail with controlled exception, not NPE");
    }

    @Test
    void invalidGroupByPathFailsControlled() {
        Customer a = new Customer("A");
        OAMatrix m = new OAMatrix();
        OAMatrix.Column root = m.addColumn(customers(a));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> m.addGroupByColumn(root, new Hub<>(Order.class), "name", "customer"));

        assertTrue(ex.getMessage().contains("invalid propertyPath"));
    }
}
