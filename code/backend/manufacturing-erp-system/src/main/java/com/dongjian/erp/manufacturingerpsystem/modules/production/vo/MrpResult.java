package com.dongjian.erp.manufacturingerpsystem.modules.production.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class MrpResult {
    private String taskKey;
    private List<PurchaseSuggestion> purchaseSuggestions;
    private List<WorkOrderSuggestion> workOrderSuggestions;
    public String getTaskKey() { return taskKey; }
    public void setTaskKey(String taskKey) { this.taskKey = taskKey; }
    public List<PurchaseSuggestion> getPurchaseSuggestions() { return purchaseSuggestions; }
    public void setPurchaseSuggestions(List<PurchaseSuggestion> purchaseSuggestions) { this.purchaseSuggestions = purchaseSuggestions; }
    public List<WorkOrderSuggestion> getWorkOrderSuggestions() { return workOrderSuggestions; }
    public void setWorkOrderSuggestions(List<WorkOrderSuggestion> workOrderSuggestions) { this.workOrderSuggestions = workOrderSuggestions; }

    public static class PurchaseSuggestion {
        private Long id;
        private Long planId;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private BigDecimal requiredQty;
        private BigDecimal availableQty;
        private BigDecimal shortageQty;
        private LocalDate needDate;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getPlanId() { return planId; }
        public void setPlanId(Long planId) { this.planId = planId; }
        public Long getMaterialId() { return materialId; }
        public void setMaterialId(Long materialId) { this.materialId = materialId; }
        public String getMaterialCode() { return materialCode; }
        public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }
        public String getMaterialName() { return materialName; }
        public void setMaterialName(String materialName) { this.materialName = materialName; }
        public BigDecimal getRequiredQty() { return requiredQty; }
        public void setRequiredQty(BigDecimal requiredQty) { this.requiredQty = requiredQty; }
        public BigDecimal getAvailableQty() { return availableQty; }
        public void setAvailableQty(BigDecimal availableQty) { this.availableQty = availableQty; }
        public BigDecimal getShortageQty() { return shortageQty; }
        public void setShortageQty(BigDecimal shortageQty) { this.shortageQty = shortageQty; }
        public LocalDate getNeedDate() { return needDate; }
        public void setNeedDate(LocalDate needDate) { this.needDate = needDate; }
    }

    public static class WorkOrderSuggestion {
        private Long id;
        private Long planId;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private Long bomId;
        private Long routeId;
        private BigDecimal planQty;
        private LocalDate startDate;
        private LocalDate endDate;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getPlanId() { return planId; }
        public void setPlanId(Long planId) { this.planId = planId; }
        public Long getMaterialId() { return materialId; }
        public void setMaterialId(Long materialId) { this.materialId = materialId; }
        public String getMaterialCode() { return materialCode; }
        public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }
        public String getMaterialName() { return materialName; }
        public void setMaterialName(String materialName) { this.materialName = materialName; }
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
    }
}
