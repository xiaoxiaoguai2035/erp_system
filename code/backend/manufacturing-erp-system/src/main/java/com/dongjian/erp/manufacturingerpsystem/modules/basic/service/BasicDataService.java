package com.dongjian.erp.manufacturingerpsystem.modules.basic.service;

import com.dongjian.erp.manufacturingerpsystem.modules.basic.dto.CustomerSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.dto.BomSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.dto.MaterialSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.dto.RouteSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.dto.SupplierSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.dto.WarehouseSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.common.page.PageResponse;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.vo.BomDetail;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.vo.BomListItem;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.vo.CustomerListItem;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.vo.MaterialListItem;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.vo.RouteDetail;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.vo.RouteListItem;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.vo.SupplierListItem;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.vo.WarehouseListItem;

public interface BasicDataService {

    PageResponse<MaterialListItem> pageMaterials(String keyword, String status, long pageNo, long pageSize);

    PageResponse<CustomerListItem> pageCustomers(String keyword, String status, long pageNo, long pageSize);

    PageResponse<SupplierListItem> pageSuppliers(String keyword, String status, long pageNo, long pageSize);

    PageResponse<WarehouseListItem> pageWarehouses(String keyword, String status, long pageNo, long pageSize);

    PageResponse<BomListItem> pageBoms(String keyword, String status, long pageNo, long pageSize);

    PageResponse<RouteListItem> pageRoutes(String keyword, String status, long pageNo, long pageSize);

    MaterialListItem getMaterial(Long id);

    CustomerListItem getCustomer(Long id);

    SupplierListItem getSupplier(Long id);

    WarehouseListItem getWarehouse(Long id);

    BomDetail getBom(Long id);

    RouteDetail getRoute(Long id);

    Long createMaterial(MaterialSaveRequest request);

    Long createCustomer(CustomerSaveRequest request);

    Long createSupplier(SupplierSaveRequest request);

    Long createWarehouse(WarehouseSaveRequest request);

    Long createBom(BomSaveRequest request);

    Long createRoute(RouteSaveRequest request);

    void updateMaterial(Long id, MaterialSaveRequest request);

    void updateCustomer(Long id, CustomerSaveRequest request);

    void updateSupplier(Long id, SupplierSaveRequest request);

    void updateWarehouse(Long id, WarehouseSaveRequest request);

    void updateBom(Long id, BomSaveRequest request);

    void updateRoute(Long id, RouteSaveRequest request);

    void deleteMaterial(Long id);

    void deleteCustomer(Long id);

    void deleteSupplier(Long id);

    void deleteWarehouse(Long id);

    void deleteBom(Long id);

    void deleteRoute(Long id);
}
