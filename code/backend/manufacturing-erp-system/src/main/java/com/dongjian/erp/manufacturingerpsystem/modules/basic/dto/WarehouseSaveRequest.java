package com.dongjian.erp.manufacturingerpsystem.modules.basic.dto;

import jakarta.validation.constraints.NotBlank;

public class WarehouseSaveRequest {

    @NotBlank(message = "仓库编码不能为空")
    private String code;

    @NotBlank(message = "仓库名称不能为空")
    private String name;

    private String warehouseType;
    private String managerName;

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

    public String getWarehouseType() {
        return warehouseType;
    }

    public void setWarehouseType(String warehouseType) {
        this.warehouseType = warehouseType;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
