package com.tianji.promotion.constants;

public interface PromotionConstants {
    // 生成优惠券序列号 自增 String
    String COUPON_CODE_SERIAL_KEY = "coupon:code:serial";
    // 优惠券是否兑换 BitMap
    String COUPON_CODE_MAP_KEY = "coupon:code:map";
    // 优惠券信息 Hash
    String COUPON_CACHE_KEY_PREFIX = "prs:coupon:";
    // 用户优惠券信息 Hash
    String USER_COUPON_CACHE_KEY_PREFIX = "prs:user:coupon:";
    // 兑换码的最大序列号 Zset
    String COUPON_RANGE_KEY = "coupon:code:range";

    String[] RECEIVE_COUPON_ERROR_MSG = {
            "活动未开始",
            "库存不足",
            "活动已经结束",
            "领取次数过多",
    };
    String[] EXCHANGE_COUPON_ERROR_MSG = {
            "兑换码已兑换",
            "无效兑换码",
            "活动未开始",
            "活动已经结束",
            "领取次数过多",
    };
}
