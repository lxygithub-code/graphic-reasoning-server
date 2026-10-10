package com.lee.graphic_reasoning_server.service;

import com.lee.graphic_reasoning_server.common.PageVO;
import com.lee.graphic_reasoning_server.dto.QuestionQueryDTO;
import com.lee.graphic_reasoning_server.dto.QuestionRandomDTO;
import com.lee.graphic_reasoning_server.dto.QuestionSaveDTO;
import com.lee.graphic_reasoning_server.vo.*;

import java.util.List;

public interface QuestionService {

    List<QuestionPracticeVO> randomPractice(QuestionRandomDTO dto);

    QuestionDetailVO detail(Long id);

    PageVO<QuestionDetailVO> page(QuestionQueryDTO dto);

    Long save(QuestionSaveDTO dto);

    void update(QuestionSaveDTO dto);

    void delete(Long id);

    void batchDelete(List<Long> ids);

    /** 套卷列表（含题目数 + 可抽取数） */
    List<QuestionSourceStatVO> sourceStats(String examType, String examSubType, String keyword);

    /** 某套卷下的所有题目 */
    List<QuestionDetailVO> listBySource(String source);

    /** 查某题的所有平台解析 */
    List<QuestionDetailVO.AnalysisVO> listAnalyses(Long questionId);

    List<ExamTypeCountVO> countByExamType();

    // ==================== ★ 新增 ====================

    /**
     * 批量设置某套卷下所有题目的可抽取状态
     * @param source 套卷值；空字符串表示"未分类"
     * @param extractable 1=可抽取，0=禁止抽取
     * @return 实际更新的行数
     */
    int updateSourceExtractable(String source, Integer extractable);

    /**
     * 设置单道题目的可抽取状态
     * @param id 题目 ID
     * @param extractable 1=可抽取，0=禁止抽取
     */
    void updateExtractable(Long id, Integer extractable);

    /** 题目预览：只返回题面，不含答案和解析 */
    QuestionPracticeVO preview(Long id);

    List<CategoryTreeVO> categoryTreeWithCount();
}