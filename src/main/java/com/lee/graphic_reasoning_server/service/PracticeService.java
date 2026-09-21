package com.lee.graphic_reasoning_server.service;

import com.lee.graphic_reasoning_server.common.PageVO;
import com.lee.graphic_reasoning_server.dto.CommentSaveDTO;
import com.lee.graphic_reasoning_server.dto.PracticeSingleDTO;
import com.lee.graphic_reasoning_server.dto.PracticeSubmitDTO;
import com.lee.graphic_reasoning_server.vo.*;

import java.util.List;

public interface PracticeService {

    /** 背题模式：单题核对 */
    PracticeSingleVO submitSingle(PracticeSingleDTO dto);

    /** 刷题模式：整组交卷 */
    PracticeSubmitVO submitExam(PracticeSubmitDTO dto);

    PracticeRecordDetailVO getRecordDetail(Long recordId);

    /** 发表评论 */
    CommentVO submitComment(CommentSaveDTO dto);

    List<CommentVO> listComments(Long questionId, Integer limit);

    PageVO<PracticeRecordVO> myRecords(Integer pageNum, Integer pageSize);

    PageVO<QuestionDetailVO> wrongList(Integer pageNum, Integer pageSize);

    void removeWrong(Long questionId);

    WrongQuestionDetailVO getWrongDetail(Long questionId);
}