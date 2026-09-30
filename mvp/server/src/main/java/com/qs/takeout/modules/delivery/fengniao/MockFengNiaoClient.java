package com.qs.takeout.modules.delivery.fengniao;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

/** 本地联调：不打真实蜂鸟，返回模拟运单号。 */
public class MockFengNiaoClient implements FengNiaoClient {

    private static final String[] NAMES = {"张伟", "李强", "王芳", "刘洋", "陈静"};

    @Override
    public CreateResult createOrder(CreateCommand cmd) {
        int i = ThreadLocalRandom.current().nextInt(NAMES.length);
        String phone = "1380013800" + i;
        BigDecimal fee = cmd.deliveryFee() == null ? BigDecimal.ZERO : cmd.deliveryFee();
        return new CreateResult("FN" + System.currentTimeMillis(), fee, "蜂鸟·" + NAMES[i], phone);
    }

    @Override
    public void cancelOrder(String partnerOrderCode, String reason) {
        // mock no-op
    }
}
