package com.dongjian.erp.manufacturingerpsystem.modules.purchase.controller;

import com.dongjian.erp.manufacturingerpsystem.common.page.PageResponse;
import com.dongjian.erp.manufacturingerpsystem.common.result.ApiResponse;
import com.dongjian.erp.manufacturingerpsystem.modules.purchase.dto.PurchaseInStockRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.purchase.dto.PurchaseOrderSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.purchase.service.PurchaseService;
import com.dongjian.erp.manufacturingerpsystem.modules.purchase.vo.PurchaseOrderDetail;
import com.dongjian.erp.manufacturingerpsystem.modules.purchase.vo.PurchaseOrderListItem;
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
@RequestMapping("/api/v1/purchase")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @GetMapping("/requests")
    public ApiResponse<PageResponse<PurchaseOrderListItem>> requests(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ApiResponse.success(purchaseService.pageRequests(keyword, status, pageNo, pageSize));
    }

    @GetMapping("/requests/{id}")
    public ApiResponse<PurchaseOrderDetail> request(@PathVariable Long id) {
        return ApiResponse.success(purchaseService.getRequest(id));
    }

    @PostMapping("/requests")
    public ApiResponse<Long> createRequest(@Valid @RequestBody PurchaseOrderSaveRequest request) {
        return ApiResponse.success("created", purchaseService.createRequest(request));
    }

    @PutMapping("/requests/{id}")
    public ApiResponse<Void> updateRequest(@PathVariable Long id, @Valid @RequestBody PurchaseOrderSaveRequest request) {
        purchaseService.updateRequest(id, request);
        return ApiResponse.success("updated", null);
    }

    @PutMapping("/requests/{id}/approve")
    public ApiResponse<Void> approveRequest(@PathVariable Long id) {
        purchaseService.approveRequest(id);
        return ApiResponse.success("approved", null);
    }

    @PutMapping("/requests/{id}/close")
    public ApiResponse<Void> closeRequest(@PathVariable Long id) {
        purchaseService.closeRequest(id);
        return ApiResponse.success("closed", null);
    }

    @GetMapping("/orders")
    public ApiResponse<PageResponse<PurchaseOrderListItem>> orders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ApiResponse.success(purchaseService.pageOrders(keyword, status, pageNo, pageSize));
    }

    @GetMapping("/orders/{id}")
    public ApiResponse<PurchaseOrderDetail> order(@PathVariable Long id) {
        return ApiResponse.success(purchaseService.getOrder(id));
    }

    @PostMapping("/orders")
    public ApiResponse<Long> createOrder(@Valid @RequestBody PurchaseOrderSaveRequest request) {
        return ApiResponse.success("created", purchaseService.createOrder(request));
    }

    @PutMapping("/orders/{id}")
    public ApiResponse<Void> updateOrder(@PathVariable Long id, @Valid @RequestBody PurchaseOrderSaveRequest request) {
        purchaseService.updateOrder(id, request);
        return ApiResponse.success("updated", null);
    }

    @PutMapping("/orders/{id}/approve")
    public ApiResponse<Void> approveOrder(@PathVariable Long id) {
        purchaseService.approveOrder(id);
        return ApiResponse.success("approved", null);
    }

    @PutMapping("/orders/{id}/close")
    public ApiResponse<Void> closeOrder(@PathVariable Long id) {
        purchaseService.closeOrder(id);
        return ApiResponse.success("closed", null);
    }

    @PostMapping("/orders/{id}/in-stock")
    public ApiResponse<Long> inStock(@PathVariable Long id, @Valid @RequestBody PurchaseInStockRequest request) {
        return ApiResponse.success("created", purchaseService.inStock(id, request));
    }
}
