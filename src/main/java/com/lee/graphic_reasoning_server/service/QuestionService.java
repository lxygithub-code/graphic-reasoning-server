package com.lee.graphic_reasoning_server.service;

import com.lee.graphic_reasoning_server.common.PageVO;
import com.lee.graphic_reasoning_server.dto.QuestionQueryDTO;
import com.lee.graphic_reasoning_server.dto.QuestionRandomDTO;
import com.lee.graphic_reasoning_server.dto.QuestionSaveDTO;
import com.lee.graphic_reasoning_server.vo.ExamTypeCountVO;
import com.lee.graphic_reasoning_server.vo.QuestionDetailVO;
import com.lee.graphic_reasoning_server.vo.QuestionPracticeVO;
import com.lee.graphic_reasoning_server.vo.QuestionSourceStatVO;

import java.util.List;

public interface QuestionService {

    List<QuestionPracticeVO> randomPractice(QuestionRandomDTO dto);

    QuestionDetailVO detail(Long id);

    PageVO<QuestionDetailVO> page(QuestionQueryDTO dto);

    Long save(QuestionSaveDTO dto);

    void update(QuestionSaveDTO dto);

    void delete(Long id);

    void batchDelete(List<Long> ids);

    /** 套卷列表（含题目数） */
    List<QuestionSourceStatVO> sourceStats();

    /** 某套卷下的所有题目 */
    List<QuestionDetailVO> listBySource(String source);

    /** 查某题的所有平台解析 */
    List<QuestionDetailVO.AnalysisVO> listAnalyses(Long questionId);

    List<ExamTypeCountVO> countByExamType();
}