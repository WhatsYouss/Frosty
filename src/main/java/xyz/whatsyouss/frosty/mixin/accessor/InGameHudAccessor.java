package xyz.whatsyouss.frosty.mixin.accessor;

import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(InGameHud.class)
public interface InGameHudAccessor {
    @Accessor("SCOREBOARD_ENTRY_COMPARATOR")
    static java.util.Comparator<net.minecraft.scoreboard.ScoreboardEntry> getScoreboardEntryComparator() {
        throw new AssertionError();
    }
}
