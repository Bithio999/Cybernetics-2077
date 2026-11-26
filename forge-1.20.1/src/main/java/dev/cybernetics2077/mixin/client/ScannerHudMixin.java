package dev.cybernetics2077.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.cybernetics2077.client.ui.NativeHudTransition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ForgeGui.class, remap = false)
public abstract class ScannerHudMixin {
  @WrapOperation(
      method = "lambda$render$0",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraftforge/client/gui/overlay/IGuiOverlay;render(Lnet/minecraftforge/client/gui/overlay/ForgeGui;Lnet/minecraft/client/gui/GuiGraphics;FII)V"))
  private void cybernetics$overlay(
      IGuiOverlay overlay,
      ForgeGui gui,
      GuiGraphics g,
      float partial,
      int width,
      int height,
      Operation<Void> original) {
    NativeHudTransition.Group group = null;
    for (var built : VanillaGuiOverlay.values())
      if (built.type().overlay() == overlay) {
        group = NativeHudTransition.vanilla(built.id());
        break;
      }
    if (group == null) original.call(overlay, gui, g, partial, width, height);
    else
      NativeHudTransition.vanilla(
          g, group, () -> original.call(overlay, gui, g, partial, width, height));
  }
}
