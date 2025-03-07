package com.tianji.learning;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.service.ILearningLessonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class QueryTest {
    @Autowired
    ILearningLessonService lessonService;
    @Test
    public void test(){
        QueryWrapper<LearningLesson> queryWrapper = new QueryWrapper<LearningLesson>().select("sum(week_freq) as weekFreq").eq("user_id", 2);
        LearningLesson one = lessonService.getOne(queryWrapper);
        System.out.println("one = " + one);
    }
}
