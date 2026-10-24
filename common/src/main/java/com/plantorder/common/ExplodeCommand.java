package com.plantorder.common;

public record ExplodeCommand(String orderId, String lines, int quantity) {
}
