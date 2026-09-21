package com.lee.graphic_reasoning_server.controller;

import com.lee.graphic_reasoning_server.common.AdminContext;
import com.lee.graphic_reasoning_server.common.BizException;
import com.lee.graphic_reasoning_server.common.R;
import com.lee.graphic_reasoning_server.po.Admin;
import com.lee.graphic_reasoning_server.mapper.AdminMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminProfileController {

    private final AdminMapper adminMapper;

    /** 获取当前登录管理员信息 */
    @GetMapping("/profile")
    public R<Admin> profile() {
        Long adminId = AdminContext.get();
        if (adminId == null) throw new BizException("管理员未登录");
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) throw new BizException("管理员不存在");
        // 密码字段在实体上做了 @TableField(select = false)，不会返回
        return R.ok(admin);
    }
}
