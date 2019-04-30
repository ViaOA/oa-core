// Copied from OATemplate project by OABuilder 07/01/16 07:41 AM
package com.cdi.model.oa.propertypath;

/**
 * Used to build compiler safe property paths.
 * @author vvia
 */
public class PP {
    /*$$Start: PPInterface.code $$*/
    public static AppServerPPx appServer() {
        return new AppServerPPx("AppServer");
    }
    public static AppServerPPx appServers() {
        return new AppServerPPx("AppServers");
    }
    public static AppUserPPx appUser() {
        return new AppUserPPx("AppUser");
    }
    public static AppUserPPx appUsers() {
        return new AppUserPPx("AppUsers");
    }
    public static AppUserErrorPPx appUserError() {
        return new AppUserErrorPPx("AppUserError");
    }
    public static AppUserErrorPPx appUserErrors() {
        return new AppUserErrorPPx("AppUserErrors");
    }
    public static AppUserLoginPPx appUserLogin() {
        return new AppUserLoginPPx("AppUserLogin");
    }
    public static AppUserLoginPPx appUserLogins() {
        return new AppUserLoginPPx("AppUserLogins");
    }
    public static ColorCodePPx colorCode() {
        return new ColorCodePPx("ColorCode");
    }
    public static ColorCodePPx colorCodes() {
        return new ColorCodePPx("ColorCodes");
    }
    public static ContactPPx contact() {
        return new ContactPPx("Contact");
    }
    public static ContactPPx contacts() {
        return new ContactPPx("Contacts");
    }
    public static CountryPPx country() {
        return new CountryPPx("Country");
    }
    public static CountryPPx countries() {
        return new CountryPPx("Countries");
    }
    public static CustomerPPx customer() {
        return new CustomerPPx("Customer");
    }
    public static CustomerPPx customers() {
        return new CustomerPPx("Customers");
    }
    public static DeliveryPPx delivery() {
        return new DeliveryPPx("Delivery");
    }
    public static DeliveryPPx deliveries() {
        return new DeliveryPPx("Deliveries");
    }
    public static DeliveryTruckPPx deliveryTruck() {
        return new DeliveryTruckPPx("DeliveryTruck");
    }
    public static DeliveryTruckPPx deliveryTrucks() {
        return new DeliveryTruckPPx("DeliveryTrucks");
    }
    public static FileInfoPPx fileInfo() {
        return new FileInfoPPx("FileInfo");
    }
    public static FileInfoPPx fileInfos() {
        return new FileInfoPPx("FileInfos");
    }
    public static ImageStorePPx imageStore() {
        return new ImageStorePPx("ImageStore");
    }
    public static ImageStorePPx imageStores() {
        return new ImageStorePPx("ImageStores");
    }
    public static ItemPPx item() {
        return new ItemPPx("Item");
    }
    public static ItemPPx items() {
        return new ItemPPx("Items");
    }
    public static ItemAddOnPPx itemAddOn() {
        return new ItemAddOnPPx("ItemAddOn");
    }
    public static ItemAddOnPPx itemAddOns() {
        return new ItemAddOnPPx("ItemAddOns");
    }
    public static ItemCategoryPPx itemCategory() {
        return new ItemCategoryPPx("ItemCategory");
    }
    public static ItemCategoryPPx itemCategories() {
        return new ItemCategoryPPx("ItemCategories");
    }
    public static ItemQuotePPx itemQuote() {
        return new ItemQuotePPx("ItemQuote");
    }
    public static ItemQuotePPx itemQuotes() {
        return new ItemQuotePPx("ItemQuotes");
    }
    public static MoldPPx mold() {
        return new MoldPPx("Mold");
    }
    public static MoldPPx molds() {
        return new MoldPPx("Molds");
    }
    public static OrderPPx order() {
        return new OrderPPx("Order");
    }
    public static OrderPPx orders() {
        return new OrderPPx("Orders");
    }
    public static OrderContactPPx orderContact() {
        return new OrderContactPPx("OrderContact");
    }
    public static OrderContactPPx orderContacts() {
        return new OrderContactPPx("OrderContacts");
    }
    public static OrderItemPPx orderItem() {
        return new OrderItemPPx("OrderItem");
    }
    public static OrderItemPPx orderItems() {
        return new OrderItemPPx("OrderItems");
    }
    public static OrderItemCommentPPx orderItemComment() {
        return new OrderItemCommentPPx("OrderItemComment");
    }
    public static OrderItemCommentPPx orderItemComments() {
        return new OrderItemCommentPPx("OrderItemComments");
    }
    public static OrderNotePPx orderNote() {
        return new OrderNotePPx("OrderNote");
    }
    public static OrderNotePPx orderNotes() {
        return new OrderNotePPx("OrderNotes");
    }
    public static PalletPPx pallet() {
        return new PalletPPx("Pallet");
    }
    public static PalletPPx pallets() {
        return new PalletPPx("Pallets");
    }
    public static PhonePPx phone() {
        return new PhonePPx("Phone");
    }
    public static PhonePPx phones() {
        return new PhonePPx("Phones");
    }
    public static PriceCodePPx priceCode() {
        return new PriceCodePPx("PriceCode");
    }
    public static PriceCodePPx priceCodes() {
        return new PriceCodePPx("PriceCodes");
    }
    public static ProductionAreaPPx productionArea() {
        return new ProductionAreaPPx("ProductionArea");
    }
    public static ProductionAreaPPx productionAreas() {
        return new ProductionAreaPPx("ProductionAreas");
    }
    public static QuickbookPPx quickbook() {
        return new QuickbookPPx("Quickbook");
    }
    public static QuickbookPPx quickbooks() {
        return new QuickbookPPx("Quickbooks");
    }
    public static RegionPPx region() {
        return new RegionPPx("Region");
    }
    public static RegionPPx regions() {
        return new RegionPPx("Regions");
    }
    public static SalesCustomerPPx salesCustomer() {
        return new SalesCustomerPPx("SalesCustomer");
    }
    public static SalesCustomerPPx salesCustomers() {
        return new SalesCustomerPPx("SalesCustomers");
    }
    public static SalesOrderPPx salesOrder() {
        return new SalesOrderPPx("SalesOrder");
    }
    public static SalesOrderPPx salesOrders() {
        return new SalesOrderPPx("SalesOrders");
    }
    public static SalesOrderItemPPx salesOrderItem() {
        return new SalesOrderItemPPx("SalesOrderItem");
    }
    public static SalesOrderItemPPx salesOrderItems() {
        return new SalesOrderItemPPx("SalesOrderItems");
    }
    public static SalesOrderNotePPx salesOrderNote() {
        return new SalesOrderNotePPx("SalesOrderNote");
    }
    public static SalesOrderNotePPx salesOrderNotes() {
        return new SalesOrderNotePPx("SalesOrderNotes");
    }
    public static SalesOrderSourcePPx salesOrderSource() {
        return new SalesOrderSourcePPx("SalesOrderSource");
    }
    public static SalesOrderSourcePPx salesOrderSources() {
        return new SalesOrderSourcePPx("SalesOrderSources");
    }
    public static SalesOrderStatusPPx salesOrderStatus() {
        return new SalesOrderStatusPPx("SalesOrderStatus");
    }
    public static SalesOrderStatusPPx salesOrderStatuses() {
        return new SalesOrderStatusPPx("SalesOrderStatuses");
    }
    public static ScheduleTypePPx scheduleType() {
        return new ScheduleTypePPx("ScheduleType");
    }
    public static ScheduleTypePPx scheduleTypes() {
        return new ScheduleTypePPx("ScheduleTypes");
    }
    public static ServiceCodePPx serviceCode() {
        return new ServiceCodePPx("ServiceCode");
    }
    public static ServiceCodePPx serviceCodes() {
        return new ServiceCodePPx("ServiceCodes");
    }
    public static ShipToPPx shipTo() {
        return new ShipToPPx("ShipTo");
    }
    public static ShipToPPx shipTos() {
        return new ShipToPPx("ShipTos");
    }
    public static TaxCodePPx taxCode() {
        return new TaxCodePPx("TaxCode");
    }
    public static TaxCodePPx taxCodes() {
        return new TaxCodePPx("TaxCodes");
    }
    public static TexturePPx texture() {
        return new TexturePPx("Texture");
    }
    public static TexturePPx textures() {
        return new TexturePPx("Textures");
    }
    public static TruckPPx truck() {
        return new TruckPPx("Truck");
    }
    public static TruckPPx trucks() {
        return new TruckPPx("Trucks");
    }
    public static UserPPx user() {
        return new UserPPx("User");
    }
    public static UserPPx users() {
        return new UserPPx("Users");
    }
    public static WebItemPPx webItem() {
        return new WebItemPPx("WebItem");
    }
    public static WebItemPPx webItems() {
        return new WebItemPPx("WebItems");
    }
    public static WebPagePPx webPage() {
        return new WebPagePPx("WebPage");
    }
    public static WebPagePPx webPages() {
        return new WebPagePPx("WebPages");
    }
    public static WebPartPPx webPart() {
        return new WebPartPPx("WebPart");
    }
    public static WebPartPPx webParts() {
        return new WebPartPPx("WebParts");
    }
    public static WODeliveryPPx woDelivery() {
        return new WODeliveryPPx("WODelivery");
    }
    public static WODeliveryPPx woDeliveries() {
        return new WODeliveryPPx("WODeliveries");
    }
    public static WOItemPPx woItem() {
        return new WOItemPPx("WOItem");
    }
    public static WOItemPPx woItems() {
        return new WOItemPPx("WOItems");
    }
    public static WorkOrderPPx workOrder() {
        return new WorkOrderPPx("WorkOrder");
    }
    public static WorkOrderPPx workOrders() {
        return new WorkOrderPPx("WorkOrders");
    }
    public static WorkOrderPalletPPx workOrderPallet() {
        return new WorkOrderPalletPPx("WorkOrderPallet");
    }
    public static WorkOrderPalletPPx workOrderPallets() {
        return new WorkOrderPalletPPx("WorkOrderPallets");
    }
    /*$$End: PPInterface.code $$*/
}
