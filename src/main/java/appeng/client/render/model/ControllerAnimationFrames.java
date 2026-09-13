package appeng.client.render.model;

import java.util.ArrayDeque;
import java.util.Arrays;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.util.FastColor;

import appeng.api.config.ControllerAnimation;
import appeng.api.util.AEColor;

final class ControllerAnimationFrames {

    private ControllerAnimationFrames() {
    }

    static NativeImage generate(NativeImage mask, AEColor color, ControllerAnimation animation) {
        if (animation == ControllerAnimation.RAINBOW) {
            throw new IllegalArgumentException("The original rainbow animation is supplied by the source texture");
        }
        if (mask.getWidth() <= 0 || mask.getWidth() != mask.getHeight()) {
            throw new IllegalArgumentException("Controller light masks must be non-empty and square");
        }

        var size = mask.getWidth();
        var frameCount = frameCount(animation);
        var result = new NativeImage(size, size * frameCount, false);
        try {
            var circuitDistances = animation == ControllerAnimation.CIRCUIT_TRACE
                    ? circuitDistances(mask)
                    : null;
            for (var frame = 0; frame < frameCount; frame++) {
                renderFrame(mask, result, color, animation, circuitDistances, frame);
            }
            return result;
        } catch (Throwable e) {
            result.close();
            throw e;
        }
    }

    static int frameCount(ControllerAnimation animation) {
        return animation == ControllerAnimation.SOFT_INTERFERENCE ? 240 : 48;
    }

    private static void renderFrame(NativeImage mask, NativeImage result, AEColor color,
            ControllerAnimation animation, double[][] circuitDistances, int frame) {
        var size = mask.getWidth();
        var time = frame / 48.0;
        for (var y = 0; y < size; y++) {
            for (var x = 0; x < size; x++) {
                var wave = circuitDistances == null
                        ? brightness(animation, x / (double) size, y / (double) size, time)
                        : 0.25 + 0.75 * pulse(circuitDistances[y][x] - time * 3, 5);
                var alpha = FastColor.ABGR32.alpha(mask.getPixelRGBA(x, y));
                result.setPixelRGBA(x, frame * size + y, animatedColor(color, wave, alpha));
            }
        }
    }

    private static int animatedColor(AEColor color, double wave, int alpha) {
        var red = animatedChannel(color.mediumVariant >> 16 & 255, color.whiteVariant >> 16 & 255, wave);
        var green = animatedChannel(color.mediumVariant >> 8 & 255, color.whiteVariant >> 8 & 255, wave);
        var blue = animatedChannel(color.mediumVariant & 255, color.whiteVariant & 255, wave);
        return FastColor.ABGR32.color(alpha, blue, green, red);
    }

    private static int animatedChannel(int base, int highlight, double wave) {
        return (int) Math.round(base * 0.65 * (1 - wave) + highlight * wave);
    }

    private static double brightness(ControllerAnimation animation, double x, double y, double time) {
        return switch (animation) {
            case RAINBOW -> throw new IllegalArgumentException("Rainbow uses the original controller texture");
            case WAVE -> pulse(x + y - time, 4);
            case COUNTERFLOW -> pulse(y + (x < 0.5 ? -time : time), 4);
            case CORE_RIPPLE -> pulse(Math.hypot(x - 0.5, y - 0.5) * 1.5 - time, 4);
            case BREATHING -> pulse(time, 1) * 0.65;
            case SOFT_INTERFERENCE -> 0.8 * pulse(x + y - time * 0.6, 2)
                    * pulse(x - y + time * 0.4, 2);
            case SINGULARITY -> 0.8 * pulse(
                    Math.hypot(x - 0.5, y - 0.5) * 2
                            + Math.atan2(y - 0.5, x - 0.5) / (2 * Math.PI) * 3 - time,
                    4);
            case CIRCUIT_TRACE -> throw new IllegalArgumentException("Circuit trace uses path distances");
        };
    }

    private static double[][] circuitDistances(NativeImage mask) {
        var size = mask.getWidth();
        var distances = new int[size][size];
        var directions = new int[size][size];
        for (var row : distances) {
            Arrays.fill(row, -1);
        }

        var queue = new ArrayDeque<Integer>();
        while (true) {
            var seed = findUnvisitedSeed(mask, distances);
            if (seed == -1) {
                break;
            }

            distances[seed / size][seed % size] = 0;
            directions[seed / size][seed % size] = 1;
            queue.add(seed);
            visitCircuitComponent(mask, distances, directions, queue);
        }

        var pathLengths = new double[size][size];
        for (var y = 0; y < size; y++) {
            for (var x = 0; x < size; x++) {
                pathLengths[y][x] = directions[y][x] * distances[y][x] / 5.0;
            }
        }
        return pathLengths;
    }

    private static int findUnvisitedSeed(NativeImage mask, int[][] distances) {
        var size = mask.getWidth();
        var seed = -1;
        var closestToCenter = Double.MAX_VALUE;
        for (var y = 0; y < size; y++) {
            for (var x = 0; x < size; x++) {
                if (distances[y][x] != -1 || FastColor.ABGR32.alpha(mask.getPixelRGBA(x, y)) == 0) {
                    continue;
                }

                var distanceToCenter = Math.hypot(x - (size - 1) / 2.0, y - (size - 1) / 2.0);
                if (distanceToCenter < closestToCenter) {
                    seed = y * size + x;
                    closestToCenter = distanceToCenter;
                }
            }
        }
        return seed;
    }

    private static void visitCircuitComponent(NativeImage mask, int[][] distances, int[][] directions,
            ArrayDeque<Integer> queue) {
        var size = mask.getWidth();
        while (!queue.isEmpty()) {
            var point = queue.remove();
            var x = point % size;
            var y = point / size;
            for (var dy = -1; dy <= 1; dy++) {
                for (var dx = -1; dx <= 1; dx++) {
                    var nextX = x + dx;
                    var nextY = y + dy;
                    if (!isUnvisitedCircuitPixel(mask, distances, nextX, nextY, dx, dy)) {
                        continue;
                    }

                    distances[nextY][nextX] = distances[y][x] + 1;
                    directions[nextY][nextX] = distances[y][x] == 0
                            ? ((nextX * 31 + nextY * 17 & 1) == 0 ? -1 : 1)
                            : directions[y][x];
                    queue.add(nextY * size + nextX);
                }
            }
        }
    }

    private static boolean isUnvisitedCircuitPixel(NativeImage mask, int[][] distances, int x, int y, int dx,
            int dy) {
        var size = mask.getWidth();
        return (dx != 0 || dy != 0)
                && x >= 0
                && x < size
                && y >= 0
                && y < size
                && distances[y][x] == -1
                && FastColor.ABGR32.alpha(mask.getPixelRGBA(x, y)) != 0;
    }

    private static double pulse(double phase, int sharpness) {
        return Math.pow((1 + Math.cos(2 * Math.PI * phase)) / 2, sharpness);
    }
}
