package dev.cybernetics2077.mixin.client;

import dev.cybernetics2077.client.mantis.CyberwareClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Local movement remains normal Minecraft collision movement; server chooses the leap trajectory.
 */
@Mixin(Player.class)
public abstract class MantisTravelMixin {
  @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
  private void cybernetics$leap(Vec3 input, CallbackInfo ci) {
    if ((Object) this instanceof LocalPlayer p && CyberwareClient.moveLunge(p)) ci.cancel();
  }
}
