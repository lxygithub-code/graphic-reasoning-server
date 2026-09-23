package com.lee.graphic_reasoning_server.service;

import com.lee.graphic_reasoning_server.dto.AdminChangePwdDTO;
import com.lee.graphic_reasoning_server.dto.AdminLoginDTO;
import com.lee.graphic_reasoning_server.dto.AdminUpdateDTO;
import com.lee.graphic_reasoning_server.vo.AdminInfoVO;
import com.lee.graphic_reasoning_server.vo.AdminLoginVO;

public interface AdminService {

    AdminLoginVO login(AdminLoginDTO dto);

    /** 获取当前管理员信息 */
    AdminInfoVO currentAdmin();

    /** 退出登录 */
    void logout(String token);

    /** 更新账号/昵称/头像 */
    void updateProfile(AdminUpdateDTO dto);

    /** 修改密码 */
    void changePassword(AdminChangePwdDTO dto);
}