package com.dongjian.erp.manufacturingerpsystem.modules.inventory.controller;

import com.dongjian.erp.manufacturingerpsystem.common.page.PageResponse;
import com.dongjian.erp.manufacturingerpsystem.common.result.ApiResponse;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.dto.InventoryCheckSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.dto.InventoryTransferSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.service.InventoryService;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.vo.InventoryDocDetail;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.vo.InventoryDocListItem;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.vo.InventoryStockListItem;
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
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/stocks")
    public ApiResponse<PageResponse<InventoryStockListItem>> stocks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String lotNo,
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ApiResponse.success(inventoryService.pageStocks(keyword, materialId, warehouseId, lotNo, pageNo, pageSize));
    }

    @GetMapping("/docs")
    public ApiResponse<PageResponse<InventoryDocListItem>> docs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String docType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ApiResponse.success(inventoryService.pageDocs(keyword, docType, status, warehouseId, pageNo, pageSize));
    }

    @GetMapping("/docs/{id}")
    public ApiResponse<InventoryDocDetail> doc(@PathVariable Long id) {
        return ApiResponse.success(inventoryService.getDoc(id));
    }

    @GetMapping("/transfers")
    public ApiResponse<PageResponse<InventoryDocListItem>> transfers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ApiResponse.success(inventoryService.pageTransfers(keyword, status, warehouseId, pageNo, pageSize));
    }

    @GetMapping("/transfers/{id}")
    public ApiResponse<InventoryDocDetail> transfer(@PathVariable Long id) {
        return ApiResponse.success(inventoryService.getTransfer(id));
    }

    @PostMapping("/transfers")
    public ApiResponse<Long> createTransfer(@Valid @RequestBody InventoryTransferSaveRequest request) {
        return ApiResponse.success("created", inventoryService.createTransfer(request));
    }

    @PutMapping("/transfers/{id}/approve")
    public ApiResponse<Void> approveTransfer(@PathVariable Long id) {
        inventoryService.approveTransfer(id);
        return ApiResponse.success("approved", null);
    }

    @GetMapping("/checks")
    public ApiResponse<PageResponse<InventoryDocListItem>> checks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ApiResponse.success(inventoryService.pageChecks(keyword, status, warehouseId, pageNo, pageSize));
    }

    @GetMapping("/checks/{id}")
    public ApiResponse<InventoryDocDetail> check(@PathVariable Long id) {
        return ApiResponse.success(inventoryService.getCheck(id));
    }

    @PostMapping("/checks")
    public ApiResponse<Long> createCheck(@Valid @RequestBody InventoryCheckSaveRequest request) {
        return ApiResponse.success("created", inventoryService.createCheck(request));
    }

    @PutMapping("/checks/{id}/approve")
    public ApiResponse<Void> approveCheck(@PathVariable Long id) {
        inventoryService.approveCheck(id);
        return ApiResponse.success("approved", null);
    }
}
