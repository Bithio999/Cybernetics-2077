package dev.cybernetics2077.review;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.client.ui.*;
import dev.cybernetics2077.cyberware.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Actual menu opening, native mouse/keyboard input and server synchronization in a disposable save.
 */
@EventBusSubscriber(modid = Cybernetics2077.ID, value = Dist.CLIENT)
public final class CyberwareClientReview {
  static final Path OUT =
      Path.of(
          System.getProperty("cybernetics2077.reviewRoot", "/tmp/cybernetics2077-review"),
          "capture");
  static int age, ticks;
  static volatile boolean ready;
  static volatile String error;
  static boolean finished;

  static void server(java.util.function.Consumer<ServerPlayer> action) {
    var s = Minecraft.getInstance().getSingleplayerServer();
    s.execute(
        () -> {
          try {
            action.accept(s.getPlayerList().getPlayers().getFirst());
          } catch (Throwable ex) {
            error = ex.toString();
            ex.printStackTrace();
          }
        });
  }

  static void shot(String name) throws Exception {
    Files.createDirectories(OUT);
    try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
      image.writeToFile(OUT.resolve(name + ".png"));
    }
  }

  static void mouse(String name, Class<?>[] types, Object... args) throws Exception {
    var method = MouseHandler.class.getDeclaredMethod(name, types);
    method.setAccessible(true);
    method.invoke(Minecraft.getInstance().mouseHandler, args);
  }

  static void click(double x, double y) throws Exception {
    var mc = Minecraft.getInstance();
    long window = mc.getWindow().getWindow();
    mouse(
        "onMove",
        new Class[] {long.class, double.class, double.class},
        window,
        x * mc.getWindow().getGuiScale(),
        y * mc.getWindow().getGuiScale());
    mouse("onPress", new Class[] {long.class, int.class, int.class, int.class}, window, 0, 1, 0);
    mouse("onPress", new Class[] {long.class, int.class, int.class, int.class}, window, 0, 0, 0);
  }

  static void openButton() throws Exception {
    var mc = Minecraft.getInstance();
    var button =
        mc.screen.children().stream()
            .filter(c -> c instanceof CyberwareInventoryButton)
            .map(c -> (CyberwareInventoryButton) c)
            .findFirst()
            .orElseThrow();
    click(button.getX() + 10, button.getY() + 9);
  }

  static CyberwareScreen screen() {
    if (!(Minecraft.getInstance().screen instanceof CyberwareScreen s))
      throw new IllegalStateException(
          "Cyberware screen did not open: " + Minecraft.getInstance().screen);
    return s;
  }

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post e) {
    var mc = Minecraft.getInstance();
    if (finished) return;
    if (age == 100 || age == 300)
      try {
        shot("bootstrap-" + age);
        System.out.println("Review bootstrap screen: " + mc.screen);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    if (++age > 1200 || error != null) {
      try {
        Files.createDirectories(OUT);
        Files.writeString(OUT.resolve("failed.txt"), error == null ? "Timeout at " + ticks : error);
      } catch (Exception ignored) {
      }
      finished = true;
      mc.stop();
      return;
    }
    if (mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
      mc.screen.children().stream()
          .filter(
              c ->
                  c instanceof net.minecraft.client.gui.components.Button b
                      && b.getMessage()
                          .getString()
                          .equals(
                              net.minecraft.network.chat.Component.translatable("gui.continue")
                                  .getString()))
          .map(c -> (net.minecraft.client.gui.components.Button) c)
          .findFirst()
          .ifPresent(net.minecraft.client.gui.components.Button::onPress);
    }
    mc.getToasts().clear();
    if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
    if (!mc.getSingleplayerServer()
        .getWorldPath(LevelResource.ROOT)
        .toString()
        .contains("Cybernetics Review")) {
      error = "Refusing non-review save";
      return;
    }
    try {
      if (!ready) {
        if (ticks++ == 0) {
          Files.createDirectories(OUT);
          Files.deleteIfExists(OUT.resolve("passed.txt"));
          Files.deleteIfExists(OUT.resolve("failed.txt"));
          mc.options.pauseOnLostFocus = false;
          mc.options.guiScale().set(2);
          mc.options.enableVsync().set(false);
          mc.options.hideGui = false;
          mc.resizeDisplay();
          mc.setScreen(null);
          server(
              p -> {
                p.setGameMode(GameType.SURVIVAL);
                p.getInventory().clearContent();
                p.setItemSlot(
                    net.minecraft.world.entity.EquipmentSlot.CHEST,
                    new net.minecraft.world.item.ItemStack(
                        net.minecraft.world.item.Items.IRON_CHESTPLATE));
                for (int i = 0; i < 21; i++)
                  p.getData(Cybernetics2077.CYBERWARE)
                      .setStackInSlot(i, net.minecraft.world.item.ItemStack.EMPTY);
                for (int x = -8; x <= 8; x++)
                  for (int z = -8; z <= 8; z++) {
                    p.serverLevel()
                        .setBlock(
                            new BlockPos(x, 63, z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                    for (int y = 64; y <= 70; y++)
                      p.serverLevel()
                          .setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                  }
                p.teleportTo(p.server.overworld(), .5, 64, .5, Set.of(), 0, 0);
                p.serverLevel().setDayTime(6000);
                p.serverLevel().setWeatherParameters(60000, 0, false, false);
                ready = true;
              });
        }
        return;
      }
      ticks++;
      if (ticks == 60) mc.setScreen(new InventoryScreen(mc.player));
      if (ticks == 70) shot("inventory-button");
      if (ticks == 75) openButton();
      if (ticks == 95) {
        var s = screen();
        if (s.getMenu().slots.size() != 21
            || s.getMenu().capacityLimit() != 100
            || s.getMenu().installedCount() != 0)
          throw new IllegalStateException("Initial menu/slot/stats synchronization failed");
        shot("cyberware-main");
      }
      if (ticks == 110) {
        var l = CyberLayout.fit(mc.screen.width, mc.screen.height);
        click(l.screenX(578), l.screenY(212));
      }
      if (ticks == 120) {
        var field = CyberwareScreen.class.getDeclaredField("selected");
        field.setAccessible(true);
        if (field.getInt(screen()) != 0)
          throw new IllegalStateException("Native mouse did not select socket");
        shot("cyberware-selected");
      }
      if (ticks == 125) {
        screen().keyPressed(258, 0, 0);
        screen().keyPressed(257, 0, 0);
      }
      if (ticks == 140) shot("cyberware-keyboard");
      if (ticks == 150) org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 960, 720);
      if (ticks == 170) shot("cyberware-four-three");
      if (ticks == 180)
        org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1920, 720);
      if (ticks == 200) shot("cyberware-ultrawide");
      if (ticks == 210)
        org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1280, 720);
      if (ticks == 220) {
        mc.options.guiScale().set(3);
        mc.resizeDisplay();
      }
      if (ticks == 240) shot("cyberware-large-gui");
      if (ticks == 250) {
        mc.options.guiScale().set(1);
        mc.resizeDisplay();
      }
      if (ticks == 260) shot("cyberware-small-gui");
      if (ticks == 265)
        server(p -> p.getData(Cybernetics2077.CYBERWARE).setStackInSlot(0, ReviewItems.cortex()));
      if (ticks == 275) {
        if (screen().getMenu().capacityUsed() != 70000 || screen().getMenu().installedCount() != 1)
          throw new IllegalStateException("Live equipment sync or 32-bit stats failed");
        shot("cyberware-installed-fixture");
      }
      if (ticks == 280) {
        screen().keyPressed(256, 0, 0);
        if (!(mc.screen instanceof InventoryScreen))
          throw new IllegalStateException("Escape did not return to inventory");
        server(
            p -> {
              p.getData(Cybernetics2077.CYBERWARE)
                  .setStackInSlot(0, net.minecraft.world.item.ItemStack.EMPTY);
              p.setGameMode(GameType.CREATIVE);
            });
      }
      if (ticks == 295) {
        mc.options.guiScale().set(2);
        mc.resizeDisplay();
        mc.setScreen(new InventoryScreen(mc.player));
      }
      if (ticks == 305) {
        if (!(mc.screen instanceof CreativeModeInventoryScreen))
          throw new IllegalStateException("Creative inventory fixture did not open");
        shot("creative-button");
      }
      if (ticks == 310) openButton();
      if (ticks == 325) {
        screen();
        shot("cyberware-creative");
        screen().onClose();
      }
      if (ticks == 335) {
        if (!(mc.screen instanceof CreativeModeInventoryScreen))
          throw new IllegalStateException("Return to creative inventory failed");
        Files.writeString(
            OUT.resolve("passed.txt"),
            "Native inventory button, server menu, 21 read-only sockets, synchronized capacity,"
                + " native mouse/keyboard selection, live implant sync, GUI scales 1/2/3, 4:3 and"
                + " ultrawide, survival and creative inventory return passed.\n");
        finished = true;
        mc.stop();
      }
    } catch (Throwable ex) {
      error = ex.toString();
      ex.printStackTrace();
    }
  }
}
