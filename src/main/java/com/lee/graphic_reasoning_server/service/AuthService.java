package com.lee.graphic_reasoning_server.service;


import com.lee.graphic_reasoning_server.dto.LoginDTO;
import com.lee.graphic_reasoning_server.vo.LoginVO;
import com.lee.graphic_reasoning_server.vo.UserVO;

public interface AuthService {
    LoginVO login(LoginDTO dto);
    UserVO currentUser();
    void logout();
}
