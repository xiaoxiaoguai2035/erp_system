package com.dongjian.erp.manufacturingerpsystem.modules.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public class SalesOrderSaveRequest {

    @NotNull(message = "客户不能为空")
    private Long customerId;

    @NotNull(message = "单据日期不能为空")
    private LocalDate docDate;

    private LocalDate deliveryDate;
    private Long sourceDocId;
    private String sourceDocType;
    private String remark;

    @Valid
    @NotEmpty(message = "销售明细不能为空")
    private List<SalesOrderItemRequest> items;

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
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

    public List<SalesOrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<SalesOrderItemRequest> items) {
        this.items = items;
    }

    public static class SalesOrderItemRequest {

        @NotNull(message = "产品不能为空")
        private Long materialId;

        @NotNull(message = "数量不能为空")
        private java.math.BigDecimal qty;

        @NotNull(message = "单价不能为空")
        private java.math.BigDecimal price;
        private Long sourceItemId;

        public Long getMaterialId() {
            return materialId;
        }

        public void setMaterialId(Long materialId) {
            this.materialId = materialId;
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

        public Long getSourceItemId() {
            return sourceItemId;
        }

        public void setSourceItemId(Long sourceItemId) {
            this.sourceItemId = sourceItemId;
        }
    }
}
