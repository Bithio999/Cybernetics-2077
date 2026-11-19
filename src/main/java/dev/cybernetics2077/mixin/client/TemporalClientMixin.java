package dev.cybernetics2077.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.cybernetics2077.temporal.Sandevistan;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientLevel.class)
public abstract class TemporalClientMixin {
  @WrapMethod(method = "tickNonPassenger")
  private void cybernetics$temporalTick(Entity entity, Operation<Void> original) {
    Sandevistan.tickEntity(entity, () -> original.call(entity));
  }
}
