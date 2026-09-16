package appeng.crafting.pattern;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;

public record PatternTooltipComponent(IPatternDetails details, List<Component> lines) implements TooltipComponent {
    public List<AEKey> inputs() {
        return Arrays.stream(details.getInputs())
                .filter(Objects::nonNull)
                .map(iInput -> iInput.getPossibleInputs()[0].what())
                .toList();
    }

    public List<AEKey> outputs() {
        return Arrays.stream(details.getOutputs())
                .filter(Objects::nonNull)
                .map(GenericStack::what)
                .toList();
    }
}
