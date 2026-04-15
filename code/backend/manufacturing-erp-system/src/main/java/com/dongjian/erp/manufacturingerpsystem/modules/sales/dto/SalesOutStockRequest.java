package com.dongjian.erp.manufacturingerpsystem.modules.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class SalesOutStockRequest {

    @NotNull(message = "出库仓库不能为空")
    private Long warehouseId;

    @NotNull(message = "业务日期不能为空")
    private LocalDate bizDate;

    private String remark;

    @Valid
    @NotEmpty(message = "出库明细不能为空")
    private List<SalesOutStockItemRequest> items;

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public LocalDate getBizDate() {
        return bizDate;
    }

    public void setBizDate(LocalDate bizDate) {
        this.bizDate = bizDate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public List<SalesOutStockItemRequest> getItems() {
        return items;
    }

    public void setItems(List<SalesOutStockItemRequest> items) {
        this.items = items;
    }

    public static class SalesOutStockItemRequest {
        @NotNull(message = "来源明细不能为空")
        private Long sourceItemId;
        @NotNull(message = "物料不能为空")
        private Long materialId;
        private String lotNo;
        @NotNull(message = "出库数量不能为空")
        @DecimalMin(value = "0.0001", message = "出库数量必须大于0")
        private BigDecimal qty;

        public Long getSourceItemId() {
            return sourceItemId;
        }

        public void setSourceItemId(Long sourceItemId) {
            this.sourceItemId = sourceItemId;
        }

        public Long getMaterialId() {
            return materialId;
        }

        public void setMaterialId(Long materialId) {
            this.materialId = materialId;
        }

        public String getLotNo() {
            return lotNo;
        }

        public void setLotNo(String lotNo) {
            this.lotNo = lotNo;
        }

        public BigDecimal getQty() {
            return qty;
        }

        public void setQty(BigDecimal qty) {
            this.qty = qty;
        }
    }
}
