package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordDTO;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.exceptions.DbException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.SectionType;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.tianji.learning.service.ILearningRecordService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 学习记录表 服务实现类
 * </p>
 *
 * @author author
 * @since 2025-03-07
 */
@Service
@AllArgsConstructor
public class LearningRecordServiceImpl extends ServiceImpl<LearningRecordMapper, LearningRecord> implements ILearningRecordService {

    private final ILearningLessonService lessonService;

    private final CourseClient courseClient;

    /**
     * 查询指定课程的学习记录
     *
     * @param courseId
     * @return
     */
    @Override
    public LearningLessonDTO queryLearningRecordByCourse(Long courseId) {
        // 获取当前登录用户
        Long userId = UserContext.getUser();
        // 查询课表信息
        LearningLesson lesson = lessonService.lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId)
                .one();
        // 查询课程学习记录
        List<LearningRecord> records = lambdaQuery()
                .eq(LearningRecord::getLessonId, lesson.getId())
                .list();
        LearningLessonDTO learningLessonDTO = new LearningLessonDTO();
        learningLessonDTO.setId(lesson.getId());
        learningLessonDTO.setLatestSectionId(lesson.getLatestSectionId());
        learningLessonDTO.setRecords(BeanUtils.copyList(records, LearningRecordDTO.class));
        return learningLessonDTO;
    }

    /**
     * 提交学习记录
     *
     * @param formDTO
     */
    @Transactional
    public void addLearningRecord(LearningRecordFormDTO formDTO) {
        // 获取登录用户
        Long userId = UserContext.getUser();
        // 处理学习记录
        boolean finished = false; // 是否完成小结
        if (formDTO.getSectionType() == SectionType.VIDEO) {
            //处理视频
            finished = handleVideoRecord(userId, formDTO);
        } else {
            // 处理考试
            finished = handleExamRecord(userId, formDTO);
        }
        // 处理课表数据
        handleLearningLessonsChanges(formDTO, finished);
    }

    private boolean handleVideoRecord(Long userId, LearningRecordFormDTO formDTO) {
        // 查询旧的学习记录，存在更新不存在添加，并判断是否已看完
        LearningRecord oldRecord = lambdaQuery()
                .eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getLessonId, formDTO.getLessonId())
                .eq(LearningRecord::getSectionId, formDTO.getSectionId())
                .one();
        if (oldRecord == null) {
            LearningRecord record = BeanUtils.copyBean(formDTO, LearningRecord.class);
            record.setUserId(userId);
            // 写入数据库
            boolean success = save(record);
            if (!success) {
                throw new DbException("新增学习记录失败！");
            }
            return false;
        }
        // 判断是否第一次看完,第一次看完才更新完成状态
        boolean isFinish = !oldRecord.getFinished() && formDTO.getMoment() > (formDTO.getDuration() >>> 1);
        boolean success = lambdaUpdate()
                .set(LearningRecord::getMoment, formDTO.getMoment())
                .set(isFinish, LearningRecord::getFinished, true)
                .set(isFinish, LearningRecord::getFinishTime, formDTO.getCommitTime())
                .eq(LearningRecord::getId, oldRecord.getId())
                .update();
        if (!success) {
            throw new DbException("更新学习记录失败！");
        }
        return isFinish;
    }

    private boolean handleExamRecord(Long userId, LearningRecordFormDTO formDTO) {
        // 查询旧的学习记录，存在更新不存在添加，并判断是否已看完
        LearningRecord oldRecord = lambdaQuery()
                .eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getLessonId, formDTO.getLessonId())
                .eq(LearningRecord::getSectionId, formDTO.getSectionId())
                .one();
        if (oldRecord == null) {
            LearningRecord record = BeanUtils.copyBean(formDTO, LearningRecord.class);
            record.setUserId(userId);
            record.setFinished(true);
            record.setFinishTime(formDTO.getCommitTime());
            // 3.写入数据库
            boolean success = save(record);
            if (!success) {
                throw new DbException("新增考试记录失败！");
            }
        }
        return true;
    }

    private void handleLearningLessonsChanges(LearningRecordFormDTO formDTO, boolean finished) {
        // 查询课表
        LearningLesson lesson = lessonService.getById(formDTO.getLessonId());
        if (lesson == null) {
            throw new BizIllegalException("课程不存在，无法更新数据！");
        }
        boolean allFinish = false;
        if (finished) {
            // 判断是否完成所有小结
            CourseFullInfoDTO cInfo = courseClient.getCourseInfoById(lesson.getCourseId(), false, false);
            if (cInfo == null) {
                throw new BizIllegalException("课程不存在，无法更新数据！");
            }
            // 比较课程是否全部学完：已学习小节 >= 课程总小节
            allFinish = lesson.getLearnedSections() + 1 >= cInfo.getSectionNum();
        }
        lessonService.lambdaUpdate()
                .setSql(finished, "learned_sections = learned_sections + 1")
                .set(allFinish, LearningLesson::getStatus, allFinish ? LessonStatus.FINISHED : LessonStatus.LEARNING)
                .set(LearningLesson::getLatestSectionId, formDTO.getLessonId())
                .set(LearningLesson::getLatestLearnTime, formDTO.getCommitTime())
                .eq(LearningLesson::getId, lesson.getId())
                .update();
    }
}
