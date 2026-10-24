package com.plantorder.common;

public record CostCommand(String orderId, int materialCents, int laborCents) {
}
