package com.plantorder.common;

public record ReserveCommand(String orderId, String sku, int quantity) {
}
