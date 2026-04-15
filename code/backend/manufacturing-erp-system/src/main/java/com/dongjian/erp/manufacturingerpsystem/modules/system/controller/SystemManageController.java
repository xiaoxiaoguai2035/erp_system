package com.dongjian.erp.manufacturingerpsystem.modules.system.controller;

import com.dongjian.erp.manufacturingerpsystem.common.page.PageResponse;
import com.dongjian.erp.manufacturingerpsystem.common.result.ApiResponse;
import com.dongjian.erp.manufacturingerpsystem.modules.system.dto.MenuSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.system.dto.ResetPasswordRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.system.dto.RoleMenusRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.system.dto.RoleSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.system.dto.UserSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.system.dto.UserStatusRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.system.service.SystemManageService;
import com.dongjian.erp.manufacturingerpsystem.modules.system.vo.MenuTreeItem;
import com.dongjian.erp.manufacturingerpsystem.modules.system.vo.RoleDetail;
import com.dongjian.erp.manufacturingerpsystem.modules.system.vo.RoleListItem;
import com.dongjian.erp.manufacturingerpsystem.modules.system.vo.UserListItem;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system")
public class SystemManageController {

    private final SystemManageService systemManageService;

    public SystemManageController(SystemManageService systemManageService) {
        this.systemManageService = systemManageService;
    }

    @GetMapping("/users")
    public ApiResponse<PageResponse<UserListItem>> users(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ApiResponse.success(systemManageService.pageUsers(keyword, status, pageNo, pageSize));
    }

    @GetMapping("/users/{id}")
    public ApiResponse<UserListItem> user(@PathVariable Long id) {
        return ApiResponse.success(systemManageService.getUser(id));
    }

    @PostMapping("/users")
    public ApiResponse<Long> createUser(@Valid @RequestBody UserSaveRequest request) {
        return ApiResponse.success("created", systemManageService.createUser(request));
    }

    @PutMapping("/users/{id}")
    public ApiResponse<Void> updateUser(@PathVariable Long id, @Valid @RequestBody UserSaveRequest request) {
        systemManageService.updateUser(id, request);
        return ApiResponse.success("updated", null);
    }

    @DeleteMapping("/users/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        systemManageService.deleteUser(id);
        return ApiResponse.success("deleted", null);
    }

    @PutMapping("/users/{id}/status")
    public ApiResponse<Void> updateUserStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        systemManageService.updateUserStatus(id, request);
        return ApiResponse.success("updated", null);
    }

    @PutMapping("/users/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        systemManageService.resetPassword(id, request);
        return ApiResponse.success("updated", null);
    }

    @GetMapping("/roles")
    public ApiResponse<List<RoleListItem>> roles(@RequestParam(required = false) String status) {
        return ApiResponse.success(systemManageService.listRoles(status));
    }

    @GetMapping("/roles/{id}")
    public ApiResponse<RoleDetail> role(@PathVariable Long id) {
        return ApiResponse.success(systemManageService.getRole(id));
    }

    @PostMapping("/roles")
    public ApiResponse<Long> createRole(@Valid @RequestBody RoleSaveRequest request) {
        return ApiResponse.success("created", systemManageService.createRole(request));
    }

    @PutMapping("/roles/{id}")
    public ApiResponse<Void> updateRole(@PathVariable Long id, @Valid @RequestBody RoleSaveRequest request) {
        systemManageService.updateRole(id, request);
        return ApiResponse.success("updated", null);
    }

    @DeleteMapping("/roles/{id}")
    public ApiResponse<Void> deleteRole(@PathVariable Long id) {
        systemManageService.deleteRole(id);
        return ApiResponse.success("deleted", null);
    }

    @PutMapping("/roles/{id}/menus")
    public ApiResponse<Void> updateRoleMenus(@PathVariable Long id, @Valid @RequestBody RoleMenusRequest request) {
        systemManageService.updateRoleMenus(id, request);
        return ApiResponse.success("updated", null);
    }

    @GetMapping("/menus/tree")
    public ApiResponse<List<MenuTreeItem>> menuTree() {
        return ApiResponse.success(systemManageService.getMenuTree());
    }

    @GetMapping("/menus/{id}")
    public ApiResponse<MenuTreeItem> menu(@PathVariable Long id) {
        return ApiResponse.success(systemManageService.getMenu(id));
    }

    @PostMapping("/menus")
    public ApiResponse<Long> createMenu(@Valid @RequestBody MenuSaveRequest request) {
        return ApiResponse.success("created", systemManageService.createMenu(request));
    }

    @PutMapping("/menus/{id}")
    public ApiResponse<Void> updateMenu(@PathVariable Long id, @Valid @RequestBody MenuSaveRequest request) {
        systemManageService.updateMenu(id, request);
        return ApiResponse.success("updated", null);
    }

    @DeleteMapping("/menus/{id}")
    public ApiResponse<Void> deleteMenu(@PathVariable Long id) {
        systemManageService.deleteMenu(id);
        return ApiResponse.success("deleted", null);
    }
}
