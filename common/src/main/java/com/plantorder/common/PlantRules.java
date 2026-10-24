package com.plantorder.common;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class PlantRules {
    private PlantRules() {
    }

    public static boolean brokenStation(String token) {
        return token != null && token.toLowerCase(Locale.ROOT).contains("broken");
    }

    public static boolean holdLot(String lot) {
        return lot != null && lot.toLowerCase(Locale.ROOT).contains("hold");
    }

    public static boolean scrapLot(String lot) {
        return lot != null && lot.toLowerCase(Locale.ROOT).contains("scrap");
    }

    public static boolean closedDock(String dock) {
        return dock != null && dock.toLowerCase(Locale.ROOT).contains("closed");
    }

    public static boolean releaseMaterial(String status) {
        return switch (status) {
            case "EXPLODED", "RESERVED", "RELEASED", "STATIONED", "ACCEPTED" -> true;
            default -> false;
        };
    }

    public static boolean voidRelease(String status) {
        return switch (status) {
            case "RELEASED", "STATIONED", "ACCEPTED" -> true;
            default -> false;
        };
    }

    public static boolean voidQuality(String status) {
        return "ACCEPTED".equals(status);
    }

    public static List<PartQty> parse(String lines) {
        List<PartQty> parsed = new ArrayList<>();
        if (lines == null || lines.isBlank()) {
            return parsed;
        }
        for (String part : lines.split("\\|")) {
            String[] bits = part.split(":");
            if (bits.length != 3 || bits[0].isBlank()) {
                throw new IllegalArgumentException("line");
            }
            parsed.add(new PartQty(bits[0], Integer.parseInt(bits[1]), Integer.parseInt(bits[2])));
        }
        return parsed;
    }

    public static String format(List<PartQty> parts) {
        StringBuilder builder = new StringBuilder();
        for (PartQty part : parts) {
            if (builder.length() > 0) {
                builder.append('|');
            }
            builder.append(part.sku()).append(':').append(part.quantity()).append(':').append(part.standardCents());
        }
        return builder.toString();
    }

    public static String explode(String lines, int orderQty) {
        List<PartQty> exploded = new ArrayList<>();
        for (PartQty part : parse(lines)) {
            exploded.add(new PartQty(part.sku(), Math.multiplyExact(part.quantity(), orderQty), part.standardCents()));
        }
        exploded.sort(Comparator.comparing(PartQty::sku));
        return format(exploded);
    }

    public static int materialCents(String exploded) {
        int total = 0;
        for (PartQty part : parse(exploded)) {
            total = Math.addExact(total, Math.multiplyExact(part.quantity(), part.standardCents()));
        }
        return total;
    }

    public static int laborCents(int rateCents, int quantity) {
        return Math.multiplyExact(rateCents, quantity);
    }

    public static int rollup(int materialCents, int laborCents) {
        return Math.addExact(materialCents, laborCents);
    }

    public record PartQty(String sku, int quantity, int standardCents) {
    }
}
