package com.lee.graphic_reasoning_server.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lee.graphic_reasoning_server.common.BizException;
import com.lee.graphic_reasoning_server.common.PageVO;
import com.lee.graphic_reasoning_server.dto.UserQueryDTO;
import com.lee.graphic_reasoning_server.mapper.UserMapper;
import com.lee.graphic_reasoning_server.po.User;
import com.lee.graphic_reasoning_server.vo.AdminUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAdminServiceImpl implements UserAdminService {

    private final UserMapper userMapper;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public PageVO<AdminUserVO> page(UserQueryDTO dto) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<User>()
                .like(StrUtil.isNotBlank(dto.getNickname()), User::getNickname, dto.getNickname())
                .eq(dto.getStatus() != null, User::getStatus, dto.getStatus())
                .orderByDesc(User::getId);

        Page<User> page = userMapper.selectPage(new Page<>(dto.getPageNum(), dto.getPageSize()), qw);

        List<AdminUserVO> records = page.getRecords().stream().map(u -> {
            AdminUserVO vo = new AdminUserVO();
            BeanUtil.copyProperties(u, vo);
            if (u.getLastLoginTime() != null) vo.setLastLoginTime(u.getLastLoginTime().format(FMT));
            if (u.getCreateTime() != null) vo.setCreateTime(u.getCreateTime().format(FMT));
            return vo;
        }).collect(Collectors.toList());

        return PageVO.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    public void updateStatus(Long userId, Integer status) {
        User u = userMapper.selectById(userId);
        if (u == null) throw new BizException("用户不存在");
        User update = new User();
        update.setId(userId);
        update.setStatus(status);
        userMapper.updateById(update);
    }

    @Override
    public void delete(Long userId) {
        userMapper.deleteById(userId);
    }

    @Override
    @Transactional
    public void updatePermission(Long userId, Integer canComment, Integer showComment, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BizException("用户不存在");

        User update = new User();
        update.setId(userId);
        if (canComment != null)  update.setCanComment(canComment);
        if (showComment != null) update.setShowComment(showComment);
        if (status != null)      update.setStatus(status);

        userMapper.updateById(update);
    }
}
