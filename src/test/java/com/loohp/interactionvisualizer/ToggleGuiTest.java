package com.loohp.interactionvisualizer;

import com.loohp.interactionvisualizer.objectholders.EntryKey;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ToggleGuiTest {
    @Test void everyKnownEntryHasARealItemIcon() {
        for (KoreanEntry entry : KoreanEntry.values()) {
            Material icon = ToggleGui.icon(new EntryKey(entry.key()));
            assertNotEquals(Material.AIR, icon, entry.key());
            assertNotEquals(Material.PAPER, icon, entry.key());
        }
    }

    @Test void unknownAddonEntriesHaveASafeFallback() {
        assertEquals(Material.PAPER, ToggleGui.icon(new EntryKey("addon:unknown_entry")));
    }

    @Test void pageBoundariesReserveTheBottomRowForControls() {
        assertEquals(1, ToggleGui.pageCount(0));
        assertEquals(1, ToggleGui.pageCount(36));
        assertEquals(2, ToggleGui.pageCount(37));
        assertEquals(2, ToggleGui.pageCount(72));
        assertEquals(3, ToggleGui.pageCount(73));
    }
}
