package com.dongjian.erp.manufacturingerpsystem.modules.basic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class MaterialSaveRequest {

    @NotBlank(message = "物料编码不能为空")
    private String code;

    @NotBlank(message = "物料名称不能为空")
    private String name;

    private String spec;

    @NotBlank(message = "物料类型不能为空")
    private String materialType;

    @NotBlank(message = "计量单位不能为空")
    private String unitCode;

    @NotNull(message = "安全库存不能为空")
    private BigDecimal safetyStock;

    @NotNull(message = "批次管理标识不能为空")
    private Integer batchEnabled;

    private Long defaultWarehouseId;

    @NotBlank(message = "状态不能为空")
    private String status;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public String getMaterialType() {
        return materialType;
    }

    public void setMaterialType(String materialType) {
        this.materialType = materialType;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public void setUnitCode(String unitCode) {
        this.unitCode = unitCode;
    }

    public BigDecimal getSafetyStock() {
        return safetyStock;
    }

    public void setSafetyStock(BigDecimal safetyStock) {
        this.safetyStock = safetyStock;
    }

    public Integer getBatchEnabled() {
        return batchEnabled;
    }

    public void setBatchEnabled(Integer batchEnabled) {
        this.batchEnabled = batchEnabled;
    }

    public Long getDefaultWarehouseId() {
        return defaultWarehouseId;
    }

    public void setDefaultWarehouseId(Long defaultWarehouseId) {
        this.defaultWarehouseId = defaultWarehouseId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
