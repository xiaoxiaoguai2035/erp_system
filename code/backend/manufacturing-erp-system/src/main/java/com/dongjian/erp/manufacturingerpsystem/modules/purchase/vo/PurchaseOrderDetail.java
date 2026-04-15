package com.dongjian.erp.manufacturingerpsystem.modules.purchase.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class PurchaseOrderDetail {

    private Long id;
    private String code;
    private String docType;
    private Long supplierId;
    private String supplierName;
    private LocalDate docDate;
    private LocalDate expectedDate;
    private BigDecimal totalAmount;
    private String status;
    private Long sourceDocId;
    private String sourceDocType;
    private String remark;
    private List<PurchaseOrderDetailItem> items;

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

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public LocalDate getDocDate() {
        return docDate;
    }

    public void setDocDate(LocalDate docDate) {
        this.docDate = docDate;
    }

    public LocalDate getExpectedDate() {
        return expectedDate;
    }

    public void setExpectedDate(LocalDate expectedDate) {
        this.expectedDate = expectedDate;
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

    public List<PurchaseOrderDetailItem> getItems() {
        return items;
    }

    public void setItems(List<PurchaseOrderDetailItem> items) {
        this.items = items;
    }

    public static class PurchaseOrderDetailItem {

        private Long id;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private Long warehouseId;
        private String warehouseName;
        private BigDecimal qty;
        private BigDecimal receivedQty;
        private BigDecimal qualifiedQty;
        private BigDecimal convertedQty;
        private BigDecimal remainConvertQty;
        private BigDecimal price;
        private BigDecimal amount;
        private LocalDate needDate;
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

        public Long getWarehouseId() {
            return warehouseId;
        }

        public void setWarehouseId(Long warehouseId) {
            this.warehouseId = warehouseId;
        }

        public String getWarehouseName() {
            return warehouseName;
        }

        public void setWarehouseName(String warehouseName) {
            this.warehouseName = warehouseName;
        }

        public BigDecimal getQty() {
            return qty;
        }

        public void setQty(BigDecimal qty) {
            this.qty = qty;
        }

        public BigDecimal getReceivedQty() {
            return receivedQty;
        }

        public void setReceivedQty(BigDecimal receivedQty) {
            this.receivedQty = receivedQty;
        }

        public BigDecimal getQualifiedQty() {
            return qualifiedQty;
        }

        public void setQualifiedQty(BigDecimal qualifiedQty) {
            this.qualifiedQty = qualifiedQty;
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

        public LocalDate getNeedDate() {
            return needDate;
        }

        public void setNeedDate(LocalDate needDate) {
            this.needDate = needDate;
        }

        public Long getSourceItemId() {
            return sourceItemId;
        }

        public void setSourceItemId(Long sourceItemId) {
            this.sourceItemId = sourceItemId;
        }
    }
}
