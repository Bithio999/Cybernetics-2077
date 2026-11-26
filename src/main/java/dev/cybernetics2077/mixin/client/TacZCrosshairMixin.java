package dev.cybernetics2077.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.cybernetics2077.client.ui.NativeHudTransition;
import dev.cybernetics2077.compat.TacZCompat;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** TaCZ replaces the crosshair in the Pre event, before the regular layer wrapper runs. */
@Pseudo
@Mixin(targets = "com.tacz.guns.client.event.RenderCrosshairEvent", remap = false)
public abstract class TacZCrosshairMixin {
  @WrapMethod(method = "onRenderCrosshair")
  private static void cybernetics$crosshair(
      RenderGuiLayerEvent.Pre event, Operation<Void> original) {
    var player = Minecraft.getInstance().player;
    if (!event.getName().equals(VanillaGuiLayers.CROSSHAIR)
        || player == null
        || player.isSpectator()
        || !TacZCompat.gun(player.getMainHandItem())) {
      original.call(event);
      return;
    }
    if (NativeHudTransition.visible(NativeHudTransition.Group.CROSSHAIR) <= .00001) {
      // TaCZ normally cancels the vanilla crosshair when holding a gun; preserve that contract.
      event.setCanceled(true);
      return;
    }
    NativeHudTransition.vanilla(
        event.getGuiGraphics(), NativeHudTransition.Group.CROSSHAIR, () -> original.call(event));
  }
}
