package appeng.client.gui.me.crafting;

import java.io.File;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;

import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.ForgeHooksClient;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.TerminalStyle;
import appeng.core.localization.GuiText;
import appeng.menu.me.crafting.CraftingPlanSummary;

final class CraftingPlanImageExporter {

    private static final Logger LOG = LoggerFactory.getLogger(CraftingPlanImageExporter.class);

    private static final int COLS = 3;
    private static final int ROW_HEIGHT = 23;
    private static final int HEADER_HEIGHT = 19;
    private static final int CAP_HEIGHT = 8;

    private static final int LEFT_WIDTH = 8;
    private static final int GROUP_X = 8;
    private static final int GROUP_WIDTH = 205;
    private static final int GAP_X = 213;
    private static final int GAP_WIDTH = 4;
    private static final int RIGHT_X = 231;
    private static final int RIGHT_WIDTH = 7;
    private static final int GROUP_STRIDE = GROUP_WIDTH + GAP_WIDTH;

    private static final long MAX_PIXELS = 64L * 1024 * 1024;

    private CraftingPlanImageExporter() {
    }

    static void export(AEBaseScreen<?> screen, CraftingPlanSummary plan, TerminalStyle style) {
        RenderSystem.assertOnRenderThread();
        var mc = Minecraft.getInstance();

        var entries = plan.getEntries();
        int totalRows = Math.max(1, (entries.size() + COLS - 1) / COLS);
        int groups = 1;
        double bestRatio = Double.MAX_VALUE;
        for (int k = 1; k <= totalRows; k++) {
            int w = getWidth(k);
            int h = getHeight(getRowsPerGroup(totalRows, k));
            double ratio = (double) Math.max(w, h) / Math.min(w, h);
            if (ratio < bestRatio) {
                bestRatio = ratio;
                groups = k;
            }
        }
        int rows = getRowsPerGroup(totalRows, groups);
        int width = getWidth(groups);
        int height = getHeight(rows);

        int scale = Math.max(2, (int) mc.getWindow().getGuiScale());
        int maxSize = RenderSystem.maxSupportedTextureSize();
        while (scale > 1 && (Math.max(width, height) * scale > maxSize
                || (long) width * height * scale * scale > MAX_PIXELS)) {
            scale--;
        }
        if (Math.max(width, height) * scale > maxSize) {
            mc.gui.getChat().addMessage(GuiText.CraftingPlanTooLarge.text());
            return;
        }

        NativeImage image = null;
        TextureTarget target = null;
        var oldProjection = RenderSystem.getProjectionMatrix();
        var oldSorting = RenderSystem.getVertexSorting();
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        try {
            target = new TextureTarget(width * scale, height * scale, true, Minecraft.ON_OSX);
            target.setClearColor(0, 0, 0, 0);
            target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);

            float farPlane = ForgeHooksClient.getGuiFarPlane();
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, width, height, 0, 1000, farPlane),
                    VertexSorting.ORTHOGRAPHIC_Z);
            modelView.setIdentity();
            modelView.translate(0, 0, 1000 - farPlane);
            RenderSystem.applyModelViewMatrix();
            Lighting.setupFor3DItems();

            var guiGraphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
            drawBackground(guiGraphics, style, groups, rows, width);

            var title = screen.getStyle().getText().get(AEBaseScreen.TEXT_ID_DIALOG_TITLE);
            if (title != null) {
                var pos = title.getPosition().resolve(new Rect2i(0, 0, width, height));
                var content = CraftConfirmScreen.getPlanTitle(plan).copy().withStyle(title.getText().getStyle());
                guiGraphics.drawString(mc.font, content, pos.getX(), pos.getY(),
                        screen.getStyle().getColor(title.getColor()).toARGB(), false);
            }

            for (int k = 0; k < groups; k++) {
                new CraftConfirmTableRenderer(screen, GROUP_X + 1 + k * GROUP_STRIDE, HEADER_HEIGHT, rows)
                        .render(guiGraphics, -100000, -100000, entries, k * rows);
            }
            guiGraphics.flush();

            image = new NativeImage(width * scale, height * scale, false);
            RenderSystem.bindTexture(target.getColorTextureId());
            image.downloadTexture(0, false);
            image.flipY();
        } catch (Exception e) {
            LOG.warn("Failed to render crafting plan image", e);
            if (image != null) {
                image.close();
            }
            mc.gui.getChat().addMessage(GuiText.CraftingPlanSaveFailed.text(e.getMessage()));
            return;
        } finally {
            RenderSystem.setProjectionMatrix(oldProjection, oldSorting);
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            if (target != null) {
                target.destroyBuffers();
            }
            mc.getMainRenderTarget().bindWrite(true);
        }

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getPixelRGBA(x, y);
                if (pixel >>> 24 != 0) {
                    image.setPixelRGBA(x, y, pixel | 0xFF000000);
                }
            }
        }

        var finalImage = image;
        var file = getOutputFile(new File(mc.gameDirectory, "screenshots/crafting_plans"));
        Util.ioPool().execute(() -> {
            Component message;
            try {
                file.getParentFile().mkdirs();
                finalImage.writeToFile(file);
                var link = Component.literal(file.getName()).withStyle(ChatFormatting.UNDERLINE)
                        .withStyle(s -> s.withClickEvent(
                                new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getAbsolutePath())));
                message = GuiText.CraftingPlanSaved.text(link);
            } catch (Exception e) {
                LOG.warn("Failed to save crafting plan image to {}", file, e);
                message = GuiText.CraftingPlanSaveFailed.text(e.getMessage());
            } finally {
                finalImage.close();
            }
            var finalMessage = message;
            mc.execute(() -> mc.gui.getChat().addMessage(finalMessage));
        });
    }

    private static File getOutputFile(File folder) {
        String name = Util.getFilenameFormattedDateTime();
        var file = new File(folder, name + ".png");
        for (int i = 1; file.exists(); i++) {
            file = new File(folder, name + "_" + i + ".png");
        }
        return file;
    }

    private static int getRowsPerGroup(int totalRows, int groups) {
        return Math.max(2, (totalRows + groups - 1) / groups);
    }

    private static int getWidth(int groups) {
        return LEFT_WIDTH + groups * GROUP_STRIDE + RIGHT_WIDTH;
    }

    private static int getHeight(int rows) {
        return HEADER_HEIGHT + rows * ROW_HEIGHT - 1 + CAP_HEIGHT;
    }

    private static void drawBackground(GuiGraphics guiGraphics, TerminalStyle style, int groups, int rows, int width) {
        drawBand(guiGraphics, style.getHeader(), 0, HEADER_HEIGHT, groups);

        int y = HEADER_HEIGHT;
        for (int i = 0; i < rows; i++) {
            var blitter = style.getRow();
            if (i == 0) {
                blitter = style.getFirstRow();
            } else if (i + 1 == rows) {
                blitter = style.getLastRow();
            }
            drawBand(guiGraphics, blitter, y, blitter.getSrcHeight(), groups);
            y += ROW_HEIGHT;
        }

        var bottom = style.getBottom();
        y -= 1;
        drawBand(guiGraphics, bottom, y, 4, groups);
        y += 4;
        int edgeY = bottom.getSrcY() + bottom.getSrcHeight() - 4;
        slice(bottom, 0, edgeY, LEFT_WIDTH, 4).dest(0, y).blit(guiGraphics);
        slice(bottom, 100, edgeY, 1, 4).dest(LEFT_WIDTH, y, width - LEFT_WIDTH - 11, 4).blit(guiGraphics);
        slice(bottom, 227, bottom.getSrcY() + 4, 11, 4).dest(width - 11, y).blit(guiGraphics);
    }

    private static void drawBand(GuiGraphics guiGraphics, Blitter blitter, int y, int height, int groups) {
        int srcY = blitter.getSrcY();
        slice(blitter, 0, srcY, LEFT_WIDTH, height).dest(0, y).blit(guiGraphics);
        int x = LEFT_WIDTH;
        for (int k = 0; k < groups; k++) {
            slice(blitter, GROUP_X, srcY, GROUP_WIDTH, height).dest(x, y).blit(guiGraphics);
            slice(blitter, GAP_X, srcY, GAP_WIDTH, height).dest(x + GROUP_WIDTH, y).blit(guiGraphics);
            x += GROUP_STRIDE;
        }
        slice(blitter, RIGHT_X, srcY, RIGHT_WIDTH, height).dest(x, y).blit(guiGraphics);
    }

    private static Blitter slice(Blitter blitter, int x, int y, int w, int h) {
        return blitter.copy().src(blitter.getSrcX() + x, y, w, h);
    }
}
