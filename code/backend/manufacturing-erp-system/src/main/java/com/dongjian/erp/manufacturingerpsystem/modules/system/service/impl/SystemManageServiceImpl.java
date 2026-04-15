package com.dongjian.erp.manufacturingerpsystem.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dongjian.erp.manufacturingerpsystem.common.exception.BusinessException;
import com.dongjian.erp.manufacturingerpsystem.common.page.PageResponse;
import com.dongjian.erp.manufacturingerpsystem.modules.system.dto.MenuSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.system.dto.ResetPasswordRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.system.dto.RoleMenusRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.system.dto.RoleSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.system.dto.UserSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.system.dto.UserStatusRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.system.entity.SysMenu;
import com.dongjian.erp.manufacturingerpsystem.modules.system.entity.SysRole;
import com.dongjian.erp.manufacturingerpsystem.modules.system.entity.SysRoleMenu;
import com.dongjian.erp.manufacturingerpsystem.modules.system.entity.SysUser;
import com.dongjian.erp.manufacturingerpsystem.modules.system.mapper.SysMenuMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.system.mapper.SysRoleMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.system.mapper.SysRoleMenuMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.system.mapper.SysUserMapper;
import com.dongjian.erp.manufacturingerpsystem.modules.system.service.SystemManageService;
import com.dongjian.erp.manufacturingerpsystem.modules.system.vo.MenuTreeItem;
import com.dongjian.erp.manufacturingerpsystem.modules.system.vo.RoleDetail;
import com.dongjian.erp.manufacturingerpsystem.modules.system.vo.RoleListItem;
import com.dongjian.erp.manufacturingerpsystem.modules.system.vo.UserListItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SystemManageServiceImpl implements SystemManageService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;

    public SystemManageServiceImpl(SysUserMapper sysUserMapper,
                                   SysRoleMapper sysRoleMapper,
                                   SysMenuMapper sysMenuMapper,
                                   SysRoleMenuMapper sysRoleMenuMapper) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysMenuMapper = sysMenuMapper;
        this.sysRoleMenuMapper = sysRoleMenuMapper;
    }

    @Override
    public PageResponse<UserListItem> pageUsers(String keyword, String status, long pageNo, long pageSize) {
        Page<SysUser> page = sysUserMapper.selectPage(
                new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getDeleted, 0)
                        .and(StringUtils.hasText(keyword), wrapper -> wrapper
                                .like(SysUser::getUsername, keyword)
                                .or()
                                .like(SysUser::getRealName, keyword))
                        .eq(StringUtils.hasText(status), SysUser::getStatus, status)
                        .orderByDesc(SysUser::getId)
        );

        Map<Long, String> roleNameMap = buildRoleNameMap(page.getRecords().stream()
                .map(SysUser::getRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        List<UserListItem> records = page.getRecords().stream().map(user -> toUserItem(user, roleNameMap)).toList();
        return PageResponse.of(page.getTotal(), pageNo, pageSize, records);
    }

    @Override
    public UserListItem getUser(Long id) {
        SysUser user = requireUser(id);
        return toUserItem(user, buildRoleNameMap(Set.of(user.getRoleId())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserSaveRequest request) {
        validateRole(request.getRoleId());
        validateUsernameUnique(null, request.getUsername());
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(400, "密码不能为空");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setRealName(request.getRealName());
        user.setRoleId(request.getRoleId());
        user.setPhone(request.getPhone());
        user.setStatus(request.getStatus());
        user.setDeleted(0);
        sysUserMapper.insert(user);
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long id, UserSaveRequest request) {
        SysUser user = requireUser(id);
        validateRole(request.getRoleId());
        validateUsernameUnique(id, request.getUsername());

        user.setUsername(request.getUsername());
        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(request.getPassword());
        }
        user.setRealName(request.getRealName());
        user.setRoleId(request.getRoleId());
        user.setPhone(request.getPhone());
        user.setStatus(request.getStatus());
        sysUserMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        SysUser user = requireUser(id);
        user.setDeleted(1);
        sysUserMapper.updateById(user);
    }

    @Override
    public void updateUserStatus(Long id, UserStatusRequest request) {
        SysUser user = requireUser(id);
        user.setStatus(request.getStatus());
        sysUserMapper.updateById(user);
    }

    @Override
    public void resetPassword(Long id, ResetPasswordRequest request) {
        SysUser user = requireUser(id);
        user.setPassword(request.getPassword());
        sysUserMapper.updateById(user);
    }

    @Override
    public List<RoleListItem> listRoles(String status) {
        return sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                        .eq(StringUtils.hasText(status), SysRole::getStatus, status)
                        .orderByDesc(SysRole::getId))
                .stream()
                .map(this::toRoleItem)
                .toList();
    }

    @Override
    public RoleDetail getRole(Long id) {
        SysRole role = requireRole(id);
        RoleDetail detail = new RoleDetail();
        detail.setId(role.getId());
        detail.setCode(role.getCode());
        detail.setName(role.getName());
        detail.setStatus(role.getStatus());
        detail.setRemark(role.getRemark());
        detail.setMenuIds(sysRoleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .eq(SysRoleMenu::getRoleId, id))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .toList());
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRole(RoleSaveRequest request) {
        validateRoleCodeUnique(null, request.getCode());
        SysRole role = new SysRole();
        role.setCode(request.getCode());
        role.setName(request.getName());
        role.setStatus(request.getStatus());
        role.setRemark(request.getRemark());
        sysRoleMapper.insert(role);
        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(Long id, RoleSaveRequest request) {
        SysRole role = requireRole(id);
        validateRoleCodeUnique(id, request.getCode());
        role.setCode(request.getCode());
        role.setName(request.getName());
        role.setStatus(request.getStatus());
        role.setRemark(request.getRemark());
        sysRoleMapper.updateById(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        requireRole(id);
        Long userCount = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRoleId, id)
                .eq(SysUser::getDeleted, 0));
        if (userCount != null && userCount > 0) {
            throw new BusinessException(409, "角色下仍存在用户，不允许删除");
        }

        sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
        sysRoleMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRoleMenus(Long id, RoleMenusRequest request) {
        requireRole(id);
        List<Long> menuIds = request.getMenuIds();
        if (!menuIds.isEmpty()) {
            long count = sysMenuMapper.selectCount(new LambdaQueryWrapper<SysMenu>().in(SysMenu::getId, menuIds));
            if (count != menuIds.size()) {
                throw new BusinessException(400, "存在无效菜单ID");
            }
        }

        sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
        for (Long menuId : menuIds) {
            SysRoleMenu relation = new SysRoleMenu();
            relation.setRoleId(id);
            relation.setMenuId(menuId);
            sysRoleMenuMapper.insert(relation);
        }
    }

    @Override
    public List<MenuTreeItem> getMenuTree() {
        List<SysMenu> menus = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getId));
        Map<Long, MenuTreeItem> nodeMap = menus.stream()
                .map(this::toMenuItem)
                .collect(Collectors.toMap(MenuTreeItem::getId, Function.identity(), (left, right) -> left));

        List<MenuTreeItem> roots = new ArrayList<>();
        for (MenuTreeItem item : nodeMap.values()) {
            if (item.getParentId() == null || item.getParentId() == 0) {
                roots.add(item);
                continue;
            }
            MenuTreeItem parent = nodeMap.get(item.getParentId());
            if (parent == null) {
                roots.add(item);
            } else {
                parent.getChildren().add(item);
            }
        }
        return roots;
    }

    @Override
    public MenuTreeItem getMenu(Long id) {
        return toMenuItem(requireMenu(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createMenu(MenuSaveRequest request) {
        validateParentMenu(request.getParentId());
        SysMenu menu = new SysMenu();
        applyMenuRequest(menu, request);
        sysMenuMapper.insert(menu);
        return menu.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMenu(Long id, MenuSaveRequest request) {
        SysMenu menu = requireMenu(id);
        validateParentMenu(request.getParentId());
        if (id.equals(request.getParentId())) {
            throw new BusinessException(400, "父菜单不能是自己");
        }
        applyMenuRequest(menu, request);
        sysMenuMapper.updateById(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenu(Long id) {
        requireMenu(id);
        Long childrenCount = sysMenuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, id));
        if (childrenCount != null && childrenCount > 0) {
            throw new BusinessException(409, "请先删除下级菜单");
        }

        sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getMenuId, id));
        sysMenuMapper.deleteById(id);
    }

    @Override
    public List<Map<String, Object>> getRoleMenus(Long roleId) {
        List<Long> menuIds = sysRoleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .eq(SysRoleMenu::getRoleId, roleId))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .toList();
        if (menuIds.isEmpty()) {
            return List.of();
        }
        return sysMenuMapper.selectBatchIds(menuIds).stream()
                .filter(menu -> "enabled".equalsIgnoreCase(menu.getStatus()))
                .map(menu -> Map.<String, Object>of(
                        "id", menu.getId(),
                        "name", menu.getName(),
                        "path", menu.getPath() != null ? menu.getPath() : "",
                        "component", menu.getComponent() != null ? menu.getComponent() : "",
                        "menuType", menu.getMenuType(),
                        "permissionCode", menu.getPermissionCode() != null ? menu.getPermissionCode() : "",
                        "parentId", menu.getParentId() != null ? menu.getParentId() : 0L
                ))
                .toList();
    }

    private UserListItem toUserItem(SysUser user, Map<Long, String> roleNameMap) {
        UserListItem item = new UserListItem();
        item.setId(user.getId());
        item.setUsername(user.getUsername());
        item.setRealName(user.getRealName());
        item.setRoleId(user.getRoleId());
        item.setRoleName(roleNameMap.get(user.getRoleId()));
        item.setPhone(user.getPhone());
        item.setStatus(user.getStatus());
        item.setCreatedAt(user.getCreatedAt());
        return item;
    }

    private RoleListItem toRoleItem(SysRole role) {
        RoleListItem item = new RoleListItem();
        item.setId(role.getId());
        item.setCode(role.getCode());
        item.setName(role.getName());
        item.setStatus(role.getStatus());
        item.setRemark(role.getRemark());
        return item;
    }

    private MenuTreeItem toMenuItem(SysMenu menu) {
        MenuTreeItem item = new MenuTreeItem();
        item.setId(menu.getId());
        item.setParentId(menu.getParentId());
        item.setName(menu.getName());
        item.setPath(menu.getPath());
        item.setComponent(menu.getComponent());
        item.setMenuType(menu.getMenuType());
        item.setPermissionCode(menu.getPermissionCode());
        item.setStatus(menu.getStatus());
        return item;
    }

    private void applyMenuRequest(SysMenu menu, MenuSaveRequest request) {
        menu.setParentId(request.getParentId());
        menu.setName(request.getName());
        menu.setPath(request.getPath());
        menu.setComponent(request.getComponent());
        menu.setMenuType(request.getMenuType());
        menu.setPermissionCode(request.getPermissionCode());
        menu.setStatus(request.getStatus());
    }

    private SysUser requireUser(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null || !Objects.equals(user.getDeleted(), 0)) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    private SysRole requireRole(Long id) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(404, "角色不存在");
        }
        return role;
    }

    private SysMenu requireMenu(Long id) {
        SysMenu menu = sysMenuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException(404, "菜单不存在");
        }
        return menu;
    }

    private void validateRole(Long roleId) {
        if (sysRoleMapper.selectById(roleId) == null) {
            throw new BusinessException(400, "角色不存在");
        }
    }

    private void validateParentMenu(Long parentId) {
        if (parentId != null && parentId != 0 && sysMenuMapper.selectById(parentId) == null) {
            throw new BusinessException(400, "父菜单不存在");
        }
    }

    private void validateUsernameUnique(Long id, String username) {
        SysUser exists = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getDeleted, 0)
                .last("limit 1"));
        if (exists != null && !exists.getId().equals(id)) {
            throw new BusinessException(409, "用户名已存在");
        }
    }

    private void validateRoleCodeUnique(Long id, String code) {
        SysRole exists = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, code)
                .last("limit 1"));
        if (exists != null && !exists.getId().equals(id)) {
            throw new BusinessException(409, "角色编码已存在");
        }
    }

    private Map<Long, String> buildRoleNameMap(Set<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return Map.of();
        }
        return sysRoleMapper.selectBatchIds(roleIds).stream()
                .collect(Collectors.toMap(SysRole::getId, SysRole::getName, (left, right) -> left));
    }
}
