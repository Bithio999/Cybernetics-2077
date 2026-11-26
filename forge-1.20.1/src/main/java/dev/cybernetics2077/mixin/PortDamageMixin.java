package dev.cybernetics2077.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.cybernetics2077.port.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public abstract class PortDamageMixin {
  @WrapMethod(method = "hurt")
  private boolean cybernetics$damage(
      DamageSource source, float amount, Operation<Boolean> original) {
    var entity = (LivingEntity) (Object) this;
    if (entity.level().isClientSide) return original.call(source, amount);
    var incoming = new PortIncomingDamage(entity, source, amount);
    if (MinecraftForge.EVENT_BUS.post(incoming)) return false;
    float before = entity.getHealth() + entity.getAbsorptionAmount();
    boolean accepted = original.call(source, incoming.getAmount());
    float lost = before - entity.getHealth() - entity.getAbsorptionAmount();
    if (accepted && lost > 0)
      MinecraftForge.EVENT_BUS.post(new PortDamagePost(entity, source, lost));
    return accepted;
  }
}
