package dev.cybernetics2077.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.settings.*;

/** New profiles avoid TaCZ's inspect, fire selector, zoom, crawl and interaction bindings. */
public final class CyberKeys {
  public static KeyMapping create(String id, int key) {
    return new KeyMapping(
        "key.cybernetics2077." + id,
        KeyConflictContext.IN_GAME,
        ModList.get().isLoaded("tacz") ? KeyModifier.ALT : KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        key,
        "key.categories.cybernetics2077");
  }

  private CyberKeys() {}
}
