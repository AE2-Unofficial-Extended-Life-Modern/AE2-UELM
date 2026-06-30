/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.client.gui.style;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import appeng.core.AppEng;

/**
 * Manages AE2 GUI styles found in resource packs.
 */
public final class StyleManager {

    private static final Map<String, ScreenStyle> styleCache = new HashMap<>();
    private static final Map<String, List<ResourceLocation>> additionalStyles = new HashMap<>();
    public static final String PROP_INCLUDES = "includes";

    private static ResourceManager resourceManager;

    private static String getBasePath(String path) {
        int lastSep = path.lastIndexOf('/');
        if (lastSep == -1) {
            return "";
        } else {
            return path.substring(0, lastSep + 1);
        }
    }

    public static ScreenStyle loadStyleDoc(String path) {
        ScreenStyle style;

        try {
            style = loadStyleDocInternal(path);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Failed to find Screen JSON file: " + path + ": " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Failed to read Screen JSON file: " + path, e);
        }

        // We only require the final style-document to be fully valid,
        // includes are allowed to be partially valid.
        style.validate();
        return style;
    }

    public static void registerAdditionalStyle(String original, ResourceLocation additional) {
        Preconditions.checkArgument(original.startsWith("/"), "Original path needs to start with slash");

        var additionalStylePaths = additionalStyles.computeIfAbsent(original, ignored -> new ArrayList<>());
        if (!additionalStylePaths.contains(additional)) {
            additionalStylePaths.add(additional);
            styleCache.remove(original);
        }
    }

    private static JsonObject loadMergedJsonTree(String path, Set<ResourceLocation> loadedFiles,
            Set<String> resourcePacks)
            throws IOException {
        Preconditions.checkArgument(path.startsWith("/"), "Path needs to start with slash");

        if (path.contains("..")) {
            path = URI.create(path).normalize().toString();
        }

        return loadMergedJsonTree(AppEng.makeId(path.substring(1)), loadedFiles, resourcePacks);
    }

    private static JsonObject loadMergedJsonTree(ResourceLocation resourceId, Set<ResourceLocation> loadedFiles,
            Set<String> resourcePacks)
            throws IOException {
        if (!loadedFiles.add(resourceId)) {
            throw new IllegalStateException("Recursive style includes: " + loadedFiles);
        }

        if (resourceManager == null) {
            throw new IllegalStateException("ResourceManager was not set. Was initialize called?");
        }

        JsonObject document;
        var resource = resourceManager.getResource(resourceId)
                .orElseThrow(() -> new FileNotFoundException(resourceId.toString()));
        resourcePacks.add(resource.sourcePackId());
        try (var reader = resourceManager.openAsReader(resourceId)) {
            document = ScreenStyle.GSON.fromJson(reader, JsonObject.class);
        }

        // Resolve the includes present in the document
        if (document.has(PROP_INCLUDES)) {
            String[] includes = ScreenStyle.GSON.fromJson(document.get(PROP_INCLUDES), String[].class);

            List<JsonObject> layers = new ArrayList<>();
            for (String include : includes) {
                layers.add(
                        loadMergedJsonTree(resolveRelativeResource(resourceId, include), loadedFiles, resourcePacks));
            }
            layers.add(document);
            document = combineLayers(layers);
        }

        return document;

    }
    // Builds a new JSON document from layered documents

    private static JsonObject combineLayers(List<JsonObject> layers) {
        JsonObject result = new JsonObject();

        // Start by copying over all properties layer-by-layer while overwriting properties set by
        // previous layers.
        for (JsonObject layer : layers) {
            for (Map.Entry<String, JsonElement> entry : layer.entrySet()) {
                result.add(entry.getKey(), entry.getValue());
            }
        }

        // Merge the following keys by merging their properties
        mergeObjectKeys("slots", layers, result);
        mergeObjectKeys("text", layers, result);
        mergeObjectKeys("palette", layers, result);
        mergeObjectKeys("images", layers, result);
        mergeObjectKeys("terminalStyle", layers, result);
        mergeObjectKeys("widgets", layers, result);
        mergeObjectKeys("tooltips", layers, result);

        return result;
    }

    /**
     * Merges a single object property across multiple layers by merging the object keys. Higher layers win when there
     * is a conflict.
     */
    private static void mergeObjectKeys(String propertyName, List<JsonObject> layers, JsonObject target)
            throws JsonParseException {
        JsonObject mergedObject = null;
        for (JsonObject layer : layers) {
            JsonElement layerEl = layer.get(propertyName);
            if (layerEl != null) {
                if (!layerEl.isJsonObject()) {
                    throw new JsonParseException("Expected " + propertyName + " to be an object, but was: " + layerEl);
                }
                JsonObject layerObj = layerEl.getAsJsonObject();

                if (mergedObject == null) {
                    mergedObject = new JsonObject();
                }
                for (Map.Entry<String, JsonElement> entry : layerObj.entrySet()) {
                    mergedObject.add(entry.getKey(), entry.getValue());
                }
            }
        }

        if (mergedObject != null) {
            target.add(propertyName, mergedObject);
        }
    }

    private static ScreenStyle loadStyleDocInternal(String path) throws IOException {

        ScreenStyle style = styleCache.get(path);
        if (style != null) {
            return style;
        }

        Set<String> resourcePacks = new HashSet<>();
        try {
            JsonObject document = loadMergedJsonTree(path, new HashSet<>(), resourcePacks);

            var additionalStylePaths = additionalStyles.get(path);
            if (additionalStylePaths != null && !additionalStylePaths.isEmpty()) {
                List<JsonObject> layers = new ArrayList<>();
                layers.add(document);

                for (var additionalStylePath : additionalStylePaths) {
                    layers.add(loadMergedJsonTree(additionalStylePath, new HashSet<>(), resourcePacks));
                }

                document = combineLayers(layers);
            }

            style = ScreenStyle.GSON.fromJson(document, ScreenStyle.class);

            style.validate();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new JsonParseException("Failed to load style from " + path + " (packs: " + resourcePacks + ")", e);
        }

        styleCache.put(path, style);
        return style;
    }

    private static ResourceLocation resolveRelativeResource(ResourceLocation original, String relativePath) {
        var path = getBasePath(original.getPath()) + relativePath;

        if (path.contains("..")) {
            path = URI.create(path).normalize().toString();
        }

        return new ResourceLocation(original.getNamespace(), path);
    }

    public static void initialize(ResourceManager resourceManager) {
        if (resourceManager instanceof ReloadableResourceManager) {
            ((ReloadableResourceManager) resourceManager).registerReloadListener(new ReloadListener());
        }
        setResourceManager(resourceManager);
    }

    private static void setResourceManager(ResourceManager resourceManager) {
        StyleManager.resourceManager = resourceManager;
        StyleManager.styleCache.clear();
    }

    private static class ReloadListener implements ResourceManagerReloadListener {
        @Override
        public void onResourceManagerReload(ResourceManager p_10758_) {
            setResourceManager(resourceManager);
        }
    }

}
