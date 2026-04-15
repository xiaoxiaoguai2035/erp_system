package com.dongjian.erp.manufacturingerpsystem.modules.production.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ProductionPlanListItem {

    private Long id;
    private String code;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private BigDecimal planQty;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long sourceSalesId;
    private String status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public String getMaterialCode() {
        return materialCode;
    }

    public void setMaterialCode(String materialCode) {
        this.materialCode = materialCode;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
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
