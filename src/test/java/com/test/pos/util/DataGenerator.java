package com.test.pos.util;
import java.util.Stack;
import java.math.*;
import java.awt.Color;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.lang.*;
import com.viaoa.runtime.OARuntime;
import com.viaoa.select.OASelect;
import com.viaoa.metadata.*;
import com.viaoa.datasource.*;
import com.viaoa.datetime.*;
import com.viaoa.converter.*;
import com.test.pos.model.oa.*;
import com.test.pos.model.oa.filter.*;
import com.test.pos.delegate.*;
 
public class DataGenerator {
    OASelect sel;
    Hub hub;
    final Stack<OALinkInfo> stack = new Stack<>();
    public boolean add(OAObject obj, String linkName) {
        if (stack.size() > 20) {
            return false;
        }
        OAObjectInfo oi = OARuntime.oa(obj).info(obj);
        OALinkInfo li = oi.getLinkInfo(linkName); 
        if (li == null) throw new RuntimeException("link="+linkName+", does not exist for object="+obj);
        if (stack.contains(li)) return false;
        stack.push(li);
        return true;
    }
    public void done(OAObject obj, String linkName) {
        OAObjectInfo oi = OARuntime.oa(obj).info(obj);
        OALinkInfo li = oi.getLinkInfo(linkName); 
        if (li == null) throw new RuntimeException("link="+linkName+", does not exist for object="+obj);
        if (stack.pop() != li) {
            throw new RuntimeException("link="+linkName+", for object="+obj+", is not on the top of the stack");
        }
    }
    
    public Address createAddress() {
        Address address = new Address();
        return address;
    }
    
    public void prepopulate(Address obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(Address obj, int level) {
        int x;
        int tot;
        if (add(obj, Address.P_Customer)) {
            // customer
            Customer customer = null;
            customer = (Customer) OARuntime.oa(obj).internal().objects().cache().getRandom(Customer.class, 500);
            if (customer != null) obj.setCustomer(customer);
            done(obj, Address.P_Customer);
        }
        if (add(obj, Address.P_Store)) {
            // store
            Store store = null;
            done(obj, Address.P_Store);
        }
    }
    
    public AppServer createAppServer() {
        AppServer appServer = new AppServer();
        return appServer;
    }
    
    public void prepopulate(AppServer obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(AppServer obj, int level) {
        int x;
        int tot;
        if (add(obj, AppServer.P_AppUserLogin)) {
            // appUserLogin
            AppUserLogin appUserLogin = null;
            if (Math.random() < .75) {
                appUserLogin = (AppUserLogin) OARuntime.oa(obj).internal().objects().cache().getRandom(AppUserLogin.class, 500);
                if (appUserLogin != null) obj.setAppUserLogin(appUserLogin);
            }
            if (appUserLogin == null) {
                appUserLogin = createAppUserLogin();
                prepopulate(appUserLogin);
                obj.setAppUserLogin(appUserLogin);
            }
            done(obj, AppServer.P_AppUserLogin);
        }
    }
    
    public AppUser createAppUser() {
        AppUser appUser = new AppUser();
        return appUser;
    }
    
    public void prepopulate(AppUser obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(AppUser obj, int level) {
        int x;
        int tot;
        if (add(obj, AppUser.P_TeamMember)) {
            // teamMember
            TeamMember teamMember = null;
            teamMember = (TeamMember) OARuntime.oa(obj).internal().objects().cache().getRandom(TeamMember.class, 500);
            if (teamMember != null) obj.setTeamMember(teamMember);
            done(obj, AppUser.P_TeamMember);
        }
    }
    
    public AppUserError createAppUserError() {
        AppUserError appUserError = new AppUserError();
        return appUserError;
    }
    
    public void prepopulate(AppUserError obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(AppUserError obj, int level) {
        int x;
        int tot;
        if (add(obj, AppUserError.P_AppUserLogin)) {
            // appUserLogin
            //    owned
            done(obj, AppUserError.P_AppUserLogin);
        }
    }
    
    public AppUserLogin createAppUserLogin() {
        AppUserLogin appUserLogin = new AppUserLogin();
        return appUserLogin;
    }
    
    public void prepopulate(AppUserLogin obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(AppUserLogin obj, int level) {
        int x;
        int tot;
        if (add(obj, AppUserLogin.P_AppUser)) {
            // appUser
            //    owned
            done(obj, AppUserLogin.P_AppUser);
        }
    }
    
    public BackroomMap createBackroomMap() {
        BackroomMap backroomMap = new BackroomMap();
        return backroomMap;
    }
    
    public void prepopulate(BackroomMap obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(BackroomMap obj, int level) {
        int x;
        int tot;
    }
    
    public BankDeposit createBankDeposit() {
        BankDeposit bankDeposit = new BankDeposit();
        return bankDeposit;
    }
    
    public void prepopulate(BankDeposit obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(BankDeposit obj, int level) {
        int x;
        int tot;
        if (add(obj, BankDeposit.P_DepositSeal)) {
            // depositSeal
            DepositSeal depositSeal = null;
            depositSeal = createDepositSeal();
            prepopulate(depositSeal);
            obj.setDepositSeal(depositSeal);
            done(obj, BankDeposit.P_DepositSeal);
        }
        if (add(obj, BankDeposit.P_StoreSafe)) {
            // storeSafe
            //    owned
            done(obj, BankDeposit.P_StoreSafe);
        }
    }
    
    public BankDepositCheck createBankDepositCheck() {
        BankDepositCheck bankDepositCheck = new BankDepositCheck();
        return bankDepositCheck;
    }
    
    public void prepopulate(BankDepositCheck obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(BankDepositCheck obj, int level) {
        int x;
        int tot;
        if (add(obj, BankDepositCheck.P_BankDeposit)) {
            // bankDeposit
            BankDeposit bankDeposit = null;
            bankDeposit = (BankDeposit) OARuntime.oa(obj).internal().objects().cache().getRandom(BankDeposit.class, 500);
            if (bankDeposit != null) obj.setBankDeposit(bankDeposit);
            done(obj, BankDepositCheck.P_BankDeposit);
        }
        if (add(obj, BankDepositCheck.P_InvoicePaymentCheck)) {
            // invoicePaymentCheck
            InvoicePayment invoicePaymentCheck = null;
            invoicePaymentCheck = (InvoicePayment) OARuntime.oa(obj).internal().objects().cache().getRandom(InvoicePayment.class, 500);
            if (invoicePaymentCheck != null) obj.setInvoicePaymentCheck(invoicePaymentCheck);
            done(obj, BankDepositCheck.P_InvoicePaymentCheck);
        }
    }
    
    public BarcodeType createBarcodeType() {
        BarcodeType barcodeType = new BarcodeType();
        return barcodeType;
    }
    
    public void prepopulate(BarcodeType obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(BarcodeType obj, int level) {
        int x;
        int tot;
    }
    
    public Catalog createCatalog() {
        Catalog catalog = new Catalog();
        return catalog;
    }
    
    public void prepopulate(Catalog obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(Catalog obj, int level) {
        int x;
        int tot;
    }
    
    public CatalogCategory createCatalogCategory() {
        CatalogCategory catalogCategory = new CatalogCategory();
        return catalogCategory;
    }
    
    public void prepopulate(CatalogCategory obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(CatalogCategory obj, int level) {
        int x;
        int tot;
        if (add(obj, CatalogCategory.P_Catalog)) {
            // catalog
            Catalog catalog = null;
            catalog = (Catalog) OARuntime.oa(obj).internal().objects().cache().getRandom(Catalog.class, 500);
            if (catalog != null) obj.setCatalog(catalog);
            done(obj, CatalogCategory.P_Catalog);
        }
        if (add(obj, CatalogCategory.P_ParentCatalogCategory)) {
            // parentCatalogCategory
            //    owned
            done(obj, CatalogCategory.P_ParentCatalogCategory);
        }
    }
    
    public CatalogItem createCatalogItem() {
        CatalogItem catalogItem = new CatalogItem();
        return catalogItem;
    }
    
    public void prepopulate(CatalogItem obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(CatalogItem obj, int level) {
        int x;
        int tot;
        if (add(obj, CatalogItem.P_Item)) {
            // item
            Item item = null;
            item = (Item) OARuntime.oa(obj).internal().objects().cache().getRandom(Item.class, 500);
            if (item != null) obj.setItem(item);
            done(obj, CatalogItem.P_Item);
        }
    }
    
    public Core createCore() {
        Core core = new Core();
        return core;
    }
    
    public void prepopulate(Core obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(Core obj, int level) {
        int x;
        int tot;
    }
    
    public CronProcess createCronProcess() {
        CronProcess cronProcess = new CronProcess();
        return cronProcess;
    }
    
    public void prepopulate(CronProcess obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(CronProcess obj, int level) {
        int x;
        int tot;
    }
    
    public CurrencyDenomination createCurrencyDenomination() {
        CurrencyDenomination currencyDenomination = new CurrencyDenomination();
        return currencyDenomination;
    }
    
    public void prepopulate(CurrencyDenomination obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(CurrencyDenomination obj, int level) {
        int x;
        int tot;
        if (add(obj, CurrencyDenomination.P_CurrencyType)) {
            // currencyType
            //    owned
            done(obj, CurrencyDenomination.P_CurrencyType);
        }
    }
    
    public CurrencyExchangeRate createCurrencyExchangeRate() {
        CurrencyExchangeRate currencyExchangeRate = new CurrencyExchangeRate();
        return currencyExchangeRate;
    }
    
    public void prepopulate(CurrencyExchangeRate obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(CurrencyExchangeRate obj, int level) {
        int x;
        int tot;
        if (add(obj, CurrencyExchangeRate.P_CurrencyType)) {
            // currencyType
            hub = ModelDelegate.getCurrencyTypes();
            if (Math.random() < .85) {
                x = (int) (Math.random()*hub.getSize());
                obj.setCurrencyType((CurrencyType) hub.getAt(x));
            }
            done(obj, CurrencyExchangeRate.P_CurrencyType);
        }
        if (add(obj, CurrencyExchangeRate.P_ToCurrencyType)) {
            // toCurrencyType
            hub = ModelDelegate.getCurrencyTypes();
            if (Math.random() < .85) {
                x = (int) (Math.random()*hub.getSize());
                obj.setToCurrencyType((CurrencyType) hub.getAt(x));
            }
            done(obj, CurrencyExchangeRate.P_ToCurrencyType);
        }
    }
    
    public CurrencyType createCurrencyType() {
        CurrencyType currencyType = new CurrencyType();
        return currencyType;
    }
    
    public void prepopulate(CurrencyType obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(CurrencyType obj, int level) {
        int x;
        int tot;
    }
    
    public Customer createCustomer() {
        Customer customer = new Customer();
        return customer;
    }
    
    public void prepopulate(Customer obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(Customer obj, int level) {
        int x;
        int tot;
        if (add(obj, Customer.P_CustomerCredit)) {
            // customerCredit
            CustomerCredit customerCredit = null;
            if (Math.random() < .75) {
                customerCredit = (CustomerCredit) OARuntime.oa(obj).internal().objects().cache().getRandom(CustomerCredit.class, 500);
                if (customerCredit != null) obj.setCustomerCredit(customerCredit);
            }
            if (customerCredit == null) {
                customerCredit = createCustomerCredit();
                prepopulate(customerCredit);
                obj.setCustomerCredit(customerCredit);
            }
            done(obj, Customer.P_CustomerCredit);
        }
        if (add(obj, Customer.P_Garage)) {
            // garage
            Garage garage = null;
            if (Math.random() < .75) {
                garage = (Garage) OARuntime.oa(obj).internal().objects().cache().getRandom(Garage.class, 500);
                if (garage != null) obj.setGarage(garage);
            }
            if (garage == null) {
                garage = createGarage();
                prepopulate(garage);
                obj.setGarage(garage);
            }
            done(obj, Customer.P_Garage);
        }
    }
    
    public CustomerConnector createCustomerConnector() {
        CustomerConnector customerConnector = new CustomerConnector();
        return customerConnector;
    }
    
    public void prepopulate(CustomerConnector obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(CustomerConnector obj, int level) {
        int x;
        int tot;
    }
    
    public CustomerCredit createCustomerCredit() {
        CustomerCredit customerCredit = new CustomerCredit();
        return customerCredit;
    }
    
    public void prepopulate(CustomerCredit obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(CustomerCredit obj, int level) {
        int x;
        int tot;
        if (add(obj, CustomerCredit.P_Customer)) {
            // customer
            Customer customer = null;
            if (Math.random() < .75) {
                customer = (Customer) OARuntime.oa(obj).internal().objects().cache().getRandom(Customer.class, 500);
                if (customer != null) obj.setCustomer(customer);
            }
            if (customer == null) {
                customer = createCustomer();
                prepopulate(customer);
                obj.setCustomer(customer);
            }
            done(obj, CustomerCredit.P_Customer);
        }
    }
    
    public DcToStore createDcToStore() {
        DcToStore dcToStore = new DcToStore();
        return dcToStore;
    }
    
    public void prepopulate(DcToStore obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(DcToStore obj, int level) {
        int x;
        int tot;
    }
    
    public DeliveryService createDeliveryService() {
        DeliveryService deliveryService = new DeliveryService();
        return deliveryService;
    }
    
    public void prepopulate(DeliveryService obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(DeliveryService obj, int level) {
        int x;
        int tot;
    }
    
    public Demo createDemo() {
        Demo demo = new Demo();
        return demo;
    }
    
    public void prepopulate(Demo obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(Demo obj, int level) {
        int x;
        int tot;
    }
    
    public DemoNode createDemoNode() {
        DemoNode demoNode = new DemoNode();
        return demoNode;
    }
    
    public void prepopulate(DemoNode obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(DemoNode obj, int level) {
        int x;
        int tot;
        if (add(obj, DemoNode.P_Demo)) {
            // demo
            //    owned
            done(obj, DemoNode.P_Demo);
        }
    }
    
    public DenominationBundle createDenominationBundle() {
        DenominationBundle denominationBundle = new DenominationBundle();
        return denominationBundle;
    }
    
    public void prepopulate(DenominationBundle obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(DenominationBundle obj, int level) {
        int x;
        int tot;
        if (add(obj, DenominationBundle.P_CurrencyDenomination)) {
            // currencyDenomination
            //    owned
            done(obj, DenominationBundle.P_CurrencyDenomination);
        }
    }
    
    public DepositSeal createDepositSeal() {
        DepositSeal depositSeal = new DepositSeal();
        return depositSeal;
    }
    
    public void prepopulate(DepositSeal obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(DepositSeal obj, int level) {
        int x;
        int tot;
        if (add(obj, DepositSeal.P_BankDeposit)) {
            // bankDeposit
            //    owned
            done(obj, DepositSeal.P_BankDeposit);
        }
    }
    
    public DiscountCoupon createDiscountCoupon() {
        DiscountCoupon discountCoupon = new DiscountCoupon();
        return discountCoupon;
    }
    
    public void prepopulate(DiscountCoupon obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(DiscountCoupon obj, int level) {
        int x;
        int tot;
    }
    
    public DiscountType createDiscountType() {
        DiscountType discountType = new DiscountType();
        return discountType;
    }
    
    public void prepopulate(DiscountType obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(DiscountType obj, int level) {
        int x;
        int tot;
    }
    
    public DistCenter createDistCenter() {
        DistCenter distCenter = new DistCenter();
        return distCenter;
    }
    
    public void prepopulate(DistCenter obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(DistCenter obj, int level) {
        int x;
        int tot;
    }
    
    public Feedback createFeedback() {
        Feedback feedback = new Feedback();
        return feedback;
    }
    
    public void prepopulate(Feedback obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(Feedback obj, int level) {
        int x;
        int tot;
    }
    
    public Garage createGarage() {
        Garage garage = new Garage();
        return garage;
    }
    
    public void prepopulate(Garage obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(Garage obj, int level) {
        int x;
        int tot;
        if (add(obj, Garage.P_Customer)) {
            // customer
            Customer customer = null;
            if (Math.random() < .75) {
                customer = (Customer) OARuntime.oa(obj).internal().objects().cache().getRandom(Customer.class, 500);
                if (customer != null) obj.setCustomer(customer);
            }
            if (customer == null) {
                customer = createCustomer();
                prepopulate(customer);
                obj.setCustomer(customer);
            }
            done(obj, Garage.P_Customer);
        }
    }
    
    public GarageVehicle createGarageVehicle() {
        GarageVehicle garageVehicle = new GarageVehicle();
        return garageVehicle;
    }
    
    public void prepopulate(GarageVehicle obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(GarageVehicle obj, int level) {
        int x;
        int tot;
        if (add(obj, GarageVehicle.P_Garage)) {
            // garage
            //    owned
            done(obj, GarageVehicle.P_Garage);
        }
        if (add(obj, GarageVehicle.P_VehicleModel)) {
            // vehicleModel
            VehicleModel vehicleModel = null;
            vehicleModel = (VehicleModel) OARuntime.oa(obj).internal().objects().cache().getRandom(VehicleModel.class, 500);
            if (vehicleModel != null) obj.setVehicleModel(vehicleModel);
            done(obj, GarageVehicle.P_VehicleModel);
        }
        if (add(obj, GarageVehicle.P_VehicleModelPackage)) {
            // vehicleModelPackage
            VehicleModelPackage vehicleModelPackage = null;
            vehicleModelPackage = (VehicleModelPackage) OARuntime.oa(obj).internal().objects().cache().getRandom(VehicleModelPackage.class, 500);
            if (vehicleModelPackage != null) obj.setVehicleModelPackage(vehicleModelPackage);
            done(obj, GarageVehicle.P_VehicleModelPackage);
        }
    }
    
    public ImageStore createImageStore() {
        ImageStore imageStore = new ImageStore();
        return imageStore;
    }
    
    public void prepopulate(ImageStore obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(ImageStore obj, int level) {
        int x;
        int tot;
    }
    
    public InventoryConnector createInventoryConnector() {
        InventoryConnector inventoryConnector = new InventoryConnector();
        return inventoryConnector;
    }
    
    public void prepopulate(InventoryConnector obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(InventoryConnector obj, int level) {
        int x;
        int tot;
    }
    
    public Invoice createInvoice() {
        Invoice invoice = new Invoice();
        return invoice;
    }
    
    public void prepopulate(Invoice obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(Invoice obj, int level) {
        int x;
        int tot;
        if (add(obj, Invoice.P_Customer)) {
            // customer
            Customer customer = null;
            customer = (Customer) OARuntime.oa(obj).internal().objects().cache().getRandom(Customer.class, 500);
            if (customer != null) obj.setCustomer(customer);
            done(obj, Invoice.P_Customer);
        }
        if (add(obj, Invoice.P_Quote)) {
            // quote
            Quote quote = null;
            done(obj, Invoice.P_Quote);
        }
        if (add(obj, Invoice.P_RegisterSession)) {
            // registerSession
            RegisterSession registerSession = null;
            done(obj, Invoice.P_RegisterSession);
        }
    }
    
    public InvoiceBasket createInvoiceBasket() {
        InvoiceBasket invoiceBasket = new InvoiceBasket();
        return invoiceBasket;
    }
    
    public void prepopulate(InvoiceBasket obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(InvoiceBasket obj, int level) {
        int x;
        int tot;
        if (add(obj, InvoiceBasket.P_Invoice)) {
            // invoice
            //    owned
            done(obj, InvoiceBasket.P_Invoice);
        }
        if (add(obj, InvoiceBasket.P_InvoiceShipTo)) {
            // invoiceShipTo
            InvoiceShipTo invoiceShipTo = null;
            if (Math.random() < .75) {
                invoiceShipTo = (InvoiceShipTo) OARuntime.oa(obj).internal().objects().cache().getRandom(InvoiceShipTo.class, 500);
                if (invoiceShipTo != null) obj.setInvoiceShipTo(invoiceShipTo);
            }
            if (invoiceShipTo == null) {
                invoiceShipTo = createInvoiceShipTo();
                prepopulate(invoiceShipTo);
                obj.setInvoiceShipTo(invoiceShipTo);
            }
            done(obj, InvoiceBasket.P_InvoiceShipTo);
        }
    }
    
    public InvoiceDiscount createInvoiceDiscount() {
        InvoiceDiscount invoiceDiscount = new InvoiceDiscount();
        return invoiceDiscount;
    }
    
    public void prepopulate(InvoiceDiscount obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(InvoiceDiscount obj, int level) {
        int x;
        int tot;
    }
    
    public InvoicePayment createInvoicePayment() {
        InvoicePayment invoicePayment = new InvoicePayment();
        return invoicePayment;
    }
    
    public void prepopulate(InvoicePayment obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(InvoicePayment obj, int level) {
        int x;
        int tot;
        if (add(obj, InvoicePayment.P_BankDepositCheck)) {
            // bankDepositCheck
            BankDepositCheck bankDepositCheck = null;
            if (Math.random() < .75) {
                bankDepositCheck = (BankDepositCheck) OARuntime.oa(obj).internal().objects().cache().getRandom(BankDepositCheck.class, 500);
                if (bankDepositCheck != null) obj.setBankDepositCheck(bankDepositCheck);
            }
            if (bankDepositCheck == null) {
                bankDepositCheck = createBankDepositCheck();
                prepopulate(bankDepositCheck);
                obj.setBankDepositCheck(bankDepositCheck);
            }
            done(obj, InvoicePayment.P_BankDepositCheck);
        }
        if (add(obj, InvoicePayment.P_Invoice)) {
            // invoice
            //    owned
            done(obj, InvoicePayment.P_Invoice);
        }
        if (add(obj, InvoicePayment.P_InvoicePaymentCheck)) {
            // invoicePaymentCheck
            InvoicePaymentCheck invoicePaymentCheck = null;
            if (Math.random() < .75) {
                invoicePaymentCheck = (InvoicePaymentCheck) OARuntime.oa(obj).internal().objects().cache().getRandom(InvoicePaymentCheck.class, 500);
                if (invoicePaymentCheck != null) obj.setInvoicePaymentCheck(invoicePaymentCheck);
            }
            if (invoicePaymentCheck == null) {
                invoicePaymentCheck = createInvoicePaymentCheck();
                prepopulate(invoicePaymentCheck);
                obj.setInvoicePaymentCheck(invoicePaymentCheck);
            }
            done(obj, InvoicePayment.P_InvoicePaymentCheck);
        }
        if (add(obj, InvoicePayment.P_TillLedgerEntry)) {
            // tillLedgerEntry
            TillLedgerEntry tillLedgerEntry = null;
            done(obj, InvoicePayment.P_TillLedgerEntry);
        }
    }
    
    public InvoicePaymentCheck createInvoicePaymentCheck() {
        InvoicePaymentCheck invoicePaymentCheck = new InvoicePaymentCheck();
        return invoicePaymentCheck;
    }
    
    public void prepopulate(InvoicePaymentCheck obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(InvoicePaymentCheck obj, int level) {
        int x;
        int tot;
        if (add(obj, InvoicePaymentCheck.P_InvoicePayment)) {
            // invoicePayment
            InvoicePayment invoicePayment = null;
            invoicePayment = (InvoicePayment) OARuntime.oa(obj).internal().objects().cache().getRandom(InvoicePayment.class, 500);
            if (invoicePayment != null) obj.setInvoicePayment(invoicePayment);
            done(obj, InvoicePaymentCheck.P_InvoicePayment);
        }
        if (add(obj, InvoicePaymentCheck.P_ReturnedCheckFee)) {
            // returnedCheckFee
            ReturnedCheckFee returnedCheckFee = null;
            returnedCheckFee = createReturnedCheckFee();
            prepopulate(returnedCheckFee);
            obj.setReturnedCheckFee(returnedCheckFee);
            done(obj, InvoicePaymentCheck.P_ReturnedCheckFee);
        }
        if (add(obj, InvoicePaymentCheck.P_StoreSafe)) {
            // storeSafe
            StoreSafe storeSafe = null;
            done(obj, InvoicePaymentCheck.P_StoreSafe);
        }
        if (add(obj, InvoicePaymentCheck.P_Till)) {
            // till
            Till till = null;
            done(obj, InvoicePaymentCheck.P_Till);
        }
    }
    
    public InvoiceRebate createInvoiceRebate() {
        InvoiceRebate invoiceRebate = new InvoiceRebate();
        return invoiceRebate;
    }
    
    public void prepopulate(InvoiceRebate obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(InvoiceRebate obj, int level) {
        int x;
        int tot;
    }
    
    public InvoiceShipTo createInvoiceShipTo() {
        InvoiceShipTo invoiceShipTo = new InvoiceShipTo();
        return invoiceShipTo;
    }
    
    public void prepopulate(InvoiceShipTo obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(InvoiceShipTo obj, int level) {
        int x;
        int tot;
        if (add(obj, InvoiceShipTo.P_Address)) {
            // address
            Address address = null;
            address = createAddress();
            prepopulate(address);
            obj.setAddress(address);
            done(obj, InvoiceShipTo.P_Address);
        }
        if (add(obj, InvoiceShipTo.P_InvoiceBasket)) {
            // invoiceBasket
            InvoiceBasket invoiceBasket = null;
            invoiceBasket = (InvoiceBasket) OARuntime.oa(obj).internal().objects().cache().getRandom(InvoiceBasket.class, 500);
            if (invoiceBasket != null) obj.setInvoiceBasket(invoiceBasket);
            done(obj, InvoiceShipTo.P_InvoiceBasket);
        }
    }
    
    public InvoiceTax createInvoiceTax() {
        InvoiceTax invoiceTax = new InvoiceTax();
        return invoiceTax;
    }
    
    public void prepopulate(InvoiceTax obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(InvoiceTax obj, int level) {
        int x;
        int tot;
    }
    
    public Item createItem() {
        Item item = new Item();
        return item;
    }
    
    public void prepopulate(Item obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(Item obj, int level) {
        int x;
        int tot;
        if (add(obj, Item.P_ItemLine)) {
            // itemLine
            hub = ModelDelegate.getItemLines();
            if (Math.random() < .85) {
                x = (int) (Math.random()*hub.getSize());
                obj.setItemLine((ItemLine) hub.getAt(x));
            }
            done(obj, Item.P_ItemLine);
        }
        if (add(obj, Item.P_Manufacturer)) {
            // manufacturer
            Manufacturer manufacturer = null;
            manufacturer = (Manufacturer) OARuntime.oa(obj).internal().objects().cache().getRandom(Manufacturer.class, 500);
            if (manufacturer != null) obj.setManufacturer(manufacturer);
            done(obj, Item.P_Manufacturer);
        }
    }
    
    public ItemAlert createItemAlert() {
        ItemAlert itemAlert = new ItemAlert();
        return itemAlert;
    }
    
    public void prepopulate(ItemAlert obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(ItemAlert obj, int level) {
        int x;
        int tot;
    }
    
    public ItemCategory createItemCategory() {
        ItemCategory itemCategory = new ItemCategory();
        return itemCategory;
    }
    
    public void prepopulate(ItemCategory obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(ItemCategory obj, int level) {
        int x;
        int tot;
        if (add(obj, ItemCategory.P_ParentItemCategory)) {
            // parentItemCategory
            //    owned
            done(obj, ItemCategory.P_ParentItemCategory);
        }
        if (add(obj, ItemCategory.P_VertexTaxCode)) {
            // vertexTaxCode
            hub = ModelDelegate.getVertexTaxCodes();
            if (Math.random() < .85) {
                x = (int) (Math.random()*hub.getSize());
                obj.setVertexTaxCode((VertexTaxCode) hub.getAt(x));
            }
            done(obj, ItemCategory.P_VertexTaxCode);
        }
    }
    
    public ItemInterchange createItemInterchange() {
        ItemInterchange itemInterchange = new ItemInterchange();
        return itemInterchange;
    }
    
    public void prepopulate(ItemInterchange obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(ItemInterchange obj, int level) {
        int x;
        int tot;
    }
    
    public ItemKit createItemKit() {
        ItemKit itemKit = new ItemKit();
        return itemKit;
    }
    
    public void prepopulate(ItemKit obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(ItemKit obj, int level) {
        int x;
        int tot;
        if (add(obj, ItemKit.P_Item)) {
            // item
            //    owned
            done(obj, ItemKit.P_Item);
        }
    }
    
    public ItemLine createItemLine() {
        ItemLine itemLine = new ItemLine();
        return itemLine;
    }
    
    public void prepopulate(ItemLine obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(ItemLine obj, int level) {
        int x;
        int tot;
    }
    
    public ItemMSDS createItemMSDS() {
        ItemMSDS itemMSDS = new ItemMSDS();
        return itemMSDS;
    }
    
    public void prepopulate(ItemMSDS obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(ItemMSDS obj, int level) {
        int x;
        int tot;
    }
    
    public ItemOption createItemOption() {
        ItemOption itemOption = new ItemOption();
        return itemOption;
    }
    
    public void prepopulate(ItemOption obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(ItemOption obj, int level) {
        int x;
        int tot;
        if (add(obj, ItemOption.P_Item)) {
            // item
            //    owned
            done(obj, ItemOption.P_Item);
        }
        if (add(obj, ItemOption.P_ItemOptionType)) {
            // itemOptionType
            hub = ModelDelegate.getItemOptionTypes();
            if (Math.random() < .85) {
                x = (int) (Math.random()*hub.getSize());
                obj.setItemOptionType((ItemOptionType) hub.getAt(x));
            }
            done(obj, ItemOption.P_ItemOptionType);
        }
    }
    
    public ItemOptionType createItemOptionType() {
        ItemOptionType itemOptionType = new ItemOptionType();
        return itemOptionType;
    }
    
    public void prepopulate(ItemOptionType obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(ItemOptionType obj, int level) {
        int x;
        int tot;
    }
    
    public ItemOptionTypeValue createItemOptionTypeValue() {
        ItemOptionTypeValue itemOptionTypeValue = new ItemOptionTypeValue();
        return itemOptionTypeValue;
    }
    
    public void prepopulate(ItemOptionTypeValue obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(ItemOptionTypeValue obj, int level) {
        int x;
        int tot;
        if (add(obj, ItemOptionTypeValue.P_ItemOptionType)) {
            // itemOptionType
            //    owned
            done(obj, ItemOptionTypeValue.P_ItemOptionType);
        }
    }
    
    public ItemOptionValue createItemOptionValue() {
        ItemOptionValue itemOptionValue = new ItemOptionValue();
        return itemOptionValue;
    }
    
    public void prepopulate(ItemOptionValue obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(ItemOptionValue obj, int level) {
        int x;
        int tot;
        if (add(obj, ItemOptionValue.P_ItemOption)) {
            // itemOption
            //    owned
            done(obj, ItemOptionValue.P_ItemOption);
        }
    }
    
    public ItemPack createItemPack() {
        ItemPack itemPack = new ItemPack();
        return itemPack;
    }
    
    public void prepopulate(ItemPack obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(ItemPack obj, int level) {
        int x;
        int tot;
        if (add(obj, ItemPack.P_Item)) {
            // item
            //    owned
            done(obj, ItemPack.P_Item);
        }
        if (add(obj, ItemPack.P_ItemPackType)) {
            // itemPackType
            hub = ModelDelegate.getItemPackTypes();
            if (Math.random() < .85) {
                x = (int) (Math.random()*hub.getSize());
                obj.setItemPackType((ItemPackType) hub.getAt(x));
            }
            done(obj, ItemPack.P_ItemPackType);
        }
    }
    
    public ItemPackType createItemPackType() {
        ItemPackType itemPackType = new ItemPackType();
        return itemPackType;
    }
    
    public void prepopulate(ItemPackType obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(ItemPackType obj, int level) {
        int x;
        int tot;
    }
    
    public ItemRestriction createItemRestriction() {
        ItemRestriction itemRestriction = new ItemRestriction();
        return itemRestriction;
    }
    
    public void prepopulate(ItemRestriction obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(ItemRestriction obj, int level) {
        int x;
        int tot;
    }
    
    public ItemVariant createItemVariant() {
        ItemVariant itemVariant = new ItemVariant();
        return itemVariant;
    }
    
    public void prepopulate(ItemVariant obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(ItemVariant obj, int level) {
        int x;
        int tot;
        if (add(obj, ItemVariant.P_Item)) {
            // item
            //    owned
            done(obj, ItemVariant.P_Item);
        }
    }
    
    public ItemVendor createItemVendor() {
        ItemVendor itemVendor = new ItemVendor();
        return itemVendor;
    }
    
    public void prepopulate(ItemVendor obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(ItemVendor obj, int level) {
        int x;
        int tot;
    }
    
    public LedgerDenominationBundle createLedgerDenominationBundle() {
        LedgerDenominationBundle ledgerDenominationBundle = new LedgerDenominationBundle();
        return ledgerDenominationBundle;
    }
    
    public void prepopulate(LedgerDenominationBundle obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(LedgerDenominationBundle obj, int level) {
        int x;
        int tot;
        if (add(obj, LedgerDenominationBundle.P_StoreSafeLedgerEntry)) {
            // storeSafeLedgerEntry
            StoreSafeLedgerEntry storeSafeLedgerEntry = null;
            done(obj, LedgerDenominationBundle.P_StoreSafeLedgerEntry);
        }
        if (add(obj, LedgerDenominationBundle.P_TillLedgerEntry)) {
            // tillLedgerEntry
            TillLedgerEntry tillLedgerEntry = null;
            done(obj, LedgerDenominationBundle.P_TillLedgerEntry);
        }
    }
    
    public LineItem createLineItem() {
        LineItem lineItem = new LineItem();
        return lineItem;
    }
    
    public void prepopulate(LineItem obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(LineItem obj, int level) {
        int x;
        int tot;
        if (add(obj, LineItem.P_InvoiceBasket)) {
            // invoiceBasket
            //    owned
            done(obj, LineItem.P_InvoiceBasket);
        }
        if (add(obj, LineItem.P_Product)) {
            // product
            Product product = null;
            product = (Product) OARuntime.oa(obj).internal().objects().cache().getRandom(Product.class, 500);
            if (product != null) obj.setProduct(product);
            done(obj, LineItem.P_Product);
        }
    }
    
    public LineItemDiscount createLineItemDiscount() {
        LineItemDiscount lineItemDiscount = new LineItemDiscount();
        return lineItemDiscount;
    }
    
    public void prepopulate(LineItemDiscount obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(LineItemDiscount obj, int level) {
        int x;
        int tot;
    }
    
    public LineItemTax createLineItemTax() {
        LineItemTax lineItemTax = new LineItemTax();
        return lineItemTax;
    }
    
    public void prepopulate(LineItemTax obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(LineItemTax obj, int level) {
        int x;
        int tot;
        if (add(obj, LineItemTax.P_LineItem)) {
            // lineItem
            LineItem lineItem = null;
            done(obj, LineItemTax.P_LineItem);
        }
    }
    
    public ManualPurchaseOrder createManualPurchaseOrder() {
        ManualPurchaseOrder manualPurchaseOrder = new ManualPurchaseOrder();
        return manualPurchaseOrder;
    }
    
    public void prepopulate(ManualPurchaseOrder obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(ManualPurchaseOrder obj, int level) {
        int x;
        int tot;
        if (add(obj, ManualPurchaseOrder.P_Store)) {
            // store
            //    owned
            done(obj, ManualPurchaseOrder.P_Store);
        }
        if (add(obj, ManualPurchaseOrder.P_StoreSafeLedgerEntry)) {
            // storeSafeLedgerEntry
            StoreSafeLedgerEntry storeSafeLedgerEntry = null;
            storeSafeLedgerEntry = (StoreSafeLedgerEntry) OARuntime.oa(obj).internal().objects().cache().getRandom(StoreSafeLedgerEntry.class, 500);
            if (storeSafeLedgerEntry != null) obj.setStoreSafeLedgerEntry(storeSafeLedgerEntry);
            done(obj, ManualPurchaseOrder.P_StoreSafeLedgerEntry);
        }
    }
    
    public Manufacturer createManufacturer() {
        Manufacturer manufacturer = new Manufacturer();
        return manufacturer;
    }
    
    public void prepopulate(Manufacturer obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(Manufacturer obj, int level) {
        int x;
        int tot;
    }
    
    public MeasureType createMeasureType() {
        MeasureType measureType = new MeasureType();
        return measureType;
    }
    
    public void prepopulate(MeasureType obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(MeasureType obj, int level) {
        int x;
        int tot;
    }
    
    public NewNetPriceCalculater createNewNetPriceCalculater() {
        NewNetPriceCalculater newNetPriceCalculater = new NewNetPriceCalculater();
        return newNetPriceCalculater;
    }
    
    public void prepopulate(NewNetPriceCalculater obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(NewNetPriceCalculater obj, int level) {
        int x;
        int tot;
    }
    
    public OnlineOrder createOnlineOrder() {
        OnlineOrder onlineOrder = new OnlineOrder();
        return onlineOrder;
    }
    
    public void prepopulate(OnlineOrder obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(OnlineOrder obj, int level) {
        int x;
        int tot;
        if (add(obj, OnlineOrder.P_Customer)) {
            // customer
            Customer customer = null;
            customer = (Customer) OARuntime.oa(obj).internal().objects().cache().getRandom(Customer.class, 500);
            if (customer != null) obj.setCustomer(customer);
            done(obj, OnlineOrder.P_Customer);
        }
    }
    
    public OnlineOrderDelivery createOnlineOrderDelivery() {
        OnlineOrderDelivery onlineOrderDelivery = new OnlineOrderDelivery();
        return onlineOrderDelivery;
    }
    
    public void prepopulate(OnlineOrderDelivery obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(OnlineOrderDelivery obj, int level) {
        int x;
        int tot;
        if (add(obj, OnlineOrderDelivery.P_DeliveryService)) {
            // deliveryService
            hub = ModelDelegate.getDeliveryServices();
            if (Math.random() < .85) {
                x = (int) (Math.random()*hub.getSize());
                obj.setDeliveryService((DeliveryService) hub.getAt(x));
            }
            done(obj, OnlineOrderDelivery.P_DeliveryService);
        }
        if (add(obj, OnlineOrderDelivery.P_OnlineOrder)) {
            // onlineOrder
            OnlineOrder onlineOrder = null;
            onlineOrder = (OnlineOrder) OARuntime.oa(obj).internal().objects().cache().getRandom(OnlineOrder.class, 500);
            if (onlineOrder != null) obj.setOnlineOrder(onlineOrder);
            done(obj, OnlineOrderDelivery.P_OnlineOrder);
        }
    }
    
    public OnlineOrderItem createOnlineOrderItem() {
        OnlineOrderItem onlineOrderItem = new OnlineOrderItem();
        return onlineOrderItem;
    }
    
    public void prepopulate(OnlineOrderItem obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(OnlineOrderItem obj, int level) {
        int x;
        int tot;
        if (add(obj, OnlineOrderItem.P_Item)) {
            // item
            Item item = null;
            item = (Item) OARuntime.oa(obj).internal().objects().cache().getRandom(Item.class, 500);
            if (item != null) obj.setItem(item);
            done(obj, OnlineOrderItem.P_Item);
        }
        if (add(obj, OnlineOrderItem.P_OnlineOrder)) {
            // onlineOrder
            //    owned
            done(obj, OnlineOrderItem.P_OnlineOrder);
        }
    }
    
    public OodItem createOodItem() {
        OodItem oodItem = new OodItem();
        return oodItem;
    }
    
    public void prepopulate(OodItem obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(OodItem obj, int level) {
        int x;
        int tot;
        if (add(obj, OodItem.P_OnlineOrderDelivery)) {
            // onlineOrderDelivery
            //    owned
            done(obj, OodItem.P_OnlineOrderDelivery);
        }
        if (add(obj, OodItem.P_OnlineOrderItem)) {
            // onlineOrderItem
            OnlineOrderItem onlineOrderItem = null;
            onlineOrderItem = (OnlineOrderItem) OARuntime.oa(obj).internal().objects().cache().getRandom(OnlineOrderItem.class, 500);
            if (onlineOrderItem != null) obj.setOnlineOrderItem(onlineOrderItem);
            done(obj, OodItem.P_OnlineOrderItem);
        }
    }
    
    public OodItemEach createOodItemEach() {
        OodItemEach oodItemEach = new OodItemEach();
        return oodItemEach;
    }
    
    public void prepopulate(OodItemEach obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(OodItemEach obj, int level) {
        int x;
        int tot;
        if (add(obj, OodItemEach.P_OodItem)) {
            // oodItem
            //    owned
            done(obj, OodItemEach.P_OodItem);
        }
    }
    
    public OPPConnector createOPPConnector() {
        OPPConnector oppConnector = new OPPConnector();
        return oppConnector;
    }
    
    public void prepopulate(OPPConnector obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(OPPConnector obj, int level) {
        int x;
        int tot;
    }
    
    public OutFrontMerch createOutFrontMerch() {
        OutFrontMerch outFrontMerch = new OutFrontMerch();
        return outFrontMerch;
    }
    
    public void prepopulate(OutFrontMerch obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(OutFrontMerch obj, int level) {
        int x;
        int tot;
    }
    
    public PaymentConnector createPaymentConnector() {
        PaymentConnector paymentConnector = new PaymentConnector();
        return paymentConnector;
    }
    
    public void prepopulate(PaymentConnector obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(PaymentConnector obj, int level) {
        int x;
        int tot;
    }
    
    public Planogram createPlanogram() {
        Planogram planogram = new Planogram();
        return planogram;
    }
    
    public void prepopulate(Planogram obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(Planogram obj, int level) {
        int x;
        int tot;
    }
    
    public PriceBookEntry createPriceBookEntry() {
        PriceBookEntry priceBookEntry = new PriceBookEntry();
        return priceBookEntry;
    }
    
    public void prepopulate(PriceBookEntry obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(PriceBookEntry obj, int level) {
        int x;
        int tot;
        if (add(obj, PriceBookEntry.P_Item)) {
            // item
            //    owned
            done(obj, PriceBookEntry.P_Item);
        }
        if (add(obj, PriceBookEntry.P_ItemOptionValue)) {
            // itemOptionValue
            ItemOptionValue itemOptionValue = null;
            itemOptionValue = (ItemOptionValue) OARuntime.oa(obj).internal().objects().cache().getRandom(ItemOptionValue.class, 500);
            if (itemOptionValue != null) obj.setItemOptionValue(itemOptionValue);
            done(obj, PriceBookEntry.P_ItemOptionValue);
        }
    }
    
    public Printer createPrinter() {
        Printer printer = new Printer();
        return printer;
    }
    
    public void prepopulate(Printer obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(Printer obj, int level) {
        int x;
        int tot;
    }
    
    public Product createProduct() {
        Product product = new Product();
        return product;
    }
    
    public void prepopulate(Product obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(Product obj, int level) {
        int x;
        int tot;
        if (add(obj, Product.P_Item)) {
            // item
            //    owned
            done(obj, Product.P_Item);
        }
    }
    
    public ProductSerialCode createProductSerialCode() {
        ProductSerialCode productSerialCode = new ProductSerialCode();
        return productSerialCode;
    }
    
    public void prepopulate(ProductSerialCode obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(ProductSerialCode obj, int level) {
        int x;
        int tot;
        if (add(obj, ProductSerialCode.P_Product)) {
            // product
            //    owned
            done(obj, ProductSerialCode.P_Product);
        }
    }
    
    public ProductUpc createProductUpc() {
        ProductUpc productUpc = new ProductUpc();
        return productUpc;
    }
    
    public void prepopulate(ProductUpc obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(ProductUpc obj, int level) {
        int x;
        int tot;
        if (add(obj, ProductUpc.P_BarcodeType)) {
            // barcodeType
            hub = ModelDelegate.getBarcodeTypes();
            if (Math.random() < .85) {
                x = (int) (Math.random()*hub.getSize());
                obj.setBarcodeType((BarcodeType) hub.getAt(x));
            }
            done(obj, ProductUpc.P_BarcodeType);
        }
        if (add(obj, ProductUpc.P_Product)) {
            // product
            //    owned
            done(obj, ProductUpc.P_Product);
        }
    }
    
    public PurchaseOrder createPurchaseOrder() {
        PurchaseOrder purchaseOrder = new PurchaseOrder();
        return purchaseOrder;
    }
    
    public void prepopulate(PurchaseOrder obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(PurchaseOrder obj, int level) {
        int x;
        int tot;
    }
    
    public Quote createQuote() {
        Quote quote = new Quote();
        return quote;
    }
    
    public void prepopulate(Quote obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(Quote obj, int level) {
        int x;
        int tot;
        if (add(obj, Quote.P_Customer)) {
            // customer
            Customer customer = null;
            customer = (Customer) OARuntime.oa(obj).internal().objects().cache().getRandom(Customer.class, 500);
            if (customer != null) obj.setCustomer(customer);
            done(obj, Quote.P_Customer);
        }
        if (add(obj, Quote.P_Invoice)) {
            // invoice
            Invoice invoice = null;
            invoice = createInvoice();
            prepopulate(invoice);
            obj.setInvoice(invoice);
            done(obj, Quote.P_Invoice);
        }
    }
    
    public Refund createRefund() {
        Refund refund = new Refund();
        return refund;
    }
    
    public void prepopulate(Refund obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(Refund obj, int level) {
        int x;
        int tot;
        if (add(obj, Refund.P_RegisterSession)) {
            // registerSession
            RegisterSession registerSession = null;
            registerSession = (RegisterSession) OARuntime.oa(obj).internal().objects().cache().getRandom(RegisterSession.class, 500);
            if (registerSession != null) obj.setRegisterSession(registerSession);
            done(obj, Refund.P_RegisterSession);
        }
    }
    
    public RefundInvoice createRefundInvoice() {
        RefundInvoice refundInvoice = new RefundInvoice();
        return refundInvoice;
    }
    
    public void prepopulate(RefundInvoice obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(RefundInvoice obj, int level) {
        int x;
        int tot;
        if (add(obj, RefundInvoice.P_Invoice)) {
            // invoice
            Invoice invoice = null;
            invoice = (Invoice) OARuntime.oa(obj).internal().objects().cache().getRandom(Invoice.class, 500);
            if (invoice != null) obj.setInvoice(invoice);
            done(obj, RefundInvoice.P_Invoice);
        }
        if (add(obj, RefundInvoice.P_Refund)) {
            // refund
            //    owned
            done(obj, RefundInvoice.P_Refund);
        }
    }
    
    public RefundLineItem createRefundLineItem() {
        RefundLineItem refundLineItem = new RefundLineItem();
        return refundLineItem;
    }
    
    public void prepopulate(RefundLineItem obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(RefundLineItem obj, int level) {
        int x;
        int tot;
        if (add(obj, RefundLineItem.P_RefundInvoice)) {
            // refundInvoice
            //    owned
            done(obj, RefundLineItem.P_RefundInvoice);
        }
    }
    
    public RefundLineItemTax createRefundLineItemTax() {
        RefundLineItemTax refundLineItemTax = new RefundLineItemTax();
        return refundLineItemTax;
    }
    
    public void prepopulate(RefundLineItemTax obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(RefundLineItemTax obj, int level) {
        int x;
        int tot;
        if (add(obj, RefundLineItemTax.P_RefundLineItem)) {
            // refundLineItem
            //    owned
            done(obj, RefundLineItemTax.P_RefundLineItem);
        }
    }
    
    public RefundPayment createRefundPayment() {
        RefundPayment refundPayment = new RefundPayment();
        return refundPayment;
    }
    
    public void prepopulate(RefundPayment obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(RefundPayment obj, int level) {
        int x;
        int tot;
        if (add(obj, RefundPayment.P_RefundInvoice)) {
            // refundInvoice
            //    owned
            done(obj, RefundPayment.P_RefundInvoice);
        }
        if (add(obj, RefundPayment.P_TillLedgerEntry)) {
            // tillLedgerEntry
            TillLedgerEntry tillLedgerEntry = null;
            if (Math.random() < .75) {
                tillLedgerEntry = (TillLedgerEntry) OARuntime.oa(obj).internal().objects().cache().getRandom(TillLedgerEntry.class, 500);
                if (tillLedgerEntry != null) obj.setTillLedgerEntry(tillLedgerEntry);
            }
            if (tillLedgerEntry == null) {
                tillLedgerEntry = createTillLedgerEntry();
                prepopulate(tillLedgerEntry);
                obj.setTillLedgerEntry(tillLedgerEntry);
            }
            done(obj, RefundPayment.P_TillLedgerEntry);
        }
    }
    
    public Register createRegister() {
        Register register = new Register();
        return register;
    }
    
    public void prepopulate(Register obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(Register obj, int level) {
        int x;
        int tot;
        if (add(obj, Register.P_Store)) {
            // store
            //    owned
            done(obj, Register.P_Store);
        }
    }
    
    public RegisterSession createRegisterSession() {
        RegisterSession registerSession = new RegisterSession();
        return registerSession;
    }
    
    public void prepopulate(RegisterSession obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(RegisterSession obj, int level) {
        int x;
        int tot;
        if (add(obj, RegisterSession.P_Register)) {
            // register
            //    owned
            done(obj, RegisterSession.P_Register);
        }
    }
    
    public Report createReport() {
        Report report = new Report();
        return report;
    }
    
    public void prepopulate(Report obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(Report obj, int level) {
        int x;
        int tot;
    }
    
    public ReportClass createReportClass() {
        ReportClass reportClass = new ReportClass();
        return reportClass;
    }
    
    public void prepopulate(ReportClass obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(ReportClass obj, int level) {
        int x;
        int tot;
    }
    
    public ReportDef createReportDef() {
        ReportDef reportDef = new ReportDef();
        return reportDef;
    }
    
    public void prepopulate(ReportDef obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(ReportDef obj, int level) {
        int x;
        int tot;
        if (add(obj, ReportDef.P_ReportClass)) {
            // reportClass
            //    owned
            done(obj, ReportDef.P_ReportClass);
        }
    }
    
    public ReturnedCheckFee createReturnedCheckFee() {
        ReturnedCheckFee returnedCheckFee = new ReturnedCheckFee();
        return returnedCheckFee;
    }
    
    public void prepopulate(ReturnedCheckFee obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(ReturnedCheckFee obj, int level) {
        int x;
        int tot;
        if (add(obj, ReturnedCheckFee.P_InvoicePaymentCheck)) {
            // invoicePaymentCheck
            //    owned
            done(obj, ReturnedCheckFee.P_InvoicePaymentCheck);
        }
    }
    
    public Reward createReward() {
        Reward reward = new Reward();
        return reward;
    }
    
    public void prepopulate(Reward obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(Reward obj, int level) {
        int x;
        int tot;
        if (add(obj, Reward.P_RewardType)) {
            // rewardType
            hub = ModelDelegate.getRewardTypes();
            if (Math.random() < .85) {
                x = (int) (Math.random()*hub.getSize());
                obj.setRewardType((RewardType) hub.getAt(x));
            }
            done(obj, Reward.P_RewardType);
        }
    }
    
    public RewardType createRewardType() {
        RewardType rewardType = new RewardType();
        return rewardType;
    }
    
    public void prepopulate(RewardType obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(RewardType obj, int level) {
        int x;
        int tot;
    }
    
    public ScannerConnector createScannerConnector() {
        ScannerConnector scannerConnector = new ScannerConnector();
        return scannerConnector;
    }
    
    public void prepopulate(ScannerConnector obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(ScannerConnector obj, int level) {
        int x;
        int tot;
    }
    
    public ShippingQuoteConnector createShippingQuoteConnector() {
        ShippingQuoteConnector shippingQuoteConnector = new ShippingQuoteConnector();
        return shippingQuoteConnector;
    }
    
    public void prepopulate(ShippingQuoteConnector obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(ShippingQuoteConnector obj, int level) {
        int x;
        int tot;
    }
    
    public Store createStore() {
        Store store = new Store();
        return store;
    }
    
    public void prepopulate(Store obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(Store obj, int level) {
        int x;
        int tot;
        if (add(obj, Store.P_CurrencyType)) {
            // currencyType
            hub = ModelDelegate.getCurrencyTypes();
            if (Math.random() < .85) {
                x = (int) (Math.random()*hub.getSize());
                obj.setCurrencyType((CurrencyType) hub.getAt(x));
            }
            done(obj, Store.P_CurrencyType);
        }
    }
    
    public StoreClosedDate createStoreClosedDate() {
        StoreClosedDate storeClosedDate = new StoreClosedDate();
        return storeClosedDate;
    }
    
    public void prepopulate(StoreClosedDate obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(StoreClosedDate obj, int level) {
        int x;
        int tot;
        if (add(obj, StoreClosedDate.P_Store)) {
            // store
            //    owned
            done(obj, StoreClosedDate.P_Store);
        }
    }
    
    public StoreCycleCount createStoreCycleCount() {
        StoreCycleCount storeCycleCount = new StoreCycleCount();
        return storeCycleCount;
    }
    
    public void prepopulate(StoreCycleCount obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(StoreCycleCount obj, int level) {
        int x;
        int tot;
    }
    
    public StoreDayEnd createStoreDayEnd() {
        StoreDayEnd storeDayEnd = new StoreDayEnd();
        return storeDayEnd;
    }
    
    public void prepopulate(StoreDayEnd obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(StoreDayEnd obj, int level) {
        int x;
        int tot;
        if (add(obj, StoreDayEnd.P_StoreSchedule)) {
            // storeSchedule
            //    owned
            done(obj, StoreDayEnd.P_StoreSchedule);
        }
    }
    
    public StoreDayOpen createStoreDayOpen() {
        StoreDayOpen storeDayOpen = new StoreDayOpen();
        return storeDayOpen;
    }
    
    public void prepopulate(StoreDayOpen obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(StoreDayOpen obj, int level) {
        int x;
        int tot;
        if (add(obj, StoreDayOpen.P_StoreSchedule)) {
            // storeSchedule
            //    owned
            done(obj, StoreDayOpen.P_StoreSchedule);
        }
    }
    
    public StoreHoursOpen createStoreHoursOpen() {
        StoreHoursOpen storeHoursOpen = new StoreHoursOpen();
        return storeHoursOpen;
    }
    
    public void prepopulate(StoreHoursOpen obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(StoreHoursOpen obj, int level) {
        int x;
        int tot;
        if (add(obj, StoreHoursOpen.P_Store)) {
            // store
            //    owned
            done(obj, StoreHoursOpen.P_Store);
        }
    }
    
    public StoreLayout createStoreLayout() {
        StoreLayout storeLayout = new StoreLayout();
        return storeLayout;
    }
    
    public void prepopulate(StoreLayout obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(StoreLayout obj, int level) {
        int x;
        int tot;
    }
    
    public StoreSafe createStoreSafe() {
        StoreSafe storeSafe = new StoreSafe();
        return storeSafe;
    }
    
    public void prepopulate(StoreSafe obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(StoreSafe obj, int level) {
        int x;
        int tot;
        if (add(obj, StoreSafe.P_Store)) {
            // store
            //    owned
            done(obj, StoreSafe.P_Store);
        }
    }
    
    public StoreSafeLedgerEntry createStoreSafeLedgerEntry() {
        StoreSafeLedgerEntry storeSafeLedgerEntry = new StoreSafeLedgerEntry();
        return storeSafeLedgerEntry;
    }
    
    public void prepopulate(StoreSafeLedgerEntry obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(StoreSafeLedgerEntry obj, int level) {
        int x;
        int tot;
        if (add(obj, StoreSafeLedgerEntry.P_ManualPurchaseOrder)) {
            // manualPurchaseOrder
            ManualPurchaseOrder manualPurchaseOrder = null;
            manualPurchaseOrder = (ManualPurchaseOrder) OARuntime.oa(obj).internal().objects().cache().getRandom(ManualPurchaseOrder.class, 500);
            if (manualPurchaseOrder != null) obj.setManualPurchaseOrder(manualPurchaseOrder);
            done(obj, StoreSafeLedgerEntry.P_ManualPurchaseOrder);
        }
        if (add(obj, StoreSafeLedgerEntry.P_StoreDayOpen)) {
            // storeDayOpen
            StoreDayOpen storeDayOpen = null;
            storeDayOpen = (StoreDayOpen) OARuntime.oa(obj).internal().objects().cache().getRandom(StoreDayOpen.class, 500);
            if (storeDayOpen != null) obj.setStoreDayOpen(storeDayOpen);
            done(obj, StoreSafeLedgerEntry.P_StoreDayOpen);
        }
        if (add(obj, StoreSafeLedgerEntry.P_StoreSafe)) {
            // storeSafe
            //    owned
            done(obj, StoreSafeLedgerEntry.P_StoreSafe);
        }
        if (add(obj, StoreSafeLedgerEntry.P_TeamMember)) {
            // teamMember
            TeamMember teamMember = null;
            done(obj, StoreSafeLedgerEntry.P_TeamMember);
        }
        if (add(obj, StoreSafeLedgerEntry.P_TillLedgerEntry)) {
            // tillLedgerEntry
            TillLedgerEntry tillLedgerEntry = null;
            done(obj, StoreSafeLedgerEntry.P_TillLedgerEntry);
        }
    }
    
    public StoreSchedule createStoreSchedule() {
        StoreSchedule storeSchedule = new StoreSchedule();
        return storeSchedule;
    }
    
    public void prepopulate(StoreSchedule obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(StoreSchedule obj, int level) {
        int x;
        int tot;
        if (add(obj, StoreSchedule.P_Store)) {
            // store
            Store store = null;
            store = (Store) OARuntime.oa(obj).internal().objects().cache().getRandom(Store.class, 500);
            if (store != null) obj.setStore(store);
            done(obj, StoreSchedule.P_Store);
        }
    }
    
    public StoreToDc createStoreToDc() {
        StoreToDc storeToDc = new StoreToDc();
        return storeToDc;
    }
    
    public void prepopulate(StoreToDc obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(StoreToDc obj, int level) {
        int x;
        int tot;
    }
    
    public StoreToStoreTransfer createStoreToStoreTransfer() {
        StoreToStoreTransfer storeToStoreTransfer = new StoreToStoreTransfer();
        return storeToStoreTransfer;
    }
    
    public void prepopulate(StoreToStoreTransfer obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(StoreToStoreTransfer obj, int level) {
        int x;
        int tot;
        if (add(obj, StoreToStoreTransfer.P_ToStore)) {
            // toStore
            Store toStore = null;
            toStore = (Store) OARuntime.oa(obj).internal().objects().cache().getRandom(Store.class, 500);
            if (toStore != null) obj.setToStore(toStore);
            done(obj, StoreToStoreTransfer.P_ToStore);
        }
    }
    
    public StsDelivery createStsDelivery() {
        StsDelivery stsDelivery = new StsDelivery();
        return stsDelivery;
    }
    
    public void prepopulate(StsDelivery obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(StsDelivery obj, int level) {
        int x;
        int tot;
        if (add(obj, StsDelivery.P_DeliveryService)) {
            // deliveryService
            hub = ModelDelegate.getDeliveryServices();
            if (Math.random() < .85) {
                x = (int) (Math.random()*hub.getSize());
                obj.setDeliveryService((DeliveryService) hub.getAt(x));
            }
            done(obj, StsDelivery.P_DeliveryService);
        }
        if (add(obj, StsDelivery.P_StoreToStoreTransfer)) {
            // storeToStoreTransfer
            //    owned
            done(obj, StsDelivery.P_StoreToStoreTransfer);
        }
    }
    
    public StsdItem createStsdItem() {
        StsdItem stsdItem = new StsdItem();
        return stsdItem;
    }
    
    public void prepopulate(StsdItem obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(StsdItem obj, int level) {
        int x;
        int tot;
        if (add(obj, StsdItem.P_StsDelivery)) {
            // stsDelivery
            //    owned
            done(obj, StsdItem.P_StsDelivery);
        }
        if (add(obj, StsdItem.P_StsItem)) {
            // stsItem
            StsItem stsItem = null;
            stsItem = (StsItem) OARuntime.oa(obj).internal().objects().cache().getRandom(StsItem.class, 500);
            if (stsItem != null) obj.setStsItem(stsItem);
            done(obj, StsdItem.P_StsItem);
        }
    }
    
    public StsdItemEach createStsdItemEach() {
        StsdItemEach stsdItemEach = new StsdItemEach();
        return stsdItemEach;
    }
    
    public void prepopulate(StsdItemEach obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(StsdItemEach obj, int level) {
        int x;
        int tot;
        if (add(obj, StsdItemEach.P_StsdItem)) {
            // stsdItem
            //    owned
            done(obj, StsdItemEach.P_StsdItem);
        }
    }
    
    public StsItem createStsItem() {
        StsItem stsItem = new StsItem();
        return stsItem;
    }
    
    public void prepopulate(StsItem obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(StsItem obj, int level) {
        int x;
        int tot;
        if (add(obj, StsItem.P_Item)) {
            // item
            Item item = null;
            item = (Item) OARuntime.oa(obj).internal().objects().cache().getRandom(Item.class, 500);
            if (item != null) obj.setItem(item);
            done(obj, StsItem.P_Item);
        }
        if (add(obj, StsItem.P_StoreToStoreTransfer)) {
            // storeToStoreTransfer
            //    owned
            done(obj, StsItem.P_StoreToStoreTransfer);
        }
    }
    
    public TeamMember createTeamMember() {
        TeamMember teamMember = new TeamMember();
        return teamMember;
    }
    
    public void prepopulate(TeamMember obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(TeamMember obj, int level) {
        int x;
        int tot;
        if (add(obj, TeamMember.P_AppUser)) {
            // appUser
            hub = ModelDelegate.getAppUsers();
            if (Math.random() < .85) {
                x = (int) (Math.random()*hub.getSize());
                obj.setAppUser((AppUser) hub.getAt(x));
            }
            done(obj, TeamMember.P_AppUser);
        }
        if (add(obj, TeamMember.P_Store)) {
            // store
            //    owned
            done(obj, TeamMember.P_Store);
        }
    }
    
    public Till createTill() {
        Till till = new Till();
        return till;
    }
    
    public void prepopulate(Till obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(Till obj, int level) {
        int x;
        int tot;
        if (add(obj, Till.P_Store)) {
            // store
            //    owned
            done(obj, Till.P_Store);
        }
    }
    
    public TillLedgerEntry createTillLedgerEntry() {
        TillLedgerEntry tillLedgerEntry = new TillLedgerEntry();
        return tillLedgerEntry;
    }
    
    public void prepopulate(TillLedgerEntry obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(TillLedgerEntry obj, int level) {
        int x;
        int tot;
        if (add(obj, TillLedgerEntry.P_InvoicePayment)) {
            // invoicePayment
            InvoicePayment invoicePayment = null;
            done(obj, TillLedgerEntry.P_InvoicePayment);
        }
        if (add(obj, TillLedgerEntry.P_RefundPayment)) {
            // refundPayment
            RefundPayment refundPayment = null;
            done(obj, TillLedgerEntry.P_RefundPayment);
        }
        if (add(obj, TillLedgerEntry.P_StoreSafeLedgerEntry)) {
            // storeSafeLedgerEntry
            StoreSafeLedgerEntry storeSafeLedgerEntry = null;
            done(obj, TillLedgerEntry.P_StoreSafeLedgerEntry);
        }
        if (add(obj, TillLedgerEntry.P_TeamMember)) {
            // teamMember
            TeamMember teamMember = null;
            done(obj, TillLedgerEntry.P_TeamMember);
        }
        if (add(obj, TillLedgerEntry.P_Till)) {
            // till
            //    owned
            done(obj, TillLedgerEntry.P_Till);
        }
    }
    
    public TMPermission createTMPermission() {
        TMPermission tmPermission = new TMPermission();
        return tmPermission;
    }
    
    public void prepopulate(TMPermission obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(TMPermission obj, int level) {
        int x;
        int tot;
    }
    
    public VehicleMake createVehicleMake() {
        VehicleMake vehicleMake = new VehicleMake();
        return vehicleMake;
    }
    
    public void prepopulate(VehicleMake obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(VehicleMake obj, int level) {
        int x;
        int tot;
    }
    
    public VehicleModel createVehicleModel() {
        VehicleModel vehicleModel = new VehicleModel();
        return vehicleModel;
    }
    
    public void prepopulate(VehicleModel obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(VehicleModel obj, int level) {
        int x;
        int tot;
        if (add(obj, VehicleModel.P_VehicleMake)) {
            // vehicleMake
            //    owned
            done(obj, VehicleModel.P_VehicleMake);
        }
    }
    
    public VehicleModelPackage createVehicleModelPackage() {
        VehicleModelPackage vehicleModelPackage = new VehicleModelPackage();
        return vehicleModelPackage;
    }
    
    public void prepopulate(VehicleModelPackage obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(VehicleModelPackage obj, int level) {
        int x;
        int tot;
        if (add(obj, VehicleModelPackage.P_VehicleModel)) {
            // vehicleModel
            //    owned
            done(obj, VehicleModelPackage.P_VehicleModel);
        }
        if (add(obj, VehicleModelPackage.P_VehicleModelYear)) {
            // vehicleModelYear
            VehicleModelYear vehicleModelYear = null;
            vehicleModelYear = (VehicleModelYear) OARuntime.oa(obj).internal().objects().cache().getRandom(VehicleModelYear.class, 500);
            if (vehicleModelYear != null) obj.setVehicleModelYear(vehicleModelYear);
            done(obj, VehicleModelPackage.P_VehicleModelYear);
        }
        if (add(obj, VehicleModelPackage.P_VinLookup)) {
            // vinLookup
            VinLookup vinLookup = null;
            vinLookup = (VinLookup) OARuntime.oa(obj).internal().objects().cache().getRandom(VinLookup.class, 500);
            if (vinLookup != null) obj.setVinLookup(vinLookup);
            done(obj, VehicleModelPackage.P_VinLookup);
        }
    }
    
    public VehicleModelYear createVehicleModelYear() {
        VehicleModelYear vehicleModelYear = new VehicleModelYear();
        return vehicleModelYear;
    }
    
    public void prepopulate(VehicleModelYear obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(VehicleModelYear obj, int level) {
        int x;
        int tot;
        if (add(obj, VehicleModelYear.P_VehicleModel)) {
            // vehicleModel
            VehicleModel vehicleModel = null;
            vehicleModel = (VehicleModel) OARuntime.oa(obj).internal().objects().cache().getRandom(VehicleModel.class, 500);
            if (vehicleModel != null) obj.setVehicleModel(vehicleModel);
            done(obj, VehicleModelYear.P_VehicleModel);
        }
    }
    
    public VertexConnector createVertexConnector() {
        VertexConnector vertexConnector = new VertexConnector();
        return vertexConnector;
    }
    
    public void prepopulate(VertexConnector obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(VertexConnector obj, int level) {
        int x;
        int tot;
    }
    
    public VertexTaxCode createVertexTaxCode() {
        VertexTaxCode vertexTaxCode = new VertexTaxCode();
        return vertexTaxCode;
    }
    
    public void prepopulate(VertexTaxCode obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(VertexTaxCode obj, int level) {
        int x;
        int tot;
    }
    
    public VertexTaxCodeRate createVertexTaxCodeRate() {
        VertexTaxCodeRate vertexTaxCodeRate = new VertexTaxCodeRate();
        return vertexTaxCodeRate;
    }
    
    public void prepopulate(VertexTaxCodeRate obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(VertexTaxCodeRate obj, int level) {
        int x;
        int tot;
        if (add(obj, VertexTaxCodeRate.P_VertexTaxCode)) {
            // vertexTaxCode
            //    owned
            done(obj, VertexTaxCodeRate.P_VertexTaxCode);
        }
    }
    
    public VinLookup createVinLookup() {
        VinLookup vinLookup = new VinLookup();
        return vinLookup;
    }
    
    public void prepopulate(VinLookup obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(VinLookup obj, int level) {
        int x;
        int tot;
        if (add(obj, VinLookup.P_VehicleModelYear)) {
            // vehicleModelYear
            VehicleModelYear vehicleModelYear = null;
            vehicleModelYear = (VehicleModelYear) OARuntime.oa(obj).internal().objects().cache().getRandom(VehicleModelYear.class, 500);
            if (vehicleModelYear != null) obj.setVehicleModelYear(vehicleModelYear);
            done(obj, VinLookup.P_VehicleModelYear);
        }
    }
    
    public ZipCodeLookupConnector createZipCodeLookupConnector() {
        ZipCodeLookupConnector zipCodeLookupConnector = new ZipCodeLookupConnector();
        return zipCodeLookupConnector;
    }
    
    public void prepopulate(ZipCodeLookupConnector obj) {
        prepopulate(obj, 0);
    }
    public void prepopulate(ZipCodeLookupConnector obj, int level) {
        int x;
        int tot;
    }
    
    public void populate(Address obj) {
        populate(obj, 0);
    }
    public void populate(Address obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setName(OAString.getDummyText(18, 0, 50));
        obj.setAddress1(OAString.getDummyText(18, 0, 50));
        obj.setAddress2(OAString.getDummyText(18, 0, 50));
        obj.setCity(OAString.getDummyText(18, 0, 50));
        obj.setState(OAString.getDummyText(18, 0, 30));
        obj.setZip(OAString.getDummyText(5, 0, 20));
        obj.setZip4(OAString.getDummyText(4, 0, 4));
        obj.setType((int) (Math.random() * 4));
        obj.setGIS(OAString.getDummyText(22, 0, 120));
        obj.setTimezone(null);
    }
    
    public void populate(AppServer obj) {
        populate(obj, 0);
    }
    public void populate(AppServer obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setStarted((new OADateTime()).plusDays((int) (Math.random() * 1000)));
        obj.setDemoMode(Math.random() < .5 ? true : false);
        obj.setTestOnly(Math.random() < .5 ? true : false);
        obj.setRelease(OAString.getDummyText(12, 0, 18));
        if (add(obj, AppServer.P_Reports)) {
            // reports
            tot = ((int) (Math.random()*4));
            tot -= obj.getReports().size();
            for (int cnt=0; cnt<tot; cnt++) {
                Report report = null;
                if (Math.random() < .75) {
                    report = (Report) OARuntime.oa(obj).internal().objects().cache().getRandom(Report.class, 500);
                    if (report != null) obj.getReports().add(report);
                }
                if (report == null) {
                    report = createReport();
                    obj.getReports().add(report);
                    populate(report);
                }
            }
            done(obj, AppServer.P_Reports);
        }
    }
    
    public void populate(AppUser obj) {
        populate(obj, 0);
    }
    public void populate(AppUser obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        obj.setLoginId(OAString.getDummyText(14, 0, 35));
        obj.setPassword(OAString.getDummyText(12, 0, 50));
        obj.setAdmin(Math.random() < .5 ? true : false);
        obj.setSuperAdmin(Math.random() < .5 ? true : false);
        obj.setEditProcessed(Math.random() < .5 ? true : false);
        obj.setFirstName(OAString.getDummyText(12, 0, 35));
        obj.setLastName(OAString.getDummyText(22, 0, 55));
        if (Math.random() < .8) obj.setInactiveDate((OADate)(new OADate()).plusDays((int) (Math.random() * 1000)));
        obj.setNote(OAString.getDummyText(20, 0, 500));
        if (add(obj, AppUser.P_AppUserLogins)) {
            // appUserLogins
            tot = ((int) (Math.random()*4));
            tot -= obj.getAppUserLogins().size();
            for (int cnt=0; cnt<tot; cnt++) {
                AppUserLogin appUserLogin = null;
                appUserLogin = createAppUserLogin();
                obj.getAppUserLogins().add(appUserLogin);
                populate(appUserLogin);
            }
            done(obj, AppUser.P_AppUserLogins);
        }
        if (add(obj, AppUser.P_Reports)) {
            // reports
            tot = ((int) (Math.random()*4));
            tot -= obj.getReports().size();
            for (int cnt=0; cnt<tot; cnt++) {
                Report report = null;
                if (Math.random() < .75) {
                    report = (Report) OARuntime.oa(obj).internal().objects().cache().getRandom(Report.class, 500);
                    if (report != null) obj.getReports().add(report);
                }
                if (report == null) {
                    report = createReport();
                    obj.getReports().add(report);
                    populate(report);
                }
            }
            done(obj, AppUser.P_Reports);
        }
    }
    
    public void populate(AppUserError obj) {
        populate(obj, 0);
    }
    public void populate(AppUserError obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        obj.setDateTime((new OADateTime()).plusDays((int) (Math.random() * 1000)));
        obj.setMessage(OAString.getDummyText(35, 0, 250));
        obj.setStackTrace(OAString.getDummyText(40, 0, 500));
        obj.setReviewed((OADate)(new OADate()).plusDays((int) (Math.random() * 1000)));
        obj.setReviewNote(OAString.getDummyText(40, 0, 254));
        if (add(obj, AppUserError.P_Reports)) {
            // reports
            tot = ((int) (Math.random()*4));
            tot -= obj.getReports().size();
            for (int cnt=0; cnt<tot; cnt++) {
                Report report = null;
                if (Math.random() < .75) {
                    report = (Report) OARuntime.oa(obj).internal().objects().cache().getRandom(Report.class, 500);
                    if (report != null) obj.getReports().add(report);
                }
                if (report == null) {
                    report = createReport();
                    obj.getReports().add(report);
                    populate(report);
                }
            }
            done(obj, AppUserError.P_Reports);
        }
    }
    
    public void populate(AppUserLogin obj) {
        populate(obj, 0);
    }
    public void populate(AppUserLogin obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setLocation(OAString.getDummyText(18, 0, 50));
        obj.setComputerName(OAString.getDummyText(14, 0, 50));
        obj.setDisconnected((new OADateTime()).plusDays((int) (Math.random() * 1000)));
        obj.setConnectionId((int) (Math.random() * 900));
        obj.setHostName(OAString.getDummyText(14, 0, 50));
        obj.setIpAddress(OAString.getDummyText(15, 0, 20));
        obj.setTotalMemory((long) (Math.random() * 900));
        obj.setFreeMemory((long) (Math.random() * 900));
        if (add(obj, AppUserLogin.P_AppUserErrors)) {
            // appUserErrors
            tot = ((int) (Math.random()*4));
            tot -= obj.getAppUserErrors().size();
            for (int cnt=0; cnt<tot; cnt++) {
                AppUserError appUserError = null;
                appUserError = createAppUserError();
                obj.getAppUserErrors().add(appUserError);
                populate(appUserError);
            }
            done(obj, AppUserLogin.P_AppUserErrors);
        }
        if (add(obj, AppUserLogin.P_Reports)) {
            // reports
            tot = ((int) (Math.random()*4));
            tot -= obj.getReports().size();
            for (int cnt=0; cnt<tot; cnt++) {
                Report report = null;
                if (Math.random() < .75) {
                    report = (Report) OARuntime.oa(obj).internal().objects().cache().getRandom(Report.class, 500);
                    if (report != null) obj.getReports().add(report);
                }
                if (report == null) {
                    report = createReport();
                    obj.getReports().add(report);
                    populate(report);
                }
            }
            done(obj, AppUserLogin.P_Reports);
        }
    }
    
    public void populate(BackroomMap obj) {
        populate(obj, 0);
    }
    public void populate(BackroomMap obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
    }
    
    public void populate(BankDeposit obj) {
        populate(obj, 0);
    }
    public void populate(BankDeposit obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setCash((double) (Math.random() * 100));
        obj.setReferenceCode(OAString.getDummyText(18, 0, 50));
        obj.setConfirmed((new OADateTime()).plusDays((int) (Math.random() * 1000)));
        if (add(obj, BankDeposit.P_BankDepositChecks)) {
            // bankDepositChecks
            tot = ((int) (Math.random()*4));
            tot -= obj.getBankDepositChecks().size();
            for (int cnt=0; cnt<tot; cnt++) {
                BankDepositCheck bankDepositCheck = null;
                if (Math.random() < .75) {
                    bankDepositCheck = (BankDepositCheck) OARuntime.oa(obj).internal().objects().cache().getRandom(BankDepositCheck.class, 500);
                    if (bankDepositCheck != null) obj.getBankDepositChecks().add(bankDepositCheck);
                }
                if (bankDepositCheck == null) {
                    bankDepositCheck = createBankDepositCheck();
                    obj.getBankDepositChecks().add(bankDepositCheck);
                    populate(bankDepositCheck);
                }
            }
            done(obj, BankDeposit.P_BankDepositChecks);
        }
    }
    
    public void populate(BankDepositCheck obj) {
        populate(obj, 0);
    }
    public void populate(BankDepositCheck obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setCleared((new OADateTime()).plusDays((int) (Math.random() * 1000)));
        obj.setRejected((new OADateTime()).plusDays((int) (Math.random() * 1000)));
        obj.setRejectedFeeCollected((new OADateTime()).plusDays((int) (Math.random() * 1000)));
        obj.setFeeAmountCollected((double) (Math.random() * 100));
    }
    
    public void populate(BarcodeType obj) {
        populate(obj, 0);
    }
    public void populate(BarcodeType obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setName(OAString.getDummyText(18, 0, 50));
        obj.setType((int) (Math.random() * 6));
        obj.setRule((int) (Math.random() * 1));
    }
    
    public void populate(Catalog obj) {
        populate(obj, 0);
    }
    public void populate(Catalog obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        if (add(obj, Catalog.P_CatalogCategories)) {
            // catalogCategories
            tot = ((int) (Math.random()*4));
            tot -= obj.getCatalogCategories().size();
            for (int cnt=0; cnt<tot; cnt++) {
                CatalogCategory catalogCategory = null;
                if (Math.random() < .75) {
                    catalogCategory = (CatalogCategory) OARuntime.oa(obj).internal().objects().cache().getRandom(CatalogCategory.class, 500);
                    if (catalogCategory != null) obj.getCatalogCategories().add(catalogCategory);
                }
                if (catalogCategory == null) {
                    catalogCategory = createCatalogCategory();
                    obj.getCatalogCategories().add(catalogCategory);
                    populate(catalogCategory);
                }
            }
            done(obj, Catalog.P_CatalogCategories);
        }
    }
    
    public void populate(CatalogCategory obj) {
        populate(obj, 0);
    }
    public void populate(CatalogCategory obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setName(OAString.getDummyText(18, 0, 50));
        if (add(obj, CatalogCategory.P_CatalogCategories)) {
            // catalogCategories
            if (level < 3) {
                tot = ((int) (Math.random()*3));
                tot -= obj.getCatalogCategories().size();
                for (int cnt=0; cnt<tot; cnt++) {
                    CatalogCategory catalogCategory = null;
                    catalogCategory = createCatalogCategory();
                    obj.getCatalogCategories().add(catalogCategory);
                    populate(catalogCategory, level+1);
                }
            }
            done(obj, CatalogCategory.P_CatalogCategories);
        }
        if (add(obj, CatalogCategory.P_CatalogItems)) {
            // catalogItems
            tot = ((int) (Math.random()*4));
            tot -= obj.getCatalogItems().size();
            for (int cnt=0; cnt<tot; cnt++) {
                CatalogItem catalogItem = null;
                if (Math.random() < .75) {
                    catalogItem = (CatalogItem) OARuntime.oa(obj).internal().objects().cache().getRandom(CatalogItem.class, 500);
                    if (catalogItem != null) obj.getCatalogItems().add(catalogItem);
                }
                if (catalogItem == null) {
                    catalogItem = createCatalogItem();
                    obj.getCatalogItems().add(catalogItem);
                    populate(catalogItem);
                }
            }
            done(obj, CatalogCategory.P_CatalogItems);
        }
    }
    
    public void populate(CatalogItem obj) {
        populate(obj, 0);
    }
    public void populate(CatalogItem obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setName(OAString.getDummyText(18, 0, 50));
        if (add(obj, CatalogItem.P_RootCatalogCategories)) {
            // rootCatalogCategories
            tot = ((int) (Math.random()*4));
            tot -= obj.getRootCatalogCategories().size();
            for (int cnt=0; cnt<tot; cnt++) {
                CatalogCategory catalogCategory = null;
                if (Math.random() < .75) {
                    catalogCategory = (CatalogCategory) OARuntime.oa(obj).internal().objects().cache().getRandom(CatalogCategory.class, 500);
                    if (catalogCategory != null) obj.getRootCatalogCategories().add(catalogCategory);
                }
                if (catalogCategory == null) {
                    catalogCategory = createCatalogCategory();
                    obj.getRootCatalogCategories().add(catalogCategory);
                    populate(catalogCategory);
                }
            }
            done(obj, CatalogItem.P_RootCatalogCategories);
        }
        if (add(obj, CatalogItem.P_VehicleModelPackages)) {
            // vehicleModelPackages
            tot = ((int) (Math.random()*4));
            tot -= obj.getVehicleModelPackages().size();
            for (int cnt=0; cnt<tot; cnt++) {
                hub = (Hub) obj.getProperty(OAString.cpp(CatalogItem.P_VehicleModels, VehicleModel.P_VehicleModelPackages));
                if (hub != null) {
                    x = (int) (Math.random()*hub.getSize());
                    obj.getVehicleModelPackages().add((VehicleModelPackage) hub.getAt(x));
                }
            }
            done(obj, CatalogItem.P_VehicleModelPackages);
        }
        if (add(obj, CatalogItem.P_VehicleModels)) {
            // vehicleModels
            tot = ((int) (Math.random()*4));
            tot -= obj.getVehicleModels().size();
            for (int cnt=0; cnt<tot; cnt++) {
                VehicleModel vehicleModel = null;
                vehicleModel = (VehicleModel) OARuntime.oa(obj).internal().objects().cache().getRandom(VehicleModel.class, 500);
                if (vehicleModel != null) obj.getVehicleModels().add(vehicleModel);
            }
            done(obj, CatalogItem.P_VehicleModels);
        }
    }
    
    public void populate(Core obj) {
        populate(obj, 0);
    }
    public void populate(Core obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setSerialCode(OAString.getDummyText(15, 0, 75));
    }
    
    public void populate(CronProcess obj) {
        populate(obj, 0);
    }
    public void populate(CronProcess obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setDescription(OAString.getDummyText(22, 0, 120));
        // enabled has a default value
        obj.setLastBegin((new OADateTime()).plusDays((int) (Math.random() * 1000)));
        obj.setLastEnd((new OADateTime()).plusDays((int) (Math.random() * 1000)));
        obj.setConsole(OAString.getDummyText(50, 0, 175));
    }
    
    public void populate(CurrencyDenomination obj) {
        populate(obj, 0);
    }
    public void populate(CurrencyDenomination obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setType((int) (Math.random() * 5));
        obj.setName(OAString.getDummyText(18, 0, 30));
        obj.setUnitValue((double) (Math.random() * 100));
        if (add(obj, CurrencyDenomination.P_DenominationBundles)) {
            // denominationBundles
            tot = ((int) (Math.random()*4));
            tot -= obj.getDenominationBundles().size();
            for (int cnt=0; cnt<tot; cnt++) {
                DenominationBundle denominationBundle = null;
                denominationBundle = createDenominationBundle();
                obj.getDenominationBundles().add(denominationBundle);
                populate(denominationBundle);
            }
            done(obj, CurrencyDenomination.P_DenominationBundles);
        }
    }
    
    public void populate(CurrencyExchangeRate obj) {
        populate(obj, 0);
    }
    public void populate(CurrencyExchangeRate obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setRate((double) (Math.random() * 100));
        obj.setBeginDate((OADate)(new OADate()).plusDays((int) (Math.random() * 1000)));
        obj.setEndDate((OADate)(new OADate()).plusDays((int) (Math.random() * 1000)));
    }
    
    public void populate(CurrencyType obj) {
        populate(obj, 0);
    }
    public void populate(CurrencyType obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setCode(OAString.getDummyText(5, 0, 12));
        obj.setName(OAString.getDummyText(18, 0, 30));
        obj.setDescription(OAString.getDummyText(22, 0, 120));
        obj.setSymbol(OAString.getDummyText(10, 0, 10));
        obj.setJavaFormatCode(OAString.getDummyText(10, 0, 10));
        obj.setMinorUnit((int) (Math.random() * 900));
        obj.setRoundingRule((int) (Math.random() * 4));
        if (add(obj, CurrencyType.P_CurrencyDenominations)) {
            // currencyDenominations
            tot = ((int) (Math.random()*4));
            tot -= obj.getCurrencyDenominations().size();
            for (int cnt=0; cnt<tot; cnt++) {
                CurrencyDenomination currencyDenomination = null;
                currencyDenomination = createCurrencyDenomination();
                obj.getCurrencyDenominations().add(currencyDenomination);
                populate(currencyDenomination);
            }
            done(obj, CurrencyType.P_CurrencyDenominations);
        }
        if (add(obj, CurrencyType.P_CurrencyExchangeRates)) {
            // currencyExchangeRates
            tot = ((int) (Math.random()*4));
            tot -= obj.getCurrencyExchangeRates().size();
            for (int cnt=0; cnt<tot; cnt++) {
                CurrencyExchangeRate currencyExchangeRate = null;
                if (Math.random() < .75) {
                    currencyExchangeRate = (CurrencyExchangeRate) OARuntime.oa(obj).internal().objects().cache().getRandom(CurrencyExchangeRate.class, 500);
                    if (currencyExchangeRate != null) obj.getCurrencyExchangeRates().add(currencyExchangeRate);
                }
                if (currencyExchangeRate == null) {
                    currencyExchangeRate = createCurrencyExchangeRate();
                    obj.getCurrencyExchangeRates().add(currencyExchangeRate);
                    populate(currencyExchangeRate);
                }
            }
            done(obj, CurrencyType.P_CurrencyExchangeRates);
        }
    }
    
    public void populate(Customer obj) {
        populate(obj, 0);
    }
    public void populate(Customer obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setName(OAString.getDummyText(22, 0, 75));
        obj.setType((int) (Math.random() * 5));
        obj.setInputMask(OAString.getDummyText(18, 0, 30));
        if (add(obj, Customer.P_Addresses)) {
            // addresses
            tot = ((int) (Math.random()*4));
            tot -= obj.getAddresses().size();
            for (int cnt=0; cnt<tot; cnt++) {
                Address address = null;
                if (Math.random() < .75) {
                    address = (Address) OARuntime.oa(obj).internal().objects().cache().getRandom(Address.class, 500);
                    if (address != null) obj.getAddresses().add(address);
                }
                if (address == null) {
                    address = createAddress();
                    obj.getAddresses().add(address);
                    populate(address);
                }
            }
            done(obj, Customer.P_Addresses);
        }
        if (add(obj, Customer.P_Invoices)) {
            // invoices
            tot = ((int) (Math.random()*4));
            tot -= obj.getInvoices().size();
            for (int cnt=0; cnt<tot; cnt++) {
                Invoice invoice = null;
                if (Math.random() < .75) {
                    invoice = (Invoice) OARuntime.oa(obj).internal().objects().cache().getRandom(Invoice.class, 500);
                    if (invoice != null) obj.getInvoices().add(invoice);
                }
                if (invoice == null) {
                    invoice = createInvoice();
                    obj.getInvoices().add(invoice);
                    populate(invoice);
                }
            }
            done(obj, Customer.P_Invoices);
        }
        if (add(obj, Customer.P_Quotes)) {
            // quotes
            tot = ((int) (Math.random()*4));
            tot -= obj.getQuotes().size();
            for (int cnt=0; cnt<tot; cnt++) {
                Quote quote = null;
                if (Math.random() < .75) {
                    quote = (Quote) OARuntime.oa(obj).internal().objects().cache().getRandom(Quote.class, 500);
                    if (quote != null) obj.getQuotes().add(quote);
                }
                if (quote == null) {
                    quote = createQuote();
                    obj.getQuotes().add(quote);
                    populate(quote);
                }
            }
            done(obj, Customer.P_Quotes);
        }
    }
    
    public void populate(CustomerConnector obj) {
        populate(obj, 0);
    }
    public void populate(CustomerConnector obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
    }
    
    public void populate(CustomerCredit obj) {
        populate(obj, 0);
    }
    public void populate(CustomerCredit obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setLimit((double) (Math.random() * 100));
    }
    
    public void populate(DcToStore obj) {
        populate(obj, 0);
    }
    public void populate(DcToStore obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
    }
    
    public void populate(DeliveryService obj) {
        populate(obj, 0);
    }
    public void populate(DeliveryService obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setName(OAString.getDummyText(22, 0, 75));
    }
    
    public void populate(Demo obj) {
        populate(obj, 0);
    }
    public void populate(Demo obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setStarted((new OADateTime()).plusDays((int) (Math.random() * 1000)));
        obj.setPaused((new OADateTime()).plusDays((int) (Math.random() * 1000)));
        obj.setStopped((new OADateTime()).plusDays((int) (Math.random() * 1000)));
        obj.setConsole(OAString.getDummyText(20, 0, 0));
        if (add(obj, Demo.P_DemoNodes)) {
            // demoNodes
            tot = ((int) (Math.random()*4));
            tot -= obj.getDemoNodes().size();
            for (int cnt=0; cnt<tot; cnt++) {
                DemoNode demoNode = null;
                demoNode = createDemoNode();
                obj.getDemoNodes().add(demoNode);
                populate(demoNode);
            }
            done(obj, Demo.P_DemoNodes);
        }
    }
    
    public void populate(DemoNode obj) {
        populate(obj, 0);
    }
    public void populate(DemoNode obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setType((int) (Math.random() * 7));
        obj.setName(OAString.getDummyText(20, 0, 0));
        obj.setStarted((new OADateTime()).plusDays((int) (Math.random() * 1000)));
        obj.setPaused((new OADateTime()).plusDays((int) (Math.random() * 1000)));
        obj.setStopped((new OADateTime()).plusDays((int) (Math.random() * 1000)));
        obj.setDisconnect((new OADateTime()).plusDays((int) (Math.random() * 1000)));
        obj.setShowOutput(Math.random() < .5 ? true : false);
        obj.setConsole(OAString.getDummyText(0, 0, 0));
    }
    
    public void populate(DenominationBundle obj) {
        populate(obj, 0);
    }
    public void populate(DenominationBundle obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setName(OAString.getDummyText(18, 0, 30));
        obj.setType((int) (Math.random() * 5));
        obj.setPackSize((int) (Math.random() * 900));
    }
    
    public void populate(DepositSeal obj) {
        populate(obj, 0);
    }
    public void populate(DepositSeal obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setSealNumber(OAString.getDummyText(20, 0, 20));
        obj.setIssuedTo(OAString.getDummyText(20, 0, 0));
        obj.setUsedOn((OADate)(new OADate()).plusDays((int) (Math.random() * 1000)));
        obj.setStatus((int) (Math.random() * 4));
    }
    
    public void populate(DiscountCoupon obj) {
        populate(obj, 0);
    }
    public void populate(DiscountCoupon obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setAmount((double) (Math.random() * 100));
        obj.setReference(OAString.getDummyText(18, 0, 35));
    }
    
    public void populate(DiscountType obj) {
        populate(obj, 0);
    }
    public void populate(DiscountType obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setType((int) (Math.random() * 4));
        obj.setType2((int) (Math.random() * 4));
        obj.setName(OAString.getDummyText(18, 0, 50));
    }
    
    public void populate(DistCenter obj) {
        populate(obj, 0);
    }
    public void populate(DistCenter obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
    }
    
    public void populate(Feedback obj) {
        populate(obj, 0);
    }
    public void populate(Feedback obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
    }
    
    public void populate(Garage obj) {
        populate(obj, 0);
    }
    public void populate(Garage obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        if (add(obj, Garage.P_GarageVehicles)) {
            // garageVehicles
            tot = ((int) (Math.random()*4));
            tot -= obj.getGarageVehicles().size();
            for (int cnt=0; cnt<tot; cnt++) {
                GarageVehicle garageVehicle = null;
                garageVehicle = createGarageVehicle();
                obj.getGarageVehicles().add(garageVehicle);
                populate(garageVehicle);
            }
            done(obj, Garage.P_GarageVehicles);
        }
    }
    
    public void populate(GarageVehicle obj) {
        populate(obj, 0);
    }
    public void populate(GarageVehicle obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
    }
    
    public void populate(ImageStore obj) {
        populate(obj, 0);
    }
    public void populate(ImageStore obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setBytes(new byte[0]);
        obj.setOrigFileName(OAString.getDummyText(30, 0, 250));
    }
    
    public void populate(InventoryConnector obj) {
        populate(obj, 0);
    }
    public void populate(InventoryConnector obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
    }
    
    public void populate(Invoice obj) {
        populate(obj, 0);
    }
    public void populate(Invoice obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setCompleted((new OADateTime()).plusDays((int) (Math.random() * 1000)));
        if (add(obj, Invoice.P_InvoiceBaskets)) {
            // invoiceBaskets
            tot = ((int) (Math.random()*4));
            tot -= obj.getInvoiceBaskets().size();
            for (int cnt=0; cnt<tot; cnt++) {
                InvoiceBasket invoiceBasket = null;
                invoiceBasket = createInvoiceBasket();
                obj.getInvoiceBaskets().add(invoiceBasket);
                populate(invoiceBasket);
            }
            done(obj, Invoice.P_InvoiceBaskets);
        }
        if (add(obj, Invoice.P_InvoicePayments)) {
            // invoicePayments
            tot = ((int) (Math.random()*4));
            tot -= obj.getInvoicePayments().size();
            for (int cnt=0; cnt<tot; cnt++) {
                InvoicePayment invoicePayment = null;
                invoicePayment = createInvoicePayment();
                obj.getInvoicePayments().add(invoicePayment);
                populate(invoicePayment);
            }
            done(obj, Invoice.P_InvoicePayments);
        }
        if (add(obj, Invoice.P_PurchaseOrders)) {
            // purchaseOrders
            tot = ((int) (Math.random()*4));
            tot -= obj.getPurchaseOrders().size();
            for (int cnt=0; cnt<tot; cnt++) {
                PurchaseOrder purchaseOrder = null;
                if (Math.random() < .75) {
                    purchaseOrder = (PurchaseOrder) OARuntime.oa(obj).internal().objects().cache().getRandom(PurchaseOrder.class, 500);
                    if (purchaseOrder != null) obj.getPurchaseOrders().add(purchaseOrder);
                }
                if (purchaseOrder == null) {
                    purchaseOrder = createPurchaseOrder();
                    obj.getPurchaseOrders().add(purchaseOrder);
                    populate(purchaseOrder);
                }
            }
            done(obj, Invoice.P_PurchaseOrders);
        }
        if (add(obj, Invoice.P_RefundInvoices)) {
            // refundInvoices
            //   will be created by Refund
            done(obj, Invoice.P_RefundInvoices);
        }
    }
    
    public void populate(InvoiceBasket obj) {
        populate(obj, 0);
    }
    public void populate(InvoiceBasket obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        if (add(obj, InvoiceBasket.P_LineItems)) {
            // lineItems
            tot = ((int) (Math.random()*4));
            tot -= obj.getLineItems().size();
            for (int cnt=0; cnt<tot; cnt++) {
                LineItem lineItems = createLineItem();
                obj.getLineItems().add(lineItems);
                populate(lineItems);
            }
            done(obj, InvoiceBasket.P_LineItems);
        }
    }
    
    public void populate(InvoiceDiscount obj) {
        populate(obj, 0);
    }
    public void populate(InvoiceDiscount obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setName(OAString.getDummyText(18, 0, 30));
        obj.setType((int) (Math.random() * 3));
        obj.setAmount((double) (Math.random() * 100));
        obj.setPercentage((double) (Math.random() * 100));
    }
    
    public void populate(InvoicePayment obj) {
        populate(obj, 0);
    }
    public void populate(InvoicePayment obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setType((int) (Math.random() * 6));
        obj.setInputCode(OAString.getDummyText(22, 0, 75));
        obj.setOutputCode(OAString.getDummyText(22, 0, 75));
        obj.setAmount((double) (Math.random() * 100));
        obj.setCashIn((double) (Math.random() * 100));
        obj.setCashOut((double) (Math.random() * 100));
        obj.setApplied((new OADateTime()).plusDays((int) (Math.random() * 1000)));
        if (add(obj, InvoicePayment.P_RefundPayments)) {
            // refundPayments
            //   will be created by RefundInvoice
            done(obj, InvoicePayment.P_RefundPayments);
        }
    }
    
    public void populate(InvoicePaymentCheck obj) {
        populate(obj, 0);
    }
    public void populate(InvoicePaymentCheck obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setLocation((int) (Math.random() * 7));
        obj.setCheckNumber((int) (Math.random() * 900));
        obj.setBankName(OAString.getDummyText(20, 0, 0));
        obj.setRoutingNumber(OAString.getDummyText(20, 0, 20));
        obj.setAccountNumber(OAString.getDummyText(20, 0, 20));
        obj.setCheckDate((OADate)(new OADate()).plusDays((int) (Math.random() * 1000)));
        obj.setStatus((int) (Math.random() * 5));
        obj.setClearDate((OADate)(new OADate()).plusDays((int) (Math.random() * 1000)));
        obj.setBouncedDate((OADate)(new OADate()).plusDays((int) (Math.random() * 1000)));
        obj.setBouncedReason(OAString.getDummyText(20, 0, 0));
        obj.setLicenseNumber(OAString.getDummyText(20, 0, 20));
        obj.setLicenseState(OAString.getDummyText(20, 0, 0));
    }
    
    public void populate(InvoiceRebate obj) {
        populate(obj, 0);
    }
    public void populate(InvoiceRebate obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
    }
    
    public void populate(InvoiceShipTo obj) {
        populate(obj, 0);
    }
    public void populate(InvoiceShipTo obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
    }
    
    public void populate(InvoiceTax obj) {
        populate(obj, 0);
    }
    public void populate(InvoiceTax obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setType((int) (Math.random() * 1));
    }
    
    public void populate(Item obj) {
        populate(obj, 0);
    }
    public void populate(Item obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setCode(OAString.getDummyText(10, 0, 10));
        obj.setName(OAString.getDummyText(22, 0, 75));
        obj.setBrand(OAString.getDummyText(12, 0, 50));
        obj.setDescription(OAString.getDummyText(30, 0, 500));
        obj.setUseSerialCode(Math.random() < .5 ? true : false);
        obj.setSerialCodeMask(OAString.getDummyText(18, 0, 30));
        obj.setKeywords(OAString.getDummyText(22, 0, 250));
        obj.setHtmlDescription(OAString.getDummyText(30, 0, 500));
        obj.setDiscontinued((OADate)(new OADate()).plusDays((int) (Math.random() * 1000)));
        obj.setDiscontinuedReason(OAString.getDummyText(22, 0, 120));
        obj.setStocking(Math.random() < .5 ? true : false);
        obj.setQuantityOnHand((int) (Math.random() * 900));
        obj.setMinQuantityOnHand((int) (Math.random() * 900));
        obj.setMaxQuantityOnHand((int) (Math.random() * 900));
        obj.setShelfLifeInDays((int) (Math.random() * 900));
        obj.setAgeRestricted(Math.random() < .5 ? true : false);
        obj.setMinAge((int) (Math.random() * 900));
        obj.setMaxAge((int) (Math.random() * 900));
        obj.setSaleByWeight(Math.random() < .5 ? true : false);
        obj.setUsedForKitOnly(Math.random() < .5 ? true : false);
        obj.setNotReturnable(Math.random() < .5 ? true : false);
        if (add(obj, Item.P_ItemCategories)) {
            // itemCategories
            tot = ((int) (Math.random()*4));
            tot -= obj.getItemCategories().size();
            for (int cnt=0; cnt<tot; cnt++) {
                hub = ModelDelegate.getItemCategories();
                if (Math.random() < .75) {
                    x = (int) (Math.random()*hub.getSize());
                    obj.getItemCategories().add((ItemCategory) hub.getAt(x));
                }
            }
            done(obj, Item.P_ItemCategories);
        }
        if (add(obj, Item.P_ItemKits)) {
            // itemKits
            tot = ((int) (Math.random()*4));
            tot -= obj.getItemKits().size();
            for (int cnt=0; cnt<tot; cnt++) {
                ItemKit itemKit = null;
                itemKit = createItemKit();
                obj.getItemKits().add(itemKit);
                populate(itemKit);
            }
            done(obj, Item.P_ItemKits);
        }
        if (add(obj, Item.P_ItemOptions)) {
            // itemOptions
            tot = ((int) (Math.random()*4));
            tot -= obj.getItemOptions().size();
            for (int cnt=0; cnt<tot; cnt++) {
                ItemOption itemOptions = createItemOption();
                obj.getItemOptions().add(itemOptions);
                populate(itemOptions);
            }
            done(obj, Item.P_ItemOptions);
        }
        if (add(obj, Item.P_ItemPacks)) {
            // itemPacks
            tot = ((int) (Math.random()*4));
            tot -= obj.getItemPacks().size();
            for (int cnt=0; cnt<tot; cnt++) {
                ItemPack itemPacks = createItemPack();
                obj.getItemPacks().add(itemPacks);
                populate(itemPacks);
            }
            done(obj, Item.P_ItemPacks);
        }
        if (add(obj, Item.P_ItemVariants)) {
            // itemVariants
            tot = ((int) (Math.random()*4));
            tot -= obj.getItemVariants().size();
            for (int cnt=0; cnt<tot; cnt++) {
                ItemVariant itemVariant = null;
                itemVariant = createItemVariant();
                obj.getItemVariants().add(itemVariant);
                populate(itemVariant);
            }
            done(obj, Item.P_ItemVariants);
        }
        if (add(obj, Item.P_ItemVendors)) {
            // itemVendors
            tot = ((int) (Math.random()*4));
            tot -= obj.getItemVendors().size();
            for (int cnt=0; cnt<tot; cnt++) {
                ItemVendor itemVendor = null;
                if (Math.random() < .75) {
                    itemVendor = (ItemVendor) OARuntime.oa(obj).internal().objects().cache().getRandom(ItemVendor.class, 500);
                    if (itemVendor != null) obj.getItemVendors().add(itemVendor);
                }
                if (itemVendor == null) {
                    itemVendor = createItemVendor();
                    obj.getItemVendors().add(itemVendor);
                    populate(itemVendor);
                }
            }
            done(obj, Item.P_ItemVendors);
        }
        if (add(obj, Item.P_OnlineOrderItems)) {
            // onlineOrderItems
            tot = ((int) (Math.random()*4));
            tot -= obj.getOnlineOrderItems().size();
            for (int cnt=0; cnt<tot; cnt++) {
                OnlineOrderItem onlineOrderItem = null;
                onlineOrderItem = (OnlineOrderItem) OARuntime.oa(obj).internal().objects().cache().getRandom(OnlineOrderItem.class, 500);
                if (onlineOrderItem != null) obj.getOnlineOrderItems().add(onlineOrderItem);
            }
            done(obj, Item.P_OnlineOrderItems);
        }
        if (add(obj, Item.P_PriceBookEntries)) {
            // priceBookEntries
            tot = ((int) (Math.random()*4));
            tot -= obj.getPriceBookEntries().size();
            for (int cnt=0; cnt<tot; cnt++) {
                PriceBookEntry priceBookEntry = null;
                priceBookEntry = createPriceBookEntry();
                obj.getPriceBookEntries().add(priceBookEntry);
                populate(priceBookEntry);
            }
            done(obj, Item.P_PriceBookEntries);
        }
        if (add(obj, Item.P_Products)) {
            // products
            tot = ((int) (Math.random()*4));
            tot -= obj.getProducts().size();
            for (int cnt=0; cnt<tot; cnt++) {
                Product product = null;
                product = createProduct();
                obj.getProducts().add(product);
                populate(product);
            }
            done(obj, Item.P_Products);
        }
        if (add(obj, Item.P_VertexTaxCodes)) {
            // vertexTaxCodes
            tot = ((int) (Math.random()*4));
            tot -= obj.getVertexTaxCodes().size();
            for (int cnt=0; cnt<tot; cnt++) {
                hub = ModelDelegate.getVertexTaxCodes();
                if (Math.random() < .75) {
                    x = (int) (Math.random()*hub.getSize());
                    obj.getVertexTaxCodes().add((VertexTaxCode) hub.getAt(x));
                }
            }
            done(obj, Item.P_VertexTaxCodes);
        }
    }
    
    public void populate(ItemAlert obj) {
        populate(obj, 0);
    }
    public void populate(ItemAlert obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setType((int) (Math.random() * 1));
    }
    
    public void populate(ItemCategory obj) {
        populate(obj, 0);
    }
    public void populate(ItemCategory obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setCode(OAString.getDummyText(10, 0, 10));
        obj.setName(OAString.getDummyText(18, 0, 30));
        if (add(obj, ItemCategory.P_Items)) {
            // items
            tot = ((int) (Math.random()*4));
            tot -= obj.getItems().size();
            for (int cnt=0; cnt<tot; cnt++) {
                Item item = null;
                if (Math.random() < .75) {
                    item = (Item) OARuntime.oa(obj).internal().objects().cache().getRandom(Item.class, 500);
                    if (item != null) obj.getItems().add(item);
                }
                if (item == null) {
                    item = createItem();
                    obj.getItems().add(item);
                    populate(item);
                }
            }
            done(obj, ItemCategory.P_Items);
        }
        if (add(obj, ItemCategory.P_SubItemCategories)) {
            // subItemCategories
            if (level < 3) {
                tot = ((int) (Math.random()*3));
                tot -= obj.getSubItemCategories().size();
                for (int cnt=0; cnt<tot; cnt++) {
                    ItemCategory itemCategory = null;
                    itemCategory = createItemCategory();
                    obj.getSubItemCategories().add(itemCategory);
                    populate(itemCategory, level+1);
                }
            }
            done(obj, ItemCategory.P_SubItemCategories);
        }
    }
    
    public void populate(ItemInterchange obj) {
        populate(obj, 0);
    }
    public void populate(ItemInterchange obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
    }
    
    public void populate(ItemKit obj) {
        populate(obj, 0);
    }
    public void populate(ItemKit obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
    }
    
    public void populate(ItemLine obj) {
        populate(obj, 0);
    }
    public void populate(ItemLine obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setCode(OAString.getDummyText(10, 0, 10));
        obj.setName(OAString.getDummyText(18, 0, 50));
        // seq is auto sequence
        if (add(obj, ItemLine.P_Items)) {
            // items
            tot = ((int) (Math.random()*4));
            tot -= obj.getItems().size();
            for (int cnt=0; cnt<tot; cnt++) {
                Item item = null;
                if (Math.random() < .75) {
                    item = (Item) OARuntime.oa(obj).internal().objects().cache().getRandom(Item.class, 500);
                    if (item != null) obj.getItems().add(item);
                }
                if (item == null) {
                    item = createItem();
                    obj.getItems().add(item);
                    populate(item);
                }
            }
            done(obj, ItemLine.P_Items);
        }
    }
    
    public void populate(ItemMSDS obj) {
        populate(obj, 0);
    }
    public void populate(ItemMSDS obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
    }
    
    public void populate(ItemOption obj) {
        populate(obj, 0);
    }
    public void populate(ItemOption obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setName(OAString.getDummyText(18, 0, 50));
        if (add(obj, ItemOption.P_ItemOptionValues)) {
            // itemOptionValues
            tot = ((int) (Math.random()*4));
            tot -= obj.getItemOptionValues().size();
            for (int cnt=0; cnt<tot; cnt++) {
                ItemOptionValue itemOptionValue = null;
                itemOptionValue = createItemOptionValue();
                obj.getItemOptionValues().add(itemOptionValue);
                populate(itemOptionValue);
            }
            done(obj, ItemOption.P_ItemOptionValues);
        }
    }
    
    public void populate(ItemOptionType obj) {
        populate(obj, 0);
    }
    public void populate(ItemOptionType obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setType((int) (Math.random() * 4));
        obj.setName(OAString.getDummyText(14, 0, 50));
        if (add(obj, ItemOptionType.P_ItemOptionTypeValues)) {
            // itemOptionTypeValues
            tot = ((int) (Math.random()*4));
            tot -= obj.getItemOptionTypeValues().size();
            for (int cnt=0; cnt<tot; cnt++) {
                ItemOptionTypeValue itemOptionTypeValue = null;
                itemOptionTypeValue = createItemOptionTypeValue();
                obj.getItemOptionTypeValues().add(itemOptionTypeValue);
                populate(itemOptionTypeValue);
            }
            done(obj, ItemOptionType.P_ItemOptionTypeValues);
        }
    }
    
    public void populate(ItemOptionTypeValue obj) {
        populate(obj, 0);
    }
    public void populate(ItemOptionTypeValue obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setValue(OAString.getDummyText(12, 0, 50));
    }
    
    public void populate(ItemOptionValue obj) {
        populate(obj, 0);
    }
    public void populate(ItemOptionValue obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setValue(OAString.getDummyText(18, 0, 50));
        if (add(obj, ItemOptionValue.P_ItemVariants)) {
            // itemVariants
            tot = ((int) (Math.random()*4));
            tot -= obj.getItemVariants().size();
            for (int cnt=0; cnt<tot; cnt++) {
                ItemVariant itemVariant = null;
            }
            done(obj, ItemOptionValue.P_ItemVariants);
        }
        if (add(obj, ItemOptionValue.P_PriceBookEntries)) {
            // priceBookEntries
            tot = ((int) (Math.random()*4));
            tot -= obj.getPriceBookEntries().size();
            for (int cnt=0; cnt<tot; cnt++) {
                PriceBookEntry priceBookEntry = null;
            }
            done(obj, ItemOptionValue.P_PriceBookEntries);
        }
    }
    
    public void populate(ItemPack obj) {
        populate(obj, 0);
    }
    public void populate(ItemPack obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setName(OAString.getDummyText(18, 0, 50));
        if (add(obj, ItemPack.P_PriceBookEntries)) {
            // priceBookEntries
            tot = ((int) (Math.random()*4));
            tot -= obj.getPriceBookEntries().size();
            for (int cnt=0; cnt<tot; cnt++) {
                PriceBookEntry priceBookEntry = null;
            }
            done(obj, ItemPack.P_PriceBookEntries);
        }
        if (add(obj, ItemPack.P_Products)) {
            // products
            tot = ((int) (Math.random()*4));
            tot -= obj.getProducts().size();
            for (int cnt=0; cnt<tot; cnt++) {
                Product product = null;
            }
            done(obj, ItemPack.P_Products);
        }
    }
    
    public void populate(ItemPackType obj) {
        populate(obj, 0);
    }
    public void populate(ItemPackType obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setName(OAString.getDummyText(18, 0, 50));
        obj.setType((int) (Math.random() * 3));
        obj.setQuantityInPack((int) (Math.random() * 900));
        if (add(obj, ItemPackType.P_ItemPacks)) {
            // itemPacks
            //   will be created by Item
            done(obj, ItemPackType.P_ItemPacks);
        }
    }
    
    public void populate(ItemRestriction obj) {
        populate(obj, 0);
    }
    public void populate(ItemRestriction obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setItemRuleType(OAString.getDummyText(20, 0, 25));
        obj.setLine(OAString.getDummyText(20, 0, 3));
        obj.setProductLineCode((int) (Math.random() * 900));
        obj.setProductLineSubcode((int) (Math.random() * 900));
        obj.setItem(OAString.getDummyText(20, 0, 14));
        obj.setLocationRuleType(OAString.getDummyText(20, 0, 20));
        obj.setStoreId((int) (Math.random() * 900));
        obj.setZipcode(OAString.getDummyText(20, 0, 5));
        obj.setState(OAString.getDummyText(20, 0, 20));
        obj.setCounty(OAString.getDummyText(20, 0, 30));
        obj.setRuleSearchValue(OAString.getDummyText(20, 0, 75));
        obj.setFlightRestricted(Math.random() < .5 ? true : false);
        obj.setCaustic(Math.random() < .5 ? true : false);
        obj.setHybridElectric(Math.random() < .5 ? true : false);
        obj.setFreonRestricted(Math.random() < .5 ? true : false);
        obj.setSalesRestricted(Math.random() < .5 ? true : false);
        obj.setSalesRestrictedEffectiveDate((OADate)(new OADate()).plusDays((int) (Math.random() * 1000)));
        obj.setProcessDate((OADate)(new OADate()).plusDays((int) (Math.random() * 1000)));
        obj.setDeleteDate((OADate)(new OADate()).plusDays((int) (Math.random() * 1000)));
    }
    
    public void populate(ItemVariant obj) {
        populate(obj, 0);
    }
    public void populate(ItemVariant obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setName(OAString.getDummyText(18, 0, 50));
        if (add(obj, ItemVariant.P_ItemOptionValues)) {
            // itemOptionValues
            tot = ((int) (Math.random()*4));
            tot -= obj.getItemOptionValues().size();
            for (int cnt=0; cnt<tot; cnt++) {
                hub = (Hub) obj.getProperty(OAString.cpp(ItemVariant.P_Item, Item.P_ItemOptions, ItemOption.P_ItemOptionValues));
                if (hub != null) {
                    x = (int) (Math.random()*hub.getSize());
                    obj.getItemOptionValues().add((ItemOptionValue) hub.getAt(x));
                }
            }
            done(obj, ItemVariant.P_ItemOptionValues);
        }
        if (add(obj, ItemVariant.P_Products)) {
            // products
            tot = ((int) (Math.random()*4));
            tot -= obj.getProducts().size();
            for (int cnt=0; cnt<tot; cnt++) {
                Product product = null;
            }
            done(obj, ItemVariant.P_Products);
        }
    }
    
    public void populate(ItemVendor obj) {
        populate(obj, 0);
    }
    public void populate(ItemVendor obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setName(OAString.getDummyText(18, 0, 50));
        if (add(obj, ItemVendor.P_Items)) {
            // items
            tot = ((int) (Math.random()*4));
            tot -= obj.getItems().size();
            for (int cnt=0; cnt<tot; cnt++) {
                Item item = null;
                if (Math.random() < .75) {
                    item = (Item) OARuntime.oa(obj).internal().objects().cache().getRandom(Item.class, 500);
                    if (item != null) obj.getItems().add(item);
                }
                if (item == null) {
                    item = createItem();
                    obj.getItems().add(item);
                    populate(item);
                }
            }
            done(obj, ItemVendor.P_Items);
        }
    }
    
    public void populate(LedgerDenominationBundle obj) {
        populate(obj, 0);
    }
    public void populate(LedgerDenominationBundle obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setQuantity((int) (Math.random() * 900));
        if (add(obj, LedgerDenominationBundle.P_DenominationBundle)) {
            // denominationBundle
            hub = (Hub) obj.getProperty(OAString.cpp(LedgerDenominationBundle.P_CalcStore, Store.P_CurrencyType, CurrencyType.P_CurrencyDenominations, CurrencyDenomination.P_DenominationBundles));
            if (hub != null) {
                x = (int) (Math.random()*hub.getSize());
                obj.setDenominationBundle((DenominationBundle) hub.getAt(x));
            }
            done(obj, LedgerDenominationBundle.P_DenominationBundle);
        }
    }
    
    public void populate(LineItem obj) {
        populate(obj, 0);
    }
    public void populate(LineItem obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setQuantity((int) (Math.random() * 900));
        obj.setSerialCode(OAString.getDummyText(15, 0, 75));
        obj.setPriceEach((double) (Math.random() * 100));
        if (add(obj, LineItem.P_RefundLineItems)) {
            // refundLineItems
            tot = ((int) (Math.random()*4));
            tot -= obj.getRefundLineItems().size();
            for (int cnt=0; cnt<tot; cnt++) {
                RefundLineItem refundLineItem = null;
                refundLineItem = (RefundLineItem) OARuntime.oa(obj).internal().objects().cache().getRandom(RefundLineItem.class, 500);
                if (refundLineItem != null) obj.getRefundLineItems().add(refundLineItem);
            }
            done(obj, LineItem.P_RefundLineItems);
        }
    }
    
    public void populate(LineItemDiscount obj) {
        populate(obj, 0);
    }
    public void populate(LineItemDiscount obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setType((int) (Math.random() * 1));
        obj.setPercentage((double) (Math.random() * 100));
        obj.setAmount((double) (Math.random() * 100));
    }
    
    public void populate(LineItemTax obj) {
        populate(obj, 0);
    }
    public void populate(LineItemTax obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setTaxPercent((double) (Math.random() * 100));
        if (add(obj, LineItemTax.P_VertexTaxCodeRate)) {
            // vertexTaxCodeRate
            hub = (Hub) obj.getProperty(OAString.cpp(LineItemTax.P_LineItem, LineItem.P_Product, Product.P_Item, Item.P_VertexTaxCodes, VertexTaxCode.P_VertexTaxCodeRates));
            if (hub != null) {
                x = (int) (Math.random()*hub.getSize());
                obj.setVertexTaxCodeRate((VertexTaxCodeRate) hub.getAt(x));
            }
            done(obj, LineItemTax.P_VertexTaxCodeRate);
        }
    }
    
    public void populate(ManualPurchaseOrder obj) {
        populate(obj, 0);
    }
    public void populate(ManualPurchaseOrder obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setCashAmount((double) (Math.random() * 100));
        obj.setNote(OAString.getDummyText(30, 0, 500));
        obj.setApplied((new OADateTime()).plusDays((int) (Math.random() * 1000)));
    }
    
    public void populate(Manufacturer obj) {
        populate(obj, 0);
    }
    public void populate(Manufacturer obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setName(OAString.getDummyText(18, 0, 50));
        if (add(obj, Manufacturer.P_Items)) {
            // items
            tot = ((int) (Math.random()*4));
            tot -= obj.getItems().size();
            for (int cnt=0; cnt<tot; cnt++) {
                Item item = null;
                if (Math.random() < .75) {
                    item = (Item) OARuntime.oa(obj).internal().objects().cache().getRandom(Item.class, 500);
                    if (item != null) obj.getItems().add(item);
                }
                if (item == null) {
                    item = createItem();
                    obj.getItems().add(item);
                    populate(item);
                }
            }
            done(obj, Manufacturer.P_Items);
        }
    }
    
    public void populate(MeasureType obj) {
        populate(obj, 0);
    }
    public void populate(MeasureType obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setType((int) (Math.random() * 6));
    }
    
    public void populate(NewNetPriceCalculater obj) {
        populate(obj, 0);
    }
    public void populate(NewNetPriceCalculater obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
    }
    
    public void populate(OnlineOrder obj) {
        populate(obj, 0);
    }
    public void populate(OnlineOrder obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        if (add(obj, OnlineOrder.P_OnlineOrderDeliveries)) {
            // onlineOrderDeliveries
            tot = ((int) (Math.random()*4));
            tot -= obj.getOnlineOrderDeliveries().size();
            for (int cnt=0; cnt<tot; cnt++) {
                OnlineOrderDelivery onlineOrderDelivery = null;
                if (Math.random() < .75) {
                    onlineOrderDelivery = (OnlineOrderDelivery) OARuntime.oa(obj).internal().objects().cache().getRandom(OnlineOrderDelivery.class, 500);
                    if (onlineOrderDelivery != null) obj.getOnlineOrderDeliveries().add(onlineOrderDelivery);
                }
                if (onlineOrderDelivery == null) {
                    onlineOrderDelivery = createOnlineOrderDelivery();
                    obj.getOnlineOrderDeliveries().add(onlineOrderDelivery);
                    populate(onlineOrderDelivery);
                }
            }
            done(obj, OnlineOrder.P_OnlineOrderDeliveries);
        }
        if (add(obj, OnlineOrder.P_OnlineOrderItems)) {
            // onlineOrderItems
            tot = ((int) (Math.random()*4));
            tot -= obj.getOnlineOrderItems().size();
            for (int cnt=0; cnt<tot; cnt++) {
                OnlineOrderItem onlineOrderItem = null;
                onlineOrderItem = createOnlineOrderItem();
                obj.getOnlineOrderItems().add(onlineOrderItem);
                populate(onlineOrderItem);
            }
            done(obj, OnlineOrder.P_OnlineOrderItems);
        }
    }
    
    public void populate(OnlineOrderDelivery obj) {
        populate(obj, 0);
    }
    public void populate(OnlineOrderDelivery obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        if (add(obj, OnlineOrderDelivery.P_OodItems)) {
            // oodItems
            tot = ((int) (Math.random()*4));
            tot -= obj.getOodItems().size();
            for (int cnt=0; cnt<tot; cnt++) {
                OodItem oodItem = null;
                oodItem = createOodItem();
                obj.getOodItems().add(oodItem);
                populate(oodItem);
            }
            done(obj, OnlineOrderDelivery.P_OodItems);
        }
    }
    
    public void populate(OnlineOrderItem obj) {
        populate(obj, 0);
    }
    public void populate(OnlineOrderItem obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setQuantity((int) (Math.random() * 900));
        if (add(obj, OnlineOrderItem.P_OodItems)) {
            // oodItems
            tot = ((int) (Math.random()*4));
            tot -= obj.getOodItems().size();
            for (int cnt=0; cnt<tot; cnt++) {
                OodItem oodItem = null;
                oodItem = (OodItem) OARuntime.oa(obj).internal().objects().cache().getRandom(OodItem.class, 500);
                if (oodItem != null) obj.getOodItems().add(oodItem);
            }
            done(obj, OnlineOrderItem.P_OodItems);
        }
    }
    
    public void populate(OodItem obj) {
        populate(obj, 0);
    }
    public void populate(OodItem obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setQuantity((int) (Math.random() * 900));
        if (add(obj, OodItem.P_OodItemEaches)) {
            // oodItemEaches
            tot = ((int) (Math.random()*4));
            tot -= obj.getOodItemEaches().size();
            for (int cnt=0; cnt<tot; cnt++) {
                OodItemEach oodItemEach = null;
                oodItemEach = createOodItemEach();
                obj.getOodItemEaches().add(oodItemEach);
                populate(oodItemEach);
            }
            done(obj, OodItem.P_OodItemEaches);
        }
    }
    
    public void populate(OodItemEach obj) {
        populate(obj, 0);
    }
    public void populate(OodItemEach obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setSerialCode(OAString.getDummyText(18, 0, 35));
    }
    
    public void populate(OPPConnector obj) {
        populate(obj, 0);
    }
    public void populate(OPPConnector obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
    }
    
    public void populate(OutFrontMerch obj) {
        populate(obj, 0);
    }
    public void populate(OutFrontMerch obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
    }
    
    public void populate(PaymentConnector obj) {
        populate(obj, 0);
    }
    public void populate(PaymentConnector obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
    }
    
    public void populate(Planogram obj) {
        populate(obj, 0);
    }
    public void populate(Planogram obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
    }
    
    public void populate(PriceBookEntry obj) {
        populate(obj, 0);
    }
    public void populate(PriceBookEntry obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setName(OAString.getDummyText(18, 0, 30));
        obj.setSalePrice((double) (Math.random() * 100));
        obj.setFromDate((OADate)(new OADate()).plusDays((int) (Math.random() * 1000)));
        obj.setToDate((OADate)(new OADate()).plusDays((int) (Math.random() * 1000)));
        obj.setPromotion(Math.random() < .5 ? true : false);
        obj.setPriority((int) (Math.random() * 900));
        if (add(obj, PriceBookEntry.P_ItemPack)) {
            // itemPack
            hub = (Hub) obj.getProperty(OAString.cpp(PriceBookEntry.P_Item, Item.P_ItemPacks));
            if (hub != null) {
                x = (int) (Math.random()*hub.getSize());
                obj.setItemPack((ItemPack) hub.getAt(x));
            }
            done(obj, PriceBookEntry.P_ItemPack);
        }
        if (add(obj, PriceBookEntry.P_Product)) {
            // product
            hub = (Hub) obj.getProperty(OAString.cpp(PriceBookEntry.P_Item, Item.P_Products));
            if (hub != null) {
                x = (int) (Math.random()*hub.getSize());
                obj.setProduct((Product) hub.getAt(x));
            }
            done(obj, PriceBookEntry.P_Product);
        }
    }
    
    public void populate(Printer obj) {
        populate(obj, 0);
    }
    public void populate(Printer obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
    }
    
    public void populate(Product obj) {
        populate(obj, 0);
    }
    public void populate(Product obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setSku(OAString.getDummyText(15, 0, 25));
        obj.setQuantityOnHand((int) (Math.random() * 900));
        obj.setWeight(OAString.getDummyText(20, 0, 0));
        obj.setSealedPackage(Math.random() < .5 ? true : false);
        obj.setDiscontinued((OADate)(new OADate()).plusDays((int) (Math.random() * 1000)));
        obj.setDiscontinuedReason(OAString.getDummyText(22, 0, 120));
        if (add(obj, Product.P_ItemPack)) {
            // itemPack
            hub = (Hub) obj.getProperty(OAString.cpp(Product.P_Item, Item.P_ItemPacks));
            if (hub != null) {
                x = (int) (Math.random()*hub.getSize());
                obj.setItemPack((ItemPack) hub.getAt(x));
            }
            done(obj, Product.P_ItemPack);
        }
        if (add(obj, Product.P_ItemVariant)) {
            // itemVariant
            hub = (Hub) obj.getProperty(OAString.cpp(Product.P_Item, Item.P_ItemVariants));
            if (hub != null) {
                x = (int) (Math.random()*hub.getSize());
                obj.setItemVariant((ItemVariant) hub.getAt(x));
            }
            done(obj, Product.P_ItemVariant);
        }
        if (add(obj, Product.P_PriceBookEntries)) {
            // priceBookEntries
            tot = ((int) (Math.random()*4));
            tot -= obj.getPriceBookEntries().size();
            for (int cnt=0; cnt<tot; cnt++) {
                PriceBookEntry priceBookEntry = null;
            }
            done(obj, Product.P_PriceBookEntries);
        }
        if (add(obj, Product.P_ProductSerialCodes)) {
            // productSerialCodes
            tot = ((int) (Math.random()*4));
            tot -= obj.getProductSerialCodes().size();
            for (int cnt=0; cnt<tot; cnt++) {
                ProductSerialCode productSerialCode = null;
                productSerialCode = createProductSerialCode();
                obj.getProductSerialCodes().add(productSerialCode);
                populate(productSerialCode);
            }
            done(obj, Product.P_ProductSerialCodes);
        }
        if (add(obj, Product.P_ProductUpcs)) {
            // productUpcs
            tot = ((int) (Math.random()*4));
            tot -= obj.getProductUpcs().size();
            for (int cnt=0; cnt<tot; cnt++) {
                ProductUpc productUpc = null;
                productUpc = createProductUpc();
                obj.getProductUpcs().add(productUpc);
                populate(productUpc);
            }
            done(obj, Product.P_ProductUpcs);
        }
    }
    
    public void populate(ProductSerialCode obj) {
        populate(obj, 0);
    }
    public void populate(ProductSerialCode obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setReceivedDate((OADate)(new OADate()).plusDays((int) (Math.random() * 1000)));
        obj.setSerialCode(OAString.getDummyText(18, 0, 35));
        obj.setSoldDate((OADate)(new OADate()).plusDays((int) (Math.random() * 1000)));
    }
    
    public void populate(ProductUpc obj) {
        populate(obj, 0);
    }
    public void populate(ProductUpc obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setUPC(OAString.getDummyText(18, 0, 35));
    }
    
    public void populate(PurchaseOrder obj) {
        populate(obj, 0);
    }
    public void populate(PurchaseOrder obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setReference(OAString.getDummyText(18, 0, 35));
        if (add(obj, PurchaseOrder.P_Invoices)) {
            // invoices
            tot = ((int) (Math.random()*4));
            tot -= obj.getInvoices().size();
            for (int cnt=0; cnt<tot; cnt++) {
                Invoice invoice = null;
                if (Math.random() < .75) {
                    invoice = (Invoice) OARuntime.oa(obj).internal().objects().cache().getRandom(Invoice.class, 500);
                    if (invoice != null) obj.getInvoices().add(invoice);
                }
                if (invoice == null) {
                    invoice = createInvoice();
                    obj.getInvoices().add(invoice);
                    populate(invoice);
                }
            }
            done(obj, PurchaseOrder.P_Invoices);
        }
    }
    
    public void populate(Quote obj) {
        populate(obj, 0);
    }
    public void populate(Quote obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setName(OAString.getDummyText(18, 0, 50));
        obj.setNote(OAString.getDummyText(30, 0, 500));
        obj.setEndDate((OADate)(new OADate()).plusDays((int) (Math.random() * 1000)));
    }
    
    public void populate(Refund obj) {
        populate(obj, 0);
    }
    public void populate(Refund obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        if (add(obj, Refund.P_RefundInvoices)) {
            // refundInvoices
            tot = ((int) (Math.random()*4));
            tot -= obj.getRefundInvoices().size();
            for (int cnt=0; cnt<tot; cnt++) {
                RefundInvoice refundInvoices = createRefundInvoice();
                obj.getRefundInvoices().add(refundInvoices);
                populate(refundInvoices);
            }
            done(obj, Refund.P_RefundInvoices);
        }
    }
    
    public void populate(RefundInvoice obj) {
        populate(obj, 0);
    }
    public void populate(RefundInvoice obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        if (add(obj, RefundInvoice.P_RefundLineItems)) {
            // refundLineItems
            tot = ((int) (Math.random()*4));
            tot -= obj.getRefundLineItems().size();
            for (int cnt=0; cnt<tot; cnt++) {
                RefundLineItem refundLineItem = null;
                refundLineItem = createRefundLineItem();
                obj.getRefundLineItems().add(refundLineItem);
                populate(refundLineItem);
            }
            done(obj, RefundInvoice.P_RefundLineItems);
        }
        if (add(obj, RefundInvoice.P_RefundPayments)) {
            // refundPayments
            tot = ((int) (Math.random()*4));
            tot -= obj.getRefundPayments().size();
            for (int cnt=0; cnt<tot; cnt++) {
                RefundPayment refundPayments = createRefundPayment();
                obj.getRefundPayments().add(refundPayments);
                populate(refundPayments);
            }
            done(obj, RefundInvoice.P_RefundPayments);
        }
    }
    
    public void populate(RefundLineItem obj) {
        populate(obj, 0);
    }
    public void populate(RefundLineItem obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setQuantity((int) (Math.random() * 900));
        obj.setPriceEach((double) (Math.random() * 100));
        if (add(obj, RefundLineItem.P_LineItem)) {
            // lineItem
            hub = (Hub) obj.getProperty(OAString.cpp(RefundLineItem.P_RefundInvoice, RefundInvoice.P_Invoice, Invoice.P_InvoiceBaskets, InvoiceBasket.P_LineItems));
            if (hub != null) {
                x = (int) (Math.random()*hub.getSize());
                obj.setLineItem((LineItem) hub.getAt(x));
            }
            done(obj, RefundLineItem.P_LineItem);
        }
        if (add(obj, RefundLineItem.P_RefundLineItemTaxes)) {
            // refundLineItemTaxes
            tot = ((int) (Math.random()*4));
            tot -= obj.getRefundLineItemTaxes().size();
            for (int cnt=0; cnt<tot; cnt++) {
                RefundLineItemTax refundLineItemTax = null;
                refundLineItemTax = createRefundLineItemTax();
                obj.getRefundLineItemTaxes().add(refundLineItemTax);
                populate(refundLineItemTax);
            }
            done(obj, RefundLineItem.P_RefundLineItemTaxes);
        }
    }
    
    public void populate(RefundLineItemTax obj) {
        populate(obj, 0);
    }
    public void populate(RefundLineItemTax obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setTaxPercent((double) (Math.random() * 100));
    }
    
    public void populate(RefundPayment obj) {
        populate(obj, 0);
    }
    public void populate(RefundPayment obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setAmount((double) (Math.random() * 100));
        obj.setApplied((new OADateTime()).plusDays((int) (Math.random() * 1000)));
        if (add(obj, RefundPayment.P_InvoicePayment)) {
            // invoicePayment
            hub = (Hub) obj.getProperty(OAString.cpp(RefundPayment.P_RefundInvoice, RefundInvoice.P_Invoice, Invoice.P_InvoicePayments));
            if (hub != null) {
                x = (int) (Math.random()*hub.getSize());
                obj.setInvoicePayment((InvoicePayment) hub.getAt(x));
            }
            done(obj, RefundPayment.P_InvoicePayment);
        }
    }
    
    public void populate(Register obj) {
        populate(obj, 0);
    }
    public void populate(Register obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setCode(OAString.getDummyText(10, 0, 15));
        obj.setDelete((new OADateTime()).plusDays((int) (Math.random() * 1000)));
        obj.setDeleteReason(OAString.getDummyText(22, 0, 120));
        if (add(obj, Register.P_RegisterSessions)) {
            // registerSessions
            tot = ((int) (Math.random()*4));
            tot -= obj.getRegisterSessions().size();
            for (int cnt=0; cnt<tot; cnt++) {
                RegisterSession registerSessions = createRegisterSession();
                obj.getRegisterSessions().add(registerSessions);
                populate(registerSessions);
            }
            done(obj, Register.P_RegisterSessions);
        }
        if (add(obj, Register.P_Till)) {
            // till
            hub = (Hub) obj.getProperty(OAString.cpp(Register.P_Store, Store.P_Tills));
            if (hub != null) {
                x = (int) (Math.random()*hub.getSize());
                obj.setTill((Till) hub.getAt(x));
            }
            done(obj, Register.P_Till);
        }
    }
    
    public void populate(RegisterSession obj) {
        populate(obj, 0);
    }
    public void populate(RegisterSession obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setEnded((new OADateTime()).plusDays((int) (Math.random() * 1000)));
        if (add(obj, RegisterSession.P_Invoices)) {
            // invoices
            tot = ((int) (Math.random()*4));
            tot -= obj.getInvoices().size();
            for (int cnt=0; cnt<tot; cnt++) {
                Invoice invoice = null;
                invoice = createInvoice();
                obj.getInvoices().add(invoice);
                populate(invoice);
            }
            done(obj, RegisterSession.P_Invoices);
        }
        if (add(obj, RegisterSession.P_Refunds)) {
            // refunds
            tot = ((int) (Math.random()*4));
            tot -= obj.getRefunds().size();
            for (int cnt=0; cnt<tot; cnt++) {
                Refund refund = null;
                if (Math.random() < .75) {
                    refund = (Refund) OARuntime.oa(obj).internal().objects().cache().getRandom(Refund.class, 500);
                    if (refund != null) obj.getRefunds().add(refund);
                }
                if (refund == null) {
                    refund = createRefund();
                    obj.getRefunds().add(refund);
                    populate(refund);
                }
            }
            done(obj, RegisterSession.P_Refunds);
        }
        if (add(obj, RegisterSession.P_TeamMember)) {
            // teamMember
            hub = (Hub) obj.getProperty(OAString.cpp(RegisterSession.P_Register, Register.P_Store, Store.P_TeamMembers));
            if (hub != null) {
                x = (int) (Math.random()*hub.getSize());
                obj.setTeamMember((TeamMember) hub.getAt(x));
            }
            done(obj, RegisterSession.P_TeamMember);
        }
        if (add(obj, RegisterSession.P_TillLedgerEntries)) {
            // tillLedgerEntries
            tot = ((int) (Math.random()*4));
            tot -= obj.getTillLedgerEntries().size();
            for (int cnt=0; cnt<tot; cnt++) {
                TillLedgerEntry tillLedgerEntry = null;
            }
            done(obj, RegisterSession.P_TillLedgerEntries);
        }
    }
    
    public void populate(Report obj) {
        populate(obj, 0);
    }
    public void populate(Report obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setGenerated((new OADateTime()).plusDays((int) (Math.random() * 1000)));
        obj.setHtml(OAString.getDummyText(30, 0, 500));
        if (add(obj, Report.P_ReportDef)) {
            // reportDef
            hub = (Hub) obj.getProperty(OAString.cpp(Report.P_CalcReportClass, ReportClass.P_ReportDefs));
            if (hub != null) {
                x = (int) (Math.random()*hub.getSize());
                obj.setReportDef((ReportDef) hub.getAt(x));
            }
            done(obj, Report.P_ReportDef);
        }
    }
    
    public void populate(ReportClass obj) {
        populate(obj, 0);
    }
    private String strReportClassClassName = "0"; // unique value for className
    public void populate(ReportClass obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setName(OAString.getDummyText(18, 0, 55));
        strReportClassClassName = "" + (OAConv.toInt(strReportClassClassName)+1);
        obj.setClassName(strReportClassClassName);
        if (add(obj, ReportClass.P_ReportDefs)) {
            // reportDefs
            tot = ((int) (Math.random()*4));
            tot -= obj.getReportDefs().size();
            for (int cnt=0; cnt<tot; cnt++) {
                ReportDef reportDef = null;
                reportDef = createReportDef();
                obj.getReportDefs().add(reportDef);
                populate(reportDef);
            }
            done(obj, ReportClass.P_ReportDefs);
        }
    }
    
    public void populate(ReportDef obj) {
        populate(obj, 0);
    }
    public void populate(ReportDef obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setName(OAString.getDummyText(15, 0, 55));
        obj.setTemplate(OAString.getDummyText(30, 0, 500));
        // seq is auto sequence
    }
    
    public void populate(ReturnedCheckFee obj) {
        populate(obj, 0);
    }
    public void populate(ReturnedCheckFee obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setAmount((double) (Math.random() * 100));
        obj.setCollectedDate((OADate)(new OADate()).plusDays((int) (Math.random() * 1000)));
        obj.setNote(OAString.getDummyText(20, 0, 0));
        obj.setStatus((int) (Math.random() * 4));
    }
    
    public void populate(Reward obj) {
        populate(obj, 0);
    }
    public void populate(Reward obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
    }
    
    public void populate(RewardType obj) {
        populate(obj, 0);
    }
    public void populate(RewardType obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        if (add(obj, RewardType.P_Rewards)) {
            // rewards
            tot = ((int) (Math.random()*4));
            tot -= obj.getRewards().size();
            for (int cnt=0; cnt<tot; cnt++) {
                Reward reward = null;
                if (Math.random() < .75) {
                    reward = (Reward) OARuntime.oa(obj).internal().objects().cache().getRandom(Reward.class, 500);
                    if (reward != null) obj.getRewards().add(reward);
                }
                if (reward == null) {
                    reward = createReward();
                    obj.getRewards().add(reward);
                    populate(reward);
                }
            }
            done(obj, RewardType.P_Rewards);
        }
    }
    
    public void populate(ScannerConnector obj) {
        populate(obj, 0);
    }
    public void populate(ScannerConnector obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
    }
    
    public void populate(ShippingQuoteConnector obj) {
        populate(obj, 0);
    }
    public void populate(ShippingQuoteConnector obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
    }
    
    public void populate(Store obj) {
        populate(obj, 0);
    }
    private int iStoreStoreNumber = 0; // unique value for storeNumber
    public void populate(Store obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        iStoreStoreNumber += 1;
        obj.setStoreNumber(iStoreStoreNumber);
        obj.setName(OAString.getDummyText(18, 0, 50));
        if (add(obj, Store.P_ManualPurchaseOrders)) {
            // manualPurchaseOrders
            tot = ((int) (Math.random()*4));
            tot -= obj.getManualPurchaseOrders().size();
            for (int cnt=0; cnt<tot; cnt++) {
                ManualPurchaseOrder manualPurchaseOrder = null;
                manualPurchaseOrder = createManualPurchaseOrder();
                obj.getManualPurchaseOrders().add(manualPurchaseOrder);
                populate(manualPurchaseOrder);
            }
            done(obj, Store.P_ManualPurchaseOrders);
        }
        if (add(obj, Store.P_Registers)) {
            // registers
            tot = ((int) (Math.random()*4));
            tot -= obj.getRegisters().size();
            for (int cnt=0; cnt<tot; cnt++) {
                Register register = null;
                register = createRegister();
                obj.getRegisters().add(register);
                populate(register);
            }
            done(obj, Store.P_Registers);
        }
        if (add(obj, Store.P_StoreClosedDates)) {
            // storeClosedDates
            tot = ((int) (Math.random()*4));
            tot -= obj.getStoreClosedDates().size();
            for (int cnt=0; cnt<tot; cnt++) {
                StoreClosedDate storeClosedDate = null;
                storeClosedDate = createStoreClosedDate();
                obj.getStoreClosedDates().add(storeClosedDate);
                populate(storeClosedDate);
            }
            done(obj, Store.P_StoreClosedDates);
        }
        if (add(obj, Store.P_StoreHoursOpens)) {
            // storeHoursOpens
            tot = ((int) (Math.random()*4));
            tot -= obj.getStoreHoursOpens().size();
            for (int cnt=0; cnt<tot; cnt++) {
                StoreHoursOpen storeHoursOpen = null;
                storeHoursOpen = createStoreHoursOpen();
                obj.getStoreHoursOpens().add(storeHoursOpen);
                populate(storeHoursOpen);
            }
            done(obj, Store.P_StoreHoursOpens);
        }
        if (add(obj, Store.P_StoreSchedules)) {
            // storeSchedules
            tot = ((int) (Math.random()*4));
            tot -= obj.getStoreSchedules().size();
            for (int cnt=0; cnt<tot; cnt++) {
                StoreSchedule storeSchedule = null;
                if (Math.random() < .75) {
                    storeSchedule = (StoreSchedule) OARuntime.oa(obj).internal().objects().cache().getRandom(StoreSchedule.class, 500);
                    if (storeSchedule != null) obj.getStoreSchedules().add(storeSchedule);
                }
                if (storeSchedule == null) {
                    storeSchedule = createStoreSchedule();
                    obj.getStoreSchedules().add(storeSchedule);
                    populate(storeSchedule);
                }
            }
            done(obj, Store.P_StoreSchedules);
        }
        if (add(obj, Store.P_TeamMembers)) {
            // teamMembers
            tot = ((int) (Math.random()*4));
            tot -= obj.getTeamMembers().size();
            for (int cnt=0; cnt<tot; cnt++) {
                TeamMember teamMember = null;
                teamMember = createTeamMember();
                obj.getTeamMembers().add(teamMember);
                populate(teamMember);
            }
            done(obj, Store.P_TeamMembers);
        }
        if (add(obj, Store.P_Tills)) {
            // tills
            tot = ((int) (Math.random()*4));
            tot -= obj.getTills().size();
            for (int cnt=0; cnt<tot; cnt++) {
                Till till = null;
                till = createTill();
                obj.getTills().add(till);
                populate(till);
            }
            done(obj, Store.P_Tills);
        }
    }
    
    public void populate(StoreClosedDate obj) {
        populate(obj, 0);
    }
    public void populate(StoreClosedDate obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setDate((OADate)(new OADate()).plusDays((int) (Math.random() * 1000)));
        obj.setReason(OAString.getDummyText(15, 0, 25));
    }
    
    public void populate(StoreCycleCount obj) {
        populate(obj, 0);
    }
    public void populate(StoreCycleCount obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
    }
    
    public void populate(StoreDayEnd obj) {
        populate(obj, 0);
    }
    public void populate(StoreDayEnd obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setPettyCash((double) (Math.random() * 100));
    }
    
    public void populate(StoreDayOpen obj) {
        populate(obj, 0);
    }
    public void populate(StoreDayOpen obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        if (add(obj, StoreDayOpen.P_StoreSafeLedgerEntries)) {
            // storeSafeLedgerEntries
            tot = ((int) (Math.random()*4));
            tot -= obj.getStoreSafeLedgerEntries().size();
            for (int cnt=0; cnt<tot; cnt++) {
                StoreSafeLedgerEntry storeSafeLedgerEntry = null;
                storeSafeLedgerEntry = (StoreSafeLedgerEntry) OARuntime.oa(obj).internal().objects().cache().getRandom(StoreSafeLedgerEntry.class, 500);
                if (storeSafeLedgerEntry != null) obj.getStoreSafeLedgerEntries().add(storeSafeLedgerEntry);
            }
            done(obj, StoreDayOpen.P_StoreSafeLedgerEntries);
        }
    }
    
    public void populate(StoreHoursOpen obj) {
        populate(obj, 0);
    }
    public void populate(StoreHoursOpen obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setDayOfWeek((int) (Math.random() * 7));
        obj.setOpenTime(new OATime());
        obj.setCloseTime(new OATime());
    }
    
    public void populate(StoreLayout obj) {
        populate(obj, 0);
    }
    public void populate(StoreLayout obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
    }
    
    public void populate(StoreSafe obj) {
        populate(obj, 0);
    }
    public void populate(StoreSafe obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setName(OAString.getDummyText(12, 0, 25));
        obj.setCashAmount((double) (Math.random() * 100));
        obj.setPettyCashAmount((double) (Math.random() * 100));
        obj.setAllowDirectChanges(Math.random() < .5 ? true : false);
        if (add(obj, StoreSafe.P_BankDeposits)) {
            // bankDeposits
            tot = ((int) (Math.random()*4));
            tot -= obj.getBankDeposits().size();
            for (int cnt=0; cnt<tot; cnt++) {
                BankDeposit bankDeposit = null;
                bankDeposit = createBankDeposit();
                obj.getBankDeposits().add(bankDeposit);
                populate(bankDeposit);
            }
            done(obj, StoreSafe.P_BankDeposits);
        }
        if (add(obj, StoreSafe.P_InvoicePaymentChecks)) {
            // invoicePaymentChecks
            tot = ((int) (Math.random()*4));
            tot -= obj.getInvoicePaymentChecks().size();
            for (int cnt=0; cnt<tot; cnt++) {
                InvoicePaymentCheck invoicePaymentCheck = null;
            }
            done(obj, StoreSafe.P_InvoicePaymentChecks);
        }
        if (add(obj, StoreSafe.P_StoreSafeLedgerEntries)) {
            // storeSafeLedgerEntries
            tot = ((int) (Math.random()*4));
            tot -= obj.getStoreSafeLedgerEntries().size();
            for (int cnt=0; cnt<tot; cnt++) {
                StoreSafeLedgerEntry storeSafeLedgerEntry = null;
                storeSafeLedgerEntry = createStoreSafeLedgerEntry();
                obj.getStoreSafeLedgerEntries().add(storeSafeLedgerEntry);
                populate(storeSafeLedgerEntry);
            }
            done(obj, StoreSafe.P_StoreSafeLedgerEntries);
        }
    }
    
    public void populate(StoreSafeLedgerEntry obj) {
        populate(obj, 0);
    }
    public void populate(StoreSafeLedgerEntry obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setType((int) (Math.random() * 15));
        obj.setLooseCashAmount((double) (Math.random() * 100));
        obj.setCheckCount((int) (Math.random() * 900));
        obj.setCheckAmount((double) (Math.random() * 100));
        obj.setPettyCashAmount((double) (Math.random() * 100));
        obj.setNote(OAString.getDummyText(22, 0, 250));
        obj.setPosted((new OADateTime()).plusDays((int) (Math.random() * 1000)));
        if (add(obj, StoreSafeLedgerEntry.P_InvoicePaymentChecks)) {
            // invoicePaymentChecks
            tot = ((int) (Math.random()*4));
            tot -= obj.getInvoicePaymentChecks().size();
            for (int cnt=0; cnt<tot; cnt++) {
                hub = (Hub) obj.getProperty(OAString.cpp(StoreSafeLedgerEntry.P_StoreSafe, StoreSafe.P_InvoicePaymentChecks));
                if (hub != null) {
                    x = (int) (Math.random()*hub.getSize());
                    obj.getInvoicePaymentChecks().add((InvoicePaymentCheck) hub.getAt(x));
                }
            }
            done(obj, StoreSafeLedgerEntry.P_InvoicePaymentChecks);
        }
        if (add(obj, StoreSafeLedgerEntry.P_LedgerDenominationBundles)) {
            // ledgerDenominationBundles
            tot = ((int) (Math.random()*4));
            tot -= obj.getLedgerDenominationBundles().size();
            for (int cnt=0; cnt<tot; cnt++) {
                LedgerDenominationBundle ledgerDenominationBundle = null;
                ledgerDenominationBundle = createLedgerDenominationBundle();
                obj.getLedgerDenominationBundles().add(ledgerDenominationBundle);
                populate(ledgerDenominationBundle);
            }
            done(obj, StoreSafeLedgerEntry.P_LedgerDenominationBundles);
        }
    }
    
    public void populate(StoreSchedule obj) {
        populate(obj, 0);
    }
    private OADate dStoreScheduleDate = new OADate(); // unique value for date
    public void populate(StoreSchedule obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        dStoreScheduleDate = (OADate) dStoreScheduleDate.plusDays(1);
        obj.setDate(dStoreScheduleDate);
        obj.setNextStep((int) (Math.random() * 3));
        obj.setVerifySchedule((new OADateTime()).plusDays((int) (Math.random() * 1000)));
        obj.setTillAuditCompleted((new OADateTime()).plusDays((int) (Math.random() * 1000)));
        if (add(obj, StoreSchedule.P_TeamMembers)) {
            // teamMembers
            tot = ((int) (Math.random()*4));
            tot -= obj.getTeamMembers().size();
            for (int cnt=0; cnt<tot; cnt++) {
                TeamMember teamMember = null;
                teamMember = (TeamMember) OARuntime.oa(obj).internal().objects().cache().getRandom(TeamMember.class, 500);
                if (teamMember != null) obj.getTeamMembers().add(teamMember);
            }
            done(obj, StoreSchedule.P_TeamMembers);
        }
    }
    
    public void populate(StoreToDc obj) {
        populate(obj, 0);
    }
    public void populate(StoreToDc obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
    }
    
    public void populate(StoreToStoreTransfer obj) {
        populate(obj, 0);
    }
    public void populate(StoreToStoreTransfer obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        if (add(obj, StoreToStoreTransfer.P_StsDeliveries)) {
            // stsDeliveries
            tot = ((int) (Math.random()*4));
            tot -= obj.getStsDeliveries().size();
            for (int cnt=0; cnt<tot; cnt++) {
                StsDelivery stsDelivery = null;
                stsDelivery = createStsDelivery();
                obj.getStsDeliveries().add(stsDelivery);
                populate(stsDelivery);
            }
            done(obj, StoreToStoreTransfer.P_StsDeliveries);
        }
        if (add(obj, StoreToStoreTransfer.P_StsItems)) {
            // stsItems
            tot = ((int) (Math.random()*4));
            tot -= obj.getStsItems().size();
            for (int cnt=0; cnt<tot; cnt++) {
                StsItem stsItem = null;
                stsItem = createStsItem();
                obj.getStsItems().add(stsItem);
                populate(stsItem);
            }
            done(obj, StoreToStoreTransfer.P_StsItems);
        }
    }
    
    public void populate(StsDelivery obj) {
        populate(obj, 0);
    }
    public void populate(StsDelivery obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        if (add(obj, StsDelivery.P_StsdItems)) {
            // stsdItems
            tot = ((int) (Math.random()*4));
            tot -= obj.getStsdItems().size();
            for (int cnt=0; cnt<tot; cnt++) {
                StsdItem stsdItem = null;
                stsdItem = createStsdItem();
                obj.getStsdItems().add(stsdItem);
                populate(stsdItem);
            }
            done(obj, StsDelivery.P_StsdItems);
        }
    }
    
    public void populate(StsdItem obj) {
        populate(obj, 0);
    }
    public void populate(StsdItem obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setQuantity((int) (Math.random() * 900));
        obj.setReceived((new OADateTime()).plusDays((int) (Math.random() * 1000)));
        if (add(obj, StsdItem.P_StsdItemEaches)) {
            // stsdItemEaches
            tot = ((int) (Math.random()*4));
            tot -= obj.getStsdItemEaches().size();
            for (int cnt=0; cnt<tot; cnt++) {
                StsdItemEach stsdItemEach = null;
                stsdItemEach = createStsdItemEach();
                obj.getStsdItemEaches().add(stsdItemEach);
                populate(stsdItemEach);
            }
            done(obj, StsdItem.P_StsdItemEaches);
        }
    }
    
    public void populate(StsdItemEach obj) {
        populate(obj, 0);
    }
    public void populate(StsdItemEach obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setSerialCode(OAString.getDummyText(18, 0, 35));
    }
    
    public void populate(StsItem obj) {
        populate(obj, 0);
    }
    public void populate(StsItem obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setQuantity((int) (Math.random() * 900));
        if (add(obj, StsItem.P_StsdItems)) {
            // stsdItems
            tot = ((int) (Math.random()*4));
            tot -= obj.getStsdItems().size();
            for (int cnt=0; cnt<tot; cnt++) {
                StsdItem stsdItem = null;
                stsdItem = (StsdItem) OARuntime.oa(obj).internal().objects().cache().getRandom(StsdItem.class, 500);
                if (stsdItem != null) obj.getStsdItems().add(stsdItem);
            }
            done(obj, StsItem.P_StsdItems);
        }
    }
    
    public void populate(TeamMember obj) {
        populate(obj, 0);
    }
    private String strTeamMemberEmpNumber = "0"; // unique value for empNumber
    public void populate(TeamMember obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        strTeamMemberEmpNumber = "" + (OAConv.toInt(strTeamMemberEmpNumber)+1);
        obj.setEmpNumber(strTeamMemberEmpNumber);
        obj.setTitle(OAString.getDummyText(18, 0, 50));
        obj.setFirstName(OAString.getDummyText(15, 0, 25));
        obj.setLastName(OAString.getDummyText(18, 0, 50));
        if (Math.random() < .8) obj.setInactiveDate((OADate)(new OADate()).plusDays((int) (Math.random() * 1000)));
        if (add(obj, TeamMember.P_RegisterSessions)) {
            // registerSessions
            //   will be created by Register
            done(obj, TeamMember.P_RegisterSessions);
        }
        if (add(obj, TeamMember.P_StoreSchedules)) {
            // storeSchedules
            tot = ((int) (Math.random()*4));
            tot -= obj.getStoreSchedules().size();
            for (int cnt=0; cnt<tot; cnt++) {
                StoreSchedule storeSchedule = null;
                if (Math.random() < .75) {
                    storeSchedule = (StoreSchedule) OARuntime.oa(obj).internal().objects().cache().getRandom(StoreSchedule.class, 500);
                    if (storeSchedule != null) obj.getStoreSchedules().add(storeSchedule);
                }
                if (storeSchedule == null) {
                    storeSchedule = createStoreSchedule();
                    obj.getStoreSchedules().add(storeSchedule);
                    populate(storeSchedule);
                }
            }
            done(obj, TeamMember.P_StoreSchedules);
        }
        if (add(obj, TeamMember.P_TMPermissions)) {
            // tmPermissions
            tot = ((int) (Math.random()*4));
            tot -= obj.getTMPermissions().size();
            for (int cnt=0; cnt<tot; cnt++) {
                hub = ModelDelegate.getTMPermissions();
                if (Math.random() < .75) {
                    x = (int) (Math.random()*hub.getSize());
                    obj.getTMPermissions().add((TMPermission) hub.getAt(x));
                }
            }
            done(obj, TeamMember.P_TMPermissions);
        }
    }
    
    public void populate(Till obj) {
        populate(obj, 0);
    }
    public void populate(Till obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setCode(OAString.getDummyText(10, 0, 15));
        obj.setCashAmount((double) (Math.random() * 100));
        if (add(obj, Till.P_InvoicePaymentChecks)) {
            // invoicePaymentChecks
            tot = ((int) (Math.random()*4));
            tot -= obj.getInvoicePaymentChecks().size();
            for (int cnt=0; cnt<tot; cnt++) {
                InvoicePaymentCheck invoicePaymentCheck = null;
            }
            done(obj, Till.P_InvoicePaymentChecks);
        }
        if (add(obj, Till.P_Register)) {
            // register
            hub = (Hub) obj.getProperty(OAString.cpp(Till.P_Store, Store.P_Registers));
            if (hub != null) {
                x = (int) (Math.random()*hub.getSize());
                obj.setRegister((Register) hub.getAt(x));
            }
            done(obj, Till.P_Register);
        }
        if (add(obj, Till.P_TillLedgerEntries)) {
            // tillLedgerEntries
            tot = ((int) (Math.random()*4));
            tot -= obj.getTillLedgerEntries().size();
            for (int cnt=0; cnt<tot; cnt++) {
                TillLedgerEntry tillLedgerEntry = null;
                tillLedgerEntry = createTillLedgerEntry();
                obj.getTillLedgerEntries().add(tillLedgerEntry);
                populate(tillLedgerEntry);
            }
            done(obj, Till.P_TillLedgerEntries);
        }
    }
    
    public void populate(TillLedgerEntry obj) {
        populate(obj, 0);
    }
    public void populate(TillLedgerEntry obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setType((int) (Math.random() * 15));
        obj.setLooseCashAmount((double) (Math.random() * 100));
        obj.setCheckCount((int) (Math.random() * 900));
        obj.setCheckAmount((double) (Math.random() * 100));
        obj.setPosted((new OADateTime()).plusDays((int) (Math.random() * 1000)));
        obj.setNote(OAString.getDummyText(22, 0, 250));
        if (add(obj, TillLedgerEntry.P_InvoicePaymentChecks)) {
            // invoicePaymentChecks
            tot = ((int) (Math.random()*4));
            tot -= obj.getInvoicePaymentChecks().size();
            for (int cnt=0; cnt<tot; cnt++) {
                hub = (Hub) obj.getProperty(OAString.cpp(TillLedgerEntry.P_Till, Till.P_InvoicePaymentChecks));
                if (hub != null) {
                    x = (int) (Math.random()*hub.getSize());
                    obj.getInvoicePaymentChecks().add((InvoicePaymentCheck) hub.getAt(x));
                }
            }
            done(obj, TillLedgerEntry.P_InvoicePaymentChecks);
        }
        if (add(obj, TillLedgerEntry.P_LedgerDenominationBundles)) {
            // ledgerDenominationBundles
            tot = ((int) (Math.random()*4));
            tot -= obj.getLedgerDenominationBundles().size();
            for (int cnt=0; cnt<tot; cnt++) {
                LedgerDenominationBundle ledgerDenominationBundle = null;
                ledgerDenominationBundle = createLedgerDenominationBundle();
                obj.getLedgerDenominationBundles().add(ledgerDenominationBundle);
                populate(ledgerDenominationBundle);
            }
            done(obj, TillLedgerEntry.P_LedgerDenominationBundles);
        }
        if (add(obj, TillLedgerEntry.P_RegisterSession)) {
            // registerSession
            hub = (Hub) obj.getProperty(OAString.cpp(TillLedgerEntry.P_Till, Till.P_Register, Register.P_RegisterSessions));
            if (hub != null) {
                x = (int) (Math.random()*hub.getSize());
                obj.setRegisterSession((RegisterSession) hub.getAt(x));
            }
            done(obj, TillLedgerEntry.P_RegisterSession);
        }
    }
    
    public void populate(TMPermission obj) {
        populate(obj, 0);
    }
    public void populate(TMPermission obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setType((int) (Math.random() * 6));
        obj.setName(OAString.getDummyText(15, 0, 25));
        obj.setDescription(OAString.getDummyText(22, 0, 175));
        if (add(obj, TMPermission.P_TeamMembers)) {
            // teamMembers
            tot = ((int) (Math.random()*4));
            tot -= obj.getTeamMembers().size();
            for (int cnt=0; cnt<tot; cnt++) {
                TeamMember teamMember = null;
                teamMember = (TeamMember) OARuntime.oa(obj).internal().objects().cache().getRandom(TeamMember.class, 500);
                if (teamMember != null) obj.getTeamMembers().add(teamMember);
            }
            done(obj, TMPermission.P_TeamMembers);
        }
    }
    
    public void populate(VehicleMake obj) {
        populate(obj, 0);
    }
    public void populate(VehicleMake obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        if (add(obj, VehicleMake.P_VehicleModels)) {
            // vehicleModels
            tot = ((int) (Math.random()*4));
            tot -= obj.getVehicleModels().size();
            for (int cnt=0; cnt<tot; cnt++) {
                VehicleModel vehicleModel = null;
                vehicleModel = createVehicleModel();
                obj.getVehicleModels().add(vehicleModel);
                populate(vehicleModel);
            }
            done(obj, VehicleMake.P_VehicleModels);
        }
    }
    
    public void populate(VehicleModel obj) {
        populate(obj, 0);
    }
    public void populate(VehicleModel obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        if (add(obj, VehicleModel.P_VehicleModelPackages)) {
            // vehicleModelPackages
            tot = ((int) (Math.random()*4));
            tot -= obj.getVehicleModelPackages().size();
            for (int cnt=0; cnt<tot; cnt++) {
                VehicleModelPackage vehicleModelPackage = null;
                vehicleModelPackage = createVehicleModelPackage();
                obj.getVehicleModelPackages().add(vehicleModelPackage);
                populate(vehicleModelPackage);
            }
            done(obj, VehicleModel.P_VehicleModelPackages);
        }
        if (add(obj, VehicleModel.P_VehicleModelYears)) {
            // vehicleModelYears
            tot = ((int) (Math.random()*4));
            tot -= obj.getVehicleModelYears().size();
            for (int cnt=0; cnt<tot; cnt++) {
                VehicleModelYear vehicleModelYear = null;
                if (Math.random() < .75) {
                    vehicleModelYear = (VehicleModelYear) OARuntime.oa(obj).internal().objects().cache().getRandom(VehicleModelYear.class, 500);
                    if (vehicleModelYear != null) obj.getVehicleModelYears().add(vehicleModelYear);
                }
                if (vehicleModelYear == null) {
                    vehicleModelYear = createVehicleModelYear();
                    obj.getVehicleModelYears().add(vehicleModelYear);
                    populate(vehicleModelYear);
                }
            }
            done(obj, VehicleModel.P_VehicleModelYears);
        }
    }
    
    public void populate(VehicleModelPackage obj) {
        populate(obj, 0);
    }
    public void populate(VehicleModelPackage obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
    }
    
    public void populate(VehicleModelYear obj) {
        populate(obj, 0);
    }
    public void populate(VehicleModelYear obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        if (add(obj, VehicleModelYear.P_VehicleModelPackages)) {
            // vehicleModelPackages
            tot = ((int) (Math.random()*4));
            tot -= obj.getVehicleModelPackages().size();
            for (int cnt=0; cnt<tot; cnt++) {
                VehicleModelPackage vehicleModelPackage = null;
                if (Math.random() < .75) {
                    vehicleModelPackage = (VehicleModelPackage) OARuntime.oa(obj).internal().objects().cache().getRandom(VehicleModelPackage.class, 500);
                    if (vehicleModelPackage != null) obj.getVehicleModelPackages().add(vehicleModelPackage);
                }
                if (vehicleModelPackage == null) {
                    vehicleModelPackage = createVehicleModelPackage();
                    obj.getVehicleModelPackages().add(vehicleModelPackage);
                    populate(vehicleModelPackage);
                }
            }
            done(obj, VehicleModelYear.P_VehicleModelPackages);
        }
    }
    
    public void populate(VertexConnector obj) {
        populate(obj, 0);
    }
    public void populate(VertexConnector obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
    }
    
    public void populate(VertexTaxCode obj) {
        populate(obj, 0);
    }
    public void populate(VertexTaxCode obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setTaxCode(OAString.getDummyText(8, 0, 8));
        obj.setTaxAuthority(OAString.getDummyText(15, 0, 20));
        if (add(obj, VertexTaxCode.P_VertexTaxCodeRates)) {
            // vertexTaxCodeRates
            tot = ((int) (Math.random()*4));
            tot -= obj.getVertexTaxCodeRates().size();
            for (int cnt=0; cnt<tot; cnt++) {
                VertexTaxCodeRate vertexTaxCodeRate = null;
                vertexTaxCodeRate = createVertexTaxCodeRate();
                obj.getVertexTaxCodeRates().add(vertexTaxCodeRate);
                populate(vertexTaxCodeRate);
            }
            done(obj, VertexTaxCode.P_VertexTaxCodeRates);
        }
    }
    
    public void populate(VertexTaxCodeRate obj) {
        populate(obj, 0);
    }
    public void populate(VertexTaxCodeRate obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setTaxPercent((double) (Math.random() * 100));
        obj.setDecimalPlaces((int) (Math.random() * 900));
        obj.setBeginDate((OADate)(new OADate()).plusDays((int) (Math.random() * 1000)));
        obj.setEndDate((OADate)(new OADate()).plusDays((int) (Math.random() * 1000)));
        obj.setMinTaxable((double) (Math.random() * 100));
        obj.setMaxTaxable((double) (Math.random() * 100));
        obj.setThresholdAmount((double) (Math.random() * 100));
    }
    
    public void populate(VinLookup obj) {
        populate(obj, 0);
    }
    public void populate(VinLookup obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
        obj.setVin(OAString.getDummyText(22, 0, 75));
    }
    
    public void populate(ZipCodeLookupConnector obj) {
        populate(obj, 0);
    }
    public void populate(ZipCodeLookupConnector obj, int level) {
        int x;
        int tot;
        // id is auto assigned
        // created has a default value
    }
    public void createSamples() {
        int x;
        // lookups
        x = 1;
        for (int i=0; i<x; i++) {
            AppServer appServer = createAppServer();
            ModelDelegate.getCreateOneAppServerHub().add(appServer);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            AppUser appUser = createAppUser();
            ModelDelegate.getAppUsers().add(appUser);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            BarcodeType barcodeType = createBarcodeType();
            ModelDelegate.getBarcodeTypes().add(barcodeType);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            CurrencyType currencyType = createCurrencyType();
            ModelDelegate.getCurrencyTypes().add(currencyType);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            DeliveryService deliveryService = createDeliveryService();
            ModelDelegate.getDeliveryServices().add(deliveryService);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            DistCenter distCenter = createDistCenter();
            ModelDelegate.getDistCenters().add(distCenter);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            ItemCategory itemCategory = createItemCategory();
            ModelDelegate.getItemCategories().add(itemCategory);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            ItemLine itemLine = createItemLine();
            ModelDelegate.getItemLines().add(itemLine);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            ItemOptionType itemOptionType = createItemOptionType();
            ModelDelegate.getItemOptionTypes().add(itemOptionType);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            ItemPackType itemPackType = createItemPackType();
            ModelDelegate.getItemPackTypes().add(itemPackType);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            MeasureType measureType = createMeasureType();
            ModelDelegate.getMeasureTypes().add(measureType);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            ReportClass reportClass = createReportClass();
            ModelDelegate.getReportClasses().add(reportClass);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            RewardType rewardType = createRewardType();
            ModelDelegate.getRewardTypes().add(rewardType);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            TMPermission tmPermission = createTMPermission();
            ModelDelegate.getTMPermissions().add(tmPermission);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            VertexTaxCode vertexTaxCode = createVertexTaxCode();
            ModelDelegate.getVertexTaxCodes().add(vertexTaxCode);
        }
        
        // others
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            Address address = createAddress();
            hubAddress.add(address);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            BackroomMap backroomMap = createBackroomMap();
            hubBackroomMap.add(backroomMap);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            BankDepositCheck bankDepositCheck = createBankDepositCheck();
            hubBankDepositCheck.add(bankDepositCheck);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            Catalog catalog = createCatalog();
            hubCatalog.add(catalog);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            CatalogCategory catalogCategory = createCatalogCategory();
            hubCatalogCategory.add(catalogCategory);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            CatalogItem catalogItem = createCatalogItem();
            hubCatalogItem.add(catalogItem);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            Core core = createCore();
            hubCore.add(core);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            CronProcess cronProcess = createCronProcess();
            hubCronProcess.add(cronProcess);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            CurrencyExchangeRate currencyExchangeRate = createCurrencyExchangeRate();
            hubCurrencyExchangeRate.add(currencyExchangeRate);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            Customer customer = createCustomer();
            hubCustomer.add(customer);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            CustomerConnector customerConnector = createCustomerConnector();
            hubCustomerConnector.add(customerConnector);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            CustomerCredit customerCredit = createCustomerCredit();
            hubCustomerCredit.add(customerCredit);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            DcToStore dcToStore = createDcToStore();
            hubDcToStore.add(dcToStore);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            Demo demo = createDemo();
            hubDemo.add(demo);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            DiscountCoupon discountCoupon = createDiscountCoupon();
            hubDiscountCoupon.add(discountCoupon);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            DiscountType discountType = createDiscountType();
            hubDiscountType.add(discountType);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            Feedback feedback = createFeedback();
            hubFeedback.add(feedback);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            Garage garage = createGarage();
            hubGarage.add(garage);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            ImageStore imageStore = createImageStore();
            hubImageStore.add(imageStore);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            InventoryConnector inventoryConnector = createInventoryConnector();
            hubInventoryConnector.add(inventoryConnector);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            Invoice invoice = createInvoice();
            hubInvoice.add(invoice);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            InvoiceDiscount invoiceDiscount = createInvoiceDiscount();
            hubInvoiceDiscount.add(invoiceDiscount);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            InvoicePaymentCheck invoicePaymentCheck = createInvoicePaymentCheck();
            hubInvoicePaymentCheck.add(invoicePaymentCheck);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            InvoiceRebate invoiceRebate = createInvoiceRebate();
            hubInvoiceRebate.add(invoiceRebate);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            InvoiceShipTo invoiceShipTo = createInvoiceShipTo();
            hubInvoiceShipTo.add(invoiceShipTo);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            InvoiceTax invoiceTax = createInvoiceTax();
            hubInvoiceTax.add(invoiceTax);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            Item item = createItem();
            hubItem.add(item);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            ItemAlert itemAlert = createItemAlert();
            hubItemAlert.add(itemAlert);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            ItemInterchange itemInterchange = createItemInterchange();
            hubItemInterchange.add(itemInterchange);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            ItemMSDS itemMSDS = createItemMSDS();
            hubItemMSDS.add(itemMSDS);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            ItemRestriction itemRestriction = createItemRestriction();
            hubItemRestriction.add(itemRestriction);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            ItemVendor itemVendor = createItemVendor();
            hubItemVendor.add(itemVendor);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            LedgerDenominationBundle ledgerDenominationBundle = createLedgerDenominationBundle();
            hubLedgerDenominationBundle.add(ledgerDenominationBundle);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            LineItemDiscount lineItemDiscount = createLineItemDiscount();
            hubLineItemDiscount.add(lineItemDiscount);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            LineItemTax lineItemTax = createLineItemTax();
            hubLineItemTax.add(lineItemTax);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            Manufacturer manufacturer = createManufacturer();
            hubManufacturer.add(manufacturer);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            NewNetPriceCalculater newNetPriceCalculater = createNewNetPriceCalculater();
            hubNewNetPriceCalculater.add(newNetPriceCalculater);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            OnlineOrder onlineOrder = createOnlineOrder();
            hubOnlineOrder.add(onlineOrder);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            OnlineOrderDelivery onlineOrderDelivery = createOnlineOrderDelivery();
            hubOnlineOrderDelivery.add(onlineOrderDelivery);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            OPPConnector oppConnector = createOPPConnector();
            hubOPPConnector.add(oppConnector);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            OutFrontMerch outFrontMerch = createOutFrontMerch();
            hubOutFrontMerch.add(outFrontMerch);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            PaymentConnector paymentConnector = createPaymentConnector();
            hubPaymentConnector.add(paymentConnector);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            Planogram planogram = createPlanogram();
            hubPlanogram.add(planogram);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            Printer printer = createPrinter();
            hubPrinter.add(printer);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            PurchaseOrder purchaseOrder = createPurchaseOrder();
            hubPurchaseOrder.add(purchaseOrder);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            Quote quote = createQuote();
            hubQuote.add(quote);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            Refund refund = createRefund();
            hubRefund.add(refund);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            Report report = createReport();
            hubReport.add(report);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            Reward reward = createReward();
            hubReward.add(reward);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            ScannerConnector scannerConnector = createScannerConnector();
            hubScannerConnector.add(scannerConnector);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            ShippingQuoteConnector shippingQuoteConnector = createShippingQuoteConnector();
            hubShippingQuoteConnector.add(shippingQuoteConnector);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            Store store = createStore();
            hubStore.add(store);
            hubAddress.add(store.getAddress());
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            StoreCycleCount storeCycleCount = createStoreCycleCount();
            hubStoreCycleCount.add(storeCycleCount);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            StoreLayout storeLayout = createStoreLayout();
            hubStoreLayout.add(storeLayout);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            StoreSchedule storeSchedule = createStoreSchedule();
            hubStoreSchedule.add(storeSchedule);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            StoreToDc storeToDc = createStoreToDc();
            hubStoreToDc.add(storeToDc);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            StoreToStoreTransfer storeToStoreTransfer = createStoreToStoreTransfer();
            hubStoreToStoreTransfer.add(storeToStoreTransfer);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            VehicleMake vehicleMake = createVehicleMake();
            hubVehicleMake.add(vehicleMake);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            VehicleModelYear vehicleModelYear = createVehicleModelYear();
            hubVehicleModelYear.add(vehicleModelYear);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            VertexConnector vertexConnector = createVertexConnector();
            hubVertexConnector.add(vertexConnector);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            VinLookup vinLookup = createVinLookup();
            hubVinLookup.add(vinLookup);
        }
        x = 5 + ((int) (Math.random()*20));
        for (int i=0; i<x; i++) {
            ZipCodeLookupConnector zipCodeLookupConnector = createZipCodeLookupConnector();
            hubZipCodeLookupConnector.add(zipCodeLookupConnector);
        }
         
        // Now prepopulate new objects
        // lookups
        for (AppServer appServer : ModelDelegate.getCreateOneAppServerHub()) {
            prepopulate(appServer);
        }
        for (AppUser appUser : ModelDelegate.getAppUsers()) {
            prepopulate(appUser);
        }
        for (BarcodeType barcodeType : ModelDelegate.getBarcodeTypes()) {
            prepopulate(barcodeType);
        }
        for (CurrencyType currencyType : ModelDelegate.getCurrencyTypes()) {
            prepopulate(currencyType);
        }
        for (DeliveryService deliveryService : ModelDelegate.getDeliveryServices()) {
            prepopulate(deliveryService);
        }
        for (DistCenter distCenter : ModelDelegate.getDistCenters()) {
            prepopulate(distCenter);
        }
        for (ItemCategory itemCategory : ModelDelegate.getItemCategories()) {
            prepopulate(itemCategory);
        }
        for (ItemLine itemLine : ModelDelegate.getItemLines()) {
            prepopulate(itemLine);
        }
        for (ItemOptionType itemOptionType : ModelDelegate.getItemOptionTypes()) {
            prepopulate(itemOptionType);
        }
        for (ItemPackType itemPackType : ModelDelegate.getItemPackTypes()) {
            prepopulate(itemPackType);
        }
        for (MeasureType measureType : ModelDelegate.getMeasureTypes()) {
            prepopulate(measureType);
        }
        for (ReportClass reportClass : ModelDelegate.getReportClasses()) {
            prepopulate(reportClass);
        }
        for (RewardType rewardType : ModelDelegate.getRewardTypes()) {
            prepopulate(rewardType);
        }
        for (TMPermission tmPermission : ModelDelegate.getTMPermissions()) {
            prepopulate(tmPermission);
        }
        for (VertexTaxCode vertexTaxCode : ModelDelegate.getVertexTaxCodes()) {
            prepopulate(vertexTaxCode);
        }
        
        // others
        for (Address address : hubAddress) {
            prepopulate(address);
        }
        for (BackroomMap backroomMap : hubBackroomMap) {
            prepopulate(backroomMap);
        }
        for (BankDepositCheck bankDepositCheck : hubBankDepositCheck) {
            prepopulate(bankDepositCheck);
        }
        for (Catalog catalog : hubCatalog) {
            prepopulate(catalog);
        }
        for (CatalogCategory catalogCategory : hubCatalogCategory) {
            prepopulate(catalogCategory);
        }
        for (CatalogItem catalogItem : hubCatalogItem) {
            prepopulate(catalogItem);
        }
        for (Core core : hubCore) {
            prepopulate(core);
        }
        for (CronProcess cronProcess : hubCronProcess) {
            prepopulate(cronProcess);
        }
        for (CurrencyExchangeRate currencyExchangeRate : hubCurrencyExchangeRate) {
            prepopulate(currencyExchangeRate);
        }
        for (Customer customer : hubCustomer) {
            prepopulate(customer);
        }
        for (CustomerConnector customerConnector : hubCustomerConnector) {
            prepopulate(customerConnector);
        }
        for (CustomerCredit customerCredit : hubCustomerCredit) {
            prepopulate(customerCredit);
        }
        for (DcToStore dcToStore : hubDcToStore) {
            prepopulate(dcToStore);
        }
        for (Demo demo : hubDemo) {
            prepopulate(demo);
        }
        for (DiscountCoupon discountCoupon : hubDiscountCoupon) {
            prepopulate(discountCoupon);
        }
        for (DiscountType discountType : hubDiscountType) {
            prepopulate(discountType);
        }
        for (Feedback feedback : hubFeedback) {
            prepopulate(feedback);
        }
        for (Garage garage : hubGarage) {
            prepopulate(garage);
        }
        for (ImageStore imageStore : hubImageStore) {
            prepopulate(imageStore);
        }
        for (InventoryConnector inventoryConnector : hubInventoryConnector) {
            prepopulate(inventoryConnector);
        }
        for (Invoice invoice : hubInvoice) {
            prepopulate(invoice);
        }
        for (InvoiceDiscount invoiceDiscount : hubInvoiceDiscount) {
            prepopulate(invoiceDiscount);
        }
        for (InvoicePaymentCheck invoicePaymentCheck : hubInvoicePaymentCheck) {
            prepopulate(invoicePaymentCheck);
        }
        for (InvoiceRebate invoiceRebate : hubInvoiceRebate) {
            prepopulate(invoiceRebate);
        }
        for (InvoiceShipTo invoiceShipTo : hubInvoiceShipTo) {
            prepopulate(invoiceShipTo);
        }
        for (InvoiceTax invoiceTax : hubInvoiceTax) {
            prepopulate(invoiceTax);
        }
        for (Item item : hubItem) {
            prepopulate(item);
        }
        for (ItemAlert itemAlert : hubItemAlert) {
            prepopulate(itemAlert);
        }
        for (ItemInterchange itemInterchange : hubItemInterchange) {
            prepopulate(itemInterchange);
        }
        for (ItemMSDS itemMSDS : hubItemMSDS) {
            prepopulate(itemMSDS);
        }
        for (ItemRestriction itemRestriction : hubItemRestriction) {
            prepopulate(itemRestriction);
        }
        for (ItemVendor itemVendor : hubItemVendor) {
            prepopulate(itemVendor);
        }
        for (LedgerDenominationBundle ledgerDenominationBundle : hubLedgerDenominationBundle) {
            prepopulate(ledgerDenominationBundle);
        }
        for (LineItemDiscount lineItemDiscount : hubLineItemDiscount) {
            prepopulate(lineItemDiscount);
        }
        for (LineItemTax lineItemTax : hubLineItemTax) {
            prepopulate(lineItemTax);
        }
        for (Manufacturer manufacturer : hubManufacturer) {
            prepopulate(manufacturer);
        }
        for (NewNetPriceCalculater newNetPriceCalculater : hubNewNetPriceCalculater) {
            prepopulate(newNetPriceCalculater);
        }
        for (OnlineOrder onlineOrder : hubOnlineOrder) {
            prepopulate(onlineOrder);
        }
        for (OnlineOrderDelivery onlineOrderDelivery : hubOnlineOrderDelivery) {
            prepopulate(onlineOrderDelivery);
        }
        for (OPPConnector oppConnector : hubOPPConnector) {
            prepopulate(oppConnector);
        }
        for (OutFrontMerch outFrontMerch : hubOutFrontMerch) {
            prepopulate(outFrontMerch);
        }
        for (PaymentConnector paymentConnector : hubPaymentConnector) {
            prepopulate(paymentConnector);
        }
        for (Planogram planogram : hubPlanogram) {
            prepopulate(planogram);
        }
        for (Printer printer : hubPrinter) {
            prepopulate(printer);
        }
        for (PurchaseOrder purchaseOrder : hubPurchaseOrder) {
            prepopulate(purchaseOrder);
        }
        for (Quote quote : hubQuote) {
            prepopulate(quote);
        }
        for (Refund refund : hubRefund) {
            prepopulate(refund);
        }
        for (Report report : hubReport) {
            prepopulate(report);
        }
        for (Reward reward : hubReward) {
            prepopulate(reward);
        }
        for (ScannerConnector scannerConnector : hubScannerConnector) {
            prepopulate(scannerConnector);
        }
        for (ShippingQuoteConnector shippingQuoteConnector : hubShippingQuoteConnector) {
            prepopulate(shippingQuoteConnector);
        }
        for (Store store : hubStore) {
            prepopulate(store);
        }
        for (StoreCycleCount storeCycleCount : hubStoreCycleCount) {
            prepopulate(storeCycleCount);
        }
        for (StoreLayout storeLayout : hubStoreLayout) {
            prepopulate(storeLayout);
        }
        for (StoreSchedule storeSchedule : hubStoreSchedule) {
            prepopulate(storeSchedule);
        }
        for (StoreToDc storeToDc : hubStoreToDc) {
            prepopulate(storeToDc);
        }
        for (StoreToStoreTransfer storeToStoreTransfer : hubStoreToStoreTransfer) {
            prepopulate(storeToStoreTransfer);
        }
        for (VehicleMake vehicleMake : hubVehicleMake) {
            prepopulate(vehicleMake);
        }
        for (VehicleModelYear vehicleModelYear : hubVehicleModelYear) {
            prepopulate(vehicleModelYear);
        }
        for (VertexConnector vertexConnector : hubVertexConnector) {
            prepopulate(vertexConnector);
        }
        for (VinLookup vinLookup : hubVinLookup) {
            prepopulate(vinLookup);
        }
        for (ZipCodeLookupConnector zipCodeLookupConnector : hubZipCodeLookupConnector) {
            prepopulate(zipCodeLookupConnector);
        }
        
        // Now populate new objects
        // lookups
        for (AppServer appServer : ModelDelegate.getCreateOneAppServerHub()) {
            populate(appServer);
        }
        for (AppUser appUser : ModelDelegate.getAppUsers()) {
            populate(appUser);
        }
        for (BarcodeType barcodeType : ModelDelegate.getBarcodeTypes()) {
            populate(barcodeType);
        }
        for (CurrencyType currencyType : ModelDelegate.getCurrencyTypes()) {
            populate(currencyType);
        }
        for (DeliveryService deliveryService : ModelDelegate.getDeliveryServices()) {
            populate(deliveryService);
        }
        for (DistCenter distCenter : ModelDelegate.getDistCenters()) {
            populate(distCenter);
        }
        for (ItemCategory itemCategory : ModelDelegate.getItemCategories()) {
            populate(itemCategory);
        }
        for (ItemLine itemLine : ModelDelegate.getItemLines()) {
            populate(itemLine);
        }
        for (ItemOptionType itemOptionType : ModelDelegate.getItemOptionTypes()) {
            populate(itemOptionType);
        }
        for (ItemPackType itemPackType : ModelDelegate.getItemPackTypes()) {
            populate(itemPackType);
        }
        for (MeasureType measureType : ModelDelegate.getMeasureTypes()) {
            populate(measureType);
        }
        for (ReportClass reportClass : ModelDelegate.getReportClasses()) {
            populate(reportClass);
        }
        for (RewardType rewardType : ModelDelegate.getRewardTypes()) {
            populate(rewardType);
        }
        for (TMPermission tmPermission : ModelDelegate.getTMPermissions()) {
            populate(tmPermission);
        }
        for (VertexTaxCode vertexTaxCode : ModelDelegate.getVertexTaxCodes()) {
            populate(vertexTaxCode);
        }
        
        // others
        for (Address address : hubAddress) {
            populate(address);
        }
        for (BackroomMap backroomMap : hubBackroomMap) {
            populate(backroomMap);
        }
        for (BankDepositCheck bankDepositCheck : hubBankDepositCheck) {
            populate(bankDepositCheck);
        }
        for (Catalog catalog : hubCatalog) {
            populate(catalog);
        }
        for (CatalogCategory catalogCategory : hubCatalogCategory) {
            populate(catalogCategory);
        }
        for (CatalogItem catalogItem : hubCatalogItem) {
            populate(catalogItem);
        }
        for (Core core : hubCore) {
            populate(core);
        }
        for (CronProcess cronProcess : hubCronProcess) {
            populate(cronProcess);
        }
        for (CurrencyExchangeRate currencyExchangeRate : hubCurrencyExchangeRate) {
            populate(currencyExchangeRate);
        }
        for (Customer customer : hubCustomer) {
            populate(customer);
        }
        for (CustomerConnector customerConnector : hubCustomerConnector) {
            populate(customerConnector);
        }
        for (CustomerCredit customerCredit : hubCustomerCredit) {
            populate(customerCredit);
        }
        for (DcToStore dcToStore : hubDcToStore) {
            populate(dcToStore);
        }
        for (Demo demo : hubDemo) {
            populate(demo);
        }
        for (DiscountCoupon discountCoupon : hubDiscountCoupon) {
            populate(discountCoupon);
        }
        for (DiscountType discountType : hubDiscountType) {
            populate(discountType);
        }
        for (Feedback feedback : hubFeedback) {
            populate(feedback);
        }
        for (Garage garage : hubGarage) {
            populate(garage);
        }
        for (ImageStore imageStore : hubImageStore) {
            populate(imageStore);
        }
        for (InventoryConnector inventoryConnector : hubInventoryConnector) {
            populate(inventoryConnector);
        }
        for (Invoice invoice : hubInvoice) {
            populate(invoice);
        }
        for (InvoiceDiscount invoiceDiscount : hubInvoiceDiscount) {
            populate(invoiceDiscount);
        }
        for (InvoicePaymentCheck invoicePaymentCheck : hubInvoicePaymentCheck) {
            populate(invoicePaymentCheck);
        }
        for (InvoiceRebate invoiceRebate : hubInvoiceRebate) {
            populate(invoiceRebate);
        }
        for (InvoiceShipTo invoiceShipTo : hubInvoiceShipTo) {
            populate(invoiceShipTo);
        }
        for (InvoiceTax invoiceTax : hubInvoiceTax) {
            populate(invoiceTax);
        }
        for (Item item : hubItem) {
            populate(item);
        }
        for (ItemAlert itemAlert : hubItemAlert) {
            populate(itemAlert);
        }
        for (ItemInterchange itemInterchange : hubItemInterchange) {
            populate(itemInterchange);
        }
        for (ItemMSDS itemMSDS : hubItemMSDS) {
            populate(itemMSDS);
        }
        for (ItemRestriction itemRestriction : hubItemRestriction) {
            populate(itemRestriction);
        }
        for (ItemVendor itemVendor : hubItemVendor) {
            populate(itemVendor);
        }
        for (LedgerDenominationBundle ledgerDenominationBundle : hubLedgerDenominationBundle) {
            populate(ledgerDenominationBundle);
        }
        for (LineItemDiscount lineItemDiscount : hubLineItemDiscount) {
            populate(lineItemDiscount);
        }
        for (LineItemTax lineItemTax : hubLineItemTax) {
            populate(lineItemTax);
        }
        for (Manufacturer manufacturer : hubManufacturer) {
            populate(manufacturer);
        }
        for (NewNetPriceCalculater newNetPriceCalculater : hubNewNetPriceCalculater) {
            populate(newNetPriceCalculater);
        }
        for (OnlineOrder onlineOrder : hubOnlineOrder) {
            populate(onlineOrder);
        }
        for (OnlineOrderDelivery onlineOrderDelivery : hubOnlineOrderDelivery) {
            populate(onlineOrderDelivery);
        }
        for (OPPConnector oppConnector : hubOPPConnector) {
            populate(oppConnector);
        }
        for (OutFrontMerch outFrontMerch : hubOutFrontMerch) {
            populate(outFrontMerch);
        }
        for (PaymentConnector paymentConnector : hubPaymentConnector) {
            populate(paymentConnector);
        }
        for (Planogram planogram : hubPlanogram) {
            populate(planogram);
        }
        for (Printer printer : hubPrinter) {
            populate(printer);
        }
        for (PurchaseOrder purchaseOrder : hubPurchaseOrder) {
            populate(purchaseOrder);
        }
        for (Quote quote : hubQuote) {
            populate(quote);
        }
        for (Refund refund : hubRefund) {
            populate(refund);
        }
        for (Report report : hubReport) {
            populate(report);
        }
        for (Reward reward : hubReward) {
            populate(reward);
        }
        for (ScannerConnector scannerConnector : hubScannerConnector) {
            populate(scannerConnector);
        }
        for (ShippingQuoteConnector shippingQuoteConnector : hubShippingQuoteConnector) {
            populate(shippingQuoteConnector);
        }
        for (Store store : hubStore) {
            populate(store);
        }
        for (StoreCycleCount storeCycleCount : hubStoreCycleCount) {
            populate(storeCycleCount);
        }
        for (StoreLayout storeLayout : hubStoreLayout) {
            populate(storeLayout);
        }
        for (StoreSchedule storeSchedule : hubStoreSchedule) {
            populate(storeSchedule);
        }
        for (StoreToDc storeToDc : hubStoreToDc) {
            populate(storeToDc);
        }
        for (StoreToStoreTransfer storeToStoreTransfer : hubStoreToStoreTransfer) {
            populate(storeToStoreTransfer);
        }
        for (VehicleMake vehicleMake : hubVehicleMake) {
            populate(vehicleMake);
        }
        for (VehicleModelYear vehicleModelYear : hubVehicleModelYear) {
            populate(vehicleModelYear);
        }
        for (VertexConnector vertexConnector : hubVertexConnector) {
            populate(vertexConnector);
        }
        for (VinLookup vinLookup : hubVinLookup) {
            populate(vinLookup);
        }
        for (ZipCodeLookupConnector zipCodeLookupConnector : hubZipCodeLookupConnector) {
            populate(zipCodeLookupConnector);
        }
    }
    
    // Hubs to hold sample data that is not in ModelDelegate
    private Hub<Address> hubAddress = new Hub<Address>(Address.class);
    private Hub<BackroomMap> hubBackroomMap = new Hub<BackroomMap>(BackroomMap.class);
    private Hub<BankDepositCheck> hubBankDepositCheck = new Hub<BankDepositCheck>(BankDepositCheck.class);
    private Hub<Catalog> hubCatalog = new Hub<Catalog>(Catalog.class);
    private Hub<CatalogCategory> hubCatalogCategory = new Hub<CatalogCategory>(CatalogCategory.class);
    private Hub<CatalogItem> hubCatalogItem = new Hub<CatalogItem>(CatalogItem.class);
    private Hub<Core> hubCore = new Hub<Core>(Core.class);
    private Hub<CronProcess> hubCronProcess = new Hub<CronProcess>(CronProcess.class);
    private Hub<CurrencyExchangeRate> hubCurrencyExchangeRate = new Hub<CurrencyExchangeRate>(CurrencyExchangeRate.class);
    private Hub<Customer> hubCustomer = new Hub<Customer>(Customer.class);
    private Hub<CustomerConnector> hubCustomerConnector = new Hub<CustomerConnector>(CustomerConnector.class);
    private Hub<CustomerCredit> hubCustomerCredit = new Hub<CustomerCredit>(CustomerCredit.class);
    private Hub<DcToStore> hubDcToStore = new Hub<DcToStore>(DcToStore.class);
    private Hub<Demo> hubDemo = new Hub<Demo>(Demo.class);
    private Hub<DiscountCoupon> hubDiscountCoupon = new Hub<DiscountCoupon>(DiscountCoupon.class);
    private Hub<DiscountType> hubDiscountType = new Hub<DiscountType>(DiscountType.class);
    private Hub<Feedback> hubFeedback = new Hub<Feedback>(Feedback.class);
    private Hub<Garage> hubGarage = new Hub<Garage>(Garage.class);
    private Hub<ImageStore> hubImageStore = new Hub<ImageStore>(ImageStore.class);
    private Hub<InventoryConnector> hubInventoryConnector = new Hub<InventoryConnector>(InventoryConnector.class);
    private Hub<Invoice> hubInvoice = new Hub<Invoice>(Invoice.class);
    private Hub<InvoiceDiscount> hubInvoiceDiscount = new Hub<InvoiceDiscount>(InvoiceDiscount.class);
    private Hub<InvoicePaymentCheck> hubInvoicePaymentCheck = new Hub<InvoicePaymentCheck>(InvoicePaymentCheck.class);
    private Hub<InvoiceRebate> hubInvoiceRebate = new Hub<InvoiceRebate>(InvoiceRebate.class);
    private Hub<InvoiceShipTo> hubInvoiceShipTo = new Hub<InvoiceShipTo>(InvoiceShipTo.class);
    private Hub<InvoiceTax> hubInvoiceTax = new Hub<InvoiceTax>(InvoiceTax.class);
    private Hub<Item> hubItem = new Hub<Item>(Item.class);
    private Hub<ItemAlert> hubItemAlert = new Hub<ItemAlert>(ItemAlert.class);
    private Hub<ItemInterchange> hubItemInterchange = new Hub<ItemInterchange>(ItemInterchange.class);
    private Hub<ItemMSDS> hubItemMSDS = new Hub<ItemMSDS>(ItemMSDS.class);
    private Hub<ItemRestriction> hubItemRestriction = new Hub<ItemRestriction>(ItemRestriction.class);
    private Hub<ItemVendor> hubItemVendor = new Hub<ItemVendor>(ItemVendor.class);
    private Hub<LedgerDenominationBundle> hubLedgerDenominationBundle = new Hub<LedgerDenominationBundle>(LedgerDenominationBundle.class);
    private Hub<LineItemDiscount> hubLineItemDiscount = new Hub<LineItemDiscount>(LineItemDiscount.class);
    private Hub<LineItemTax> hubLineItemTax = new Hub<LineItemTax>(LineItemTax.class);
    private Hub<Manufacturer> hubManufacturer = new Hub<Manufacturer>(Manufacturer.class);
    private Hub<NewNetPriceCalculater> hubNewNetPriceCalculater = new Hub<NewNetPriceCalculater>(NewNetPriceCalculater.class);
    private Hub<OnlineOrder> hubOnlineOrder = new Hub<OnlineOrder>(OnlineOrder.class);
    private Hub<OnlineOrderDelivery> hubOnlineOrderDelivery = new Hub<OnlineOrderDelivery>(OnlineOrderDelivery.class);
    private Hub<OPPConnector> hubOPPConnector = new Hub<OPPConnector>(OPPConnector.class);
    private Hub<OutFrontMerch> hubOutFrontMerch = new Hub<OutFrontMerch>(OutFrontMerch.class);
    private Hub<PaymentConnector> hubPaymentConnector = new Hub<PaymentConnector>(PaymentConnector.class);
    private Hub<Planogram> hubPlanogram = new Hub<Planogram>(Planogram.class);
    private Hub<Printer> hubPrinter = new Hub<Printer>(Printer.class);
    private Hub<PurchaseOrder> hubPurchaseOrder = new Hub<PurchaseOrder>(PurchaseOrder.class);
    private Hub<Quote> hubQuote = new Hub<Quote>(Quote.class);
    private Hub<Refund> hubRefund = new Hub<Refund>(Refund.class);
    private Hub<Report> hubReport = new Hub<Report>(Report.class);
    private Hub<Reward> hubReward = new Hub<Reward>(Reward.class);
    private Hub<ScannerConnector> hubScannerConnector = new Hub<ScannerConnector>(ScannerConnector.class);
    private Hub<ShippingQuoteConnector> hubShippingQuoteConnector = new Hub<ShippingQuoteConnector>(ShippingQuoteConnector.class);
    private Hub<Store> hubStore = new Hub<Store>(Store.class);
    private Hub<StoreCycleCount> hubStoreCycleCount = new Hub<StoreCycleCount>(StoreCycleCount.class);
    private Hub<StoreLayout> hubStoreLayout = new Hub<StoreLayout>(StoreLayout.class);
    private Hub<StoreSchedule> hubStoreSchedule = new Hub<StoreSchedule>(StoreSchedule.class);
    private Hub<StoreToDc> hubStoreToDc = new Hub<StoreToDc>(StoreToDc.class);
    private Hub<StoreToStoreTransfer> hubStoreToStoreTransfer = new Hub<StoreToStoreTransfer>(StoreToStoreTransfer.class);
    private Hub<VehicleMake> hubVehicleMake = new Hub<VehicleMake>(VehicleMake.class);
    private Hub<VehicleModelYear> hubVehicleModelYear = new Hub<VehicleModelYear>(VehicleModelYear.class);
    private Hub<VertexConnector> hubVertexConnector = new Hub<VertexConnector>(VertexConnector.class);
    private Hub<VinLookup> hubVinLookup = new Hub<VinLookup>(VinLookup.class);
    private Hub<ZipCodeLookupConnector> hubZipCodeLookupConnector = new Hub<ZipCodeLookupConnector>(ZipCodeLookupConnector.class);
    
    public static void main(String[] args) {
        //qqqqqqqqq OAObjectCallbackDelegate.demoAllowAllToPass(true);
        DataGenerator dg = new DataGenerator();
        dg.createSamples();
        System.out.println("createSamples is done");
    }
}
 
