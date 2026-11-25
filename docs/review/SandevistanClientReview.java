package dev.cybernetics2077.review;

import static dev.cybernetics2077.review.ContentClientReview.check;
import static dev.cybernetics2077.review.CyberwareClientReview.*;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.client.mantis.CyberwareClient;
import dev.cybernetics2077.client.temporal.*;
import dev.cybernetics2077.combat.*;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.temporal.*;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

/** Native input, tracked second runner, actual render capture and GPU lens in a disposable save. */
final class SandevistanClientReview {
  static ServerPlayer actor;
  static int actorId, target;
  static int maxGhosts, maxVertices, maxActors;
  static long sampleNanos;
  static int sampleFrames;
  static final List<Double> frames = new ArrayList<>();

  static void frame() {
    int t = CyberwareClientReview.contentTicks;
    if (t < 180 || t > 285) {
      sampleNanos = 0;
      return;
    }
    long now = System.nanoTime();
    if (sampleNanos != 0) frames.add((now - sampleNanos) / 1e6);
    sampleNanos = now;
  }

  static void tick(int t) throws Exception {
    var m = Minecraft.getInstance();
    m.gui.getChat().clearMessages(true);
    if (t == 0) {
      m.setScreen(null);
      m.options.hideGui = false;
      m.options.setCameraType(CameraType.FIRST_PERSON);
      server(
          p -> {
            p.closeContainer();
            p.getAbilities().flying = false;
            p.getAbilities().mayfly = false;
            p.onUpdateAbilities();
            p.server.setPvpAllowed(true);
            p.setHealth(p.getMaxHealth());
            p.setData(Cybernetics2077.AUGMENT_STATE, new AugmentState());
            for (var mob :
                p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(80)))
              mob.discard();
            p.serverLevel().setDayTime(11000);
            p.serverLevel().setWeatherParameters(60000, 0, false, false);
            for (int x = -18; x <= 18; x++)
              for (int z = -8; z <= 28; z++) {
                p.serverLevel()
                    .setBlock(
                        new BlockPos(x, 63, z),
                        (x % 8 == 0 || z % 8 == 0 ? Blocks.SMOOTH_STONE : Blocks.GRAY_CONCRETE)
                            .defaultBlockState(),
                        3);
                for (int y = 64; y <= 69; y++)
                  p.serverLevel()
                      .setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
              }
            for (int z = 0; z <= 24; z += 8)
              for (int x : new int[] {-12, 12})
                for (int y = 64; y <= 68; y++)
                  p.serverLevel()
                      .setBlock(
                          new BlockPos(x, y, z),
                          (y == 68 ? Blocks.SEA_LANTERN : Blocks.POLISHED_DEEPSLATE)
                              .defaultBlockState(),
                          3);
            p.teleportTo(p.serverLevel(), .5, 64, .5, Set.of(), 0, 0);
          });
    }
    if (t == 15)
      AugmentClientReview.openBench(
          List.of(
              AugmentClientReview.item(Augment.DYNALAR_SANDEVISTAN),
              new ItemStack(Cybernetics2077.MANTIS_TIERS.get(3).get())));
    if (t == 40) {
      check(
          m.player.containerMenu instanceof dev.cybernetics2077.bench.CyberneticsBenchMenu,
          "Sandy bench did not open");
      ContentClientReview.quick(48);
      ContentClientReview.quick(49);
    }
    if (t == 55) {
      check(CyberwareClient.own().getInt("sandyTier") == 5, "Installed Dynalar not synchronized");
      shot("sandevistan-installation");
      AugmentClientReview.close();
    }
    if (t == 65)
      m.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(m.player));
    if (t == 75) openButton();
    if (t == 90) AugmentClientReview.select(3);
    if (t == 105) shot("sandevistan-details");
    if (t == 110) AugmentClientReview.close();
    if (t == 125) {
      AugmentClientReview.press(CyberwareClient.DRAW);
      server(
          p -> {
            p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
            var z = EntityType.ZOMBIE.create(p.serverLevel());
            z.setPos(.5, 64, 10);
            z.setNoAi(true);
            z.setNoGravity(true);
            z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
            z.setHealth(1000);
            p.serverLevel().addFreshEntity(z);
            target = z.getId();
          });
    }
    if (t == 155) shot("sandevistan-before");
    if (t == 160) AugmentClientReview.press(SandevistanClient.KEY);
    if (t == 170) {
      check(
          Sandevistan.active(m.player) && CyberwareClient.own().getDouble("temporalPace") == 1.25,
          "Native Sandy activation failed");
      check(
          SandevistanClient.intensity > .5 && SandevistanLens.passes > 0,
          "Lens transition never rendered");
      shot("sandevistan-activation");
      server(
          p -> {
            actor =
                new net.neoforged.neoforge.common.util.FakePlayer(
                    p.serverLevel(),
                    new com.mojang.authlib.GameProfile(
                        UUID.fromString("00000000-0000-4000-8000-000000002077"), "SecondRunner")) {
                  @Override
                  public boolean canHarmPlayer(net.minecraft.world.entity.player.Player other) {
                    return true;
                  }
                };
            actor.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            actor.setNoGravity(true);
            actor.setPos(5, 64, 9);
            actor.setYRot(90);
            actor
                .getData(Cybernetics2077.CYBERWARE)
                .setStackInSlot(
                    3, new ItemStack(Cybernetics2077.augment(Augment.DYNALAR_SANDEVISTAN, 2)));
            actor
                .getData(Cybernetics2077.CYBERWARE)
                .setStackInSlot(4, new ItemStack(Cybernetics2077.MANTIS_TIERS.get(3).get()));
            actor.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            p.connection.send(
                ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(actor)));
            p.serverLevel().addNewPlayer(actor);
            actorId = actor.getId();
            var s = CyberwareRuntime.state(actor);
            s.deployed = true;
            s.changed = p.serverLevel().getGameTime() - 40;
            AugmentRuntime.action(actor, 13);
            CyberwareRuntime.broadcast(actor);
          });
    }
    if (t >= 175 && t < 325) {
      int step = t;
      server(
          p -> {
            if (actor == null) return;
            double a = (step - 175) * .055;
            actor.setPos(5 + Math.sin(a) * 3, 64, 10 + Math.cos(a) * 3);
            actor.yBodyRot = (float) (-Math.toDegrees(a));
            actor.setYRot(actor.yBodyRot);
            actor.yHeadRot = actor.yBodyRot;
            if (step % 10 == 0) {
              var state = CyberwareRuntime.state(actor);
              state.attack = p.serverLevel().getGameTime();
              state.combo = (step / 10) % 2 + 1;
              CyberwareRuntime.broadcast(actor);
            }
            CyberwareRuntime.tick(actor);
          });
    }
    if (t == 180) m.options.keyUp.setDown(true);
    if (t == 190) m.options.keyUse.setDown(true);
    if (t == 206) m.options.keyUse.setDown(false);
    if (t == 220) {
      ContentClientReview.attack();
      shot("sandevistan-first-person");
    }
    if (t == 228) m.options.keyUp.setDown(false);
    if (t == 235) {
      m.options.setCameraType(CameraType.THIRD_PERSON_BACK);
      m.options.keyLeft.setDown(true);
    }
    if (t == 248) {
      ContentClientReview.attack();
      shot("sandevistan-third-person");
    }
    if (t == 263) {
      m.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
      m.options.keyLeft.setDown(false);
      m.options.keyRight.setDown(true);
    }
    if (t == 278) shot("sandevistan-afterimages");
    if (t == 290) {
      m.options.keyRight.setDown(false);
      m.options.setCameraType(CameraType.FIRST_PERSON);
      AugmentClientReview.press(SandevistanClient.KEY);
    }
    if (t == 305) {
      check(!Sandevistan.active(m.player), "Early deactivation failed");
      check(
          SandevistanTrails.lastActors <= 6 && maxGhosts > 0 && maxVertices > 0,
          "No bounded pose echoes were drawn");
      check(
          m.level.getEntity(actorId) instanceof net.minecraft.client.player.RemotePlayer,
          "Tracked second runner did not reach client");
      check(
          Math.abs(CyberwareClient.own().getDouble("temporalPace") - .625) < .001,
          "Remaining opponent did not slow the real player after early off");
      shot("sandevistan-recharge");
    }
    if (t >= 170 && t <= 305) {
      maxGhosts = Math.max(maxGhosts, SandevistanTrails.lastEchoes);
      maxVertices = Math.max(maxVertices, SandevistanTrails.lastVertices);
      maxActors = Math.max(maxActors, SandevistanTrails.lastActors);
    }
    if (t >= 325 && Boolean.getBoolean("cybernetics2077.cameraReview")) {
      TrailCameraReview.tick(t);
      return;
    }
    if (t >= 325 && Boolean.getBoolean("cybernetics2077.handsReview")) {
      HandsClientReview.tick(t);
      return;
    }
    if (t >= 325 && Boolean.getBoolean("cybernetics2077.trailReview")) {
      TrailClientReview.tick(t);
      return;
    }
    if (t == 325) {
      server(
          p -> {
            if (actor != null) {
              Sandevistan.stop(actor, AugmentRuntime.now(actor));
              actor.discard();
              actor = null;
            }
          });
      Files.writeString(
          OUT.resolve("sandevistan-passed.txt"),
          "PASS: native bench install and OS detail page; key activation and early off; actual GPU"
              + " lens; tracked second player with independent Sandevistan and Mantis Blades;"
              + " textured pose trails and armor; first-person blade echoes; real battery recharge"
              + " HUD. Maximum actors="
              + maxActors
              + ", echoes="
              + maxGhosts
              + ", vertices="
              + maxVertices
              + ".\n");
      if (Boolean.getBoolean("cybernetics2077.recording")) {
        Files.writeString(
            OUT.resolve("sandevistan-recording-marks.json"),
            new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(recordingMarks));
        if (!frames.isEmpty()) {
          var sorted = frames.stream().sorted().toList();
          Files.writeString(
              OUT.resolve("sandevistan-frame-times.txt"),
              "Frames="
                  + sorted.size()
                  + ", median="
                  + sorted.get(sorted.size() / 2)
                  + " ms, p95="
                  + sorted.get((int) (sorted.size() * .95))
                  + " ms\n");
        }
      }
      finished = true;
      m.stop();
    }
  }
}
