package com.tianji.promotion.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tianji.promotion.domain.po.CouponScope;
import com.tianji.promotion.service.ICouponScopeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CouponServiceImplTest {
    @Autowired
    private ICouponScopeService scopeService;

    @Test
    public void query() {
        List<CouponScope> list = scopeService.lambdaQuery()
                .eq(CouponScope::getCouponId, 1570595584270774273L).list();
        System.out.println("list = " + list);

        scopeService.remove(Wrappers.lambdaQuery(CouponScope.class)
                .eq(CouponScope::getCouponId, 1900437651677253633L));
    }


}