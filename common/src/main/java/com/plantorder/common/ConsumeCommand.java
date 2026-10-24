package com.plantorder.common;

public record ConsumeCommand(String orderId, int lineCount) {
}
