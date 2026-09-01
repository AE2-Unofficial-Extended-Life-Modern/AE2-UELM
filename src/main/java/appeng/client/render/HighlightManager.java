package appeng.client.render;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.systems.RenderSystem;

import org.jetbrains.annotations.Nullable;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import appeng.api.parts.IPartHost;
import appeng.client.render.overlay.OverlayRenderType;
import appeng.parts.BusCollisionHelper;

/**
 * generic highlighting for full blocks and cable parts
 * <p>
 * base part highlight logic copied from {@link appeng.hooks.RenderBlockOutlineHook}
 */
public final class HighlightManager {

    public static final HighlightManager INSTANCE = new HighlightManager();

    private static final long HIGHLIGHT_DURATION_MS = 15_000;
    private static final long BLINK_INTERVAL_MS = 500;

    private final Map<Target, Long> targets = new LinkedHashMap<>();

    private HighlightManager() {
    }

    public static void highlight(ResourceKey<Level> dimension, BlockPos pos, @Nullable Direction side) {
        var target = new Target(dimension, pos.immutable(), side);
        INSTANCE.targets.put(target, Util.getMillis() + HIGHLIGHT_DURATION_MS);
    }

    @SubscribeEvent
    public void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || targets.isEmpty()) {
            return;
        }

        var now = Util.getMillis();
        targets.values().removeIf(expiresAt -> now >= expiresAt);
        if (targets.isEmpty()) {
            return;
        }
        if ((now / BLINK_INTERVAL_MS & 1) != 0) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        var currentDimension = minecraft.level.dimension();
        var hasVisibleTarget = targets.keySet().stream()
                .anyMatch(target -> target.dimension.equals(currentDimension));
        if (!hasVisibleTarget) {
            return;
        }

        var camera = event.getCamera();
        if (!camera.isInitialized()) {
            return;
        }

        var poseStack = event.getPoseStack();
        var bufferSource = minecraft.renderBuffers().bufferSource();
        var renderType = OverlayRenderType.getBlockHilightLine();
        var cameraPosition = camera.getPosition();

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        try {
            var buffer = bufferSource.getBuffer(renderType);
            for (var target : targets.keySet()) {
                if (target.dimension.equals(currentDimension)) {
                    for (var bounds : getBounds(minecraft.level, target.pos, target.side)) {
                        LevelRenderer.renderShape(poseStack, buffer, Shapes.create(bounds),
                                target.pos.getX() - cameraPosition.x,
                                target.pos.getY() - cameraPosition.y,
                                target.pos.getZ() - cameraPosition.z,
                                1.0f, 0.0f, 0.0f, 1.0f);
                    }
                }
            }
            bufferSource.endBatch(renderType);
        } finally {
            RenderSystem.enableDepthTest();
            RenderSystem.defaultBlendFunc();
        }
    }

    static List<AABB> getBounds(Level level, BlockPos pos, @Nullable Direction side) {
        if (side != null && level.getBlockEntity(pos) instanceof IPartHost partHost) {
            var part = partHost.getPart(side);
            if (part != null) {
                var boxes = new ArrayList<AABB>();
                part.getBoxes(new BusCollisionHelper(boxes, side, true));
                if (!boxes.isEmpty()) {
                    return boxes;
                }
            }
        }

        var blockShape = level.getBlockState(pos).getShape(level, pos);
        return blockShape.isEmpty() ? List.of(new AABB(0, 0, 0, 1, 1, 1)) : blockShape.toAabbs();
    }

    private record Target(ResourceKey<Level> dimension, BlockPos pos, @Nullable Direction side) {
    }
}
