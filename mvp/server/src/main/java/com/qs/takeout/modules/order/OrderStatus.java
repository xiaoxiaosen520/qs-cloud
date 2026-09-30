package com.qs.takeout.modules.order;

/**
 * 订单状态常量。流转见 docs/modules.md。
 */
public final class OrderStatus {

    public static final String PENDING_PAY = "PENDING_PAY";
    public static final String PAID = "PAID";
    public static final String ACCEPTED = "ACCEPTED";
    public static final String DELIVERING = "DELIVERING";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";
    public static final String REFUNDING = "REFUNDING";
    public static final String REFUNDED = "REFUNDED";

    private OrderStatus() {
    }
}
