package dev.cybernetics2077.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.cybernetics2077.client.ui.NativeHudTransition;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.*;
import net.neoforged.neoforge.client.gui.GuiLayerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Wrap the draw after cancellable Pre events, so a cancelled layer cannot leak fade state. */
@Mixin(value = GuiLayerManager.class, remap = false)
public abstract class ScannerHudMixin {
  @WrapOperation(
      method = "renderInner",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/gui/LayeredDraw$Layer;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"))
  private void cybernetics$transition(
      LayeredDraw.Layer layer,
      GuiGraphics g,
      DeltaTracker delta,
      Operation<Void> original,
      @Local GuiLayerManager.NamedLayer named) {
    var group = NativeHudTransition.vanilla(named.name());
    if (group == null) original.call(layer, g, delta);
    else NativeHudTransition.vanilla(g, group, () -> original.call(layer, g, delta));
  }
}
