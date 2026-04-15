package com.dongjian.erp.manufacturingerpsystem.modules.basic.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class BomDetail {

    private Long id;
    private Long productId;
    private String productCode;
    private String productName;
    private String versionNo;
    private String status;
    private LocalDate effectiveDate;
    private List<BomDetailItem> items;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
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

    public List<BomDetailItem> getItems() {
        return items;
    }

    public void setItems(List<BomDetailItem> items) {
        this.items = items;
    }

    public static class BomDetailItem {

        private Long id;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private BigDecimal qty;
        private BigDecimal lossRate;
        private Integer sortNo;

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
