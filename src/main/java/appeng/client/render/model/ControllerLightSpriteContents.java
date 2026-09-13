package appeng.client.render.model;

import java.util.List;

import javax.annotation.Nullable;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteTicker;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraftforge.client.textures.ForgeTextureMetadata;

import appeng.api.config.ControllerAnimation;
import appeng.api.util.AEColor;
import appeng.core.AEConfig;

/**
 * Controller light sprite that can change its animation without rebuilding the texture atlas
 */
final class ControllerLightSpriteContents extends SpriteContents {

    private final NativeImage mask;
    private final AEColor color;

    private int mipLevel;

    @Nullable
    private ControllerAnimation generatedAnimation;

    @Nullable
    private SpriteContents generatedContents;

    ControllerLightSpriteContents(
            ResourceLocation name,
            FrameSize frameSize,
            NativeImage rainbowImage,
            AnimationMetadataSection rainbowMetadata,
            ForgeTextureMetadata forgeMeta,
            AEColor color) {

        super(
                name,
                frameSize,
                rainbowImage,
                rainbowMetadata,
                forgeMeta);

        if (frameSize.width() <= 0
                || frameSize.height() <= 0
                || frameSize.width() != frameSize.height()) {
            throw new IllegalArgumentException(
                    "Controller light frames must be non-empty and square");
        }

        this.color = color;
        this.mask = createMask(rainbowImage, frameSize);
    }

    /**
     * Extracts the alpha mask from the first frame
     */
    private static NativeImage createMask(
            NativeImage image,
            FrameSize frameSize) {

        var width = frameSize.width();
        var height = frameSize.height();

        if (image.getWidth() < width || image.getHeight() < height) {
            throw new IllegalArgumentException(
                    "Controller animation sheet is smaller than one frame");
        }

        var mask = new NativeImage(width, height, false);

        try {
            for (var y = 0; y < height; y++) {
                for (var x = 0; x < width; x++) {
                    var alpha = FastColor.ABGR32.alpha(
                            image.getPixelRGBA(x, y));

                    mask.setPixelRGBA(
                            x,
                            y,
                            FastColor.ABGR32.color(
                                    alpha,
                                    255,
                                    255,
                                    255));
                }
            }

            return mask;
        } catch (Throwable t) {
            mask.close();
            throw t;
        }
    }

    @Override
    public void increaseMipLevel(int mipLevel) {
        super.increaseMipLevel(mipLevel);

        this.mipLevel = Math.max(this.mipLevel, mipLevel);

        if (generatedContents != null) {
            generatedContents.increaseMipLevel(mipLevel);
        }
    }

    /**
     * Gets the generated SpriteContents for a non-rainbow animation
     */
    private SpriteContents getGeneratedContents(
            ControllerAnimation animation) {

        if (animation == ControllerAnimation.RAINBOW) {
            throw new IllegalArgumentException(
                    "Rainbow uses the original SpriteContents");
        }

        if (generatedContents != null
                && generatedAnimation == animation) {
            return generatedContents;
        }

        discardGeneratedContents();

        var generated = ControllerAnimationFrames.generate(
                mask,
                color,
                animation);

        var size = mask.getWidth();

        var metadata = new AnimationMetadataSection(
                List.of(),
                size,
                size,
                1,
                false);

        var generatedName = new ResourceLocation(
                name().getNamespace(),
                name().getPath() + "/" + animation.textureSuffix());

        var contents = new SpriteContents(
                generatedName,
                new FrameSize(size, size),
                generated,
                metadata);

        try {
            if (mipLevel > 0) {
                contents.increaseMipLevel(mipLevel);
            }
        } catch (Throwable t) {
            contents.close();
            throw t;
        }

        generatedAnimation = animation;
        generatedContents = contents;

        return contents;
    }

    private void discardGeneratedContents() {
        if (generatedContents != null) {
            generatedContents.close();
            generatedContents = null;
            generatedAnimation = null;
        }
    }

    /**
     * Upload whatever animation is currently selected when the atlas is first populated
     */
    @Override
    public void uploadFirstFrame(int x, int y) {
        uploadFirstFrame(
                AEConfig.instance().getControllerAnimation(),
                x,
                y);
    }

    private void uploadFirstFrame(
            ControllerAnimation animation,
            int x,
            int y) {

        if (animation == ControllerAnimation.RAINBOW) {
            super.uploadFirstFrame(x, y);
        } else {
            getGeneratedContents(animation)
                    .uploadFirstFrame(x, y);
        }
    }

    @Override
    public SpriteTicker createTicker() {
        return new SwitchingTicker();
    }

    @Nullable
    private SpriteTicker createTicker(
            ControllerAnimation animation) {

        if (animation == ControllerAnimation.RAINBOW) {
            return super.createTicker();
        }

        return getGeneratedContents(animation).createTicker();
    }

    private final class SwitchingTicker implements SpriteTicker {

        private ControllerAnimation activeAnimation;

        @Nullable
        private SpriteTicker activeTicker;

        private SwitchingTicker() {
            activeAnimation = AEConfig.instance()
                    .getControllerAnimation();

            activeTicker = ControllerLightSpriteContents.this
                    .createTicker(activeAnimation);
        }

        @Override
        public void tickAndUpload(int x, int y) {
            var requestedAnimation = AEConfig.instance()
                    .getControllerAnimation();

            if (requestedAnimation != activeAnimation) {
                closeActiveTicker();

                activeAnimation = requestedAnimation;

                ControllerLightSpriteContents.this.uploadFirstFrame(
                        activeAnimation,
                        x,
                        y);

                activeTicker = ControllerLightSpriteContents.this
                        .createTicker(activeAnimation);

                return;
            }

            if (activeTicker != null) {
                activeTicker.tickAndUpload(x, y);
            }
        }

        private void closeActiveTicker() {
            if (activeTicker != null) {
                activeTicker.close();
                activeTicker = null;
            }
        }

        @Override
        public void close() {
            closeActiveTicker();
        }
    }

    @Override
    public void close() {
        try {
            discardGeneratedContents();
            mask.close();
        } finally {
            super.close();
        }
    }
}
