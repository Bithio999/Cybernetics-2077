package dev.cybernetics2077.mixin;

import dev.cybernetics2077.cyberware.AugmentRuntime;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Add concealment without applying or deleting another mod's/potion's invisibility effect. */
@Mixin(Entity.class)
public abstract class OpticalCamoMixin {
  @Inject(method = "isInvisible", at = @At("HEAD"), cancellable = true)
  private void cybernetics$camo(CallbackInfoReturnable<Boolean> ci) {
    if ((Object) this instanceof Player p && AugmentRuntime.camouflaged(p)) ci.setReturnValue(true);
  }
}
