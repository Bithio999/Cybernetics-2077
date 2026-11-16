package dev.cybernetics2077.client;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.client.ui.*;
import dev.cybernetics2077.network.OpenCyberware;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Cybernetics2077.ID, value = Dist.CLIENT)
public final class ClientHooks {
  private static final Tooltip OPEN_TOOLTIP =
      Tooltip.create(Component.translatable("cybernetics2077.button.open"));
  private static final Tooltip BUSY_TOOLTIP =
      Tooltip.create(Component.translatable("cybernetics2077.button.busy"));
  private static Screen inventory;
  private static CyberwareInventoryButton button;

  @SubscribeEvent
  public static void init(ScreenEvent.Init.Post e) {
    if (e.getScreen() != Minecraft.getInstance().screen) return;
    if (e.getScreen() instanceof InventoryScreen
        || e.getScreen() instanceof CreativeModeInventoryScreen) {
      inventory = e.getScreen();
      button =
          new CyberwareInventoryButton(
              () -> {
                PacketDistributor.sendToServer(OpenCyberware.INSTANCE);
              });
      position();
      e.addListener(button);
    } else {
      inventory = null;
      button = null;
    }
  }

  private static void position() {
    if (inventory instanceof AbstractContainerScreen<?> screen && button != null) {
      // Creative has a search field and scrollbar in the header's right corner.
      int x =
          screen.getGuiLeft()
              + screen.getXSize()
              + (screen instanceof CreativeModeInventoryScreen ? 4 : -23);
      button.setPosition(x, screen.getGuiTop() + 5);
      var player = Minecraft.getInstance().player;
      boolean free = player != null && player.containerMenu.getCarried().isEmpty();
      button.active = free;
      button.setTooltip(free ? OPEN_TOOLTIP : BUSY_TOOLTIP);
    }
  }

  @SubscribeEvent
  public static void render(ScreenEvent.Render.Pre e) {
    if (e.getScreen() == inventory) position();
  }

  @EventBusSubscriber(
      modid = Cybernetics2077.ID,
      value = Dist.CLIENT,
      bus = EventBusSubscriber.Bus.MOD)
  public static final class Setup {
    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent e) {
      e.<dev.cybernetics2077.cyberware.CyberwareMenu, CyberwareScreen>register(
          Cybernetics2077.MENU.get(), CyberwareScreen::new);
    }

    @SubscribeEvent
    public static void shaders(RegisterShadersEvent e) throws java.io.IOException {
      HologramRenderer.register(e);
      CyberMaterial.register(e);
    }

    @SubscribeEvent
    public static void reload(RegisterClientReloadListenersEvent e) {
      e.registerReloadListener(
          (net.minecraft.server.packs.resources.ResourceManagerReloadListener)
              manager -> CyberwareScreen.reloadResources());
    }
  }

  private ClientHooks() {}
}
