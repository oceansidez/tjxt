package com.tianji.learning;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

@SpringBootTest
public class RedisTest {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    public void bitMap() {
        for (int i = 0; i < 365; i++) {
            if (i % 3 == 0) {
                redisTemplate.opsForValue().setBit("bitKey", i, true);
            }
        }


    }

    @Test
    public void bitMap2() {
        List<Long> bitKey = redisTemplate.opsForValue().bitField("bitKey",
                BitFieldSubCommands.create().
                        get(BitFieldSubCommands.BitFieldType.unsigned(10))
                        .valueAt(0)
        );
        for (Long aLong : bitKey) {
            System.out.println("aLong = " + aLong);
        }
    }

}
