package com.dongjian.erp.manufacturingerpsystem.modules.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("prd_report")
public class PrdReport {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workOrderId;
    private Long processItemId;
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
