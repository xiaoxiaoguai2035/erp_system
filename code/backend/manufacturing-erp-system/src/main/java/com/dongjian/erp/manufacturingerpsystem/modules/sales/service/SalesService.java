package com.dongjian.erp.manufacturingerpsystem.modules.sales.service;

import com.dongjian.erp.manufacturingerpsystem.modules.sales.dto.SalesOrderSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.sales.dto.SalesOutStockRequest;
import com.dongjian.erp.manufacturingerpsystem.common.page.PageResponse;
import com.dongjian.erp.manufacturingerpsystem.modules.sales.vo.SalesOrderDetail;
import com.dongjian.erp.manufacturingerpsystem.modules.sales.vo.SalesOrderListItem;

public interface SalesService {

    PageResponse<SalesOrderListItem> pageQuotes(String keyword, String status, long pageNo, long pageSize);

    SalesOrderDetail getQuote(Long id);

    Long createQuote(SalesOrderSaveRequest request);

    void updateQuote(Long id, SalesOrderSaveRequest request);

    void approveQuote(Long id);

    PageResponse<SalesOrderListItem> pageOrders(String keyword, String status, long pageNo, long pageSize);

    SalesOrderDetail getOrder(Long id);

    Long createOrder(SalesOrderSaveRequest request);

    void updateOrder(Long id, SalesOrderSaveRequest request);

    void approveOrder(Long id);

    void closeOrder(Long id);

    Long outStock(Long id, SalesOutStockRequest request);
}
