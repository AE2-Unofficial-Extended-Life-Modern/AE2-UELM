package appeng.menu.implementations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import appeng.api.filterterminal.FilterTerminalTargetMetadata;
import appeng.api.filterterminal.FilterTerminalTargetRegistry;
import appeng.api.filterterminal.IFilterTerminalTarget;
import appeng.api.filterterminal.IFilterTerminalTargetProvider;
import appeng.api.networking.IGrid;
import appeng.core.AELog;

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

    private static final Set<Class<?>> REPORTED_AMBIGUOUS_PROVIDER_TYPES = ConcurrentHashMap.newKeySet();

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

        var providerList = new ArrayList<IFilterTerminalTargetProvider<?>>();
        providers.forEach(providerList::add);
        Set<Object> identities = Collections.newSetFromMap(new IdentityHashMap<>());
        var result = new ArrayList<IFilterTerminalTarget>();
        for (var machineClass : grid.getMachineClasses()) {
            var provider = selectProvider(machineClass, providerList);
            if (provider != null) {
                addProviderTargets(grid, machineClass, provider, identities, result);
            }
        }

        result.sort(Comparator.comparing(IFilterTerminalTarget::getMetadata, METADATA_COMPARATOR));
        return result;
    }

    @Nullable
    static IFilterTerminalTargetProvider<?> selectProvider(Class<?> machineClass,
            Iterable<IFilterTerminalTargetProvider<?>> providers) {
        var matching = new ArrayList<IFilterTerminalTargetProvider<?>>();
        for (var provider : providers) {
            if (provider.getTargetType().isAssignableFrom(machineClass)) {
                matching.add(provider);
            }
        }

        IFilterTerminalTargetProvider<?> selected = null;
        for (var candidate : matching) {
            if (isLessSpecific(candidate, matching)) {
                continue;
            }
            if (selected != null) {
                if (REPORTED_AMBIGUOUS_PROVIDER_TYPES.add(machineClass)) {
                    AELog.error(
                            "Ambiguous filter terminal target providers for {}: {} and {}",
                            machineClass.getName(),
                            selected.getTargetType().getName(),
                            candidate.getTargetType().getName());
                }

                return null;
            }
            selected = candidate;
        }
        return selected;
    }

    private static boolean isLessSpecific(IFilterTerminalTargetProvider<?> candidate,
            List<IFilterTerminalTargetProvider<?>> matching) {
        var candidateType = candidate.getTargetType();
        for (var other : matching) {
            var otherType = other.getTargetType();
            if (candidateType != otherType && candidateType.isAssignableFrom(otherType)) {
                return true;
            }
        }
        return false;
    }

    private static <T> void addProviderTargets(IGrid grid, Class<?> machineClass,
            IFilterTerminalTargetProvider<T> provider,
            Set<Object> identities, List<IFilterTerminalTarget> result) {
        var targetType = provider.getTargetType();
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
