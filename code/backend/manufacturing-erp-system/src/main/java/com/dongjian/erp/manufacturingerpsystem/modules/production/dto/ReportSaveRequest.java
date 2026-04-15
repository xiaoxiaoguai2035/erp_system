package com.dongjian.erp.manufacturingerpsystem.modules.production.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReportSaveRequest {
    @NotNull(message = "工单不能为空")
    private Long workOrderId;
    @NotNull(message = "工序不能为空")
    private Long processItemId;
    @NotNull(message = "报工时间不能为空")
    private LocalDateTime reportDate;
    @NotNull(message = "报工数量不能为空")
    @DecimalMin(value = "0.0001", message = "报工数量必须大于0")
    private BigDecimal reportQty;
    @NotNull(message = "合格数量不能为空")
    @DecimalMin(value = "0", message = "合格数量不能小于0")
    private BigDecimal qualifiedQty;
    @NotNull(message = "不良数量不能为空")
    @DecimalMin(value = "0", message = "不良数量不能小于0")
    private BigDecimal defectiveQty;
    private String reporterName;
    private String remark;
    public Long getWorkOrderId() { return workOrderId; }
    public void setWorkOrderId(Long workOrderId) { this.workOrderId = workOrderId; }
    public Long getProcessItemId() { return processItemId; }
    public void setProcessItemId(Long processItemId) { this.processItemId = processItemId; }
    public LocalDateTime getReportDate() { return reportDate; }
    public void setReportDate(LocalDateTime reportDate) { this.reportDate = reportDate; }
    public BigDecimal getReportQty() { return reportQty; }
    public void setReportQty(BigDecimal reportQty) { this.reportQty = reportQty; }
    public BigDecimal getQualifiedQty() { return qualifiedQty; }
    public void setQualifiedQty(BigDecimal qualifiedQty) { this.qualifiedQty = qualifiedQty; }
    public BigDecimal getDefectiveQty() { return defectiveQty; }
    public void setDefectiveQty(BigDecimal defectiveQty) { this.defectiveQty = defectiveQty; }
    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
