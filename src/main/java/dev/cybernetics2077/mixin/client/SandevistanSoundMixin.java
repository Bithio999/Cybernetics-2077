package dev.cybernetics2077.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.cybernetics2077.client.temporal.SandevistanClient;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SoundEngine.class)
public abstract class SandevistanSoundMixin {
  @ModifyReturnValue(method = "calculatePitch", at = @At("RETURN"))
  private float cybernetics$temporalPitch(float original, SoundInstance sound) {
    return original * SandevistanClient.soundFactor(sound, true);
  }

  @ModifyReturnValue(
      method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F",
      at = @At("RETURN"))
  private float cybernetics$temporalVolume(float original, SoundInstance sound) {
    return original * SandevistanClient.soundFactor(sound, false);
  }
}
