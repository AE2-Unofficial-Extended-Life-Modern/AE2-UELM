package appeng.client.render.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.data.ModelData;

import appeng.block.networking.ColorableControllerBlock;
import appeng.block.networking.ControllerColor;
import appeng.client.render.DelegateBakedModel;

final class ControllerBakedModel extends DelegateBakedModel {

    @Nullable
    private final TextureAtlasSprite sourceCasing;
    private final Map<ControllerColor, TextureAtlasSprite> casingSprites;
    @Nullable
    private final TextureAtlasSprite sourceLights;
    private final Map<ControllerColor, TextureAtlasSprite> poweredLightSprites;

    ControllerBakedModel(BakedModel baseModel, @Nullable TextureAtlasSprite sourceCasing,
            Map<ControllerColor, TextureAtlasSprite> casingSprites, @Nullable TextureAtlasSprite sourceLights,
            Map<ControllerColor, TextureAtlasSprite> poweredLightSprites) {
        super(baseModel);
        this.sourceCasing = sourceCasing;
        this.casingSprites = new EnumMap<>(ControllerColor.class);
        this.casingSprites.putAll(casingSprites);
        this.sourceLights = sourceLights;
        this.poweredLightSprites = new EnumMap<>(ControllerColor.class);
        this.poweredLightSprites.putAll(poweredLightSprites);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return transformQuads(state, super.getQuads(state, side, rand));
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
            ModelData modelData, @Nullable RenderType renderType) {
        return transformQuads(state, super.getQuads(state, side, rand, modelData, renderType));
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return getBaseModel().getRenderTypes(state, rand, data);
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack itemStack, boolean fabulous) {
        return getBaseModel().getRenderTypes(itemStack, fabulous);
    }

    private List<BakedQuad> transformQuads(@Nullable BlockState state, List<BakedQuad> quads) {
        var color = getColor(state);
        if (color == null || quads.isEmpty()) {
            return quads;
        }

        var casingSprite = casingSprites.get(color);
        var poweredLightSprite = poweredLightSprites.get(color);
        var result = new ArrayList<BakedQuad>(quads.size());
        for (var quad : quads) {
            result.add(transformQuad(quad, casingSprite, poweredLightSprite));
        }
        return result;
    }

    private BakedQuad transformQuad(BakedQuad quad, @Nullable TextureAtlasSprite casingSprite,
            @Nullable TextureAtlasSprite poweredLightSprite) {
        if (sourceCasing != null && quad.getSprite() == sourceCasing && casingSprite != null) {
            return replaceSprite(quad, sourceCasing, casingSprite);
        }
        if (sourceLights != null && quad.getSprite() == sourceLights && poweredLightSprite != null) {
            return replaceSprite(quad, sourceLights, poweredLightSprite);
        }
        return quad;
    }

    private static BakedQuad replaceSprite(BakedQuad quad, TextureAtlasSprite source,
            TextureAtlasSprite destination) {
        return new BakedQuad(
                remapUv(quad.getVertices(), source, destination),
                quad.getTintIndex(),
                quad.getDirection(),
                destination,
                quad.isShade(),
                quad.hasAmbientOcclusion());
    }

    @Nullable
    private static ControllerColor getColor(@Nullable BlockState state) {
        return state != null && state.hasProperty(ColorableControllerBlock.COLOR)
                ? state.getValue(ColorableControllerBlock.COLOR)
                : null;
    }

    private static int[] remapUv(int[] original, TextureAtlasSprite source, TextureAtlasSprite destination) {
        var result = original.clone();
        for (var vertex = 0; vertex < 4; vertex++) {
            var offset = vertex * IQuadTransformer.STRIDE + IQuadTransformer.UV0;
            var sourceU = Float.intBitsToFloat(original[offset]);
            var sourceV = Float.intBitsToFloat(original[offset + 1]);
            var u = source.getUOffset(sourceU);
            var v = source.getVOffset(sourceV);
            result[offset] = Float.floatToRawIntBits(destination.getU(u));
            result[offset + 1] = Float.floatToRawIntBits(destination.getV(v));
        }
        return result;
    }
}
