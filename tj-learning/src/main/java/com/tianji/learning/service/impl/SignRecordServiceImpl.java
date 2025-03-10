package com.tianji.learning.service.impl;

import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.*;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.mq.message.SignInMessage;
import com.tianji.learning.service.ISignRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SignRecordServiceImpl implements ISignRecordService {

    private final StringRedisTemplate redisTemplate;

    private final RabbitMqHelper mqHelper;

    @Override
    public SignResultVO addSignRecords() {
        // 创建可以
        Long userId = UserContext.getUser();
        LocalDate now = LocalDate.now();
        String key = RedisConstants.SIGN_RECORD_KEY_PREFIX + userId + now.format(DateUtils.SIGN_DATE_SUFFIX_FORMATTER);
        // 计算offset
        int offset = now.getDayOfMonth() - 1;
        Boolean exists = redisTemplate.opsForValue().setBit(key, offset, true);
        if (BooleanUtils.isTrue(exists)) {
            throw new BizIllegalException("不允许重复签到！");
        }
        // 计算来连续签到天数
        int signDays = countSignDays(key, now.getDayOfMonth());
        // 计算签到得分
        int rewardPoints = 0;
        switch (signDays) {
            case 7:
                rewardPoints = 10;
                break;
            case 14:
                rewardPoints = 20;
                break;
            case 28:
                rewardPoints = 40;
                break;
        }
        // 保存积分明细记录
        mqHelper.send(
                MqConstants.Exchange.LEARNING_EXCHANGE,
                MqConstants.Key.SIGN_IN,
                SignInMessage.of(userId, rewardPoints + 1));// 签到积分是基本得分+奖励积分
        SignResultVO vo = new SignResultVO();
        vo.setSignDays(signDays);
        vo.setRewardPoints(rewardPoints);
        return vo;
    }

    private int countSignDays(String key, int len) {
        // 从月的第一天开始
        List<Long> result = redisTemplate.opsForValue()
                .bitField(key, BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(len))
                        .valueAt(0));
        if (CollUtils.isEmpty(result)) {
            return 0;
        }
        int count = 0;
        Long num = result.get(0);
        // 与1做与运算，得到最后一个bit，判断是否为0，为0则终止，为1则继续
        while ((num & 1) == 1) {
            count++;
            num = num >>> 1;
        }
        return count;
    }

    @Override
    public Byte[] querySignRecords() {
        Long userId = UserContext.getUser();
        LocalDate now = LocalDate.now();
        int dayOfMonth = now.getDayOfMonth();
        String key = RedisConstants.SIGN_RECORD_KEY_PREFIX + userId + now.format(DateUtils.SIGN_DATE_SUFFIX_FORMATTER);
        List<Long> result = redisTemplate.opsForValue().bitField(key,
                BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth))
                        .valueAt(0));
        if (CollUtils.isEmpty(result)) {
            return new Byte[0];
        }
        int num = result.get(0).intValue();
        Byte[] arr = new Byte[dayOfMonth];
        int pos = dayOfMonth - 1;
        while (pos >= 0) {
            arr[pos--] = (byte) (num & 1);
            num = num >>> 1;
        }
        return arr;
    }

}