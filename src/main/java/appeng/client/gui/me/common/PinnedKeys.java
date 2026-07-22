package appeng.client.gui.me.common;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLPaths;

import appeng.api.stacks.AEKey;
import appeng.core.AEConfig;

@OnlyIn(Dist.CLIENT)
public final class PinnedKeys {
    // One rows worth of keys, multiplied by the max rows
    public static final int MAX_PINNED = AEConfig.instance().getMaxPinnedRows() * 9;

    // Compares by time the entry was pinned in ascending order
    private static final Comparator<Map.Entry<AEKey, PinInfo>> TIME_COMPARATOR = Comparator
            .comparing(e -> e.getValue().since);

    private static final Map<AEKey, PinInfo> pinned = new HashMap<>(MAX_PINNED);

    @Nullable
    private static Path persistenceFile;

    private PinnedKeys() {
    }

    public static boolean isEmpty() {
        return pinned.isEmpty();
    }

    public static Set<AEKey> getPinnedKeys() {
        return ImmutableSet.copyOf(pinned.keySet());
    }

    @Nullable
    public static PinInfo getPinInfo(AEKey key) {
        return pinned.get(key);
    }

    public static void clearPinnedKeys() {
        pinned.clear();
    }

    public static void loadPinnedKeys(Minecraft minecraft) {
        clearPinnedKeys();
        persistenceFile = getPersistenceFile(minecraft);
        if (persistenceFile == null) {
            return;
        }

        var newestPins = new HashMap<AEKey, PinnedKeysStorage.StoredPin>();
        for (var pin : PinnedKeysStorage.load(persistenceFile)) {
            newestPins.merge(pin.key(), pin,
                    (first, second) -> first.since().isAfter(second.since()) ? first : second);
        }

        var pins = new ArrayList<>(newestPins.values());
        pins.sort(Comparator.comparing(PinnedKeysStorage.StoredPin::since));
        var firstPin = Math.max(0, pins.size() - MAX_PINNED);
        for (var pin : pins.subList(firstPin, pins.size())) {
            pinned.put(pin.key(), new PinInfo(PinReason.MANUAL, pin.since()));
        }
    }

    public static void disconnect() {
        clearPinnedKeys();
        persistenceFile = null;
    }

    public static void pinKey(AEKey key, PinReason reason) {
        var manualPinsChanged = false;

        // Refresh timer for existing pinned keys if they're re-pinned
        var info = pinned.get(key);
        if (info != null) {
            info.since = Instant.now();
            manualPinsChanged = info.reason == PinReason.MANUAL;
            // if you manually pin a crafting key, it should become manual
            if (reason == PinReason.MANUAL) {
                info.reason = PinReason.MANUAL;
                manualPinsChanged = true;
            }
        } else {
            pinned.put(key, new PinInfo(reason));
            manualPinsChanged = reason == PinReason.MANUAL;
        }

        // Remove older keys if we exceed the max amount of pinned keys
        if (pinned.size() > MAX_PINNED) {
            var toRemove = new ArrayList<>(pinned.entrySet());
            toRemove.sort(TIME_COMPARATOR);
            for (var entry : toRemove.subList(0, toRemove.size() - MAX_PINNED)) {
                pinned.remove(entry.getKey());
                manualPinsChanged |= entry.getValue().reason == PinReason.MANUAL;
            }
        }

        if (manualPinsChanged) {
            savePinnedKeys();
        }
    }

    public static void unpin(AEKey what) {
        var removed = pinned.remove(what);
        if (removed != null && removed.reason == PinReason.MANUAL) {
            savePinnedKeys();
        }
    }

    public static boolean isPinned(AEKey what) {
        return pinned.containsKey(what);
    }

    public static void prune() {
        pinned.values().removeIf(v -> v.canPrune);
    }

    private static void savePinnedKeys() {
        if (persistenceFile == null) {
            return;
        }

        var manualPins = pinned.entrySet().stream()
                .filter(entry -> entry.getValue().reason == PinReason.MANUAL)
                .sorted(TIME_COMPARATOR)
                .map(entry -> new PinnedKeysStorage.StoredPin(entry.getKey(), entry.getValue().since))
                .toList();
        PinnedKeysStorage.save(persistenceFile, manualPins);
    }

    @Nullable
    private static Path getPersistenceFile(Minecraft minecraft) {
        String scopeType;
        String scopeIdentity;

        var integratedServer = minecraft.getSingleplayerServer();
        if (integratedServer != null) {
            scopeType = "world";
            var worldDirectory = integratedServer.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
            var savesDirectory = minecraft.gameDirectory.toPath().resolve("saves").toAbsolutePath().normalize();
            scopeIdentity = worldDirectory.startsWith(savesDirectory)
                    ? savesDirectory.relativize(worldDirectory).toString()
                    : worldDirectory.toString();
        } else {
            var server = minecraft.getCurrentServer();
            if (server == null) {
                return null;
            }
            scopeType = "server";
            scopeIdentity = server.ip.trim().toLowerCase(Locale.ROOT);
        }

        var directory = FMLPaths.CONFIGDIR.get().resolve("ae2").resolve("pinned");
        return PinnedKeysStorage.getFile(directory, scopeType, scopeIdentity);
    }

    public static class PinInfo {
        // When was it pinned?
        public Instant since;
        // Why was it pinned?
        public PinReason reason;
        // Can it be pruned the next time the UI is opened?
        public boolean canPrune;

        public PinInfo(PinReason reason) {
            this(reason, Instant.now());
        }

        private PinInfo(PinReason reason, Instant since) {
            this.reason = reason;
            this.since = since;
        }
    }

    public enum PinReason {
        CRAFTING,
        MANUAL
    }
}
