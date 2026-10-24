package com.plantorder.common;

public final class Channels {
    public static final String ITEMS = "plant.items";
    public static final String BOMS = "plant.boms";
    public static final String ORDERS = "plant.orders";
    public static final String EXPLOSIONS = "plant.explosions";
    public static final String MATERIALS = "plant.materials";
    public static final String RELEASES = "plant.releases";
    public static final String STATIONS = "plant.stations";
    public static final String QUALITY = "plant.quality";
    public static final String SCRAPS = "plant.scraps";
    public static final String RECEIPTS = "plant.receipts";
    public static final String LABOR = "plant.labor";
    public static final String COSTS = "plant.costs";

    public static final String EXPLODE = "plant.explosion.explode";
    public static final String EXPLODE_DLQ = "plant.explosion.explode.dlq";
    public static final String RESERVE = "plant.material.reserve";
    public static final String RESERVE_DLQ = "plant.material.reserve.dlq";
    public static final String MATERIAL_RELEASE = "plant.material.release";
    public static final String MATERIAL_RELEASE_DLQ = "plant.material.release.dlq";
    public static final String CONSUME = "plant.material.consume";
    public static final String CONSUME_DLQ = "plant.material.consume.dlq";
    public static final String FLOOR = "plant.release.issue";
    public static final String FLOOR_DLQ = "plant.release.issue.dlq";
    public static final String FLOOR_VOID = "plant.release.void";
    public static final String FLOOR_VOID_DLQ = "plant.release.void.dlq";
    public static final String STATION = "plant.station.pass";
    public static final String STATION_DLQ = "plant.station.pass.dlq";
    public static final String INSPECT = "plant.quality.inspect";
    public static final String INSPECT_DLQ = "plant.quality.inspect.dlq";
    public static final String QUALITY_VOID = "plant.quality.void";
    public static final String QUALITY_VOID_DLQ = "plant.quality.void.dlq";
    public static final String SCRAP = "plant.scrap.record";
    public static final String SCRAP_DLQ = "plant.scrap.record.dlq";
    public static final String RECEIVE = "plant.receipt.receive";
    public static final String RECEIVE_DLQ = "plant.receipt.receive.dlq";
    public static final String ACCRUE = "plant.labor.accrue";
    public static final String LABOR_DLQ = "plant.labor.accrue.dlq";
    public static final String COST = "plant.cost.roll";
    public static final String COST_DLQ = "plant.cost.roll.dlq";

    private Channels() {
    }
}
