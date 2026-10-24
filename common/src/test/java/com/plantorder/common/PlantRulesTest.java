package com.plantorder.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlantRulesTest {

    @Test
    void stationLotAndDockAreExact() {
        assertTrue(PlantRules.brokenStation("STATION-BROKEN"));
        assertFalse(PlantRules.brokenStation("STATION-1"));
        assertTrue(PlantRules.holdLot("LOT-HOLD"));
        assertFalse(PlantRules.holdLot("LOT-A"));
        assertTrue(PlantRules.scrapLot("LOT-SCRAP"));
        assertFalse(PlantRules.scrapLot("LOT-HOLD"));
        assertTrue(PlantRules.closedDock("DOCK-CLOSED"));
        assertFalse(PlantRules.closedDock("DOCK-1"));
    }

    @Test
    void compensationFollowsTheStepsThatAlreadyCommitted() {
        assertFalse(PlantRules.releaseMaterial("DRAFT"));
        assertTrue(PlantRules.releaseMaterial("EXPLODED"));
        assertTrue(PlantRules.releaseMaterial("ACCEPTED"));
        assertFalse(PlantRules.releaseMaterial("COSTED"));
        assertFalse(PlantRules.voidRelease("EXPLODED"));
        assertTrue(PlantRules.voidRelease("RELEASED"));
        assertTrue(PlantRules.voidRelease("ACCEPTED"));
        assertFalse(PlantRules.voidQuality("STATIONED"));
        assertTrue(PlantRules.voidQuality("ACCEPTED"));
    }

    @Test
    void explosionRollsMaterialAndLaborInIntegers() {
        String exploded = PlantRules.explode("NUT:2:30|BOLT:4:20", 2);
        assertEquals("BOLT:8:20|NUT:4:30", exploded);
        assertEquals(280, PlantRules.materialCents(exploded));
        assertEquals(200, PlantRules.laborCents(100, 2));
        assertEquals(480, PlantRules.rollup(280, 200));
        assertEquals("", PlantRules.explode("", 2));
        assertEquals(0, PlantRules.materialCents(""));
        assertThrows(ArithmeticException.class, () -> PlantRules.explode("BOLT:2147483647:1", 2));
    }
}
