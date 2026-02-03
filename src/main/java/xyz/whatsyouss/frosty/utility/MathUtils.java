package xyz.whatsyouss.frosty.utility;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class MathUtils {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public static final Random rand = new Random();

    public static double roundToDecimals(double value, int places) {
        return Double.parseDouble(String.format("%." + places + "f", value));
    }

    /**
     * Wraps the angle to the [-180, 180) range.
     */
    public static double wrapAngleTo180(double angle) {
        return angle - Math.floor(angle / 360.0 + 0.5) * 360.0;
    }

    public static float wrapAngleTo180(float value) {
        if ((value %= 360.0f) >= 180.0f) {
            value -= 360.0f;
        }
        if (value < -180.0f) {
            value += 360.0f;
        }
        return value;
    }

    /**
     * Reverses the given character array.
     */
    public static void reverseCharArray(char[] array) {
        int left = 0;
        for (int right = array.length - 1; left < right; ++left, --right) {
            char temp = array[left];
            array[left] = array[right];
            array[right] = temp;
        }
    }

    public static double sq(double in) {
        return in * in;
    }

    public static double randomDouble(double min, double max) {
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    public static double interpolate(double oldValue, double newValue, double interpolationValue) {
        return oldValue + (newValue - oldValue) * interpolationValue;
    }

    public static float calculateGaussianValue(float x, float sigma) {
        double PI = 3.141592653;
        double output = 1.0 / Math.sqrt(2.0 * PI * (double)(sigma * sigma));
        return (float)(output * Math.exp((double)(-(x * x)) / (2.0 * (double)(sigma * sigma))));
    }

    public static double roundToDecimal(double n, int point) {
        if (point == 0) {
            return Math.floor(n);
        }
        double factor = Math.pow(10.0, point);
        return (double)Math.round(n * factor) / factor;
    }

    public static float clamp(double num, double min, double max) {
        return (float)(num < min ? min : Math.min(num, max));
    }


    public static double randomizeDouble(double min, double max) {
        return min + (max - min) * rand.nextDouble();
    }
}