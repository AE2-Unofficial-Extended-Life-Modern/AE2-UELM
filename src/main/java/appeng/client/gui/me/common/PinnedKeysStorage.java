package appeng.client.gui.me.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

import appeng.api.stacks.AEKey;
import appeng.core.AELog;

final class PinnedKeysStorage {
    private static final int CURRENT_VERSION = 1;
    private static final String TAG_VERSION = "version";
    private static final String TAG_PINS = "pins";
    private static final String TAG_KEY = "key";
    private static final String TAG_SINCE = "since";

    private PinnedKeysStorage() {
    }

    static Path getFile(Path directory, String scopeType, String scopeIdentity) {
        return directory.resolve(scopeType + "-" + hash(scopeType + ":" + scopeIdentity) + ".dat");
    }

    static List<StoredPin> load(Path file) {
        if (!Files.isRegularFile(file)) {
            return List.of();
        }

        try {
            var root = NbtIo.readCompressed(file.toFile());
            if (root.getInt(TAG_VERSION) != CURRENT_VERSION) {
                AELog.warn("Cannot load pinned keys from %s because its version is unsupported.", file);
                return List.of();
            }

            var result = new ArrayList<StoredPin>();
            var pins = root.getList(TAG_PINS, Tag.TAG_COMPOUND);
            for (var pinTag : pins) {
                var pin = (CompoundTag) pinTag;
                if (!pin.contains(TAG_KEY, Tag.TAG_COMPOUND) || !pin.contains(TAG_SINCE, Tag.TAG_LONG)) {
                    continue;
                }

                var key = AEKey.fromTagGeneric(pin.getCompound(TAG_KEY));
                if (key != null) {
                    result.add(new StoredPin(key, Instant.ofEpochMilli(pin.getLong(TAG_SINCE))));
                }
            }
            return result;
        } catch (Exception e) {
            AELog.warn(e, "Cannot load pinned keys from " + file);
            return List.of();
        }
    }

    static void save(Path file, Collection<StoredPin> pins) {
        try {
            if (pins.isEmpty()) {
                Files.deleteIfExists(file);
                return;
            }

            Files.createDirectories(file.getParent());

            var root = new CompoundTag();
            root.putInt(TAG_VERSION, CURRENT_VERSION);

            var pinTags = new ListTag();
            for (var pin : pins) {
                var pinTag = new CompoundTag();
                pinTag.put(TAG_KEY, pin.key().toTagGeneric());
                pinTag.putLong(TAG_SINCE, pin.since().toEpochMilli());
                pinTags.add(pinTag);
            }
            root.put(TAG_PINS, pinTags);

            var temporaryFile = file.resolveSibling(file.getFileName() + ".tmp");
            try {
                NbtIo.writeCompressed(root, temporaryFile.toFile());
                replaceFile(temporaryFile, file);
            } finally {
                Files.deleteIfExists(temporaryFile);
            }
        } catch (IOException e) {
            AELog.warn(e, "Cannot save pinned keys to " + file);
        }
    }

    private static void replaceFile(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String hash(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    record StoredPin(AEKey key, Instant since) {
    }
}
