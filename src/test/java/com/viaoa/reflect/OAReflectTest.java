package com.viaoa.reflect;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Invoice;
import com.test.pos.model.oa.InvoiceBasket;
import com.test.pos.model.oa.Item;
import com.test.pos.model.oa.LineItem;
import com.test.pos.model.oa.Product;
import com.test.pos.model.oa.Register;
import com.test.pos.model.oa.RegisterSession;
import com.test.pos.model.oa.Store;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

class OAReflectTest {

    private static int nextId = 1000;

    @Test
    void getMethodWithNameFindsPublicMethodCaseInsensitive() {
        Method m = OAReflect.getMethod(Store.class, "getname");

        assertNotNull(m);
        assertEquals("getName", m.getName());
    }

    @Test
    void getMethodWithNameReturnsNullForInvalidInputs() {
        assertNull(OAReflect.getMethod(null, "getName"));
        assertNull(OAReflect.getMethod(Store.class, null));
        assertNull(OAReflect.getMethod(Store.class, ""));
        assertNull(OAReflect.getMethod(Store.class, "missingMethod"));
    }

    @Test
    void getMethodWithParamCountMatchesArity() {
        assertNotNull(OAReflect.getMethod(Store.class, "setName", 1));
        assertNull(OAReflect.getMethod(Store.class, "setName", 0));
        assertNull(OAReflect.getMethod(Store.class, null, 0));
    }

    @Test
    void getMethodWithArgsUsesRuntimeArgumentTypes() {
        Method stringSetter = OAReflect.getMethod(Store.class, "setName", new Object[] { "Main" });
        Method intSetter = OAReflect.getMethod(Store.class, "setStoreNumber", 1, new Object[] { Integer.valueOf(100) });

        assertNotNull(stringSetter);
        assertEquals(String.class, stringSetter.getParameterTypes()[0]);
        assertNull(intSetter, "Current argument matching is exact and does not match Integer to primitive int");
    }

    @Test
    void getMethodWithArgsAllowsNullArgumentForMatchingArity() {
        Method m = OAReflect.getMethod(Store.class, "setName", new Object[] { null });

        assertNotNull(m);
        assertEquals(String.class, m.getParameterTypes()[0]);
    }

    @Test
    void getMethodWithClassParamAllowsPrimitiveWrapperEquivalence() {
        Method m = OAReflect.getMethod(Store.class, "setStoreNumber", Integer.class);

        assertNotNull(m);
        assertEquals(int.class, m.getParameterTypes()[0]);
    }

    @Test
    void getMethodWithClassParamReturnsNullForInvalidInputsAndPrivateMethods() {
        assertNull(OAReflect.getMethod(null, "setName", String.class));
        assertNull(OAReflect.getMethod(Store.class, null, String.class));
        assertNull(OAReflect.getMethod(Store.class, "", String.class));
        assertNull(OAReflect.getMethod(Store.class, "setName", (Class) null));
        assertNull(OAReflect.getMethod(PrivateMethodBean.class, "setHidden", String.class));
    }

    @Test
    void getMethodsResolvesSimplePosPropertyPath() {
        Method[] ms = OAReflect.getMethods(Store.class, Store.P_Name);

        assertNotNull(ms);
        assertEquals(1, ms.length);
        assertEquals("getName", ms[0].getName());
    }

    @Test
    void getMethodsWithThrowFlagReturnsNullForInvalidPathWhenLenient() {
        assertNull(OAReflect.getMethods(Store.class, "missingProperty", false));
    }

    @Test
    void getMethodsWithThrowFlagThrowsForInvalidPathWhenStrict() {
        assertThrows(RuntimeException.class, () -> OAReflect.getMethods(Store.class, "missingProperty", true));
    }

    @Test
    void getMethodsWithSubstituteClassUsesProvidedTypeForOAObjectReturn() {
        Method[] ms = OAReflect.getMethods(SubstituteRoot.class, "object.name", Store.class, true);

        assertNotNull(ms);
        assertEquals(2, ms.length);
        assertEquals("getObject", ms[0].getName());
        assertEquals("getName", ms[1].getName());
    }

    @Test
    void getMethodsSupportsCastSegmentBeforeContinuingPath() {
        Method[] ms = OAReflect.getMethods(SubstituteRoot.class, "(com.test.pos.model.oa.Store)object.name", true);

        assertNotNull(ms);
        assertEquals(2, ms.length);
        assertEquals("getObject", ms[0].getName());
        assertEquals("getName", ms[1].getName());
    }

    @Test
    void convertParameterFromStringWithMethodUsesSingleParameterType() throws Exception {
        Method m = Store.class.getMethod("setStoreNumber", int.class);

        assertEquals(Integer.valueOf(123), OAReflect.convertParameterFromString(m, "123"));
    }

    @Test
    void convertParameterFromStringWithMethodReturnsNullForNonSingleParameterMethod() throws Exception {
        assertNull(OAReflect.convertParameterFromString(Store.class.getMethod("getName"), "x"));
        assertNull(OAReflect.convertParameterFromString(HelperBean.class.getMethod("setBoth", String.class, String.class), "x"));
    }

    @Test
    void convertParameterFromStringWithMethodAndFormatUsesFormat() throws Exception {
        Method m = LineItem.class.getMethod("setPriceEach", double.class);

        assertEquals(Double.valueOf(1234.5d), OAReflect.convertParameterFromString(m, "1,234.50", "#,##0.00"));
    }

    @Test
    void convertParameterFromStringWithMethodAndFormatReturnsNullForNonSingleParameterMethod() throws Exception {
        assertNull(OAReflect.convertParameterFromString(Store.class.getMethod("getName"), "x", null));
        assertNull(OAReflect.convertParameterFromString(HelperBean.class.getMethod("setBoth", String.class, String.class), "x", null));
    }

    @Test
    void convertParameterFromStringWithClassUsesConverter() {
        assertEquals(Integer.valueOf(42), OAReflect.convertParameterFromString(Integer.class, "42"));
    }

    @Test
    void convertParameterFromStringWithClassAndFormatUsesConverterFormat() {
        assertEquals(new BigDecimal("1234.5"), OAReflect.convertParameterFromString(BigDecimal.class, "1,234.50", "#,##0.00"));
    }

    @Test
    void getPropertyValueAsStringWithMethodArrayFormatsTerminalValue() {
        Store store = new Store();
        store.setStoreNumber(7);
        Method[] ms = OAReflect.getMethods(Store.class, Store.P_StoreNumber);

        assertEquals("0007", OAReflect.getPropertyValueAsString(store, ms, "0000"));
    }

    @Test
    void getPropertyValueAsStringWithMethodArrayReturnsNullForNullIntermediate() {
        Store store = new Store();
        Method[] ms = OAReflect.getMethods(Store.class, Store.P_Address + "." + com.test.pos.model.oa.Address.P_Name);

        store.setAddress(null);

        assertNull(OAReflect.getPropertyValueAsString(store, ms));
    }

    @Test
    void getPropertyValueAsStringWithMethodReturnsEmptyStringForNullDefault() throws Exception {
        Store store = new Store();
        Method m = Store.class.getMethod("getName");

        assertEquals("", OAReflect.getPropertyValueAsString(store, m));
    }

    @Test
    void getPropertyValueAsStringWithFormatAndNullValueUsesNullValue() throws Exception {
        Store store = new Store();
        Method m = Store.class.getMethod("getName");

        assertEquals("(none)", OAReflect.getPropertyValueAsString(store, m, null, "(none)"));
    }

    @Test
    void getPropertyValueAsStringWithNullObjectUsesNullValue() throws Exception {
        Method m = Store.class.getMethod("getName");

        assertEquals("(none)", OAReflect.getPropertyValueAsString(null, m, null, "(none)"));
    }

    @Test
    void executeMethodWithMethodArrayWalksPosGraph() {
        Fixture fx = fixture();
        Method[] ms = OAReflect.getMethods(Invoice.class,
                Invoice.P_InvoiceBaskets + "." + InvoiceBasket.P_LineItems + "." + LineItem.P_Product + "." + Product.P_Item + "."
                        + Item.P_Name);

        assertEquals("Battery", OAReflect.executeMethod(fx.invoice, ms));
    }

    @Test
    void executeMethodWithPathWalksPosGraph() {
        Fixture fx = fixture();
        String path = Invoice.P_InvoiceBaskets + "." + InvoiceBasket.P_LineItems + "." + LineItem.P_Product + "." + Product.P_Item + "."
                + Item.P_Code;

        assertEquals("BAT", OAReflect.executeMethod(fx.invoice, path));
    }

    @Test
    void executeMethodWithPathReturnsNullForNullOrBlankInputs() {
        Store store = new Store();

        assertNull(OAReflect.executeMethod(null, Store.P_Name));
        assertNull(OAReflect.executeMethod(store, (String) null));
        assertNull(OAReflect.executeMethod(store, ""));
    }

    @Test
    void executeMethodWithPathThrowsWhenPathCannotResolve() {
        Store store = new Store();

        assertThrows(RuntimeException.class, () -> OAReflect.executeMethod(store, "missingProperty"));
    }

    @Test
    void getPropertyValueWithMethodArrayReturnsStartObjectForNullOrEmptyArray() {
        Store store = new Store();

        assertSame(store, OAReflect.getPropertyValue(store, (Method[]) null));
        assertSame(store, OAReflect.getPropertyValue(store, new Method[0]));
    }

    @Test
    void getPropertyValueWithAmountStopsAfterRequestedDepth() {
        Fixture fx = fixture();
        Method[] ms = OAReflect.getMethods(Invoice.class,
                Invoice.P_InvoiceBaskets + "." + InvoiceBasket.P_LineItems + "." + LineItem.P_Product + "." + Product.P_Item);

        assertSame(fx.invoice, OAReflect.getPropertyValue(fx.invoice, ms, 0));
        assertInstanceOf(Hub.class, OAReflect.getPropertyValue(fx.invoice, ms, 1));
        assertSame(fx.item, OAReflect.getPropertyValue(fx.invoice, ms, 99));
    }

    @Test
    void getPropertyValueWithMethodInvokesGetterAndHonorsPrimitiveNull() throws Exception {
        LineItem lineItem = new LineItem();
        lineItem.setQuantity(321);
        Method m = LineItem.class.getMethod("getQuantity");

        assertEquals(321, OAReflect.getPropertyValue(lineItem, m));

        lineItem.setNull(LineItem.P_Quantity);
        assertNull(OAReflect.getPropertyValue(lineItem, m));
    }

    @Test
    void getPropertyValueWithMethodReturnsNullForNullObjectAndObjectForNullMethod() throws Exception {
        Store store = new Store();
        Method m = Store.class.getMethod("getName");

        assertNull(OAReflect.getPropertyValue(null, m));
        assertSame(store, OAReflect.getPropertyValue(store, (Method) null));
    }

    @Test
    void getPropertyValueWithMethodWrapsInvocationException() throws Exception {
        ThrowingBean bean = new ThrowingBean();
        Method m = ThrowingBean.class.getMethod("getValue");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> OAReflect.getPropertyValue(bean, m));

        assertTrue(ex.getMessage().contains("Error calling Method"));
        assertNotNull(ex.getCause());
    }

    @Test
    void setPropertyValueWithStringConvertsBeforeInvokingSetter() throws Exception {
        Store store = new Store();
        Method m = Store.class.getMethod("setStoreNumber", int.class);

        OAReflect.setPropertyValue(store, m, "456");

        assertEquals(456, store.getStoreNumber());
    }

    @Test
    void setPropertyValueWithStringAndFormatConvertsBeforeInvokingSetter() throws Exception {
        LineItem lineItem = new LineItem();
        Method m = LineItem.class.getMethod("setPriceEach", double.class);

        OAReflect.setPropertyValue(lineItem, m, "1,234.50", "#,##0.00");

        assertEquals(1234.5d, lineItem.getPriceEach());
    }

    @Test
    void setPropertyValueWithObjectInvokesSetter() throws Exception {
        Store store = new Store();
        Method m = Store.class.getMethod("setName", String.class);

        OAReflect.setPropertyValue(store, m, "Main Store");

        assertEquals("Main Store", store.getName());
    }

    @Test
    void setPropertyValueWithObjectNullForPrimitiveMarksOAObjectPrimitiveNull() throws Exception {
        LineItem lineItem = new LineItem();
        lineItem.setQuantity(5);
        Method m = LineItem.class.getMethod("setQuantity", int.class);

        OAReflect.setPropertyValue(lineItem, m, (Object) null);

        assertTrue(lineItem.isNull(LineItem.P_Quantity));
    }

    @Test
    void setPropertyValueWithObjectWrapsInvocationException() throws Exception {
        ThrowingBean bean = new ThrowingBean();
        Method m = ThrowingBean.class.getMethod("setValue", String.class);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> OAReflect.setPropertyValue(bean, m, "x"));

        assertNotNull(ex.getCause());
    }

    @Test
    void getClassReturnsReturnTypeOrSingleParameterType() throws Exception {
        assertEquals(String.class, OAReflect.getClass(Store.class.getMethod("getName")));
        assertEquals(String.class, OAReflect.getClass(Store.class.getMethod("setName", String.class)));
        assertNull(OAReflect.getClass(HelperBean.class.getMethod("clear")));
        assertNull(OAReflect.getClass(HelperBean.class.getMethod("setBoth", String.class, String.class)));
        assertNull(OAReflect.getClass(null));
    }

    @Test
    void isNumberRecognizesNumericTypes() {
        assertTrue(OAReflect.isNumber(int.class));
        assertTrue(OAReflect.isNumber(BigDecimal.class));
        assertFalse(OAReflect.isNumber(String.class));
        assertFalse(OAReflect.isNumber(null));
    }

    @Test
    void isIntegerRecognizesIntegerFamily() {
        assertTrue(OAReflect.isInteger(int.class));
        assertTrue(OAReflect.isInteger(BigInteger.class));
        assertFalse(OAReflect.isInteger(double.class));
        assertFalse(OAReflect.isInteger(null));
    }

    @Test
    void isFloatRecognizesFloatingFamily() {
        assertTrue(OAReflect.isFloat(double.class));
        assertTrue(OAReflect.isFloat(BigDecimal.class));
        assertFalse(OAReflect.isFloat(long.class));
        assertFalse(OAReflect.isFloat(null));
    }

    @Test
    void getClassWrapperMapsPrimitiveToWrapper() {
        assertEquals(Integer.class, OAReflect.getClassWrapper(int.class));
        assertEquals(Boolean.class, OAReflect.getClassWrapper(boolean.class));
        assertEquals(String.class, OAReflect.getClassWrapper(String.class));
    }

    @Test
    void getPrimitiveClassWrapperMapsPrimitiveToWrapper() {
        assertEquals(Integer.class, OAReflect.getPrimitiveClassWrapper(int.class));
        assertEquals(Boolean.class, OAReflect.getPrimitiveClassWrapper(boolean.class));
        assertEquals(String.class, OAReflect.getPrimitiveClassWrapper(String.class));
        assertNull(OAReflect.getPrimitiveClassWrapper(null));
    }

    @Test
    void isPrimitiveClassWrapperRecognizesWrapperClassesOnly() {
        assertTrue(OAReflect.isPrimitiveClassWrapper(Integer.class));
        assertTrue(OAReflect.isPrimitiveClassWrapper(Boolean.class));
        assertFalse(OAReflect.isPrimitiveClassWrapper(int.class));
        assertFalse(OAReflect.isPrimitiveClassWrapper(String.class));
        assertFalse(OAReflect.isPrimitiveClassWrapper(null));
    }

    @Test
    void isEqualEvenIfWrapperSupportsPrimitiveWrapperAndCurrentNumericCompatibility() {
        assertTrue(OAReflect.isEqualEvenIfWrapper(int.class, Integer.class));
        assertTrue(OAReflect.isEqualEvenIfWrapper(boolean.class, Boolean.class));
        assertTrue(OAReflect.isEqualEvenIfWrapper(Integer.class, Long.class));
        assertFalse(OAReflect.isEqualEvenIfWrapper(String.class, Object.class));
        assertFalse(OAReflect.isEqualEvenIfWrapper(null, Integer.class));
    }

    @Test
    void getPrimitiveClassWrapperObjectReturnsWrapperDefaultValues() {
        assertEquals(Integer.valueOf(0), OAReflect.getPrimitiveClassWrapperObject(int.class));
        assertEquals(Boolean.FALSE, OAReflect.getPrimitiveClassWrapperObject(Boolean.class));
        assertEquals(Character.valueOf((char) 0), OAReflect.getPrimitiveClassWrapperObject(char.class));
        assertNull(OAReflect.getPrimitiveClassWrapperObject(String.class));
        assertNull(OAReflect.getPrimitiveClassWrapperObject(null));
    }

    @Test
    void getEmptyPrimitiveReturnsCurrentPrimitiveDefaults() {
        assertEquals(Boolean.TRUE, OAReflect.getEmptyPrimitive(boolean.class));
        assertEquals(Integer.valueOf(0), OAReflect.getEmptyPrimitive(int.class));
        assertEquals(Long.valueOf(0L), OAReflect.getEmptyPrimitive(long.class));
        assertEquals(Short.valueOf((short) 0), OAReflect.getEmptyPrimitive(short.class));
        assertEquals(Double.valueOf(0.0d), OAReflect.getEmptyPrimitive(double.class));
        assertEquals(Float.valueOf(0.0f), OAReflect.getEmptyPrimitive(float.class));
        assertNull(OAReflect.getEmptyPrimitive(Integer.class));
    }

    @SuppressWarnings("deprecation")
    @Test
    void getClassesDelegatesToOAObjectClasses() throws Exception {
        String[] names = OAReflect.getClasses("com.test.pos.model.oa");

        assertTrue(Arrays.asList(names).contains("Store"));
        assertTrue(Arrays.asList(names).contains("Invoice"));
    }

    @Test
    void getOAObjectClassesThrowsWhenContextClassLoaderIsNull() {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(null);

            assertThrows(ClassNotFoundException.class, () -> OAReflect.getOAObjectClasses("com.test.pos.model.oa"));
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    @Test
    void getOAObjectClassesReturnsTopLevelClassesInPackage() throws Exception {
        String[] names = OAReflect.getOAObjectClasses("com.test.pos.model.oa");

        assertTrue(Arrays.asList(names).contains("Store"));
        assertTrue(Arrays.asList(names).contains("LineItem"));
        assertFalse(Arrays.stream(names).anyMatch(s -> s.contains("$")));
    }

    @Test
    void getOAObjectClassesReturnsEmptyArrayForMissingPackage() throws Exception {
        String[] names = OAReflect.getOAObjectClasses("com.test.pos.model.oa.missing");

        assertNotNull(names);
        assertEquals(0, names.length);
    }

    @Test
    void getClassPathReturnsClasspathRootForNormalClass() {
        String path = OAReflect.getClassPath(Store.class);

        assertNotNull(path);
        assertFalse(path.isBlank());
    }

    @Test
    void getClassPathReturnsNullForNullClass() {
        assertNull(OAReflect.getClassPath(null));
    }

    private static Fixture fixture() {
        int id = nextId++;
        Store store = new Store(id);
        store.setStoreNumber(id);
        store.setName("Test Store");

        Register register = new Register(id + 1000);
        register.setCode("R1");
        store.getRegisters().add(register);
        store.getRegisters().setAO(register);

        RegisterSession session = new RegisterSession(id + 2000);
        register.getRegisterSessions().add(session);
        register.getRegisterSessions().setAO(session);

        Invoice invoice = new Invoice(id + 3000);
        session.getInvoices().add(invoice);
        session.getInvoices().setAO(invoice);

        InvoiceBasket basket = new InvoiceBasket(id + 4000);
        invoice.getInvoiceBaskets().add(basket);
        invoice.getInvoiceBaskets().setAO(basket);

        Item item = new Item(id + 5000);
        item.setCode("BAT");
        item.setName("Battery");

        Product product = new Product(id + 6000);
        product.setSku("BAT-001");
        item.getProducts().add(product);
        item.getProducts().setAO(product);

        LineItem lineItem = new LineItem(id + 7000);
        lineItem.setQuantity(2);
        lineItem.setPriceEach(12.50);
        lineItem.setProduct(product);
        basket.getLineItems().add(lineItem);
        basket.getLineItems().setAO(lineItem);

        return new Fixture(store, invoice, item);
    }

    public static class SubstituteRoot {
        private OAObject object;

        public OAObject getObject() {
            return object;
        }

        public void setObject(OAObject object) {
            this.object = object;
        }
    }

    public static class HelperBean {
        public void clear() {
        }

        public void setBoth(String a, String b) {
        }
    }

    public static class ThrowingBean {
        public String getValue() {
            throw new IllegalStateException("getter failed");
        }

        public void setValue(String value) {
            throw new IllegalArgumentException("setter failed");
        }
    }

    public static class PrivateMethodBean {
        @SuppressWarnings("unused")
        private void setHidden(String value) {
        }
    }

    private static class Fixture {
        final Store store;
        final Invoice invoice;
        final Item item;

        Fixture(Store store, Invoice invoice, Item item) {
            this.store = store;
            this.invoice = invoice;
            this.item = item;
        }
    }
}
