package com.viaoa.path;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Invoice;
import com.test.pos.model.oa.InvoiceBasket;
import com.test.pos.model.oa.Item;
import com.test.pos.model.oa.ItemCategory;
import com.test.pos.model.oa.LineItem;
import com.test.pos.model.oa.Product;
import com.test.pos.model.oa.Register;
import com.test.pos.model.oa.RegisterSession;
import com.test.pos.model.oa.Store;
import com.test.pos.model.oa.propertypath.InvoicePP;
import com.test.pos.model.oa.propertypath.StorePP;
import com.viaoa.graph.api.internal.OAGraphInternal;
import com.viaoa.graph.service.OAObjectInternalService;
import com.viaoa.hub.Hub;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;
import com.viaoa.text.OATextUtil;

class OAPathTest {

    private static final String RAW_ITEM_NAME_PATH = Invoice.P_InvoiceBaskets + "." + InvoiceBasket.P_LineItems + "."
            + LineItem.P_Product + "." + Product.P_Item + "." + Item.P_Name;

    private static final String GENERATED_ITEM_NAME_PATH = InvoicePP.invoiceBaskets().lineItems().product().item().name();

    private static final String TEXT_UTIL_ITEM_NAME_PATH = OATextUtil.createPropertyPath(Invoice.P_InvoiceBaskets,
            InvoiceBasket.P_LineItems, LineItem.P_Product, Product.P_Item, Item.P_Name);

    private static final String RAW_STORE_ITEM_NAME_PATH = Store.P_Registers + "." + Register.P_RegisterSessions + "."
            + RegisterSession.P_Invoices + "." + Invoice.P_InvoiceBaskets + "." + InvoiceBasket.P_LineItems + "."
            + LineItem.P_Product + "." + Product.P_Item + "." + Item.P_Name;

    private static final String GENERATED_STORE_ITEM_NAME_PATH = StorePP.registers().registerSessions().invoices()
            .invoiceBaskets().lineItems().product().item().name();

    private static final String TEXT_UTIL_STORE_ITEM_NAME_PATH = OATextUtil.createPropertyPath(Store.P_Registers,
            Register.P_RegisterSessions, RegisterSession.P_Invoices, Invoice.P_InvoiceBaskets,
            InvoiceBasket.P_LineItems, LineItem.P_Product, Product.P_Item, Item.P_Name);

    @BeforeEach
    void beforeEach() {
    	OAGraphInternal og = (OAGraphInternal) OARuntime.graph(Register.class);
    	OAObjectInternalService os = (OAObjectInternalService) og.objectsInternal();
    	os.getOAObjectCacheService().removeAllObjects();
    }

    @Test
    void stringConstructorStoresPathAndDefersSetup() {
        OAPath<Invoice> path = new OAPath<>(RAW_ITEM_NAME_PATH);

        assertEquals(RAW_ITEM_NAME_PATH, path.getPropertyPath());
        assertNull(path.getFromClass());
        assertEquals(0, path.getMethods().length);

        Invoice invoice = createInvoiceGraph().invoice;
        assertEquals("Brake Pad", path.getValue(invoice));
        assertEquals(Invoice.class, path.getFromClass());
    }

    @Test
    void classConstructorParsesRawPathConstantsAndRejectsInvalidPath() {
        OAPath<Invoice> path = new OAPath<>(Invoice.class, RAW_ITEM_NAME_PATH);

        assertEquals(RAW_ITEM_NAME_PATH, path.getPropertyPath());
        assertEquals(Invoice.class, path.getFromClass());
        assertArrayEquals(new String[] { Invoice.P_InvoiceBaskets, InvoiceBasket.P_LineItems, LineItem.P_Product,
                Product.P_Item, Item.P_Name }, path.getProperties());

        assertThrows(IllegalArgumentException.class, () -> new OAPath<>(Invoice.class, "missingProperty"));
    }

    @Test
    void lenientConstructorKeepsInvalidPathWithoutThrowing() {
        OAPath<Invoice> path = new OAPath<>(Invoice.class, "missingProperty", true);

        assertEquals("missingProperty", path.getPropertyPath());
        assertEquals(Invoice.class, path.getFromClass());
        assertEquals(0, path.getMethods().length);
    }

    @Test
    void getPropertyPathReturnsOriginalPathText() {
        OAPath<Invoice> path = new OAPath<>(Invoice.class, "  " + RAW_ITEM_NAME_PATH + "  ");

        assertEquals("  " + RAW_ITEM_NAME_PATH + "  ", path.getPropertyPath());
        assertEquals("Brake Pad", path.getValue(createInvoiceGraph().invoice));
    }

    @Test
    void getReversePathBuildsReverseFromResolvedLinksAndCachesResult() {
        OAPath<Invoice> path = new OAPath<>(Invoice.class, Invoice.P_InvoiceBaskets + "." + InvoiceBasket.P_LineItems);

        OAPath reverse = path.getReversePath();

        assertNotNull(reverse);
        assertEquals((LineItem.P_InvoiceBasket + "." + InvoiceBasket.P_Invoice).toLowerCase(), reverse.getPropertyPath().toLowerCase());
        assertSame(reverse, path.getReversePath());
    }

    @Test
    void getReversePathBooleanUsesSamePublicReverseForNonPrivateLinks() {
        OAPath<Invoice> path = new OAPath<>(Invoice.class, Invoice.P_InvoiceBaskets + "." + InvoiceBasket.P_LineItems);

        assertEquals(path.getReversePath().getPropertyPath(), path.getReversePath(true).getPropertyPath());
        assertNull(new OAPath<>(Invoice.class, Invoice.P_Id).getReversePath(true));
    }

    @Test
    void getPathLinksOnlyReturnsOnlyLinkSegmentsBeforeTerminalProperty() {
        OAPath<Invoice> path = new OAPath<>(Invoice.class, RAW_ITEM_NAME_PATH);

        assertEquals((Invoice.P_InvoiceBaskets + "." + InvoiceBasket.P_LineItems + "." + LineItem.P_Product + "."
                + Product.P_Item).toUpperCase(), path.getPathLinksOnly().toUpperCase());
    }

    @Test
    void terminalMetadataAccessorsIdentifyPropertyCalculatedPropertyAndLink() {
        OAPath<Store> propertyPath = new OAPath<>(Store.class, Store.P_Name);
        assertNotNull(propertyPath.getEndPropertyInfo());
        assertNull(propertyPath.getEndCalcInfo());
        assertNull(propertyPath.getEndLinkInfo());
        assertNotNull(propertyPath.getOAPropertyAnnotation());
        assertNull(propertyPath.getOACalculatedPropertyAnnotation());
        assertNull(propertyPath.getOAOneAnnotation());

        OAPath<LineItem> calcPath = new OAPath<>(LineItem.class, LineItem.P_TotalItemAmount);
        assertNull(calcPath.getEndPropertyInfo());
        assertNotNull(calcPath.getEndCalcInfo());
        assertNull(calcPath.getEndLinkInfo());
        assertNull(calcPath.getOAPropertyAnnotation());
        assertNotNull(calcPath.getOACalculatedPropertyAnnotation());

        OAPath<LineItem> linkPath = new OAPath<>(LineItem.class, LineItem.P_Product);
        assertNull(linkPath.getEndPropertyInfo());
        assertNull(linkPath.getEndCalcInfo());
        assertNotNull(linkPath.getEndLinkInfo());
        assertNotNull(linkPath.getOAOneAnnotation());
    }

    @Test
    void parsedArraysExposePropertiesCastsFiltersMethodsClassesAndLinks() {
        OAPath<Invoice> path = new OAPath<>(Invoice.class, Invoice.P_InvoiceBaskets + ":Open()."
            + InvoiceBasket.P_LineItems + "." + LineItem.P_Product + "." + Product.P_Item + "." + Item.P_Name);
        
        assertArrayEquals(new String[] { Invoice.P_InvoiceBaskets, InvoiceBasket.P_LineItems, LineItem.P_Product,
                Product.P_Item, Item.P_Name }, path.getProperties());
        assertArrayEquals(new String[] { null, null, null, null, null }, path.getCastNames());
        assertArrayEquals(new String[] { "Open", null, null, null, null }, path.getFilterNames());
        assertArrayEquals(new String[] { "()", null, null, null, null }, path.getFilterParams());
        assertEquals(5, path.getFilterParamValues().length);
        assertNull(path.getFilterParamValues()[0]);

        Method[] methods = path.getMethods();
        assertEquals(5, methods.length);
        assertEquals("getInvoiceBaskets", methods[0].getName());
        assertEquals("getName", methods[4].getName());

        assertArrayEquals(new Class[] { InvoiceBasket.class, LineItem.class, Product.class, Item.class, String.class },
                path.getClasses());

        Constructor[] filterConstructors = path.getFilterConstructors();
        assertEquals(5, filterConstructors.length);
        assertNull(filterConstructors[0]);

        assertEquals(4, path.getLinkInfos().length);
        assertTrue(path.hasLinks());
    }

    @Test
    void recursiveLinkInfoIsResolvedForRecursiveModelRelationship() {
        OAPath<ItemCategory> path = new OAPath<>(ItemCategory.class,
                ItemCategory.P_SubItemCategories + "." + ItemCategory.P_Name);

        OALinkInfo[] recursiveLinks = path.getRecursiveLinkInfos();

        assertEquals(1, recursiveLinks.length);
        assertNotNull(recursiveLinks[0]);
        assertEquals(ItemCategory.P_SubItemCategories.toUpperCase(), path.getLinkInfos()[0].getName().toUpperCase());
    }

    @Test
    void getValueTraversesNestedHubActiveObjectsAndSupportsGeneratedPathHelpers() {
        InvoiceGraph graph = createInvoiceGraph();
        OAPath<Invoice> rawPath = new OAPath<>(Invoice.class, RAW_ITEM_NAME_PATH);
        OAPath<Invoice> generatedPath = new OAPath<>(Invoice.class, GENERATED_ITEM_NAME_PATH);

        assertEquals("Brake Pad", rawPath.getValue(graph.invoice));
        assertEquals("Brake Pad", generatedPath.getValue(graph.invoice));

        Item second = new Item(902);
        second.setName("Oil Filter");
        Product secondProduct = new Product(903);
        secondProduct.setItem(second);
        LineItem secondLine = new LineItem(904);
        secondLine.setProduct(secondProduct);
        graph.basket.getLineItems().add(secondLine);
        graph.basket.getLineItems().setAO(secondLine);

        assertEquals("Oil Filter", rawPath.getValue(graph.invoice));
    }

    @Test
    void getValueTraversesPathBuiltWithOATextUtilCreatePropertyPath() {
        assertEquals(RAW_ITEM_NAME_PATH, TEXT_UTIL_ITEM_NAME_PATH);

        OAPath<Invoice> path = new OAPath<>(Invoice.class, TEXT_UTIL_ITEM_NAME_PATH);

        assertEquals("Brake Pad", path.getValue(createInvoiceGraph().invoice));
    }

    @Test
    void pathConstructionMechanismsProduceEquivalentStoreToItemNamePath() {
        assertEquals(RAW_STORE_ITEM_NAME_PATH, GENERATED_STORE_ITEM_NAME_PATH);
        assertEquals(RAW_STORE_ITEM_NAME_PATH, TEXT_UTIL_STORE_ITEM_NAME_PATH);

        InvoiceGraph graph = createInvoiceGraph();
        OAPath<Store> rawPath = new OAPath<>(Store.class, RAW_STORE_ITEM_NAME_PATH);
        OAPath<Store> generatedPath = new OAPath<>(Store.class, GENERATED_STORE_ITEM_NAME_PATH);
        OAPath<Store> textUtilPath = new OAPath<>(Store.class, TEXT_UTIL_STORE_ITEM_NAME_PATH);

        assertEquals("Brake Pad", rawPath.getValue(graph.store));
        assertEquals("Brake Pad", generatedPath.getValue(graph.store));
        assertEquals("Brake Pad", textUtilPath.getValue(graph.store));
    }

    @Test
    void setupParsesFilterSegmentBuiltWithOATextUtilCreatePropertyPath() {
        String pathText = OATextUtil.createPropertyPath(Invoice.P_InvoiceBaskets, ":Open()",
                InvoiceBasket.P_LineItems);
        OAPath<Invoice> path = new OAPath<>(Invoice.class, pathText);

        assertEquals(Invoice.P_InvoiceBaskets + ":Open()." + InvoiceBasket.P_LineItems, pathText);
        assertArrayEquals(new String[] { "Open", null }, path.getFilterNames());
        assertArrayEquals(new String[] { "()", null }, path.getFilterParams());
        assertEquals(2, path.getLinkInfos().length);
    }


    @Test
    void getValueAsStringUsesDefaultAndExplicitFormatting() {
        LineItem lineItem = createInvoiceGraph().lineItem;
        OAPath<LineItem> pricePath = new OAPath<>(LineItem.class, LineItem.P_PriceEach);

        assertNotNull(pricePath.getValueAsString(lineItem));
        assertEquals("12.50", pricePath.getValueAsString(null, lineItem, "0.00"));
    }

    @Test
    void getLastLinkValueReturnsLastResolvedLinkObject() {
        InvoiceGraph graph = createInvoiceGraph();
        OAPath<Invoice> path = new OAPath<>(Invoice.class, RAW_ITEM_NAME_PATH);

        assertSame(graph.item, path.getLastLinkValue(graph.invoice));
    }

    @Test
    void firstAndLastPropertyNamesReflectParsedPath() {
        OAPath<Invoice> path = new OAPath<>(Invoice.class, RAW_ITEM_NAME_PATH);

        assertEquals(Item.P_Name, path.getLastPropertyName());
        assertEquals(Invoice.P_InvoiceBaskets, path.getFirstPropertyName());
    }

    @Test
    void hubValueMethodsUseHubContextAndLinksOnlyTraversal() {
        InvoiceGraph graph = createInvoiceGraph();
        Hub<Invoice> hub = new Hub<>(Invoice.class);
        hub.add(graph.invoice);
        hub.setAO(graph.invoice);
        OAPath<Invoice> path = new OAPath<>(Invoice.class, RAW_ITEM_NAME_PATH);

        assertEquals("Brake Pad", path.getValue(hub, graph.invoice));
        assertSame(graph.item, path.getValue(hub, graph.invoice, true));
        assertEquals("Brake Pad", path.getValueAsString(hub, graph.invoice));
        assertEquals("Brake Pad", path.getValueAsString(hub, graph.invoice, "ignored-for-strings"));
        assertNull(path.getValue(hub, null));
    }

    @Test
    void getFromClassReturnsResolvedClassIncludingLeadingRootQualifier() {
        OAPath<Store> path = new OAPath<>(Store.class, "[Invoice]." + RAW_ITEM_NAME_PATH);

        assertEquals(Invoice.class, path.getFromClass());
    }

    @Test
    void setupHubUsesHubObjectClass() {
        OAPath<Store> path = new OAPath<>(Store.P_Name);
        Hub<Store> hub = new Hub<>(Store.class);

        path.setup(hub);

        assertEquals(Store.class, path.getFromClass());
        assertEquals("getName", path.getMethods()[0].getName());
    }

    @Test
    void setupClassParsesValidPathAndThrowsForInvalidPath() {
        OAPath<Store> path = new OAPath<>(Store.P_Name);
        path.setup(Store.class);

        assertEquals(Store.class, path.getFromClass());
        assertThrows(RuntimeException.class, () -> new OAPath<Store>("badProperty").setup(Store.class));
    }

    @Test
    void setupClassWithPrivateFlagParsesPublicLinksAndHasNoPrivateLink() {
        OAPath<Invoice> path = new OAPath<>(RAW_ITEM_NAME_PATH);

        path.setup(Invoice.class, true);

        assertFalse(path.hasPrivateLink());
        assertEquals("Brake Pad", path.getValue(createInvoiceGraph().invoice));
    }

    @Test
    void hubParamAndNeedsDataFlagsReflectCurrentSetupState() {
        OAPath<Invoice> path = new OAPath<>(RAW_ITEM_NAME_PATH);

        assertFalse(path.getDoesLastMethodHasHubParam());
        assertFalse(path.getNeedsDataToVerify());

        String error = path.setup(null, null, false);

        assertEquals("Hub.objectClass not set", error);
        assertTrue(path.getNeedsDataToVerify());
    }

    @Test
    void setupWithHubClassAndSubstituteClassReturnsErrorOrNullInsteadOfThrowing() {
        OAPath<Invoice> valid = new OAPath<>(RAW_ITEM_NAME_PATH);
        assertNull(valid.setup(null, Invoice.class, false));

        OAPath<OAObject> invalid = new OAPath<>("missingProperty");
        assertNotNull(invalid.setup(null, Invoice.class, Product.class, false));
    }

    @Test
    void getFormatAndHasHubPropertyUseTerminalMetadata() {
        OAPath<LineItem> pricePath = new OAPath<>(LineItem.class, LineItem.P_PriceEach);

        assertNotNull(pricePath.getFormat());
        assertTrue(pricePath.getFormat().endsWith("00"));
        assertFalse(pricePath.getHasHubProperty());

        OAPath<Invoice> hubPath = new OAPath<>(Invoice.class, RAW_ITEM_NAME_PATH);
        assertTrue(hubPath.getHasHubProperty());
        assertSame(hubPath.getFormat(), hubPath.getFormat());
    }

    @Test
    void getValueWithStartPositionContinuesTraversalFromObjectAlreadyInPath() {
        InvoiceGraph graph = createInvoiceGraph();
        OAPath<Invoice> path = new OAPath<>(Invoice.class, RAW_ITEM_NAME_PATH);

        assertEquals("Brake Pad", path.getValue(graph.lineItem, 2));
        assertEquals("Brake Pad", path.getValue(graph.product, 3));
        assertNull(path.getValue(null, 2));
        assertDoesNotThrow(() -> path.getValue(graph.invoice, 99));
    }

    private static InvoiceGraph createInvoiceGraph() {
        Store store = new Store(100);
        store.setName("Main Store");
        store.setStoreNumber(42);

        Register register = new Register(200);
        register.setCode("REG-1");
        store.getRegisters().add(register);
        store.getRegisters().setAO(register);

        RegisterSession session = new RegisterSession(300);
        register.getRegisterSessions().add(session);
        register.getRegisterSessions().setAO(session);

        Invoice invoice = new Invoice(400);
        session.getInvoices().add(invoice);
        session.getInvoices().setAO(invoice);

        InvoiceBasket basket = new InvoiceBasket(500);
        invoice.getInvoiceBaskets().add(basket);
        invoice.getInvoiceBaskets().setAO(basket);

        LineItem lineItem = new LineItem(600);
        lineItem.setQuantity(2);
        lineItem.setPriceEach(12.5);
        basket.getLineItems().add(lineItem);
        basket.getLineItems().setAO(lineItem);

        Product product = new Product(700);
        product.setSku("SKU-1");
        lineItem.setProduct(product);

        Item item = new Item(800);
        item.setName("Brake Pad");
        item.setCode("BP1");
        product.setItem(item);

        return new InvoiceGraph(store, register, session, invoice, basket, lineItem, product, item);
    }

    private static final class InvoiceGraph {
        final Store store;
        final Register register;
        final RegisterSession session;
        final Invoice invoice;
        final InvoiceBasket basket;
        final LineItem lineItem;
        final Product product;
        final Item item;

        InvoiceGraph(Store store, Register register, RegisterSession session, Invoice invoice, InvoiceBasket basket,
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
