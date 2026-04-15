package com.dongjian.erp.manufacturingerpsystem.modules.system.service;

import com.dongjian.erp.manufacturingerpsystem.common.page.PageResponse;
import com.dongjian.erp.manufacturingerpsystem.modules.system.dto.MenuSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.system.dto.ResetPasswordRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.system.dto.RoleMenusRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.system.dto.RoleSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.system.dto.UserSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.system.dto.UserStatusRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.system.vo.MenuTreeItem;
import com.dongjian.erp.manufacturingerpsystem.modules.system.vo.RoleDetail;
import com.dongjian.erp.manufacturingerpsystem.modules.system.vo.RoleListItem;
import com.dongjian.erp.manufacturingerpsystem.modules.system.vo.UserListItem;

import java.util.List;
import java.util.Map;

public interface SystemManageService {

    PageResponse<UserListItem> pageUsers(String keyword, String status, long pageNo, long pageSize);

    UserListItem getUser(Long id);

    Long createUser(UserSaveRequest request);

    void updateUser(Long id, UserSaveRequest request);

    void deleteUser(Long id);

    void updateUserStatus(Long id, UserStatusRequest request);

    void resetPassword(Long id, ResetPasswordRequest request);

    List<RoleListItem> listRoles(String status);

    RoleDetail getRole(Long id);

    Long createRole(RoleSaveRequest request);

    void updateRole(Long id, RoleSaveRequest request);

    void deleteRole(Long id);

    void updateRoleMenus(Long id, RoleMenusRequest request);

    List<MenuTreeItem> getMenuTree();

    MenuTreeItem getMenu(Long id);

    Long createMenu(MenuSaveRequest request);

    void updateMenu(Long id, MenuSaveRequest request);

    void deleteMenu(Long id);

    List<Map<String, Object>> getRoleMenus(Long roleId);
}
