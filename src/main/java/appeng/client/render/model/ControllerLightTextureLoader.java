package appeng.client.render.model;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;

import appeng.api.util.AEColor;
import appeng.block.networking.ControllerColor;
import appeng.core.AELog;

/**
 * Atlas source that adds one recolored, switchable controller light sprite.
 */
public record ControllerLightTextureLoader(ResourceLocation resource, ResourceLocation sprite)
        implements SpriteSource {

    private static final MapCodec<ControllerLightTextureLoader> CODEC = RecordCodecBuilder.mapCodec(
            builder -> builder.group(
                    ResourceLocation.CODEC.fieldOf("resource").forGetter(ControllerLightTextureLoader::resource),
                    ResourceLocation.CODEC.fieldOf("sprite").forGetter(ControllerLightTextureLoader::sprite))
                    .apply(builder, ControllerLightTextureLoader::new));

    public static final SpriteSourceType TYPE = new SpriteSourceType(CODEC);

    @Override
    public void run(ResourceManager resourceManager, Output output) {
        var texture = TEXTURE_ID_CONVERTER.idToFile(resource);
        var textureResource = resourceManager.getResource(texture);
        if (textureResource.isEmpty()) {
            AELog.warn("Missing controller light texture: %s", texture);
            return;
        }

        var color = colorFromSpriteName(sprite);
        output.add(sprite, loader -> loader.loadSprite(sprite, textureResource.get(),
                (id, frameSize, image, metadata) -> createContents(id, frameSize, image, metadata, color)));
    }

    @Override
    public SpriteSourceType type() {
        return TYPE;
    }

    private static SpriteContents createContents(ResourceLocation id, FrameSize frameSize, NativeImage image,
            ResourceMetadata metadata, AEColor color) {
        try {
            return new ControllerLightSpriteContents(id, frameSize, image, metadata, color);
        } catch (Throwable t) {
            image.close();
            throw t;
        }
    }

    private static AEColor colorFromSpriteName(ResourceLocation name) {
        var path = name.getPath();
        var separator = path.lastIndexOf('/');
        var colorName = separator == -1 ? path : path.substring(separator + 1);

        for (var controllerColor : ControllerColor.values()) {
            if (controllerColor.getSerializedName().equals(colorName)) {
                return controllerColor.getColor();
            }
        }

        throw new IllegalArgumentException("Unknown controller color in generated sprite name: " + name);
    }
}
