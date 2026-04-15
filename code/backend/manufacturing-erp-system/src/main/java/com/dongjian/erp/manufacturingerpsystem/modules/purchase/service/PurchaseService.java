package com.dongjian.erp.manufacturingerpsystem.modules.purchase.service;

import com.dongjian.erp.manufacturingerpsystem.modules.purchase.dto.PurchaseOrderSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.purchase.dto.PurchaseInStockRequest;
import com.dongjian.erp.manufacturingerpsystem.common.page.PageResponse;
import com.dongjian.erp.manufacturingerpsystem.modules.purchase.vo.PurchaseOrderDetail;
import com.dongjian.erp.manufacturingerpsystem.modules.purchase.vo.PurchaseOrderListItem;

public interface PurchaseService {

    PageResponse<PurchaseOrderListItem> pageRequests(String keyword, String status, long pageNo, long pageSize);

    PurchaseOrderDetail getRequest(Long id);

    Long createRequest(PurchaseOrderSaveRequest request);

    void updateRequest(Long id, PurchaseOrderSaveRequest request);

    void approveRequest(Long id);

    void closeRequest(Long id);

    PageResponse<PurchaseOrderListItem> pageOrders(String keyword, String status, long pageNo, long pageSize);

    PurchaseOrderDetail getOrder(Long id);

    Long createOrder(PurchaseOrderSaveRequest request);

    void updateOrder(Long id, PurchaseOrderSaveRequest request);

    void approveOrder(Long id);

    void closeOrder(Long id);

    Long inStock(Long id, PurchaseInStockRequest request);
}
