/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.api.stacks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Unbreakable;

import appeng.util.BootstrapMinecraft;

@BootstrapMinecraft
public class FuzzySearchTest {

    @Test
    void testOrderForDamagedItems() {
        // Diamond Sword @ 100% durability
        ItemStack undamagedSword = new ItemStack(Items.DIAMOND_SWORD);
        AEItemKey undamagedStack = AEItemKey.of(undamagedSword);

        // Unbreakable Diamond Sword @ 50% durability
        ItemStack unbreakableSword = new ItemStack(
                Items.DIAMOND_SWORD);
        unbreakableSword.setDamageValue(unbreakableSword.getMaxDamage() / 2);
        unbreakableSword.set(DataComponents.UNBREAKABLE, new Unbreakable(true));
        assertFalse(unbreakableSword.isDamageableItem());
        AEItemKey unbreakableStack = AEItemKey.of(unbreakableSword);

        // Unenchanted Diamond Sword @ 0% durability
        ItemStack damagedSword = new ItemStack(Items.DIAMOND_SWORD);
        damagedSword.setDamageValue(damagedSword.getMaxDamage());
        AEItemKey damagedStack = AEItemKey.of(damagedSword);

        // Create a list of stacks and sort by their natural order
        AEItemKey[] stacks = new AEItemKey[] {
                damagedStack, undamagedStack, unbreakableStack
        };
        Arrays.sort(stacks, FuzzySearch.COMPARATOR);
        assertThat(stacks).containsExactly(damagedStack, unbreakableStack, undamagedStack);
    }
}
