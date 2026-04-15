package com.dongjian.erp.manufacturingerpsystem.modules.purchase.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public class PurchaseOrderSaveRequest {

    @NotNull(message = "供应商不能为空")
    private Long supplierId;

    @NotNull(message = "单据日期不能为空")
    private LocalDate docDate;

    private LocalDate expectedDate;
    private Long sourceDocId;
    private String sourceDocType;
    private String remark;

    @Valid
    @NotEmpty(message = "采购明细不能为空")
    private List<PurchaseOrderItemRequest> items;

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
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

    public List<PurchaseOrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<PurchaseOrderItemRequest> items) {
        this.items = items;
    }

    public static class PurchaseOrderItemRequest {

        @NotNull(message = "物料不能为空")
        private Long materialId;

        private Long warehouseId;

        @NotNull(message = "数量不能为空")
        private java.math.BigDecimal qty;

        @NotNull(message = "单价不能为空")
        private java.math.BigDecimal price;

        private LocalDate needDate;
        private Long sourceItemId;

        public Long getMaterialId() {
            return materialId;
        }

        public void setMaterialId(Long materialId) {
            this.materialId = materialId;
        }

        public Long getWarehouseId() {
            return warehouseId;
        }

        public void setWarehouseId(Long warehouseId) {
            this.warehouseId = warehouseId;
        }

        public java.math.BigDecimal getQty() {
            return qty;
        }

        public void setQty(java.math.BigDecimal qty) {
            this.qty = qty;
        }

        public java.math.BigDecimal getPrice() {
            return price;
        }

        public void setPrice(java.math.BigDecimal price) {
            this.price = price;
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
