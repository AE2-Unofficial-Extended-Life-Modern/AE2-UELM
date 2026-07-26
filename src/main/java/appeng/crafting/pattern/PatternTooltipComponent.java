package appeng.crafting.pattern;

import java.util.List;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

import appeng.api.stacks.AEKey;

public record PatternTooltipComponent(
        List<AEKey> inputs,
        List<AEKey> outputs) implements TooltipComponent {
}
