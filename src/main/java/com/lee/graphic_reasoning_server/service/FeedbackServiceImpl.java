package com.lee.graphic_reasoning_server.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lee.graphic_reasoning_server.common.AdminContext;
import com.lee.graphic_reasoning_server.common.BizException;
import com.lee.graphic_reasoning_server.common.PageVO;
import com.lee.graphic_reasoning_server.common.UserContext;
import com.lee.graphic_reasoning_server.dto.FeedbackQueryDTO;
import com.lee.graphic_reasoning_server.dto.FeedbackReplyDTO;
import com.lee.graphic_reasoning_server.dto.FeedbackSubmitDTO;
import com.lee.graphic_reasoning_server.mapper.AdminMapper;
import com.lee.graphic_reasoning_server.mapper.FeedbackMapper;
import com.lee.graphic_reasoning_server.mapper.UserMapper;
import com.lee.graphic_reasoning_server.po.Admin;
import com.lee.graphic_reasoning_server.po.Feedback;
import com.lee.graphic_reasoning_server.po.User;
import com.lee.graphic_reasoning_server.vo.AdminFeedbackVO;
import com.lee.graphic_reasoning_server.vo.FeedbackVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackMapper feedbackMapper;
    private final UserMapper userMapper;
    private final AdminMapper adminMapper;
    private final AdminMessageService adminMessageService;
    private final StringRedisTemplate redis;
    // ==================== 用户端 ====================

    @Override
    @Transactional
    public Long submit(FeedbackSubmitDTO dto) {
        Long userId = UserContext.get();
        if (userId == null) throw new BizException("请先登录");

        String content = dto.getContent().trim();

        // ============ 1. 限流：60 秒内只能提交 1 条 ============
        // （反馈比评论重量级，间隔设长一些）
        String rateKey = "feedback:rate:" + userId;
        Boolean ok = redis.opsForValue().setIfAbsent(rateKey, "1", Duration.ofSeconds(60));
        if (Boolean.FALSE.equals(ok)) {
            throw new BizException("提交太频繁，请稍后再试");
        }

        // ============ 2. 防重复：5 分钟内不能发相同内容 ============
        String contentKey = "feedback:content:" + userId + ":" + content.hashCode();
        Boolean ok2 = redis.opsForValue().setIfAbsent(contentKey, "1", Duration.ofMinutes(5));
        if (Boolean.FALSE.equals(ok2)) {
            throw new BizException("请勿重复提交相同内容");
        }

        // ============ 3. 落库 ============
        Feedback fb = new Feedback();
        fb.setUserId(userId);
        fb.setContent(content);
        fb.setStatus(0);
        fb.setUserRead(1);
        feedbackMapper.insert(fb);

        // ============ 4. 推送消息给管理员 ============
        adminMessageService.pushMessage(
                null,
                "feedback",
                "新意见反馈",
                StrUtil.maxLength(content, 100),
                fb.getId(),
                null
        );

        return fb.getId();
    }

    @Override
    public PageVO<FeedbackVO> myList(Integer pageNum, Integer pageSize) {
        Long userId = UserContext.get();
        if (userId == null) throw new BizException("请先登录");

        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;

        Page<Feedback> page = feedbackMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Feedback>()
                        .eq(Feedback::getUserId, userId)
                        .orderByDesc(Feedback::getCreateTime));

        List<FeedbackVO> records = page.getRecords().stream()
                .map(this::toUserVO)
                .collect(Collectors.toList());

        return PageVO.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    @Transactional
    public FeedbackVO detail(Long id) {
        Long userId = UserContext.get();
        if (userId == null) throw new BizException("请先登录");

        Feedback fb = feedbackMapper.selectById(id);
        if (fb == null) throw new BizException("意见不存在");
        if (!fb.getUserId().equals(userId)) throw new BizException("无权查看");

        // ★ 已解决 + 未读 → 标记已读
        if (fb.getStatus() != null && fb.getStatus() == 2
                && fb.getUserRead() != null && fb.getUserRead() == 0) {
            Feedback update = new Feedback();
            update.setId(fb.getId());
            update.setUserRead(1);
            feedbackMapper.updateById(update);
            fb.setUserRead(1);
        }

        return toUserVO(fb);
    }

    @Override
    public Long unreadCount() {
        Long userId = UserContext.get();
        if (userId == null) return 0L;

        return feedbackMapper.selectCount(
                new LambdaQueryWrapper<Feedback>()
                        .eq(Feedback::getUserId, userId)
                        .eq(Feedback::getStatus, 2)
                        .eq(Feedback::getUserRead, 0));
    }

    // ==================== 管理端 ====================

    @Override
    public PageVO<AdminFeedbackVO> adminPage(FeedbackQueryDTO dto) {
        int pageNum = dto.getPageNum() == null || dto.getPageNum() < 1 ? 1 : dto.getPageNum();
        int pageSize = dto.getPageSize() == null || dto.getPageSize() < 1 ? 10 : dto.getPageSize();

        LambdaQueryWrapper<Feedback> qw = new LambdaQueryWrapper<Feedback>()
                .eq(dto.getStatus() != null, Feedback::getStatus, dto.getStatus())
                .like(StrUtil.isNotBlank(dto.getKeyword()), Feedback::getContent, dto.getKeyword())
                .orderByDesc(Feedback::getCreateTime);

        Page<Feedback> page = feedbackMapper.selectPage(new Page<>(pageNum, pageSize), qw);

        List<AdminFeedbackVO> records = new ArrayList<>();
        if (!page.getRecords().isEmpty()) {
            List<Long> userIds = page.getRecords().stream()
                    .map(Feedback::getUserId).distinct().collect(Collectors.toList());
            Map<Long, User> userMap = userMapper.selectByIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));

            for (Feedback fb : page.getRecords()) {
                AdminFeedbackVO vo = new AdminFeedbackVO();
                BeanUtil.copyProperties(fb, vo);
                User u = userMap.get(fb.getUserId());
                if (u != null) {
                    vo.setNickname(StrUtil.blankToDefault(u.getNickname(), "匿名"));
                    vo.setAvatarUrl(u.getAvatarUrl());
                } else {
                    vo.setNickname("匿名");
                }
                vo.setStatusLabel(adminStatusLabel(fb.getStatus()));
                records.add(vo);
            }
        }

        return PageVO.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    public AdminFeedbackVO adminDetail(Long id) {
        Feedback fb = feedbackMapper.selectById(id);
        if (fb == null) throw new BizException("意见不存在");

        AdminFeedbackVO vo = new AdminFeedbackVO();
        BeanUtil.copyProperties(fb, vo);

        User u = userMapper.selectById(fb.getUserId());
        if (u != null) {
            vo.setNickname(StrUtil.blankToDefault(u.getNickname(), "匿名"));
            vo.setAvatarUrl(u.getAvatarUrl());
        } else {
            vo.setNickname("匿名");
        }
        vo.setStatusLabel(adminStatusLabel(fb.getStatus()));
        return vo;
    }

    private String adminStatusLabel(Integer status) {
        if (status == null) {
            return "待处理";
        }
        return switch (status) {
            case 0 -> "待处理";
            case 1 -> "处理中";
            case 2 -> "已解决";
            default -> "未知";
        };
    }

    @Override
    @Transactional
    public void reply(Long id, FeedbackReplyDTO dto) {
        Long adminId = AdminContext.get();
        if (adminId == null) throw new BizException("管理员未登录");

        Feedback fb = feedbackMapper.selectById(id);
        if (fb == null) throw new BizException("意见不存在");

        Admin admin = adminMapper.selectById(adminId);
        String adminName = admin != null ? StrUtil.blankToDefault(admin.getNickname(), "管理员") : "管理员";

        Feedback update = new Feedback();
        update.setId(id);
        update.setStatus(2);
        update.setReplyContent(dto.getReplyContent().trim());
        update.setReplyAdminId(adminId);
        update.setReplyAdminName(adminName);
        update.setReplyTime(LocalDateTime.now());
        update.setUserRead(0);   // ★ 用户未读，能收到红点
        feedbackMapper.updateById(update);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        feedbackMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        if (status == null || status < 0 || status > 2) {
            throw new BizException("状态值不合法");
        }
        Feedback fb = feedbackMapper.selectById(id);
        if (fb == null) throw new BizException("意见不存在");

        Feedback update = new Feedback();
        update.setId(id);
        update.setStatus(status);
        feedbackMapper.updateById(update);
    }

    // ==================== 工具 ====================

    private FeedbackVO toUserVO(Feedback fb) {
        FeedbackVO vo = new FeedbackVO();
        BeanUtil.copyProperties(fb, vo);
        vo.setStatusLabel(statusLabel(fb.getStatus()));
        return vo;
    }

    private String statusLabel(Integer status) {
        if (status == null) {
            return "已提交";
        }
        return switch (status) {
            case 0 -> "已提交";
            case 1 -> "处理中";
            case 2 -> "已解决";
            default -> "未知";
        };
    }

}
