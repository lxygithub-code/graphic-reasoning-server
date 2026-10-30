package com.lee.graphic_reasoning_server.vo;

import com.lee.graphic_reasoning_server.po.Question;
import lombok.Data;

import java.util.List;

@Data
public class WrongPdfItemVO {
    private Question question;
    /** 用户错选的选项 */
    private String userOption;
    /** 答题时长（秒） */
    private Integer duration;
    /** 平台解析列表 */
    private List<QuestionDetailVO.AnalysisVO> analyses;
}