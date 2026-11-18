package dev.cybernetics2077.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.cybernetics2077.cyberware.AugmentExpansion;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Quiet only normal footsteps, preserving combat audio and native game-event vibrations. */
@Mixin(Entity.class)
public abstract class LynxStepsMixin {
  @WrapOperation(
      method = "playStepSound",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/entity/Entity;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"))
  private void cybernetics$quietStep(
      Entity entity, SoundEvent sound, float volume, float pitch, Operation<Void> original) {
    original.call(entity, sound, AugmentExpansion.stepVolume(entity, volume), pitch);
  }
}
