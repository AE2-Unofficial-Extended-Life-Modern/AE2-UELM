package appeng.client;

import java.util.HashMap;
import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.KeyMapping;

import appeng.hotkeys.HotkeyActions;

/**
 * client side component of {@link HotkeyActions}
 */
public class Hotkeys {

    private static final HashMap<String, Hotkey> HOTKEYS = new HashMap<>();
    private static final HashMap<String, ClientHotkey> CLIENT_HOTKEYS = new HashMap<>();

    private static boolean finalized;

    private static Hotkey createHotkey(String id) {
        if (finalized) {
            throw new IllegalStateException("Hotkey registration already finalized!");
        }
        return new Hotkey(id, new KeyMapping("key.ae2." + id, GLFW.GLFW_KEY_UNKNOWN, "key.ae2.category"));
    }

    private static void registerHotkey(Hotkey hotkey) {
        HOTKEYS.put(hotkey.name(), hotkey);
    }

    public static void finalizeRegistration(Consumer<KeyMapping> register) {
        for (var value : HOTKEYS.values()) {
            register.accept(value.mapping());
        }
        for (var value : CLIENT_HOTKEYS.values()) {
            register.accept(value.mapping());
        }
        finalized = true;
    }

    public static void registerHotkey(String id) {
        registerHotkey(createHotkey(id));
    }

    public static void registerClientHotkey(String id, Runnable action) {
        if (finalized) {
            throw new IllegalStateException("Hotkey registration already finalized!");
        }

        var mapping = new KeyMapping("key.ae2." + id, GLFW.GLFW_KEY_UNKNOWN, "key.ae2.category");
        CLIENT_HOTKEYS.put(id, new ClientHotkey(mapping, Objects.requireNonNull(action)));
    }

    public static void checkHotkeys() {
        HOTKEYS.forEach((name, hotkey) -> hotkey.check());
        CLIENT_HOTKEYS.forEach((name, hotkey) -> hotkey.check());
    }

    @Nullable
    public static Hotkey getHotkeyMapping(@Nullable String id) {
        return HOTKEYS.get(id);
    }

    private record ClientHotkey(KeyMapping mapping, Runnable action) {
        private void check() {
            while (mapping.consumeClick()) {
                action.run();
            }
        }
    }
}
