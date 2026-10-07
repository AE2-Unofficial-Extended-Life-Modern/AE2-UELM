package appeng.api.filterterminal;

import org.jetbrains.annotations.Nullable;

/**
 * Adapts loaded hosts of one type into filter terminal targets.
 *
 * @param <T> the host type handled by this provider
 */
public interface IFilterTerminalTargetProvider<T> {

    /**
     * Returns the host type this provider is registered for.
     */
    Class<T> getTargetType();

    /**
     * Creates a target view for the host.
     *
     * @return a target, or null when this host should not be exposed
     */
    @Nullable
    IFilterTerminalTarget getTarget(T host);
}
