package com.dongjian.erp.manufacturingerpsystem.modules.basic.controller;

import com.dongjian.erp.manufacturingerpsystem.common.page.PageResponse;
import com.dongjian.erp.manufacturingerpsystem.common.result.ApiResponse;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.dto.BomSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.dto.CustomerSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.dto.MaterialSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.dto.RouteSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.dto.SupplierSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.dto.WarehouseSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.service.BasicDataService;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.vo.BomDetail;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.vo.BomListItem;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.vo.CustomerListItem;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.vo.MaterialListItem;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.vo.RouteDetail;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.vo.RouteListItem;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.vo.SupplierListItem;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.vo.WarehouseListItem;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/basic")
public class BasicDataController {

    private final BasicDataService basicDataService;

    public BasicDataController(BasicDataService basicDataService) {
        this.basicDataService = basicDataService;
    }

    @GetMapping("/materials")
    public ApiResponse<PageResponse<MaterialListItem>> materials(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ApiResponse.success(basicDataService.pageMaterials(keyword, status, pageNo, pageSize));
    }

    @GetMapping("/customers")
    public ApiResponse<PageResponse<CustomerListItem>> customers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ApiResponse.success(basicDataService.pageCustomers(keyword, status, pageNo, pageSize));
    }

    @GetMapping("/suppliers")
    public ApiResponse<PageResponse<SupplierListItem>> suppliers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ApiResponse.success(basicDataService.pageSuppliers(keyword, status, pageNo, pageSize));
    }

    @GetMapping("/warehouses")
    public ApiResponse<PageResponse<WarehouseListItem>> warehouses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ApiResponse.success(basicDataService.pageWarehouses(keyword, status, pageNo, pageSize));
    }

    @GetMapping("/boms")
    public ApiResponse<PageResponse<BomListItem>> boms(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ApiResponse.success(basicDataService.pageBoms(keyword, status, pageNo, pageSize));
    }

    @GetMapping("/routes")
    public ApiResponse<PageResponse<RouteListItem>> routes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ApiResponse.success(basicDataService.pageRoutes(keyword, status, pageNo, pageSize));
    }

    @GetMapping("/materials/{id}")
    public ApiResponse<MaterialListItem> material(@PathVariable Long id) {
        return ApiResponse.success(basicDataService.getMaterial(id));
    }

    @PostMapping("/materials")
    public ApiResponse<Long> createMaterial(@Valid @RequestBody MaterialSaveRequest request) {
        return ApiResponse.success("created", basicDataService.createMaterial(request));
    }

    @PutMapping("/materials/{id}")
    public ApiResponse<Void> updateMaterial(@PathVariable Long id, @Valid @RequestBody MaterialSaveRequest request) {
        basicDataService.updateMaterial(id, request);
        return ApiResponse.success("updated", null);
    }

    @DeleteMapping("/materials/{id}")
    public ApiResponse<Void> deleteMaterial(@PathVariable Long id) {
        basicDataService.deleteMaterial(id);
        return ApiResponse.success("deleted", null);
    }

    @GetMapping("/customers/{id}")
    public ApiResponse<CustomerListItem> customer(@PathVariable Long id) {
        return ApiResponse.success(basicDataService.getCustomer(id));
    }

    @PostMapping("/customers")
    public ApiResponse<Long> createCustomer(@Valid @RequestBody CustomerSaveRequest request) {
        return ApiResponse.success("created", basicDataService.createCustomer(request));
    }

    @PutMapping("/customers/{id}")
    public ApiResponse<Void> updateCustomer(@PathVariable Long id, @Valid @RequestBody CustomerSaveRequest request) {
        basicDataService.updateCustomer(id, request);
        return ApiResponse.success("updated", null);
    }

    @DeleteMapping("/customers/{id}")
    public ApiResponse<Void> deleteCustomer(@PathVariable Long id) {
        basicDataService.deleteCustomer(id);
        return ApiResponse.success("deleted", null);
    }

    @GetMapping("/suppliers/{id}")
    public ApiResponse<SupplierListItem> supplier(@PathVariable Long id) {
        return ApiResponse.success(basicDataService.getSupplier(id));
    }

    @PostMapping("/suppliers")
    public ApiResponse<Long> createSupplier(@Valid @RequestBody SupplierSaveRequest request) {
        return ApiResponse.success("created", basicDataService.createSupplier(request));
    }

    @PutMapping("/suppliers/{id}")
    public ApiResponse<Void> updateSupplier(@PathVariable Long id, @Valid @RequestBody SupplierSaveRequest request) {
        basicDataService.updateSupplier(id, request);
        return ApiResponse.success("updated", null);
    }

    @DeleteMapping("/suppliers/{id}")
    public ApiResponse<Void> deleteSupplier(@PathVariable Long id) {
        basicDataService.deleteSupplier(id);
        return ApiResponse.success("deleted", null);
    }

    @GetMapping("/warehouses/{id}")
    public ApiResponse<WarehouseListItem> warehouse(@PathVariable Long id) {
        return ApiResponse.success(basicDataService.getWarehouse(id));
    }

    @GetMapping("/boms/{id}")
    public ApiResponse<BomDetail> bom(@PathVariable Long id) {
        return ApiResponse.success(basicDataService.getBom(id));
    }

    @GetMapping("/routes/{id}")
    public ApiResponse<RouteDetail> route(@PathVariable Long id) {
        return ApiResponse.success(basicDataService.getRoute(id));
    }

    @PostMapping("/warehouses")
    public ApiResponse<Long> createWarehouse(@Valid @RequestBody WarehouseSaveRequest request) {
        return ApiResponse.success("created", basicDataService.createWarehouse(request));
    }

    @PostMapping("/boms")
    public ApiResponse<Long> createBom(@Valid @RequestBody BomSaveRequest request) {
        return ApiResponse.success("created", basicDataService.createBom(request));
    }

    @PostMapping("/routes")
    public ApiResponse<Long> createRoute(@Valid @RequestBody RouteSaveRequest request) {
        return ApiResponse.success("created", basicDataService.createRoute(request));
    }

    @PutMapping("/warehouses/{id}")
    public ApiResponse<Void> updateWarehouse(@PathVariable Long id, @Valid @RequestBody WarehouseSaveRequest request) {
        basicDataService.updateWarehouse(id, request);
        return ApiResponse.success("updated", null);
    }

    @PutMapping("/boms/{id}")
    public ApiResponse<Void> updateBom(@PathVariable Long id, @Valid @RequestBody BomSaveRequest request) {
        basicDataService.updateBom(id, request);
        return ApiResponse.success("updated", null);
    }

    @PutMapping("/routes/{id}")
    public ApiResponse<Void> updateRoute(@PathVariable Long id, @Valid @RequestBody RouteSaveRequest request) {
        basicDataService.updateRoute(id, request);
        return ApiResponse.success("updated", null);
    }

    @DeleteMapping("/warehouses/{id}")
    public ApiResponse<Void> deleteWarehouse(@PathVariable Long id) {
        basicDataService.deleteWarehouse(id);
        return ApiResponse.success("deleted", null);
    }

    @DeleteMapping("/boms/{id}")
    public ApiResponse<Void> deleteBom(@PathVariable Long id) {
        basicDataService.deleteBom(id);
        return ApiResponse.success("deleted", null);
    }

    @DeleteMapping("/routes/{id}")
    public ApiResponse<Void> deleteRoute(@PathVariable Long id) {
        basicDataService.deleteRoute(id);
        return ApiResponse.success("deleted", null);
    }
}
