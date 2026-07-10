
package com.viaoa.filter;

import com.test.pos.model.oa.Invoice;
import com.test.pos.model.oa.InvoiceBasket;
import com.test.pos.model.oa.Item;
import com.test.pos.model.oa.LineItem;
import com.test.pos.model.oa.Product;
import com.test.pos.model.oa.Register;
import com.test.pos.model.oa.RegisterSession;
import com.test.pos.model.oa.Store;
import com.test.pos.model.oa.propertypath.InvoicePP;
import com.viaoa.hub.Hub;
import com.viaoa.text.OATextUtil;

final class FilterTestSupport {
    private static final java.util.concurrent.atomic.AtomicInteger NEXT = new java.util.concurrent.atomic.AtomicInteger(1000);
    static final String ITEM_NAME_PATH = Invoice.P_InvoiceBaskets + "." + InvoiceBasket.P_LineItems + "."
            + LineItem.P_Product + "." + Product.P_Item + "." + Item.P_Name;
    static final String PRICE_PATH = Invoice.P_InvoiceBaskets + "." + InvoiceBasket.P_LineItems + "." + LineItem.P_PriceEach;
    static final String SEALED_PACKAGE_PATH = Invoice.P_InvoiceBaskets + "." + InvoiceBasket.P_LineItems + "."
            + LineItem.P_Product + "." + Product.P_SealedPackage;
    static final String GENERATED_ITEM_NAME_PATH = InvoicePP.invoiceBaskets().lineItems().product().item().name();
    static final String TEXT_UTIL_ITEM_NAME_PATH = OATextUtil.createPath(Invoice.P_InvoiceBaskets,
            InvoiceBasket.P_LineItems, LineItem.P_Product, Product.P_Item, Item.P_Name);

    private FilterTestSupport() {
    }

    static PosGraph graph() {
        int base = NEXT.addAndGet(100);
        Store store = new Store(base + 1);
        store.setName("Main Store");
        store.setStoreNumber(base + 42);

        Register register = new Register(base + 2);
        register.setCode("REG-1");
        store.getRegisters().add(register);
        store.getRegisters().setAO(register);

        RegisterSession session = new RegisterSession(base + 3);
        register.getRegisterSessions().add(session);
        register.getRegisterSessions().setAO(session);

        Invoice invoice = new Invoice(base + 4);
        session.getInvoices().add(invoice);
        session.getInvoices().setAO(invoice);

        InvoiceBasket basket = new InvoiceBasket(base + 5);
        invoice.getInvoiceBaskets().add(basket);
        invoice.getInvoiceBaskets().setAO(basket);

        LineItem lineItem = new LineItem(base + 6);
        lineItem.setQuantity(2);
        lineItem.setPriceEach(12.50);
        basket.getLineItems().add(lineItem);
        basket.getLineItems().setAO(lineItem);

        Product product = new Product(base + 7);
        product.setSku("SKU-001");
        product.setQuantityOnHand(7);
        product.setSealedPackage(true);
        lineItem.setProduct(product);

        Item item = new Item(base + 8);
        item.setCode("BP1");
        item.setName("Brake Pad");
        item.setBrand("ACME");
        product.setItem(item);

        return new PosGraph(store, register, session, invoice, basket, lineItem, product, item);
    }

    static Hub<Store> storeHub(PosGraph graph) {
        Hub<Store> hub = new Hub<>(Store.class);
        hub.add(graph.store);
        hub.setAO(graph.store);
        return hub;
    }

    static final class PosGraph {
        final Store store;
        final Register register;
        final RegisterSession session;
        final Invoice invoice;
        final InvoiceBasket basket;
        final LineItem lineItem;
        final Product product;
        final Item item;

        PosGraph(Store store, Register register, RegisterSession session, Invoice invoice, InvoiceBasket basket,
                LineItem lineItem, Product product, Item item) {
            this.store = store;
            this.register = register;
            this.session = session;
            this.invoice = invoice;
            this.basket = basket;
            this.lineItem = lineItem;
            this.product = product;
            this.item = item;
        }
    }
}
