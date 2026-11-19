package dev.cybernetics2077.client.ui;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.client.mantis.CyberwareClient;
import dev.cybernetics2077.combat.CyberwareNetwork;
import dev.cybernetics2077.cyberware.AugmentRuntime;
import net.minecraft.client.*;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Cybernetics2077.ID, value = Dist.CLIENT)
public final class AugmentClient {
  public static final KeyMapping BERSERK = key("berserk", GLFW.GLFW_KEY_I),
      PUMP = key("blood_pump", GLFW.GLFW_KEY_H),
      CAMO = key("optical_camo", GLFW.GLFW_KEY_C);
  private static boolean jumpDown, charging;
  private static Object level;

  private static KeyMapping key(String id, int code) {
    return new KeyMapping(
        "key.cybernetics2077." + id,
        net.neoforged.neoforge.client.settings.KeyConflictContext.IN_GAME,
        com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
        code,
        "key.categories.cybernetics2077");
  }

  public static void action(int id) {
    PacketDistributor.sendToServer(new CyberwareNetwork.Action(id));
  }

  public static boolean highlighted(Entity entity) {
    var m = Minecraft.getInstance();
    if (m.level == null || m.player == null || entity == null || !entity.isAlive()) return false;
    for (var tag : CyberwareClient.own().getList("highlights", 10)) {
      var mark = (net.minecraft.nbt.CompoundTag) tag;
      if (mark.getInt("id") == entity.getId() && mark.getLong("until") > m.level.getGameTime())
        return true;
    }
    return false;
  }

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post e) {
    var m = Minecraft.getInstance();
    if (level != m.level || m.screen != null) {
      charging = false;
      jumpDown = m.options.keyJump.isDown();
      level = m.level;
    }
    if (m.player == null || m.screen != null) return;
    while (BERSERK.consumeClick()) action(5);
    while (PUMP.consumeClick()) action(6);
    while (CAMO.consumeClick()) action(7);
  }

  @SubscribeEvent
  public static void movement(MovementInputUpdateEvent e) {
    var m = Minecraft.getInstance();
    if (e.getEntity() != m.player || m.player == null || m.screen != null) return;
    boolean down = m.options.keyJump.isDown();
    var data = CyberwareClient.own();
    if (data.getInt("legTier") > 0 && !m.player.getAbilities().flying && !m.player.isPassenger()) {
      if (data.getBoolean("ankles")) {
        if (down && m.player.onGround() && !m.player.isInWater() && !m.player.onClimbable()) {
          if (!charging) {
            charging = true;
            action(9);
          }
          e.getInput().jumping = false;
        } else if (charging && !down) {
          charging = false;
          action(12);
          e.getInput().jumping = false;
        } else if (!m.player.onGround()) charging = false;
      } else if (down
          && !jumpDown
          && !m.player.onGround()
          && !m.player.isInWater()
          && !m.player.onClimbable()) action(8);
    }
    jumpDown = down;
  }

  @SubscribeEvent
  public static void hud(RenderGuiEvent.Post e) {
    var m = Minecraft.getInstance();
    if (m.player == null
        || m.screen != null
        || m.options.hideGui
        || ScannerClient.hudMotion().lens() > 0) return;
    var s = CyberwareClient.own();
    long time = m.level.getGameTime();
    if (s.getInt("pumpTier") == 0
        && s.getInt("berserkTier") == 0
        && s.getInt("camoTier") == 0
        && s.getInt("iceTier") == 0
        && s.getInt("heartTier") == 0
        && s.getInt("sandyTier") == 0) return;
    var g = e.getGuiGraphics();
    CyberBatch.begin();
    try {
      var c = new CyberCanvas(g, 1);
      float y = g.guiHeight() - 72;
      if (s.getInt("berserkTier") > 0) {
        badge(
            c,
            5,
            y,
            BERSERK.getTranslatedKeyMessage().getString(),
            s.getBoolean("berserk")
                ? "BERSERK " + seconds(s.getLong("berserkEnd"), time)
                : ready(s.getLong("berserkReady"), time),
            s.getBoolean("berserk"));
        y -= 21;
      }
      if (s.getInt("pumpTier") > 0) {
        badge(
            c,
            5,
            y,
            PUMP.getTranslatedKeyMessage().getString(),
            "PUMP "
                + s.getInt("pumpCharges")
                + "/2"
                + (s.getInt("pumpCharges") < 2 ? "  " + seconds(s.getLong("pumpNext"), time) : ""),
            false);
        y -= 21;
      }
      if (s.getInt("camoTier") > 0) {
        badge(
            c,
            5,
            y,
            CAMO.getTranslatedKeyMessage().getString(),
            s.getBoolean("camo")
                ? "CLOAK " + seconds(s.getLong("camoEnd"), time)
                : "CAMO " + ready(s.getLong("camoReady"), time),
            s.getBoolean("camo"));
        y -= 21;
      }
      if (s.getInt("iceTier") > 0) {
        badge(c, 5, y, "ICE", "SELF-ICE " + ready(s.getLong("iceReady"), time), false);
        y -= 21;
      }
      if (s.getInt("heartTier") > 0) {
        badge(c, 5, y, "HP", "HEART " + ready(s.getLong("heartReady"), time), false);
        y -= 21;
      }
      if (s.getInt("sandyTier") > 0) {
        int profile = Math.clamp(s.getInt("sandyProfile"), 2, 5);
        boolean active = s.getLong("sandyEnd") > time;
        double charge =
            active
                ? (s.getLong("sandyEnd") - time)
                    / (20. * dev.cybernetics2077.temporal.TemporalRules.duration(profile))
                : s.getDouble("sandyCharge")
                    + Math.max(0, time - s.getLong("sandyClock"))
                        / (20. * dev.cybernetics2077.temporal.TemporalRules.recharge(profile));
        badge(
            c,
            5,
            y,
            dev.cybernetics2077.client.temporal.SandevistanClient.KEY
                .getTranslatedKeyMessage()
                .getString(),
            "SANDY "
                + (active
                    ? seconds(s.getLong("sandyEnd"), time)
                    : Math.round(Math.clamp(charge, 0, 1) * 100) + "%"),
            active);
        c.rect(9, y + 15, (float) (108 * Math.clamp(charge, 0, 1)), 1, 0xffa2f08a);
      }
    } finally {
      CyberBatch.flush(g);
    }
  }

  private static String seconds(long until, long time) {
    return Math.max(0, (until - time + 19) / 20) + "s";
  }

  private static String ready(long until, long time) {
    return until <= time ? "READY" : seconds(until, time);
  }

  private static void badge(
      CyberCanvas c, float x, float y, String key, String text, boolean active) {
    int color = active ? 0xffff6b75 : 0xff71dbe4;
    c.panel(x, y, 116, 18, 3, 0xcf09131d, color);
    c.text(key, x + 5, y + 4, 10, color);
    c.body(text, x + 23, y + 5, 8, color);
  }

  @EventBusSubscriber(
      modid = Cybernetics2077.ID,
      value = Dist.CLIENT,
      bus = EventBusSubscriber.Bus.MOD)
  public static final class Setup {
    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent e) {
      e.register(BERSERK);
      e.register(PUMP);
      e.register(CAMO);
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e) {
      e.enqueueWork(
          () -> {
            dev.cybernetics2077.cyberware.AugmentExpansion.clientLynx =
                p -> CyberwareClient.state(p.getId()).getInt("lynxTier");
            AugmentRuntime.clientCamo =
                p ->
                    CyberwareClient.state(p.getId()).getBoolean("camo")
                        && p.level().getGameTime()
                            < CyberwareClient.state(p.getId()).getLong("camoEnd");
            AugmentRuntime.clientBerserk =
                p ->
                    CyberwareClient.state(p.getId()).getBoolean("berserk")
                        && p.level().getGameTime()
                            < CyberwareClient.state(p.getId()).getLong("berserkEnd");
          });
    }
  }

  private AugmentClient() {}
}
