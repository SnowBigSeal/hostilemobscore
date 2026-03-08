package com.snowbigdeal.hostilemobscore.items;

import net.minecraft.world.item.DyeColor;

import java.util.EnumMap;
import java.util.Map;

/**
 * Fixed dye-pair mixing table, mirroring vanilla Minecraft dye crafting recipes.
 * Mixing is count-independent: 1 red + 100 blue gives the same color as 50 red + 50 blue.
 * For pairs not in the table, the majority color wins (ties keep colorA).
 */
public final class DyeColorMixing {

    private DyeColorMixing() {}

    private static final Map<DyeColor, Map<DyeColor, DyeColor>> TABLE = new EnumMap<>(DyeColor.class);

    static {
        add(DyeColor.RED,    DyeColor.BLUE,   DyeColor.PURPLE);
        add(DyeColor.RED,    DyeColor.WHITE,  DyeColor.PINK);
        add(DyeColor.RED,    DyeColor.YELLOW, DyeColor.ORANGE);
        add(DyeColor.BLUE,   DyeColor.WHITE,  DyeColor.LIGHT_BLUE);
        add(DyeColor.BLUE,   DyeColor.GREEN,  DyeColor.CYAN);
        add(DyeColor.BLUE,   DyeColor.YELLOW, DyeColor.CYAN);
        add(DyeColor.GREEN,  DyeColor.WHITE,  DyeColor.LIME);
        add(DyeColor.BLACK,  DyeColor.WHITE,  DyeColor.GRAY);
        add(DyeColor.GRAY,   DyeColor.WHITE,  DyeColor.LIGHT_GRAY);
        add(DyeColor.PINK,   DyeColor.PURPLE, DyeColor.MAGENTA);
    }

    private static void add(DyeColor a, DyeColor b, DyeColor result) {
        TABLE.computeIfAbsent(a, k -> new EnumMap<>(DyeColor.class)).put(b, result);
        TABLE.computeIfAbsent(b, k -> new EnumMap<>(DyeColor.class)).put(a, result);
    }

    /**
     * Mix two DyeColors together.
     * If the pair has a known recipe result, returns that.
     * Otherwise returns whichever color has more items; ties keep {@code colorA}.
     */
    public static DyeColor mix(DyeColor colorA, int countA, DyeColor colorB, int countB) {
        if (colorA == colorB) return colorA;
        Map<DyeColor, DyeColor> row = TABLE.get(colorA);
        if (row != null) {
            DyeColor result = row.get(colorB);
            if (result != null) return result;
        }
        return countB > countA ? colorB : colorA;
    }
}
