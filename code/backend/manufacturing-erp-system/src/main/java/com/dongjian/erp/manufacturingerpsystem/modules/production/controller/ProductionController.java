package com.dongjian.erp.manufacturingerpsystem.modules.production.controller;

import com.dongjian.erp.manufacturingerpsystem.common.page.PageResponse;
import com.dongjian.erp.manufacturingerpsystem.common.result.ApiResponse;
import com.dongjian.erp.manufacturingerpsystem.modules.production.dto.MrpCalculateRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.production.dto.MrpGenerateRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.production.dto.ProductionPlanSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.production.dto.ReportSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.production.dto.WorkOrderFinishInRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.production.dto.WorkOrderPickRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.production.dto.WorkOrderSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.production.service.ProductionService;
import com.dongjian.erp.manufacturingerpsystem.modules.production.vo.MrpResult;
import com.dongjian.erp.manufacturingerpsystem.modules.production.vo.ProductionPlanListItem;
import com.dongjian.erp.manufacturingerpsystem.modules.production.vo.ReportListItem;
import com.dongjian.erp.manufacturingerpsystem.modules.production.vo.WorkOrderListItem;
import com.dongjian.erp.manufacturingerpsystem.modules.production.vo.WorkOrderProgress;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/production")
public class ProductionController {

    private final ProductionService productionService;

    public ProductionController(ProductionService productionService) {
        this.productionService = productionService;
    }

    @GetMapping("/plans")
    public ApiResponse<PageResponse<ProductionPlanListItem>> plans(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ApiResponse.success(productionService.pagePlans(keyword, status, pageNo, pageSize));
    }

    @GetMapping("/plans/{id}")
    public ApiResponse<ProductionPlanListItem> plan(@PathVariable Long id) {
        return ApiResponse.success(productionService.getPlan(id));
    }

    @PostMapping("/plans")
    public ApiResponse<Long> createPlan(@Valid @RequestBody ProductionPlanSaveRequest request) {
        return ApiResponse.success("created", productionService.createPlan(request));
    }

    @PutMapping("/plans/{id}")
    public ApiResponse<Void> updatePlan(@PathVariable Long id, @Valid @RequestBody ProductionPlanSaveRequest request) {
        productionService.updatePlan(id, request);
        return ApiResponse.success("updated", null);
    }

    @PutMapping("/plans/{id}/approve")
    public ApiResponse<Void> approvePlan(@PathVariable Long id) {
        productionService.approvePlan(id);
        return ApiResponse.success("approved", null);
    }

    @PutMapping("/plans/{id}/close")
    public ApiResponse<Void> closePlan(@PathVariable Long id) {
        productionService.closePlan(id);
        return ApiResponse.success("closed", null);
    }

    @PostMapping("/mrp/calculate")
    public ApiResponse<MrpResult> calculateMrp(@Valid @RequestBody MrpCalculateRequest request) {
        return ApiResponse.success(productionService.calculateMrp(request));
    }

    @PostMapping("/mrp/generate-purchase")
    public ApiResponse<List<Long>> generatePurchase(@Valid @RequestBody MrpGenerateRequest request) {
        return ApiResponse.success("created", productionService.generatePurchaseByMrp(request));
    }

    @PostMapping("/mrp/generate-work-orders")
    public ApiResponse<List<Long>> generateWorkOrders(@Valid @RequestBody MrpGenerateRequest request) {
        return ApiResponse.success("created", productionService.generateWorkOrdersByMrp(request));
    }

    @GetMapping("/work-orders")
    public ApiResponse<PageResponse<WorkOrderListItem>> workOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ApiResponse.success(productionService.pageWorkOrders(keyword, status, pageNo, pageSize));
    }

    @GetMapping("/work-orders/{id}")
    public ApiResponse<WorkOrderListItem> workOrder(@PathVariable Long id) {
        return ApiResponse.success(productionService.getWorkOrder(id));
    }

    @PostMapping("/work-orders")
    public ApiResponse<Long> createWorkOrder(@Valid @RequestBody WorkOrderSaveRequest request) {
        return ApiResponse.success("created", productionService.createWorkOrder(request));
    }

    @PutMapping("/work-orders/{id}")
    public ApiResponse<Void> updateWorkOrder(@PathVariable Long id, @Valid @RequestBody WorkOrderSaveRequest request) {
        productionService.updateWorkOrder(id, request);
        return ApiResponse.success("updated", null);
    }

    @PutMapping("/work-orders/{id}/approve")
    public ApiResponse<Void> approveWorkOrder(@PathVariable Long id) {
        productionService.approveWorkOrder(id);
        return ApiResponse.success("approved", null);
    }

    @PutMapping("/work-orders/{id}/close")
    public ApiResponse<Void> closeWorkOrder(@PathVariable Long id) {
        productionService.closeWorkOrder(id);
        return ApiResponse.success("closed", null);
    }

    @PostMapping("/work-orders/{id}/pick")
    public ApiResponse<Long> pickMaterials(@PathVariable Long id, @Valid @RequestBody WorkOrderPickRequest request) {
        return ApiResponse.success("created", productionService.pickMaterials(id, request));
    }

    @PostMapping("/work-orders/{id}/finish-in")
    public ApiResponse<Long> finishIn(@PathVariable Long id, @Valid @RequestBody WorkOrderFinishInRequest request) {
        return ApiResponse.success("created", productionService.finishIn(id, request));
    }

    @GetMapping("/work-orders/{id}/progress")
    public ApiResponse<WorkOrderProgress> progress(@PathVariable Long id) {
        return ApiResponse.success(productionService.getProgress(id));
    }

    @GetMapping("/reports")
    public ApiResponse<PageResponse<ReportListItem>> reports(
            @RequestParam(required = false) Long workOrderId,
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ApiResponse.success(productionService.pageReports(workOrderId, pageNo, pageSize));
    }

    @PostMapping("/reports")
    public ApiResponse<Long> createReport(@Valid @RequestBody ReportSaveRequest request) {
        return ApiResponse.success("created", productionService.createReport(request));
    }
}
