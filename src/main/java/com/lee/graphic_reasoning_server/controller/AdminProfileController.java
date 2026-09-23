package com.lee.graphic_reasoning_server.controller;

import com.lee.graphic_reasoning_server.common.AdminContext;
import com.lee.graphic_reasoning_server.common.BizException;
import com.lee.graphic_reasoning_server.common.R;
import com.lee.graphic_reasoning_server.dto.AdminChangePwdDTO;
import com.lee.graphic_reasoning_server.dto.AdminUpdateDTO;
import com.lee.graphic_reasoning_server.po.Admin;
import com.lee.graphic_reasoning_server.mapper.AdminMapper;
import com.lee.graphic_reasoning_server.service.AdminService;
import com.lee.graphic_reasoning_server.vo.AdminInfoVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/profile")
@RequiredArgsConstructor
public class AdminProfileController {

    private final AdminService adminService;

    /** 获取当前管理员信息 */
    @GetMapping
    public R<AdminInfoVO> info() {
        return R.ok(adminService.currentAdmin());
    }

    /** 修改账号 / 昵称 / 头像 */
    @PutMapping
    public R<Void> update(@Valid @RequestBody AdminUpdateDTO dto) {
        adminService.updateProfile(dto);
        return R.ok();
    }

    /** 修改密码 */
    @PutMapping("/password")
    public R<Void> changePassword(@Valid @RequestBody AdminChangePwdDTO dto) {
        adminService.changePassword(dto);
        return R.ok();
    }
}
