package com.lee.graphic_reasoning_server.util;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 评论内容过滤器：
 * 1. 长度、空内容校验
 * 2. 联系方式（手机号 / QQ / 微信 / URL）正则拦截
 * 3. 敏感词库（广告、脏话）DFA 匹配
 * 4. 重复字符 / 刷屏拦截
 */
@Component
public class ContentFilter {

    /** 联系方式正则 */
    private static final Pattern PHONE = Pattern.compile("1[3-9]\\d{9}");
    private static final Pattern QQ = Pattern.compile("[1-9]\\d{4,10}");
    private static final Pattern WX = Pattern.compile("(?i)(wx|weixin|vx|v信|微信|扣扣|qq|企鹅)\\s*[:：]?\\s*[a-zA-Z0-9_\\-]{5,20}");
    private static final Pattern URL = Pattern.compile("(?i)(https?://|www\\.)[a-z0-9\\-]+(\\.[a-z0-9\\-]+)+");

    /** 敏感词库（可换成从文件/DB 加载） */
    private static final Set<String> SENSITIVE = new HashSet<>(Arrays.asList(
            // 广告
            "加微信", "加qq", "加Q", "私聊", "代购", "代写", "包过", "包学", "出售",
            "低价", "优惠券", "扫码", "点击链接", "推广", "广告", "接单", "包教包会",
            "培训", "网课", "押题", "内部资料", "答案出售","V","v",
            // 不文明（示例，可扩展）
            "傻逼", "智障", "脑残", "狗东西", "滚蛋", "去死","傻","蠢","sb","SB","CNM","cnm","滚",
            // 违规
            "赌博", "博彩", "色情", "暴力", "毒品"
    ));

    /**
     * 校验评论内容
     * @return null 通过；非 null 为拒绝原因
     */
    public String validate(String content) {
        if (StrUtil.isBlank(content)) return "评论不能为空";

        String text = content.trim();
        if (text.length() < 2) return "评论太短";
        if (text.length() > 200) return "评论最多 200 字";

        // 联系方式
        if (PHONE.matcher(text).find()) return "评论不能包含手机号";
        if (URL.matcher(text).find()) return "评论不能包含链接";
        if (WX.matcher(text).find()) return "评论不能包含联系方式";
        if (QQ.matcher(text).find()) return "评论不能包含联系方式";

        // 敏感词
        for (String word : SENSITIVE) {
            if (text.contains(word)) {
                return "评论包含敏感内容，请文明发言";
            }
        }

        // 刷屏：同一字符连续超过 8 次
        if (Pattern.compile("(.)\\1{7,}").matcher(text).find()) {
            return "请勿刷屏";
        }

        return null;
    }
}
