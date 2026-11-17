package dev.cybernetics2077.mixin;

import dev.cybernetics2077.combat.MantisHands;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Shared on client and server so rendering, modifiers, enchantments and use agree. */
@Mixin(Player.class)
public abstract class MantisHandsMixin {
  @Inject(method = "getItemBySlot", at = @At("HEAD"), cancellable = true)
  private void cybernetics$emptyCyberarms(
      EquipmentSlot slot, CallbackInfoReturnable<ItemStack> ci) {
    if ((slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND)
        && MantisHands.occupied((Player) (Object) this)) ci.setReturnValue(ItemStack.EMPTY);
  }
}
