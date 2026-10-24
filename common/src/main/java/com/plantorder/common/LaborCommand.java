package com.plantorder.common;

public record LaborCommand(String orderId, int rateCents, int quantity) {
}
