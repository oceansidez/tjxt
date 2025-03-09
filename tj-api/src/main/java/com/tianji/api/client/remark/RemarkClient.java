package com.tianji.api.client.remark;

import com.tianji.api.client.remark.fallback.RemarkClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;

@FeignClient(value = "remark-service", fallbackFactory = RemarkClientFallback.class)
public interface RemarkClient {

    /**
     * 根据业务id查询点赞状态 返回点赞的id
     *
     * @param bizIds
     * @return
     */
    @GetMapping("/likes/list")
    Set<Long> isBizLiked(@RequestParam("bizIds") Iterable<Long> bizIds);
}
