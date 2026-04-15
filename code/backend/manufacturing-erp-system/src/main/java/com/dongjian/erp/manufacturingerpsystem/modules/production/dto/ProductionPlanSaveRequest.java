package com.dongjian.erp.manufacturingerpsystem.modules.production.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ProductionPlanSaveRequest {

    @NotNull(message = "产品不能为空")
    private Long materialId;

    @NotNull(message = "计划数量不能为空")
    private BigDecimal planQty;

    @NotNull(message = "计划开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "计划结束日期不能为空")
    private LocalDate endDate;

    private Long sourceSalesId;
    private String status;

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
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

    public Long getSourceSalesId() {
        return sourceSalesId;
    }

    public void setSourceSalesId(Long sourceSalesId) {
        this.sourceSalesId = sourceSalesId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
