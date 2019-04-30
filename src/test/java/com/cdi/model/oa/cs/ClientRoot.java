// Copied from OATemplate project by OABuilder 07/01/16 07:41 AM
package com.cdi.model.oa.cs;

import com.viaoa.object.*;
import com.cdi.model.oa.*;
import com.viaoa.annotation.*;
import com.viaoa.hub.*;


/**
 * Root Object that is automatically updated between the Server and Clients.
 * ServerController will do the selects for these objects.
 * Model will share these hubs after the application is started.
 * */
@OAClass(
    useDataSource = false,
    displayProperty = "Id"
)
public class ClientRoot extends OAObject {
    private static final long serialVersionUID = 1L;

    public static final String PROPERTY_Id = "Id";
    public static final String PROPERTY_ConnectionInfo = "ConnectionInfo";
    /*$$Start: ClientRoot1 $$*/
    public static final String P_SearchSalesOrders = "SearchSalesOrders";
    public static final String P_NewSalesOrders = "NewSalesOrders";
    public static final String P_SearchOrders1 = "SearchOrders1";
    public static final String P_AllDeliveries = "AllDeliveries";
    public static final String P_SearchDeliveries = "SearchDeliveries";
    public static final String P_SearchTrucks1 = "SearchTrucks1";
    public static final String P_SearchItems = "SearchItems";
    public static final String P_SearchMolds = "SearchMolds";
    public static final String P_SearchCustomers = "SearchCustomers";
    public static final String P_SearchContacts = "SearchContacts";
    public static final String P_SearchCustomers1 = "SearchCustomers1";
    public static final String P_SearchImageStores = "SearchImageStores";
    public static final String P_SearchItems1 = "SearchItems1";
    public static final String P_SearchMolds1 = "SearchMolds1";
    public static final String P_SearchOrders = "SearchOrders";
    public static final String P_SearchOrderItems = "SearchOrderItems";
    public static final String P_SearchSalesCustomers = "SearchSalesCustomers";
    public static final String P_SearchSalesOrders1 = "SearchSalesOrders1";
    public static final String P_SearchTrucks = "SearchTrucks";
    public static final String P_SearchWorkOrders = "SearchWorkOrders";
    /*$$End: ClientRoot1 $$*/

    protected int id;	

    // Hub
    /*$$Start: ClientRoot2 $$*/
    protected transient Hub<SalesOrder> hubSearchSalesOrders;
    protected transient Hub<SalesOrder> hubNewSalesOrders;
    protected transient Hub<Order> hubSearchOrders1;
    protected transient Hub<Delivery> hubAllDeliveries;
    protected transient Hub<Delivery> hubSearchDeliveries;
    protected transient Hub<Truck> hubSearchTrucks1;
    protected transient Hub<Item> hubSearchItems;
    protected transient Hub<Mold> hubSearchMolds;
    protected transient Hub<Customer> hubSearchCustomers;
    protected transient Hub<Contact> hubSearchContacts;
    protected transient Hub<Customer> hubSearchCustomers1;
    protected transient Hub<ImageStore> hubSearchImageStores;
    protected transient Hub<Item> hubSearchItems1;
    protected transient Hub<Mold> hubSearchMolds1;
    protected transient Hub<Order> hubSearchOrders;
    protected transient Hub<OrderItem> hubSearchOrderItems;
    protected transient Hub<SalesCustomer> hubSearchSalesCustomers;
    protected transient Hub<SalesOrder> hubSearchSalesOrders1;
    protected transient Hub<Truck> hubSearchTrucks;
    protected transient Hub<WorkOrder> hubSearchWorkOrders;
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
    @OAMany(toClass = SalesOrder.class, cascadeSave = true)
    public Hub<SalesOrder> getSearchSalesOrders() {
        if (hubSearchSalesOrders == null) {
            hubSearchSalesOrders = (Hub<SalesOrder>) super.getHub(P_SearchSalesOrders);
        }
        return hubSearchSalesOrders;
    }
    @OAMany(toClass = SalesOrder.class, cascadeSave = true)
    public Hub<SalesOrder> getNewSalesOrders() {
        if (hubNewSalesOrders == null) {
            hubNewSalesOrders = (Hub<SalesOrder>) super.getHub(P_NewSalesOrders);
        }
        return hubNewSalesOrders;
    }
    @OAMany(toClass = Order.class, cascadeSave = true, isProcessed = true)
    public Hub<Order> getSearchOrders1() {
        if (hubSearchOrders1 == null) {
            hubSearchOrders1 = (Hub<Order>) super.getHub(P_SearchOrders1);
        }
        return hubSearchOrders1;
    }
    @OAMany(toClass = Delivery.class, cascadeSave = true, isProcessed = true)
    public Hub<Delivery> getAllDeliveries() {
        if (hubAllDeliveries == null) {
            hubAllDeliveries = (Hub<Delivery>) super.getHub(P_AllDeliveries);
        }
        return hubAllDeliveries;
    }
    @OAMany(toClass = Delivery.class, cascadeSave = true, isProcessed = true)
    public Hub<Delivery> getSearchDeliveries() {
        if (hubSearchDeliveries == null) {
            hubSearchDeliveries = (Hub<Delivery>) super.getHub(P_SearchDeliveries);
        }
        return hubSearchDeliveries;
    }
    @OAMany(toClass = Truck.class, cascadeSave = true)
    public Hub<Truck> getSearchTrucks1() {
        if (hubSearchTrucks1 == null) {
            hubSearchTrucks1 = (Hub<Truck>) super.getHub(P_SearchTrucks1);
        }
        return hubSearchTrucks1;
    }
    @OAMany(toClass = Item.class, cascadeSave = true)
    public Hub<Item> getSearchItems() {
        if (hubSearchItems == null) {
            hubSearchItems = (Hub<Item>) super.getHub(P_SearchItems);
        }
        return hubSearchItems;
    }
    @OAMany(toClass = Mold.class, cascadeSave = true)
    public Hub<Mold> getSearchMolds() {
        if (hubSearchMolds == null) {
            hubSearchMolds = (Hub<Mold>) super.getHub(P_SearchMolds);
        }
        return hubSearchMolds;
    }
    @OAMany(toClass = Customer.class, cascadeSave = true, isProcessed = true)
    public Hub<Customer> getSearchCustomers() {
        if (hubSearchCustomers == null) {
            hubSearchCustomers = (Hub<Customer>) super.getHub(P_SearchCustomers);
        }
        return hubSearchCustomers;
    }
    @OAMany(toClass = Contact.class, cascadeSave = true)
    public Hub<Contact> getSearchContacts() {
        if (hubSearchContacts == null) {
            hubSearchContacts = (Hub<Contact>) super.getHub(P_SearchContacts);
        }
        return hubSearchContacts;
    }
    @OAMany(toClass = Customer.class, cascadeSave = true, isProcessed = true)
    public Hub<Customer> getSearchCustomers1() {
        if (hubSearchCustomers1 == null) {
            hubSearchCustomers1 = (Hub<Customer>) super.getHub(P_SearchCustomers1);
        }
        return hubSearchCustomers1;
    }
    @OAMany(toClass = ImageStore.class, cascadeSave = true)
    public Hub<ImageStore> getSearchImageStores() {
        if (hubSearchImageStores == null) {
            hubSearchImageStores = (Hub<ImageStore>) super.getHub(P_SearchImageStores);
        }
        return hubSearchImageStores;
    }
    @OAMany(toClass = Item.class, cascadeSave = true)
    public Hub<Item> getSearchItems1() {
        if (hubSearchItems1 == null) {
            hubSearchItems1 = (Hub<Item>) super.getHub(P_SearchItems1);
        }
        return hubSearchItems1;
    }
    @OAMany(toClass = Mold.class, cascadeSave = true)
    public Hub<Mold> getSearchMolds1() {
        if (hubSearchMolds1 == null) {
            hubSearchMolds1 = (Hub<Mold>) super.getHub(P_SearchMolds1);
        }
        return hubSearchMolds1;
    }
    @OAMany(toClass = Order.class, cascadeSave = true, isProcessed = true)
    public Hub<Order> getSearchOrders() {
        if (hubSearchOrders == null) {
            hubSearchOrders = (Hub<Order>) super.getHub(P_SearchOrders);
        }
        return hubSearchOrders;
    }
    @OAMany(toClass = OrderItem.class, cascadeSave = true)
    public Hub<OrderItem> getSearchOrderItems() {
        if (hubSearchOrderItems == null) {
            hubSearchOrderItems = (Hub<OrderItem>) super.getHub(P_SearchOrderItems);
        }
        return hubSearchOrderItems;
    }
    @OAMany(toClass = SalesCustomer.class, cascadeSave = true)
    public Hub<SalesCustomer> getSearchSalesCustomers() {
        if (hubSearchSalesCustomers == null) {
            hubSearchSalesCustomers = (Hub<SalesCustomer>) super.getHub(P_SearchSalesCustomers);
        }
        return hubSearchSalesCustomers;
    }
    @OAMany(toClass = SalesOrder.class, cascadeSave = true)
    public Hub<SalesOrder> getSearchSalesOrders1() {
        if (hubSearchSalesOrders1 == null) {
            hubSearchSalesOrders1 = (Hub<SalesOrder>) super.getHub(P_SearchSalesOrders1);
        }
        return hubSearchSalesOrders1;
    }
    @OAMany(toClass = Truck.class, cascadeSave = true)
    public Hub<Truck> getSearchTrucks() {
        if (hubSearchTrucks == null) {
            hubSearchTrucks = (Hub<Truck>) super.getHub(P_SearchTrucks);
        }
        return hubSearchTrucks;
    }
    @OAMany(toClass = WorkOrder.class, cascadeSave = true, isProcessed = true)
    public Hub<WorkOrder> getSearchWorkOrders() {
        if (hubSearchWorkOrders == null) {
            hubSearchWorkOrders = (Hub<WorkOrder>) super.getHub(P_SearchWorkOrders);
        }
        return hubSearchWorkOrders;
    }
    /*$$End: ClientRoot3 $$*/

	
}
