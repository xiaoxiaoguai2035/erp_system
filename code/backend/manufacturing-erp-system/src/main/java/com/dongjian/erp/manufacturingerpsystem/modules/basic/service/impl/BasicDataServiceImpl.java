package com.dongjian.erp.manufacturingerpsystem.modules.basic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dongjian.erp.manufacturingerpsystem.common.exception.BusinessException;
import com.dongjian.erp.manufacturingerpsystem.common.page.PageResponse;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.dto.BomSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.dto.CustomerSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.dto.MaterialSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.dto.RouteSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.dto.SupplierSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.dto.WarehouseSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.entity.BasBom;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.entity.BasBomItem;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.entity.BasCustomer;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.entity.BasMaterial;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.entity.BasProcessRoute;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.entity.BasProcessRouteItem;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.entity.BasSupplier;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.entity.BasWarehouse;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.mapper.BasBomItemMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.mapper.BasBomMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.mapper.BasCustomerMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.mapper.BasMaterialMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.mapper.BasProcessRouteItemMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.mapper.BasProcessRouteMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.mapper.BasSupplierMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.mapper.BasWarehouseMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.service.BasicDataService;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.vo.BomDetail;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.vo.BomListItem;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.vo.CustomerListItem;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.vo.MaterialListItem;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.vo.RouteDetail;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.vo.RouteListItem;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.vo.SupplierListItem;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.vo.WarehouseListItem;
import com.dongjian.erp.manufacturingerpsystem.modules.production.entity.PrdReport;
import com.dongjian.erp.manufacturingerpsystem.modules.production.mapper.PrdReportMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BasicDataServiceImpl implements BasicDataService {

    private final BasMaterialMapper basMaterialMapper;
    private final BasCustomerMapper basCustomerMapper;
    private final BasSupplierMapper basSupplierMapper;
    private final BasWarehouseMapper basWarehouseMapper;
    private final BasBomMapper basBomMapper;
    private final BasBomItemMapper basBomItemMapper;
    private final BasProcessRouteMapper basProcessRouteMapper;
    private final BasProcessRouteItemMapper basProcessRouteItemMapper;
    private final PrdReportMapper prdReportMapper;

    public BasicDataServiceImpl(BasMaterialMapper basMaterialMapper,
                                BasCustomerMapper basCustomerMapper,
                                BasSupplierMapper basSupplierMapper,
                                BasWarehouseMapper basWarehouseMapper,
                                BasBomMapper basBomMapper,
                                BasBomItemMapper basBomItemMapper,
                                BasProcessRouteMapper basProcessRouteMapper,
                                BasProcessRouteItemMapper basProcessRouteItemMapper,
                                PrdReportMapper prdReportMapper) {
        this.basMaterialMapper = basMaterialMapper;
        this.basCustomerMapper = basCustomerMapper;
        this.basSupplierMapper = basSupplierMapper;
        this.basWarehouseMapper = basWarehouseMapper;
        this.basBomMapper = basBomMapper;
        this.basBomItemMapper = basBomItemMapper;
        this.basProcessRouteMapper = basProcessRouteMapper;
        this.basProcessRouteItemMapper = basProcessRouteItemMapper;
        this.prdReportMapper = prdReportMapper;
    }

    @Override
    public PageResponse<MaterialListItem> pageMaterials(String keyword, String status, long pageNo, long pageSize) {
        Page<BasMaterial> page = basMaterialMapper.selectPage(
                new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<BasMaterial>()
                        .and(StringUtils.hasText(keyword), wrapper -> wrapper
                                .like(BasMaterial::getCode, keyword)
                                .or()
                                .like(BasMaterial::getName, keyword))
                        .eq(StringUtils.hasText(status), BasMaterial::getStatus, status)
                        .orderByDesc(BasMaterial::getId)
        );

        Map<Long, String> warehouseNameMap = buildWarehouseNameMap(page.getRecords().stream()
                .map(BasMaterial::getDefaultWarehouseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        List<MaterialListItem> records = page.getRecords().stream()
                .map(item -> {
                    MaterialListItem result = new MaterialListItem();
                    result.setId(item.getId());
                    result.setCode(item.getCode());
                    result.setName(item.getName());
                    result.setSpec(item.getSpec());
                    result.setMaterialType(item.getMaterialType());
                    result.setUnitCode(item.getUnitCode());
                    result.setSafetyStock(item.getSafetyStock());
                    result.setBatchEnabled(item.getBatchEnabled());
                    result.setDefaultWarehouseId(item.getDefaultWarehouseId());
                    result.setDefaultWarehouseName(warehouseNameMap.get(item.getDefaultWarehouseId()));
                    result.setStatus(item.getStatus());
                    return result;
                })
                .toList();

        return PageResponse.of(page.getTotal(), pageNo, pageSize, records);
    }

    @Override
    public PageResponse<CustomerListItem> pageCustomers(String keyword, String status, long pageNo, long pageSize) {
        Page<BasCustomer> page = basCustomerMapper.selectPage(
                new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<BasCustomer>()
                        .and(StringUtils.hasText(keyword), wrapper -> wrapper
                                .like(BasCustomer::getCode, keyword)
                                .or()
                                .like(BasCustomer::getName, keyword))
                        .eq(StringUtils.hasText(status), BasCustomer::getStatus, status)
                        .orderByDesc(BasCustomer::getId)
        );

        return PageResponse.of(
                page.getTotal(),
                pageNo,
                pageSize,
                page.getRecords().stream()
                        .map(item -> {
                            CustomerListItem result = new CustomerListItem();
                            result.setId(item.getId());
                            result.setCode(item.getCode());
                            result.setName(item.getName());
                            result.setContact(item.getContact());
                            result.setPhone(item.getPhone());
                            result.setAddress(item.getAddress());
                            result.setStatus(item.getStatus());
                            return result;
                        })
                        .toList()
        );
    }

    @Override
    public PageResponse<SupplierListItem> pageSuppliers(String keyword, String status, long pageNo, long pageSize) {
        Page<BasSupplier> page = basSupplierMapper.selectPage(
                new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<BasSupplier>()
                        .and(StringUtils.hasText(keyword), wrapper -> wrapper
                                .like(BasSupplier::getCode, keyword)
                                .or()
                                .like(BasSupplier::getName, keyword))
                        .eq(StringUtils.hasText(status), BasSupplier::getStatus, status)
                        .orderByDesc(BasSupplier::getId)
        );

        return PageResponse.of(
                page.getTotal(),
                pageNo,
                pageSize,
                page.getRecords().stream()
                        .map(item -> {
                            SupplierListItem result = new SupplierListItem();
                            result.setId(item.getId());
                            result.setCode(item.getCode());
                            result.setName(item.getName());
                            result.setContact(item.getContact());
                            result.setPhone(item.getPhone());
                            result.setAddress(item.getAddress());
                            result.setStatus(item.getStatus());
                            return result;
                        })
                        .toList()
        );
    }

    @Override
    public PageResponse<WarehouseListItem> pageWarehouses(String keyword, String status, long pageNo, long pageSize) {
        Page<BasWarehouse> page = basWarehouseMapper.selectPage(
                new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<BasWarehouse>()
                        .and(StringUtils.hasText(keyword), wrapper -> wrapper
                                .like(BasWarehouse::getCode, keyword)
                                .or()
                                .like(BasWarehouse::getName, keyword))
                        .eq(StringUtils.hasText(status), BasWarehouse::getStatus, status)
                        .orderByDesc(BasWarehouse::getId)
        );

        return PageResponse.of(
                page.getTotal(),
                pageNo,
                pageSize,
                page.getRecords().stream()
                        .map(item -> {
                            WarehouseListItem result = new WarehouseListItem();
                            result.setId(item.getId());
                            result.setCode(item.getCode());
                            result.setName(item.getName());
                            result.setWarehouseType(item.getWarehouseType());
                            result.setManagerName(item.getManagerName());
                            result.setStatus(item.getStatus());
                            return result;
                        })
                        .toList()
        );
    }

    @Override
    public PageResponse<BomListItem> pageBoms(String keyword, String status, long pageNo, long pageSize) {
        Set<Long> matchedProductIds = loadMatchedMaterialIds(keyword);
        if (matchedProductIds != null && matchedProductIds.isEmpty()) {
            return PageResponse.empty(pageNo, pageSize);
        }
        Page<BasBom> page = basBomMapper.selectPage(
                new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<BasBom>()
                        .in(matchedProductIds != null && !matchedProductIds.isEmpty(), BasBom::getProductId, matchedProductIds)
                        .eq(StringUtils.hasText(status), BasBom::getStatus, status)
                        .orderByDesc(BasBom::getId)
        );

        Map<Long, BasMaterial> productMap = loadMaterialMap(page.getRecords().stream()
                .map(BasBom::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        List<BomListItem> records = page.getRecords().stream()
                .map(item -> toBomListItem(item, productMap.get(item.getProductId())))
                .toList();

        return PageResponse.of(page.getTotal(), pageNo, pageSize, records);
    }

    @Override
    public PageResponse<RouteListItem> pageRoutes(String keyword, String status, long pageNo, long pageSize) {
        Set<Long> matchedProductIds = loadMatchedMaterialIds(keyword);
        if (matchedProductIds != null && matchedProductIds.isEmpty()) {
            return PageResponse.empty(pageNo, pageSize);
        }
        Page<BasProcessRoute> page = basProcessRouteMapper.selectPage(
                new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<BasProcessRoute>()
                        .in(matchedProductIds != null && !matchedProductIds.isEmpty(), BasProcessRoute::getProductId, matchedProductIds)
                        .eq(StringUtils.hasText(status), BasProcessRoute::getStatus, status)
                        .orderByDesc(BasProcessRoute::getId)
        );

        Map<Long, BasMaterial> productMap = loadMaterialMap(page.getRecords().stream()
                .map(BasProcessRoute::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        List<RouteListItem> records = page.getRecords().stream()
                .map(item -> toRouteListItem(item, productMap.get(item.getProductId())))
                .toList();

        return PageResponse.of(page.getTotal(), pageNo, pageSize, records);
    }

    @Override
    public MaterialListItem getMaterial(Long id) {
        return toMaterialItem(requireMaterial(id));
    }

    @Override
    public CustomerListItem getCustomer(Long id) {
        return toCustomerItem(requireCustomer(id));
    }

    @Override
    public SupplierListItem getSupplier(Long id) {
        return toSupplierItem(requireSupplier(id));
    }

    @Override
    public WarehouseListItem getWarehouse(Long id) {
        return toWarehouseItem(requireWarehouse(id));
    }

    @Override
    public BomDetail getBom(Long id) {
        BasBom bom = requireBom(id);
        List<BasBomItem> bomItems = loadBomItems(id);
        Map<Long, BasMaterial> materialMap = loadMaterialMap(bomItems.stream()
                .map(BasBomItem::getMaterialId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        BasMaterial product = basMaterialMapper.selectById(bom.getProductId());

        BomDetail detail = new BomDetail();
        detail.setId(bom.getId());
        detail.setProductId(bom.getProductId());
        detail.setProductCode(product != null ? product.getCode() : null);
        detail.setProductName(product != null ? product.getName() : null);
        detail.setVersionNo(bom.getVersionNo());
        detail.setStatus(bom.getStatus());
        detail.setEffectiveDate(bom.getEffectiveDate());
        detail.setItems(bomItems.stream().map(item -> {
            BasMaterial material = materialMap.get(item.getMaterialId());
            BomDetail.BomDetailItem result = new BomDetail.BomDetailItem();
            result.setId(item.getId());
            result.setMaterialId(item.getMaterialId());
            result.setMaterialCode(material != null ? material.getCode() : null);
            result.setMaterialName(material != null ? material.getName() : null);
            result.setQty(item.getQty());
            result.setLossRate(item.getLossRate());
            result.setSortNo(item.getSortNo());
            return result;
        }).toList());
        return detail;
    }

    @Override
    public RouteDetail getRoute(Long id) {
        BasProcessRoute route = requireRoute(id);
        BasMaterial product = basMaterialMapper.selectById(route.getProductId());
        List<BasProcessRouteItem> items = loadRouteItems(id);

        RouteDetail detail = new RouteDetail();
        detail.setId(route.getId());
        detail.setProductId(route.getProductId());
        detail.setProductCode(product != null ? product.getCode() : null);
        detail.setProductName(product != null ? product.getName() : null);
        detail.setVersionNo(route.getVersionNo());
        detail.setStatus(route.getStatus());
        detail.setItems(items.stream().map(item -> {
            RouteDetail.RouteDetailItem result = new RouteDetail.RouteDetailItem();
            result.setId(item.getId());
            result.setProcessCode(item.getProcessCode());
            result.setProcessName(item.getProcessName());
            result.setStandardHours(item.getStandardHours());
            result.setWorkCenter(item.getWorkCenter());
            result.setSortNo(item.getSortNo());
            return result;
        }).toList());
        return detail;
    }

    @Override
    public Long createMaterial(MaterialSaveRequest request) {
        validateMaterialCode(null, request.getCode());
        BasMaterial entity = new BasMaterial();
        fillMaterial(entity, request);
        basMaterialMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public Long createCustomer(CustomerSaveRequest request) {
        validateCustomerCode(null, request.getCode());
        BasCustomer entity = new BasCustomer();
        fillCustomer(entity, request);
        basCustomerMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public Long createSupplier(SupplierSaveRequest request) {
        validateSupplierCode(null, request.getCode());
        BasSupplier entity = new BasSupplier();
        fillSupplier(entity, request);
        basSupplierMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public Long createWarehouse(WarehouseSaveRequest request) {
        validateWarehouseCode(null, request.getCode());
        BasWarehouse entity = new BasWarehouse();
        fillWarehouse(entity, request);
        basWarehouseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBom(BomSaveRequest request) {
        if (basMaterialMapper.selectById(request.getProductId()) == null) {
            throw new BusinessException(400, "BOM 产品物料不存在");
        }
        BasBom entity = new BasBom();
        fillBom(entity, request);
        basBomMapper.insert(entity);
        saveBomItems(entity.getId(), request.getItems());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRoute(RouteSaveRequest request) {
        if (basMaterialMapper.selectById(request.getProductId()) == null) {
            throw new BusinessException(400, "工艺路线产品物料不存在");
        }
        BasProcessRoute entity = new BasProcessRoute();
        fillRoute(entity, request);
        basProcessRouteMapper.insert(entity);
        saveRouteItems(entity.getId(), request.getItems());
        return entity.getId();
    }

    @Override
    public void updateMaterial(Long id, MaterialSaveRequest request) {
        BasMaterial entity = requireMaterial(id);
        validateMaterialCode(id, request.getCode());
        fillMaterial(entity, request);
        basMaterialMapper.updateById(entity);
    }

    @Override
    public void updateCustomer(Long id, CustomerSaveRequest request) {
        BasCustomer entity = requireCustomer(id);
        validateCustomerCode(id, request.getCode());
        fillCustomer(entity, request);
        basCustomerMapper.updateById(entity);
    }

    @Override
    public void updateSupplier(Long id, SupplierSaveRequest request) {
        BasSupplier entity = requireSupplier(id);
        validateSupplierCode(id, request.getCode());
        fillSupplier(entity, request);
        basSupplierMapper.updateById(entity);
    }

    @Override
    public void updateWarehouse(Long id, WarehouseSaveRequest request) {
        BasWarehouse entity = requireWarehouse(id);
        validateWarehouseCode(id, request.getCode());
        fillWarehouse(entity, request);
        basWarehouseMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBom(Long id, BomSaveRequest request) {
        BasBom entity = requireBom(id);
        if (basMaterialMapper.selectById(request.getProductId()) == null) {
            throw new BusinessException(400, "BOM 产品物料不存在");
        }
        fillBom(entity, request);
        basBomMapper.updateById(entity);
        basBomItemMapper.delete(new LambdaQueryWrapper<BasBomItem>().eq(BasBomItem::getBomId, id));
        saveBomItems(id, request.getItems());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRoute(Long id, RouteSaveRequest request) {
        BasProcessRoute entity = requireRoute(id);
        if (basMaterialMapper.selectById(request.getProductId()) == null) {
            throw new BusinessException(400, "工艺路线产品物料不存在");
        }
        fillRoute(entity, request);
        basProcessRouteMapper.updateById(entity);
        syncRouteItems(id, request.getItems());
    }

    @Override
    public void deleteMaterial(Long id) {
        requireMaterial(id);
        basMaterialMapper.deleteById(id);
    }

    @Override
    public void deleteCustomer(Long id) {
        requireCustomer(id);
        basCustomerMapper.deleteById(id);
    }

    @Override
    public void deleteSupplier(Long id) {
        requireSupplier(id);
        basSupplierMapper.deleteById(id);
    }

    @Override
    public void deleteWarehouse(Long id) {
        requireWarehouse(id);
        basWarehouseMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBom(Long id) {
        requireBom(id);
        basBomItemMapper.delete(new LambdaQueryWrapper<BasBomItem>().eq(BasBomItem::getBomId, id));
        basBomMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRoute(Long id) {
        requireRoute(id);
        List<BasProcessRouteItem> routeItems = loadRouteItems(id);
        validateRouteItemsNotReferenced(routeItems.stream().map(BasProcessRouteItem::getId).toList());
        basProcessRouteItemMapper.delete(new LambdaQueryWrapper<BasProcessRouteItem>().eq(BasProcessRouteItem::getRouteId, id));
        basProcessRouteMapper.deleteById(id);
    }

    private Map<Long, String> buildWarehouseNameMap(Set<Long> warehouseIds) {
        if (warehouseIds.isEmpty()) {
            return Map.of();
        }

        return basWarehouseMapper.selectBatchIds(warehouseIds).stream()
                .collect(Collectors.toMap(BasWarehouse::getId, BasWarehouse::getName, (left, right) -> left));
    }

    private BasMaterial requireMaterial(Long id) {
        BasMaterial entity = basMaterialMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "物料不存在");
        }
        return entity;
    }

    private BasCustomer requireCustomer(Long id) {
        BasCustomer entity = basCustomerMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "客户不存在");
        }
        return entity;
    }

    private BasSupplier requireSupplier(Long id) {
        BasSupplier entity = basSupplierMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "供应商不存在");
        }
        return entity;
    }

    private BasWarehouse requireWarehouse(Long id) {
        BasWarehouse entity = basWarehouseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "仓库不存在");
        }
        return entity;
    }

    private BasBom requireBom(Long id) {
        BasBom entity = basBomMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "BOM 不存在");
        }
        return entity;
    }

    private BasProcessRoute requireRoute(Long id) {
        BasProcessRoute entity = basProcessRouteMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "工艺路线不存在");
        }
        return entity;
    }

    private void validateMaterialCode(Long id, String code) {
        BasMaterial existing = basMaterialMapper.selectOne(new LambdaQueryWrapper<BasMaterial>()
                .eq(BasMaterial::getCode, code)
                .last("limit 1"));
        if (existing != null && !existing.getId().equals(id)) {
            throw new BusinessException(409, "物料编码已存在");
        }
    }

    private void validateCustomerCode(Long id, String code) {
        BasCustomer existing = basCustomerMapper.selectOne(new LambdaQueryWrapper<BasCustomer>()
                .eq(BasCustomer::getCode, code)
                .last("limit 1"));
        if (existing != null && !existing.getId().equals(id)) {
            throw new BusinessException(409, "客户编码已存在");
        }
    }

    private void validateSupplierCode(Long id, String code) {
        BasSupplier existing = basSupplierMapper.selectOne(new LambdaQueryWrapper<BasSupplier>()
                .eq(BasSupplier::getCode, code)
                .last("limit 1"));
        if (existing != null && !existing.getId().equals(id)) {
            throw new BusinessException(409, "供应商编码已存在");
        }
    }

    private void validateWarehouseCode(Long id, String code) {
        BasWarehouse existing = basWarehouseMapper.selectOne(new LambdaQueryWrapper<BasWarehouse>()
                .eq(BasWarehouse::getCode, code)
                .last("limit 1"));
        if (existing != null && !existing.getId().equals(id)) {
            throw new BusinessException(409, "仓库编码已存在");
        }
    }

    private void fillMaterial(BasMaterial entity, MaterialSaveRequest request) {
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setSpec(request.getSpec());
        entity.setMaterialType(request.getMaterialType());
        entity.setUnitCode(request.getUnitCode());
        entity.setSafetyStock(request.getSafetyStock());
        entity.setBatchEnabled(request.getBatchEnabled());
        entity.setDefaultWarehouseId(request.getDefaultWarehouseId());
        entity.setStatus(request.getStatus());
    }

    private void fillCustomer(BasCustomer entity, CustomerSaveRequest request) {
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setContact(request.getContact());
        entity.setPhone(request.getPhone());
        entity.setAddress(request.getAddress());
        entity.setStatus(request.getStatus());
    }

    private void fillSupplier(BasSupplier entity, SupplierSaveRequest request) {
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setContact(request.getContact());
        entity.setPhone(request.getPhone());
        entity.setAddress(request.getAddress());
        entity.setStatus(request.getStatus());
    }

    private void fillWarehouse(BasWarehouse entity, WarehouseSaveRequest request) {
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setWarehouseType(request.getWarehouseType());
        entity.setManagerName(request.getManagerName());
        entity.setStatus(request.getStatus());
    }

    private void fillBom(BasBom entity, BomSaveRequest request) {
        entity.setProductId(request.getProductId());
        entity.setVersionNo(request.getVersionNo());
        entity.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "enabled");
        entity.setEffectiveDate(request.getEffectiveDate());
    }

    private void fillRoute(BasProcessRoute entity, RouteSaveRequest request) {
        entity.setProductId(request.getProductId());
        entity.setVersionNo(request.getVersionNo());
        entity.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "enabled");
    }

    private MaterialListItem toMaterialItem(BasMaterial item) {
        MaterialListItem result = new MaterialListItem();
        result.setId(item.getId());
        result.setCode(item.getCode());
        result.setName(item.getName());
        result.setSpec(item.getSpec());
        result.setMaterialType(item.getMaterialType());
        result.setUnitCode(item.getUnitCode());
        result.setSafetyStock(item.getSafetyStock());
        result.setBatchEnabled(item.getBatchEnabled());
        result.setDefaultWarehouseId(item.getDefaultWarehouseId());
        result.setDefaultWarehouseName(resolveWarehouseName(item.getDefaultWarehouseId()));
        result.setStatus(item.getStatus());
        return result;
    }

    private CustomerListItem toCustomerItem(BasCustomer item) {
        CustomerListItem result = new CustomerListItem();
        result.setId(item.getId());
        result.setCode(item.getCode());
        result.setName(item.getName());
        result.setContact(item.getContact());
        result.setPhone(item.getPhone());
        result.setAddress(item.getAddress());
        result.setStatus(item.getStatus());
        return result;
    }

    private SupplierListItem toSupplierItem(BasSupplier item) {
        SupplierListItem result = new SupplierListItem();
        result.setId(item.getId());
        result.setCode(item.getCode());
        result.setName(item.getName());
        result.setContact(item.getContact());
        result.setPhone(item.getPhone());
        result.setAddress(item.getAddress());
        result.setStatus(item.getStatus());
        return result;
    }

    private WarehouseListItem toWarehouseItem(BasWarehouse item) {
        WarehouseListItem result = new WarehouseListItem();
        result.setId(item.getId());
        result.setCode(item.getCode());
        result.setName(item.getName());
        result.setWarehouseType(item.getWarehouseType());
        result.setManagerName(item.getManagerName());
        result.setStatus(item.getStatus());
        return result;
    }

    private String resolveWarehouseName(Long warehouseId) {
        if (warehouseId == null) {
            return null;
        }
        BasWarehouse warehouse = basWarehouseMapper.selectById(warehouseId);
        return warehouse != null ? warehouse.getName() : null;
    }

    private Map<Long, BasMaterial> loadMaterialMap(Set<Long> materialIds) {
        if (materialIds.isEmpty()) {
            return Map.of();
        }
        return basMaterialMapper.selectBatchIds(materialIds).stream()
                .collect(Collectors.toMap(BasMaterial::getId, item -> item, (left, right) -> left));
    }

    private Set<Long> loadMatchedMaterialIds(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return basMaterialMapper.selectList(new LambdaQueryWrapper<BasMaterial>()
                        .like(BasMaterial::getCode, keyword)
                        .or()
                        .like(BasMaterial::getName, keyword))
                .stream()
                .map(BasMaterial::getId)
                .collect(Collectors.toSet());
    }

    private BomListItem toBomListItem(BasBom item, BasMaterial product) {
        BomListItem result = new BomListItem();
        result.setId(item.getId());
        result.setProductId(item.getProductId());
        result.setProductCode(product != null ? product.getCode() : null);
        result.setProductName(product != null ? product.getName() : null);
        result.setVersionNo(item.getVersionNo());
        result.setStatus(item.getStatus());
        result.setEffectiveDate(item.getEffectiveDate());
        return result;
    }

    private RouteListItem toRouteListItem(BasProcessRoute item, BasMaterial product) {
        RouteListItem result = new RouteListItem();
        result.setId(item.getId());
        result.setProductId(item.getProductId());
        result.setProductCode(product != null ? product.getCode() : null);
        result.setProductName(product != null ? product.getName() : null);
        result.setVersionNo(item.getVersionNo());
        result.setStatus(item.getStatus());
        return result;
    }

    private List<BasBomItem> loadBomItems(Long bomId) {
        return basBomItemMapper.selectList(new LambdaQueryWrapper<BasBomItem>()
                .eq(BasBomItem::getBomId, bomId)
                .orderByAsc(BasBomItem::getSortNo)
                .orderByAsc(BasBomItem::getId));
    }

    private List<BasProcessRouteItem> loadRouteItems(Long routeId) {
        return basProcessRouteItemMapper.selectList(new LambdaQueryWrapper<BasProcessRouteItem>()
                .eq(BasProcessRouteItem::getRouteId, routeId)
                .orderByAsc(BasProcessRouteItem::getSortNo)
                .orderByAsc(BasProcessRouteItem::getId));
    }

    private void saveBomItems(Long bomId, List<BomSaveRequest.BomItemRequest> requests) {
        for (BomSaveRequest.BomItemRequest request : requests) {
            if (basMaterialMapper.selectById(request.getMaterialId()) == null) {
                throw new BusinessException(400, "BOM 明细中的物料不存在");
            }
            BasBomItem item = new BasBomItem();
            item.setBomId(bomId);
            item.setMaterialId(request.getMaterialId());
            item.setQty(request.getQty());
            item.setLossRate(request.getLossRate() != null ? request.getLossRate() : BigDecimal.ZERO);
            item.setSortNo(request.getSortNo() != null ? request.getSortNo() : 0);
            basBomItemMapper.insert(item);
        }
    }

    private void saveRouteItems(Long routeId, List<RouteSaveRequest.RouteItemRequest> requests) {
        for (RouteSaveRequest.RouteItemRequest request : requests) {
            BasProcessRouteItem item = new BasProcessRouteItem();
            item.setRouteId(routeId);
            item.setProcessCode(request.getProcessCode());
            item.setProcessName(request.getProcessName());
            item.setStandardHours(request.getStandardHours());
            item.setWorkCenter(request.getWorkCenter());
            item.setSortNo(request.getSortNo() != null ? request.getSortNo() : 0);
            basProcessRouteItemMapper.insert(item);
        }
    }

    private void syncRouteItems(Long routeId, List<RouteSaveRequest.RouteItemRequest> requests) {
        List<BasProcessRouteItem> existingItems = loadRouteItems(routeId);
        Map<Long, BasProcessRouteItem> existingItemMap = existingItems.stream()
                .collect(Collectors.toMap(BasProcessRouteItem::getId, item -> item, (left, right) -> left, HashMap::new));

        Set<Long> requestItemIds = requests.stream()
                .map(RouteSaveRequest.RouteItemRequest::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (RouteSaveRequest.RouteItemRequest request : requests) {
            if (request.getId() != null && !existingItemMap.containsKey(request.getId())) {
                throw new BusinessException(400, "工艺工序明细不存在或不属于当前工艺路线");
            }
        }

        List<BasProcessRouteItem> deleteCandidates = existingItems.stream()
                .filter(item -> !requestItemIds.contains(item.getId()))
                .toList();
        validateRouteItemsNotReferenced(deleteCandidates.stream().map(BasProcessRouteItem::getId).toList());
        if (!deleteCandidates.isEmpty()) {
            basProcessRouteItemMapper.deleteBatchIds(deleteCandidates.stream().map(BasProcessRouteItem::getId).toList());
        }

        for (RouteSaveRequest.RouteItemRequest request : requests) {
            BasProcessRouteItem item = request.getId() != null ? existingItemMap.get(request.getId()) : new BasProcessRouteItem();
            item.setRouteId(routeId);
            item.setProcessCode(request.getProcessCode());
            item.setProcessName(request.getProcessName());
            item.setStandardHours(request.getStandardHours());
            item.setWorkCenter(request.getWorkCenter());
            item.setSortNo(request.getSortNo() != null ? request.getSortNo() : 0);

            if (request.getId() != null) {
                basProcessRouteItemMapper.updateById(item);
            } else {
                basProcessRouteItemMapper.insert(item);
            }
        }
    }

    private void validateRouteItemsNotReferenced(List<Long> routeItemIds) {
        if (routeItemIds == null || routeItemIds.isEmpty()) {
            return;
        }

        Long referencedCount = prdReportMapper.selectCount(new LambdaQueryWrapper<PrdReport>()
                .in(PrdReport::getProcessItemId, routeItemIds));
        if (referencedCount != null && referencedCount > 0) {
            throw new BusinessException(409, "工艺工序已被报工记录引用，不能删除；请保留原工序或新增工序");
        }
    }
}
