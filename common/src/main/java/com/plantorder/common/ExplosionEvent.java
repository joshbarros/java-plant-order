package com.plantorder.common;

public record ExplosionEvent(String orderId, String lines, int materialCents, String status) {
}
