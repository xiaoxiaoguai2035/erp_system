package com.dongjian.erp.manufacturingerpsystem.modules.auth.vo;

import java.util.List;
import java.util.Map;

public class AuthProfileResponse {

    private LoginUserInfo userInfo;
    private RoleInfo roleInfo;
    private List<Map<String, Object>> menus;

    public static class RoleInfo {
        private Long id;
        private String code;
        private String name;

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
    }

    public LoginUserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(LoginUserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public RoleInfo getRoleInfo() {
        return roleInfo;
    }

    public void setRoleInfo(RoleInfo roleInfo) {
        this.roleInfo = roleInfo;
    }

    public List<Map<String, Object>> getMenus() {
        return menus;
    }

    public void setMenus(List<Map<String, Object>> menus) {
        this.menus = menus;
    }
}
