package com.qs.takeout.modules.delivery.fengniao;

/** 蜂鸟 Anubis 订单状态码 + 本地发单状态。 */
public final class FengNiaoStatuses {

    public static final int ANUBIS_CREATED = 0;
    public static final int ANUBIS_WAYBILL = 1;
    public static final int ANUBIS_RIDER_ACCEPT = 20;
    public static final int ANUBIS_ARRIVED_SHOP = 80;
    public static final int ANUBIS_DELIVERING = 2;
    public static final int ANUBIS_DONE = 3;
    public static final int ANUBIS_CANCELLED = 4;
    public static final int ANUBIS_EXCEPTION = 5;

    public static final String CREATED = "CREATED";
    public static final String RIDER_ACCEPTED = "RIDER_ACCEPTED";
    public static final String ARRIVED = "ARRIVED";
    public static final String DELIVERING = "DELIVERING";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";
    public static final String FAILED = "FAILED";

    private FengNiaoStatuses() {
    }

    public static String localOf(int anubis) {
        return switch (anubis) {
            case ANUBIS_RIDER_ACCEPT -> RIDER_ACCEPTED;
            case ANUBIS_ARRIVED_SHOP -> ARRIVED;
            case ANUBIS_DELIVERING -> DELIVERING;
            case ANUBIS_DONE -> COMPLETED;
            case ANUBIS_CANCELLED, ANUBIS_EXCEPTION -> CANCELLED;
            default -> CREATED;
        };
    }

    public static String labelOf(String local) {
        return switch (local) {
            case RIDER_ACCEPTED -> "蜂鸟骑手已接单";
            case ARRIVED -> "骑手已到店";
            case DELIVERING -> "蜂鸟配送中";
            case COMPLETED -> "蜂鸟已送达";
            case CANCELLED -> "蜂鸟运单已取消";
            case FAILED -> "蜂鸟发单失败";
            default -> "已呼叫蜂鸟，等待接单";
        };
    }
}
