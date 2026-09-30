package com.qs.takeout.modules.delivery.fengniao;

import java.math.BigDecimal;

public interface FengNiaoClient {

    CreateResult createOrder(CreateCommand cmd);

    void cancelOrder(String partnerOrderCode, String reason);

    record CreateCommand(
            String partnerOrderCode,
            String notifyUrl,
            String storeCode,
            String shopName,
            String shopAddress,
            BigDecimal shopLat,
            BigDecimal shopLng,
            String shopPhone,
            String receiverName,
            String receiverPhone,
            String receiverAddress,
            BigDecimal receiverLat,
            BigDecimal receiverLng,
            BigDecimal goodsAmount,
            BigDecimal deliveryFee,
            String remark
    ) {
    }

    record CreateResult(String trackingNo, BigDecimal fee, String riderName, String riderPhone) {
    }
}
