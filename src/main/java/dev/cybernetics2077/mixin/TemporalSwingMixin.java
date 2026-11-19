package dev.cybernetics2077.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.cybernetics2077.temporal.Sandevistan;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class TemporalSwingMixin {
  @ModifyReturnValue(method = "getCurrentSwingDuration", at = @At("RETURN"))
  private int cybernetics$swingClock(int duration) {
    return (Object) this instanceof Player p
        ? Math.max(1, (int) Math.ceil(duration / Sandevistan.playerPace(p)))
        : duration;
  }
}
