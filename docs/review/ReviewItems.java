package dev.cybernetics2077.review;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.cyberware.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = Cybernetics2077.ID, bus = EventBusSubscriber.Bus.MOD)
public final class ReviewItems {
  @SubscribeEvent
  public static void register(RegisterEvent e) {
    e.register(
        Registries.ITEM,
        h ->
            h.register(
                Cybernetics2077.id("review_cortex"),
                new CyberwareItem(new Item.Properties(), CyberwareCategory.FRONTAL_CORTEX, 70000) {
                  @Override
                  public Component getName(ItemStack stack) {
                    return Component.literal("Diagnostic Cortex");
                  }
                }));
  }

  public static ItemStack cortex() {
    return new ItemStack(BuiltInRegistries.ITEM.get(Cybernetics2077.id("review_cortex")));
  }
}
