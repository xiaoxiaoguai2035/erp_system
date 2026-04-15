package com.dongjian.erp.manufacturingerpsystem.modules.basic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;

@TableName("bas_material")
public class BasMaterial {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String spec;
    private String materialType;
    private String unitCode;
    private BigDecimal safetyStock;
    private Integer batchEnabled;
    private Long defaultWarehouseId;
    private String status;

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
