package dev.cybernetics2077.cyberware;

import dev.cybernetics2077.Cybernetics2077;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.*;

/** Existing physical IDs remain unchanged; elemental arms are separate installable families. */
public enum MantisVariant {
  PHYSICAL("physical", "Physical", 0xc0d6d9),
  THERMAL("thermal", "Thermal", 0xff8d38),
  ELECTRICAL("electrical", "Electrifying", 0x66ddff),
  TOXIC("toxic", "Toxic", 0xa4e451);
  public final String id, title;
  public final int color;

  MantisVariant(String id, String title, int color) {
    this.id = id;
    this.title = title;
    this.color = color;
  }

  public static MantisVariant of(int ordinal) {
    return values()[Math.clamp(ordinal, 0, values().length - 1)];
  }

  public DamageSource source(ServerPlayer p) {
    return this == PHYSICAL
        ? p.damageSources().playerAttack(p)
        : new DamageSource(
            p.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(
                    ResourceKey.create(Registries.DAMAGE_TYPE, Cybernetics2077.id("mantis_" + id))),
            p,
            p);
  }

  public static boolean elemental(DamageSource source) {
    for (var v : values())
      if (v != PHYSICAL
          && source.is(
              ResourceKey.create(Registries.DAMAGE_TYPE, Cybernetics2077.id("mantis_" + v.id))))
        return true;
    return false;
  }

  public String effect() {
    return switch (this) {
      case PHYSICAL -> "20% bleed chance: four physical pulses over 4 seconds.";
      case THERMAL -> "Thermal damage; 20% chance to ignite for 4 seconds.";
      case ELECTRICAL ->
          "Electrical damage; 20% chance for Slowness II / Weakness I for 2 seconds.";
      case TOXIC -> "Chemical damage; 20% chance for Poison I for 5 seconds.";
    };
  }
}
