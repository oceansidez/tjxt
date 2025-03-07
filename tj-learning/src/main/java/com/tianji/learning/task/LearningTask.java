package com.tianji.learning.task;

import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.service.ILearningLessonService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
@AllArgsConstructor
public class LearningTask {

    private final ILearningLessonService lessonService;

    // 检查课程是否过期，过期则删除
    @Scheduled(cron = "0 0 2 * * ?")
    public void lesson() {
        log.debug("检查课程是否过期，过期则删除开始执行....");
        lessonService.lambdaUpdate()
                .set(LearningLesson::getStatus, LessonStatus.EXPIRED)
                .lt(LearningLesson::getExpireTime, LocalDateTime.now())
                .update();
    }
}
