package appeng.menu.implementations;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import appeng.api.filterterminal.IFilterTerminalTarget;
import appeng.api.filterterminal.IFilterTerminalTargetProvider;

// yummy tests
class FilterTerminalTargetDiscoveryTest {

    @Test
    void selectsMostSpecificProviderRegardlessOfRegistrationOrder() {
        var broad = provider(BroadMachine.class);
        var specialized = provider(SpecializedMachine.class);

        assertThat(FilterTerminalTargetDiscovery.selectProvider(SpecializedMachine.class,
                List.of(broad, specialized))).isSameAs(specialized);
        assertThat(FilterTerminalTargetDiscovery.selectProvider(SpecializedMachine.class,
                List.of(specialized, broad))).isSameAs(specialized);
    }

    @Test
    void skipsUnrelatedMatchingProviders() {
        var first = provider(FirstMachineType.class);
        var second = provider(SecondMachineType.class);

        assertThat(FilterTerminalTargetDiscovery.selectProvider(
                AmbiguousMachine.class,
                List.of(first, second)))
                .isNull();
    }

    @Test
    void commonSpecializationResolvesOtherwiseUnrelatedProviders() {
        var first = provider(FirstMachineType.class);
        var second = provider(SecondMachineType.class);
        var specialized = provider(AmbiguousMachine.class);

        assertThat(FilterTerminalTargetDiscovery.selectProvider(AmbiguousMachine.class,
                List.of(first, second, specialized))).isSameAs(specialized);
    }

    private static <T> IFilterTerminalTargetProvider<T> provider(Class<T> targetType) {
        return new IFilterTerminalTargetProvider<>() {
            @Override
            public Class<T> getTargetType() {
                return targetType;
            }

            @Override
            public IFilterTerminalTarget getTarget(T host) {
                return null;
            }
        };
    }

    private interface BroadMachine {
    }

    private static final class SpecializedMachine implements BroadMachine {
    }

    private interface FirstMachineType {
    }

    private interface SecondMachineType {
    }

    private static final class AmbiguousMachine implements FirstMachineType, SecondMachineType {
    }
}
