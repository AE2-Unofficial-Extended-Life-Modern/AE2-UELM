package appeng.crafting.pattern;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import org.junit.jupiter.api.Test;

import appeng.util.BootstrapMinecraft;

@BootstrapMinecraft
class EncodedPatternAuthorTest {
    @Test
    void dataWithoutAuthorDecodesToEmptyAuthor() {
        var json = JsonParser.parseString("{\"sparseInputs\":[],\"sparseOutputs\":[]}");
        var decoded = EncodedProcessingPattern.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertThat(decoded.author()).isEmpty();
    }
}
