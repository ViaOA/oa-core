// Copied from OATemplate project by OABuilder 07/01/16 07:41 AM
package com.test.pos.model.oa.cs;

import java.io.IOException;

import com.test.pos.model.oa.*;
import com.test.pos.model.oa.propertypath.*;
import com.viaoa.annotation.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;

/**
 * Root Object that is automatically updated between the Server and Clients. ServerController will do the selects for these objects. Model
 * will share these hubs after the application is started.
 */
@OAClass(useDataSource = false, displayProperty = "Id")
public class ClientRoot extends OAObject {
	private static final long serialVersionUID = 1L;

	public static final String PROPERTY_Id = "Id";
	public static final String P_Id = "Id";
	public static final String PROPERTY_ConnectionInfo = "ConnectionInfo";
	public static final String P_ConnectionInfo = "ConnectionInfo";
	/*$$Start: ClientRoot1 $$*/
    // Hubs for Client UI
    public static final String P_SearchInvoices = "SearchInvoices";
    public static final String P_SearchCustomers = "SearchCustomers";
    public static final String P_SearchItems = "SearchItems";
    public static final String P_SearchBankDeposits1 = "SearchBankDeposits1";
/*$$End: ClientRoot1 $$*/

	protected int id;

	// Hub
	/*$$Start: ClientRoot2 $$*/
    // Hubs for Client UI
    protected transient Hub<Invoice> hubSearchInvoices;
    protected transient Hub<Customer> hubSearchCustomers;
    protected transient Hub<Item> hubSearchItems;
    protected transient Hub<BankDeposit> hubSearchBankDeposits1;
/*$$End: ClientRoot2 $$*/

	@OAProperty(displayName = "Id")
	@OAId
	public int getId() {
		return id;
	}

	public void setId(int id) {
		int old = this.id;
		this.id = id;
		firePropertyChange("id", old, id);
	}

	/*$$Start: ClientRoot3 $$*/
    // Hubs for Client UI
    @OAMany(toClass = Invoice.class, cascadeSave = true)
    public Hub<Invoice> getSearchInvoices() {
        if (hubSearchInvoices == null) {
            hubSearchInvoices = (Hub<Invoice>) super.getHub(P_SearchInvoices);
        }
        return hubSearchInvoices;
    }
    @OAMany(toClass = Customer.class, cascadeSave = true)
    public Hub<Customer> getSearchCustomers() {
        if (hubSearchCustomers == null) {
            hubSearchCustomers = (Hub<Customer>) super.getHub(P_SearchCustomers);
        }
        return hubSearchCustomers;
    }
    @OAMany(toClass = Item.class, cascadeSave = true)
    public Hub<Item> getSearchItems() {
        if (hubSearchItems == null) {
            hubSearchItems = (Hub<Item>) super.getHub(P_SearchItems);
        }
        return hubSearchItems;
    }
    @OAMany(toClass = BankDeposit.class, cascadeSave = true)
    public Hub<BankDeposit> getSearchBankDeposits1() {
        if (hubSearchBankDeposits1 == null) {
            hubSearchBankDeposits1 = (Hub<BankDeposit>) super.getHub(P_SearchBankDeposits1);
        }
        return hubSearchBankDeposits1;
    }
/*$$End: ClientRoot3 $$*/

	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		stream.defaultWriteObject();
	}

    
}
