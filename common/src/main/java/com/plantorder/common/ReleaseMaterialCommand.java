package com.plantorder.common;

public record ReleaseMaterialCommand(String orderId, String sku, int quantity) {
}
