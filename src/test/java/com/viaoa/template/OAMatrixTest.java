package com.viaoa.template;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
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
import com.viaoa.graph.OAGraph;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;
import com.viaoa.text.OATextUtil;

class OAMatrixTest {

    private static final String STORE_TO_LINE_ITEMS_PATH = Store.P_Registers + "." + Register.P_RegisterSessions + "."
            + RegisterSession.P_Invoices + "." + Invoice.P_InvoiceBaskets + "." + InvoiceBasket.P_LineItems;
    private static final String TEXT_UTIL_STORE_TO_LINE_ITEMS_PATH = OATextUtil.createPropertyPath(Store.P_Registers,
            Register.P_RegisterSessions, RegisterSession.P_Invoices, Invoice.P_InvoiceBaskets,
            InvoiceBasket.P_LineItems);
    private static final String PP_STORE_TO_LINE_ITEMS_PATH = StorePP.registers().registerSessions().invoices()
            .invoiceBaskets().lineItems().pp();

    private static class ExposedMatrix extends OAMatrix {
        Object objectAt(int row, Column column, List<Object[]> rows, boolean real) {
            return getObject(row, column, rows, real);
        }

        boolean hasChild(Column column, int row, List<Object[]> rows) {
            return hasChildRow(column, row, rows);
        }

        int populate(Column column, int row, OAObject object, List<Object[]> rows) {
            return _populateGridRows(column, row, object, rows);
        }
    }

    @BeforeEach
    void beforeEach() {
        OAGraph og = OARuntime.graph(Register.class);
    }
    @AfterEach
    void afterEach() {
        OARuntime.graph(Register.class).close();
    }

    @Test
    void columnAccessorsExposePropertyPathAndParentColumn() {
        Hub<Store> stores = hub(fixtureStore());
        OAMatrix matrix = new OAMatrix();

        OAMatrix.Column root = matrix.addColumn(stores);
        OAMatrix.Column detail = matrix.addDetailColumn(root, Store.P_Registers);

        assertNull(root.getPropertyPath());
        assertNull(root.getFromColumn());
        assertEquals(Store.P_Registers, detail.getPropertyPath());
        assertSame(root, detail.getFromColumn());
    }

    @Test
    void addColumnRejectsNullHubAndCreatesRootColumn() {
        OAMatrix matrix = new OAMatrix();
        Hub<Store> stores = hub(fixtureStore());

        assertThrows(IllegalArgumentException.class, () -> matrix.addColumn(null));
        OAMatrix.Column root = matrix.addColumn(stores);

        assertNotNull(root);
        assertSame(root, matrix.getColumn(0));
        assertEquals(1, matrix.getColumnCount());
    }

    @Test
    void addDetailColumnAcceptsRawPConstantTextUtilAndGeneratedPaths() {
        Hub<Store> stores = hub(fixtureStore());

        OAMatrix raw = new OAMatrix();
        OAMatrix.Column rawRoot = raw.addColumn(stores);
        assertEquals(STORE_TO_LINE_ITEMS_PATH, raw.addDetailColumn(rawRoot, STORE_TO_LINE_ITEMS_PATH).getPropertyPath());

        OAMatrix textUtil = new OAMatrix();
        OAMatrix.Column textRoot = textUtil.addColumn(stores);
        assertEquals(TEXT_UTIL_STORE_TO_LINE_ITEMS_PATH,
                textUtil.addDetailColumn(textRoot, TEXT_UTIL_STORE_TO_LINE_ITEMS_PATH).getPropertyPath());

        OAMatrix pp = new OAMatrix();
        OAMatrix.Column ppRoot = pp.addColumn(stores);
        assertEquals(PP_STORE_TO_LINE_ITEMS_PATH, pp.addDetailColumn(ppRoot, PP_STORE_TO_LINE_ITEMS_PATH).getPropertyPath());
    }

    @Test
    void addDetailColumnReturnsNullForMissingInputsAndRejectsScalarPath() {
        OAMatrix matrix = new OAMatrix();
        OAMatrix.Column root = matrix.addColumn(hub(fixtureStore()));

        assertNull(matrix.addDetailColumn(null, Store.P_Registers));
        assertNull(matrix.addDetailColumn(root, null));
        assertNull(matrix.addDetailColumn(root, ""));
        assertThrows(RuntimeException.class, () -> matrix.addDetailColumn(root, Store.P_Name));
    }

    @Test
    void addGroupByColumnReturnsNullForMissingLeftColumnAndRejectsInvalidPath() {
        OAMatrix matrix = new OAMatrix();
        OAMatrix.Column root = matrix.addColumn(hub(fixtureStore()));

        assertNull(matrix.addGroupByColumn(null, new Hub<>(Register.class), Store.P_Registers, Register.P_Store));
        assertThrows(RuntimeException.class,
                () -> matrix.addGroupByColumn(root, new Hub<>(Register.class), Store.P_Name, Register.P_Store));
    }

    @Test
    void getRootColumnReturnsTopLevelColumn() {
        OAMatrix matrix = new OAMatrix();
        OAMatrix.Column root = matrix.addColumn(hub(fixtureStore()));
        OAMatrix.Column registers = matrix.addDetailColumn(root, Store.P_Registers);
        OAMatrix.Column sessions = matrix.addDetailColumn(registers, Register.P_RegisterSessions);

        assertNull(matrix.getRootColumn(null));
        assertSame(root, matrix.getRootColumn(root));
        assertSame(root, matrix.getRootColumn(sessions));
    }

    @Test
    void getPropertyPathFromRootPrependsParentPaths() {
        OAMatrix matrix = new OAMatrix();
        OAMatrix.Column root = matrix.addColumn(hub(fixtureStore()));
        OAMatrix.Column registers = matrix.addDetailColumn(root, Store.P_Registers);
        OAMatrix.Column sessions = matrix.addDetailColumn(registers, Register.P_RegisterSessions);

        assertEquals(Store.P_Registers + "." + Register.P_RegisterSessions + "." + RegisterSession.P_Invoices,
                matrix.getPropertyPathFromRoot(sessions, RegisterSession.P_Invoices));
        assertEquals(Store.P_Registers, matrix.getPropertyPathFromRoot(null, Store.P_Registers));
    }

    @Test
    void verifyLinkPropertyUsesOaMetadata() {
        assertTrue(OAMatrix.verifyLinkProperty(Store.class, Store.P_Registers));
        assertTrue(OAMatrix.verifyLinkProperty(Store.class, STORE_TO_LINE_ITEMS_PATH));
        assertFalse(OAMatrix.verifyLinkProperty(Store.class, Store.P_Name));
    }

    @Test
    void getColumnAndCountsExposeColumnState() {
        OAMatrix matrix = new OAMatrix();
        OAMatrix.Column root = matrix.addColumn(hub(fixtureStore()));

        assertNull(matrix.getColumn(-1));
        assertNull(matrix.getColumn(1));
        assertEquals(1, matrix.getColumnCount());
        assertSame(root, matrix.getColumns().get(0));
    }

    @Test
    void getGridIsLazyAndClearGridForcesRebuild() {
        OAMatrix matrix = new OAMatrix();
        matrix.addColumn(hub(fixtureStore()));

        List<Object[]> grid1 = matrix.getGrid();
        List<Object[]> grid2 = matrix.getGrid();
        matrix.clearGrid();
        List<Object[]> grid3 = matrix.getGrid();

        assertSame(grid1, grid2);
        assertNotSame(grid1, grid3);
    }

    @Test
    void getObjectAndGetRealObjectReturnNullForOutOfRangeCells() {
        OAMatrix matrix = new OAMatrix();
        matrix.addColumn(hub(fixtureStore()));

        assertNull(matrix.getObject(-1, 0));
        assertNull(matrix.getObject(0, -1));
        assertNull(matrix.getObject(99, 0));
        assertNull(matrix.getObject(0, 99));
        assertNull(matrix.getRealObject(-1, 0));
        assertNull(matrix.getRealObject(0, -1));
        assertNull(matrix.getRealObject(99, 0));
        assertNull(matrix.getRealObject(0, 99));
    }

    @Test
    void protectedGetObjectAndHasChildHandleMissingInputs() {
        ExposedMatrix matrix = new ExposedMatrix();
        OAMatrix.Column root = matrix.addColumn(hub(fixtureStore()));
        List<Object[]> rows = matrix.getGrid();

        assertNull(matrix.objectAt(0, null, rows, false));
        assertNull(matrix.objectAt(0, root, null, false));
        assertFalse(matrix.hasChild(null, 0, rows));
        assertFalse(matrix.hasChild(root, 99, rows));
    }

    @Test
    void getRowCountBeforeAndAfterGridCreationReflectsCachedGridState() {
        OAMatrix matrix = new OAMatrix();
        OAMatrix.Column root = matrix.addColumn(hub(fixtureStore()));

        assertEquals(0, matrix.getRowCount());
        assertEquals(1, matrix.getRowCount(root));
        matrix.getGrid();
        assertEquals(1, matrix.getRowCount());
    }

    @Test
    void createGridBuildsRootAndDetailRowsFromOaposGraph() {
        Store store = fixtureStore();
        OAMatrix matrix = new OAMatrix();
        OAMatrix.Column root = matrix.addColumn(hub(store));
        OAMatrix.Column registers = matrix.addDetailColumn(root, Store.P_Registers);
        OAMatrix.Column lineItems = matrix.addDetailColumn(root, STORE_TO_LINE_ITEMS_PATH);

        List<Object[]> grid = matrix.createGrid();

        assertEquals(1, grid.size());
        assertSame(store, matrix.getObject(0, 0));
        assertSame(store.getRegisters().getAt(0), matrix.getObject(0, 1));
        assertSame(store.getRegisters().getAt(0).getRegisterSessions().getAt(0).getInvoices().getAt(0)
                .getInvoiceBaskets().getAt(0).getLineItems().getAt(0), matrix.getObject(0, 2));
        assertSame(root, lineItems.getFromColumn());
        assertSame(registers, matrix.getColumn(1));
    }

    @Test
    void populateGridRowsCanBeCalledForRootColumn() {
        ExposedMatrix matrix = new ExposedMatrix();
        Store store = fixtureStore();
        OAMatrix.Column root = matrix.addColumn(hub(store));
        List<Object[]> rows = new java.util.ArrayList<>();

        int next = matrix.populate(root, 0, store, rows);

        assertEquals(1, next);
        assertSame(store, rows.get(0)[0]);
    }

    @Test
    void getRowCountForObjectReturnsAtLeastOneForLeafObject() {
        OAMatrix matrix = new OAMatrix();
        OAMatrix.Column root = matrix.addColumn(hub(fixtureStore()));

        assertEquals(1, matrix.getRowCount(root, fixtureStore()));
    }

    private static Hub<Store> hub(Store... stores) {
        Hub<Store> hub = new Hub<>(Store.class);
        for (Store store : stores) {
            hub.add(store);
        }
        return hub;
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
