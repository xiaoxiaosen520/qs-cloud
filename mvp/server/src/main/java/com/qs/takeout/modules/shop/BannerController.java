package com.qs.takeout.modules.shop;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qs.takeout.common.result.ApiResult;
import com.qs.takeout.modules.shop.entity.Banner;
import com.qs.takeout.modules.shop.mapper.BannerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
public class BannerController {

    private final BannerMapper bannerMapper;

    @GetMapping
    public ApiResult<List<Banner>> list() {
        return ApiResult.ok(bannerMapper.selectList(new LambdaQueryWrapper<Banner>()
                .eq(Banner::getStatus, 1)
                .orderByAsc(Banner::getSort)
                .orderByDesc(Banner::getId)));
    }
}
