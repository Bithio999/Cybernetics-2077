package dev.cybernetics2077.review;

import static dev.cybernetics2077.review.ContentClientReview.check;
import static dev.cybernetics2077.review.CyberwareClientReview.*;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.client.temporal.*;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.temporal.*;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.*;

/** A vanilla animation gallery plus isolated GPU pixel tests and native piston render samples. */
final class AnimationClientReview {
  static List<Map<String, Object>> pixels;
  static final List<Float> progress = new ArrayList<>();
  static final BlockPos PISTON = new BlockPos(0, 64, 7);
  static PistonMovingBlockEntity moving;
  static int sprites;
  static long bytes, uses;
  static long offAt = -1;

  static long atlasTick() throws Exception {
    var field = BlockAnimations.class.getDeclaredField("ticks");
    field.setAccessible(true);
    return field.getLong(null);
  }

  static void tick(int tick) throws Exception {
    int t = tick - 325;
    var mc = Minecraft.getInstance();
    if (t == 0) {
      Files.deleteIfExists(OUT.resolve("animations-passed.txt"));
      mc.options.keyUp.setDown(false);
      mc.options.keyRight.setDown(false);
      mc.options.keyLeft.setDown(false);
      mc.options.setCameraType(CameraType.FIRST_PERSON);
      mc.options.hideGui = true;
      server(
          p -> {
            if (SandevistanClientReview.actor != null) {
              Sandevistan.stop(SandevistanClientReview.actor, AugmentRuntime.now(p));
              SandevistanClientReview.actor.discard();
              SandevistanClientReview.actor = null;
            }
            Sandevistan.stop(p, AugmentRuntime.now(p));
            AugmentGameTests.equip(p, Augment.MILITECH_APOGEE, 5, 0);
            p.setData(Cybernetics2077.AUGMENT_STATE, new AugmentState());
            p.getAbilities().mayfly = true;
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            p.teleportTo(p.serverLevel(), .5, 66, 1, Set.of(), 0, 16);
            p.serverLevel().setDayTime(6000);
            p.serverLevel().setWeatherParameters(60000, 0, false, false);
            for (var mob :
                p.serverLevel()
                    .getEntitiesOfClass(
                        net.minecraft.world.entity.Mob.class, p.getBoundingBox().inflate(80)))
              mob.discard();
            for (int x = -10; x <= 10; x++)
              for (int z = 6; z <= 15; z++)
                for (int y = 64; y <= 69; y++)
                  p.serverLevel()
                      .setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            for (int x : new int[] {-7, -3}) {
              for (int dx = -2; dx <= 2; dx++)
                for (int dz = -2; dz <= 2; dz++)
                  p.serverLevel()
                      .setBlock(
                          new BlockPos(x + dx, 64, 10 + dz),
                          (Math.abs(dx) == 2 || Math.abs(dz) == 2
                                  ? Blocks.STONE
                                  : x == -7 ? Blocks.WATER : Blocks.LAVA)
                              .defaultBlockState(),
                          2);
            }
            p.serverLevel()
                .setBlock(new BlockPos(1, 63, 10), Blocks.NETHERRACK.defaultBlockState(), 2);
            p.serverLevel().setBlock(new BlockPos(1, 64, 10), Blocks.FIRE.defaultBlockState(), 2);
            p.serverLevel()
                .setBlock(new BlockPos(3, 63, 10), Blocks.SOUL_SOIL.defaultBlockState(), 2);
            p.serverLevel()
                .setBlock(new BlockPos(3, 64, 10), Blocks.SOUL_FIRE.defaultBlockState(), 2);
            for (int x = 5; x <= 8; x++)
              for (int y = 64; y <= 68; y++)
                if (x == 5 || x == 8 || y == 64 || y == 68)
                  p.serverLevel()
                      .setBlock(new BlockPos(x, y, 11), Blocks.OBSIDIAN.defaultBlockState(), 2);
            for (int x = 6; x <= 7; x++)
              for (int y = 65; y <= 67; y++)
                p.serverLevel()
                    .setBlock(
                        new BlockPos(x, y, 11),
                        Blocks.NETHER_PORTAL
                            .defaultBlockState()
                            .setValue(NetherPortalBlock.AXIS, Direction.Axis.X),
                        18);
            for (int x = -5; x <= -1; x += 2)
              for (int y = 65; y <= 66; y++)
                p.serverLevel()
                    .setBlock(
                        new BlockPos(x, y, 14),
                        (x == -5
                                ? Blocks.MAGMA_BLOCK
                                : x == -3 ? Blocks.PRISMARINE : Blocks.SEA_LANTERN)
                            .defaultBlockState(),
                        2);
          });
    }
    if (t == 15) shot("animated-blocks-normal");
    if (t == 20) AugmentClientReview.press(SandevistanClient.KEY);
    if (t == 45) {
      check(Sandevistan.active(mc.player), "No active gallery bubble");
      check(
          BlockAnimations.spriteCount() >= 10 && BlockAnimations.shaderUses > 0,
          "Animated texture pipeline inactive");
      shot("animated-blocks-apogee");
    }
    if (t == 65) {
      pixels = AnimationPixelProbe.run();
      sprites = BlockAnimations.spriteCount();
      bytes = BlockAnimations.textureBytes();
      uses = BlockAnimations.shaderUses;
    }
    if (t == 75) {
      // This fixture only initializes a client moving model; normal rendering/ticking runs it.
      var state =
          Blocks.MOVING_PISTON
              .defaultBlockState()
              .setValue(MovingPistonBlock.FACING, Direction.EAST);
      mc.level.setBlock(PISTON, state, 2);
      moving =
          new PistonMovingBlockEntity(
              PISTON, state, Blocks.GOLD_BLOCK.defaultBlockState(), Direction.EAST, true, false);
      mc.level.setBlockEntity(moving);
    }
    if (t >= 76 && t <= 81) {
      float value = moving.getProgress(1);
      progress.add(value);
      check(value > 0 && value < 1, "Piston did not have fractional client progress");
      if (progress.size() > 1)
        check(
            value > progress.get(progress.size() - 2), "Client piston froze between native ticks");
    }
    if (t == 80) shot("animated-piston-mid-motion");
    if (t == 90) AugmentClientReview.press(SandevistanClient.KEY);
    if (t == 110) {
      check(!Sandevistan.active(mc.player), "Early-off input was not acknowledged");
      offAt = atlasTick();
      shot("animated-blocks-after-off");
    }
    if (t > 350) throw new IllegalStateException("Atlas clock did not advance through restoration");
    if (t >= 195 && offAt >= 0 && atlasTick() - offAt >= 105) {
      check(BlockAnimations.LATTICE.clocks.isEmpty(), "Visual clocks did not fade/prune after off");
      shot("animated-blocks-restored");
      Files.writeString(
          OUT.resolve("animations-native-results.json"),
          new com.google.gson.GsonBuilder()
              .setPrettyPrinting()
              .create()
              .toJson(
                  Map.of(
                      "pixelCases",
                      pixels,
                      "pistonNativeTickProgress",
                      progress,
                      "animatedSprites",
                      sprites,
                      "gpuTextureBytes",
                      bytes,
                      "shaderUses",
                      uses,
                      "clockCacheAfterOff",
                      BlockAnimations.LATTICE.clocks.size(),
                      "spriteNames",
                      BlockAnimations.spriteNames())));
      Files.writeString(
          OUT.resolve("animations-passed.txt"),
          "PASS: native water/lava/fire/portal/magma shader pixels match independently selected"
              + " local frames; outside-field pixels unchanged; client piston advances each real"
              + " tick; bounded frame/clock storage; visual clocks fade and prune after off.\n");
      finished = true;
      mc.stop();
    }
  }
}
