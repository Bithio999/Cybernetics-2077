package dev.cybernetics2077.review;

import static dev.cybernetics2077.review.ContentClientReview.check;
import static dev.cybernetics2077.review.CyberwareClientReview.*;

import dev.cybernetics2077.client.temporal.*;
import dev.cybernetics2077.cyberware.AugmentRuntime;
import dev.cybernetics2077.temporal.*;
import java.nio.file.Files;
import java.util.Set;
import net.minecraft.client.*;
import net.minecraft.world.level.GameRules;

final class BubbleClientReview {
  static int own;
  static long sun, time;

  static void tick(int t) throws Exception {
    int phase = t - 325;
    var m = Minecraft.getInstance();
    if (phase == 0) {
      m.options.keyUp.setDown(false);
      m.options.keyLeft.setDown(false);
      m.options.keyRight.setDown(false);
      m.options.setCameraType(CameraType.FIRST_PERSON);
      m.options.hideGui = false;
      TemporalWeather.slowedColumns = TemporalWeather.slowedParticles = 0;
      SandevistanTrails.clear();
      server(
          p -> {
            if (SandevistanClientReview.actor != null) {
              Sandevistan.stop(SandevistanClientReview.actor, AugmentRuntime.now(p));
              SandevistanClientReview.actor.discard();
              SandevistanClientReview.actor = null;
            }
            TrailClientReview.charge(p);
            p.teleportTo(p.serverLevel(), .5, 64, 8, Set.of(), 0, 0);
            p.serverLevel().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(true, p.server);
            p.serverLevel().setWeatherParameters(0, 6000, true, false);
            p.serverLevel().setRainLevel(1);
            sun = p.serverLevel().getDayTime();
            time = p.serverLevel().getGameTime();
          });
    }
    // Immediate fixture rain; the vanilla server weather packets still control the normal game.
    if (phase < 135) m.level.setRainLevel(1);
    if (phase == 10) AugmentClientReview.press(SandevistanClient.KEY);
    if (phase == 20) m.options.keyDown.setDown(true);
    if (phase >= 30 && phase <= 100) own = Math.max(own, SandevistanTrails.lastOwnEchoes);
    if (phase == 55) {
      check(own > 0, "Own body trail never became visible in first person");
      check(TemporalWeather.slowedColumns > 0, "Rain UV clock did not run");
      shot("bubble-first-person-trails-rain");
    }
    if (phase == 65) m.options.keyDown.setDown(false);
    if (phase == 75) {
      check(TemporalWeather.slowedParticles > 0, "Rain splashes were not time paced");
      shot("bubble-first-person-rain");
    }
    if (phase == 80) m.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
    if (phase == 95) shot("bubble-third-person-rain");
    if (phase == 105) {
      m.options.setCameraType(CameraType.FIRST_PERSON);
      AugmentClientReview.press(SandevistanClient.KEY);
    }
    if (phase == 120) {
      check(!Sandevistan.active(m.player), "Native bubble did not deactivate");
      check(TemporalWeather.lastPace == 1, "Rain retained slow pace after the bubble ended");
      shot("bubble-rain-normal-speed");
      server(
          p -> {
            long elapsed = p.serverLevel().getGameTime() - time;
            check(
                p.serverLevel().getDayTime() - sun == elapsed,
                "Sun did not keep normal server time");
          });
    }
    if (phase == 140) {
      Files.writeString(
          OUT.resolve("bubble-native-results.json"),
          new com.google.gson.GsonBuilder()
              .setPrettyPrinting()
              .create()
              .toJson(
                  java.util.Map.of(
                      "visibleOwnEchoes",
                      own,
                      "slowedRainColumns",
                      TemporalWeather.slowedColumns,
                      "slowedSplashTicks",
                      TemporalWeather.slowedParticles,
                      "finalRainPace",
                      TemporalWeather.lastPace,
                      "sunClock",
                      "normal")));
      Files.writeString(
          OUT.resolve("bubble-passed.txt"),
          "PASS: local rain UV phase, paced splashes, first-person own full-body echoes with"
              + " clearance, third-person rendering, normal pace after off, unchanged sun"
              + " clock.\n");
      finished = true;
      m.stop();
    }
  }
}
