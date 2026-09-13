package appeng.client.render.model;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;

import appeng.block.networking.ControllerColor;
import appeng.client.render.BasicUnbakedModel;
import appeng.core.AppEng;

public class ControllerModel implements BasicUnbakedModel {

    public enum CasingType {
        NONE(null),
        BLOCK("controller"),
        POWERED_BLOCK("controller_powered"),
        COLUMN("controller_column"),
        POWERED_COLUMN("controller_column_powered");

        @Nullable
        private final String textureName;

        CasingType(@Nullable String textureName) {
            this.textureName = textureName;
        }

        @Nullable
        private ResourceLocation sourceTexture() {
            return textureName == null ? null : controllerTexture(textureName);
        }

        private ResourceLocation coloredTexture(ControllerColor color) {
            if (textureName == null || color == ControllerColor.TRANSPARENT) {
                throw new IllegalArgumentException("This casing has no colored texture: " + this);
            }
            return controllerTexture(textureName + "_" + color.getSerializedName());
        }
    }

    public enum LightType {
        NONE(null),
        BLOCK("controller_lights"),
        COLUMN("controller_column_lights");

        @Nullable
        private final String textureName;

        LightType(@Nullable String textureName) {
            this.textureName = textureName;
        }

        @Nullable
        private ResourceLocation sourceTexture() {
            return textureName == null ? null : controllerTexture(textureName);
        }

        private ResourceLocation generatedTexture(ControllerColor color) {
            if (textureName == null) {
                throw new IllegalArgumentException("This model has no powered light texture");
            }
            return controllerTexture("generated/" + textureName + "/" + color.getSerializedName());
        }
    }

    private final ResourceLocation baseModel;
    private final CasingType casingType;
    private final LightType lightType;

    public ControllerModel(ResourceLocation baseModel, CasingType casingType, LightType lightType) {
        this.baseModel = baseModel;
        this.casingType = casingType;
        this.lightType = lightType;
    }

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return List.of(baseModel);
    }

    @Nullable
    @Override
    public BakedModel bake(ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter,
            ModelState modelTransform, ResourceLocation modelId) {
        var base = baker.bake(baseModel, modelTransform, spriteGetter);
        var sourceCasing = getSprite(spriteGetter, casingType.sourceTexture());
        var sourceLights = getSprite(spriteGetter, lightType.sourceTexture());

        var casingSprites = new EnumMap<ControllerColor, TextureAtlasSprite>(ControllerColor.class);
        if (sourceCasing != null) {
            for (var color : ControllerColor.values()) {
                if (color != ControllerColor.TRANSPARENT) {
                    casingSprites.put(color, spriteGetter.apply(material(casingType.coloredTexture(color))));
                }
            }
        }

        var poweredLightSprites = new EnumMap<ControllerColor, TextureAtlasSprite>(ControllerColor.class);

        if (sourceLights != null) {
            for (var color : ControllerColor.values()) {
                poweredLightSprites.put(
                        color,
                        spriteGetter.apply(
                                material(lightType.generatedTexture(color))));
            }
        }

        return new ControllerBakedModel(base, sourceCasing, casingSprites, sourceLights, poweredLightSprites);
    }

    @Nullable
    private static TextureAtlasSprite getSprite(Function<Material, TextureAtlasSprite> spriteGetter,
            @Nullable ResourceLocation texture) {
        return texture == null ? null : spriteGetter.apply(material(texture));
    }

    private static ResourceLocation controllerTexture(String textureName) {
        return AppEng.makeId("block/colorable_controller/" + textureName);
    }

    private static Material material(ResourceLocation texture) {
        return new Material(TextureAtlas.LOCATION_BLOCKS, texture);
    }
}
