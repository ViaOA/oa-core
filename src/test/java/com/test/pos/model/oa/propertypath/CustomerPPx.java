package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class CustomerPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public CustomerPPx(String name) {
        this(null, name);
    }

    public CustomerPPx(PPxInterface parent, String name) {
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

    public AddressPPx addresses() {
        AddressPPx ppx = new AddressPPx(this, Customer.P_Addresses);
        return ppx;
    }

    public CustomerCreditPPx customerCredit() {
        CustomerCreditPPx ppx = new CustomerCreditPPx(this, Customer.P_CustomerCredit);
        return ppx;
    }

    public GaragePPx garage() {
        GaragePPx ppx = new GaragePPx(this, Customer.P_Garage);
        return ppx;
    }

    public InvoicePPx invoices() {
        InvoicePPx ppx = new InvoicePPx(this, Customer.P_Invoices);
        return ppx;
    }

    public OnlineOrderPPx onlineOrders() {
        OnlineOrderPPx ppx = new OnlineOrderPPx(this, Customer.P_OnlineOrders);
        return ppx;
    }

    public QuotePPx quotes() {
        QuotePPx ppx = new QuotePPx(this, Customer.P_Quotes);
        return ppx;
    }

    public String id() {
        return pp + "." + Customer.P_Id;
    }

    public String created() {
        return pp + "." + Customer.P_Created;
    }

    public String name() {
        return pp + "." + Customer.P_Name;
    }

    public String type() {
        return pp + "." + Customer.P_Type;
    }

    public String inputMask() {
        return pp + "." + Customer.P_InputMask;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
