package appeng.init.internal;

import static appeng.api.filterterminal.FilterTerminalTargetRegistry.register;

import appeng.blockentity.misc.SuperMEReplenisherBlockEntity;
import appeng.helpers.IConfigInvHost;
import appeng.helpers.filterterminal.AEFilterTerminalTargetProvider;
import appeng.helpers.filterterminal.SuperMEReplenisherFilterTerminalTargetProvider;

public final class InitFilterTerminalTargets {

    private InitFilterTerminalTargets() {
    }

    public static void init() {
        register(IConfigInvHost.class, AEFilterTerminalTargetProvider.INSTANCE);
        register(SuperMEReplenisherBlockEntity.class, SuperMEReplenisherFilterTerminalTargetProvider.INSTANCE);
    }
}
