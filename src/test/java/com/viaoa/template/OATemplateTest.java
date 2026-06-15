package com.viaoa.template;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Invoice;
import com.test.pos.model.oa.InvoiceBasket;
import com.test.pos.model.oa.Item;
import com.test.pos.model.oa.LineItem;
import com.test.pos.model.oa.Product;
import com.test.pos.model.oa.Register;
import com.test.pos.model.oa.RegisterSession;
import com.test.pos.model.oa.Store;
import com.test.pos.model.oa.propertypath.StorePP;
import com.viaoa.config.OAProperties;
import com.viaoa.graph.api.internal.OAGraphInternal;
import com.viaoa.graph.service.OAObjectInternalService;
import com.viaoa.hub.Hub;
import com.viaoa.runtime.OARuntime;
import com.viaoa.text.OATextUtil;

class OATemplateTest {

    private static final String RAW_STORE_TO_ITEM_NAME_PATH =
            "registers.registerSessions.invoices.invoiceBaskets.lineItems.product.item.name";
    private static final String PP_STORE_TO_ITEM_NAME_PATH = StorePP.registers().registerSessions().invoices()
            .invoiceBaskets().lineItems().product().item().name();
    private static final String TEXT_UTIL_STORE_TO_ITEM_NAME_PATH = OATextUtil.createPropertyPath(Store.P_Registers,
            Register.P_RegisterSessions, RegisterSession.P_Invoices, Invoice.P_InvoiceBaskets,
            InvoiceBasket.P_LineItems, LineItem.P_Product, Product.P_Item, Item.P_Name);

    private static class ExposedTemplate extends OATemplate<Store> {
        ExposedTemplate() {
        }

        ExposedTemplate(String template) {
            super(template);
        }

        TreeNode tree(String doc) {
            return createTree(doc);
        }

        String preprocessText(String doc) {
            return preprocess(doc);
        }

        String preprocessText(String doc, ArrayList<String> includes) {
            return preprocess(doc, includes);
        }

        ArrayList<Token> tokens(String doc) {
            return parseTokens(doc);
        }

        boolean generateText(TreeNode node, Store store, Hub<Store> hub, StringBuilder sb, OAProperties props, int cntStop) {
            return generate(node, store, hub, sb, props, cntStop);
        }

        OAMatrix matrix(TreeNode node, Hub hub) {
            return createMatrix(node, hub);
        }

        String output(String value) {
            return getOutputText(value);
        }

        String value(Store store, String propertyName, int width, String fmt, OAProperties props, boolean useFormat) {
            return getValue(store, propertyName, width, fmt, props, useFormat);
        }

        Object property(Store store, String propertyName) {
            return getProperty(store, propertyName);
        }

        @Override
        protected String getIncludeText(String name) {
            if ("header".equals(name)) return "Store:<%= storeNumber %>";
            if ("nested".equals(name)) return "<%=include header%>";
            return super.getIncludeText(name);
        }
    }

    @BeforeEach
    void beforeEach() {
        OAGraphInternal og = (OAGraphInternal) OARuntime.graph(Register.class);
        OAObjectInternalService os = (OAObjectInternalService) og.objectsInternal();
        os.getOAObjectCacheService().removeAllObjects();
    }

    @Test
    void constructorsAndSetTemplateStoreRawTemplate() {
        OATemplate<Store> empty = new OATemplate<>();
        assertNull(empty.getTemplate());

        OATemplate<Store> template = new OATemplate<>("Store=<%= storeNumber %>");
        assertEquals("Store=<%= storeNumber %>", template.getTemplate());

        template.setTemplate("Name=<%= name %>");
        assertEquals("Name=<%= name %>", template.getTemplate());
    }

    @Test
    void processWithoutRootRendersLiteralAndInternalProperties() {
        OATemplate<Store> template = new OATemplate<>("Hello <%= $who %>");
        template.setProperty("who", "POS");

        assertEquals("Hello POS", template.process());
    }

    @Test
    void processWithObjectRendersSimpleAndNestedOaProperties() {
        Store store = fixtureStore();
        OATemplate<Store> template = new OATemplate<>("Store <%= storeNumber %> has <%= " + RAW_STORE_TO_ITEM_NAME_PATH + " %>");

        assertEquals("Store 100 has Brake Pads", template.process(store));
    }

    @Test
    void processWithTwoRootsSelectsRootThatSupportsSampledPath() {
        Store store = fixtureStore();
        Store other = new Store(2);
        other.setName("Other");
        OATemplate<Store> template = new OATemplate<>("<%= " + Store.P_StoreNumber + " %>:<%= " + Store.P_Name + " %>");

        assertEquals("100:Main Store", template.process(store, other));
    }

    @Test
    void processWithTwoRootsAndExternalPropertiesUsesExternalDollarValues() {
        Store store = fixtureStore();
        OAProperties props = new OAProperties();
        props.put("cashier", "Taylor");
        OATemplate<Store> template = new OATemplate<>("<%= storeNumber %>-<%= $cashier %>");

        assertEquals("100-Taylor", template.process(store, (Store) null, props));
    }

    @Test
    void processWithObjectAndExternalPropertiesAllowsInternalOverride() {
        Store store = fixtureStore();
        OAProperties props = new OAProperties();
        props.put("cashier", "External");
        OATemplate<Store> template = new OATemplate<>("<%= $cashier %>@<%= name %>");
        template.setProperty("$cashier", "Internal");

        assertEquals("Internal@Main Store", template.process(store, props));
    }

    @Test
    void processWithHubAndPropertiesSupportsForeachOverRootHub() {
        Hub<Store> stores = new Hub<>(Store.class);
        stores.add(fixtureStore());
        Store store2 = new Store(2);
        store2.setStoreNumber(200);
        store2.setName("Second");
        stores.add(store2);
        OAProperties props = new OAProperties();
        props.put("prefix", "S");
        OATemplate<Store> template = new OATemplate<>("<%=foreach%><%= $prefix %><%= storeNumber %>;<%=end%>");

        assertEquals("S100;S200;", template.process(stores, props));
        assertEquals("S100;S200;", template.process(stores));
    }

    @Test
    void stopProcessingCancelsNextProcess() {
        OATemplate<Store> template = new OATemplate<>("before");

        template.stopProcessing();

        assertEquals("cancelled", template.process(fixtureStore()));
    }

    @Test
    void processWithObjectHubAndPropertiesUsesObjectForPropertiesAndHubForForeach() {
        Store store = fixtureStore();
        Hub<Store> hub = new Hub<>(Store.class);
        hub.add(store);
        OAProperties props = new OAProperties();
        props.put("suffix", "!");
        OATemplate<Store> template = new OATemplate<>("<%= name %>:<%=foreach%><%= storeNumber %><%= $suffix %><%=end%>");

        assertEquals("Main Store:100!", template.process(store, hub, props));
    }

    @Test
    void processCoreOverloadSupportsObjectObjectHubAndProperties() {
        Store store = fixtureStore();
        Hub<Store> hub = new Hub<>(Store.class);
        hub.add(store);
        OAProperties props = new OAProperties();
        props.put("label", "Store");
        OATemplate<Store> template = new OATemplate<>("<%= $label %>=<%= storeNumber %>");

        assertEquals("Store=100", template.process(store, null, hub, props));
    }

    @Test
    void setPropertyRemovesValueWhenNull() {
        OATemplate<Store> template = new OATemplate<>("<%= $x %>");
        template.setProperty("x", "value");
        assertEquals("value", template.process());

        template.setProperty("$x", null);
        assertEquals("", template.process());
    }

    @Test
    void createTreeParsesNullAndEncodedDirectives() {
        ExposedTemplate template = new ExposedTemplate();

        assertNotNull(template.tree(null));
        template.setTemplate("&lt;%= $x %&gt;");
        template.setProperty("x", "decoded");

        assertEquals("decoded", template.process());
    }

    @Test
    void preprocessExpandsIncludesAndPreventsRecursion() {
        ExposedTemplate template = new ExposedTemplate();

        assertEquals("x Store:<%= storeNumber %> y", template.preprocessText("x <%=include header%> y"));
        assertTrue(template.preprocessText("<%=include header%>", new ArrayList<>()).contains("Store:"));
        assertTrue(template.preprocessText("<%=include header%>", new ArrayList<>()).contains("storeNumber"));

        ArrayList<String> includes = new ArrayList<>();
        includes.add("header");
        assertTrue(template.preprocessText("<%=include header%>", includes).contains("recursive include"));
    }

    @Test
    void defaultIncludeTextReportsMissingInclude() {
        ExposedTemplate template = new ExposedTemplate();

        assertTrue(template.preprocessText("<%=include missing%>").contains("no text for include missing"));
    }

    @Test
    void getHasParseErrorReportsMalformedBlocks() {
        OATemplate<Store> template = new OATemplate<>("<%=if name%>missing end");

        String out = template.process(fixtureStore());

        assertTrue(template.getHasParseError());
        assertTrue(out.contains("missing end tag"));
    }

    @Test
    void parseTokensClassifiesLiteralsPropertiesCommandsAndBlocks() {
        ExposedTemplate template = new ExposedTemplate();

        ArrayList<OATemplate.Token> tokens = template.tokens("a<%= name %><%=foreach registers%>b<%=end%><%=#counter registers%>");

        assertEquals("a", tokens.get(0).data);
        assertEquals(OATemplate.TagType.GetProp, tokens.get(1).tagType);
        assertEquals(OATemplate.TagType.ForEach, tokens.get(2).tagType);
        assertTrue(tokens.get(2).hasEndToken());
        assertEquals(OATemplate.TagType.End, tokens.get(4).tagType);
        assertEquals(OATemplate.TagType.Command, tokens.get(5).tagType);
    }

    @Test
    void generateCanRenderPreviouslyCreatedTree() {
        ExposedTemplate template = new ExposedTemplate();
        OATemplate.TreeNode node = template.tree("<%= name %>");
        StringBuilder sb = new StringBuilder();

        assertTrue(template.generateText(node, fixtureStore(), null, sb, null, -1));
        assertEquals("Main Store", sb.toString());
    }

    @Test
    void createMatrixReturnsMatrixForForeachWithNestedManyPaths() {
        ExposedTemplate template = new ExposedTemplate();
        OATemplate.TreeNode node = template.tree("<%=foreach " + Store.P_Registers + "%><%= "
                + Register.P_RegisterSessions + "." + RegisterSession.P_Invoices + " %><%=end%>");
        Hub<Store> stores = new Hub<>(Store.class);
        stores.add(fixtureStore());

        OAMatrix matrix = template.matrix(node.alChildren.get(0), stores);

        assertNotNull(matrix);
        assertTrue(matrix.getColumnCount() >= 1);
    }

    @Test
    void outputConversionAndHighlightAreAppliedToOutputText() {
        ExposedTemplate template = new ExposedTemplate();
        template.setOutputTextConversion("Brake", "Disc");
        assertEquals("Disc Pads", template.output("Brake Pads"));

        template.setOutputTextConversion(null, null);
        template.setHiliteOutputText("Pads");
        assertTrue(template.output("Brake Pads").contains("Pads"));
    }

    @Test
    void getValueHandlesDollarPropertiesFormattingWidthAndNulls() {
        ExposedTemplate template = new ExposedTemplate();
        Store store = fixtureStore();
        OAProperties props = new OAProperties();
        props.put("code", "ABCDE");

        assertEquals("", template.value(store, null, 0, null, props, false));
        assertEquals("ABC", template.value(store, "$code", 3, null, props, false));
        assertEquals("100", template.value(store, Store.P_StoreNumber, 0, null, props, false));
        assertEquals("Main", template.value(store, Store.P_Name, 4, null, props, false));
    }

    @Test
    void getPropertySupportsRawPConstantsTextUtilAndGeneratedPathHelpers() {
        ExposedTemplate template = new ExposedTemplate();
        Store store = fixtureStore();
        String pConstantPath = Store.P_Registers + "." + Register.P_RegisterSessions + "." + RegisterSession.P_Invoices
                + "." + Invoice.P_InvoiceBaskets + "." + InvoiceBasket.P_LineItems + "." + LineItem.P_Product + "."
                + Product.P_Item + "." + Item.P_Name;

        assertEquals("Brake Pads", template.property(store, RAW_STORE_TO_ITEM_NAME_PATH));
        assertEquals("Brake Pads", template.property(store, pConstantPath));
        assertEquals("Brake Pads", template.property(store, TEXT_UTIL_STORE_TO_ITEM_NAME_PATH));
        assertEquals("Brake Pads", template.property(store, PP_STORE_TO_ITEM_NAME_PATH));
    }

    private static Store fixtureStore() {
        Store store = new Store(1);
        store.setStoreNumber(100);
        store.setName("Main Store");

        Register register = new Register(2);
        register.setCode("R1");
        RegisterSession session = new RegisterSession(3);
        Invoice invoice = new Invoice(4);
        InvoiceBasket basket = new InvoiceBasket(5);
        LineItem line = new LineItem(6);
        line.setQuantity(2);
        line.setPriceEach(12.5);
        Product product = new Product(7);
        product.setSku("BP-1");
        Item item = new Item(8);
        item.setCode("BP1");
        item.setName("Brake Pads");

        store.getRegisters().add(register);
        register.getRegisterSessions().add(session);
        session.getInvoices().add(invoice);
        invoice.getInvoiceBaskets().add(basket);
        basket.getLineItems().add(line);
        line.setProduct(product);
        product.setItem(item);
        return store;
    }
}
