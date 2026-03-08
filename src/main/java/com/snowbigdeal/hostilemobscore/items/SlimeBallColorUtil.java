package com.snowbigdeal.hostilemobscore.items;

import net.minecraft.world.item.DyeColor;

/** Utility methods shared between slimeball merge mixins and the colored slime block. */
public final class SlimeBallColorUtil {

    private SlimeBallColorUtil() {}

    /**
     * Weighted linear RGB blend: each channel is averaged by stack count.
     *
     * @param colorA  packed RGB (0xRRGGBB) of the existing/destination stack
     * @param countA  number of items in the existing/destination stack
     * @param colorB  packed RGB of the incoming/source stack
     * @param countB  number of items being transferred from the source
     */
    public static int blend(int colorA, int countA, int colorB, int countB) {
        int total = countA + countB;
        int r = ((colorA >> 16 & 0xFF) * countA + (colorB >> 16 & 0xFF) * countB) / total;
        int g = ((colorA >>  8 & 0xFF) * countA + (colorB >>  8 & 0xFF) * countB) / total;
        int b = ((colorA       & 0xFF) * countA + (colorB       & 0xFF) * countB) / total;
        return (r << 16) | (g << 8) | b;
    }

    /** Returns the packed RGB (0xRRGGBB) for a {@link DyeColor}'s texture diffuse color. */
    public static int dyeColorToRgb(DyeColor dye) {
        return dye.getTextureDiffuseColor() & 0x00FFFFFF;
    }

    /** Returns the {@link DyeColor} whose texture color is closest to the given packed RGB. */
    public static DyeColor nearestDyeColor(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >>  8) & 0xFF;
        int b =  rgb        & 0xFF;
        DyeColor best = DyeColor.WHITE;
        double bestDist = Double.MAX_VALUE;
        for (DyeColor dye : DyeColor.values()) {
            int dc = dye.getTextureDiffuseColor();
            double dr = r - ((dc >> 16) & 0xFF);
            double dg = g - ((dc >>  8) & 0xFF);
            double db = b - ( dc        & 0xFF);
            double dist = dr * dr + dg * dg + db * db;
            if (dist < bestDist) {
                bestDist = dist;
                best = dye;
            }
        }
        return best;
    }
}
