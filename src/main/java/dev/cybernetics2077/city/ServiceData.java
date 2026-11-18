package dev.cybernetics2077.city;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;

/** Wallet values use 16-bit lanes because vanilla container data packets carry signed shorts. */
final class ServiceData implements ContainerData {
  private final Player owner;
  private final int[] values = new int[7];

  ServiceData(Player p) {
    owner = p;
  }

  public int get(int i) {
    if (owner.level().isClientSide) return values[i];
    var account = owner.getData(CityContent.ACCOUNT);
    if (i < 4) return (int) (account.eddies() >>> (i * 16)) & 65535;
    if (i == 4) return account.streetCred();
    return values[i];
  }

  public void set(int i, int v) {
    if (i >= 0 && i < values.length) values[i] = v;
  }

  public int getCount() {
    return values.length;
  }

  long balance() {
    long n = 0;
    for (int i = 0; i < 4; i++) n |= (long) (get(i) & 65535) << (i * 16);
    return n;
  }
}
