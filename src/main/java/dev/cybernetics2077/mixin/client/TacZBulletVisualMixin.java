package dev.cybernetics2077.mixin.client;

import dev.cybernetics2077.client.GuidedBulletClient;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Server pose samples replace independent straight-line prediction for managed rounds only. */
@Pseudo
@Mixin(targets = "com.tacz.guns.entity.EntityKineticBullet", remap = false)
public abstract class TacZBulletVisualMixin {
  @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
  private void cybernetics$authoritative(CallbackInfo ci) {
    if (GuidedBulletClient.managed((Entity) (Object) this)) ci.cancel();
  }
}
