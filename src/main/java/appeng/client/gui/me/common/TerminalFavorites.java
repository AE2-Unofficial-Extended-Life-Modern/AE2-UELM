package appeng.client.gui.me.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import appeng.core.AELog;

@OnlyIn(Dist.CLIENT)
public final class TerminalFavorites {
    private static final String TAG_FAVORITES = "favorites";

    private static final Set<String> favorites = new HashSet<>();

    @Nullable
    private static Path persistenceFile;

    private TerminalFavorites() {
    }

    public static void load(Minecraft minecraft) {
        favorites.clear();
        persistenceFile = PinnedKeys.getPersistenceFile(minecraft, "favorites");
        if (persistenceFile == null || !Files.isRegularFile(persistenceFile)) {
            return;
        }

        try {
            var root = NbtIo.readCompressed(persistenceFile.toFile());
            for (var favorite : root.getList(TAG_FAVORITES, Tag.TAG_STRING)) {
                favorites.add(favorite.getAsString());
            }
        } catch (Exception e) {
            AELog.warn(e, "Cannot load terminal favorites from " + persistenceFile);
        }
    }

    public static void disconnect() {
        favorites.clear();
        persistenceFile = null;
    }

    public static boolean isFavorite(String key) {
        return favorites.contains(key);
    }

    public static void toggle(String key) {
        if (!favorites.remove(key)) {
            favorites.add(key);
        }
        save();
    }

    private static void save() {
        if (persistenceFile == null) {
            return;
        }

        try {
            if (favorites.isEmpty()) {
                Files.deleteIfExists(persistenceFile);
                return;
            }

            Files.createDirectories(persistenceFile.getParent());

            var favoriteTags = new ListTag();
            for (var favorite : favorites) {
                favoriteTags.add(StringTag.valueOf(favorite));
            }
            var root = new CompoundTag();
            root.put(TAG_FAVORITES, favoriteTags);
            NbtIo.writeCompressed(root, persistenceFile.toFile());
        } catch (IOException e) {
            AELog.warn(e, "Cannot save terminal favorites to " + persistenceFile);
        }
    }
}
