package com.qs.takeout.modules.delivery.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RiderReviewVO {

    private Long orderId;
    private String orderNo;
    private String shopName;
    private Integer score;
    private String content;
    private List<String> imageUrls;
    private LocalDateTime createdAt;
}
