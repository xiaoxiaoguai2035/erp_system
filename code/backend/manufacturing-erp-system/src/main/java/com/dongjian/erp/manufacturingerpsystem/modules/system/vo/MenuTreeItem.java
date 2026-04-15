package com.dongjian.erp.manufacturingerpsystem.modules.system.vo;

import java.util.ArrayList;
import java.util.List;

public class MenuTreeItem {

    private Long id;
    private Long parentId;
    private String name;
    private String path;
    private String component;
    private String menuType;
    private String permissionCode;
    private String status;
    private List<MenuTreeItem> children = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public List<MenuTreeItem> getChildren() {
        return children;
    }

    public void setChildren(List<MenuTreeItem> children) {
        this.children = children;
    }
}
