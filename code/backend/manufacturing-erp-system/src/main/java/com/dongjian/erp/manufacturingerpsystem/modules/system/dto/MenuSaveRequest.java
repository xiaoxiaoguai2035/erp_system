package com.dongjian.erp.manufacturingerpsystem.modules.system.dto;

import jakarta.validation.constraints.NotBlank;

public class MenuSaveRequest {

    private Long parentId;

    @NotBlank(message = "菜单名称不能为空")
    private String name;

    private String path;
    private String component;

    @NotBlank(message = "菜单类型不能为空")
    private String menuType;

    private String permissionCode;

    @NotBlank(message = "状态不能为空")
    private String status;

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getMenuType() {
        return menuType;
    }

    public void setMenuType(String menuType) {
        this.menuType = menuType;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
