package appeng.client.render.model;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.client.textures.ForgeTextureMetadata;
import net.minecraftforge.client.textures.ITextureAtlasSpriteLoader;

import appeng.api.util.AEColor;
import appeng.block.networking.ControllerColor;
import appeng.core.AppEng;

public final class ControllerLightTextureLoader
        implements ITextureAtlasSpriteLoader {

    public static final ControllerLightTextureLoader INSTANCE = new ControllerLightTextureLoader();

    private static final ResourceLocation CONTROLLER_LIGHTS = AppEng.makeId(
            "block/colorable_controller/controller_lights");

    private static final ResourceLocation CONTROLLER_COLUMN_LIGHTS = AppEng.makeId(
            "block/colorable_controller/controller_column_lights");

    private static final ResourceLocation OLD_CONTROLLER_LIGHTS_MASK = AppEng.makeId(
            "block/colorable_controller/controller_lights_mask");

    private static final ResourceLocation OLD_CONTROLLER_COLUMN_LIGHTS_MASK = AppEng.makeId(
            "block/colorable_controller/controller_column_lights_mask");

    private ControllerLightTextureLoader() {
    }

    @Override
    public SpriteContents loadContents(
            ResourceLocation name,
            Resource resource,
            FrameSize frameSize,
            NativeImage image,
            AnimationMetadataSection animationMeta,
            ForgeTextureMetadata forgeMeta) {

        if (isPassthroughTexture(name)) {
            return null;
        }

        try {
            return new ControllerLightSpriteContents(
                    name,
                    frameSize,
                    image,
                    animationMeta,
                    forgeMeta,
                    colorFromSpriteName(name));
        } catch (Throwable t) {
            image.close();
            throw t;
        }
    }

    private static boolean isPassthroughTexture(
            ResourceLocation name) {

        return name.equals(CONTROLLER_LIGHTS)
                || name.equals(CONTROLLER_COLUMN_LIGHTS)
                || name.equals(OLD_CONTROLLER_LIGHTS_MASK)
                || name.equals(OLD_CONTROLLER_COLUMN_LIGHTS_MASK);
    }

    private static AEColor colorFromSpriteName(
            ResourceLocation name) {

        var path = name.getPath();
        var separator = path.lastIndexOf('/');

        var colorName = separator == -1
                ? path
                : path.substring(separator + 1);

        for (var controllerColor : ControllerColor.values()) {
            if (controllerColor.getSerializedName()
                    .equals(colorName)) {
                return controllerColor.getColor();
            }
        }

        throw new IllegalArgumentException(
                "Unknown controller color in generated sprite name: "
                        + name);
    }

    @Override
    public TextureAtlasSprite makeSprite(
            ResourceLocation atlasName,
            SpriteContents contents,
            int atlasWidth,
            int atlasHeight,
            int spriteX,
            int spriteY,
            int mipmapLevel) {

        return new TextureAtlasSprite(
                atlasName,
                contents,
                atlasWidth,
                atlasHeight,
                spriteX,
                spriteY) {
        };
    }
}
