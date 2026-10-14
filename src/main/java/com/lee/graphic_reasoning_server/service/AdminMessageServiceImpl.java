package com.lee.graphic_reasoning_server.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lee.graphic_reasoning_server.common.AdminContext;
import com.lee.graphic_reasoning_server.common.BizException;
import com.lee.graphic_reasoning_server.common.PageVO;
import com.lee.graphic_reasoning_server.mapper.AdminMessageMapper;
import com.lee.graphic_reasoning_server.po.AdminMessage;
import com.lee.graphic_reasoning_server.vo.AdminMessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMessageServiceImpl implements AdminMessageService {

    private final AdminMessageMapper messageMapper;

    @Override
    public PageVO<AdminMessageVO> page(Integer pageNum, Integer pageSize, Integer isRead) {
        Long adminId = AdminContext.get();
        if (adminId == null) throw new BizException("管理员未登录");

        Page<AdminMessage> page = messageMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<AdminMessage>()
                        .and(w -> w.eq(AdminMessage::getAdminId, adminId)
                                .or()
                                .isNull(AdminMessage::getAdminId))
                        .eq(isRead != null, AdminMessage::getIsRead, isRead)
                        .orderByDesc(AdminMessage::getCreateTime));

        List<AdminMessageVO> records = page.getRecords().stream().map(m -> {
            AdminMessageVO vo = new AdminMessageVO();
            BeanUtil.copyProperties(m, vo);
            return vo;
        }).collect(Collectors.toList());

        return PageVO.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    public Long unreadCount() {
        Long adminId = AdminContext.get();
        if (adminId == null) return 0L;

        return messageMapper.selectCount(
                new LambdaQueryWrapper<AdminMessage>()
                        .and(w -> w.eq(AdminMessage::getAdminId, adminId)
                                .or()
                                .isNull(AdminMessage::getAdminId))
                        .eq(AdminMessage::getIsRead, 0));
    }

    @Override
    public void markRead(Long id) {
        AdminMessage msg = messageMapper.selectById(id);
        if (msg == null) return;

        AdminMessage update = new AdminMessage();
        update.setId(id);
        update.setIsRead(1);
        messageMapper.updateById(update);
    }

    @Override
    public void markAllRead() {
        Long adminId = AdminContext.get();
        if (adminId == null) return;

        messageMapper.update(null,
                new LambdaUpdateWrapper<AdminMessage>()
                        .and(w -> w.eq(AdminMessage::getAdminId, adminId)
                                .or()
                                .isNull(AdminMessage::getAdminId))
                        .eq(AdminMessage::getIsRead, 0)
                        .set(AdminMessage::getIsRead, 1));
    }

    @Override
    public void pushMessage(Long adminId, String type, String title, String content,
                            Long refId, Long refQuestionId) {
        try {
            AdminMessage msg = new AdminMessage();
            msg.setAdminId(adminId);
            msg.setType(type);
            msg.setTitle(title);
            msg.setContent(content);
            msg.setRefId(refId);
            msg.setRefQuestionId(refQuestionId);
            msg.setIsRead(0);
            messageMapper.insert(msg);
        } catch (Exception e) {
            // 消息推送失败不能影响主业务
            log.error("[消息推送] 失败 type={}, refId={}", type, refId, e);
        }
    }
}