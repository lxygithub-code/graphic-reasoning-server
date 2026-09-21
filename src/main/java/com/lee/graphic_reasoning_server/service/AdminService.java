package com.lee.graphic_reasoning_server.service;

import com.lee.graphic_reasoning_server.dto.AdminLoginDTO;
import com.lee.graphic_reasoning_server.vo.AdminLoginVO;

public interface AdminService {

    AdminLoginVO login(AdminLoginDTO dto);

    void logout();
}