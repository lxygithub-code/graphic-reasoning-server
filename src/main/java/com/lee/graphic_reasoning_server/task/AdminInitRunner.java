package com.lee.graphic_reasoning_server.task;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lee.graphic_reasoning_server.po.Admin;
import com.lee.graphic_reasoning_server.mapper.AdminMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitRunner implements CommandLineRunner {

    private final AdminMapper adminMapper;

    @Override
    public void run(String... args) {
        Long count = adminMapper.selectCount(
                new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, "admin"));
        if (count != null && count > 0) return;

        Admin admin = new Admin();
        admin.setUsername("admin");
        admin.setPassword(BCrypt.hashpw("admin123", BCrypt.gensalt()));
        admin.setNickname("超级管理员");
        admin.setStatus(1);
        adminMapper.insert(admin);

        log.info("已初始化默认管理员账号：admin / admin123");
    }
}
