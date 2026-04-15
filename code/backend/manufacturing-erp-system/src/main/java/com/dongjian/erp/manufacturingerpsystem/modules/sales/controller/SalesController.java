package com.dongjian.erp.manufacturingerpsystem.modules.sales.controller;

import com.dongjian.erp.manufacturingerpsystem.common.page.PageResponse;
import com.dongjian.erp.manufacturingerpsystem.common.result.ApiResponse;
import com.dongjian.erp.manufacturingerpsystem.modules.sales.dto.SalesOrderSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.sales.dto.SalesOutStockRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.sales.service.SalesService;
import com.dongjian.erp.manufacturingerpsystem.modules.sales.vo.SalesOrderDetail;
import com.dongjian.erp.manufacturingerpsystem.modules.sales.vo.SalesOrderListItem;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sales")
public class SalesController {

    private final SalesService salesService;

    public SalesController(SalesService salesService) {
        this.salesService = salesService;
    }

    @GetMapping("/quotes")
    public ApiResponse<PageResponse<SalesOrderListItem>> quotes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ApiResponse.success(salesService.pageQuotes(keyword, status, pageNo, pageSize));
    }

    @GetMapping("/quotes/{id}")
    public ApiResponse<SalesOrderDetail> quote(@PathVariable Long id) {
        return ApiResponse.success(salesService.getQuote(id));
    }

    @PostMapping("/quotes")
    public ApiResponse<Long> createQuote(@Valid @RequestBody SalesOrderSaveRequest request) {
        return ApiResponse.success("created", salesService.createQuote(request));
    }

    @PutMapping("/quotes/{id}")
    public ApiResponse<Void> updateQuote(@PathVariable Long id, @Valid @RequestBody SalesOrderSaveRequest request) {
        salesService.updateQuote(id, request);
        return ApiResponse.success("updated", null);
    }

    @PutMapping("/quotes/{id}/approve")
    public ApiResponse<Void> approveQuote(@PathVariable Long id) {
        salesService.approveQuote(id);
        return ApiResponse.success("approved", null);
    }

    @GetMapping("/orders")
    public ApiResponse<PageResponse<SalesOrderListItem>> orders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ApiResponse.success(salesService.pageOrders(keyword, status, pageNo, pageSize));
    }

    @GetMapping("/orders/{id}")
    public ApiResponse<SalesOrderDetail> order(@PathVariable Long id) {
        return ApiResponse.success(salesService.getOrder(id));
    }

    @PostMapping("/orders")
    public ApiResponse<Long> createOrder(@Valid @RequestBody SalesOrderSaveRequest request) {
        return ApiResponse.success("created", salesService.createOrder(request));
    }

    @PutMapping("/orders/{id}")
    public ApiResponse<Void> updateOrder(@PathVariable Long id, @Valid @RequestBody SalesOrderSaveRequest request) {
        salesService.updateOrder(id, request);
        return ApiResponse.success("updated", null);
    }

    @PutMapping("/orders/{id}/approve")
    public ApiResponse<Void> approveOrder(@PathVariable Long id) {
        salesService.approveOrder(id);
        return ApiResponse.success("approved", null);
    }

    @PutMapping("/orders/{id}/close")
    public ApiResponse<Void> closeOrder(@PathVariable Long id) {
        salesService.closeOrder(id);
        return ApiResponse.success("closed", null);
    }

    @PostMapping("/orders/{id}/out-stock")
    public ApiResponse<Long> outStock(@PathVariable Long id, @Valid @RequestBody SalesOutStockRequest request) {
        return ApiResponse.success("created", salesService.outStock(id, request));
    }
}
