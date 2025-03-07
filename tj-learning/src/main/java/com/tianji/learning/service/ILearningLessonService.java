package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.po.LearningLesson;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;

import java.util.List;

/**
 * <p>
 * 学生课程表 服务类
 * </p>
 *
 * @author author
 * @since 2025-03-06
 */
public interface ILearningLessonService extends IService<LearningLesson> {

    /**
     * 添加到课程表
     *
     * @param userId
     * @param courseIds
     */
    void addUserLessons(Long userId, List<Long> courseIds);

    /**
     * 查询我的课
     *
     * @param query
     * @return
     */
    PageDTO<LearningLessonVO> queryMyLessons(PageQuery query);

    /**
     * 查询我正在学习的课程
     *
     * @return
     */
    LearningLessonVO queryMyCurrentLesson();

    /**
     * 删除课程
     *
     * @param courseId
     * @return
     */
    void deleteCourseFromLesson(Long userId, Long courseId);

    /**
     * 校验当前用户是否可以学习当前课程
     *
     * @param courseId
     * @return
     */
    Long isLessonValid(Long courseId);

    /**
     * 查询指定课程信息
     *
     * @param courseId
     * @return
     */
    LearningLessonVO queryLessonByCourseId(Long courseId);

    /**
     * 统计课程学习人数
     *
     * @param courseId
     * @return
     */
    Integer countLearningLessonByCourse(Long courseId);

    /**
     * 创建学习计划
     *
     * @param courseId
     * @param freq
     */
    void createLearningPlan(Long courseId, Integer freq);

    /**
     * 查询我的学习计划
     *
     * @param query
     * @return
     */
    LearningPlanPageVO queryMyPlans(PageQuery query);
}
