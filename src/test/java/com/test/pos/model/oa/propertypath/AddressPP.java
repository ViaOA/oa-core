package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class AddressPP {
    private static CustomerPPx customer;
    private static InvoiceShipToPPx invoiceShipTos;
    private static StorePPx store;
     

    public static CustomerPPx customer() {
        if (customer == null) customer = new CustomerPPx(Address.P_Customer);
        return customer;
    }

    public static InvoiceShipToPPx invoiceShipTos() {
        if (invoiceShipTos == null) invoiceShipTos = new InvoiceShipToPPx(Address.P_InvoiceShipTos);
        return invoiceShipTos;
    }

    public static StorePPx store() {
        if (store == null) store = new StorePPx(Address.P_Store);
        return store;
    }

    public static String id() {
        String s = Address.P_Id;
        return s;
    }

    public static String created() {
        String s = Address.P_Created;
        return s;
    }

    public static String name() {
        String s = Address.P_Name;
        return s;
    }

    public static String address1() {
        String s = Address.P_Address1;
        return s;
    }

    public static String address2() {
        String s = Address.P_Address2;
        return s;
    }

    public static String city() {
        String s = Address.P_City;
        return s;
    }

    public static String state() {
        String s = Address.P_State;
        return s;
    }

    public static String zip() {
        String s = Address.P_Zip;
        return s;
    }

    public static String zip4() {
        String s = Address.P_Zip4;
        return s;
    }

    public static String type() {
        String s = Address.P_Type;
        return s;
    }

    public static String gis() {
        String s = Address.P_GIS;
        return s;
    }

    public static String timezone() {
        String s = Address.P_Timezone;
        return s;
    }

    public static String calcCityStateZip() {
        String s = Address.P_CalcCityStateZip;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
