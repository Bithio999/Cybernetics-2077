package dev.cybernetics2077.compat.review;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.combat.*;
import dev.cybernetics2077.cyberware.*;
import java.nio.file.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Disposable native client smoke test, included only by -PcompatClientReview. */
@EventBusSubscriber(modid = "cybernetics2077", value = Dist.CLIENT)
public final class NeoClientReview {
  private static boolean opening;
  private static int tick;
  private static final Path OUT =
      Path.of(System.getProperty("cybernetics2077.compatOutput", "/tmp/cybernetics-forge-review"));

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post e) {
    var mc = Minecraft.getInstance();
    try {
      if (!opening && mc.screen instanceof TitleScreen) {
        opening = true;
        mc.createWorldOpenFlows()
            .openWorld("Cybernetics Neo Review", () -> mc.setScreen(new TitleScreen()));
        return;
      }
      if (!opening && mc.screen instanceof AccessibilityOnboardingScreen) {
        mc.setScreen(new TitleScreen());
        return;
      }
      if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
      if (tick == 0 && net.neoforged.fml.ModList.get().isLoaded("jei")) {
        var internal = Class.forName("mezz.jei.common.Internal");
        if (((java.util.Optional<?>) internal.getMethod("getOptionalJeiRuntime").invoke(null))
            .isEmpty()) return;
      }
      tick++;
      if (tick == 15) {
        dev.cybernetics2077.client.ui.ScannerClient.KEY.setKeyModifierAndCode(
            net.neoforged.neoforge.client.settings.KeyModifier.NONE,
            dev.cybernetics2077.client.ui.ScannerClient.KEY.getKey());
        mc.getSingleplayerServer()
            .execute(
                () -> {
                  var p = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                  p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                  p.teleportTo(0, 90, 0);
                  var inv = p.getData(Cybernetics2077.CYBERWARE);
                  inv.setStackInSlot(
                      CyberwareCategory.FACE.firstSlot(),
                      new ItemStack(Cybernetics2077.OPTICS.get()));
                  inv.setStackInSlot(
                      CyberwareCategory.ARMS.firstSlot(),
                      new ItemStack(Cybernetics2077.MANTIS.get()));
                  inv.setStackInSlot(
                      CyberwareCategory.OPERATING_SYSTEM.firstSlot(),
                      new ItemStack(Cybernetics2077.CYBERDECK.get()));
                  var menu =
                      new dev.cybernetics2077.deck.CyberdeckMenu(
                          77,
                          p.getInventory(),
                          net.minecraft.world.inventory.ContainerLevelAccess.NULL,
                          inv.getStackInSlot(CyberwareCategory.OPERATING_SYSTEM.firstSlot()));
                  menu.getSlot(0).set(new ItemStack(Cybernetics2077.SHORT_CIRCUIT.get()));
                  for (int x = -5; x <= 5; x++)
                    for (int z = -5; z <= 7; z++)
                      p.serverLevel()
                          .setBlock(
                              new BlockPos(x, 89, z),
                              net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),
                              3);
                  var target = EntityType.HUSK.create(p.serverLevel());
                  target.setNoAi(true);
                  target.moveTo(0, 90, 6, 180, 0);
                  p.serverLevel().addFreshEntity(target);
                  dev.cybernetics2077.scanner.ScannerNetwork.sync(p);
                  CyberwareRuntime.broadcast(p);
                });
      }
      if (tick == 60)
        PacketDistributor.sendToServer(dev.cybernetics2077.network.OpenCyberware.INSTANCE);
      if (tick == 90) {
        if (!(mc.screen instanceof dev.cybernetics2077.client.ui.CyberwareScreen))
          throw new IllegalStateException("Viewer packet failed to open native menu");
        if (net.neoforged.fml.ModList.get().isLoaded("jei")) {
          var runtime =
              Class.forName("mezz.jei.common.Internal").getMethod("getJeiRuntime").invoke(null);
          var api = Class.forName("mezz.jei.api.runtime.IJeiRuntime");
          var overlay = api.getMethod("getIngredientListOverlay").invoke(runtime);
          if ((boolean)
              Class.forName("mezz.jei.api.runtime.IIngredientListOverlay")
                  .getMethod("isListDisplayed")
                  .invoke(overlay))
            throw new IllegalStateException("JEI overlaps the full-screen cyberware menu");
        }
        shot("cyberware");
        var field = dev.cybernetics2077.client.ui.HologramRenderer.class.getDeclaredField("target");
        field.setAccessible(true);
        try (var image =
            Screenshot.takeScreenshot((com.mojang.blaze3d.pipeline.RenderTarget) field.get(null))) {
          image.writeToFile(OUT.resolve("hologram-buffer.png"));
        }
      }
      if (tick == 100) {
        mc.player.closeContainer();
        mc.setScreen(null);
        PacketDistributor.sendToServer(new CyberwareNetwork.Action(0));
      }
      if (tick == 140) shot("mantis");
      if (tick == 150) {
        PacketDistributor.sendToServer(new CyberwareNetwork.Action(0));
        dev.cybernetics2077.client.ui.ScannerClient.KEY.setDown(true);
      }
      if (tick == 180) shot("scanner");
      if (tick == 190) {
        dev.cybernetics2077.client.ui.ScannerClient.KEY.setDown(false);
        mc.getSingleplayerServer()
            .execute(
                () -> {
                  var p = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                  p.getData(Cybernetics2077.CYBERWARE)
                      .setStackInSlot(
                          CyberwareCategory.OPERATING_SYSTEM.firstSlot(),
                          new ItemStack(Cybernetics2077.augment(Augment.MILITECH_APOGEE, 5)));
                  dev.cybernetics2077.temporal.Sandevistan.toggle(p);
                });
      }
      if (tick == 220) shot("sandevistan");
      if (tick == 230) {
        mc.getSingleplayerServer()
            .execute(
                () -> {
                  var p = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                  if (dev.cybernetics2077.temporal.Sandevistan.active(p))
                    dev.cybernetics2077.temporal.Sandevistan.toggle(p);
                });
      }
      if (Boolean.getBoolean("cybernetics2077.gunReview") && tick >= 300) {
        TacZHudReview.tick(tick - 300, OUT);
        return;
      }
      if (tick == 300) {
        Files.createDirectories(OUT);
        Files.writeString(
            OUT.resolve("passed.txt"),
            "NeoForge client: login, cyberware menu/networking, hologram, mantis, scanner,"
                + " Sandevistan rendered.\n");
        mc.stop();
      }
    } catch (Throwable failure) {
      try {
        Files.createDirectories(OUT);
        Files.writeString(OUT.resolve("failed.txt"), failure.toString());
      } catch (Exception ignored) {
      }
      failure.printStackTrace();
      mc.stop();
    }
  }

  private static void shot(String name) throws Exception {
    Files.createDirectories(OUT);
    Screenshot.grab(
        OUT.toFile(), name + ".png", Minecraft.getInstance().getMainRenderTarget(), message -> {});
  }
}
