package appeng.server.services.compass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraftforge.event.level.LevelEvent;

import appeng.util.BootstrapMinecraft;

@BootstrapMinecraft
@MockitoSettings
class CompassServiceTest {
    private static final int MAX_RANGE = 174;

    @Mock
    ServerLevel level;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        var dataStorage = new DimensionDataStorage(tempDir.toFile(), DataFixers.getDataFixer());
        when(level.getDataStorage()).thenReturn(dataStorage);
    }

    @Test
    void testDirectionQueryIsCached() {
        var origin = new ChunkPos(0, 0);
        assertThat(CompassService.getDirection(level, origin, MAX_RANGE))
                .isEqualTo(CompassService.Result.INDETERMINATE);
        verify(level, atLeastOnce()).getDataStorage();

        clearInvocations(level);
        assertThat(CompassService.getDirection(level, new ChunkPos(origin.x, origin.z), MAX_RANGE))
                .isEqualTo(CompassService.Result.INDETERMINATE);
        verify(level, never()).getDataStorage();
    }

    @Test
    void testCacheInvalidatedOnAnyChunkUpdate() {
        var origin = new ChunkPos(0, 0);
        var changedChunkPos = new ChunkPos(1, 0);
        var compassRegion = CompassRegion.get(level, origin);
        compassRegion.setHasSkyStone(origin.x, origin.z, 0, true);
        assertThat(CompassService.getDirection(level, origin, MAX_RANGE)).isEqualTo(CompassService.Result.ON_THE_SPOT);

        compassRegion.setHasSkyStone(origin.x, origin.z, 0, false);
        compassRegion.setHasSkyStone(changedChunkPos.x, changedChunkPos.z, 0, true);
        var changedChunk = mock(ChunkAccess.class);
        when(changedChunk.getPos()).thenReturn(changedChunkPos);
        CompassService.updateArea(level, changedChunk);

        var result = CompassService.getDirection(level, origin, MAX_RANGE);
        assertThat(result.hasResult()).isTrue();
        assertThat(result.spin()).isFalse();
        assertThat(result.distance()).isEqualTo(16 * 16);
    }

    @Test
    void testCacheRemovedOnLevelUnload() {
        var origin = new ChunkPos(0, 0);
        assertThat(CompassService.getDirection(level, origin, MAX_RANGE))
                .isEqualTo(CompassService.Result.INDETERMINATE);

        CompassService.onLevelUnload(new LevelEvent.Unload(level));

        clearInvocations(level);
        assertThat(CompassService.getDirection(level, origin, MAX_RANGE))
                .isEqualTo(CompassService.Result.INDETERMINATE);
        verify(level, atLeastOnce()).getDataStorage();
    }
}
