package appeng.menu.implementations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import appeng.api.filterterminal.FilterTerminalTargetMetadata;
import appeng.api.filterterminal.FilterTerminalTargetRegistry;
import appeng.api.filterterminal.IFilterTerminalTarget;
import appeng.api.filterterminal.IFilterTerminalTargetProvider;
import appeng.api.networking.IGrid;

/**
 * Discovers active grid machines through the host types declared by registered providers.
 */
final class FilterTerminalTargetDiscovery {

    private static final Comparator<FilterTerminalTargetMetadata> METADATA_COMPARATOR = Comparator
            .comparing((FilterTerminalTargetMetadata metadata) -> metadata.group().name().getString(),
                    String.CASE_INSENSITIVE_ORDER)
            .thenComparing(metadata -> metadata.dimension().location().toString())
            .thenComparingLong(metadata -> metadata.pos().asLong())
            .thenComparingInt(metadata -> metadata.side() == null ? -1 : metadata.side().ordinal());

    private FilterTerminalTargetDiscovery() {
    }

    static List<IFilterTerminalTarget> findTargets(@Nullable IGrid grid) {
        return findTargets(grid, FilterTerminalTargetRegistry.getProviders());
    }

    static List<IFilterTerminalTarget> findTargets(@Nullable IGrid grid,
            Iterable<IFilterTerminalTargetProvider<?>> providers) {
        if (grid == null) {
            return List.of();
        }

        Set<Object> identities = Collections.newSetFromMap(new IdentityHashMap<>());
        var result = new ArrayList<IFilterTerminalTarget>();
        for (var provider : providers) {
            addProviderTargets(grid, provider, identities, result);
        }

        result.sort(Comparator.comparing(IFilterTerminalTarget::getMetadata, METADATA_COMPARATOR));
        return result;
    }

    private static <T> void addProviderTargets(IGrid grid, IFilterTerminalTargetProvider<T> provider,
            Set<Object> identities, List<IFilterTerminalTarget> result) {
        var targetType = provider.getTargetType();
        for (var machineClass : grid.getMachineClasses()) {
            if (!targetType.isAssignableFrom(machineClass)) {
                continue;
            }

            var compatibleClass = machineClass.asSubclass(targetType);
            for (var host : grid.getActiveMachines(compatibleClass)) {
                var target = provider.getTarget(host);
                if (target != null
                        && FilterTerminalEditValidation.isValidTarget(grid, target)
                        && identities.add(target.getIdentity())) {
                    result.add(target);
                }
            }
        }
    }
}
