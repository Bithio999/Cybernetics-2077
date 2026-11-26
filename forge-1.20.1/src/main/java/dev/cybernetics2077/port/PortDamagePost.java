package dev.cybernetics2077.port;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;

public final class PortDamagePost extends LivingEvent {
  private final DamageSource source;
  private final float damage;

  public PortDamagePost(LivingEntity e, DamageSource source, float damage) {
    super(e);
    this.source = source;
    this.damage = damage;
  }

  public DamageSource getSource() {
    return source;
  }

  public float getNewDamage() {
    return damage;
  }

  public float getAmount() {
    return damage;
  }
}
