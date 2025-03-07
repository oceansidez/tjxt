package com.tianji.learning.mapper;

import com.tianji.learning.domain.po.LearningLesson;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 * 学生课程表 Mapper 接口
 * </p>
 *
 * @author author
 * @since 2025-03-06
 */
public interface LearningLessonMapper extends BaseMapper<LearningLesson> {

    /**
     * 查询计划全部课程的周计划数
     *
     * @param userId
     * @return
     */
    Integer queryTotalPlan(Long userId);
}
