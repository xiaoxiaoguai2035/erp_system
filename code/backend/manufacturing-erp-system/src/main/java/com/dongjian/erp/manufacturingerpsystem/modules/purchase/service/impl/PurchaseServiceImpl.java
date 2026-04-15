package com.dongjian.erp.manufacturingerpsystem.modules.purchase.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dongjian.erp.manufacturingerpsystem.common.exception.BusinessException;
import com.dongjian.erp.manufacturingerpsystem.common.page.PageResponse;
import com.dongjian.erp.manufacturingerpsystem.common.util.DocSourceTraceCodec;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.entity.BasMaterial;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.entity.BasSupplier;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.entity.BasWarehouse;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.mapper.BasMaterialMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.mapper.BasSupplierMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.mapper.BasWarehouseMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.purchase.dto.PurchaseOrderSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.purchase.dto.PurchaseInStockRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.purchase.entity.PurDoc;
import com.dongjian.erp.manufacturingerpsystem.modules.purchase.entity.PurDocItem;
import com.dongjian.erp.manufacturingerpsystem.modules.purchase.mapper.PurDocMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.purchase.mapper.PurDocItemMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.service.InventoryOperationService;
import com.dongjian.erp.manufacturingerpsystem.modules.purchase.service.PurchaseService;
import com.dongjian.erp.manufacturingerpsystem.modules.purchase.vo.PurchaseOrderDetail;
import com.dongjian.erp.manufacturingerpsystem.modules.purchase.vo.PurchaseOrderListItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PurchaseServiceImpl implements PurchaseService {
    private static final String DOC_TYPE_REQUEST = "request";
    private static final String DOC_TYPE_ORDER = "order";

    private final PurDocMapper purDocMapper;
    private final PurDocItemMapper purDocItemMapper;
    private final BasSupplierMapper basSupplierMapper;
    private final BasMaterialMapper basMaterialMapper;
    private final BasWarehouseMapper basWarehouseMapper;
    private final InventoryOperationService inventoryOperationService;

    public PurchaseServiceImpl(PurDocMapper purDocMapper,
                               PurDocItemMapper purDocItemMapper,
                               BasSupplierMapper basSupplierMapper,
                               BasMaterialMapper basMaterialMapper,
                               BasWarehouseMapper basWarehouseMapper,
                               InventoryOperationService inventoryOperationService) {
        this.purDocMapper = purDocMapper;
        this.purDocItemMapper = purDocItemMapper;
        this.basSupplierMapper = basSupplierMapper;
        this.basMaterialMapper = basMaterialMapper;
        this.basWarehouseMapper = basWarehouseMapper;
        this.inventoryOperationService = inventoryOperationService;
    }

    @Override
    public PageResponse<PurchaseOrderListItem> pageRequests(String keyword, String status, long pageNo, long pageSize) {
        return pageDocs(DOC_TYPE_REQUEST, keyword, status, pageNo, pageSize);
    }

    @Override
    public PurchaseOrderDetail getRequest(Long id) {
        return getDocDetail(id, DOC_TYPE_REQUEST, "采购申请不存在");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRequest(PurchaseOrderSaveRequest request) {
        return createDoc(request, DOC_TYPE_REQUEST);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRequest(Long id, PurchaseOrderSaveRequest request) {
        updateDoc(id, request, DOC_TYPE_REQUEST, "采购申请仅草稿状态允许修改", "采购申请不存在");
    }

    @Override
    public void approveRequest(Long id) {
        approveDoc(id, DOC_TYPE_REQUEST, "采购申请不存在", "仅草稿状态的采购申请可以审核");
    }

    @Override
    public void closeRequest(Long id) {
        closeDoc(id, DOC_TYPE_REQUEST, "采购申请不存在", "采购申请已关闭");
    }

    @Override
    public PageResponse<PurchaseOrderListItem> pageOrders(String keyword, String status, long pageNo, long pageSize) {
        return pageDocs(DOC_TYPE_ORDER, keyword, status, pageNo, pageSize);
    }

    @Override
    public PurchaseOrderDetail getOrder(Long id) {
        return getDocDetail(id, DOC_TYPE_ORDER, "采购订单不存在");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(PurchaseOrderSaveRequest request) {
        return createDoc(request, DOC_TYPE_ORDER);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(Long id, PurchaseOrderSaveRequest request) {
        updateDoc(id, request, DOC_TYPE_ORDER, "采购订单仅草稿状态允许修改", "采购订单不存在");
    }

    @Override
    public void approveOrder(Long id) {
        approveDoc(id, DOC_TYPE_ORDER, "采购订单不存在", "仅草稿状态的采购订单可以审核");
    }

    @Override
    public void closeOrder(Long id) {
        closeDoc(id, DOC_TYPE_ORDER, "采购订单不存在", "采购订单已关闭");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long inStock(Long id, PurchaseInStockRequest request) {
        PurDoc doc = requireOrder(id);
        if ("draft".equals(doc.getStatus()) || "closed".equals(doc.getStatus())) {
            throw new BusinessException(409, "当前采购订单不允许执行入库");
        }
        if (basWarehouseMapper.selectById(request.getWarehouseId()) == null) {
            throw new BusinessException(400, "入库仓库不存在");
        }

        List<PurDocItem> sourceItems = purDocItemMapper.selectList(new LambdaQueryWrapper<PurDocItem>()
                .eq(PurDocItem::getDocId, id)
                .orderByAsc(PurDocItem::getId));
        Map<Long, PurDocItem> sourceItemMap = sourceItems.stream()
                .collect(Collectors.toMap(PurDocItem::getId, item -> item, (left, right) -> left));

        List<InventoryOperationService.DocItemParam> docItems = request.getItems().stream()
                .map(itemRequest -> {
                    PurDocItem sourceItem = sourceItemMap.get(itemRequest.getSourceItemId());
                    if (sourceItem == null) {
                        throw new BusinessException(400, "采购入库来源明细不存在");
                    }
                    if (!Objects.equals(sourceItem.getMaterialId(), itemRequest.getMaterialId())) {
                        throw new BusinessException(400, "采购入库物料与来源明细不一致");
                    }
                    if (itemRequest.getReceivedQty().compareTo(itemRequest.getQualifiedQty()) < 0) {
                        throw new BusinessException(400, "到货数量不能小于合格数量");
                    }
                    if (itemRequest.getQualifiedQty().compareTo(itemRequest.getQty()) < 0) {
                        throw new BusinessException(400, "合格数量不能小于入库数量");
                    }

                    BigDecimal remainQty = defaultDecimal(sourceItem.getQty()).subtract(defaultDecimal(sourceItem.getReceivedQty()));
                    if (itemRequest.getReceivedQty().compareTo(remainQty) > 0) {
                        throw new BusinessException(409, "采购明细到货数量超过剩余未到货数量");
                    }

                    sourceItem.setReceivedQty(defaultDecimal(sourceItem.getReceivedQty()).add(itemRequest.getReceivedQty()));
                    sourceItem.setQualifiedQty(defaultDecimal(sourceItem.getQualifiedQty()).add(itemRequest.getQualifiedQty()));
                    sourceItem.setLotNo(itemRequest.getLotNo());
                    purDocItemMapper.updateById(sourceItem);

                    InventoryOperationService.DocItemParam docItem = new InventoryOperationService.DocItemParam();
                    docItem.setSourceItemId(sourceItem.getId());
                    docItem.setMaterialId(itemRequest.getMaterialId());
                    docItem.setLotNo(itemRequest.getLotNo());
                    docItem.setQty(itemRequest.getQty());
                    docItem.setUnitPrice(sourceItem.getPrice());
                    docItem.setAmount(itemRequest.getQty().multiply(defaultDecimal(sourceItem.getPrice())));
                    return docItem;
                })
                .toList();

        Long stockDocId = inventoryOperationService.createInStockDoc(
                "purchase_in",
                "purchase_order",
                id,
                request.getWarehouseId(),
                request.getBizDate(),
                request.getRemark(),
                docItems
        );

        syncPurchaseOrderStatus(doc, sourceItemMap.values().stream().toList());
        return stockDocId;
    }

    private Map<Long, String> buildSupplierMap(Set<Long> supplierIds) {
        if (supplierIds.isEmpty()) {
            return Map.of();
        }

        return basSupplierMapper.selectBatchIds(supplierIds).stream()
                .collect(Collectors.toMap(BasSupplier::getId, BasSupplier::getName, (left, right) -> left));
    }

    private PurDoc requireOrder(Long id) {
        return requireDoc(id, DOC_TYPE_ORDER, "采购订单不存在");
    }

    private PurDoc requireDoc(Long id, String docType, String notFoundMessage) {
        PurDoc doc = purDocMapper.selectById(id);
        if (doc == null || !docType.equals(doc.getDocType())) {
            throw new BusinessException(404, notFoundMessage);
        }
        return doc;
    }

    private void ensureDraft(String status, String message) {
        if (!"draft".equals(status)) {
            throw new BusinessException(409, message);
        }
    }

    private List<PurDocItem> saveOrderItems(Long docId, PurchaseOrderSaveRequest request) {
        List<PurDocItem> savedItems = new java.util.ArrayList<>();
        for (PurchaseOrderSaveRequest.PurchaseOrderItemRequest itemRequest : request.getItems()) {
            if (basMaterialMapper.selectById(itemRequest.getMaterialId()) == null) {
                throw new BusinessException(400, "采购明细中的物料不存在");
            }
            if (itemRequest.getWarehouseId() != null && basWarehouseMapper.selectById(itemRequest.getWarehouseId()) == null) {
                throw new BusinessException(400, "采购明细中的仓库不存在");
            }

            PurDocItem item = new PurDocItem();
            item.setDocId(docId);
            item.setMaterialId(itemRequest.getMaterialId());
            item.setWarehouseId(itemRequest.getWarehouseId());
            item.setQty(itemRequest.getQty());
            item.setReceivedQty(BigDecimal.ZERO);
            item.setQualifiedQty(BigDecimal.ZERO);
            item.setPrice(itemRequest.getPrice());
            item.setAmount(itemRequest.getQty().multiply(itemRequest.getPrice()));
            item.setNeedDate(itemRequest.getNeedDate());
            purDocItemMapper.insert(item);
            savedItems.add(item);
        }
        return savedItems;
    }

    private String resolveSupplierName(Long supplierId) {
        BasSupplier supplier = basSupplierMapper.selectById(supplierId);
        return supplier != null ? supplier.getName() : null;
    }

    private PageResponse<PurchaseOrderListItem> pageDocs(String docType, String keyword, String status, long pageNo, long pageSize) {
        Page<PurDoc> page = purDocMapper.selectPage(
                new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<PurDoc>()
                        .eq(PurDoc::getDocType, docType)
                        .and(StringUtils.hasText(keyword), wrapper -> wrapper.like(PurDoc::getCode, keyword))
                        .eq(StringUtils.hasText(status), PurDoc::getStatus, status)
                        .orderByDesc(PurDoc::getId)
        );

        Map<Long, String> supplierNameMap = buildSupplierMap(page.getRecords().stream()
                .map(PurDoc::getSupplierId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        return PageResponse.of(
                page.getTotal(),
                pageNo,
                pageSize,
                page.getRecords().stream().map(item -> {
                    DocSourceTraceCodec.TraceMetadata traceMetadata = DocSourceTraceCodec.parse(item.getRemark());
                    PurchaseOrderListItem result = new PurchaseOrderListItem();
                    result.setId(item.getId());
                    result.setCode(item.getCode());
                    result.setDocType(item.getDocType());
                    result.setSupplierId(item.getSupplierId());
                    result.setSupplierName(item.getSupplierId() != null ? supplierNameMap.get(item.getSupplierId()) : null);
                    result.setDocDate(item.getDocDate());
                    result.setExpectedDate(item.getExpectedDate());
                    result.setTotalAmount(item.getTotalAmount());
                    result.setStatus(item.getStatus());
                    result.setRemark(traceMetadata.getUserRemark());
                    return result;
                }).toList()
        );
    }

    private PurchaseOrderDetail getDocDetail(Long id, String docType, String notFoundMessage) {
        PurDoc doc = requireDoc(id, docType, notFoundMessage);
        DocSourceTraceCodec.TraceMetadata traceMetadata = DocSourceTraceCodec.parse(doc.getRemark());
        List<PurDocItem> items = purDocItemMapper.selectList(new LambdaQueryWrapper<PurDocItem>()
                .eq(PurDocItem::getDocId, id)
                .orderByAsc(PurDocItem::getId));

        Set<Long> materialIds = items.stream()
                .map(PurDocItem::getMaterialId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> warehouseIds = items.stream()
                .map(PurDocItem::getWarehouseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, BasMaterial> materialMap = materialIds.isEmpty()
                ? Map.of()
                : basMaterialMapper.selectBatchIds(materialIds).stream()
                .collect(Collectors.toMap(BasMaterial::getId, item -> item, (left, right) -> left));
        Map<Long, BigDecimal> convertedQtyMap = DOC_TYPE_REQUEST.equals(docType)
                ? loadConvertedQtyBySourceItem(id, null)
                : Map.of();

        Map<Long, String> warehouseMap = warehouseIds.isEmpty()
                ? Map.of()
                : basWarehouseMapper.selectBatchIds(warehouseIds).stream()
                .collect(Collectors.toMap(BasWarehouse::getId, BasWarehouse::getName, (left, right) -> left));

        PurchaseOrderDetail detail = new PurchaseOrderDetail();
        detail.setId(doc.getId());
        detail.setCode(doc.getCode());
        detail.setDocType(doc.getDocType());
        detail.setSupplierId(doc.getSupplierId());
        detail.setSupplierName(resolveSupplierName(doc.getSupplierId()));
        detail.setDocDate(doc.getDocDate());
        detail.setExpectedDate(doc.getExpectedDate());
        detail.setTotalAmount(doc.getTotalAmount());
        detail.setStatus(doc.getStatus());
        detail.setSourceDocId(traceMetadata.getSourceDocId());
        detail.setSourceDocType(traceMetadata.getSourceDocType());
        detail.setRemark(traceMetadata.getUserRemark());
        detail.setItems(items.stream().map(item -> {
            BasMaterial material = materialMap.get(item.getMaterialId());
            PurchaseOrderDetail.PurchaseOrderDetailItem result = new PurchaseOrderDetail.PurchaseOrderDetailItem();
            result.setId(item.getId());
            result.setMaterialId(item.getMaterialId());
            result.setMaterialCode(material != null ? material.getCode() : null);
            result.setMaterialName(material != null ? material.getName() : null);
            result.setWarehouseId(item.getWarehouseId());
            result.setWarehouseName(warehouseMap.get(item.getWarehouseId()));
            result.setQty(item.getQty());
            result.setReceivedQty(item.getReceivedQty());
            result.setQualifiedQty(item.getQualifiedQty());
            BigDecimal convertedQty = convertedQtyMap.getOrDefault(item.getId(), BigDecimal.ZERO);
            result.setConvertedQty(convertedQty);
            result.setRemainConvertQty(defaultDecimal(item.getQty()).subtract(convertedQty).max(BigDecimal.ZERO));
            result.setPrice(item.getPrice());
            result.setAmount(item.getAmount());
            result.setNeedDate(item.getNeedDate());
            result.setSourceItemId(resolveSourceItemId(traceMetadata, items, item.getId()));
            return result;
        }).toList());
        return detail;
    }

    private Long createDoc(PurchaseOrderSaveRequest request, String docType) {
        if (request.getSupplierId() != null && basSupplierMapper.selectById(request.getSupplierId()) == null) {
            throw new BusinessException(400, "供应商不存在");
        }
        validateSourceTrace(docType, request, null);

        PurDoc doc = new PurDoc();
        doc.setCode(generateOrderCode(docType));
        doc.setDocType(docType);
        doc.setSupplierId(request.getSupplierId());
        doc.setDocDate(request.getDocDate());
        doc.setExpectedDate(request.getExpectedDate());
        doc.setStatus("draft");
        doc.setRemark("");
        doc.setTotalAmount(calculateTotalAmount(request));
        purDocMapper.insert(doc);
        List<PurDocItem> savedItems = saveOrderItems(doc.getId(), request);
        updateDocRemarkWithTrace(doc, request, savedItems);
        return doc.getId();
    }

    private void updateDoc(Long id, PurchaseOrderSaveRequest request, String docType, String draftMessage, String notFoundMessage) {
        PurDoc doc = requireDoc(id, docType, notFoundMessage);
        ensureDraft(doc.getStatus(), draftMessage);
        if (request.getSupplierId() != null && basSupplierMapper.selectById(request.getSupplierId()) == null) {
            throw new BusinessException(400, "供应商不存在");
        }
        validateSourceTrace(docType, request, id);

        doc.setSupplierId(request.getSupplierId());
        doc.setDocDate(request.getDocDate());
        doc.setExpectedDate(request.getExpectedDate());
        doc.setTotalAmount(calculateTotalAmount(request));

        purDocItemMapper.delete(new LambdaQueryWrapper<PurDocItem>().eq(PurDocItem::getDocId, id));
        List<PurDocItem> savedItems = saveOrderItems(id, request);
        updateDocRemarkWithTrace(doc, request, savedItems);
    }

    private void approveDoc(Long id, String docType, String notFoundMessage, String draftMessage) {
        PurDoc doc = requireDoc(id, docType, notFoundMessage);
        ensureDraft(doc.getStatus(), draftMessage);
        doc.setStatus("approved");
        purDocMapper.updateById(doc);
    }

    private void closeDoc(Long id, String docType, String notFoundMessage, String closedMessage) {
        PurDoc doc = requireDoc(id, docType, notFoundMessage);
        if ("closed".equals(doc.getStatus())) {
            throw new BusinessException(409, closedMessage);
        }
        doc.setStatus("closed");
        purDocMapper.updateById(doc);
    }

    private void syncPurchaseOrderStatus(PurDoc doc, List<PurDocItem> items) {
        boolean completed = items.stream().allMatch(item ->
                defaultDecimal(item.getReceivedQty()).compareTo(defaultDecimal(item.getQty())) >= 0);
        boolean partial = items.stream().anyMatch(item ->
                defaultDecimal(item.getReceivedQty()).compareTo(BigDecimal.ZERO) > 0);

        if (completed) {
            doc.setStatus("completed");
        } else if (partial) {
            doc.setStatus("partial");
        }
        purDocMapper.updateById(doc);
    }

    private BigDecimal calculateTotalAmount(PurchaseOrderSaveRequest request) {
        return request.getItems().stream()
                .map(item -> item.getQty().multiply(item.getPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void updateDocRemarkWithTrace(PurDoc doc, PurchaseOrderSaveRequest request, List<PurDocItem> savedItems) {
        String remark = request.getRemark();
        if (DOC_TYPE_ORDER.equals(doc.getDocType()) && request.getSourceDocId() != null) {
            if (savedItems.size() != request.getItems().size()) {
                throw new BusinessException(500, "采购订单来源映射写入失败");
            }
            remark = DocSourceTraceCodec.encode(
                    request.getSourceDocType(),
                    request.getSourceDocId(),
                    request.getItems().stream().map(PurchaseOrderSaveRequest.PurchaseOrderItemRequest::getSourceItemId).toList(),
                    request.getRemark()
            );
        }
        if (remark != null && remark.length() > 255) {
            throw new BusinessException(400, "来源追踪信息过长，请减少下推明细或缩短备注");
        }
        doc.setRemark(remark);
        purDocMapper.updateById(doc);
    }

    private void validateSourceTrace(String docType, PurchaseOrderSaveRequest request, Long currentDocId) {
        boolean hasSourceDoc = request.getSourceDocId() != null || StringUtils.hasText(request.getSourceDocType());
        boolean hasSourceItem = request.getItems().stream().anyMatch(item -> item.getSourceItemId() != null);

        if (!DOC_TYPE_ORDER.equals(docType)) {
            if (hasSourceDoc || hasSourceItem) {
                throw new BusinessException(400, "采购申请不支持写入来源转单关系");
            }
            return;
        }

        if (!hasSourceDoc && !hasSourceItem) {
            return;
        }
        if (request.getSourceDocId() == null || !StringUtils.hasText(request.getSourceDocType())) {
            throw new BusinessException(400, "来源采购申请信息不完整");
        }
        if (!DOC_TYPE_REQUEST.equals(request.getSourceDocType())) {
            throw new BusinessException(400, "采购订单仅支持来源采购申请");
        }

        PurDoc sourceDoc = requireDoc(request.getSourceDocId(), DOC_TYPE_REQUEST, "来源采购申请不存在");
        if (!"approved".equalsIgnoreCase(sourceDoc.getStatus()) && !"partial".equalsIgnoreCase(sourceDoc.getStatus())) {
            throw new BusinessException(409, "仅已审核或部分执行的采购申请允许下推采购订单");
        }

        List<PurDocItem> sourceItems = purDocItemMapper.selectList(new LambdaQueryWrapper<PurDocItem>()
                .eq(PurDocItem::getDocId, sourceDoc.getId())
                .orderByAsc(PurDocItem::getId));
        Map<Long, PurDocItem> sourceItemMap = sourceItems.stream()
                .collect(Collectors.toMap(PurDocItem::getId, item -> item, (left, right) -> left));
        Map<Long, BigDecimal> convertedQtyMap = loadConvertedQtyBySourceItem(sourceDoc.getId(), currentDocId);
        Map<Long, BigDecimal> requestQtyMap = new java.util.LinkedHashMap<>();

        for (PurchaseOrderSaveRequest.PurchaseOrderItemRequest itemRequest : request.getItems()) {
            if (itemRequest.getSourceItemId() == null) {
                throw new BusinessException(400, "来源采购申请明细不能为空");
            }
            PurDocItem sourceItem = sourceItemMap.get(itemRequest.getSourceItemId());
            if (sourceItem == null) {
                throw new BusinessException(400, "来源采购申请明细不存在");
            }
            if (!Objects.equals(sourceItem.getMaterialId(), itemRequest.getMaterialId())) {
                throw new BusinessException(400, "采购订单物料与来源申请明细不一致");
            }
            requestQtyMap.merge(itemRequest.getSourceItemId(), defaultDecimal(itemRequest.getQty()), BigDecimal::add);
        }

        for (Map.Entry<Long, BigDecimal> entry : requestQtyMap.entrySet()) {
            PurDocItem sourceItem = sourceItemMap.get(entry.getKey());
            BigDecimal remainQty = defaultDecimal(sourceItem.getQty())
                    .subtract(convertedQtyMap.getOrDefault(entry.getKey(), BigDecimal.ZERO))
                    .max(BigDecimal.ZERO);
            if (entry.getValue().compareTo(remainQty) > 0) {
                BasMaterial material = basMaterialMapper.selectById(sourceItem.getMaterialId());
                String materialName = material != null ? material.getName() : "来源物料";
                throw new BusinessException(409, materialName + " 超过来源申请剩余可转订单数量");
            }
        }
    }

    private Map<Long, BigDecimal> loadConvertedQtyBySourceItem(Long sourceDocId, Long currentDocId) {
        List<PurDoc> orders = purDocMapper.selectList(new LambdaQueryWrapper<PurDoc>()
                .eq(PurDoc::getDocType, DOC_TYPE_ORDER)
                .ne(currentDocId != null, PurDoc::getId, currentDocId)
                .ne(PurDoc::getStatus, "closed")
                .orderByAsc(PurDoc::getId));
        List<PurDoc> linkedOrders = orders.stream()
                .filter(item -> {
                    DocSourceTraceCodec.TraceMetadata metadata = DocSourceTraceCodec.parse(item.getRemark());
                    return DOC_TYPE_REQUEST.equals(metadata.getSourceDocType()) && Objects.equals(metadata.getSourceDocId(), sourceDocId);
                })
                .toList();
        if (linkedOrders.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<PurDocItem>> itemGroup = purDocItemMapper.selectList(new LambdaQueryWrapper<PurDocItem>()
                        .in(PurDocItem::getDocId, linkedOrders.stream().map(PurDoc::getId).toList())
                        .orderByAsc(PurDocItem::getId))
                .stream()
                .collect(Collectors.groupingBy(PurDocItem::getDocId, java.util.LinkedHashMap::new, Collectors.toList()));

        Map<Long, BigDecimal> convertedQtyMap = new java.util.LinkedHashMap<>();
        for (PurDoc order : linkedOrders) {
            DocSourceTraceCodec.TraceMetadata metadata = DocSourceTraceCodec.parse(order.getRemark());
            List<PurDocItem> orderItems = itemGroup.getOrDefault(order.getId(), List.of());
            for (int index = 0; index < orderItems.size() && index < metadata.getSourceItemIds().size(); index++) {
                convertedQtyMap.merge(
                        metadata.getSourceItemIds().get(index),
                        defaultDecimal(orderItems.get(index).getQty()),
                        BigDecimal::add
                );
            }
        }
        return convertedQtyMap;
    }

    private Long resolveSourceItemId(DocSourceTraceCodec.TraceMetadata traceMetadata, List<PurDocItem> items, Long itemId) {
        if (!traceMetadata.hasSource()) {
            return null;
        }
        for (int index = 0; index < items.size(); index++) {
            if (Objects.equals(items.get(index).getId(), itemId) && index < traceMetadata.getSourceItemIds().size()) {
                return traceMetadata.getSourceItemIds().get(index);
            }
        }
        return null;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String generateOrderCode(String docType) {
        String prefix = DOC_TYPE_REQUEST.equals(docType) ? "PR" : "PO";
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }
}
