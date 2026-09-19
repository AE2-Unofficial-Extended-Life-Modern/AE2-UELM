package appeng.init.internal;

import appeng.api.filterterminal.FilterTerminalTargetRegistry;
import appeng.helpers.IConfigInvHost;
import appeng.helpers.filterterminal.AEFilterTerminalTargetProvider;

public final class InitFilterTerminalTargets {

    private InitFilterTerminalTargets() {
    }

    public static void init() {
        FilterTerminalTargetRegistry.register(IConfigInvHost.class, AEFilterTerminalTargetProvider.INSTANCE);
    }
}
