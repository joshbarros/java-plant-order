package com.plantorder.common;

public record ReceiptEvent(String orderId, int quantity, String status) {
}
