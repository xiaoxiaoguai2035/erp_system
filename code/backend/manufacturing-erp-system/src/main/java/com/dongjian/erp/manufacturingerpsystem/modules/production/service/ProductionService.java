package com.dongjian.erp.manufacturingerpsystem.modules.production.service;

import com.dongjian.erp.manufacturingerpsystem.modules.production.dto.ProductionPlanSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.production.dto.MrpCalculateRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.production.dto.MrpGenerateRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.production.dto.ReportSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.production.dto.WorkOrderFinishInRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.production.dto.WorkOrderPickRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.production.dto.WorkOrderSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.common.page.PageResponse;
import com.dongjian.erp.manufacturingerpsystem.modules.production.vo.MrpResult;
import com.dongjian.erp.manufacturingerpsystem.modules.production.vo.ProductionPlanListItem;
import com.dongjian.erp.manufacturingerpsystem.modules.production.vo.ReportListItem;
import com.dongjian.erp.manufacturingerpsystem.modules.production.vo.WorkOrderListItem;
import com.dongjian.erp.manufacturingerpsystem.modules.production.vo.WorkOrderProgress;

import java.util.List;
import java.util.Map;

public interface ProductionService {

    PageResponse<ProductionPlanListItem> pagePlans(String keyword, String status, long pageNo, long pageSize);

    ProductionPlanListItem getPlan(Long id);

    Long createPlan(ProductionPlanSaveRequest request);

    void updatePlan(Long id, ProductionPlanSaveRequest request);

    void approvePlan(Long id);

    void closePlan(Long id);

    MrpResult calculateMrp(MrpCalculateRequest request);

    List<Long> generatePurchaseByMrp(MrpGenerateRequest request);

    List<Long> generateWorkOrdersByMrp(MrpGenerateRequest request);

    PageResponse<WorkOrderListItem> pageWorkOrders(String keyword, String status, long pageNo, long pageSize);

    WorkOrderListItem getWorkOrder(Long id);

    Long createWorkOrder(WorkOrderSaveRequest request);

    void updateWorkOrder(Long id, WorkOrderSaveRequest request);

    void approveWorkOrder(Long id);

    void closeWorkOrder(Long id);

    Long pickMaterials(Long id, WorkOrderPickRequest request);

    PageResponse<ReportListItem> pageReports(Long workOrderId, long pageNo, long pageSize);

    Long createReport(ReportSaveRequest request);

    Long finishIn(Long id, WorkOrderFinishInRequest request);

    WorkOrderProgress getProgress(Long id);
}
