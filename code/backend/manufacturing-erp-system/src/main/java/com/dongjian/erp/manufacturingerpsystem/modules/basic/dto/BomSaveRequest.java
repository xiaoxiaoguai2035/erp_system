package com.dongjian.erp.manufacturingerpsystem.modules.basic.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class BomSaveRequest {

    @NotNull(message = "产品物料不能为空")
    private Long productId;

    private String versionNo;
    private String status;
    private LocalDate effectiveDate;

    @Valid
    @NotEmpty(message = "BOM 明细不能为空")
    private List<BomItemRequest> items;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(String versionNo) {
        this.versionNo = versionNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public List<BomItemRequest> getItems() {
        return items;
    }

    public void setItems(List<BomItemRequest> items) {
        this.items = items;
    }

    public static class BomItemRequest {

        @NotNull(message = "子项物料不能为空")
        private Long materialId;

        @NotNull(message = "用量不能为空")
        private BigDecimal qty;

        private BigDecimal lossRate;
        private Integer sortNo;

        public Long getMaterialId() {
            return materialId;
        }

        public void setMaterialId(Long materialId) {
            this.materialId = materialId;
        }

        public BigDecimal getQty() {
            return qty;
        }

        public void setQty(BigDecimal qty) {
            this.qty = qty;
        }

        public BigDecimal getLossRate() {
            return lossRate;
        }

        public void setLossRate(BigDecimal lossRate) {
            this.lossRate = lossRate;
        }

        public Integer getSortNo() {
            return sortNo;
        }

        public void setSortNo(Integer sortNo) {
            this.sortNo = sortNo;
        }
    }
}
