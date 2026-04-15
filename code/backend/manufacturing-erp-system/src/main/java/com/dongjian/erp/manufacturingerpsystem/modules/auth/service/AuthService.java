package com.dongjian.erp.manufacturingerpsystem.modules.auth.service;

import com.dongjian.erp.manufacturingerpsystem.modules.auth.dto.LoginRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.auth.vo.AuthProfileResponse;
import com.dongjian.erp.manufacturingerpsystem.modules.auth.vo.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    void logout();

    AuthProfileResponse profile();
}
