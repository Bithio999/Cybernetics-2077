package dev.cybernetics2077.compat.review;

import dev.cybernetics2077.*;
import java.nio.file.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;

/** Native creative inventory, real mod tab and real search filtering; review builds only. */
public final class CreativeClientReview {
  private static java.lang.reflect.Method refresh;
  private static EditBox search;

  public static void tick(int t, Path out) throws Exception {
    var mc = Minecraft.getInstance();
    if (t == 0) {
      Files.createDirectories(out);
      mc.getSingleplayerServer()
          .execute(
              () ->
                  mc.getSingleplayerServer()
                      .getPlayerList()
                      .getPlayer(mc.player.getUUID())
                      .setGameMode(net.minecraft.world.level.GameType.CREATIVE));
    }
    if (t == 15) {
      var screen = new CreativeModeInventoryScreen(mc.player, mc.level.enabledFeatures(), false);
      mc.setScreen(screen);
      var tab = CreativeContent.TAB.get();
      for (int i = 0; i < 10 && !screen.getCurrentPage().getVisibleTabs().contains(tab); i++) {
        var next =
            screen.children().stream()
                .filter(net.minecraft.client.gui.components.AbstractButton.class::isInstance)
                .map(net.minecraft.client.gui.components.AbstractButton.class::cast)
                .filter(b -> b.visible && b.active && b.getMessage().getString().equals(">"))
                .findFirst()
                .orElseThrow();
        screen.mouseClicked(next.getX() + 5, next.getY() + 5, 0);
        screen.mouseReleased(next.getX() + 5, next.getY() + 5, 0);
      }
      if (!screen.getCurrentPage().getVisibleTabs().contains(tab))
        throw new IllegalStateException("Mod tab page is inaccessible");
      var tabX =
          CreativeModeInventoryScreen.class.getDeclaredMethod("getTabX", CreativeModeTab.class);
      tabX.setAccessible(true);
      var tabY =
          CreativeModeInventoryScreen.class.getDeclaredMethod("getTabY", CreativeModeTab.class);
      tabY.setAccessible(true);
      double x = screen.getGuiLeft() + ((Number) tabX.invoke(screen, tab)).intValue() + 12,
          y = screen.getGuiTop() + ((Number) tabY.invoke(screen, tab)).intValue() + 12;
      screen.mouseClicked(x, y, 0);
      screen.mouseReleased(x, y, 0);
      var field = CreativeModeInventoryScreen.class.getDeclaredField("searchBox");
      field.setAccessible(true);
      search = (EditBox) field.get(screen);
      refresh = CreativeModeInventoryScreen.class.getDeclaredMethod("refreshSearchResults");
      refresh.setAccessible(true);
      if (!search.isVisible() || screen.getMenu().items.size() != CreativeContent.items().size())
        throw new IllegalStateException("Native creative contents/search absent");
    }
    if (t == 20) {
      org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), 0, 0);
      var move =
          mc.mouseHandler
              .getClass()
              .getDeclaredMethod("onMove", long.class, double.class, double.class);
      move.setAccessible(true);
      move.invoke(mc.mouseHandler, mc.getWindow().getWindow(), 0d, 0d);
    }
    if (t == 30)
      Screenshot.grab(out.toFile(), "creative-all.png", mc.getMainRenderTarget(), m -> {});
    if (t == 35) {
      search.setValue("Sandevistan");
      refresh.invoke(mc.screen);
    }
    if (t == 50) {
      var screen = (CreativeModeInventoryScreen) mc.screen;
      if (screen.getMenu().items.size() != 6
          || screen.getMenu().items.stream()
              .anyMatch(
                  s ->
                      !s.getHoverName()
                          .getString()
                          .toLowerCase(java.util.Locale.ROOT)
                          .contains("sandevistan")))
        throw new IllegalStateException("Native per-tab search did not filter six Sandevistans");
      Screenshot.grab(out.toFile(), "creative-search.png", mc.getMainRenderTarget(), m -> {});
    }
    if (t == 60) {
      search.setValue("");
      refresh.invoke(mc.screen);
    }
    if (t == 75) {
      var screen = (CreativeModeInventoryScreen) mc.screen;
      if (screen.getMenu().items.size() != CreativeContent.items().size())
        throw new IllegalStateException("Clearing search lost items");
      Files.writeString(
          out.resolve("passed.txt"),
          "PASS: native Cybernetics 2077 creative tab; "
              + CreativeContent.items().size()
              + " canonical items; native search returns six Sandevistans and clearing restores all"
              + " contents.\n");
      mc.stop();
    }
  }
}
