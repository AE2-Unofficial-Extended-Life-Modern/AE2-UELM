package appeng.util;

import java.text.DecimalFormat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/**
 * Utility class for number-related operations.
 */
public class NumberUtil {
    private static final String[] UNITS = { "", "K", "M", "G", "T", "P", "E", "Y", "Z", "R", "Q" };
    private static final DecimalFormat DF = new DecimalFormat("#.##");

    public static String formatNumber(double number) {
        if (number < 1000)
            return DF.format(number);
        int unit = Math.min((int) (Math.log10(number) / 3), UNITS.length - 1);
        return DF.format(number / Math.pow(1000, unit)) + UNITS[unit];
    }

    public static Component coloredPercentage(double available, double requested, boolean hasMissing) {
        if (requested <= 0)
            return Component.literal("0%").withStyle(ChatFormatting.GREEN);

        double percentage = available / requested + (hasMissing ? 1 : 0);
        String percentageText = formatNumber(percentage * 100) + "%";

        if (percentage > 1.0) {
            return Component.literal(percentageText)
                    .withStyle(Style.EMPTY.withColor(0xFF0000));
        }

        return Component.literal(percentageText)
                .withStyle(style -> style.withColor(color(percentage)));
    }

    public static Component coloredText(Component text, double percentage) {
        return text.copy().withStyle(style -> style.withColor(color(percentage)));
    }

    private static int color(double percentage) {
        int red, green, blue = 0;
        if (percentage <= 0.33) {
            double localPercentage = percentage / 0.33;
            red = (int) (localPercentage * 180);
            green = 180;
        } else if (percentage <= 0.66) {
            double localPercentage = (percentage - 0.33) / 0.33;
            red = 180;
            green = (int) (180 - (localPercentage * 90));
        } else {
            double localPercentage = (percentage - 0.66) / 0.34;
            red = (int) (180 + (localPercentage * 75));
            green = (int) (90 - (localPercentage * 90));
        }

        return (red << 16) | (green << 8) | blue;
    }
}
