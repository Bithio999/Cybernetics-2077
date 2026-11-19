package dev.cybernetics2077.review;

import static dev.cybernetics2077.review.ContentClientReview.check;
import static dev.cybernetics2077.review.CyberwareClientReview.*;

import dev.cybernetics2077.client.temporal.*;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.temporal.*;
import java.nio.file.Files;
import java.util.Set;
import net.minecraft.client.*;

/**
 * Regression scenes for stationary overlap, a camera inside a saved head, reversal and view
 * changes.
 */
final class TrailClientReview {
  static int maxEchoes, maxVertices, firstPersonSuppressed, exclusions;
  static long previousFrame;
  static final java.util.List<Double> frames = new java.util.ArrayList<>();

  static void frame() {
    int t = contentTicks;
    if (t < 485 || t > 590) {
      previousFrame = 0;
      return;
    }
    long now = System.nanoTime();
    if (previousFrame != 0) frames.add((now - previousFrame) / 1e6);
    previousFrame = now;
  }

  static void charge(net.minecraft.server.level.ServerPlayer p) {
    Sandevistan.stop(p, AugmentRuntime.now(p));
    var s = AugmentRuntime.state(p);
    s.sandyCharge = 1;
    s.sandyClock = AugmentRuntime.now(p);
    s.sandyRearm = 0;
  }

  static void tick(int t) throws Exception {
    var m = Minecraft.getInstance();
    if (t == 325) {
      m.options.keyUp.setDown(false);
      m.options.keyRight.setDown(false);
      m.options.keyLeft.setDown(false);
      m.options.setCameraType(CameraType.FIRST_PERSON);
      server(
          p -> {
            charge(p);
            p.teleportTo(p.serverLevel(), .5, 64, 8, Set.of(), 0, 0);
          });
    }
    if (t == 335) AugmentClientReview.press(SandevistanClient.KEY);
    if (t == 345) {
      check(
          SandevistanTrails.lastSelfSuppressed > 0,
          "Local body history was not excluded from first person");
      shot("trails-stationary-first-person");
    }
    if (t >= 350 && t <= 390 && t % 10 == 0) ContentClientReview.attack();
    if (t == 367) shot("trails-first-person-attack");
    if (t == 395) m.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
    if (t == 410) {
      check(
          SandevistanTrails.lastBodyExclusions > 0,
          "Live body volume was not excluded from its ghosts");
      shot("trails-stationary-third-person");
    }
    if (t == 415)
      server(
          p -> {
            var actor = SandevistanClientReview.actor;
            charge(actor);
            actor.setPos(.5, 64, 11);
            actor.setYRot(0);
            actor.yBodyRot = 0;
            actor.yHeadRot = 0;
            AugmentRuntime.action(actor, 13);
          });
    if (t == 425)
      server(
          p -> {
            var actor = SandevistanClientReview.actor;
            actor.setPos(4.5, 64, 11);
            dev.cybernetics2077.combat.CyberwareRuntime.broadcast(actor);
          });
    if (t == 428) {
      m.options.setCameraType(CameraType.FIRST_PERSON);
      server(p -> p.teleportTo(p.serverLevel(), .5, 64, 11, Set.of(), 0, 0));
    }
    if (t == 433) shot("trails-camera-inside-ghost");
    if (t == 435) SandevistanTrails.clear();
    if (t == 437) shot("trails-camera-cleared");
    if (t == 438) AugmentClientReview.press(SandevistanClient.KEY);
    if (t == 452) server(TrailClientReview::charge);
    if (t == 460) AugmentClientReview.press(SandevistanClient.KEY);
    if (t == 465) {
      m.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
      m.options.keyLeft.setDown(true);
      shot("trails-long-start");
    }
    if (t == 505) shot("trails-long-spectrum");
    if (t == 520) {
      m.options.keyLeft.setDown(false);
      m.options.keyRight.setDown(true);
    }
    if (t == 542) shot("trails-reverse-overlap");
    if (t == 545) m.options.setCameraType(CameraType.THIRD_PERSON_BACK);
    if (t == 565) {
      shot("trails-long-back");
      m.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
    }
    if (t == 585) shot("trails-long-front");
    if (t == 595) m.options.setCameraType(CameraType.FIRST_PERSON);
    if (t == 600) {
      check(
          SandevistanTrails.lastSelfSuppressed > 0, "View switch exposed local full-body history");
      shot("trails-first-person-return");
    }
    if (t == 620) {
      m.options.keyRight.setDown(false);
      AugmentClientReview.press(SandevistanClient.KEY);
      shot("trails-preview-end");
    }
    if (t >= 325) {
      maxEchoes = Math.max(maxEchoes, SandevistanTrails.lastEchoes);
      maxVertices = Math.max(maxVertices, SandevistanTrails.lastVertices);
      firstPersonSuppressed = Math.max(firstPersonSuppressed, SandevistanTrails.lastSelfSuppressed);
      exclusions = Math.max(exclusions, SandevistanTrails.lastBodyExclusions);
      check(
          SandevistanTrails.lastEchoes <= TrailStyle.WORLD_DRAW_LIMIT,
          "World trail draw budget exceeded");
    }
    if (t == 635) {
      check(
          maxEchoes >= 10 && firstPersonSuppressed > 0 && exclusions > 0,
          "Long trail or clipping checks did not run");
      check(!frames.isEmpty(), "No active trail frames sampled");
      frames.sort(Double::compare);
      Files.writeString(
          OUT.resolve("trails-frame-times.json"),
          new com.google.gson.GsonBuilder()
              .setPrettyPrinting()
              .create()
              .toJson(
                  java.util.Map.of(
                      "frames",
                      frames.size(),
                      "medianMs",
                      frames.get(frames.size() / 2),
                      "p95Ms",
                      frames.get((int) (frames.size() * .95)))));
      Files.writeString(
          OUT.resolve("trails-passed.txt"),
          "PASS: longer spectrum and bounded GPU histories; stationary first-person exclusion;"
              + " stationary third-person body clipping; camera inside saved head; reversal across"
              + " own history; return to first person; opaque hands/player render after echoes."
              + " Maximum echoes="
              + maxEchoes
              + ", vertices="
              + maxVertices
              + ", clipped="
              + exclusions
              + ".\n");
      if (Boolean.getBoolean("cybernetics2077.recording")) {
        Files.writeString(
            OUT.resolve("trails-recording-marks.json"),
            new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(recordingMarks));
      }
      server(
          p -> {
            var actor = SandevistanClientReview.actor;
            if (actor != null) {
              Sandevistan.stop(actor, AugmentRuntime.now(actor));
              actor.discard();
              SandevistanClientReview.actor = null;
            }
          });
      finished = true;
      m.stop();
    }
  }
}
