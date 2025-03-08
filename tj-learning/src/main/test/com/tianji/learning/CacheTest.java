package com.tianji.learning;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.Duration;

@Slf4j
public class CacheTest {

    @Test
    void testBasicOps() {

        // 创建缓存对象 设置缓存的数量上限
        Cache<String, String> cache2 = Caffeine.newBuilder()
                .maximumSize(1) // 设置缓存大小上限为 1
                .build();

        // 创建缓存对象 设置缓存的有效时间
        Cache<String, String> cache3 = Caffeine.newBuilder()
                // 设置缓存有效期为 10 秒，从最后一次写入开始计时
                .expireAfterWrite(Duration.ofSeconds(10))
                .build();

        // 构建cache对象
        Cache<String, String> cache = Caffeine.newBuilder().build();

        // 存数据
        cache.put("gf", "迪丽热巴");

        // 取数据
        String gf = cache.getIfPresent("gf");
        System.out.println("gf = " + gf);

        // 取数据，包含两个参数：
        // 参数一：缓存的key
        // 参数二：Lambda表达式，表达式参数就是缓存的key，方法体是查询数据库的逻辑
        // 优先根据key查询JVM缓存，如果未命中，则执行参数二的Lambda表达式
        String defaultGF = cache.get("defaultGF", key -> {
            // 根据key去数据库查询数据
            return "柳岩";
        });
        System.out.println("defaultGF = " + defaultGF);
    }
}
