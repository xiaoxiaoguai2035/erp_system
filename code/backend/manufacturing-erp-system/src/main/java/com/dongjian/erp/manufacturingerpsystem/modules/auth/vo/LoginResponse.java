package com.dongjian.erp.manufacturingerpsystem.modules.auth.vo;

public class LoginResponse {

    private String token;
    private String tokenName;
    private LoginUserInfo userInfo;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenName() {
        return tokenName;
    }

    public void setTokenName(String tokenName) {
        this.tokenName = tokenName;
    }

    public LoginUserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(LoginUserInfo userInfo) {
        this.userInfo = userInfo;
    }
}
