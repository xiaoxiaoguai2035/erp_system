package com.dongjian.erp.manufacturingerpsystem.modules.sales.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dongjian.erp.manufacturingerpsystem.common.exception.BusinessException;
import com.dongjian.erp.manufacturingerpsystem.common.page.PageResponse;
import com.dongjian.erp.manufacturingerpsystem.common.util.DocSourceTraceCodec;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.entity.BasMaterial;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.entity.BasCustomer;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.mapper.BasMaterialMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.mapper.BasCustomerMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.production.entity.PrdPlan;
import com.dongjian.erp.manufacturingerpsystem.modules.production.mapper.PrdPlanMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.sales.dto.SalesOrderSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.sales.dto.SalesOutStockRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.sales.entity.SalDoc;
import com.dongjian.erp.manufacturingerpsystem.modules.sales.entity.SalDocItem;
import com.dongjian.erp.manufacturingerpsystem.modules.sales.mapper.SalDocMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.sales.mapper.SalDocItemMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.service.InventoryOperationService;
import com.dongjian.erp.manufacturingerpsystem.modules.sales.service.SalesService;
import com.dongjian.erp.manufacturingerpsystem.modules.sales.vo.SalesOrderDetail;
import com.dongjian.erp.manufacturingerpsystem.modules.sales.vo.SalesOrderListItem;
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
public class SalesServiceImpl implements SalesService {
    private static final String DOC_TYPE_QUOTE = "quote";
    private static final String DOC_TYPE_ORDER = "order";

    private final SalDocMapper salDocMapper;
    private final SalDocItemMapper salDocItemMapper;
    private final BasCustomerMapper basCustomerMapper;
    private final BasMaterialMapper basMaterialMapper;
    private final PrdPlanMapper prdPlanMapper;
    private final InventoryOperationService inventoryOperationService;

    public SalesServiceImpl(SalDocMapper salDocMapper,
                            SalDocItemMapper salDocItemMapper,
                            BasCustomerMapper basCustomerMapper,
                            BasMaterialMapper basMaterialMapper,
                            PrdPlanMapper prdPlanMapper,
                            InventoryOperationService inventoryOperationService) {
        this.salDocMapper = salDocMapper;
        this.salDocItemMapper = salDocItemMapper;
        this.basCustomerMapper = basCustomerMapper;
        this.basMaterialMapper = basMaterialMapper;
        this.prdPlanMapper = prdPlanMapper;
        this.inventoryOperationService = inventoryOperationService;
    }

    @Override
    public PageResponse<SalesOrderListItem> pageQuotes(String keyword, String status, long pageNo, long pageSize) {
        return pageDocs(DOC_TYPE_QUOTE, keyword, status, pageNo, pageSize);
    }

    @Override
    public SalesOrderDetail getQuote(Long id) {
        return getDocDetail(id, DOC_TYPE_QUOTE, "销售报价不存在");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createQuote(SalesOrderSaveRequest request) {
        return createDoc(request, DOC_TYPE_QUOTE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQuote(Long id, SalesOrderSaveRequest request) {
        updateDoc(id, request, DOC_TYPE_QUOTE, "销售报价仅草稿状态允许修改", "销售报价不存在");
    }

    @Override
    public void approveQuote(Long id) {
        approveDoc(id, DOC_TYPE_QUOTE, "销售报价不存在", "仅草稿状态的销售报价可以审核");
    }

    @Override
    public PageResponse<SalesOrderListItem> pageOrders(String keyword, String status, long pageNo, long pageSize) {
        return pageDocs(DOC_TYPE_ORDER, keyword, status, pageNo, pageSize);
    }

    @Override
    public SalesOrderDetail getOrder(Long id) {
        return getDocDetail(id, DOC_TYPE_ORDER, "销售订单不存在");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(SalesOrderSaveRequest request) {
        return createDoc(request, DOC_TYPE_ORDER);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(Long id, SalesOrderSaveRequest request) {
        updateDoc(id, request, DOC_TYPE_ORDER, "销售订单仅草稿状态允许修改", "销售订单不存在");
    }

    @Override
    public void approveOrder(Long id) {
        approveDoc(id, DOC_TYPE_ORDER, "销售订单不存在", "仅草稿状态的销售订单可以审核");
    }

    @Override
    public void closeOrder(Long id) {
        closeDoc(id, DOC_TYPE_ORDER, "销售订单不存在", "销售订单已关闭");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long outStock(Long id, SalesOutStockRequest request) {
        SalDoc doc = requireOrder(id);
        if ("draft".equals(doc.getStatus()) || "closed".equals(doc.getStatus())) {
            throw new BusinessException(409, "当前销售订单不允许执行出库");
        }

        List<SalDocItem> sourceItems = salDocItemMapper.selectList(new LambdaQueryWrapper<SalDocItem>()
                .eq(SalDocItem::getDocId, id)
                .orderByAsc(SalDocItem::getId));
        Map<Long, SalDocItem> sourceItemMap = sourceItems.stream()
                .collect(Collectors.toMap(SalDocItem::getId, item -> item, (left, right) -> left));

        List<InventoryOperationService.DocItemParam> docItems = request.getItems().stream()
                .map(itemRequest -> {
                    SalDocItem sourceItem = sourceItemMap.get(itemRequest.getSourceItemId());
                    if (sourceItem == null) {
                        throw new BusinessException(400, "销售出库来源明细不存在");
                    }
                    if (!Objects.equals(sourceItem.getMaterialId(), itemRequest.getMaterialId())) {
                        throw new BusinessException(400, "销售出库物料与来源明细不一致");
                    }

                    BigDecimal remainQty = defaultDecimal(sourceItem.getQty()).subtract(defaultDecimal(sourceItem.getShippedQty()));
                    if (itemRequest.getQty().compareTo(remainQty) > 0) {
                        throw new BusinessException(409, "销售明细出库数量超过剩余可发货数量");
                    }

                    sourceItem.setShippedQty(defaultDecimal(sourceItem.getShippedQty()).add(itemRequest.getQty()));
                    salDocItemMapper.updateById(sourceItem);

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

        Long stockDocId = inventoryOperationService.createOutStockDoc(
                "sales_out",
                "sales_order",
                id,
                request.getWarehouseId(),
                request.getBizDate(),
                request.getRemark(),
                docItems
        );

        syncSalesOrderStatus(doc, sourceItemMap.values().stream().toList());
        return stockDocId;
    }

    private Map<Long, String> buildCustomerMap(Set<Long> customerIds) {
        if (customerIds.isEmpty()) {
            return Map.of();
        }

        return basCustomerMapper.selectBatchIds(customerIds).stream()
                .collect(Collectors.toMap(BasCustomer::getId, BasCustomer::getName, (left, right) -> left));
    }

    private SalDoc requireOrder(Long id) {
        return requireDoc(id, DOC_TYPE_ORDER, "销售订单不存在");
    }

    private SalDoc requireDoc(Long id, String docType, String notFoundMessage) {
        SalDoc doc = salDocMapper.selectById(id);
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

    private List<SalDocItem> saveOrderItems(Long docId, SalesOrderSaveRequest request) {
        List<SalDocItem> savedItems = new java.util.ArrayList<>();
        for (SalesOrderSaveRequest.SalesOrderItemRequest itemRequest : request.getItems()) {
            if (basMaterialMapper.selectById(itemRequest.getMaterialId()) == null) {
                throw new BusinessException(400, "销售明细中的产品不存在");
            }

            SalDocItem item = new SalDocItem();
            item.setDocId(docId);
            item.setMaterialId(itemRequest.getMaterialId());
            item.setQty(itemRequest.getQty());
            item.setShippedQty(BigDecimal.ZERO);
            item.setPrice(itemRequest.getPrice());
            item.setAmount(itemRequest.getQty().multiply(itemRequest.getPrice()));
            salDocItemMapper.insert(item);
            savedItems.add(item);
        }
        return savedItems;
    }

    private String resolveCustomerName(Long customerId) {
        BasCustomer customer = basCustomerMapper.selectById(customerId);
        return customer != null ? customer.getName() : null;
    }

    private PageResponse<SalesOrderListItem> pageDocs(String docType, String keyword, String status, long pageNo, long pageSize) {
        Page<SalDoc> page = salDocMapper.selectPage(
                new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<SalDoc>()
                        .eq(SalDoc::getDocType, docType)
                        .and(StringUtils.hasText(keyword), wrapper -> wrapper.like(SalDoc::getCode, keyword))
                        .eq(StringUtils.hasText(status), SalDoc::getStatus, status)
                        .orderByDesc(SalDoc::getId)
        );

        Map<Long, String> customerNameMap = buildCustomerMap(page.getRecords().stream()
                .map(SalDoc::getCustomerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        return PageResponse.of(
                page.getTotal(),
                pageNo,
                pageSize,
                page.getRecords().stream().map(item -> {
                    DocSourceTraceCodec.TraceMetadata traceMetadata = DocSourceTraceCodec.parse(item.getRemark());
                    SalesOrderListItem result = new SalesOrderListItem();
                    result.setId(item.getId());
                    result.setCode(item.getCode());
                    result.setDocType(item.getDocType());
                    result.setCustomerId(item.getCustomerId());
                    result.setCustomerName(customerNameMap.get(item.getCustomerId()));
                    result.setDocDate(item.getDocDate());
                    result.setDeliveryDate(item.getDeliveryDate());
                    result.setTotalAmount(item.getTotalAmount());
                    result.setStatus(item.getStatus());
                    result.setRemark(traceMetadata.getUserRemark());
                    return result;
                }).toList()
        );
    }

    private SalesOrderDetail getDocDetail(Long id, String docType, String notFoundMessage) {
        SalDoc doc = requireDoc(id, docType, notFoundMessage);
        DocSourceTraceCodec.TraceMetadata traceMetadata = DocSourceTraceCodec.parse(doc.getRemark());
        List<SalDocItem> items = salDocItemMapper.selectList(new LambdaQueryWrapper<SalDocItem>()
                .eq(SalDocItem::getDocId, id)
                .orderByAsc(SalDocItem::getId));

        Set<Long> materialIds = items.stream()
                .map(SalDocItem::getMaterialId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, BasMaterial> materialMap = materialIds.isEmpty()
                ? Map.of()
                : basMaterialMapper.selectBatchIds(materialIds).stream()
                .collect(Collectors.toMap(BasMaterial::getId, item -> item, (left, right) -> left));
        Map<Long, BigDecimal> convertedQtyMap = DOC_TYPE_QUOTE.equals(docType)
                ? loadConvertedQtyBySourceItem(id, null)
                : Map.of();
        Map<Long, BigDecimal> plannedQtyMap = DOC_TYPE_ORDER.equals(docType)
                ? buildPlannedQtyByItem(id, items)
                : Map.of();

        SalesOrderDetail detail = new SalesOrderDetail();
        detail.setId(doc.getId());
        detail.setCode(doc.getCode());
        detail.setDocType(doc.getDocType());
        detail.setCustomerId(doc.getCustomerId());
        detail.setCustomerName(resolveCustomerName(doc.getCustomerId()));
        detail.setDocDate(doc.getDocDate());
        detail.setDeliveryDate(doc.getDeliveryDate());
        detail.setTotalAmount(doc.getTotalAmount());
        detail.setStatus(doc.getStatus());
        detail.setSourceDocId(traceMetadata.getSourceDocId());
        detail.setSourceDocType(traceMetadata.getSourceDocType());
        detail.setRemark(traceMetadata.getUserRemark());
        detail.setItems(items.stream().map(item -> {
            BasMaterial material = materialMap.get(item.getMaterialId());
            SalesOrderDetail.SalesOrderDetailItem result = new SalesOrderDetail.SalesOrderDetailItem();
            result.setId(item.getId());
            result.setMaterialId(item.getMaterialId());
            result.setMaterialCode(material != null ? material.getCode() : null);
            result.setMaterialName(material != null ? material.getName() : null);
            result.setQty(item.getQty());
            result.setShippedQty(item.getShippedQty());
            BigDecimal convertedQty = convertedQtyMap.getOrDefault(item.getId(), BigDecimal.ZERO);
            result.setConvertedQty(convertedQty);
            result.setRemainConvertQty(defaultDecimal(item.getQty()).subtract(convertedQty).max(BigDecimal.ZERO));
            BigDecimal plannedQty = plannedQtyMap.getOrDefault(item.getId(), BigDecimal.ZERO);
            result.setPlannedQty(plannedQty);
            result.setRemainPlanQty(defaultDecimal(item.getQty()).subtract(plannedQty).max(BigDecimal.ZERO));
            result.setPrice(item.getPrice());
            result.setAmount(item.getAmount());
            result.setSourceItemId(resolveSourceItemId(traceMetadata, items, item.getId()));
            return result;
        }).toList());
        return detail;
    }

    private Long createDoc(SalesOrderSaveRequest request, String docType) {
        if (basCustomerMapper.selectById(request.getCustomerId()) == null) {
            throw new BusinessException(400, "客户不存在");
        }
        validateSourceTrace(docType, request, null);

        SalDoc doc = new SalDoc();
        doc.setCode(generateOrderCode(docType));
        doc.setDocType(docType);
        doc.setCustomerId(request.getCustomerId());
        doc.setDocDate(request.getDocDate());
        doc.setDeliveryDate(request.getDeliveryDate());
        doc.setStatus("draft");
        doc.setRemark("");
        doc.setTotalAmount(calculateTotalAmount(request));
        salDocMapper.insert(doc);
        List<SalDocItem> savedItems = saveOrderItems(doc.getId(), request);
        updateDocRemarkWithTrace(doc, request, savedItems);
        return doc.getId();
    }

    private void updateDoc(Long id, SalesOrderSaveRequest request, String docType, String draftMessage, String notFoundMessage) {
        SalDoc doc = requireDoc(id, docType, notFoundMessage);
        ensureDraft(doc.getStatus(), draftMessage);
        if (basCustomerMapper.selectById(request.getCustomerId()) == null) {
            throw new BusinessException(400, "客户不存在");
        }
        validateSourceTrace(docType, request, id);

        doc.setCustomerId(request.getCustomerId());
        doc.setDocDate(request.getDocDate());
        doc.setDeliveryDate(request.getDeliveryDate());
        doc.setTotalAmount(calculateTotalAmount(request));

        salDocItemMapper.delete(new LambdaQueryWrapper<SalDocItem>().eq(SalDocItem::getDocId, id));
        List<SalDocItem> savedItems = saveOrderItems(id, request);
        updateDocRemarkWithTrace(doc, request, savedItems);
    }

    private void approveDoc(Long id, String docType, String notFoundMessage, String draftMessage) {
        SalDoc doc = requireDoc(id, docType, notFoundMessage);
        ensureDraft(doc.getStatus(), draftMessage);
        doc.setStatus("approved");
        salDocMapper.updateById(doc);
    }

    private void closeDoc(Long id, String docType, String notFoundMessage, String closedMessage) {
        SalDoc doc = requireDoc(id, docType, notFoundMessage);
        if ("closed".equals(doc.getStatus())) {
            throw new BusinessException(409, closedMessage);
        }
        doc.setStatus("closed");
        salDocMapper.updateById(doc);
    }

    private void syncSalesOrderStatus(SalDoc doc, List<SalDocItem> items) {
        boolean completed = items.stream().allMatch(item ->
                defaultDecimal(item.getShippedQty()).compareTo(defaultDecimal(item.getQty())) >= 0);
        boolean partial = items.stream().anyMatch(item ->
                defaultDecimal(item.getShippedQty()).compareTo(BigDecimal.ZERO) > 0);

        if (completed) {
            doc.setStatus("completed");
        } else if (partial) {
            doc.setStatus("partial");
        }
        salDocMapper.updateById(doc);
    }

    private BigDecimal calculateTotalAmount(SalesOrderSaveRequest request) {
        return request.getItems().stream()
                .map(item -> item.getQty().multiply(item.getPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void updateDocRemarkWithTrace(SalDoc doc, SalesOrderSaveRequest request, List<SalDocItem> savedItems) {
        String remark = request.getRemark();
        if (DOC_TYPE_ORDER.equals(doc.getDocType()) && request.getSourceDocId() != null) {
            if (savedItems.size() != request.getItems().size()) {
                throw new BusinessException(500, "销售订单来源映射写入失败");
            }
            remark = DocSourceTraceCodec.encode(
                    request.getSourceDocType(),
                    request.getSourceDocId(),
                    request.getItems().stream().map(SalesOrderSaveRequest.SalesOrderItemRequest::getSourceItemId).toList(),
                    request.getRemark()
            );
        }
        if (remark != null && remark.length() > 255) {
            throw new BusinessException(400, "来源追踪信息过长，请减少下推明细或缩短备注");
        }
        doc.setRemark(remark);
        salDocMapper.updateById(doc);
    }

    private void validateSourceTrace(String docType, SalesOrderSaveRequest request, Long currentDocId) {
        boolean hasSourceDoc = request.getSourceDocId() != null || StringUtils.hasText(request.getSourceDocType());
        boolean hasSourceItem = request.getItems().stream().anyMatch(item -> item.getSourceItemId() != null);

        if (!DOC_TYPE_ORDER.equals(docType)) {
            if (hasSourceDoc || hasSourceItem) {
                throw new BusinessException(400, "销售报价不支持写入来源转单关系");
            }
            return;
        }

        if (!hasSourceDoc && !hasSourceItem) {
            return;
        }
        if (request.getSourceDocId() == null || !StringUtils.hasText(request.getSourceDocType())) {
            throw new BusinessException(400, "来源销售报价信息不完整");
        }
        if (!DOC_TYPE_QUOTE.equals(request.getSourceDocType())) {
            throw new BusinessException(400, "销售订单仅支持来源销售报价");
        }

        SalDoc sourceDoc = requireDoc(request.getSourceDocId(), DOC_TYPE_QUOTE, "来源销售报价不存在");
        if (!"approved".equalsIgnoreCase(sourceDoc.getStatus())) {
            throw new BusinessException(409, "仅已审核销售报价允许下推销售订单");
        }

        List<SalDocItem> sourceItems = salDocItemMapper.selectList(new LambdaQueryWrapper<SalDocItem>()
                .eq(SalDocItem::getDocId, sourceDoc.getId())
                .orderByAsc(SalDocItem::getId));
        Map<Long, SalDocItem> sourceItemMap = sourceItems.stream()
                .collect(Collectors.toMap(SalDocItem::getId, item -> item, (left, right) -> left));
        Map<Long, BigDecimal> convertedQtyMap = loadConvertedQtyBySourceItem(sourceDoc.getId(), currentDocId);
        Map<Long, BigDecimal> requestQtyMap = new java.util.LinkedHashMap<>();

        for (SalesOrderSaveRequest.SalesOrderItemRequest itemRequest : request.getItems()) {
            if (itemRequest.getSourceItemId() == null) {
                throw new BusinessException(400, "来源销售报价明细不能为空");
            }
            SalDocItem sourceItem = sourceItemMap.get(itemRequest.getSourceItemId());
            if (sourceItem == null) {
                throw new BusinessException(400, "来源销售报价明细不存在");
            }
            if (!Objects.equals(sourceItem.getMaterialId(), itemRequest.getMaterialId())) {
                throw new BusinessException(400, "销售订单产品与来源报价明细不一致");
            }
            requestQtyMap.merge(itemRequest.getSourceItemId(), defaultDecimal(itemRequest.getQty()), BigDecimal::add);
        }

        for (Map.Entry<Long, BigDecimal> entry : requestQtyMap.entrySet()) {
            SalDocItem sourceItem = sourceItemMap.get(entry.getKey());
            BigDecimal remainQty = defaultDecimal(sourceItem.getQty())
                    .subtract(convertedQtyMap.getOrDefault(entry.getKey(), BigDecimal.ZERO))
                    .max(BigDecimal.ZERO);
            if (entry.getValue().compareTo(remainQty) > 0) {
                BasMaterial material = basMaterialMapper.selectById(sourceItem.getMaterialId());
                String materialName = material != null ? material.getName() : "来源产品";
                throw new BusinessException(409, materialName + " 超过来源报价剩余可转订单数量");
            }
        }
    }

    private Map<Long, BigDecimal> loadConvertedQtyBySourceItem(Long sourceDocId, Long currentDocId) {
        List<SalDoc> orders = salDocMapper.selectList(new LambdaQueryWrapper<SalDoc>()
                .eq(SalDoc::getDocType, DOC_TYPE_ORDER)
                .ne(currentDocId != null, SalDoc::getId, currentDocId)
                .ne(SalDoc::getStatus, "closed")
                .orderByAsc(SalDoc::getId));
        List<SalDoc> linkedOrders = orders.stream()
                .filter(item -> {
                    DocSourceTraceCodec.TraceMetadata metadata = DocSourceTraceCodec.parse(item.getRemark());
                    return DOC_TYPE_QUOTE.equals(metadata.getSourceDocType()) && Objects.equals(metadata.getSourceDocId(), sourceDocId);
                })
                .toList();
        if (linkedOrders.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<SalDocItem>> itemGroup = salDocItemMapper.selectList(new LambdaQueryWrapper<SalDocItem>()
                        .in(SalDocItem::getDocId, linkedOrders.stream().map(SalDoc::getId).toList())
                        .orderByAsc(SalDocItem::getId))
                .stream()
                .collect(Collectors.groupingBy(SalDocItem::getDocId, java.util.LinkedHashMap::new, Collectors.toList()));

        Map<Long, BigDecimal> convertedQtyMap = new java.util.LinkedHashMap<>();
        for (SalDoc order : linkedOrders) {
            DocSourceTraceCodec.TraceMetadata metadata = DocSourceTraceCodec.parse(order.getRemark());
            List<SalDocItem> orderItems = itemGroup.getOrDefault(order.getId(), List.of());
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

    private Long resolveSourceItemId(DocSourceTraceCodec.TraceMetadata traceMetadata, List<SalDocItem> items, Long itemId) {
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

    private Map<Long, BigDecimal> buildPlannedQtyByItem(Long sourceSalesId, List<SalDocItem> items) {
        List<PrdPlan> plans = prdPlanMapper.selectList(new LambdaQueryWrapper<PrdPlan>()
                .eq(PrdPlan::getSourceSalesId, sourceSalesId)
                .ne(PrdPlan::getStatus, "closed")
                .orderByAsc(PrdPlan::getId));
        if (plans.isEmpty()) {
            return Map.of();
        }

        Map<Long, BigDecimal> materialPlannedQtyMap = plans.stream()
                .collect(Collectors.toMap(
                        PrdPlan::getMaterialId,
                        item -> defaultDecimal(item.getPlanQty()),
                        BigDecimal::add,
                        java.util.LinkedHashMap::new
                ));
        Map<Long, BigDecimal> result = new java.util.LinkedHashMap<>();
        for (SalDocItem item : items) {
            BigDecimal remainingPlannedQty = materialPlannedQtyMap.getOrDefault(item.getMaterialId(), BigDecimal.ZERO);
            BigDecimal allocatedQty = defaultDecimal(item.getQty()).min(remainingPlannedQty.max(BigDecimal.ZERO));
            result.put(item.getId(), allocatedQty);
            materialPlannedQtyMap.put(item.getMaterialId(), remainingPlannedQty.subtract(allocatedQty).max(BigDecimal.ZERO));
        }
        return result;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String generateOrderCode(String docType) {
        String prefix = DOC_TYPE_QUOTE.equals(docType) ? "SQ" : "SO";
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }
}
