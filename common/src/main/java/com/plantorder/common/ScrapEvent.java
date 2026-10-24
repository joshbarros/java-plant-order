package com.plantorder.common;

public record ScrapEvent(String orderId, int quantity, String status) {
}
