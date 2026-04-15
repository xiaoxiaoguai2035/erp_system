import http from "./http";

export const loginByPassword = (payload) => http.post("/auth/login", payload);
export const logoutRequest = () => http.post("/auth/logout");
export const fetchMyProfile = () => http.get("/auth/me");
export const fetchHealth = () => http.get("/health");

export const fetchMaterialUnits = () => http.get("/system/dicts/material_unit");

export const fetchMaterialList = (params) => http.get("/basic/materials", { params });
export const fetchMaterialDetail = (id) => http.get(`/basic/materials/${id}`);
export const createMaterial = (payload) => http.post("/basic/materials", payload);
export const updateMaterial = (id, payload) => http.put(`/basic/materials/${id}`, payload);
export const deleteMaterial = (id) => http.delete(`/basic/materials/${id}`);

export const fetchCustomerList = (params) => http.get("/basic/customers", { params });
export const fetchCustomerDetail = (id) => http.get(`/basic/customers/${id}`);
export const createCustomer = (payload) => http.post("/basic/customers", payload);
export const updateCustomer = (id, payload) => http.put(`/basic/customers/${id}`, payload);
export const deleteCustomer = (id) => http.delete(`/basic/customers/${id}`);

export const fetchSupplierList = (params) => http.get("/basic/suppliers", { params });
export const fetchSupplierDetail = (id) => http.get(`/basic/suppliers/${id}`);
export const createSupplier = (payload) => http.post("/basic/suppliers", payload);
export const updateSupplier = (id, payload) => http.put(`/basic/suppliers/${id}`, payload);
export const deleteSupplier = (id) => http.delete(`/basic/suppliers/${id}`);

export const fetchWarehouseList = (params) => http.get("/basic/warehouses", { params });
export const fetchWarehouseDetail = (id) => http.get(`/basic/warehouses/${id}`);
export const createWarehouse = (payload) => http.post("/basic/warehouses", payload);
export const updateWarehouse = (id, payload) => http.put(`/basic/warehouses/${id}`, payload);
export const deleteWarehouse = (id) => http.delete(`/basic/warehouses/${id}`);

export const fetchBomList = (params) => http.get("/basic/boms", { params });
export const fetchBomDetail = (id) => http.get(`/basic/boms/${id}`);
export const createBom = (payload) => http.post("/basic/boms", payload);
export const updateBom = (id, payload) => http.put(`/basic/boms/${id}`, payload);
export const deleteBom = (id) => http.delete(`/basic/boms/${id}`);

export const fetchRouteList = (params) => http.get("/basic/routes", { params });
export const fetchRouteDetail = (id) => http.get(`/basic/routes/${id}`);
export const createRoute = (payload) => http.post("/basic/routes", payload);
export const updateRoute = (id, payload) => http.put(`/basic/routes/${id}`, payload);
export const deleteRoute = (id) => http.delete(`/basic/routes/${id}`);

export const fetchPurchaseOrders = (params) => http.get("/purchase/orders", { params });
export const fetchPurchaseOrderDetail = (id) => http.get(`/purchase/orders/${id}`);
export const createPurchaseOrder = (payload) => http.post("/purchase/orders", payload);
export const updatePurchaseOrder = (id, payload) => http.put(`/purchase/orders/${id}`, payload);
export const approvePurchaseOrder = (id) => http.put(`/purchase/orders/${id}/approve`);
export const closePurchaseOrder = (id) => http.put(`/purchase/orders/${id}/close`);
export const purchaseOrderInStock = (id, payload) => http.post(`/purchase/orders/${id}/in-stock`, payload);
export const fetchPurchaseRequests = (params) => http.get("/purchase/requests", { params });
export const fetchPurchaseRequestDetail = (id) => http.get(`/purchase/requests/${id}`);
export const createPurchaseRequest = (payload) => http.post("/purchase/requests", payload);
export const updatePurchaseRequest = (id, payload) => http.put(`/purchase/requests/${id}`, payload);
export const approvePurchaseRequest = (id) => http.put(`/purchase/requests/${id}/approve`);
export const closePurchaseRequest = (id) => http.put(`/purchase/requests/${id}/close`);

export const fetchSalesOrders = (params) => http.get("/sales/orders", { params });
export const fetchSalesOrderDetail = (id) => http.get(`/sales/orders/${id}`);
export const createSalesOrder = (payload) => http.post("/sales/orders", payload);
export const updateSalesOrder = (id, payload) => http.put(`/sales/orders/${id}`, payload);
export const approveSalesOrder = (id) => http.put(`/sales/orders/${id}/approve`);
export const closeSalesOrder = (id) => http.put(`/sales/orders/${id}/close`);
export const salesOrderOutStock = (id, payload) => http.post(`/sales/orders/${id}/out-stock`, payload);
export const fetchSalesQuotes = (params) => http.get("/sales/quotes", { params });
export const fetchSalesQuoteDetail = (id) => http.get(`/sales/quotes/${id}`);
export const createSalesQuote = (payload) => http.post("/sales/quotes", payload);
export const updateSalesQuote = (id, payload) => http.put(`/sales/quotes/${id}`, payload);
export const approveSalesQuote = (id) => http.put(`/sales/quotes/${id}/approve`);

export const fetchInventoryStocks = (params) => http.get("/inventory/stocks", { params });
export const fetchInventoryDocs = (params) => http.get("/inventory/docs", { params });
export const fetchInventoryDocDetail = (id) => http.get(`/inventory/docs/${id}`);
export const fetchInventoryTransfers = (params) => http.get("/inventory/transfers", { params });
export const fetchInventoryTransferDetail = (id) => http.get(`/inventory/transfers/${id}`);
export const createInventoryTransfer = (payload) => http.post("/inventory/transfers", payload);
export const approveInventoryTransfer = (id) => http.put(`/inventory/transfers/${id}/approve`);
export const fetchInventoryChecks = (params) => http.get("/inventory/checks", { params });
export const fetchInventoryCheckDetail = (id) => http.get(`/inventory/checks/${id}`);
export const createInventoryCheck = (payload) => http.post("/inventory/checks", payload);
export const approveInventoryCheck = (id) => http.put(`/inventory/checks/${id}/approve`);

export const fetchProductionPlans = (params) => http.get("/production/plans", { params });
export const fetchProductionPlanDetail = (id) => http.get(`/production/plans/${id}`);
export const createProductionPlan = (payload) => http.post("/production/plans", payload);
export const updateProductionPlan = (id, payload) => http.put(`/production/plans/${id}`, payload);
export const approveProductionPlan = (id) => http.put(`/production/plans/${id}/approve`);
export const closeProductionPlan = (id) => http.put(`/production/plans/${id}/close`);
export const calculateProductionMrp = (payload) => http.post("/production/mrp/calculate", payload);
export const generateProductionPurchase = (payload) => http.post("/production/mrp/generate-purchase", payload);
export const generateProductionWorkOrders = (payload) => http.post("/production/mrp/generate-work-orders", payload);
export const fetchProductionWorkOrders = (params) => http.get("/production/work-orders", { params });
export const fetchProductionWorkOrderDetail = (id) => http.get(`/production/work-orders/${id}`);
export const createProductionWorkOrder = (payload) => http.post("/production/work-orders", payload);
export const updateProductionWorkOrder = (id, payload) => http.put(`/production/work-orders/${id}`, payload);
export const approveProductionWorkOrder = (id) => http.put(`/production/work-orders/${id}/approve`);
export const closeProductionWorkOrder = (id) => http.put(`/production/work-orders/${id}/close`);
export const pickProductionWorkOrder = (id, payload) => http.post(`/production/work-orders/${id}/pick`, payload);
export const finishProductionWorkOrder = (id, payload) => http.post(`/production/work-orders/${id}/finish-in`, payload);
export const fetchProductionWorkOrderProgress = (id) => http.get(`/production/work-orders/${id}/progress`);
export const fetchProductionReports = (params) => http.get("/production/reports", { params });
export const createProductionReport = (payload) => http.post("/production/reports", payload);

export const fetchSystemUsers = (params) => http.get("/system/users", { params });
export const fetchSystemUserDetail = (id) => http.get(`/system/users/${id}`);
export const createSystemUser = (payload) => http.post("/system/users", payload);
export const updateSystemUser = (id, payload) => http.put(`/system/users/${id}`, payload);
export const updateSystemUserStatus = (id, payload) => http.put(`/system/users/${id}/status`, payload);
export const resetSystemUserPassword = (id, payload) => http.put(`/system/users/${id}/reset-password`, payload);
export const deleteSystemUser = (id) => http.delete(`/system/users/${id}`);

export const fetchSystemRoles = (params) => http.get("/system/roles", { params });
export const fetchSystemRoleDetail = (id) => http.get(`/system/roles/${id}`);
export const createSystemRole = (payload) => http.post("/system/roles", payload);
export const updateSystemRole = (id, payload) => http.put(`/system/roles/${id}`, payload);
export const updateSystemRoleMenus = (id, payload) => http.put(`/system/roles/${id}/menus`, payload);
export const deleteSystemRole = (id) => http.delete(`/system/roles/${id}`);

export const fetchSystemMenuTree = () => http.get("/system/menus/tree");
export const fetchSystemMenuDetail = (id) => http.get(`/system/menus/${id}`);
export const createSystemMenu = (payload) => http.post("/system/menus", payload);
export const updateSystemMenu = (id, payload) => http.put(`/system/menus/${id}`, payload);
export const deleteSystemMenu = (id) => http.delete(`/system/menus/${id}`);

export const fetchSalesSummary = (params) => http.get("/reports/sales-summary", { params });
export const fetchPurchaseSummary = (params) => http.get("/reports/purchase-summary", { params });
export const fetchInventorySummary = (params) => http.get("/reports/inventory-summary", { params });
export const fetchProductionSummary = (params) => http.get("/reports/production-summary", { params });
export const fetchArSummary = (params) => http.get("/reports/ar-summary", { params });
export const fetchApSummary = (params) => http.get("/reports/ap-summary", { params });

export const chatWithAi = (payload) => http.post("/ai/chat", payload);
export const fetchAiReportInsight = (payload) => http.post("/ai/report-insight", payload);
