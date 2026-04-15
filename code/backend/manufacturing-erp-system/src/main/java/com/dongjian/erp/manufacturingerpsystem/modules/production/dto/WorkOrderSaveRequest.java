package com.dongjian.erp.manufacturingerpsystem.modules.production.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class WorkOrderSaveRequest {
    @NotNull(message = "来源计划不能为空")
    private Long planId;

    @NotNull(message = "产品不能为空")
    private Long materialId;

    @NotNull(message = "BOM不能为空")
    private Long bomId;

    @NotNull(message = "工艺路线不能为空")
    private Long routeId;

    @NotNull(message = "计划数量不能为空")
    @DecimalMin(value = "0.0001", message = "计划数量必须大于0")
    private BigDecimal planQty;

    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }
    public Long getBomId() { return bomId; }
    public void setBomId(Long bomId) { this.bomId = bomId; }
    public Long getRouteId() { return routeId; }
    public void setRouteId(Long routeId) { this.routeId = routeId; }
    public BigDecimal getPlanQty() { return planQty; }
    public void setPlanQty(BigDecimal planQty) { this.planQty = planQty; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
