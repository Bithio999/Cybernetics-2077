package dev.cybernetics2077.mixin.client;

import dev.cybernetics2077.client.temporal.BlockAnimations;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureAtlas.class)
public abstract class TemporalAtlasMixin {
  @Inject(method = "upload", at = @At("TAIL"))
  private void cybernetics$reload(CallbackInfo ci) {
    BlockAnimations.uploaded((TextureAtlas) (Object) this);
  }

  @Inject(method = "cycleAnimationFrames", at = @At("TAIL"))
  private void cybernetics$tick(CallbackInfo ci) {
    BlockAnimations.tick((TextureAtlas) (Object) this);
  }
}
