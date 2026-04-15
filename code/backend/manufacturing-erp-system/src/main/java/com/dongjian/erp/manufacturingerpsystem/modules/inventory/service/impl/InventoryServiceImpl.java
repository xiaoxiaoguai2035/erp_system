package com.dongjian.erp.manufacturingerpsystem.modules.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dongjian.erp.manufacturingerpsystem.common.exception.BusinessException;
import com.dongjian.erp.manufacturingerpsystem.common.page.PageResponse;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.entity.BasMaterial;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.entity.BasWarehouse;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.mapper.BasMaterialMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.mapper.BasWarehouseMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.dto.InventoryCheckSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.dto.InventoryTransferSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.entity.StkDoc;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.entity.StkDocItem;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.entity.StkInventory;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.mapper.StkDocItemMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.mapper.StkDocMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.mapper.StkInventoryMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.service.InventoryService;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.vo.InventoryDocDetail;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.vo.InventoryDocListItem;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.vo.InventoryStockListItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InventoryServiceImpl implements InventoryService {

    private static final String DOC_TYPE_TRANSFER = "transfer";
    private static final String DOC_TYPE_CHECK = "check";
    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_APPROVED = "approved";

    private final StkInventoryMapper stkInventoryMapper;
    private final StkDocMapper stkDocMapper;
    private final StkDocItemMapper stkDocItemMapper;
    private final BasMaterialMapper basMaterialMapper;
    private final BasWarehouseMapper basWarehouseMapper;

    public InventoryServiceImpl(StkInventoryMapper stkInventoryMapper,
                                StkDocMapper stkDocMapper,
                                StkDocItemMapper stkDocItemMapper,
                                BasMaterialMapper basMaterialMapper,
                                BasWarehouseMapper basWarehouseMapper) {
        this.stkInventoryMapper = stkInventoryMapper;
        this.stkDocMapper = stkDocMapper;
        this.stkDocItemMapper = stkDocItemMapper;
        this.basMaterialMapper = basMaterialMapper;
        this.basWarehouseMapper = basWarehouseMapper;
    }

    @Override
    public PageResponse<InventoryStockListItem> pageStocks(String keyword,
                                                           Long materialId,
                                                           Long warehouseId,
                                                           String lotNo,
                                                           long pageNo,
                                                           long pageSize) {
        Set<Long> matchedMaterialIds = loadMatchedMaterialIds(keyword);
        Page<StkInventory> page = stkInventoryMapper.selectPage(
                new Page<>(pageNo, pageSize),
                buildStockQuery(keyword, matchedMaterialIds, materialId, warehouseId, lotNo)
        );

        Set<Long> materialIds = page.getRecords().stream()
                .map(StkInventory::getMaterialId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> warehouseIds = page.getRecords().stream()
                .map(StkInventory::getWarehouseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, BasMaterial> materialMap = loadMaterialMap(materialIds);
        Map<Long, String> warehouseNameMap = loadWarehouseMap(warehouseIds);

        List<InventoryStockListItem> records = page.getRecords().stream()
                .map(item -> {
                    BasMaterial material = materialMap.get(item.getMaterialId());
                    BigDecimal safetyStock = material != null && material.getSafetyStock() != null
                            ? material.getSafetyStock() : BigDecimal.ZERO;
                    BigDecimal qty = defaultDecimal(item.getQty());
                    BigDecimal lockedQty = defaultDecimal(item.getLockedQty());

                    InventoryStockListItem result = new InventoryStockListItem();
                    result.setId(item.getId());
                    result.setMaterialId(item.getMaterialId());
                    result.setMaterialCode(material != null ? material.getCode() : null);
                    result.setMaterialName(material != null ? material.getName() : null);
                    result.setUnitCode(material != null ? material.getUnitCode() : null);
                    result.setSafetyStock(safetyStock);
                    result.setWarehouseId(item.getWarehouseId());
                    result.setWarehouseName(warehouseNameMap.get(item.getWarehouseId()));
                    result.setLotNo(item.getLotNo());
                    result.setQty(qty);
                    result.setLockedQty(lockedQty);
                    result.setAvailableQty(qty.subtract(lockedQty));
                    result.setStockStatus(qty.compareTo(safetyStock) < 0 ? "warning" : "normal");
                    result.setUpdatedAt(item.getUpdatedAt());
                    return result;
                })
                .toList();

        return PageResponse.of(page.getTotal(), pageNo, pageSize, records);
    }

    @Override
    public PageResponse<InventoryDocListItem> pageDocs(String keyword,
                                                       String docType,
                                                       String status,
                                                       Long warehouseId,
                                                       long pageNo,
                                                       long pageSize) {
        return pageDocsByType(keyword, docType, status, warehouseId, pageNo, pageSize);
    }

    @Override
    public InventoryDocDetail getDoc(Long id) {
        return toDocDetail(requireDoc(id, null));
    }

    @Override
    public PageResponse<InventoryDocListItem> pageTransfers(String keyword, String status, Long warehouseId, long pageNo, long pageSize) {
        return pageDocsByType(keyword, DOC_TYPE_TRANSFER, status, warehouseId, pageNo, pageSize);
    }

    @Override
    public InventoryDocDetail getTransfer(Long id) {
        return toDocDetail(requireDoc(id, DOC_TYPE_TRANSFER));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTransfer(InventoryTransferSaveRequest request) {
        validateWarehouseExists(request.getWarehouseId(), "调拨源仓库不存在");
        validateWarehouseExists(request.getTargetWarehouseId(), "调拨目标仓库不存在");
        if (Objects.equals(request.getWarehouseId(), request.getTargetWarehouseId())) {
            throw new BusinessException(400, "调拨源仓库和目标仓库不能相同");
        }

        ensureUniqueMaterialLot(
                request.getItems().stream()
                        .map(item -> buildMaterialLotKey(item.getMaterialId(), item.getLotNo()))
                        .toList(),
                "调拨明细中存在重复的物料批次"
        );

        StkDoc doc = new StkDoc();
        doc.setCode(generateDocCode("TR"));
        doc.setDocType(DOC_TYPE_TRANSFER);
        doc.setWarehouseId(request.getWarehouseId());
        doc.setTargetWarehouseId(request.getTargetWarehouseId());
        doc.setBizDate(request.getBizDate());
        doc.setStatus(STATUS_DRAFT);
        doc.setRemark(request.getRemark());
        stkDocMapper.insert(doc);

        for (InventoryTransferSaveRequest.InventoryTransferItemRequest itemRequest : request.getItems()) {
            BasMaterial material = requireMaterial(itemRequest.getMaterialId());
            String normalizedLotNo = normalizeLotNo(itemRequest.getLotNo());
            validateBatchRequirement(material, normalizedLotNo);

            StkDocItem item = new StkDocItem();
            item.setDocId(doc.getId());
            item.setMaterialId(itemRequest.getMaterialId());
            item.setLotNo(normalizedLotNo);
            item.setQty(itemRequest.getQty());
            item.setUnitPrice(defaultDecimal(itemRequest.getUnitPrice()));
            item.setAmount(itemRequest.getQty().multiply(defaultDecimal(itemRequest.getUnitPrice())));
            stkDocItemMapper.insert(item);
        }

        return doc.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveTransfer(Long id) {
        StkDoc doc = requireDoc(id, DOC_TYPE_TRANSFER);
        ensureDraft(doc.getStatus(), "仅草稿状态的调拨单可以审核");
        if (doc.getTargetWarehouseId() == null) {
            throw new BusinessException(400, "调拨单缺少目标仓库");
        }

        List<StkDocItem> items = requireDocItems(doc.getId());
        for (StkDocItem item : items) {
            BasMaterial material = requireMaterial(item.getMaterialId());
            validateBatchRequirement(material, item.getLotNo());

            StkInventory sourceInventory = requireInventory(item.getMaterialId(), doc.getWarehouseId(), item.getLotNo());
            BigDecimal availableQty = defaultDecimal(sourceInventory.getQty()).subtract(defaultDecimal(sourceInventory.getLockedQty()));
            if (availableQty.compareTo(defaultDecimal(item.getQty())) < 0) {
                throw new BusinessException(409, material.getName() + " 可用库存不足");
            }
            sourceInventory.setQty(defaultDecimal(sourceInventory.getQty()).subtract(defaultDecimal(item.getQty())));
            stkInventoryMapper.updateById(sourceInventory);

            StkInventory targetInventory = findInventory(item.getMaterialId(), doc.getTargetWarehouseId(), item.getLotNo());
            if (targetInventory == null) {
                targetInventory = new StkInventory();
                targetInventory.setMaterialId(item.getMaterialId());
                targetInventory.setWarehouseId(doc.getTargetWarehouseId());
                targetInventory.setLotNo(item.getLotNo());
                targetInventory.setQty(defaultDecimal(item.getQty()));
                targetInventory.setLockedQty(BigDecimal.ZERO);
                targetInventory.setUpdatedAt(LocalDateTime.now());
                stkInventoryMapper.insert(targetInventory);
            } else {
                targetInventory.setQty(defaultDecimal(targetInventory.getQty()).add(defaultDecimal(item.getQty())));
                stkInventoryMapper.updateById(targetInventory);
            }
        }

        doc.setStatus(STATUS_APPROVED);
        stkDocMapper.updateById(doc);
    }

    @Override
    public PageResponse<InventoryDocListItem> pageChecks(String keyword, String status, Long warehouseId, long pageNo, long pageSize) {
        return pageDocsByType(keyword, DOC_TYPE_CHECK, status, warehouseId, pageNo, pageSize);
    }

    @Override
    public InventoryDocDetail getCheck(Long id) {
        return toDocDetail(requireDoc(id, DOC_TYPE_CHECK));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCheck(InventoryCheckSaveRequest request) {
        validateWarehouseExists(request.getWarehouseId(), "盘点仓库不存在");
        ensureUniqueMaterialLot(
                request.getItems().stream()
                        .map(item -> buildMaterialLotKey(item.getMaterialId(), item.getLotNo()))
                        .toList(),
                "盘点明细中存在重复的物料批次"
        );

        StkDoc doc = new StkDoc();
        doc.setCode(generateDocCode("CK"));
        doc.setDocType(DOC_TYPE_CHECK);
        doc.setWarehouseId(request.getWarehouseId());
        doc.setBizDate(request.getBizDate());
        doc.setStatus(STATUS_DRAFT);
        doc.setRemark(request.getRemark());
        stkDocMapper.insert(doc);

        for (InventoryCheckSaveRequest.InventoryCheckItemRequest itemRequest : request.getItems()) {
            BasMaterial material = requireMaterial(itemRequest.getMaterialId());
            String normalizedLotNo = normalizeLotNo(itemRequest.getLotNo());
            validateBatchRequirement(material, normalizedLotNo);

            StkDocItem item = new StkDocItem();
            item.setDocId(doc.getId());
            item.setMaterialId(itemRequest.getMaterialId());
            item.setLotNo(normalizedLotNo);
            item.setQty(itemRequest.getActualQty());
            item.setUnitPrice(BigDecimal.ZERO);
            item.setAmount(BigDecimal.ZERO);
            stkDocItemMapper.insert(item);
        }

        return doc.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveCheck(Long id) {
        StkDoc doc = requireDoc(id, DOC_TYPE_CHECK);
        ensureDraft(doc.getStatus(), "仅草稿状态的盘点单可以审核");

        List<StkDocItem> items = requireDocItems(doc.getId());
        for (StkDocItem item : items) {
            BasMaterial material = requireMaterial(item.getMaterialId());
            validateBatchRequirement(material, item.getLotNo());

            BigDecimal actualQty = defaultDecimal(item.getQty());
            StkInventory inventory = findInventory(item.getMaterialId(), doc.getWarehouseId(), item.getLotNo());
            if (inventory == null) {
                if (actualQty.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                inventory = new StkInventory();
                inventory.setMaterialId(item.getMaterialId());
                inventory.setWarehouseId(doc.getWarehouseId());
                inventory.setLotNo(item.getLotNo());
                inventory.setQty(actualQty);
                inventory.setLockedQty(BigDecimal.ZERO);
                inventory.setUpdatedAt(LocalDateTime.now());
                stkInventoryMapper.insert(inventory);
            } else {
                inventory.setQty(actualQty);
                if (defaultDecimal(inventory.getLockedQty()).compareTo(actualQty) > 0) {
                    inventory.setLockedQty(actualQty);
                }
                stkInventoryMapper.updateById(inventory);
            }
        }

        doc.setStatus(STATUS_APPROVED);
        stkDocMapper.updateById(doc);
    }

    private LambdaQueryWrapper<StkInventory> buildStockQuery(String keyword,
                                                             Set<Long> matchedMaterialIds,
                                                             Long materialId,
                                                             Long warehouseId,
                                                             String lotNo) {
        LambdaQueryWrapper<StkInventory> queryWrapper = new LambdaQueryWrapper<StkInventory>()
                .eq(materialId != null, StkInventory::getMaterialId, materialId)
                .eq(warehouseId != null, StkInventory::getWarehouseId, warehouseId)
                .like(StringUtils.hasText(lotNo), StkInventory::getLotNo, lotNo);
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> {
                if (matchedMaterialIds != null && !matchedMaterialIds.isEmpty()) {
                    wrapper.in(StkInventory::getMaterialId, matchedMaterialIds).or();
                }
                wrapper.like(StkInventory::getLotNo, keyword);
            });
        }
        return queryWrapper.orderByDesc(StkInventory::getUpdatedAt).orderByDesc(StkInventory::getId);
    }

    private PageResponse<InventoryDocListItem> pageDocsByType(String keyword,
                                                              String docType,
                                                              String status,
                                                              Long warehouseId,
                                                              long pageNo,
                                                              long pageSize) {
        Page<StkDoc> page = stkDocMapper.selectPage(
                new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<StkDoc>()
                        .eq(StringUtils.hasText(docType), StkDoc::getDocType, docType)
                        .and(StringUtils.hasText(keyword), wrapper -> wrapper.like(StkDoc::getCode, keyword))
                        .eq(StringUtils.hasText(status), StkDoc::getStatus, status)
                        .and(warehouseId != null, wrapper -> wrapper.eq(StkDoc::getWarehouseId, warehouseId)
                                .or()
                                .eq(StkDoc::getTargetWarehouseId, warehouseId))
                        .orderByDesc(StkDoc::getBizDate)
                        .orderByDesc(StkDoc::getId)
        );
        if (page.getRecords().isEmpty()) {
            return PageResponse.empty(pageNo, pageSize);
        }

        Set<Long> docIds = page.getRecords().stream().map(StkDoc::getId).collect(Collectors.toSet());
        List<StkDocItem> items = stkDocItemMapper.selectList(new LambdaQueryWrapper<StkDocItem>()
                .in(StkDocItem::getDocId, docIds)
                .orderByAsc(StkDocItem::getId));
        Map<Long, List<StkDocItem>> itemMap = items.stream().collect(Collectors.groupingBy(StkDocItem::getDocId));

        Set<Long> warehouseIds = page.getRecords().stream()
                .flatMap(item -> java.util.stream.Stream.of(item.getWarehouseId(), item.getTargetWarehouseId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> warehouseNameMap = loadWarehouseMap(warehouseIds);

        List<InventoryDocListItem> records = page.getRecords().stream()
                .map(doc -> {
                    List<StkDocItem> docItems = itemMap.getOrDefault(doc.getId(), List.of());
                    InventoryDocListItem result = new InventoryDocListItem();
                    result.setId(doc.getId());
                    result.setCode(doc.getCode());
                    result.setDocType(doc.getDocType());
                    result.setDocTypeName(resolveDocTypeName(doc.getDocType()));
                    result.setWarehouseId(doc.getWarehouseId());
                    result.setWarehouseName(warehouseNameMap.get(doc.getWarehouseId()));
                    result.setTargetWarehouseId(doc.getTargetWarehouseId());
                    result.setTargetWarehouseName(warehouseNameMap.get(doc.getTargetWarehouseId()));
                    result.setBizDate(doc.getBizDate());
                    result.setStatus(doc.getStatus());
                    result.setRemark(doc.getRemark());
                    result.setItemCount(docItems.size());
                    result.setTotalQty(sumQty(docItems));
                    result.setTotalAmount(sumAmount(docItems));
                    return result;
                })
                .toList();

        return PageResponse.of(page.getTotal(), pageNo, pageSize, records);
    }

    private InventoryDocDetail toDocDetail(StkDoc doc) {
        List<StkDocItem> items = requireDocItems(doc.getId());
        Set<Long> materialIds = items.stream()
                .map(StkDocItem::getMaterialId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> warehouseIds = new HashSet<>();
        warehouseIds.add(doc.getWarehouseId());
        if (doc.getTargetWarehouseId() != null) {
            warehouseIds.add(doc.getTargetWarehouseId());
        }

        Map<Long, BasMaterial> materialMap = loadMaterialMap(materialIds);
        Map<Long, String> warehouseNameMap = loadWarehouseMap(warehouseIds);

        InventoryDocDetail detail = new InventoryDocDetail();
        detail.setId(doc.getId());
        detail.setCode(doc.getCode());
        detail.setDocType(doc.getDocType());
        detail.setDocTypeName(resolveDocTypeName(doc.getDocType()));
        detail.setSourceType(doc.getSourceType());
        detail.setSourceId(doc.getSourceId());
        detail.setWarehouseId(doc.getWarehouseId());
        detail.setWarehouseName(warehouseNameMap.get(doc.getWarehouseId()));
        detail.setTargetWarehouseId(doc.getTargetWarehouseId());
        detail.setTargetWarehouseName(warehouseNameMap.get(doc.getTargetWarehouseId()));
        detail.setBizDate(doc.getBizDate());
        detail.setStatus(doc.getStatus());
        detail.setRemark(doc.getRemark());
        detail.setTotalQty(sumQty(items));
        detail.setTotalAmount(sumAmount(items));
        detail.setItems(items.stream().map(item -> {
            BasMaterial material = materialMap.get(item.getMaterialId());
            InventoryDocDetail.InventoryDocDetailItem result = new InventoryDocDetail.InventoryDocDetailItem();
            result.setId(item.getId());
            result.setMaterialId(item.getMaterialId());
            result.setMaterialCode(material != null ? material.getCode() : null);
            result.setMaterialName(material != null ? material.getName() : null);
            result.setUnitCode(material != null ? material.getUnitCode() : null);
            result.setLotNo(item.getLotNo());
            result.setQty(defaultDecimal(item.getQty()));
            result.setUnitPrice(defaultDecimal(item.getUnitPrice()));
            result.setAmount(defaultDecimal(item.getAmount()));
            result.setSourceItemId(item.getSourceItemId());
            return result;
        }).toList());
        return detail;
    }

    private StkDoc requireDoc(Long id, String expectedDocType) {
        StkDoc doc = stkDocMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException(404, "库存单据不存在");
        }
        if (StringUtils.hasText(expectedDocType) && !expectedDocType.equals(doc.getDocType())) {
            throw new BusinessException(404, "库存单据不存在");
        }
        return doc;
    }

    private List<StkDocItem> requireDocItems(Long docId) {
        List<StkDocItem> items = stkDocItemMapper.selectList(new LambdaQueryWrapper<StkDocItem>()
                .eq(StkDocItem::getDocId, docId)
                .orderByAsc(StkDocItem::getId));
        if (items.isEmpty()) {
            throw new BusinessException(400, "库存单据缺少明细");
        }
        return items;
    }

    private StkInventory requireInventory(Long materialId, Long warehouseId, String lotNo) {
        StkInventory inventory = findInventory(materialId, warehouseId, lotNo);
        if (inventory == null) {
            throw new BusinessException(409, "库存记录不存在");
        }
        return inventory;
    }

    private StkInventory findInventory(Long materialId, Long warehouseId, String lotNo) {
        LambdaQueryWrapper<StkInventory> queryWrapper = new LambdaQueryWrapper<StkInventory>()
                .eq(StkInventory::getMaterialId, materialId)
                .eq(StkInventory::getWarehouseId, warehouseId);
        if (StringUtils.hasText(lotNo)) {
            queryWrapper.eq(StkInventory::getLotNo, lotNo);
        } else {
            queryWrapper.isNull(StkInventory::getLotNo);
        }
        return stkInventoryMapper.selectOne(queryWrapper.last("limit 1"));
    }

    private BasMaterial requireMaterial(Long materialId) {
        BasMaterial material = basMaterialMapper.selectById(materialId);
        if (material == null) {
            throw new BusinessException(400, "物料不存在");
        }
        return material;
    }

    private void validateWarehouseExists(Long warehouseId, String message) {
        if (warehouseId == null || basWarehouseMapper.selectById(warehouseId) == null) {
            throw new BusinessException(400, message);
        }
    }

    private void validateBatchRequirement(BasMaterial material, String lotNo) {
        if (material != null && Integer.valueOf(1).equals(material.getBatchEnabled()) && !StringUtils.hasText(lotNo)) {
            throw new BusinessException(400, material.getName() + " 已启用批次管理，批次号不能为空");
        }
    }

    private void ensureDraft(String status, String message) {
        if (!STATUS_DRAFT.equals(status)) {
            throw new BusinessException(409, message);
        }
    }

    private void ensureUniqueMaterialLot(List<String> keys, String message) {
        Set<String> seen = new HashSet<>();
        for (String key : keys) {
            if (!seen.add(key)) {
                throw new BusinessException(400, message);
            }
        }
    }

    private String buildMaterialLotKey(Long materialId, String lotNo) {
        return materialId + "::" + normalizeLotNo(lotNo);
    }

    private String normalizeLotNo(String lotNo) {
        return StringUtils.hasText(lotNo) ? lotNo.trim() : null;
    }

    private Set<Long> loadMatchedMaterialIds(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return basMaterialMapper.selectList(
                        new LambdaQueryWrapper<BasMaterial>()
                                .like(BasMaterial::getCode, keyword)
                                .or()
                                .like(BasMaterial::getName, keyword)
                ).stream()
                .map(BasMaterial::getId)
                .collect(Collectors.toSet());
    }

    private Map<Long, BasMaterial> loadMaterialMap(Set<Long> materialIds) {
        if (materialIds.isEmpty()) {
            return Map.of();
        }
        return basMaterialMapper.selectBatchIds(materialIds).stream()
                .collect(Collectors.toMap(BasMaterial::getId, item -> item, (left, right) -> left));
    }

    private Map<Long, String> loadWarehouseMap(Set<Long> warehouseIds) {
        if (warehouseIds.isEmpty()) {
            return Map.of();
        }
        return basWarehouseMapper.selectBatchIds(warehouseIds).stream()
                .collect(Collectors.toMap(BasWarehouse::getId, BasWarehouse::getName, (left, right) -> left));
    }

    private BigDecimal sumQty(List<StkDocItem> items) {
        return items.stream().map(item -> defaultDecimal(item.getQty())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumAmount(List<StkDocItem> items) {
        return items.stream().map(item -> defaultDecimal(item.getAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String resolveDocTypeName(String docType) {
        return switch (docType) {
            case "purchase_in" -> "采购入库";
            case "sales_out" -> "销售出库";
            case "work_order_pick" -> "生产领料";
            case "work_order_finish_in" -> "完工入库";
            case DOC_TYPE_TRANSFER -> "库存调拨";
            case DOC_TYPE_CHECK -> "库存盘点";
            default -> "库存单";
        };
    }

    private String generateDocCode(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }
}
