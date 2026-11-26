package dev.cybernetics2077.compat.review;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.city.*;
import dev.cybernetics2077.client.ui.*;
import dev.cybernetics2077.cyberware.*;
import java.nio.file.*;
import java.util.function.Consumer;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Native packets and rendered screens in the disposable client-review save. */
public final class BountyClientReview {
  private static volatile Throwable failure;
  private static volatile int fixerId, targetId;

  private static void check(boolean pass, String message) {
    if (!pass) throw new IllegalStateException(message);
  }

  private static void server(Consumer<ServerPlayer> work) {
    var mc = Minecraft.getInstance();
    mc.getSingleplayerServer()
        .execute(
            () -> {
              try {
                work.accept(
                    mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()));
              } catch (Throwable t) {
                failure = t;
              }
            });
  }

  private static void button(String label) {
    var mc = Minecraft.getInstance();
    var b =
        mc.screen.children().stream()
            .filter(AbstractButton.class::isInstance)
            .map(AbstractButton.class::cast)
            .filter(v -> v.visible && v.active && v.getMessage().getString().equals(label))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Button missing: " + label));
    b.onPress();
  }

  private static void shot(Path out, String name) {
    Screenshot.grab(
        out.toFile(), name + ".png", Minecraft.getInstance().getMainRenderTarget(), m -> {});
  }

  private static void open(ServerPlayer p) {
    var fixer = (CityResident) p.serverLevel().getEntity(fixerId);
    p.openMenu(
        new SimpleMenuProvider(
            (id, inv, owner) -> new FixerMenu(id, inv, fixerId), fixer.getName()),
        b -> b.writeVarInt(fixerId));
  }

  public static void tick(int tick, Path out) throws Exception {
    var mc = Minecraft.getInstance();
    if (failure != null) throw new IllegalStateException("Server review failed", failure);
    if (tick == 0) {
      mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
      mc.getToasts().clear();
    }
    if (tick == 10 || tick == 130 || tick == 160) {
      mc.getToasts().clear();
      mc.gui.getChat().clearMessages(false);
    }
    if (tick == 0)
      server(
          p -> {
            p.setGameMode(GameType.SURVIVAL);
            p.teleportTo(0, 90, 0);
            p.setHealth(p.getMaxHealth());
            p.getFoodData().setFoodLevel(20);
            p.setData(CityContent.ACCOUNT, new StreetAccount());
            p.getData(CityContent.ACCOUNT).reputation(20);
            BountyLedger.get(p.server).remove(p.getUUID());
            p.getInventory().clearContent();
            for (int i = 0; i < p.getData(Cybernetics2077.CYBERWARE).getSlots(); i++)
              p.getData(Cybernetics2077.CYBERWARE).setStackInSlot(i, ItemStack.EMPTY);
            p.getData(Cybernetics2077.CYBERWARE)
                .setStackInSlot(
                    CyberwareCategory.FACE.firstSlot(),
                    new ItemStack(Cybernetics2077.OPTICS.get()));
            for (var e :
                p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(80)))
              e.discard();
            for (int x = -8; x <= 80; x++)
              for (int z = -32; z <= 32; z++)
                p.serverLevel()
                    .setBlock(new BlockPos(x, 89, z), Blocks.STONE.defaultBlockState(), 3);
            var fixer = CityContent.FIXER.get().create(p.serverLevel());
            fixer.moveTo(2, 90, 0, 90, 0);
            fixer.setNoAi(true);
            p.serverLevel().addFreshEntity(fixer);
            fixerId = fixer.getId();
            var doc = CityContent.RIPPERDOC.get().create(p.serverLevel());
            doc.moveTo(2, 90, 3, 90, 0);
            doc.setNoAi(true);
            p.serverLevel().addFreshEntity(doc);
            ClinicDirectory.get(p.server).record(doc);
            dev.cybernetics2077.scanner.ScannerNetwork.sync(p);
            open(p);
          });
    if (tick == 20) {
      check(mc.screen instanceof FixerScreen, "Native fixer screen missing");
      check(
          ((FixerMenu) mc.player.containerMenu).board().getList("offers", 10).size() == 3,
          "No synchronized daily offers");
      shot(out, "bounty-board");
    }
    if (tick == 30) button("ACCEPT BOUNTY");
    if (tick == 50) {
      check(
          ((FixerMenu) mc.player.containerMenu).board().getCompound("job").getInt("stage") == 1,
          "Accept packet did not create active job");
      check(
          mc.player.getInventory().items.stream()
              .anyMatch(s -> s.is(CityContent.BOUNTY_CONTRACT.get())),
          "No client dossier");
      shot(out, "bounty-active");
    }
    if (tick == 60) {
      mc.player.closeContainer();
      mc.setScreen(null);
      server(
          p -> {
            var j = BountyContracts.job(p);
            j.putInt("x", 60);
            j.putInt("y", 90);
            j.putInt("z", 0);
            BountyLedger.get(p.server).put(p.getUUID(), j);
            check(BountyContracts.deploy(p), "Native objective did not deploy on safe platform");
            j = BountyContracts.job(p);
            var target = (Mob) p.serverLevel().getEntity(j.getUUID("target"));
            targetId = target.getId();
            for (var mob :
                p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(80)))
              mob.setNoAi(true);
            p.teleportTo(12, 90, 0);
          });
    }
    if (tick == 80) {
      var target = mc.level.getEntity(targetId);
      check(target != null, "Target not synchronized to client");
      var d = target.getEyePosition().subtract(mc.player.getEyePosition());
      mc.player.setYRot((float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90));
      mc.player.setXRot((float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))));
      ScannerClient.KEY.setDown(true);
    }
    if (tick == 105) {
      shot(out, "bounty-scanner");
      server(
          p ->
              check(
                  BountyContracts.job(p).getBoolean("scanned"),
                  "Native scanner did not record pre-combat identification"));
    }
    if (tick == 115) {
      ScannerClient.KEY.setDown(false);
      server(
          p -> {
            var target = (Mob) p.serverLevel().getEntity(targetId);
            target.hurt(p.serverLevel().damageSources().playerAttack(p), 100000);
            check(
                BountyContracts.job(p).getInt("stage") == 2,
                "Native kill did not confirm objective");
            p.teleportTo(0, 90, 0);
            open(p);
          });
    }
    if (tick == 140) {
      check(mc.screen instanceof FixerScreen, "Return menu missing");
      shot(out, "bounty-ready");
    }
    if (tick == 145) button("CLAIM PAYMENT");
    if (tick == 170) {
      check(
          ((FixerMenu) mc.player.containerMenu).board().getCompound("job").isEmpty(),
          "Claim did not clear active job");
      check(
          ((FixerMenu) mc.player.containerMenu).board().getCompound("report").getInt("payout")
              == 8400,
          "Client debrief payout incorrect");
      shot(out, "bounty-debrief");
      server(
          p ->
              check(p.getData(CityContent.ACCOUNT).streetCred() == 25, "Wrong earned Street Cred"));
    }
    if (tick == 180) button("DELIVERY");
    if (tick == 190) {
      shot(out, "fixer-delivery");
      button("ACCEPT CONTRACT");
    }
    if (tick == 210) {
      check(
          ((FixerMenu) mc.player.containerMenu).stage() == 1,
          "Original delivery no longer works through native menu");
      shot(out, "fixer-delivery-active");
    }
    if (tick == 220) {
      button("BOUNTIES");
      org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 640, 480);
    }
    if (tick == 240) {
      for (var child : mc.screen.children())
        if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget
            && widget.visible)
          check(
              widget.getX() >= 0
                  && widget.getY() >= 0
                  && widget.getX() + widget.getWidth() <= mc.getWindow().getGuiScaledWidth()
                  && widget.getY() + widget.getHeight() <= mc.getWindow().getGuiScaledHeight(),
              "Widget clipped in minimum GUI size");
      shot(out, "bounty-small-window");
      org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 854, 480);
    }
    if (tick == 255) {
      Files.writeString(
          out.resolve("passed.txt"),
          "PASS: daily bounty board, native accept/dossier, real target/guards deployment,"
              + " pre-combat optical scan and navigation, actual player kill, native claim, S-rated"
              + " 8400 payment and +5 SC, preserved introductory delivery.\n");
      mc.stop();
    }
  }

  private BountyClientReview() {}
}
