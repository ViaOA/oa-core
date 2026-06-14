package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class LineItemPP {
    private static InvoiceBasketPPx invoiceBasket;
    private static LineItemTaxPPx lineItemTaxes;
    private static ProductPPx product;
    private static RefundLineItemPPx refundLineItems;
     

    public static InvoiceBasketPPx invoiceBasket() {
        if (invoiceBasket == null) invoiceBasket = new InvoiceBasketPPx(LineItem.P_InvoiceBasket);
        return invoiceBasket;
    }

    public static LineItemTaxPPx lineItemTaxes() {
        if (lineItemTaxes == null) lineItemTaxes = new LineItemTaxPPx(LineItem.P_LineItemTaxes);
        return lineItemTaxes;
    }

    public static ProductPPx product() {
        if (product == null) product = new ProductPPx(LineItem.P_Product);
        return product;
    }

    public static RefundLineItemPPx refundLineItems() {
        if (refundLineItems == null) refundLineItems = new RefundLineItemPPx(LineItem.P_RefundLineItems);
        return refundLineItems;
    }

    public static String id() {
        String s = LineItem.P_Id;
        return s;
    }

    public static String created() {
        String s = LineItem.P_Created;
        return s;
    }

    public static String quantity() {
        String s = LineItem.P_Quantity;
        return s;
    }

    public static String serialCode() {
        String s = LineItem.P_SerialCode;
        return s;
    }

    public static String priceEach() {
        String s = LineItem.P_PriceEach;
        return s;
    }

    public static String totalItemAmount() {
        String s = LineItem.P_TotalItemAmount;
        return s;
    }

    public static String totalTaxAmount() {
        String s = LineItem.P_TotalTaxAmount;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
