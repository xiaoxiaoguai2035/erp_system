package com.dongjian.erp.manufacturingerpsystem.modules.sales.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class SalesOrderDetail {

    private Long id;
    private String code;
    private String docType;
    private Long customerId;
    private String customerName;
    private LocalDate docDate;
    private LocalDate deliveryDate;
    private BigDecimal totalAmount;
    private String status;
    private Long sourceDocId;
    private String sourceDocType;
    private String remark;
    private List<SalesOrderDetailItem> items;

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

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public LocalDate getDocDate() {
        return docDate;
    }

    public void setDocDate(LocalDate docDate) {
        this.docDate = docDate;
    }

    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(LocalDate deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getSourceDocId() {
        return sourceDocId;
    }

    public void setSourceDocId(Long sourceDocId) {
        this.sourceDocId = sourceDocId;
    }

    public String getSourceDocType() {
        return sourceDocType;
    }

    public void setSourceDocType(String sourceDocType) {
        this.sourceDocType = sourceDocType;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public List<SalesOrderDetailItem> getItems() {
        return items;
    }

    public void setItems(List<SalesOrderDetailItem> items) {
        this.items = items;
    }

    public static class SalesOrderDetailItem {

        private Long id;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private BigDecimal qty;
        private BigDecimal shippedQty;
        private BigDecimal convertedQty;
        private BigDecimal remainConvertQty;
        private BigDecimal plannedQty;
        private BigDecimal remainPlanQty;
        private BigDecimal price;
        private BigDecimal amount;
        private Long sourceItemId;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
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

        public BigDecimal getQty() {
            return qty;
        }

        public void setQty(BigDecimal qty) {
            this.qty = qty;
        }

        public BigDecimal getShippedQty() {
            return shippedQty;
        }

        public void setShippedQty(BigDecimal shippedQty) {
            this.shippedQty = shippedQty;
        }

        public BigDecimal getConvertedQty() {
            return convertedQty;
        }

        public void setConvertedQty(BigDecimal convertedQty) {
            this.convertedQty = convertedQty;
        }

        public BigDecimal getRemainConvertQty() {
            return remainConvertQty;
        }

        public void setRemainConvertQty(BigDecimal remainConvertQty) {
            this.remainConvertQty = remainConvertQty;
        }

        public BigDecimal getPlannedQty() {
            return plannedQty;
        }

        public void setPlannedQty(BigDecimal plannedQty) {
            this.plannedQty = plannedQty;
        }

        public BigDecimal getRemainPlanQty() {
            return remainPlanQty;
        }

        public void setRemainPlanQty(BigDecimal remainPlanQty) {
            this.remainPlanQty = remainPlanQty;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public Long getSourceItemId() {
            return sourceItemId;
        }

        public void setSourceItemId(Long sourceItemId) {
            this.sourceItemId = sourceItemId;
        }
    }
}
