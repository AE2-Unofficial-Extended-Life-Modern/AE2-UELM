package appeng.me.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKeyType;
import appeng.util.BootstrapMinecraft;
import appeng.util.Platform;

@BootstrapMinecraft
@ExtendWith(EphemeralTestServerProvider.class)
class CompositeStorageTest {
    @Test
    void testContainsAnyFuzzyIgnoresSecondaryData(MinecraftServer server) {
        var items = new ItemStackHandler(3);
        var sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(Platform.getEnchantment(server, Enchantments.SHARPNESS), 2);
        items.setStackInSlot(1, sword);
        var storage = new CompositeStorage(Map.of(AEKeyType.items(), ExternalStorageFacade.of(items)));

        assertThat(storage.containsAnyFuzzy(Set.of(AEItemKey.of(Items.DIAMOND_SWORD)))).isTrue();
        assertThat(storage.containsAnyFuzzy(Set.of(AEItemKey.of(Items.STONE)))).isFalse();
        assertThat(storage.containsAnyFuzzy(Set.of())).isFalse();
    }

    @Test
    void testContainsAnyFuzzyChecksFluids() {
        var tank = new FluidTank(1000);
        tank.fill(new FluidStack(Fluids.WATER, 500), IFluidHandler.FluidAction.EXECUTE);
        var storage = new CompositeStorage(Map.of(
                AEKeyType.items(), ExternalStorageFacade.of(new ItemStackHandler(2)),
                AEKeyType.fluids(), ExternalStorageFacade.of(tank)));

        assertThat(storage.containsAnyFuzzy(Set.of(AEFluidKey.of(Fluids.WATER)))).isTrue();
        assertThat(storage.containsAnyFuzzy(Set.of(AEFluidKey.of(Fluids.LAVA), AEItemKey.of(Items.STONE))))
                .isFalse();
    }
}
