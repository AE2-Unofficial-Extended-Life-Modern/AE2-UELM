package appeng.api.filterterminal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.ApiStatus;

/**
 * Registry of adapters that expose AE2 connected machines to the filter terminal.
 * <p>
 * Providers must be registered during addon initialization. Each exact target type can have only one provider.
 */
public final class FilterTerminalTargetRegistry {

    private static final Map<Class<?>, IFilterTerminalTargetProvider<?>> PROVIDERS = new LinkedHashMap<>();

    private static boolean frozen;

    private FilterTerminalTargetRegistry() {
    }

    /**
     * Registers a provider for an exact host type.
     *
     * @throws IllegalArgumentException if the provider reports a different type or the type already has a provider
     * @throws IllegalStateException if addon registration has already ended
     */
    public static synchronized <T> void register(Class<T> targetType,
            IFilterTerminalTargetProvider<T> provider) {
        if (frozen) {
            throw new IllegalStateException("Filter terminal target provider registration is frozen.");
        }

        Objects.requireNonNull(targetType, "targetType");
        Objects.requireNonNull(provider, "provider");

        var providerTargetType = Objects.requireNonNull(provider.getTargetType(), "provider.getTargetType()");
        if (providerTargetType != targetType) {
            throw new IllegalArgumentException("Provider target type " + providerTargetType.getName()
                    + " does not match registered target type " + targetType.getName() + ".");
        }
        if (PROVIDERS.containsKey(targetType)) {
            throw new IllegalArgumentException(
                    "A filter terminal target provider is already registered for " + targetType.getName() + ".");
        }

        PROVIDERS.put(targetType, provider);
    }

    /**
     * @return an immutable snapshot of registered providers in registration order
     */
    public static synchronized List<IFilterTerminalTargetProvider<?>> getProviders() {
        return List.copyOf(PROVIDERS.values());
    }

    /**
     * Ends addon registration. Further registration attempts will fail.
     */
    @ApiStatus.Internal
    public static synchronized void freeze() {
        frozen = true;
    }
}
