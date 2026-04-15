package com.dongjian.erp.manufacturingerpsystem.modules.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.entity.BasCustomer;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.entity.BasMaterial;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.entity.BasSupplier;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.mapper.BasCustomerMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.mapper.BasMaterialMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.basic.mapper.BasSupplierMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.entity.StkInventory;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.mapper.StkInventoryMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.production.entity.PrdReport;
import com.dongjian.erp.manufacturingerpsystem.modules.production.entity.PrdWorkOrder;
import com.dongjian.erp.manufacturingerpsystem.modules.production.mapper.PrdReportMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.production.mapper.PrdWorkOrderMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.purchase.entity.PurDoc;
import com.dongjian.erp.manufacturingerpsystem.modules.purchase.entity.PurDocItem;
import com.dongjian.erp.manufacturingerpsystem.modules.purchase.mapper.PurDocItemMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.purchase.mapper.PurDocMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.report.service.ReportService;
import com.dongjian.erp.manufacturingerpsystem.modules.sales.entity.SalDoc;
import com.dongjian.erp.manufacturingerpsystem.modules.sales.entity.SalDocItem;
import com.dongjian.erp.manufacturingerpsystem.modules.sales.mapper.SalDocItemMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.sales.mapper.SalDocMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    private static final String DOC_TYPE_ORDER = "order";
    private static final String STATUS_CLOSED = "closed";
    private static final String STATUS_COMPLETED = "completed";
    private static final int DEFAULT_TOP_LIMIT = 10;
    private static final int MAX_TOP_LIMIT = 20;

    private final SalDocMapper salDocMapper;
    private final SalDocItemMapper salDocItemMapper;
    private final PurDocMapper purDocMapper;
    private final PurDocItemMapper purDocItemMapper;
    private final StkInventoryMapper stkInventoryMapper;
    private final PrdWorkOrderMapper prdWorkOrderMapper;
    private final PrdReportMapper prdReportMapper;
    private final BasMaterialMapper basMaterialMapper;
    private final BasCustomerMapper basCustomerMapper;
    private final BasSupplierMapper basSupplierMapper;

    public ReportServiceImpl(SalDocMapper salDocMapper,
                             SalDocItemMapper salDocItemMapper,
                             PurDocMapper purDocMapper,
                             PurDocItemMapper purDocItemMapper,
                             StkInventoryMapper stkInventoryMapper,
                             PrdWorkOrderMapper prdWorkOrderMapper,
                             PrdReportMapper prdReportMapper,
                             BasMaterialMapper basMaterialMapper,
                             BasCustomerMapper basCustomerMapper,
                             BasSupplierMapper basSupplierMapper) {
        this.salDocMapper = salDocMapper;
        this.salDocItemMapper = salDocItemMapper;
        this.purDocMapper = purDocMapper;
        this.purDocItemMapper = purDocItemMapper;
        this.stkInventoryMapper = stkInventoryMapper;
        this.prdWorkOrderMapper = prdWorkOrderMapper;
        this.prdReportMapper = prdReportMapper;
        this.basMaterialMapper = basMaterialMapper;
        this.basCustomerMapper = basCustomerMapper;
        this.basSupplierMapper = basSupplierMapper;
    }

    @Override
    public Map<String, Object> salesSummary(LocalDate startDate, LocalDate endDate, Long customerId) {
        DateRange dateRange = normalizeDateRange(startDate, endDate);
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<SalDoc> orderQuery = new LambdaQueryWrapper<SalDoc>()
                .eq(SalDoc::getDocType, DOC_TYPE_ORDER)
                .orderByAsc(SalDoc::getDocDate);
        if (customerId != null) {
            orderQuery.eq(SalDoc::getCustomerId, customerId);
        }
        List<SalDoc> orders = salDocMapper.selectList(orderQuery);
        List<LocalDate> orderDates = orders.stream().map(SalDoc::getDocDate).filter(Objects::nonNull).toList();
        LocalDate seriesStart = resolveSeriesStart(dateRange.getStartDate(), dateRange.getEndDate(), orderDates, today);
        LocalDate seriesEnd = resolveSeriesEnd(dateRange.getStartDate(), dateRange.getEndDate(), orderDates, today);

        if (orders.isEmpty()) {
            return buildEmptySalesSummary(seriesStart, seriesEnd, dateRange);
        }

        Map<Long, SalDoc> orderMap = orders.stream().collect(Collectors.toMap(SalDoc::getId, item -> item));
        List<SalDocItem> items = listSalesItems(orderMap.keySet());
        List<SalDoc> scopedOrders = filterByDateRange(orders, SalDoc::getDocDate, dateRange.getStartDate(), dateRange.getEndDate());
        Set<Long> scopedOrderIds = scopedOrders.stream().map(SalDoc::getId).collect(Collectors.toSet());
        List<SalDocItem> scopedItems = items.stream()
                .filter(item -> scopedOrderIds.contains(item.getDocId()))
                .toList();

        BigDecimal summaryAmount = scopedOrders.stream()
                .map(SalDoc::getTotalAmount)
                .map(this::defaultDecimal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalQty = scopedItems.stream()
                .map(SalDocItem::getQty)
                .map(this::defaultDecimal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shippedQty = scopedItems.stream()
                .map(SalDocItem::getShippedQty)
                .map(this::defaultDecimal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<YearMonth, BigDecimal> monthAmountMap = initMonthAmountMap(seriesStart, seriesEnd);
        for (SalDoc order : orders) {
            if (order.getDocDate() == null) {
                continue;
            }
            YearMonth month = YearMonth.from(order.getDocDate());
            if (monthAmountMap.containsKey(month)
                    && isWithinDateRange(order.getDocDate(), dateRange.getStartDate(), dateRange.getEndDate())) {
                monthAmountMap.put(month, monthAmountMap.get(month).add(defaultDecimal(order.getTotalAmount())));
            }
        }

        List<SalDocItem> topProductSource = scopedItems;
        if (topProductSource.isEmpty() && !hasDateFilter(dateRange.getStartDate(), dateRange.getEndDate())) {
            topProductSource = items;
        }
        Map<Long, BigDecimal> productAmountMap = aggregateSalesAmountByMaterial(topProductSource);
        Map<Long, BasMaterial> materialMap = loadMaterialMap(productAmountMap.keySet());
        Long topProductId = resolveTopEntryId(productAmountMap);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summaryLabel", buildSummaryLabel(dateRange, "本期销售额", "本月销售额"));
        result.put("rangeStart", dateRange.getStartDate());
        result.put("rangeEnd", dateRange.getEndDate());
        result.put("currentMonthSales", scaleAmount(summaryAmount));
        result.put("summaryAmount", scaleAmount(summaryAmount));
        result.put("shipmentRate", calculateRatio(shippedQty, totalQty, 4));
        result.put("topProduct", resolveTopMaterialName(productAmountMap, materialMap, "暂无销售产品"));
        result.put("topProductId", topProductId);
        result.put("series", monthAmountMap.values().stream().map(this::scaleAmount).toList());
        result.put("monthLabels", monthAmountMap.keySet().stream().map(this::toMonthLabel).toList());
        result.put("orderCount", scopedOrders.size());
        result.put("shippedQty", scaleQty(shippedQty));
        result.put("pendingShipmentQty", scaleQty(totalQty.subtract(shippedQty).max(BigDecimal.ZERO)));
        return result;
    }

    @Override
    public Map<String, Object> purchaseSummary(LocalDate startDate, LocalDate endDate, Long supplierId) {
        DateRange dateRange = normalizeDateRange(startDate, endDate);
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<PurDoc> orderQuery = new LambdaQueryWrapper<PurDoc>()
                .eq(PurDoc::getDocType, DOC_TYPE_ORDER)
                .orderByAsc(PurDoc::getDocDate);
        if (supplierId != null) {
            orderQuery.eq(PurDoc::getSupplierId, supplierId);
        }
        List<PurDoc> orders = purDocMapper.selectList(orderQuery);
        List<LocalDate> orderDates = orders.stream().map(PurDoc::getDocDate).filter(Objects::nonNull).toList();
        LocalDate seriesStart = resolveSeriesStart(dateRange.getStartDate(), dateRange.getEndDate(), orderDates, today);
        LocalDate seriesEnd = resolveSeriesEnd(dateRange.getStartDate(), dateRange.getEndDate(), orderDates, today);

        if (orders.isEmpty()) {
            return buildEmptyPurchaseSummary(seriesStart, seriesEnd, dateRange);
        }

        Map<Long, PurDoc> orderMap = orders.stream().collect(Collectors.toMap(PurDoc::getId, item -> item));
        List<PurDocItem> items = listPurchaseItems(orderMap.keySet());
        List<PurDoc> scopedOrders = filterByDateRange(orders, PurDoc::getDocDate, dateRange.getStartDate(), dateRange.getEndDate());
        Set<Long> scopedOrderIds = scopedOrders.stream().map(PurDoc::getId).collect(Collectors.toSet());
        List<PurDocItem> scopedItems = items.stream()
                .filter(item -> scopedOrderIds.contains(item.getDocId()))
                .toList();

        BigDecimal summaryAmount = scopedOrders.stream()
                .map(PurDoc::getTotalAmount)
                .map(this::defaultDecimal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalQty = scopedItems.stream()
                .map(PurDocItem::getQty)
                .map(this::defaultDecimal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal receivedQty = scopedItems.stream()
                .map(PurDocItem::getReceivedQty)
                .map(this::defaultDecimal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<YearMonth, BigDecimal> monthAmountMap = initMonthAmountMap(seriesStart, seriesEnd);
        for (PurDoc order : orders) {
            if (order.getDocDate() == null) {
                continue;
            }
            YearMonth month = YearMonth.from(order.getDocDate());
            if (monthAmountMap.containsKey(month)
                    && isWithinDateRange(order.getDocDate(), dateRange.getStartDate(), dateRange.getEndDate())) {
                monthAmountMap.put(month, monthAmountMap.get(month).add(defaultDecimal(order.getTotalAmount())));
            }
        }

        List<PurDoc> topSupplierSource = scopedOrders;
        if (topSupplierSource.isEmpty() && !hasDateFilter(dateRange.getStartDate(), dateRange.getEndDate())) {
            topSupplierSource = orders;
        }
        Map<Long, BigDecimal> supplierAmountMap = new HashMap<>();
        for (PurDoc order : topSupplierSource) {
            if (order.getSupplierId() == null) {
                continue;
            }
            supplierAmountMap.merge(order.getSupplierId(), defaultDecimal(order.getTotalAmount()), BigDecimal::add);
        }
        Map<Long, BasSupplier> supplierMap = loadSupplierMap(supplierAmountMap.keySet());
        Long topSupplierId = resolveTopEntryId(supplierAmountMap);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summaryLabel", buildSummaryLabel(dateRange, "本期采购额", "本月采购额"));
        result.put("rangeStart", dateRange.getStartDate());
        result.put("rangeEnd", dateRange.getEndDate());
        result.put("currentMonthPurchase", scaleAmount(summaryAmount));
        result.put("summaryAmount", scaleAmount(summaryAmount));
        result.put("topSupplier", resolveTopSupplierName(supplierAmountMap, supplierMap, "暂无供应商"));
        result.put("topSupplierId", topSupplierId);
        result.put("deliveryRate", calculateRatio(receivedQty, totalQty, 4));
        result.put("series", monthAmountMap.values().stream().map(this::scaleAmount).toList());
        result.put("monthLabels", monthAmountMap.keySet().stream().map(this::toMonthLabel).toList());
        result.put("orderCount", scopedOrders.size());
        result.put("receivedQty", scaleQty(receivedQty));
        result.put("pendingReceiveQty", scaleQty(totalQty.subtract(receivedQty).max(BigDecimal.ZERO)));
        return result;
    }

    @Override
    public Map<String, Object> inventorySummary(LocalDate startDate, LocalDate endDate, Long warehouseId) {
        DateRange dateRange = normalizeDateRange(startDate, endDate);
        LambdaQueryWrapper<StkInventory> inventoryQuery = new LambdaQueryWrapper<StkInventory>()
                .orderByAsc(StkInventory::getMaterialId);
        if (warehouseId != null) {
            inventoryQuery.eq(StkInventory::getWarehouseId, warehouseId);
        }
        List<StkInventory> inventories = stkInventoryMapper.selectList(inventoryQuery);
        if (inventories.isEmpty()) {
            return buildEmptyInventorySummary(dateRange);
        }

        Map<Long, BigDecimal> stockQtyMap = new HashMap<>();
        Map<Long, BigDecimal> availableQtyMap = new HashMap<>();
        for (StkInventory inventory : inventories) {
            BigDecimal qty = defaultDecimal(inventory.getQty());
            BigDecimal availableQty = qty.subtract(defaultDecimal(inventory.getLockedQty())).max(BigDecimal.ZERO);
            stockQtyMap.merge(inventory.getMaterialId(), qty, BigDecimal::add);
            availableQtyMap.merge(inventory.getMaterialId(), availableQty, BigDecimal::add);
        }

        Map<Long, BasMaterial> materialMap = loadMaterialMap(stockQtyMap.keySet());
        Map<Long, BigDecimal> materialPriceMap = loadMaterialPurchasePriceMap();
        BigDecimal inventoryAmount = BigDecimal.ZERO;
        BigDecimal totalQty = BigDecimal.ZERO;
        long warningCount = 0;
        for (Map.Entry<Long, BigDecimal> entry : stockQtyMap.entrySet()) {
            Long materialId = entry.getKey();
            BigDecimal qty = entry.getValue();
            inventoryAmount = inventoryAmount.add(qty.multiply(materialPriceMap.getOrDefault(materialId, BigDecimal.ZERO)));
            totalQty = totalQty.add(qty);

            BasMaterial material = materialMap.get(materialId);
            BigDecimal safetyStock = material != null ? defaultDecimal(material.getSafetyStock()) : BigDecimal.ZERO;
            if (safetyStock.compareTo(BigDecimal.ZERO) > 0
                    && availableQtyMap.getOrDefault(materialId, BigDecimal.ZERO).compareTo(safetyStock) < 0) {
                warningCount += 1;
            }
        }

        BigDecimal outboundAmount = hasDateFilter(dateRange.getStartDate(), dateRange.getEndDate())
                ? calculateShipmentAmount(dateRange.getStartDate(), dateRange.getEndDate())
                : calculateShipmentAmount(LocalDate.now().minusDays(29), LocalDate.now());
        long turnoverBaseDays = hasDateFilter(dateRange.getStartDate(), dateRange.getEndDate())
                ? calculateTurnoverBaseDays(dateRange.getStartDate(), dateRange.getEndDate())
                : 30L;
        BigDecimal turnoverDays = BigDecimal.ZERO;
        if (outboundAmount.compareTo(BigDecimal.ZERO) > 0 && turnoverBaseDays > 0) {
            turnoverDays = inventoryAmount.multiply(BigDecimal.valueOf(turnoverBaseDays))
                    .divide(outboundAmount, 2, RoundingMode.HALF_UP);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summaryLabel", buildSummaryLabel(dateRange, "所选期间库存快照", "库存快照"));
        result.put("rangeStart", dateRange.getStartDate());
        result.put("rangeEnd", dateRange.getEndDate());
        result.put("inventoryAmount", scaleAmount(inventoryAmount));
        result.put("warningCount", warningCount);
        result.put("turnoverDays", turnoverDays);
        result.put("inventoryQty", scaleQty(totalQty));
        result.put("availableQty", scaleQty(availableQtyMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)));
        return result;
    }

    @Override
    public Map<String, Object> productionSummary(LocalDate startDate, LocalDate endDate) {
        DateRange dateRange = normalizeDateRange(startDate, endDate);
        List<PrdWorkOrder> workOrders = prdWorkOrderMapper.selectList(new LambdaQueryWrapper<PrdWorkOrder>()
                .orderByDesc(PrdWorkOrder::getId));
        List<PrdWorkOrder> scopedWorkOrders = workOrders.stream()
                .filter(item -> isWorkOrderWithinRange(item, dateRange.getStartDate(), dateRange.getEndDate()))
                .toList();
        if (scopedWorkOrders.isEmpty()) {
            return buildEmptyProductionSummary(dateRange);
        }

        List<Long> workOrderIds = scopedWorkOrders.stream().map(PrdWorkOrder::getId).toList();
        Map<Long, List<PrdReport>> reportMap = listReports(workOrderIds).stream()
                .collect(Collectors.groupingBy(PrdReport::getWorkOrderId));
        List<PrdWorkOrder> completedOrders = scopedWorkOrders.stream().filter(this::isCompletedWorkOrder).toList();
        long onTimeCount = completedOrders.stream()
                .filter(item -> isOnTime(item, reportMap.getOrDefault(item.getId(), List.of())))
                .count();
        long delayRiskCount = scopedWorkOrders.stream()
                .filter(item -> !STATUS_CLOSED.equalsIgnoreCase(item.getStatus()))
                .filter(item -> !isCompletedWorkOrder(item))
                .filter(this::hasDelayRisk)
                .count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summaryLabel", buildSummaryLabel(dateRange, "所选期间生产执行", "生产执行概览"));
        result.put("rangeStart", dateRange.getStartDate());
        result.put("rangeEnd", dateRange.getEndDate());
        result.put("workOrderCount", scopedWorkOrders.size());
        result.put("onTimeRate", calculateRatio(BigDecimal.valueOf(onTimeCount), BigDecimal.valueOf(completedOrders.size()), 4));
        result.put("delayRiskCount", delayRiskCount);
        result.put("completedCount", completedOrders.size());
        result.put("inProgressCount", scopedWorkOrders.stream()
                .filter(item -> !isCompletedWorkOrder(item) && !STATUS_CLOSED.equalsIgnoreCase(item.getStatus()))
                .count());
        return result;
    }

    @Override
    public List<Map<String, Object>> arSummary(LocalDate startDate, LocalDate endDate, Integer limit, Long customerId) {
        DateRange dateRange = normalizeDateRange(startDate, endDate);
        LambdaQueryWrapper<SalDoc> orderQuery = new LambdaQueryWrapper<SalDoc>()
                .eq(SalDoc::getDocType, DOC_TYPE_ORDER);
        if (customerId != null) {
            orderQuery.eq(SalDoc::getCustomerId, customerId);
        }
        List<SalDoc> orders = filterByDateRange(
                salDocMapper.selectList(orderQuery),
                SalDoc::getDocDate,
                dateRange.getStartDate(),
                dateRange.getEndDate()
        );
        if (orders.isEmpty()) {
            return List.of();
        }

        Map<Long, SalDoc> orderMap = orders.stream().collect(Collectors.toMap(SalDoc::getId, item -> item));
        List<SalDocItem> items = listSalesItems(orderMap.keySet());
        Map<Long, OutstandingAggregate> customerAmountMap = new HashMap<>();
        for (SalDocItem item : items) {
            SalDoc order = orderMap.get(item.getDocId());
            if (order == null || order.getCustomerId() == null) {
                continue;
            }
            BigDecimal outstandingAmount = calculateOutstandingAmount(item.getAmount(), item.getPrice(), item.getQty(), item.getShippedQty());
            if (outstandingAmount.compareTo(BigDecimal.ZERO) > 0) {
                customerAmountMap.computeIfAbsent(order.getCustomerId(), key -> new OutstandingAggregate())
                        .accumulate(order.getId(), order.getCode(), order.getDocDate(), outstandingAmount);
            }
        }

        Map<Long, BasCustomer> customerMap = loadCustomerMap(customerAmountMap.keySet());
        return customerAmountMap.entrySet().stream()
                .filter(entry -> entry.getValue().getAmount().compareTo(BigDecimal.ZERO) > 0)
                .sorted((left, right) -> right.getValue().getAmount().compareTo(left.getValue().getAmount()))
                .limit(resolveRankingLimit(limit))
                .map(entry -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    BasCustomer customer = customerMap.get(entry.getKey());
                    OutstandingAggregate aggregate = entry.getValue();
                    row.put("customerId", entry.getKey());
                    row.put("customer", customer != null ? customer.getName() : "客户#" + entry.getKey());
                    row.put("amount", scaleAmount(aggregate.getAmount()));
                    row.put("orderCount", aggregate.getOrderCount());
                    row.put("sourceOrderId", aggregate.getSourceDocId());
                    row.put("sourceOrderCode", aggregate.getSourceDocCode());
                    row.put("sourceDocDate", aggregate.getSourceDocDate());
                    return row;
                })
                .toList();
    }

    @Override
    public List<Map<String, Object>> apSummary(LocalDate startDate, LocalDate endDate, Integer limit, Long supplierId) {
        DateRange dateRange = normalizeDateRange(startDate, endDate);
        LambdaQueryWrapper<PurDoc> orderQuery = new LambdaQueryWrapper<PurDoc>()
                .eq(PurDoc::getDocType, DOC_TYPE_ORDER);
        if (supplierId != null) {
            orderQuery.eq(PurDoc::getSupplierId, supplierId);
        }
        List<PurDoc> orders = filterByDateRange(
                purDocMapper.selectList(orderQuery),
                PurDoc::getDocDate,
                dateRange.getStartDate(),
                dateRange.getEndDate()
        );
        if (orders.isEmpty()) {
            return List.of();
        }

        Map<Long, PurDoc> orderMap = orders.stream().collect(Collectors.toMap(PurDoc::getId, item -> item));
        List<PurDocItem> items = listPurchaseItems(orderMap.keySet());
        Map<Long, OutstandingAggregate> supplierAmountMap = new HashMap<>();
        for (PurDocItem item : items) {
            PurDoc order = orderMap.get(item.getDocId());
            if (order == null || order.getSupplierId() == null) {
                continue;
            }
            BigDecimal outstandingAmount = calculateOutstandingAmount(item.getAmount(), item.getPrice(), item.getQty(), item.getReceivedQty());
            if (outstandingAmount.compareTo(BigDecimal.ZERO) > 0) {
                supplierAmountMap.computeIfAbsent(order.getSupplierId(), key -> new OutstandingAggregate())
                        .accumulate(order.getId(), order.getCode(), order.getDocDate(), outstandingAmount);
            }
        }

        Map<Long, BasSupplier> supplierMap = loadSupplierMap(supplierAmountMap.keySet());
        return supplierAmountMap.entrySet().stream()
                .filter(entry -> entry.getValue().getAmount().compareTo(BigDecimal.ZERO) > 0)
                .sorted((left, right) -> right.getValue().getAmount().compareTo(left.getValue().getAmount()))
                .limit(resolveRankingLimit(limit))
                .map(entry -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    BasSupplier supplier = supplierMap.get(entry.getKey());
                    OutstandingAggregate aggregate = entry.getValue();
                    row.put("supplierId", entry.getKey());
                    row.put("supplier", supplier != null ? supplier.getName() : "供应商#" + entry.getKey());
                    row.put("amount", scaleAmount(aggregate.getAmount()));
                    row.put("orderCount", aggregate.getOrderCount());
                    row.put("sourceOrderId", aggregate.getSourceDocId());
                    row.put("sourceOrderCode", aggregate.getSourceDocCode());
                    row.put("sourceDocDate", aggregate.getSourceDocDate());
                    return row;
                })
                .toList();
    }

    private Map<String, Object> buildEmptySalesSummary(LocalDate seriesStart, LocalDate seriesEnd, DateRange dateRange) {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<YearMonth, BigDecimal> monthAmountMap = initMonthAmountMap(seriesStart, seriesEnd);
        result.put("summaryLabel", buildSummaryLabel(dateRange, "本期销售额", "本月销售额"));
        result.put("rangeStart", dateRange.getStartDate());
        result.put("rangeEnd", dateRange.getEndDate());
        result.put("currentMonthSales", BigDecimal.ZERO);
        result.put("summaryAmount", BigDecimal.ZERO);
        result.put("shipmentRate", BigDecimal.ZERO);
        result.put("topProduct", "暂无销售产品");
        result.put("topProductId", null);
        result.put("series", monthAmountMap.values().stream().map(this::scaleAmount).toList());
        result.put("monthLabels", monthAmountMap.keySet().stream().map(this::toMonthLabel).toList());
        result.put("orderCount", 0);
        result.put("shippedQty", BigDecimal.ZERO);
        result.put("pendingShipmentQty", BigDecimal.ZERO);
        return result;
    }

    private Map<String, Object> buildEmptyPurchaseSummary(LocalDate seriesStart, LocalDate seriesEnd, DateRange dateRange) {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<YearMonth, BigDecimal> monthAmountMap = initMonthAmountMap(seriesStart, seriesEnd);
        result.put("summaryLabel", buildSummaryLabel(dateRange, "本期采购额", "本月采购额"));
        result.put("rangeStart", dateRange.getStartDate());
        result.put("rangeEnd", dateRange.getEndDate());
        result.put("currentMonthPurchase", BigDecimal.ZERO);
        result.put("summaryAmount", BigDecimal.ZERO);
        result.put("topSupplier", "暂无供应商");
        result.put("topSupplierId", null);
        result.put("deliveryRate", BigDecimal.ZERO);
        result.put("series", monthAmountMap.values().stream().map(this::scaleAmount).toList());
        result.put("monthLabels", monthAmountMap.keySet().stream().map(this::toMonthLabel).toList());
        result.put("orderCount", 0);
        result.put("receivedQty", BigDecimal.ZERO);
        result.put("pendingReceiveQty", BigDecimal.ZERO);
        return result;
    }

    private Map<String, Object> buildEmptyInventorySummary(DateRange dateRange) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summaryLabel", buildSummaryLabel(dateRange, "所选期间库存快照", "库存快照"));
        result.put("rangeStart", dateRange.getStartDate());
        result.put("rangeEnd", dateRange.getEndDate());
        result.put("inventoryAmount", BigDecimal.ZERO);
        result.put("warningCount", 0);
        result.put("turnoverDays", BigDecimal.ZERO);
        result.put("inventoryQty", BigDecimal.ZERO);
        result.put("availableQty", BigDecimal.ZERO);
        return result;
    }

    private Map<String, Object> buildEmptyProductionSummary(DateRange dateRange) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summaryLabel", buildSummaryLabel(dateRange, "所选期间生产执行", "生产执行概览"));
        result.put("rangeStart", dateRange.getStartDate());
        result.put("rangeEnd", dateRange.getEndDate());
        result.put("workOrderCount", 0);
        result.put("onTimeRate", BigDecimal.ZERO);
        result.put("delayRiskCount", 0);
        result.put("completedCount", 0);
        result.put("inProgressCount", 0);
        return result;
    }

    private List<SalDocItem> listSalesItems(Collection<Long> docIds) {
        if (docIds == null || docIds.isEmpty()) {
            return List.of();
        }
        return salDocItemMapper.selectList(new LambdaQueryWrapper<SalDocItem>()
                .in(SalDocItem::getDocId, docIds));
    }

    private List<PurDocItem> listPurchaseItems(Collection<Long> docIds) {
        if (docIds == null || docIds.isEmpty()) {
            return List.of();
        }
        return purDocItemMapper.selectList(new LambdaQueryWrapper<PurDocItem>()
                .in(PurDocItem::getDocId, docIds));
    }

    private List<PrdReport> listReports(Collection<Long> workOrderIds) {
        if (workOrderIds == null || workOrderIds.isEmpty()) {
            return List.of();
        }
        return prdReportMapper.selectList(new LambdaQueryWrapper<PrdReport>()
                .in(PrdReport::getWorkOrderId, workOrderIds));
    }

    private Map<Long, BigDecimal> aggregateSalesAmountByMaterial(List<SalDocItem> items) {
        Map<Long, BigDecimal> amountMap = new HashMap<>();
        for (SalDocItem item : items) {
            if (item.getMaterialId() == null) {
                continue;
            }
            BigDecimal amount = defaultDecimal(item.getAmount());
            if (amount.compareTo(BigDecimal.ZERO) <= 0 && defaultDecimal(item.getPrice()).compareTo(BigDecimal.ZERO) > 0) {
                amount = defaultDecimal(item.getPrice()).multiply(defaultDecimal(item.getQty()));
            }
            amountMap.merge(item.getMaterialId(), amount, BigDecimal::add);
        }
        return amountMap;
    }

    private Map<Long, BigDecimal> loadMaterialPurchasePriceMap() {
        List<PurDoc> purchaseOrders = purDocMapper.selectList(new LambdaQueryWrapper<PurDoc>()
                .eq(PurDoc::getDocType, DOC_TYPE_ORDER));
        if (purchaseOrders.isEmpty()) {
            return Map.of();
        }
        List<PurDocItem> items = listPurchaseItems(purchaseOrders.stream().map(PurDoc::getId).toList());
        Map<Long, BigDecimal> totalAmountMap = new HashMap<>();
        Map<Long, BigDecimal> totalQtyMap = new HashMap<>();
        for (PurDocItem item : items) {
            if (item.getMaterialId() == null) {
                continue;
            }
            BigDecimal qty = defaultDecimal(item.getQty());
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal amount = defaultDecimal(item.getAmount());
            if (amount.compareTo(BigDecimal.ZERO) <= 0 && defaultDecimal(item.getPrice()).compareTo(BigDecimal.ZERO) > 0) {
                amount = defaultDecimal(item.getPrice()).multiply(qty);
            }
            totalAmountMap.merge(item.getMaterialId(), amount, BigDecimal::add);
            totalQtyMap.merge(item.getMaterialId(), qty, BigDecimal::add);
        }

        Map<Long, BigDecimal> priceMap = new HashMap<>();
        for (Map.Entry<Long, BigDecimal> entry : totalQtyMap.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            priceMap.put(entry.getKey(), totalAmountMap.getOrDefault(entry.getKey(), BigDecimal.ZERO)
                    .divide(entry.getValue(), 4, RoundingMode.HALF_UP));
        }
        return priceMap;
    }

    private BigDecimal calculateShipmentAmount(LocalDate startDate, LocalDate endDate) {
        DateRange dateRange = normalizeDateRange(startDate, endDate);
        List<SalDoc> scopedOrders = filterByDateRange(
                salDocMapper.selectList(new LambdaQueryWrapper<SalDoc>().eq(SalDoc::getDocType, DOC_TYPE_ORDER)),
                SalDoc::getDocDate,
                dateRange.getStartDate(),
                dateRange.getEndDate()
        );
        if (scopedOrders.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return listSalesItems(scopedOrders.stream().map(SalDoc::getId).toList()).stream()
                .map(item -> calculateFulfilledAmount(item.getAmount(), item.getPrice(), item.getQty(), item.getShippedQty()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean isCompletedWorkOrder(PrdWorkOrder item) {
        if (item == null) {
            return false;
        }
        return STATUS_COMPLETED.equalsIgnoreCase(item.getStatus())
                || defaultDecimal(item.getFinishedQty()).compareTo(defaultDecimal(item.getPlanQty())) >= 0;
    }

    private boolean isOnTime(PrdWorkOrder workOrder, List<PrdReport> reports) {
        if (workOrder.getEndDate() == null) {
            return true;
        }
        LocalDate actualFinishDate = reports.stream()
                .map(PrdReport::getReportDate)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .map(value -> value.toLocalDate())
                .orElse(workOrder.getEndDate());
        return !actualFinishDate.isAfter(workOrder.getEndDate());
    }

    private boolean hasDelayRisk(PrdWorkOrder workOrder) {
        if (workOrder.getEndDate() == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        if (workOrder.getEndDate().isBefore(today)) {
            return true;
        }
        BigDecimal completionRate = calculateRatio(defaultDecimal(workOrder.getFinishedQty()), defaultDecimal(workOrder.getPlanQty()), 4);
        return !workOrder.getEndDate().isAfter(today.plusDays(3)) && completionRate.compareTo(BigDecimal.valueOf(0.8)) < 0;
    }

    private boolean isWorkOrderWithinRange(PrdWorkOrder workOrder, LocalDate startDate, LocalDate endDate) {
        if (!hasDateFilter(startDate, endDate)) {
            return true;
        }
        LocalDate workStart = workOrder.getStartDate();
        LocalDate workEnd = workOrder.getEndDate();
        if (workStart == null && workEnd == null) {
            return false;
        }
        if (workStart == null) {
            workStart = workEnd;
        }
        if (workEnd == null) {
            workEnd = workStart;
        }
        if (startDate != null && workEnd.isBefore(startDate)) {
            return false;
        }
        return endDate == null || !workStart.isAfter(endDate);
    }

    private BigDecimal calculateOutstandingAmount(BigDecimal amount, BigDecimal price, BigDecimal totalQty, BigDecimal fulfilledQty) {
        BigDecimal qty = defaultDecimal(totalQty);
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal remainingQty = qty.subtract(defaultDecimal(fulfilledQty)).max(BigDecimal.ZERO);
        if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal lineAmount = defaultDecimal(amount);
        if (lineAmount.compareTo(BigDecimal.ZERO) <= 0 && defaultDecimal(price).compareTo(BigDecimal.ZERO) > 0) {
            lineAmount = defaultDecimal(price).multiply(qty);
        }
        return lineAmount.multiply(remainingQty).divide(qty, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateFulfilledAmount(BigDecimal amount, BigDecimal price, BigDecimal totalQty, BigDecimal fulfilledQty) {
        BigDecimal qty = defaultDecimal(totalQty);
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal actualQty = defaultDecimal(fulfilledQty).min(qty);
        BigDecimal lineAmount = defaultDecimal(amount);
        if (lineAmount.compareTo(BigDecimal.ZERO) <= 0 && defaultDecimal(price).compareTo(BigDecimal.ZERO) > 0) {
            lineAmount = defaultDecimal(price).multiply(qty);
        }
        return lineAmount.multiply(actualQty).divide(qty, 2, RoundingMode.HALF_UP);
    }

    private <T> List<T> filterByDateRange(List<T> source,
                                          Function<T, LocalDate> dateExtractor,
                                          LocalDate startDate,
                                          LocalDate endDate) {
        if (!hasDateFilter(startDate, endDate)) {
            return source;
        }
        return source.stream()
                .filter(item -> isWithinDateRange(dateExtractor.apply(item), startDate, endDate))
                .toList();
    }

    private boolean isWithinDateRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
        if (date == null) {
            return false;
        }
        if (startDate != null && date.isBefore(startDate)) {
            return false;
        }
        return endDate == null || !date.isAfter(endDate);
    }

    private Map<YearMonth, BigDecimal> initMonthAmountMap(LocalDate startDate, LocalDate endDate) {
        Map<YearMonth, BigDecimal> monthAmountMap = new LinkedHashMap<>();
        YearMonth startMonth = YearMonth.from(startDate);
        YearMonth endMonth = YearMonth.from(endDate);
        YearMonth current = startMonth;
        while (!current.isAfter(endMonth)) {
            monthAmountMap.put(current, BigDecimal.ZERO);
            current = current.plusMonths(1);
        }
        return monthAmountMap;
    }

    private LocalDate resolveSeriesStart(LocalDate startDate,
                                         LocalDate endDate,
                                         List<LocalDate> availableDates,
                                         LocalDate today) {
        if (startDate != null) {
            return startDate.withDayOfMonth(1);
        }
        if (endDate != null) {
            LocalDate fallback = availableDates.isEmpty() ? endDate.minusMonths(5) : availableDates.get(0);
            return fallback.withDayOfMonth(1);
        }
        return today.minusMonths(5).withDayOfMonth(1);
    }

    private LocalDate resolveSeriesEnd(LocalDate startDate,
                                       LocalDate endDate,
                                       List<LocalDate> availableDates,
                                       LocalDate today) {
        if (endDate != null) {
            return endDate.withDayOfMonth(endDate.lengthOfMonth());
        }
        if (startDate != null) {
            LocalDate fallback = availableDates.isEmpty() ? startDate : availableDates.get(availableDates.size() - 1);
            return fallback.withDayOfMonth(fallback.lengthOfMonth());
        }
        return today.withDayOfMonth(today.lengthOfMonth());
    }

    private String toMonthLabel(YearMonth month) {
        return month.getMonthValue() + "月";
    }

    private String buildSummaryLabel(DateRange dateRange, String rangedLabel, String defaultLabel) {
        return hasDateFilter(dateRange.getStartDate(), dateRange.getEndDate()) ? rangedLabel : defaultLabel;
    }

    private boolean hasDateFilter(LocalDate startDate, LocalDate endDate) {
        return startDate != null || endDate != null;
    }

    private long calculateTurnoverBaseDays(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return 0L;
        }
        return endDate.toEpochDay() - startDate.toEpochDay() + 1;
    }

    private Long resolveTopEntryId(Map<Long, BigDecimal> amountMap) {
        return amountMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private String resolveTopMaterialName(Map<Long, BigDecimal> amountMap,
                                          Map<Long, BasMaterial> materialMap,
                                          String fallback) {
        return amountMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .map(materialMap::get)
                .map(BasMaterial::getName)
                .filter(Objects::nonNull)
                .orElse(fallback);
    }

    private String resolveTopSupplierName(Map<Long, BigDecimal> amountMap,
                                          Map<Long, BasSupplier> supplierMap,
                                          String fallback) {
        return amountMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .map(supplierMap::get)
                .map(BasSupplier::getName)
                .filter(Objects::nonNull)
                .orElse(fallback);
    }

    private Map<Long, BasMaterial> loadMaterialMap(Collection<Long> materialIds) {
        if (materialIds == null || materialIds.isEmpty()) {
            return Map.of();
        }
        return basMaterialMapper.selectBatchIds(materialIds).stream()
                .collect(Collectors.toMap(BasMaterial::getId, item -> item, (left, right) -> left));
    }

    private Map<Long, BasCustomer> loadCustomerMap(Collection<Long> customerIds) {
        if (customerIds == null || customerIds.isEmpty()) {
            return Map.of();
        }
        return basCustomerMapper.selectBatchIds(customerIds).stream()
                .collect(Collectors.toMap(BasCustomer::getId, item -> item, (left, right) -> left));
    }

    private Map<Long, BasSupplier> loadSupplierMap(Collection<Long> supplierIds) {
        if (supplierIds == null || supplierIds.isEmpty()) {
            return Map.of();
        }
        return basSupplierMapper.selectBatchIds(supplierIds).stream()
                .collect(Collectors.toMap(BasSupplier::getId, item -> item, (left, right) -> left));
    }

    private int resolveRankingLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_TOP_LIMIT;
        }
        return Math.min(limit, MAX_TOP_LIMIT);
    }

    private DateRange normalizeDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            return new DateRange(endDate, startDate);
        }
        return new DateRange(startDate, endDate);
    }

    private BigDecimal calculateRatio(BigDecimal numerator, BigDecimal denominator, int scale) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return defaultDecimal(numerator).divide(denominator, scale, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal scaleAmount(BigDecimal value) {
        return defaultDecimal(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleQty(BigDecimal value) {
        return defaultDecimal(value).setScale(4, RoundingMode.HALF_UP);
    }

    private static final class DateRange {

        private final LocalDate startDate;
        private final LocalDate endDate;

        private DateRange(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }
    }

    private static final class OutstandingAggregate {

        private BigDecimal amount = BigDecimal.ZERO;
        private final Set<Long> docIds = new HashSet<>();
        private Long sourceDocId;
        private String sourceDocCode;
        private LocalDate sourceDocDate;

        private void accumulate(Long docId, String docCode, LocalDate docDate, BigDecimal lineAmount) {
            amount = amount.add(lineAmount);
            if (docId != null) {
                docIds.add(docId);
            }
            if (docDate != null && (sourceDocDate == null || docDate.isAfter(sourceDocDate))) {
                sourceDocDate = docDate;
                sourceDocId = docId;
                sourceDocCode = docCode;
            }
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public int getOrderCount() {
            return docIds.size();
        }

        public Long getSourceDocId() {
            return sourceDocId;
        }

        public String getSourceDocCode() {
            return sourceDocCode;
        }

        public LocalDate getSourceDocDate() {
            return sourceDocDate;
        }
    }

}
