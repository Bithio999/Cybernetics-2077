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

  static void benchClick(int index) throws Exception {
    var mc = Minecraft.getInstance();
    if (!(mc.screen instanceof CyberneticsBenchScreen b))
      throw new IllegalStateException("Expected workstation menu: " + mc.screen);
    var slot = mc.player.containerMenu.getSlot(index);
    click(b.getGuiLeft() + slot.x + 8, b.getGuiTop() + slot.y + 8);
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
        if (mc.screen instanceof CyberwareScreen) screen().keyPressed(256, 0, 0);
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
        // Continue into real workstation installation and optical scanning checks.
      }
      if (ticks == 340) {
        mc.setScreen(null);
        server(
            p -> {
              p.closeContainer();
              p.setGameMode(GameType.SURVIVAL);
              for (var old :
                  p.serverLevel()
                      .getEntitiesOfClass(
                          net.minecraft.world.entity.Mob.class, p.getBoundingBox().inflate(20)))
                old.discard();
              p.getInventory().clearContent();
              p.getInventory()
                  .setItem(0, new net.minecraft.world.item.ItemStack(Cybernetics2077.OPTICS.get()));
              p.serverLevel()
                  .setBlock(
                      new BlockPos(0, 64, 2), Cybernetics2077.BENCH.get().defaultBlockState(), 3);
              var zombie = net.minecraft.world.entity.EntityType.HUSK.create(p.serverLevel());
              zombie.setPos(.5, 64, 7.5);
              zombie.setNoAi(true);
              zombie.setInvulnerable(true);
              zombie.setCustomName(
                  net.minecraft.network.chat.Component.literal("Scanner Test Target"));
              p.serverLevel().addFreshEntity(zombie);
              p.teleportTo(p.server.overworld(), .5, 64, .5, Set.of(), 0, 0);
            });
      }
      if (ticks == 355)
        mc.gameMode.useItemOn(
            mc.player,
            net.minecraft.world.InteractionHand.MAIN_HAND,
            new net.minecraft.world.phys.BlockHitResult(
                new net.minecraft.world.phys.Vec3(.5, 64.7, 2),
                net.minecraft.core.Direction.NORTH,
                new BlockPos(0, 64, 2),
                false));
      if (ticks == 365) {
        if (!(mc.screen instanceof CyberneticsBenchScreen))
          throw new IllegalStateException("Bench interaction did not open installation menu");
        benchClick(48);
      }
      if (ticks == 370) benchClick(5);
      if (ticks == 378)
        mouse(
            "onMove",
            new Class[] {long.class, double.class, double.class},
            mc.getWindow().getWindow(),
            1100d,
            650d);
      if (ticks == 380) {
        if (!(mc.player.containerMenu instanceof dev.cybernetics2077.bench.CyberneticsBenchMenu b)
            || !b.getSlot(5).getItem().is(Cybernetics2077.OPTICS.get())
            || b.capacityUsed() != 4
            || !b.getCarried().isEmpty()
            || !ScannerClient.available())
          throw new IllegalStateException("Native bench install/synchronization failed");
        shot("installation-bench");
        mc.screen.onClose();
      }
      if (ticks == 390) {
        mc.player.setYRot(0);
        mc.player.setXRot(0);
        ScannerClient.KEY.setDown(true);
      }
      if (ticks >= 391 && ticks <= 406) shot("scan-opening-" + (ticks - 391));
      if (ticks == 456) {
        if (!ScannerClient.active())
          throw new IllegalStateException("Installed optics did not enable scanner");
        var field = ScannerClient.class.getDeclaredField("target");
        field.setAccessible(true);
        var data = (net.minecraft.nbt.CompoundTag) field.get(null);
        if (!data.contains("id")
            || !data.getString("name").equals("Scanner Test Target")
            || data.contains("ep")
            || data.contains("mp"))
          throw new IllegalStateException(
              "Optical scan did not acquire correct vanilla target: " + data);
        System.out.println(
            "Scanner animation: age="
                + ScannerClient.hudMotion().age()
                + " data="
                + ScannerClient.hudMotion().data()
                + " shader="
                + com.mojang.blaze3d.systems.RenderSystem.getShaderColor()[3]);
        if (ScannerClient.hudMotion().data() < .99)
          throw new IllegalStateException("Scanner data panel failed to fully reveal");
        shot("optical-scanner");
      }
      if (ticks == 460) ScannerClient.KEY.setDown(false);
      if (ticks == 470) {
        if (ScannerClient.active() || ScannerClient.hudMotion().nativeHotbar() < .99)
          throw new IllegalStateException("Scanner did not restore native HUD");
        mc.setScreen(new InventoryScreen(mc.player));
      }
      if (ticks == 475) openButton();
      if (ticks == 485) {
        shot("installed-optics-overview");
        var l = CyberLayout.fit(mc.screen.width, mc.screen.height);
        click(l.screenX(1276), l.screenY(370));
      }
      if (ticks >= 486 && ticks <= 498) shot("face-zoom-" + (ticks - 486));
      if (ticks == 505) {
        shot("installed-optics-detail");
        screen().keyPressed(65, 0, 0);
        screen().keyPressed(65, 0, 0);
        screen().keyPressed(65, 0, 0);
      }
      if (ticks == 525) {
        shot("cortex-detail");
        screen().onClose();
        mc.setScreen(null);
      }
      if (ticks == 530)
        mc.gameMode.useItemOn(
            mc.player,
            net.minecraft.world.InteractionHand.MAIN_HAND,
            new net.minecraft.world.phys.BlockHitResult(
                new net.minecraft.world.phys.Vec3(.5, 64.7, 2),
                net.minecraft.core.Direction.NORTH,
                new BlockPos(0, 64, 2),
                false));
      if (ticks == 540) benchClick(5);
      if (ticks == 545) benchClick(48);
      if (ticks == 555) {
        if (ScannerClient.available() || !mc.player.containerMenu.getSlot(5).getItem().isEmpty())
          throw new IllegalStateException("Removing optics left scanner enabled");
        mc.screen.onClose();
        ScannerClient.KEY.setDown(true);
      }
      if (ticks == 565) {
        if (ScannerClient.active())
          throw new IllegalStateException("Scanner opened without installed optics");
        ScannerClient.KEY.setDown(false);
        mc.setScreen(new InventoryScreen(mc.player));
      }
      if (ticks == 570) openButton();
      if (ticks == 580) {
        var l = CyberLayout.fit(mc.screen.width, mc.screen.height);
        click(l.screenX(578), l.screenY(212));
      }
      for (int category = 0; category < 10; category++) {
        int at = 600 + category * 20;
        if (ticks == at) {
          shot("anatomy-" + CyberwareCategory.values()[category].id);
          if (category < 9) screen().keyPressed(68, 0, 0);
        }
      }
      if (ticks == 790) {
        Files.writeString(
            OUT.resolve("passed.txt"),
            "PASS: native inventory button; 21 read-only viewer sockets; mouse and keyboard"
                + " category zoom; GUI scales 1/2/3, 4:3 and ultrawide; creative inventory"
                + " entry/return; live 32-bit capacity sync; actual bench block interaction; native"
                + " pickup/install/remove; scanner availability follows equipment; held scanner"
                + " acquires server-authorized vanilla target; no Tensura fields; native HUD"
                + " returns after release.\n");
        finished = true;
        mc.stop();
      }
    } catch (Throwable ex) {
      error = ex.toString();
      ex.printStackTrace();
    }
  }
}
