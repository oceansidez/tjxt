package com.tianji.learning.mapper;

import com.tianji.api.dto.IdAndNumDTO;
import com.tianji.learning.domain.po.LearningRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 学习记录表 Mapper 接口
 * </p>
 *
 * @author author
 * @since 2025-03-07
 */
public interface LearningRecordMapper extends BaseMapper<LearningRecord> {
    /**
     * 统计每一个课程本周已学习小节数量
     *
     * @param userId
     * @param weekBeginTime
     * @param weekEndTime
     * @return
     */
    List<IdAndNumDTO> countLearnedSections(@Param("userId") Long userId, @Param("weekBeginTime") LocalDateTime weekBeginTime, @Param("weekEndTime") LocalDateTime weekEndTime);
}
