package dev.cybernetics2077.port;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.Cancelable;

@Cancelable
public final class PortIncomingDamage extends LivingEvent {
  private final DamageSource source;
  private float amount;

  public PortIncomingDamage(LivingEntity e, DamageSource source, float amount) {
    super(e);
    this.source = source;
    this.amount = amount;
  }

  public DamageSource getSource() {
    return source;
  }

  public float getAmount() {
    return amount;
  }

  public void setAmount(float amount) {
    this.amount = amount;
  }

  public void setInvulnerabilityTicks(int ticks) {
    getEntity().invulnerableTime = ticks;
  }
}
