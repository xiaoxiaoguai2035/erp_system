package com.dongjian.erp.manufacturingerpsystem.modules.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dongjian.erp.manufacturingerpsystem.common.exception.BusinessException;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.entity.BasMaterial;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.mapper.BasMaterialMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.mapper.BasWarehouseMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.entity.StkDoc;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.entity.StkDocItem;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.entity.StkInventory;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.mapper.StkDocItemMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.mapper.StkDocMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.mapper.StkInventoryMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.service.InventoryOperationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class InventoryOperationServiceImpl implements InventoryOperationService {

    private final StkDocMapper stkDocMapper;
    private final StkDocItemMapper stkDocItemMapper;
    private final StkInventoryMapper stkInventoryMapper;
    private final BasMaterialMapper basMaterialMapper;
    private final BasWarehouseMapper basWarehouseMapper;

    public InventoryOperationServiceImpl(StkDocMapper stkDocMapper,
                                         StkDocItemMapper stkDocItemMapper,
                                         StkInventoryMapper stkInventoryMapper,
                                         BasMaterialMapper basMaterialMapper,
                                         BasWarehouseMapper basWarehouseMapper) {
        this.stkDocMapper = stkDocMapper;
        this.stkDocItemMapper = stkDocItemMapper;
        this.stkInventoryMapper = stkInventoryMapper;
        this.basMaterialMapper = basMaterialMapper;
        this.basWarehouseMapper = basWarehouseMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createInStockDoc(String docType,
                                 String sourceType,
                                 Long sourceId,
                                 Long warehouseId,
                                 LocalDate bizDate,
                                 String remark,
                                 List<DocItemParam> items) {
        validateWarehouse(warehouseId);
        StkDoc doc = createDoc(docType, sourceType, sourceId, warehouseId, bizDate, remark);
        for (DocItemParam item : items) {
            BasMaterial material = requireMaterial(item.getMaterialId());
            String lotNo = normalizeLotNo(item.getLotNo());
            validateBatch(material, lotNo);
            insertDocItem(doc.getId(), item, lotNo);
            increaseInventory(item.getMaterialId(), warehouseId, lotNo, item.getQty());
        }
        return doc.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOutStockDoc(String docType,
                                  String sourceType,
                                  Long sourceId,
                                  Long warehouseId,
                                  LocalDate bizDate,
                                  String remark,
                                  List<DocItemParam> items) {
        validateWarehouse(warehouseId);
        StkDoc doc = createDoc(docType, sourceType, sourceId, warehouseId, bizDate, remark);
        for (DocItemParam item : items) {
            BasMaterial material = requireMaterial(item.getMaterialId());
            String lotNo = normalizeLotNo(item.getLotNo());
            validateBatch(material, lotNo);
            decreaseInventory(material, warehouseId, lotNo, item.getQty());
            insertDocItem(doc.getId(), item, lotNo);
        }
        return doc.getId();
    }

    private StkDoc createDoc(String docType,
                             String sourceType,
                             Long sourceId,
                             Long warehouseId,
                             LocalDate bizDate,
                             String remark) {
        StkDoc doc = new StkDoc();
        doc.setCode(generateDocCode(docType));
        doc.setDocType(docType);
        doc.setSourceType(sourceType);
        doc.setSourceId(sourceId);
        doc.setWarehouseId(warehouseId);
        doc.setBizDate(bizDate);
        doc.setStatus("approved");
        doc.setRemark(remark);
        stkDocMapper.insert(doc);
        return doc;
    }

    private void insertDocItem(Long docId, DocItemParam item, String lotNo) {
        StkDocItem docItem = new StkDocItem();
        docItem.setDocId(docId);
        docItem.setMaterialId(item.getMaterialId());
        docItem.setLotNo(lotNo);
        docItem.setQty(defaultDecimal(item.getQty()));
        docItem.setUnitPrice(defaultDecimal(item.getUnitPrice()));
        docItem.setAmount(defaultDecimal(item.getAmount()));
        docItem.setSourceItemId(item.getSourceItemId());
        stkDocItemMapper.insert(docItem);
    }

    private void increaseInventory(Long materialId, Long warehouseId, String lotNo, BigDecimal qty) {
        StkInventory inventory = findInventory(materialId, warehouseId, lotNo);
        if (inventory == null) {
            inventory = new StkInventory();
            inventory.setMaterialId(materialId);
            inventory.setWarehouseId(warehouseId);
            inventory.setLotNo(lotNo);
            inventory.setQty(defaultDecimal(qty));
            inventory.setLockedQty(BigDecimal.ZERO);
            inventory.setUpdatedAt(LocalDateTime.now());
            stkInventoryMapper.insert(inventory);
            return;
        }
        inventory.setQty(defaultDecimal(inventory.getQty()).add(defaultDecimal(qty)));
        stkInventoryMapper.updateById(inventory);
    }

    private void decreaseInventory(BasMaterial material, Long warehouseId, String lotNo, BigDecimal qty) {
        StkInventory inventory = findInventory(material.getId(), warehouseId, lotNo);
        if (inventory == null) {
            throw new BusinessException(409, material.getName() + " 库存不足");
        }
        BigDecimal availableQty = defaultDecimal(inventory.getQty()).subtract(defaultDecimal(inventory.getLockedQty()));
        if (availableQty.compareTo(defaultDecimal(qty)) < 0) {
            throw new BusinessException(409, material.getName() + " 可用库存不足");
        }
        inventory.setQty(defaultDecimal(inventory.getQty()).subtract(defaultDecimal(qty)));
        stkInventoryMapper.updateById(inventory);
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

    private void validateWarehouse(Long warehouseId) {
        if (warehouseId == null || basWarehouseMapper.selectById(warehouseId) == null) {
            throw new BusinessException(400, "仓库不存在");
        }
    }

    private void validateBatch(BasMaterial material, String lotNo) {
        if (Integer.valueOf(1).equals(material.getBatchEnabled()) && !StringUtils.hasText(lotNo)) {
            throw new BusinessException(400, material.getName() + " 已启用批次管理，批次号不能为空");
        }
    }

    private String normalizeLotNo(String lotNo) {
        return StringUtils.hasText(lotNo) ? lotNo.trim() : null;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String generateDocCode(String docType) {
        String prefix = switch (docType) {
            case "purchase_in" -> "PI";
            case "sales_out" -> "SO";
            case "work_order_pick" -> "WP";
            case "work_order_finish_in" -> "FI";
            default -> "ST";
        };
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }
}
