package dev.cybernetics2077.mixin.client;

import dev.cybernetics2077.client.mantis.CyberwareClient;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class MantisPlayerMixin {
  @Inject(method = "setModelProperties", at = @At("TAIL"))
  private void cybernetics$splitArms(AbstractClientPlayer player, CallbackInfo ci) {
    if (CyberwareClient.visible(player)) {
      var model = ((PlayerRenderer) (Object) this).getModel();
      model.rightArm.visible = false;
      model.leftArm.visible = false;
      model.rightSleeve.visible = false;
      model.leftSleeve.visible = false;
    }
  }
}
