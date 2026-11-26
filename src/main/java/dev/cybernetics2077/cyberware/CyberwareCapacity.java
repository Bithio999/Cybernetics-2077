package dev.cybernetics2077.cyberware;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.city.CityContent;
import net.minecraft.world.entity.player.Player;

/** The configured base remains intact; each full ten Street Cred adds ten capacity. */
public final class CyberwareCapacity {
  public static int limit(int base, int streetCred) {
    return Math.clamp(
        (long) Math.max(1, base) + Math.clamp(streetCred, 0, 50) / 10 * 10, 1, Integer.MAX_VALUE);
  }

  public static int limit(Player player) {
    return limit(Cybernetics2077.CAPACITY.get(), player.getData(CityContent.ACCOUNT).streetCred());
  }

  private CyberwareCapacity() {}
}
