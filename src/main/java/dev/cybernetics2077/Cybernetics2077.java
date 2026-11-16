package dev.cybernetics2077;

import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.network.OpenCyberware;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.*;

@Mod(Cybernetics2077.ID)
public final class Cybernetics2077 {
  public static final String ID = "cybernetics2077";
  private static final DeferredRegister<MenuType<?>> MENUS =
      DeferredRegister.create(Registries.MENU, ID);
  private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
      DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ID);
  public static final Supplier<MenuType<CyberwareMenu>> MENU =
      MENUS.register(
          "cyberware", () -> new MenuType<>(CyberwareMenu::new, FeatureFlags.DEFAULT_FLAGS));
  public static final Supplier<AttachmentType<CyberwareInventory>> CYBERWARE =
      ATTACHMENTS.register(
          "cyberware",
          () -> AttachmentType.serializable(CyberwareInventory::new).copyOnDeath().build());
  private static final ModConfigSpec.Builder CONFIG = new ModConfigSpec.Builder();
  public static final ModConfigSpec.IntValue CAPACITY =
      CONFIG
          .comment("Base cyberware capacity. No implants are included in the interface release.")
          .defineInRange("baseCapacity", 100, 1, 100000);
  private static final ModConfigSpec SPEC = CONFIG.build();

  public Cybernetics2077(IEventBus bus, ModContainer container) {
    MENUS.register(bus);
    ATTACHMENTS.register(bus);
    container.registerConfig(ModConfig.Type.SERVER, SPEC);
    bus.addListener(this::network);
  }

  private void network(RegisterPayloadHandlersEvent e) {
    e.registrar("1")
        .playToServer(
            OpenCyberware.TYPE,
            OpenCyberware.CODEC,
            (payload, context) ->
                context.enqueueWork(
                    () -> {
                      if (context.player() instanceof ServerPlayer p
                          && p.isAlive()
                          && p.containerMenu == p.inventoryMenu
                          && p.inventoryMenu.getCarried().isEmpty())
                        p.openMenu(
                            new SimpleMenuProvider(
                                (id, inventory, owner) -> CyberwareMenu.server(id, inventory),
                                Component.translatable("cybernetics2077.screen.title")));
                    }));
  }

  public static ResourceLocation id(String path) {
    return ResourceLocation.fromNamespaceAndPath(ID, path);
  }
}
