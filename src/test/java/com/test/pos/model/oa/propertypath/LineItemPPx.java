package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class LineItemPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public LineItemPPx(String name) {
        this(null, name);
    }

    public LineItemPPx(PPxInterface parent, String name) {
        String s = null;
        if (parent != null) {
            s = parent.toString();
        }
        if (s == null) s = "";
        if (name != null && name.length() > 0) {
            if (s.length() > 0 && name.charAt(0) != ':') s += ".";
            s += name;
        }
        pp = s;
    }

    public InvoiceBasketPPx invoiceBasket() {
        InvoiceBasketPPx ppx = new InvoiceBasketPPx(this, LineItem.P_InvoiceBasket);
        return ppx;
    }

    public LineItemTaxPPx lineItemTaxes() {
        LineItemTaxPPx ppx = new LineItemTaxPPx(this, LineItem.P_LineItemTaxes);
        return ppx;
    }

    public ProductPPx product() {
        ProductPPx ppx = new ProductPPx(this, LineItem.P_Product);
        return ppx;
    }

    public RefundLineItemPPx refundLineItems() {
        RefundLineItemPPx ppx = new RefundLineItemPPx(this, LineItem.P_RefundLineItems);
        return ppx;
    }

    public String id() {
        return pp + "." + LineItem.P_Id;
    }

    public String created() {
        return pp + "." + LineItem.P_Created;
    }

    public String quantity() {
        return pp + "." + LineItem.P_Quantity;
    }

    public String serialCode() {
        return pp + "." + LineItem.P_SerialCode;
    }

    public String priceEach() {
        return pp + "." + LineItem.P_PriceEach;
    }

    public String totalItemAmount() {
        return pp + "." + LineItem.P_TotalItemAmount;
    }

    public String totalTaxAmount() {
        return pp + "." + LineItem.P_TotalTaxAmount;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
