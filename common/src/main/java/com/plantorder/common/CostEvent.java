package com.plantorder.common;

public record CostEvent(String orderId, int amountCents, String status) {
}
