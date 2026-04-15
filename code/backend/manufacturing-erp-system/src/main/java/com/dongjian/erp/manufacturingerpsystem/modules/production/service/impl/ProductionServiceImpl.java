package com.dongjian.erp.manufacturingerpsystem.modules.production.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dongjian.erp.manufacturingerpsystem.common.exception.BusinessException;
import com.dongjian.erp.manufacturingerpsystem.common.page.PageResponse;
import com.dongjian.erp.manufacturingerpsystem.common.util.DocSourceTraceCodec;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.entity.BasBom;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.entity.BasBomItem;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.entity.BasMaterial;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.entity.BasProcessRoute;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.entity.BasProcessRouteItem;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.mapper.BasBomItemMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.mapper.BasBomMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.mapper.BasMaterialMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.mapper.BasProcessRouteItemMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.mapper.BasProcessRouteMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.entity.StkDoc;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.entity.StkDocItem;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.entity.StkInventory;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.mapper.StkDocItemMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.mapper.StkDocMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.mapper.StkInventoryMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.service.InventoryOperationService;
import com.dongjian.erp.manufacturingerpsystem.modules.production.dto.MrpCalculateRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.production.dto.MrpGenerateRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.production.dto.ProductionPlanSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.production.dto.ReportSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.production.dto.WorkOrderFinishInRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.production.dto.WorkOrderPickRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.production.dto.WorkOrderSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.production.entity.PrdPlan;
import com.dongjian.erp.manufacturingerpsystem.modules.production.entity.PrdReport;
import com.dongjian.erp.manufacturingerpsystem.modules.production.entity.PrdWorkOrder;
import com.dongjian.erp.manufacturingerpsystem.modules.production.mapper.PrdPlanMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.production.mapper.PrdReportMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.production.mapper.PrdWorkOrderMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.production.service.ProductionService;
import com.dongjian.erp.manufacturingerpsystem.modules.production.vo.MrpResult;
import com.dongjian.erp.manufacturingerpsystem.modules.production.vo.ProductionPlanListItem;
import com.dongjian.erp.manufacturingerpsystem.modules.production.vo.ReportListItem;
import com.dongjian.erp.manufacturingerpsystem.modules.production.vo.WorkOrderListItem;
import com.dongjian.erp.manufacturingerpsystem.modules.production.vo.WorkOrderProgress;
import com.dongjian.erp.manufacturingerpsystem.modules.purchase.entity.PurDoc;
import com.dongjian.erp.manufacturingerpsystem.modules.purchase.entity.PurDocItem;
import com.dongjian.erp.manufacturingerpsystem.modules.purchase.mapper.PurDocItemMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.purchase.mapper.PurDocMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.sales.entity.SalDoc;
import com.dongjian.erp.manufacturingerpsystem.modules.sales.entity.SalDocItem;
import com.dongjian.erp.manufacturingerpsystem.modules.sales.mapper.SalDocMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.sales.mapper.SalDocItemMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ProductionServiceImpl implements ProductionService {

    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_APPROVED = "approved";
    private static final String STATUS_IN_PROGRESS = "in_progress";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_CLOSED = "closed";

    private static final String DOC_TYPE_PURCHASE_REQUEST = "request";
    private static final String DOC_TYPE_WORK_ORDER_PICK = "work_order_pick";
    private static final String DOC_TYPE_WORK_ORDER_FINISH_IN = "work_order_finish_in";
    private static final String SOURCE_TYPE_WORK_ORDER_PICK = "work_order_pick";
    private static final String SOURCE_TYPE_WORK_ORDER_FINISH_IN = "work_order_finish_in";
    private static final String ENABLED_STATUS = "enabled";

    private final PrdPlanMapper prdPlanMapper;
    private final PrdWorkOrderMapper prdWorkOrderMapper;
    private final PrdReportMapper prdReportMapper;
    private final BasMaterialMapper basMaterialMapper;
    private final BasBomMapper basBomMapper;
    private final BasBomItemMapper basBomItemMapper;
    private final BasProcessRouteMapper basProcessRouteMapper;
    private final BasProcessRouteItemMapper basProcessRouteItemMapper;
    private final StkInventoryMapper stkInventoryMapper;
    private final StkDocMapper stkDocMapper;
    private final StkDocItemMapper stkDocItemMapper;
    private final PurDocMapper purDocMapper;
    private final PurDocItemMapper purDocItemMapper;
    private final SalDocMapper salDocMapper;
    private final SalDocItemMapper salDocItemMapper;
    private final InventoryOperationService inventoryOperationService;

    private final Map<String, MrpTaskSnapshot> mrpTaskCache = new ConcurrentHashMap<>();

    public ProductionServiceImpl(PrdPlanMapper prdPlanMapper,
                                 PrdWorkOrderMapper prdWorkOrderMapper,
                                 PrdReportMapper prdReportMapper,
                                 BasMaterialMapper basMaterialMapper,
                                 BasBomMapper basBomMapper,
                                 BasBomItemMapper basBomItemMapper,
                                 BasProcessRouteMapper basProcessRouteMapper,
                                 BasProcessRouteItemMapper basProcessRouteItemMapper,
                                 StkInventoryMapper stkInventoryMapper,
                                 StkDocMapper stkDocMapper,
                                 StkDocItemMapper stkDocItemMapper,
                                 PurDocMapper purDocMapper,
                                 PurDocItemMapper purDocItemMapper,
                                 SalDocMapper salDocMapper,
                                 SalDocItemMapper salDocItemMapper,
                                 InventoryOperationService inventoryOperationService) {
        this.prdPlanMapper = prdPlanMapper;
        this.prdWorkOrderMapper = prdWorkOrderMapper;
        this.prdReportMapper = prdReportMapper;
        this.basMaterialMapper = basMaterialMapper;
        this.basBomMapper = basBomMapper;
        this.basBomItemMapper = basBomItemMapper;
        this.basProcessRouteMapper = basProcessRouteMapper;
        this.basProcessRouteItemMapper = basProcessRouteItemMapper;
        this.stkInventoryMapper = stkInventoryMapper;
        this.stkDocMapper = stkDocMapper;
        this.stkDocItemMapper = stkDocItemMapper;
        this.purDocMapper = purDocMapper;
        this.purDocItemMapper = purDocItemMapper;
        this.salDocMapper = salDocMapper;
        this.salDocItemMapper = salDocItemMapper;
        this.inventoryOperationService = inventoryOperationService;
    }

    @Override
    public PageResponse<ProductionPlanListItem> pagePlans(String keyword, String status, long pageNo, long pageSize) {
        Set<Long> matchedMaterialIds = loadMatchedMaterialIds(keyword);
        Page<PrdPlan> page = prdPlanMapper.selectPage(
                new Page<>(pageNo, pageSize),
                buildPlanQuery(keyword, matchedMaterialIds, status)
        );

        Map<Long, BasMaterial> materialMap = loadMaterialMap(page.getRecords().stream()
                .map(PrdPlan::getMaterialId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        return PageResponse.of(
                page.getTotal(),
                pageNo,
                pageSize,
                page.getRecords().stream()
                        .map(item -> toPlanItem(item, materialMap.get(item.getMaterialId())))
                        .toList()
        );
    }

    @Override
    public ProductionPlanListItem getPlan(Long id) {
        PrdPlan plan = requirePlan(id);
        return toPlanItem(plan, requireMaterial(plan.getMaterialId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPlan(ProductionPlanSaveRequest request) {
        validatePlanRequest(request, null);

        PrdPlan entity = new PrdPlan();
        entity.setCode(generatePlanCode());
        entity.setMaterialId(request.getMaterialId());
        entity.setPlanQty(normalizePositiveQty(request.getPlanQty(), "计划数量必须大于0"));
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setSourceSalesId(request.getSourceSalesId());
        entity.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : STATUS_DRAFT);
        prdPlanMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePlan(Long id, ProductionPlanSaveRequest request) {
        PrdPlan entity = requirePlan(id);
        ensureDraft(entity.getStatus(), "生产计划仅草稿状态允许修改");
        validatePlanRequest(request, id);

        entity.setMaterialId(request.getMaterialId());
        entity.setPlanQty(normalizePositiveQty(request.getPlanQty(), "计划数量必须大于0"));
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setSourceSalesId(request.getSourceSalesId());
        if (StringUtils.hasText(request.getStatus())) {
            entity.setStatus(request.getStatus());
        }
        prdPlanMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approvePlan(Long id) {
        PrdPlan entity = requirePlan(id);
        ensureDraft(entity.getStatus(), "仅草稿状态的生产计划可以审核");
        entity.setStatus(STATUS_APPROVED);
        prdPlanMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closePlan(Long id) {
        PrdPlan entity = requirePlan(id);
        if (STATUS_CLOSED.equals(entity.getStatus())) {
            throw new BusinessException(409, "生产计划已关闭");
        }
        if (STATUS_COMPLETED.equals(entity.getStatus())) {
            throw new BusinessException(409, "生产计划已完成，不允许关闭");
        }

        List<PrdWorkOrder> workOrders = prdWorkOrderMapper.selectList(new LambdaQueryWrapper<PrdWorkOrder>()
                .eq(PrdWorkOrder::getPlanId, id));
        boolean hasActiveWorkOrders = workOrders.stream()
                .anyMatch(item -> !STATUS_CLOSED.equals(item.getStatus()) && !STATUS_COMPLETED.equals(item.getStatus()));
        if (hasActiveWorkOrders) {
            throw new BusinessException(409, "计划存在未关闭工单，不允许直接关闭");
        }

        entity.setStatus(STATUS_CLOSED);
        prdPlanMapper.updateById(entity);
    }

    @Override
    public MrpResult calculateMrp(MrpCalculateRequest request) {
        List<PrdPlan> plans = loadPlansForMrp(request.getPlanIds());
        Map<Long, BigDecimal> inventoryRemainMap = loadMrpSupplyMap();
        Set<String> activeWorkOrderKeys = loadActiveWorkOrderKeys();
        MrpAggregation aggregation = new MrpAggregation();
        MrpReferenceContext referenceContext = new MrpReferenceContext();

        for (PrdPlan plan : plans) {
            BasMaterial product = requireMaterial(plan.getMaterialId());
            BasBom productBom = requireActiveBom(plan.getMaterialId(), product.getName() + " 缺少已启用BOM，无法执行MRP");
            BasProcessRoute productRoute = requireActiveRoute(plan.getMaterialId(), product.getName() + " 缺少已启用工艺路线，无法执行MRP");

            addWorkOrderSuggestion(aggregation, plan.getId(), plan.getMaterialId(), productBom.getId(),
                    productRoute.getId(), plan.getPlanQty(), plan.getStartDate(), plan.getEndDate(), activeWorkOrderKeys);
            explodeBomDemand(plan, plan.getMaterialId(), plan.getPlanQty(), inventoryRemainMap, aggregation, referenceContext, activeWorkOrderKeys, new HashSet<>());
        }

        MrpTaskSnapshot snapshot = buildMrpSnapshot(aggregation);
        mrpTaskCache.put(snapshot.getTaskKey(), snapshot);
        return snapshot.toResult(loadMaterialMap(snapshot.collectMaterialIds()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> generatePurchaseByMrp(MrpGenerateRequest request) {
        MrpTaskSnapshot snapshot = requireMrpTask(request.getTaskKey());
        List<PurchaseSuggestionPayload> suggestions = request.getSelectedItems().stream()
                .map(snapshot.getPurchaseSuggestions()::get)
                .filter(Objects::nonNull)
                .toList();
        if (suggestions.isEmpty()) {
            throw new BusinessException(400, "未找到可生成的采购建议");
        }

        LocalDate today = LocalDate.now();
        LocalDate expectedDate = suggestions.stream()
                .map(PurchaseSuggestionPayload::getNeedDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(today);

        PurDoc doc = new PurDoc();
        doc.setCode(generatePurchaseRequestCode());
        doc.setDocType(DOC_TYPE_PURCHASE_REQUEST);
        doc.setSupplierId(null);
        doc.setDocDate(today);
        doc.setExpectedDate(expectedDate);
        doc.setStatus(STATUS_DRAFT);
        doc.setRemark("MRP任务 " + request.getTaskKey() + " 生成");
        doc.setTotalAmount(BigDecimal.ZERO);
        purDocMapper.insert(doc);

        for (PurchaseSuggestionPayload suggestion : suggestions) {
            PurDocItem item = new PurDocItem();
            item.setDocId(doc.getId());
            item.setMaterialId(suggestion.getMaterialId());
            item.setWarehouseId(suggestion.getWarehouseId());
            item.setQty(suggestion.getShortageQty());
            item.setReceivedQty(BigDecimal.ZERO);
            item.setQualifiedQty(BigDecimal.ZERO);
            item.setPrice(BigDecimal.ZERO);
            item.setAmount(BigDecimal.ZERO);
            item.setNeedDate(suggestion.getNeedDate());
            purDocItemMapper.insert(item);
        }

        mrpTaskCache.remove(request.getTaskKey());
        return List.of(doc.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> generateWorkOrdersByMrp(MrpGenerateRequest request) {
        MrpTaskSnapshot snapshot = requireMrpTask(request.getTaskKey());
        List<WorkOrderSuggestionPayload> suggestions = request.getSelectedItems().stream()
                .map(snapshot.getWorkOrderSuggestions()::get)
                .filter(Objects::nonNull)
                .toList();
        if (suggestions.isEmpty()) {
            throw new BusinessException(400, "未找到可生成的工单建议");
        }

        List<Long> workOrderIds = new ArrayList<>();
        for (WorkOrderSuggestionPayload suggestion : suggestions) {
            ensureNoActiveWorkOrder(suggestion);

            PrdWorkOrder entity = new PrdWorkOrder();
            entity.setCode(generateWorkOrderCode());
            entity.setPlanId(suggestion.getPlanId());
            entity.setMaterialId(suggestion.getMaterialId());
            entity.setBomId(suggestion.getBomId());
            entity.setRouteId(suggestion.getRouteId());
            entity.setPlanQty(suggestion.getPlanQty());
            entity.setFinishedQty(BigDecimal.ZERO);
            entity.setStatus(STATUS_DRAFT);
            entity.setStartDate(suggestion.getStartDate());
            entity.setEndDate(suggestion.getEndDate());
            prdWorkOrderMapper.insert(entity);
            workOrderIds.add(entity.getId());
        }

        for (WorkOrderSuggestionPayload suggestion : suggestions) {
            syncPlanStatus(suggestion.getPlanId());
        }
        mrpTaskCache.remove(request.getTaskKey());
        return workOrderIds;
    }

    @Override
    public PageResponse<WorkOrderListItem> pageWorkOrders(String keyword, String status, long pageNo, long pageSize) {
        Set<Long> matchedMaterialIds = loadMatchedMaterialIds(keyword);
        Page<PrdWorkOrder> page = prdWorkOrderMapper.selectPage(
                new Page<>(pageNo, pageSize),
                buildWorkOrderQuery(keyword, matchedMaterialIds, status)
        );

        Map<Long, BasMaterial> materialMap = loadMaterialMap(page.getRecords().stream()
                .map(PrdWorkOrder::getMaterialId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        return PageResponse.of(
                page.getTotal(),
                pageNo,
                pageSize,
                page.getRecords().stream()
                        .map(item -> toWorkOrderItem(item, materialMap.get(item.getMaterialId())))
                        .toList()
        );
    }

    @Override
    public WorkOrderListItem getWorkOrder(Long id) {
        PrdWorkOrder workOrder = requireWorkOrder(id);
        return toWorkOrderItem(workOrder, requireMaterial(workOrder.getMaterialId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWorkOrder(WorkOrderSaveRequest request) {
        validateWorkOrderRequest(request);

        PrdWorkOrder entity = new PrdWorkOrder();
        entity.setCode(generateWorkOrderCode());
        entity.setPlanId(request.getPlanId());
        entity.setMaterialId(request.getMaterialId());
        entity.setBomId(request.getBomId());
        entity.setRouteId(request.getRouteId());
        entity.setPlanQty(normalizePositiveQty(request.getPlanQty(), "计划数量必须大于0"));
        entity.setFinishedQty(BigDecimal.ZERO);
        entity.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : STATUS_DRAFT);
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        prdWorkOrderMapper.insert(entity);

        syncPlanStatus(entity.getPlanId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWorkOrder(Long id, WorkOrderSaveRequest request) {
        PrdWorkOrder entity = requireWorkOrder(id);
        ensureDraft(entity.getStatus(), "生产工单仅草稿状态允许修改");
        validateWorkOrderRequest(request);

        entity.setPlanId(request.getPlanId());
        entity.setMaterialId(request.getMaterialId());
        entity.setBomId(request.getBomId());
        entity.setRouteId(request.getRouteId());
        entity.setPlanQty(normalizePositiveQty(request.getPlanQty(), "计划数量必须大于0"));
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        if (StringUtils.hasText(request.getStatus())) {
            entity.setStatus(request.getStatus());
        }
        prdWorkOrderMapper.updateById(entity);

        syncPlanStatus(entity.getPlanId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveWorkOrder(Long id) {
        PrdWorkOrder entity = requireWorkOrder(id);
        ensureDraft(entity.getStatus(), "仅草稿状态的工单可以审核");
        entity.setStatus(STATUS_APPROVED);
        prdWorkOrderMapper.updateById(entity);

        syncPlanStatus(entity.getPlanId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeWorkOrder(Long id) {
        PrdWorkOrder entity = requireWorkOrder(id);
        if (STATUS_CLOSED.equals(entity.getStatus())) {
            throw new BusinessException(409, "工单已关闭");
        }
        entity.setStatus(STATUS_CLOSED);
        prdWorkOrderMapper.updateById(entity);

        syncPlanStatus(entity.getPlanId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long pickMaterials(Long id, WorkOrderPickRequest request) {
        PrdWorkOrder workOrder = requireExecutableWorkOrder(id, "当前工单状态不允许领料");
        List<BasBomItem> bomItems = requireBomItems(workOrder.getBomId());
        Map<Long, List<BasBomItem>> bomItemsByMaterial = bomItems.stream()
                .collect(Collectors.groupingBy(BasBomItem::getMaterialId));
        Map<Long, BigDecimal> requiredQtyMap = buildRequiredMaterialQtyMap(bomItems, workOrder.getPlanQty());
        Map<Long, BigDecimal> pickedQtyMap = loadStockQtyByMaterial(SOURCE_TYPE_WORK_ORDER_PICK, id);
        Map<Long, BigDecimal> requestQtyMap = aggregateMaterialQty(request.getItems().stream()
                .collect(Collectors.toMap(
                        WorkOrderPickRequest.PickItem::getMaterialId,
                        WorkOrderPickRequest.PickItem::getQty,
                        BigDecimal::add,
                        LinkedHashMap::new
                )));

        for (Map.Entry<Long, BigDecimal> entry : requestQtyMap.entrySet()) {
            if (!bomItemsByMaterial.containsKey(entry.getKey())) {
                throw new BusinessException(400, "领料物料不在工单BOM范围内");
            }
            BigDecimal requiredQty = requiredQtyMap.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            BigDecimal pickedQty = pickedQtyMap.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            BigDecimal remainQty = requiredQty.subtract(pickedQty);
            if (entry.getValue().compareTo(remainQty) > 0) {
                BasMaterial material = requireMaterial(entry.getKey());
                throw new BusinessException(409, material.getName() + " 领料数量超过剩余应发数量");
            }
        }

        List<InventoryOperationService.DocItemParam> docItems = request.getItems().stream()
                .map(item -> {
                    InventoryOperationService.DocItemParam docItem = new InventoryOperationService.DocItemParam();
                    docItem.setMaterialId(item.getMaterialId());
                    docItem.setLotNo(item.getLotNo());
                    docItem.setQty(item.getQty());
                    docItem.setUnitPrice(BigDecimal.ZERO);
                    docItem.setAmount(BigDecimal.ZERO);
                    docItem.setSourceItemId(bomItemsByMaterial.get(item.getMaterialId()).get(0).getId());
                    return docItem;
                })
                .toList();

        Long stockDocId = inventoryOperationService.createOutStockDoc(
                DOC_TYPE_WORK_ORDER_PICK,
                SOURCE_TYPE_WORK_ORDER_PICK,
                id,
                request.getWarehouseId(),
                request.getBizDate(),
                request.getRemark(),
                docItems
        );

        markWorkOrderInProgress(workOrder, request.getBizDate());
        syncPlanStatus(workOrder.getPlanId());
        return stockDocId;
    }

    @Override
    public PageResponse<ReportListItem> pageReports(Long workOrderId, long pageNo, long pageSize) {
        Page<PrdReport> page = prdReportMapper.selectPage(
                new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<PrdReport>()
                        .eq(workOrderId != null, PrdReport::getWorkOrderId, workOrderId)
                        .orderByDesc(PrdReport::getReportDate)
                        .orderByDesc(PrdReport::getId)
        );

        Set<Long> workOrderIds = page.getRecords().stream()
                .map(PrdReport::getWorkOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> processItemIds = page.getRecords().stream()
                .map(PrdReport::getProcessItemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, PrdWorkOrder> workOrderMap = loadWorkOrderMap(workOrderIds);
        Map<Long, BasProcessRouteItem> processItemMap = loadProcessItemMap(processItemIds);

        return PageResponse.of(
                page.getTotal(),
                pageNo,
                pageSize,
                page.getRecords().stream().map(item -> {
                    ReportListItem result = new ReportListItem();
                    PrdWorkOrder workOrder = workOrderMap.get(item.getWorkOrderId());
                    BasProcessRouteItem processItem = processItemMap.get(item.getProcessItemId());
                    result.setId(item.getId());
                    result.setWorkOrderId(item.getWorkOrderId());
                    result.setWorkOrderCode(workOrder != null ? workOrder.getCode() : null);
                    result.setProcessItemId(item.getProcessItemId());
                    result.setProcessName(processItem != null ? processItem.getProcessName() : null);
                    result.setReportDate(item.getReportDate());
                    result.setReportQty(item.getReportQty());
                    result.setQualifiedQty(item.getQualifiedQty());
                    result.setDefectiveQty(item.getDefectiveQty());
                    result.setReporterName(item.getReporterName());
                    result.setRemark(item.getRemark());
                    return result;
                }).toList()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReport(ReportSaveRequest request) {
        PrdWorkOrder workOrder = requireExecutableWorkOrder(request.getWorkOrderId(), "当前工单状态不允许报工");
        validateReportRequest(workOrder, request);

        PrdReport entity = new PrdReport();
        entity.setWorkOrderId(request.getWorkOrderId());
        entity.setProcessItemId(request.getProcessItemId());
        entity.setReportDate(request.getReportDate());
        entity.setReportQty(request.getReportQty());
        entity.setQualifiedQty(request.getQualifiedQty());
        entity.setDefectiveQty(request.getDefectiveQty());
        entity.setReporterName(request.getReporterName());
        entity.setRemark(request.getRemark());
        prdReportMapper.insert(entity);

        markWorkOrderInProgress(workOrder, request.getReportDate().toLocalDate());
        syncPlanStatus(workOrder.getPlanId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long finishIn(Long id, WorkOrderFinishInRequest request) {
        PrdWorkOrder workOrder = requireExecutableWorkOrder(id, "当前工单状态不允许完工入库");
        validateFinishInRequest(workOrder, request);

        BigDecimal finishQty = request.getItems().stream()
                .map(WorkOrderFinishInRequest.FinishItem::getQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        WorkOrderReportSummary reportSummary = summarizeReports(id);
        BigDecimal alreadyFinishedQty = defaultDecimal(workOrder.getFinishedQty());
        BigDecimal availableFinishQty = reportSummary.getQualifiedQty().subtract(alreadyFinishedQty);
        BigDecimal remainPlanQty = defaultDecimal(workOrder.getPlanQty()).subtract(alreadyFinishedQty);
        if (finishQty.compareTo(availableFinishQty) > 0) {
            throw new BusinessException(409, "完工入库数量超过已报工合格数量");
        }
        if (finishQty.compareTo(remainPlanQty) > 0) {
            throw new BusinessException(409, "完工入库数量超过工单剩余计划数量");
        }

        List<InventoryOperationService.DocItemParam> docItems = request.getItems().stream()
                .map(item -> {
                    InventoryOperationService.DocItemParam docItem = new InventoryOperationService.DocItemParam();
                    docItem.setMaterialId(item.getMaterialId());
                    docItem.setLotNo(item.getLotNo());
                    docItem.setQty(item.getQty());
                    docItem.setUnitPrice(BigDecimal.ZERO);
                    docItem.setAmount(BigDecimal.ZERO);
                    docItem.setSourceItemId(workOrder.getId());
                    return docItem;
                })
                .toList();

        Long stockDocId = inventoryOperationService.createInStockDoc(
                DOC_TYPE_WORK_ORDER_FINISH_IN,
                SOURCE_TYPE_WORK_ORDER_FINISH_IN,
                id,
                request.getWarehouseId(),
                request.getBizDate(),
                request.getRemark(),
                docItems
        );

        workOrder.setFinishedQty(alreadyFinishedQty.add(finishQty));
        if (workOrder.getFinishedQty().compareTo(defaultDecimal(workOrder.getPlanQty())) >= 0) {
            workOrder.setStatus(STATUS_COMPLETED);
            workOrder.setEndDate(request.getBizDate());
        } else {
            markWorkOrderInProgress(workOrder, request.getBizDate());
        }
        prdWorkOrderMapper.updateById(workOrder);

        syncPlanStatus(workOrder.getPlanId());
        return stockDocId;
    }

    @Override
    public WorkOrderProgress getProgress(Long id) {
        PrdWorkOrder workOrder = requireWorkOrder(id);
        List<PrdReport> reports = loadReportsByWorkOrder(id);
        List<BasProcessRouteItem> routeItems = loadRouteItems(workOrder.getRouteId());
        List<BasBomItem> bomItems = loadBomItems(workOrder.getBomId());

        WorkOrderReportSummary reportSummary = summarizeReports(reports);
        Map<Long, BigDecimal> pickedQtyMap = loadStockQtyByMaterial(SOURCE_TYPE_WORK_ORDER_PICK, id);
        Map<Long, BigDecimal> requiredQtyMap = buildRequiredMaterialQtyMap(bomItems, workOrder.getPlanQty());

        Set<Long> materialIds = new HashSet<>(requiredQtyMap.keySet());
        materialIds.add(workOrder.getMaterialId());
        Map<Long, BasMaterial> materialMap = loadMaterialMap(materialIds);

        WorkOrderProgress progress = new WorkOrderProgress();
        progress.setWorkOrderId(workOrder.getId());
        progress.setWorkOrderCode(workOrder.getCode());
        progress.setPlanQty(defaultDecimal(workOrder.getPlanQty()));
        progress.setReportedQty(reportSummary.getReportQty());
        progress.setQualifiedQty(reportSummary.getQualifiedQty());
        progress.setDefectiveQty(reportSummary.getDefectiveQty());
        progress.setFinishInQty(defaultDecimal(workOrder.getFinishedQty()));
        progress.setCompletionRate(calculateRate(defaultDecimal(workOrder.getFinishedQty()), defaultDecimal(workOrder.getPlanQty())));
        progress.setMaterials(buildMaterialProgress(requiredQtyMap, pickedQtyMap, materialMap));
        progress.setProcesses(buildProcessProgress(routeItems, reports, defaultDecimal(workOrder.getPlanQty())));
        return progress;
    }

    private LambdaQueryWrapper<PrdPlan> buildPlanQuery(String keyword, Set<Long> matchedMaterialIds, String status) {
        LambdaQueryWrapper<PrdPlan> queryWrapper = new LambdaQueryWrapper<PrdPlan>()
                .eq(StringUtils.hasText(status), PrdPlan::getStatus, status)
                .orderByDesc(PrdPlan::getId);
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> {
                wrapper.like(PrdPlan::getCode, keyword);
                if (matchedMaterialIds != null && !matchedMaterialIds.isEmpty()) {
                    wrapper.or().in(PrdPlan::getMaterialId, matchedMaterialIds);
                }
            });
        }
        return queryWrapper;
    }

    private LambdaQueryWrapper<PrdWorkOrder> buildWorkOrderQuery(String keyword, Set<Long> matchedMaterialIds, String status) {
        LambdaQueryWrapper<PrdWorkOrder> queryWrapper = new LambdaQueryWrapper<PrdWorkOrder>()
                .eq(StringUtils.hasText(status), PrdWorkOrder::getStatus, status)
                .orderByDesc(PrdWorkOrder::getId);
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> {
                wrapper.like(PrdWorkOrder::getCode, keyword);
                if (matchedMaterialIds != null && !matchedMaterialIds.isEmpty()) {
                    wrapper.or().in(PrdWorkOrder::getMaterialId, matchedMaterialIds);
                }
            });
        }
        return queryWrapper;
    }

    private void validatePlanRequest(ProductionPlanSaveRequest request, Long currentPlanId) {
        requireMaterial(request.getMaterialId());
        normalizePositiveQty(request.getPlanQty(), "计划数量必须大于0");
        validateDateRange(request.getStartDate(), request.getEndDate(), "计划完工日期不能早于开工日期");
        validateSourceSales(request, currentPlanId);
    }

    private void validateWorkOrderRequest(WorkOrderSaveRequest request) {
        PrdPlan plan = requirePlan(request.getPlanId());
        requireMaterial(request.getMaterialId());
        BasBom bom = requireBom(request.getBomId());
        BasProcessRoute route = requireRoute(request.getRouteId());

        if (!Objects.equals(bom.getProductId(), request.getMaterialId())) {
            throw new BusinessException(400, "工单产品与BOM产品不一致");
        }
        if (!Objects.equals(route.getProductId(), request.getMaterialId())) {
            throw new BusinessException(400, "工单产品与工艺路线产品不一致");
        }
        if (!StringUtils.hasText(bom.getStatus()) || !ENABLED_STATUS.equals(bom.getStatus())) {
            throw new BusinessException(400, "工单使用的BOM未启用");
        }
        if (!StringUtils.hasText(route.getStatus()) || !ENABLED_STATUS.equals(route.getStatus())) {
            throw new BusinessException(400, "工单使用的工艺路线未启用");
        }
        normalizePositiveQty(request.getPlanQty(), "计划数量必须大于0");
        if (request.getStartDate() != null && request.getEndDate() != null) {
            validateDateRange(request.getStartDate(), request.getEndDate(), "工单完工日期不能早于开工日期");
        }
        requireBomItems(request.getBomId());
        requireRouteItems(request.getRouteId());
        if (STATUS_CLOSED.equals(plan.getStatus())) {
            throw new BusinessException(409, "来源计划已关闭，不允许创建工单");
        }
    }

    private void validateReportRequest(PrdWorkOrder workOrder, ReportSaveRequest request) {
        if (request.getQualifiedQty().add(request.getDefectiveQty()).compareTo(request.getReportQty()) != 0) {
            throw new BusinessException(400, "合格数量与不良数量之和必须等于报工数量");
        }
        if (request.getReportQty().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "报工数量必须大于0");
        }

        BasProcessRouteItem processItem = requireProcessItem(request.getProcessItemId());
        if (!Objects.equals(processItem.getRouteId(), workOrder.getRouteId())) {
            throw new BusinessException(400, "报工工序不属于当前工单工艺路线");
        }

        BigDecimal alreadyReportedQty = loadReportQty(workOrder.getId(), request.getProcessItemId());
        BigDecimal totalReportedQty = alreadyReportedQty.add(request.getReportQty());
        if (totalReportedQty.compareTo(defaultDecimal(workOrder.getPlanQty())) > 0) {
            throw new BusinessException(409, "累计报工数量超过工单计划数量");
        }
    }

    private void validateFinishInRequest(PrdWorkOrder workOrder, WorkOrderFinishInRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException(400, "完工明细不能为空");
        }
        for (WorkOrderFinishInRequest.FinishItem item : request.getItems()) {
            if (!Objects.equals(item.getMaterialId(), workOrder.getMaterialId())) {
                throw new BusinessException(400, "完工入库物料必须与工单产品一致");
            }
            if (item.getQty() == null || item.getQty().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(400, "完工数量必须大于0");
            }
        }
    }

    private List<PrdPlan> loadPlansForMrp(List<Long> planIds) {
        List<Long> uniquePlanIds = planIds.stream().distinct().toList();
        List<PrdPlan> plans = prdPlanMapper.selectBatchIds(uniquePlanIds);
        if (plans.size() != uniquePlanIds.size()) {
            throw new BusinessException(404, "存在未找到的生产计划");
        }
        for (PrdPlan plan : plans) {
            if (!(STATUS_APPROVED.equals(plan.getStatus()) || STATUS_IN_PROGRESS.equals(plan.getStatus()))) {
                throw new BusinessException(409, "MRP仅支持已审核或执行中的生产计划");
            }
        }
        return plans.stream()
                .sorted(Comparator.comparing(PrdPlan::getStartDate).thenComparing(PrdPlan::getId))
                .toList();
    }

    private void explodeBomDemand(PrdPlan plan,
                                  Long parentMaterialId,
                                  BigDecimal demandQty,
                                  Map<Long, BigDecimal> inventoryRemainMap,
                                  MrpAggregation aggregation,
                                  MrpReferenceContext referenceContext,
                                  Set<String> activeWorkOrderKeys,
                                  Set<Long> path) {
        if (!path.add(parentMaterialId)) {
            throw new BusinessException(409, "BOM存在循环引用，无法执行MRP");
        }

        BasBom bom = referenceContext.getActiveBom(parentMaterialId);
        if (bom == null) {
            path.remove(parentMaterialId);
            return;
        }

        List<BasBomItem> bomItems = referenceContext.getBomItems(bom.getId());
        for (BasBomItem bomItem : bomItems) {
            BigDecimal grossRequiredQty = calculateRequiredQty(demandQty, bomItem.getQty(), bomItem.getLossRate());
            BigDecimal currentAvailableQty = inventoryRemainMap.getOrDefault(bomItem.getMaterialId(), BigDecimal.ZERO);
            BigDecimal usedInventoryQty = grossRequiredQty.min(currentAvailableQty.max(BigDecimal.ZERO));
            BigDecimal shortageQty = grossRequiredQty.subtract(usedInventoryQty);
            inventoryRemainMap.put(bomItem.getMaterialId(), currentAvailableQty.subtract(usedInventoryQty).max(BigDecimal.ZERO));

            if (shortageQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BasMaterial material = requireMaterial(bomItem.getMaterialId());
            BasBom childBom = referenceContext.getActiveBom(bomItem.getMaterialId());
            BasProcessRoute childRoute = referenceContext.getActiveRoute(bomItem.getMaterialId());

            if (childBom != null && childRoute != null) {
                addWorkOrderSuggestion(aggregation, plan.getId(), material.getId(), childBom.getId(),
                        childRoute.getId(), shortageQty, plan.getStartDate(), plan.getEndDate(), activeWorkOrderKeys);
                explodeBomDemand(plan, material.getId(), shortageQty, inventoryRemainMap, aggregation, referenceContext, activeWorkOrderKeys, path);
            } else {
                addPurchaseSuggestion(aggregation, plan.getId(), material.getId(), material.getDefaultWarehouseId(),
                        grossRequiredQty, shortageQty, plan.getStartDate());
            }
        }

        path.remove(parentMaterialId);
    }

    private MrpTaskSnapshot buildMrpSnapshot(MrpAggregation aggregation) {
        String taskKey = "mrp_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        Map<Long, PurchaseSuggestionPayload> purchaseSuggestions = new LinkedHashMap<>();
        Map<Long, WorkOrderSuggestionPayload> workOrderSuggestions = new LinkedHashMap<>();

        long suggestionId = 1L;
        for (PurchaseSuggestionAggregate aggregate : aggregation.getPurchaseSuggestions().values()) {
            PurchaseSuggestionPayload payload = new PurchaseSuggestionPayload();
            payload.setSuggestionId(suggestionId);
            payload.setPlanId(aggregate.getPlanId());
            payload.setMaterialId(aggregate.getMaterialId());
            payload.setWarehouseId(aggregate.getWarehouseId());
            payload.setRequiredQty(aggregate.getRequiredQty());
            payload.setShortageQty(aggregate.getShortageQty());
            payload.setNeedDate(aggregate.getNeedDate());
            purchaseSuggestions.put(suggestionId, payload);
            suggestionId++;
        }

        for (WorkOrderSuggestionAggregate aggregate : aggregation.getWorkOrderSuggestions().values()) {
            WorkOrderSuggestionPayload payload = new WorkOrderSuggestionPayload();
            payload.setSuggestionId(suggestionId);
            payload.setPlanId(aggregate.getPlanId());
            payload.setMaterialId(aggregate.getMaterialId());
            payload.setBomId(aggregate.getBomId());
            payload.setRouteId(aggregate.getRouteId());
            payload.setPlanQty(aggregate.getPlanQty());
            payload.setStartDate(aggregate.getStartDate());
            payload.setEndDate(aggregate.getEndDate());
            workOrderSuggestions.put(suggestionId, payload);
            suggestionId++;
        }

        return new MrpTaskSnapshot(taskKey, purchaseSuggestions, workOrderSuggestions);
    }

    private void addPurchaseSuggestion(MrpAggregation aggregation,
                                       Long planId,
                                       Long materialId,
                                       Long warehouseId,
                                       BigDecimal requiredQty,
                                       BigDecimal shortageQty,
                                       LocalDate needDate) {
        String key = planId + "::" + materialId + "::" + warehouseId + "::" + needDate;
        PurchaseSuggestionAggregate aggregate = aggregation.getPurchaseSuggestions()
                .computeIfAbsent(key, ignored -> new PurchaseSuggestionAggregate(planId, materialId, warehouseId, needDate));
        aggregate.setRequiredQty(aggregate.getRequiredQty().add(requiredQty));
        aggregate.setShortageQty(aggregate.getShortageQty().add(shortageQty));
    }

    private void addWorkOrderSuggestion(MrpAggregation aggregation,
                                        Long planId,
                                        Long materialId,
                                        Long bomId,
                                        Long routeId,
                                        BigDecimal planQty,
                                        LocalDate startDate,
                                        LocalDate endDate,
                                        Set<String> activeWorkOrderKeys) {
        if (activeWorkOrderKeys.contains(buildWorkOrderTraceKey(planId, materialId, bomId, routeId))) {
            return;
        }
        String key = planId + "::" + materialId + "::" + bomId + "::" + routeId + "::" + startDate + "::" + endDate;
        WorkOrderSuggestionAggregate aggregate = aggregation.getWorkOrderSuggestions()
                .computeIfAbsent(key, ignored -> new WorkOrderSuggestionAggregate(planId, materialId, bomId, routeId, startDate, endDate));
        aggregate.setPlanQty(aggregate.getPlanQty().add(planQty));
    }

    private void ensureNoActiveWorkOrder(WorkOrderSuggestionPayload suggestion) {
        PrdWorkOrder existing = prdWorkOrderMapper.selectOne(new LambdaQueryWrapper<PrdWorkOrder>()
                .eq(PrdWorkOrder::getPlanId, suggestion.getPlanId())
                .eq(PrdWorkOrder::getMaterialId, suggestion.getMaterialId())
                .eq(PrdWorkOrder::getBomId, suggestion.getBomId())
                .eq(PrdWorkOrder::getRouteId, suggestion.getRouteId())
                .ne(PrdWorkOrder::getStatus, STATUS_CLOSED)
                .last("limit 1"));
        if (existing != null) {
            throw new BusinessException(409, "存在已生成的有效工单，请勿重复转单");
        }
    }

    private PrdPlan requirePlan(Long id) {
        PrdPlan entity = prdPlanMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "生产计划不存在");
        }
        return entity;
    }

    private PrdWorkOrder requireWorkOrder(Long id) {
        PrdWorkOrder entity = prdWorkOrderMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "生产工单不存在");
        }
        return entity;
    }

    private PrdWorkOrder requireExecutableWorkOrder(Long id, String message) {
        PrdWorkOrder workOrder = requireWorkOrder(id);
        if (STATUS_DRAFT.equals(workOrder.getStatus()) || STATUS_CLOSED.equals(workOrder.getStatus()) || STATUS_COMPLETED.equals(workOrder.getStatus())) {
            throw new BusinessException(409, message);
        }
        return workOrder;
    }

    private BasMaterial requireMaterial(Long materialId) {
        BasMaterial material = basMaterialMapper.selectById(materialId);
        if (material == null) {
            throw new BusinessException(404, "物料不存在");
        }
        return material;
    }

    private BasBom requireBom(Long bomId) {
        BasBom bom = basBomMapper.selectById(bomId);
        if (bom == null) {
            throw new BusinessException(404, "BOM不存在");
        }
        return bom;
    }

    private BasProcessRoute requireRoute(Long routeId) {
        BasProcessRoute route = basProcessRouteMapper.selectById(routeId);
        if (route == null) {
            throw new BusinessException(404, "工艺路线不存在");
        }
        return route;
    }

    private BasBom requireActiveBom(Long materialId, String message) {
        BasBom bom = findActiveBom(materialId);
        if (bom == null) {
            throw new BusinessException(400, message);
        }
        return bom;
    }

    private BasProcessRoute requireActiveRoute(Long materialId, String message) {
        BasProcessRoute route = findActiveRoute(materialId);
        if (route == null) {
            throw new BusinessException(400, message);
        }
        return route;
    }

    private BasBom findActiveBom(Long materialId) {
        return basBomMapper.selectOne(new LambdaQueryWrapper<BasBom>()
                .eq(BasBom::getProductId, materialId)
                .eq(BasBom::getStatus, ENABLED_STATUS)
                .orderByDesc(BasBom::getEffectiveDate)
                .orderByDesc(BasBom::getId)
                .last("limit 1"));
    }

    private BasProcessRoute findActiveRoute(Long materialId) {
        return basProcessRouteMapper.selectOne(new LambdaQueryWrapper<BasProcessRoute>()
                .eq(BasProcessRoute::getProductId, materialId)
                .eq(BasProcessRoute::getStatus, ENABLED_STATUS)
                .orderByDesc(BasProcessRoute::getId)
                .last("limit 1"));
    }

    private BasProcessRouteItem requireProcessItem(Long processItemId) {
        BasProcessRouteItem processItem = basProcessRouteItemMapper.selectById(processItemId);
        if (processItem == null) {
            throw new BusinessException(404, "工序不存在");
        }
        return processItem;
    }

    private List<BasBomItem> requireBomItems(Long bomId) {
        List<BasBomItem> items = loadBomItems(bomId);
        if (items.isEmpty()) {
            throw new BusinessException(400, "BOM缺少明细，无法生成工单");
        }
        return items;
    }

    private List<BasProcessRouteItem> requireRouteItems(Long routeId) {
        List<BasProcessRouteItem> items = loadRouteItems(routeId);
        if (items.isEmpty()) {
            throw new BusinessException(400, "工艺路线缺少工序，无法生成工单");
        }
        return items;
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

    private List<PrdReport> loadReportsByWorkOrder(Long workOrderId) {
        return prdReportMapper.selectList(new LambdaQueryWrapper<PrdReport>()
                .eq(PrdReport::getWorkOrderId, workOrderId)
                .orderByAsc(PrdReport::getReportDate)
                .orderByAsc(PrdReport::getId));
    }

    private Map<Long, BasMaterial> loadMaterialMap(Collection<Long> materialIds) {
        Set<Long> ids = materialIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return basMaterialMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(BasMaterial::getId, item -> item, (left, right) -> left));
    }

    private Map<Long, PrdWorkOrder> loadWorkOrderMap(Set<Long> workOrderIds) {
        if (workOrderIds.isEmpty()) {
            return Map.of();
        }
        return prdWorkOrderMapper.selectBatchIds(workOrderIds).stream()
                .collect(Collectors.toMap(PrdWorkOrder::getId, item -> item, (left, right) -> left));
    }

    private Map<Long, BasProcessRouteItem> loadProcessItemMap(Set<Long> processItemIds) {
        if (processItemIds.isEmpty()) {
            return Map.of();
        }
        return basProcessRouteItemMapper.selectBatchIds(processItemIds).stream()
                .collect(Collectors.toMap(BasProcessRouteItem::getId, item -> item, (left, right) -> left));
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

    private void validateSourceSales(ProductionPlanSaveRequest request, Long currentPlanId) {
        if (request.getSourceSalesId() == null) {
            return;
        }
        SalDoc salesDoc = salDocMapper.selectById(request.getSourceSalesId());
        if (salesDoc == null || !"order".equals(salesDoc.getDocType())) {
            throw new BusinessException(400, "来源销售订单不存在");
        }

        if ("draft".equalsIgnoreCase(salesDoc.getStatus()) || "closed".equalsIgnoreCase(salesDoc.getStatus())) {
            throw new BusinessException(409, "当前销售订单状态不允许继续下推生产计划");
        }

        List<SalDocItem> salesItems = salDocItemMapper.selectList(new LambdaQueryWrapper<SalDocItem>()
                .eq(SalDocItem::getDocId, salesDoc.getId())
                .orderByAsc(SalDocItem::getId));
        BigDecimal salesOrderQty = salesItems.stream()
                .filter(item -> Objects.equals(item.getMaterialId(), request.getMaterialId()))
                .map(SalDocItem::getQty)
                .map(this::defaultDecimal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (salesOrderQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(409, "来源销售订单中不存在当前产品的可排产数量");
        }

        BigDecimal otherPlannedQty = prdPlanMapper.selectList(new LambdaQueryWrapper<PrdPlan>()
                        .eq(PrdPlan::getSourceSalesId, salesDoc.getId())
                        .eq(PrdPlan::getMaterialId, request.getMaterialId())
                        .ne(PrdPlan::getStatus, STATUS_CLOSED)
                        .ne(currentPlanId != null, PrdPlan::getId, currentPlanId))
                .stream()
                .map(PrdPlan::getPlanQty)
                .map(this::defaultDecimal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainQty = salesOrderQty.subtract(otherPlannedQty).max(BigDecimal.ZERO);
        if (defaultDecimal(request.getPlanQty()).compareTo(remainQty) > 0) {
            throw new BusinessException(409, "计划数量超过销售订单剩余可下推数量");
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate, String message) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException(400, message);
        }
    }

    private BigDecimal normalizePositiveQty(BigDecimal qty, String message) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, message);
        }
        return qty;
    }

    private void ensureDraft(String status, String message) {
        if (!STATUS_DRAFT.equals(status)) {
            throw new BusinessException(409, message);
        }
    }

    private ProductionPlanListItem toPlanItem(PrdPlan item, BasMaterial material) {
        ProductionPlanListItem result = new ProductionPlanListItem();
        result.setId(item.getId());
        result.setCode(item.getCode());
        result.setMaterialId(item.getMaterialId());
        result.setMaterialCode(material != null ? material.getCode() : null);
        result.setMaterialName(material != null ? material.getName() : null);
        result.setPlanQty(item.getPlanQty());
        result.setStartDate(item.getStartDate());
        result.setEndDate(item.getEndDate());
        result.setSourceSalesId(item.getSourceSalesId());
        result.setStatus(item.getStatus());
        return result;
    }

    private WorkOrderListItem toWorkOrderItem(PrdWorkOrder item, BasMaterial material) {
        WorkOrderListItem result = new WorkOrderListItem();
        result.setId(item.getId());
        result.setCode(item.getCode());
        result.setPlanId(item.getPlanId());
        result.setMaterialId(item.getMaterialId());
        result.setMaterialCode(material != null ? material.getCode() : null);
        result.setMaterialName(material != null ? material.getName() : null);
        result.setBomId(item.getBomId());
        result.setRouteId(item.getRouteId());
        result.setPlanQty(defaultDecimal(item.getPlanQty()));
        result.setFinishedQty(defaultDecimal(item.getFinishedQty()));
        result.setStatus(item.getStatus());
        result.setStartDate(item.getStartDate());
        result.setEndDate(item.getEndDate());
        return result;
    }

    private BigDecimal calculateRequiredQty(BigDecimal demandQty, BigDecimal unitQty, BigDecimal lossRate) {
        BigDecimal lossFactor = BigDecimal.ONE.add(defaultDecimal(lossRate));
        return defaultDecimal(demandQty).multiply(defaultDecimal(unitQty)).multiply(lossFactor);
    }

    private Map<Long, BigDecimal> loadMrpSupplyMap() {
        Map<Long, BigDecimal> inventoryRemainMap = new HashMap<>();
        for (StkInventory inventory : stkInventoryMapper.selectList(new LambdaQueryWrapper<StkInventory>())) {
            BigDecimal remainQty = defaultDecimal(inventory.getQty()).subtract(defaultDecimal(inventory.getLockedQty()));
            inventoryRemainMap.merge(inventory.getMaterialId(), remainQty.max(BigDecimal.ZERO), BigDecimal::add);
        }
        for (Map.Entry<Long, BigDecimal> entry : loadPendingPurchaseSupplyMap().entrySet()) {
            inventoryRemainMap.merge(entry.getKey(), entry.getValue(), BigDecimal::add);
        }
        return inventoryRemainMap;
    }

    private Map<Long, BigDecimal> loadPendingPurchaseSupplyMap() {
        List<PurDoc> openDocs = purDocMapper.selectList(new LambdaQueryWrapper<PurDoc>()
                .ne(PurDoc::getStatus, STATUS_CLOSED)
                .ne(PurDoc::getStatus, STATUS_COMPLETED)
                .orderByAsc(PurDoc::getId));
        if (openDocs.isEmpty()) {
            return Map.of();
        }

        Set<Long> docIds = openDocs.stream().map(PurDoc::getId).collect(Collectors.toSet());
        List<PurDoc> requestDocs = openDocs.stream()
                .filter(item -> DOC_TYPE_PURCHASE_REQUEST.equals(item.getDocType()))
                .toList();
        List<PurDoc> orderDocs = openDocs.stream()
                .filter(item -> !DOC_TYPE_PURCHASE_REQUEST.equals(item.getDocType()))
                .toList();
        Map<Long, List<PurDocItem>> itemGroup = purDocItemMapper.selectList(new LambdaQueryWrapper<PurDocItem>()
                        .in(PurDocItem::getDocId, docIds)
                        .orderByAsc(PurDocItem::getId))
                .stream()
                .collect(Collectors.groupingBy(PurDocItem::getDocId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, BigDecimal> convertedRequestQtyMap = new HashMap<>();
        for (PurDoc orderDoc : orderDocs) {
            DocSourceTraceCodec.TraceMetadata metadata = DocSourceTraceCodec.parse(orderDoc.getRemark());
            if (!DOC_TYPE_PURCHASE_REQUEST.equals(metadata.getSourceDocType()) || metadata.getSourceDocId() == null) {
                continue;
            }
            List<PurDocItem> orderItems = itemGroup.getOrDefault(orderDoc.getId(), List.of());
            for (int index = 0; index < orderItems.size() && index < metadata.getSourceItemIds().size(); index++) {
                convertedRequestQtyMap.merge(
                        metadata.getSourceItemIds().get(index),
                        defaultDecimal(orderItems.get(index).getQty()),
                        BigDecimal::add
                );
            }
        }

        Map<Long, BigDecimal> pendingSupplyMap = new HashMap<>();
        for (PurDoc requestDoc : requestDocs) {
            for (PurDocItem item : itemGroup.getOrDefault(requestDoc.getId(), List.of())) {
                BigDecimal pendingQty = defaultDecimal(item.getQty())
                        .subtract(convertedRequestQtyMap.getOrDefault(item.getId(), BigDecimal.ZERO))
                        .max(BigDecimal.ZERO);
                if (pendingQty.compareTo(BigDecimal.ZERO) > 0) {
                    pendingSupplyMap.merge(item.getMaterialId(), pendingQty, BigDecimal::add);
                }
            }
        }
        for (PurDoc orderDoc : orderDocs) {
            for (PurDocItem item : itemGroup.getOrDefault(orderDoc.getId(), List.of())) {
                BigDecimal pendingQty = defaultDecimal(item.getQty()).subtract(defaultDecimal(item.getReceivedQty())).max(BigDecimal.ZERO);
                if (pendingQty.compareTo(BigDecimal.ZERO) > 0) {
                    pendingSupplyMap.merge(item.getMaterialId(), pendingQty, BigDecimal::add);
                }
            }
        }
        return pendingSupplyMap;
    }

    private Set<String> loadActiveWorkOrderKeys() {
        return prdWorkOrderMapper.selectList(new LambdaQueryWrapper<PrdWorkOrder>()
                        .ne(PrdWorkOrder::getStatus, STATUS_CLOSED)
                        .orderByAsc(PrdWorkOrder::getId))
                .stream()
                .map(item -> buildWorkOrderTraceKey(item.getPlanId(), item.getMaterialId(), item.getBomId(), item.getRouteId()))
                .collect(Collectors.toSet());
    }

    private String buildWorkOrderTraceKey(Long planId, Long materialId, Long bomId, Long routeId) {
        return planId + "::" + materialId + "::" + bomId + "::" + routeId;
    }

    private Map<Long, BigDecimal> loadStockQtyByMaterial(String sourceType, Long sourceId) {
        List<StkDoc> docs = stkDocMapper.selectList(new LambdaQueryWrapper<StkDoc>()
                .eq(StkDoc::getSourceType, sourceType)
                .eq(StkDoc::getSourceId, sourceId)
                .orderByAsc(StkDoc::getId));
        if (docs.isEmpty()) {
            return Map.of();
        }
        List<Long> docIds = docs.stream().map(StkDoc::getId).toList();
        return stkDocItemMapper.selectList(new LambdaQueryWrapper<StkDocItem>()
                        .in(StkDocItem::getDocId, docIds))
                .stream()
                .collect(Collectors.toMap(
                        StkDocItem::getMaterialId,
                        item -> defaultDecimal(item.getQty()),
                        BigDecimal::add,
                        LinkedHashMap::new
                ));
    }

    private Map<Long, BigDecimal> aggregateMaterialQty(Map<Long, BigDecimal> materialQtyMap) {
        if (materialQtyMap.isEmpty()) {
            return Map.of();
        }
        return materialQtyMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> defaultDecimal(entry.getValue()),
                        BigDecimal::add,
                        LinkedHashMap::new
                ));
    }

    private Map<Long, BigDecimal> buildRequiredMaterialQtyMap(List<BasBomItem> bomItems, BigDecimal planQty) {
        Map<Long, BigDecimal> requiredQtyMap = new LinkedHashMap<>();
        for (BasBomItem bomItem : bomItems) {
            BigDecimal requiredQty = calculateRequiredQty(planQty, bomItem.getQty(), bomItem.getLossRate());
            requiredQtyMap.merge(bomItem.getMaterialId(), requiredQty, BigDecimal::add);
        }
        return requiredQtyMap;
    }

    private BigDecimal loadReportQty(Long workOrderId, Long processItemId) {
        return prdReportMapper.selectList(new LambdaQueryWrapper<PrdReport>()
                        .eq(PrdReport::getWorkOrderId, workOrderId)
                        .eq(PrdReport::getProcessItemId, processItemId))
                .stream()
                .map(PrdReport::getReportQty)
                .map(this::defaultDecimal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private WorkOrderReportSummary summarizeReports(Long workOrderId) {
        return summarizeReports(loadReportsByWorkOrder(workOrderId));
    }

    private WorkOrderReportSummary summarizeReports(List<PrdReport> reports) {
        WorkOrderReportSummary summary = new WorkOrderReportSummary();
        summary.setReportQty(reports.stream().map(PrdReport::getReportQty).map(this::defaultDecimal).reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.setQualifiedQty(reports.stream().map(PrdReport::getQualifiedQty).map(this::defaultDecimal).reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.setDefectiveQty(reports.stream().map(PrdReport::getDefectiveQty).map(this::defaultDecimal).reduce(BigDecimal.ZERO, BigDecimal::add));
        return summary;
    }

    private List<WorkOrderProgress.MaterialProgressItem> buildMaterialProgress(Map<Long, BigDecimal> requiredQtyMap,
                                                                               Map<Long, BigDecimal> pickedQtyMap,
                                                                               Map<Long, BasMaterial> materialMap) {
        if (requiredQtyMap.isEmpty()) {
            return Collections.emptyList();
        }
        List<WorkOrderProgress.MaterialProgressItem> items = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : requiredQtyMap.entrySet()) {
            BasMaterial material = materialMap.get(entry.getKey());
            BigDecimal pickedQty = pickedQtyMap.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            WorkOrderProgress.MaterialProgressItem item = new WorkOrderProgress.MaterialProgressItem();
            item.setMaterialId(entry.getKey());
            item.setMaterialCode(material != null ? material.getCode() : null);
            item.setMaterialName(material != null ? material.getName() : null);
            item.setRequiredQty(entry.getValue());
            item.setPickedQty(pickedQty);
            item.setPickRate(calculateRate(pickedQty, entry.getValue()));
            items.add(item);
        }
        return items;
    }

    private List<WorkOrderProgress.ProcessProgressItem> buildProcessProgress(List<BasProcessRouteItem> routeItems,
                                                                             List<PrdReport> reports,
                                                                             BigDecimal planQty) {
        if (routeItems.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<PrdReport>> reportMap = reports.stream().collect(Collectors.groupingBy(PrdReport::getProcessItemId));
        List<WorkOrderProgress.ProcessProgressItem> items = new ArrayList<>();
        for (BasProcessRouteItem routeItem : routeItems) {
            List<PrdReport> processReports = reportMap.getOrDefault(routeItem.getId(), List.of());
            BigDecimal reportQty = processReports.stream().map(PrdReport::getReportQty).map(this::defaultDecimal).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal qualifiedQty = processReports.stream().map(PrdReport::getQualifiedQty).map(this::defaultDecimal).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal defectiveQty = processReports.stream().map(PrdReport::getDefectiveQty).map(this::defaultDecimal).reduce(BigDecimal.ZERO, BigDecimal::add);
            LocalDateTime latestReportTime = processReports.stream()
                    .map(PrdReport::getReportDate)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);

            WorkOrderProgress.ProcessProgressItem item = new WorkOrderProgress.ProcessProgressItem();
            item.setProcessItemId(routeItem.getId());
            item.setProcessCode(routeItem.getProcessCode());
            item.setProcessName(routeItem.getProcessName());
            item.setReportQty(reportQty);
            item.setQualifiedQty(qualifiedQty);
            item.setDefectiveQty(defectiveQty);
            item.setCompletionRate(calculateRate(qualifiedQty, planQty));
            item.setLatestReportTime(latestReportTime);
            items.add(item);
        }
        return items;
    }

    private BigDecimal calculateRate(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return defaultDecimal(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private void markWorkOrderInProgress(PrdWorkOrder workOrder, LocalDate executeDate) {
        if (STATUS_APPROVED.equals(workOrder.getStatus())) {
            workOrder.setStatus(STATUS_IN_PROGRESS);
        }
        if (workOrder.getStartDate() == null) {
            workOrder.setStartDate(executeDate);
        }
        prdWorkOrderMapper.updateById(workOrder);
    }

    private void syncPlanStatus(Long planId) {
        PrdPlan plan = prdPlanMapper.selectById(planId);
        if (plan == null || STATUS_CLOSED.equals(plan.getStatus())) {
            return;
        }

        List<PrdWorkOrder> workOrders = prdWorkOrderMapper.selectList(new LambdaQueryWrapper<PrdWorkOrder>()
                .eq(PrdWorkOrder::getPlanId, planId)
                .orderByAsc(PrdWorkOrder::getId));
        if (workOrders.isEmpty()) {
            return;
        }

        List<PrdWorkOrder> topLevelOrders = workOrders.stream()
                .filter(item -> Objects.equals(item.getMaterialId(), plan.getMaterialId()))
                .toList();
        BigDecimal topLevelFinishedQty = topLevelOrders.stream()
                .map(PrdWorkOrder::getFinishedQty)
                .map(this::defaultDecimal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String nextStatus = plan.getStatus();
        if (!topLevelOrders.isEmpty() && topLevelFinishedQty.compareTo(defaultDecimal(plan.getPlanQty())) >= 0) {
            nextStatus = STATUS_COMPLETED;
        } else if (workOrders.stream().anyMatch(item -> STATUS_IN_PROGRESS.equals(item.getStatus())
                || STATUS_COMPLETED.equals(item.getStatus())
                || STATUS_APPROVED.equals(item.getStatus()))) {
            nextStatus = STATUS_IN_PROGRESS;
        } else if (workOrders.stream().allMatch(item -> STATUS_DRAFT.equals(item.getStatus()))) {
            nextStatus = STATUS_APPROVED;
        }

        if (!Objects.equals(plan.getStatus(), nextStatus)) {
            plan.setStatus(nextStatus);
            prdPlanMapper.updateById(plan);
        }
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String generatePlanCode() {
        return "PP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private String generateWorkOrderCode() {
        return "WO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private String generatePurchaseRequestCode() {
        return "PR" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private MrpTaskSnapshot requireMrpTask(String taskKey) {
        MrpTaskSnapshot snapshot = mrpTaskCache.get(taskKey);
        if (snapshot == null) {
            throw new BusinessException(404, "MRP任务不存在或已失效");
        }
        return snapshot;
    }

    private static class MrpAggregation {
        private final Map<String, PurchaseSuggestionAggregate> purchaseSuggestions = new LinkedHashMap<>();
        private final Map<String, WorkOrderSuggestionAggregate> workOrderSuggestions = new LinkedHashMap<>();

        public Map<String, PurchaseSuggestionAggregate> getPurchaseSuggestions() {
            return purchaseSuggestions;
        }

        public Map<String, WorkOrderSuggestionAggregate> getWorkOrderSuggestions() {
            return workOrderSuggestions;
        }
    }

    private class MrpReferenceContext {
        private final Map<Long, BasBom> bomCache = new HashMap<>();
        private final Map<Long, BasProcessRoute> routeCache = new HashMap<>();
        private final Map<Long, List<BasBomItem>> bomItemCache = new HashMap<>();

        public BasBom getActiveBom(Long materialId) {
            return bomCache.computeIfAbsent(materialId, ProductionServiceImpl.this::findActiveBom);
        }

        public BasProcessRoute getActiveRoute(Long materialId) {
            return routeCache.computeIfAbsent(materialId, ProductionServiceImpl.this::findActiveRoute);
        }

        public List<BasBomItem> getBomItems(Long bomId) {
            return bomItemCache.computeIfAbsent(bomId, ProductionServiceImpl.this::loadBomItems);
        }
    }

    private static class PurchaseSuggestionAggregate {
        private final Long planId;
        private final Long materialId;
        private final Long warehouseId;
        private final LocalDate needDate;
        private BigDecimal requiredQty = BigDecimal.ZERO;
        private BigDecimal shortageQty = BigDecimal.ZERO;

        private PurchaseSuggestionAggregate(Long planId, Long materialId, Long warehouseId, LocalDate needDate) {
            this.planId = planId;
            this.materialId = materialId;
            this.warehouseId = warehouseId;
            this.needDate = needDate;
        }

        public Long getPlanId() {
            return planId;
        }

        public Long getMaterialId() {
            return materialId;
        }

        public Long getWarehouseId() {
            return warehouseId;
        }

        public LocalDate getNeedDate() {
            return needDate;
        }

        public BigDecimal getRequiredQty() {
            return requiredQty;
        }

        public void setRequiredQty(BigDecimal requiredQty) {
            this.requiredQty = requiredQty;
        }

        public BigDecimal getShortageQty() {
            return shortageQty;
        }

        public void setShortageQty(BigDecimal shortageQty) {
            this.shortageQty = shortageQty;
        }
    }

    private static class WorkOrderSuggestionAggregate {
        private final Long planId;
        private final Long materialId;
        private final Long bomId;
        private final Long routeId;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private BigDecimal planQty = BigDecimal.ZERO;

        private WorkOrderSuggestionAggregate(Long planId, Long materialId, Long bomId, Long routeId, LocalDate startDate, LocalDate endDate) {
            this.planId = planId;
            this.materialId = materialId;
            this.bomId = bomId;
            this.routeId = routeId;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public Long getPlanId() {
            return planId;
        }

        public Long getMaterialId() {
            return materialId;
        }

        public Long getBomId() {
            return bomId;
        }

        public Long getRouteId() {
            return routeId;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public BigDecimal getPlanQty() {
            return planQty;
        }

        public void setPlanQty(BigDecimal planQty) {
            this.planQty = planQty;
        }
    }

    private static class PurchaseSuggestionPayload {
        private Long suggestionId;
        private Long planId;
        private Long materialId;
        private Long warehouseId;
        private BigDecimal requiredQty;
        private BigDecimal shortageQty;
        private LocalDate needDate;

        public Long getSuggestionId() {
            return suggestionId;
        }

        public void setSuggestionId(Long suggestionId) {
            this.suggestionId = suggestionId;
        }

        public Long getPlanId() {
            return planId;
        }

        public void setPlanId(Long planId) {
            this.planId = planId;
        }

        public Long getMaterialId() {
            return materialId;
        }

        public void setMaterialId(Long materialId) {
            this.materialId = materialId;
        }

        public Long getWarehouseId() {
            return warehouseId;
        }

        public void setWarehouseId(Long warehouseId) {
            this.warehouseId = warehouseId;
        }

        public BigDecimal getRequiredQty() {
            return requiredQty;
        }

        public void setRequiredQty(BigDecimal requiredQty) {
            this.requiredQty = requiredQty;
        }

        public BigDecimal getShortageQty() {
            return shortageQty;
        }

        public void setShortageQty(BigDecimal shortageQty) {
            this.shortageQty = shortageQty;
        }

        public LocalDate getNeedDate() {
            return needDate;
        }

        public void setNeedDate(LocalDate needDate) {
            this.needDate = needDate;
        }
    }

    private static class WorkOrderSuggestionPayload {
        private Long suggestionId;
        private Long planId;
        private Long materialId;
        private Long bomId;
        private Long routeId;
        private BigDecimal planQty;
        private LocalDate startDate;
        private LocalDate endDate;

        public Long getSuggestionId() {
            return suggestionId;
        }

        public void setSuggestionId(Long suggestionId) {
            this.suggestionId = suggestionId;
        }

        public Long getPlanId() {
            return planId;
        }

        public void setPlanId(Long planId) {
            this.planId = planId;
        }

        public Long getMaterialId() {
            return materialId;
        }

        public void setMaterialId(Long materialId) {
            this.materialId = materialId;
        }

        public Long getBomId() {
            return bomId;
        }

        public void setBomId(Long bomId) {
            this.bomId = bomId;
        }

        public Long getRouteId() {
            return routeId;
        }

        public void setRouteId(Long routeId) {
            this.routeId = routeId;
        }

        public BigDecimal getPlanQty() {
            return planQty;
        }

        public void setPlanQty(BigDecimal planQty) {
            this.planQty = planQty;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }
    }

    private static class MrpTaskSnapshot {
        private final String taskKey;
        private final Map<Long, PurchaseSuggestionPayload> purchaseSuggestions;
        private final Map<Long, WorkOrderSuggestionPayload> workOrderSuggestions;

        private MrpTaskSnapshot(String taskKey,
                                Map<Long, PurchaseSuggestionPayload> purchaseSuggestions,
                                Map<Long, WorkOrderSuggestionPayload> workOrderSuggestions) {
            this.taskKey = taskKey;
            this.purchaseSuggestions = purchaseSuggestions;
            this.workOrderSuggestions = workOrderSuggestions;
        }

        public String getTaskKey() {
            return taskKey;
        }

        public Map<Long, PurchaseSuggestionPayload> getPurchaseSuggestions() {
            return purchaseSuggestions;
        }

        public Map<Long, WorkOrderSuggestionPayload> getWorkOrderSuggestions() {
            return workOrderSuggestions;
        }

        public Set<Long> collectMaterialIds() {
            Set<Long> materialIds = new HashSet<>();
            purchaseSuggestions.values().forEach(item -> materialIds.add(item.getMaterialId()));
            workOrderSuggestions.values().forEach(item -> materialIds.add(item.getMaterialId()));
            return materialIds;
        }

        public MrpResult toResult(Map<Long, BasMaterial> materialMap) {
            MrpResult result = new MrpResult();
            result.setTaskKey(taskKey);
            result.setPurchaseSuggestions(purchaseSuggestions.values().stream().map(item -> {
                BasMaterial material = materialMap.get(item.getMaterialId());
                MrpResult.PurchaseSuggestion suggestion = new MrpResult.PurchaseSuggestion();
                suggestion.setId(item.getSuggestionId());
                suggestion.setPlanId(item.getPlanId());
                suggestion.setMaterialId(item.getMaterialId());
                suggestion.setMaterialCode(material != null ? material.getCode() : null);
                suggestion.setMaterialName(material != null ? material.getName() : null);
                suggestion.setRequiredQty(item.getRequiredQty());
                suggestion.setShortageQty(item.getShortageQty());
                suggestion.setAvailableQty(item.getRequiredQty().subtract(item.getShortageQty()));
                suggestion.setNeedDate(item.getNeedDate());
                return suggestion;
            }).toList());
            result.setWorkOrderSuggestions(workOrderSuggestions.values().stream().map(item -> {
                BasMaterial material = materialMap.get(item.getMaterialId());
                MrpResult.WorkOrderSuggestion suggestion = new MrpResult.WorkOrderSuggestion();
                suggestion.setId(item.getSuggestionId());
                suggestion.setPlanId(item.getPlanId());
                suggestion.setMaterialId(item.getMaterialId());
                suggestion.setMaterialCode(material != null ? material.getCode() : null);
                suggestion.setMaterialName(material != null ? material.getName() : null);
                suggestion.setBomId(item.getBomId());
                suggestion.setRouteId(item.getRouteId());
                suggestion.setPlanQty(item.getPlanQty());
                suggestion.setStartDate(item.getStartDate());
                suggestion.setEndDate(item.getEndDate());
                return suggestion;
            }).toList());
            return result;
        }
    }

    private static class WorkOrderReportSummary {
        private BigDecimal reportQty = BigDecimal.ZERO;
        private BigDecimal qualifiedQty = BigDecimal.ZERO;
        private BigDecimal defectiveQty = BigDecimal.ZERO;

        public BigDecimal getReportQty() {
            return reportQty;
        }

        public void setReportQty(BigDecimal reportQty) {
            this.reportQty = reportQty;
        }

        public BigDecimal getQualifiedQty() {
            return qualifiedQty;
        }

        public void setQualifiedQty(BigDecimal qualifiedQty) {
            this.qualifiedQty = qualifiedQty;
        }

        public BigDecimal getDefectiveQty() {
            return defectiveQty;
        }

        public void setDefectiveQty(BigDecimal defectiveQty) {
            this.defectiveQty = defectiveQty;
        }
    }
}
