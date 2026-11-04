package com.restauranthub.entity.enums;

public enum OrderStatus {
    PENDING_PAYMENT,
    PLACED,
    ORDER_PLACED,
    ACCEPTED,
    ORDER_CONFIRMED,
    PREPARING,
    RESTAURANT_PREPARING,
    READY_FOR_PICKUP,
    FOOD_READY,
    OUT_FOR_DELIVERY,
    COMPLETED,
    DELIVERED,
    CANCELLED,
    REFUNDED,
    PAYMENT_FAILED;

    public static OrderStatus fromString(String statusStr) {
        if (statusStr == null || statusStr.trim().isEmpty()) {
            return PLACED;
        }
        String normalized = statusStr.trim().toUpperCase();
        return switch (normalized) {
            case "ORDER_PLACED", "PLACED" -> PLACED;
            case "ORDER_CONFIRMED", "ACCEPTED", "CONFIRMED" -> ACCEPTED;
            case "RESTAURANT_PREPARING", "PREPARING" -> PREPARING;
            case "FOOD_READY", "READY_FOR_PICKUP", "READY" -> READY_FOR_PICKUP;
            case "OUT_FOR_DELIVERY", "ON_THE_WAY" -> OUT_FOR_DELIVERY;
            case "DELIVERED", "COMPLETED" -> COMPLETED;
            case "CANCELLED", "CANCELED" -> CANCELLED;
            case "PENDING_PAYMENT" -> PENDING_PAYMENT;
            case "REFUNDED" -> REFUNDED;
            case "PAYMENT_FAILED" -> PAYMENT_FAILED;
            default -> valueOf(normalized);
        };
    }

    public String getDisplayLabel() {
        return switch (this) {
            case PLACED, ORDER_PLACED -> "Order Placed";
            case ACCEPTED, ORDER_CONFIRMED -> "Order Confirmed";
            case PREPARING, RESTAURANT_PREPARING -> "Restaurant Preparing";
            case READY_FOR_PICKUP, FOOD_READY -> "Food Ready";
            case OUT_FOR_DELIVERY -> "Out for Delivery";
            case COMPLETED, DELIVERED -> "Delivered";
            case CANCELLED -> "Cancelled";
            default -> name();
        };
    }
}
