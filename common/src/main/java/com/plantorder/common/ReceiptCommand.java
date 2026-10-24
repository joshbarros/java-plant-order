package com.plantorder.common;

public record ReceiptCommand(String orderId, int quantity, String dock) {
}
