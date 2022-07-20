// Copied from OATemplate project by OABuilder 07/01/16 07:41 AM
package com.corptostore.model.oa.cs;

import com.corptostore.model.oa.*;
import com.corptostore.model.oa.propertypath.*;
import com.corptostore.model.oa.Batch;
import com.corptostore.model.oa.Receive;
import com.corptostore.model.oa.Send;
import com.corptostore.model.oa.Store;
import com.corptostore.model.oa.StoreBatch;
import com.corptostore.model.oa.StoreTransmitBatch;
import com.corptostore.model.oa.Tester;
import com.corptostore.model.oa.Transmit;
import com.corptostore.model.oa.TransmitBatch;
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
    public static final String P_NewTest = "NewTest";
    public static final String P_SearchTests = "SearchTests";
    public static final String P_SearchBatches = "SearchBatches";
    public static final String P_SearchSends = "SearchSends";
    public static final String P_SearchStores = "SearchStores";
    public static final String P_SearchStoreBatches = "SearchStoreBatches";
    public static final String P_SearchStoreTransmitBatches = "SearchStoreTransmitBatches";
    public static final String P_SearchTransmitBatches = "SearchTransmitBatches";
    public static final String P_SearchTransmits = "SearchTransmits";
    public static final String P_SearchReceives = "SearchReceives";
/*$$End: ClientRoot1 $$*/

	protected int id;

	// Hub
	/*$$Start: ClientRoot2 $$*/
    // Hubs for Client UI
    protected transient Hub<Tester> hubNewTest;
    protected transient Hub<Tester> hubSearchTests;
    protected transient Hub<Batch> hubSearchBatches;
    protected transient Hub<Send> hubSearchSends;
    protected transient Hub<Store> hubSearchStores;
    protected transient Hub<StoreBatch> hubSearchStoreBatches;
    protected transient Hub<StoreTransmitBatch> hubSearchStoreTransmitBatches;
    protected transient Hub<TransmitBatch> hubSearchTransmitBatches;
    protected transient Hub<Transmit> hubSearchTransmits;
    protected transient Hub<Receive> hubSearchReceives;
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
    @OAMany(toClass = Tester.class, cascadeSave = true)
    public Hub<Tester> getNewTest() {
        if (hubNewTest == null) {
            hubNewTest = (Hub<Tester>) super.getHub(P_NewTest);
        }
        return hubNewTest;
    }
    @OAMany(toClass = Tester.class, cascadeSave = true)
    public Hub<Tester> getSearchTests() {
        if (hubSearchTests == null) {
            hubSearchTests = (Hub<Tester>) super.getHub(P_SearchTests);
        }
        return hubSearchTests;
    }
    @OAMany(toClass = Batch.class, cascadeSave = true)
    public Hub<Batch> getSearchBatches() {
        if (hubSearchBatches == null) {
            hubSearchBatches = (Hub<Batch>) super.getHub(P_SearchBatches);
        }
        return hubSearchBatches;
    }
    @OAMany(toClass = Send.class, cascadeSave = true)
    public Hub<Send> getSearchSends() {
        if (hubSearchSends == null) {
            hubSearchSends = (Hub<Send>) super.getHub(P_SearchSends);
        }
        return hubSearchSends;
    }
    @OAMany(toClass = Store.class, cascadeSave = true)
    public Hub<Store> getSearchStores() {
        if (hubSearchStores == null) {
            hubSearchStores = (Hub<Store>) super.getHub(P_SearchStores);
        }
        return hubSearchStores;
    }
    @OAMany(toClass = StoreBatch.class, cascadeSave = true)
    public Hub<StoreBatch> getSearchStoreBatches() {
        if (hubSearchStoreBatches == null) {
            hubSearchStoreBatches = (Hub<StoreBatch>) super.getHub(P_SearchStoreBatches);
        }
        return hubSearchStoreBatches;
    }
    @OAMany(toClass = StoreTransmitBatch.class, cascadeSave = true)
    public Hub<StoreTransmitBatch> getSearchStoreTransmitBatches() {
        if (hubSearchStoreTransmitBatches == null) {
            hubSearchStoreTransmitBatches = (Hub<StoreTransmitBatch>) super.getHub(P_SearchStoreTransmitBatches);
        }
        return hubSearchStoreTransmitBatches;
    }
    @OAMany(toClass = TransmitBatch.class, cascadeSave = true)
    public Hub<TransmitBatch> getSearchTransmitBatches() {
        if (hubSearchTransmitBatches == null) {
            hubSearchTransmitBatches = (Hub<TransmitBatch>) super.getHub(P_SearchTransmitBatches);
        }
        return hubSearchTransmitBatches;
    }
    @OAMany(toClass = Transmit.class, cascadeSave = true)
    public Hub<Transmit> getSearchTransmits() {
        if (hubSearchTransmits == null) {
            hubSearchTransmits = (Hub<Transmit>) super.getHub(P_SearchTransmits);
        }
        return hubSearchTransmits;
    }
    @OAMany(toClass = Receive.class, cascadeSave = true)
    public Hub<Receive> getSearchReceives() {
        if (hubSearchReceives == null) {
            hubSearchReceives = (Hub<Receive>) super.getHub(P_SearchReceives);
        }
        return hubSearchReceives;
    }
/*$$End: ClientRoot3 $$*/

}
