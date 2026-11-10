package appeng.me.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;

public class InventoryGenerator {

    private final Random random;
    private final List<Item> items;
    private final List<Holder<Enchantment>> enchantments;

    public InventoryGenerator(Random random, HolderLookup.Provider registries) {
        this.random = random;
        // Sort by ID, since the registry's entry set is ordered by identity hash codes, which differ between JVMs
        items = BuiltInRegistries.ITEM.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().location()))
                .map(Map.Entry::getValue)
                .toList();
        // Enchantments are data-driven and not part of the built-in registries
        enchantments = registries.lookupOrThrow(Registries.ENCHANTMENT).listElements()
                .sorted(Comparator.comparing(holder -> holder.key().location()))
                .<Holder<Enchantment>>map(holder -> holder)
                .toList();
    }

    private Item randomItem() {
        return items.get(random.nextInt(items.size()));
    }

    private Holder<Enchantment> randomEnchantment() {
        return enchantments.get(random.nextInt(enchantments.size()));
    }

    public void fillInventory(MEStorage storage, long targetUniques) {
        long total = 0, scale = 1;
        int fails = 0;
        do {
            long inserted = attemptFill(storage, scale);
            if (inserted <= 0) {
                fails++;
            } else {
                fails = 0;
                total += inserted;
                if (total >= targetUniques) {
                    scale++;
                }
            }
            if (fails > 100) {
                break;
            }
        } while (scale > 0);
        var finalContents = storage.getAvailableStacks();
        System.out.println("Filled storage with " + total + " items in " + finalContents.size() + " unique stacks");
    }

    public long attemptFill(MEStorage storage, long scale) {
        long inserted = 0;
        var item = randomItem();
        var stack = new ItemStack(item);
        if (item.isDamageable(stack)) {
            inserted += fillDamaged(storage, scale, item);
        }
        if (item.isEnchantable(stack)) {
            inserted += fillEnchantable(storage, scale, item);
        }
        inserted += fillRegular(storage, scale, new ItemStack(item));
        return inserted;
    }

    public long fillDamaged(MEStorage storage, long scale, Item item) {
        var stack = new ItemStack(item);
        stack.setDamageValue(random.nextInt(stack.getMaxDamage()));
        return fillRegular(storage, scale, stack);
    }

    public long fillEnchantable(MEStorage storage, long scale, Item item) {
        var stack = new ItemStack(item);
        var enchantment = randomEnchantment();
        stack.enchant(enchantment, 1 + random.nextInt(enchantment.value().getMaxLevel()));
        return fillRegular(storage, scale, stack);
    }

    public long fillRegular(MEStorage storage, long scale, ItemStack stack) {
        var item = GenericStack.fromItemStack(stack);
        if (item == null) {
            return 1;
        }
        return storage.insert(item.what(), 1L + random.nextLong(scale), Actionable.MODULATE, IActionSource.empty());
    }
}
