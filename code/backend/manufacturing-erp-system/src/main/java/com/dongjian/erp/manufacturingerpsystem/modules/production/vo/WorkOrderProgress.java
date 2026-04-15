package com.dongjian.erp.manufacturingerpsystem.modules.production.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class WorkOrderProgress {
    private Long workOrderId;
    private String workOrderCode;
    private BigDecimal planQty;
    private BigDecimal reportedQty;
    private BigDecimal qualifiedQty;
    private BigDecimal defectiveQty;
    private BigDecimal finishInQty;
    private BigDecimal completionRate;
    private List<MaterialProgressItem> materials;
    private List<ProcessProgressItem> processes;
    public Long getWorkOrderId() { return workOrderId; }
    public void setWorkOrderId(Long workOrderId) { this.workOrderId = workOrderId; }
    public String getWorkOrderCode() { return workOrderCode; }
    public void setWorkOrderCode(String workOrderCode) { this.workOrderCode = workOrderCode; }
    public BigDecimal getPlanQty() { return planQty; }
    public void setPlanQty(BigDecimal planQty) { this.planQty = planQty; }
    public BigDecimal getReportedQty() { return reportedQty; }
    public void setReportedQty(BigDecimal reportedQty) { this.reportedQty = reportedQty; }
    public BigDecimal getQualifiedQty() { return qualifiedQty; }
    public void setQualifiedQty(BigDecimal qualifiedQty) { this.qualifiedQty = qualifiedQty; }
    public BigDecimal getDefectiveQty() { return defectiveQty; }
    public void setDefectiveQty(BigDecimal defectiveQty) { this.defectiveQty = defectiveQty; }
    public BigDecimal getFinishInQty() { return finishInQty; }
    public void setFinishInQty(BigDecimal finishInQty) { this.finishInQty = finishInQty; }
    public BigDecimal getCompletionRate() { return completionRate; }
    public void setCompletionRate(BigDecimal completionRate) { this.completionRate = completionRate; }
    public List<MaterialProgressItem> getMaterials() { return materials; }
    public void setMaterials(List<MaterialProgressItem> materials) { this.materials = materials; }
    public List<ProcessProgressItem> getProcesses() { return processes; }
    public void setProcesses(List<ProcessProgressItem> processes) { this.processes = processes; }

    public static class MaterialProgressItem {
        private Long materialId;
        private String materialCode;
        private String materialName;
        private BigDecimal requiredQty;
        private BigDecimal pickedQty;
        private BigDecimal pickRate;
        public Long getMaterialId() { return materialId; }
        public void setMaterialId(Long materialId) { this.materialId = materialId; }
        public String getMaterialCode() { return materialCode; }
        public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }
        public String getMaterialName() { return materialName; }
        public void setMaterialName(String materialName) { this.materialName = materialName; }
        public BigDecimal getRequiredQty() { return requiredQty; }
        public void setRequiredQty(BigDecimal requiredQty) { this.requiredQty = requiredQty; }
        public BigDecimal getPickedQty() { return pickedQty; }
        public void setPickedQty(BigDecimal pickedQty) { this.pickedQty = pickedQty; }
        public BigDecimal getPickRate() { return pickRate; }
        public void setPickRate(BigDecimal pickRate) { this.pickRate = pickRate; }
    }

    public static class ProcessProgressItem {
        private Long processItemId;
        private String processCode;
        private String processName;
        private BigDecimal reportQty;
        private BigDecimal qualifiedQty;
        private BigDecimal defectiveQty;
        private BigDecimal completionRate;
        private LocalDateTime latestReportTime;
        public Long getProcessItemId() { return processItemId; }
        public void setProcessItemId(Long processItemId) { this.processItemId = processItemId; }
        public String getProcessCode() { return processCode; }
        public void setProcessCode(String processCode) { this.processCode = processCode; }
        public String getProcessName() { return processName; }
        public void setProcessName(String processName) { this.processName = processName; }
        public BigDecimal getReportQty() { return reportQty; }
        public void setReportQty(BigDecimal reportQty) { this.reportQty = reportQty; }
        public BigDecimal getQualifiedQty() { return qualifiedQty; }
        public void setQualifiedQty(BigDecimal qualifiedQty) { this.qualifiedQty = qualifiedQty; }
        public BigDecimal getDefectiveQty() { return defectiveQty; }
        public void setDefectiveQty(BigDecimal defectiveQty) { this.defectiveQty = defectiveQty; }
        public BigDecimal getCompletionRate() { return completionRate; }
        public void setCompletionRate(BigDecimal completionRate) { this.completionRate = completionRate; }
        public LocalDateTime getLatestReportTime() { return latestReportTime; }
        public void setLatestReportTime(LocalDateTime latestReportTime) { this.latestReportTime = latestReportTime; }
    }
}
