package dev.cybernetics2077.review;

import static dev.cybernetics2077.review.CyberwareClientReview.*;

import dev.cybernetics2077.client.temporal.SandevistanTrails;
import dev.cybernetics2077.combat.CyberwareRuntime;
import dev.cybernetics2077.cyberware.AugmentRuntime;
import dev.cybernetics2077.temporal.Sandevistan;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.world.phys.Vec3;

/** Pixel regression: an isolated saved remote pose must remain visible from every camera angle. */
final class TrailCameraReview {
  static final float[][] ANGLES = {
    {0, 0}, {45, 0}, {90, 0}, {180, 0}, {270, 0}, {135, 25}, {0, 0}, {0, 0}
  };
  static final List<Map<String, Object>> results = new ArrayList<>();
  static int[] ghost;
  static Vec3 expected;
  static int width, height;

  static int[] pixels() {
    var m = Minecraft.getInstance();
    try (var im = Screenshot.takeScreenshot(m.getMainRenderTarget())) {
      width = im.getWidth();
      height = im.getHeight();
      int[] data = new int[width * height];
      for (int y = 0; y < height; y++)
        for (int x = 0; x < width; x++) data[y * width + x] = im.getPixelRGBA(x, y);
      return data;
    }
  }

  static void tick(int t) throws Exception {
    var m = Minecraft.getInstance();
    int elapsed = t - 325;
    int scene = elapsed / 36, phase = elapsed % 36;
    if (scene >= ANGLES.length) {
      Files.writeString(
          OUT.resolve("trail-camera-results.json"),
          new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(results));
      var failed =
          results.stream().filter(r -> ((Number) r.get("changedPixels")).intValue() < 80).toList();
      ContentClientReview.check(
          failed.isEmpty(), "Invisible/misplaced saved poses at camera angles: " + failed);
      Files.writeString(
          OUT.resolve("trail-camera-passed.txt"),
          "PASS: pixel-visible saved poses at six first-person yaw/pitch angles and front/back"
              + " third person.\n");
      finished = true;
      m.stop();
      return;
    }
    m.options.hideGui = true;
    m.options.cloudStatus().set(CloudStatus.OFF);
    m.player.setDeltaMovement(Vec3.ZERO);
    String name = "trail-camera-" + scene;
    if (phase == 0) {
      SandevistanTrails.clear();
      m.options.setCameraType(
          scene < 6
              ? CameraType.FIRST_PERSON
              : scene == 6 ? CameraType.THIRD_PERSON_FRONT : CameraType.THIRD_PERSON_BACK);
      server(
          p -> {
            var actor = SandevistanClientReview.actor;
            TrailClientReview.charge(actor);
            Sandevistan.stop(p, AugmentRuntime.now(p));
            p.setDeltaMovement(Vec3.ZERO);
            p.hurtMarked = true;
            p.getAbilities().mayfly = true;
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            p.teleportTo(p.serverLevel(), .5, 80, .5, Set.of(), ANGLES[scene][0], ANGLES[scene][1]);
            p.serverLevel().setDayTime(11000);
            p.serverLevel()
                .getGameRules()
                .getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT)
                .set(false, p.server);
            CyberwareRuntime.broadcast(actor);
          });
    }
    if (phase == 5) {
      var camera = m.gameRenderer.getMainCamera();
      var look = camera.getLookVector();
      Vec3 direction = new Vec3(look.x(), look.y(), look.z());
      Vec3 target = camera.getPosition().add(direction.scale(8));
      if (scene >= 6) target = target.add(direction.cross(new Vec3(0, 1, 0)).normalize().scale(2));
      Vec3 spot = target;
      expected = target.add(0, -1, 0);
      server(
          p -> {
            var actor = SandevistanClientReview.actor;
            actor.setPos(spot.x, spot.y - 1, spot.z);
            actor.setYRot(ANGLES[scene][0] + 180);
            actor.yBodyRot = actor.getYRot();
            actor.yHeadRot = actor.getYRot();
            AugmentRuntime.action(actor, 13);
            CyberwareRuntime.broadcast(actor);
          });
    }
    if (phase == 18) {
      var actor = m.level.getEntity(SandevistanClientReview.actorId);
      ContentClientReview.check(
          actor != null
              && actor.position().distanceToSqr(expected) < .25
              && actor instanceof net.minecraft.world.entity.player.Player player
              && Sandevistan.active(player),
          "Scripted actor did not finish tracking/interpolating before camera capture: " + scene);
    }
    if (phase == 19)
      server(
          p -> {
            var actor = SandevistanClientReview.actor;
            Sandevistan.stop(actor, AugmentRuntime.now(actor));
            actor.setPos(40, 80, 40);
            CyberwareRuntime.broadcast(actor);
          });
    if (phase == 25) {
      ghost = pixels();
      shot(name + "-ghost");
    }
    if (phase == 26) SandevistanTrails.clear();
    if (phase == 29) {
      int[] clean = pixels();
      int changed = 0;
      int centre = width / 2 + (scene >= 6 ? width / 10 : 0);
      // The known saved pose is centred here. Exclude held-arm/HUD animation and
      // disable moving clouds so framebuffer differences measure the ghost itself.
      for (int y = height / 3; y < height * 2 / 3; y++)
        for (int x = centre - width / 10; x < centre + width / 10; x++) {
          int a = ghost[y * width + x], b = clean[y * width + x], diff = 0;
          for (int shift = 0; shift <= 16; shift += 8)
            diff = Math.max(diff, Math.abs(((a >> shift) & 255) - ((b >> shift) & 255)));
          if (diff > 12) changed++;
        }
      results.add(
          Map.of(
              "scene",
              scene,
              "yaw",
              ANGLES[scene][0],
              "pitch",
              ANGLES[scene][1],
              "camera",
              m.options.getCameraType().name(),
              "changedPixels",
              changed));
      shot(name + "-clear");
    }
  }
}
