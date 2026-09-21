package com.lee.graphic_reasoning_server.service;

import com.lee.graphic_reasoning_server.common.PageVO;
import com.lee.graphic_reasoning_server.dto.UserQueryDTO;
import com.lee.graphic_reasoning_server.vo.AdminUserVO;

public interface UserAdminService {

    PageVO<AdminUserVO> page(UserQueryDTO dto);

    void updateStatus(Long userId, Integer status);

    void delete(Long userId);
}