package com.dongjian.erp.manufacturingerpsystem.modules.production.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReportListItem {
    private Long id;
    private Long workOrderId;
    private String workOrderCode;
    private Long processItemId;
    private String processName;
    private LocalDateTime reportDate;
    private BigDecimal reportQty;
    private BigDecimal qualifiedQty;
    private BigDecimal defectiveQty;
    private String reporterName;
    private String remark;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getWorkOrderId() { return workOrderId; }
    public void setWorkOrderId(Long workOrderId) { this.workOrderId = workOrderId; }
    public String getWorkOrderCode() { return workOrderCode; }
    public void setWorkOrderCode(String workOrderCode) { this.workOrderCode = workOrderCode; }
    public Long getProcessItemId() { return processItemId; }
    public void setProcessItemId(Long processItemId) { this.processItemId = processItemId; }
    public String getProcessName() { return processName; }
    public void setProcessName(String processName) { this.processName = processName; }
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
