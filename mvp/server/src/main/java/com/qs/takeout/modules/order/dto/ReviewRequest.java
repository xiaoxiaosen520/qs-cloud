package com.qs.takeout.modules.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ReviewRequest {

    @NotNull
    @Min(1)
    @Max(5)
    private Integer score;

    private String content;

    /** 评价图片，最多 9 张，值为 /uploads/... */
    @Size(max = 9)
    private List<String> imageUrls;
}
