package dev.cybernetics2077.client.temporal;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.client.mantis.CyberwareClient;
import dev.cybernetics2077.combat.CyberwareNetwork;
import dev.cybernetics2077.temporal.*;
import net.minecraft.client.*;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.sounds.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Cybernetics2077.ID, value = Dist.CLIENT)
public final class SandevistanClient {
  public static final KeyMapping KEY =
      new KeyMapping(
          "key.cybernetics2077.sandevistan",
          net.neoforged.neoforge.client.settings.KeyConflictContext.IN_GAME,
          com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
          GLFW.GLFW_KEY_O,
          "key.categories.cybernetics2077");
  public static volatile float intensity;
  private static long frameTime, activation;
  private static boolean wasActive;
  private static Object level;
  private static Hum hum;

  public static Sandevistan.View view(net.minecraft.world.entity.player.Player p) {
    var t = CyberwareClient.state(p.getId());
    return new Sandevistan.View(
        t.getInt("sandyTier"),
        t.getLong("sandyEnd"),
        t.contains("temporalPace") ? Math.clamp(t.getDouble("temporalPace"), .05, 1.25) : 1,
        t.getBoolean("sandyPvp"),
        t.getBoolean("sandyIconic"));
  }

  public static void frame() {
    var m = Minecraft.getInstance();
    long now = System.nanoTime();
    double delta = frameTime == 0 ? 0 : Math.min(.1, (now - frameTime) / 1e9);
    frameTime = now;
    boolean active = m.player != null && m.level != null && Sandevistan.active(m.player);
    if (active != wasActive) {
      if (active) {
        activation = now;
        sound("sandy_on", .55f);
        if (hum != null) m.getSoundManager().stop(hum);
        hum = new Hum();
        m.getSoundManager().play(hum);
      } else sound("sandy_off", .4f);
      wasActive = active;
    }
    intensity +=
        (float) (((active ? 1 : 0) - intensity) * (1 - Math.exp(-delta * (active ? 12 : 9))));
    if (!active && intensity < .002) intensity = 0;
  }

  public static float pulse() {
    return (float) Math.exp(-Math.max(0, System.nanoTime() - activation) / 1e9 * 6);
  }

  public static float soundFactor(SoundInstance sound, boolean pitch) {
    if (sound.isRelative()
        || sound.getSource() == SoundSource.MUSIC
        || sound.getSource() == SoundSource.RECORDS
        || sound.getSource() == SoundSource.PLAYERS
        || sound.getSource() == SoundSource.VOICE) return 1;
    return 1 - intensity * (pitch ? .22f : .25f);
  }

  private static void sound(String name, float volume) {
    var m = Minecraft.getInstance();
    if (m.player != null)
      m.getSoundManager()
          .play(
              SimpleSoundInstance.forUI(
                  SoundEvent.createVariableRangeEvent(Cybernetics2077.id(name)), 1, volume));
  }

  private static final class Hum extends AbstractTickableSoundInstance {
    Hum() {
      super(
          SoundEvent.createVariableRangeEvent(Cybernetics2077.id("sandy_hum")),
          SoundSource.PLAYERS,
          SoundInstance.createUnseededRandom());
      looping = true;
      relative = true;
      attenuation = Attenuation.NONE;
      volume = .11f;
    }

    @Override
    public void tick() {
      if (Minecraft.getInstance().player == null || !wasActive && intensity == 0) {
        stop();
        return;
      }
      volume = .11f * intensity;
    }
  }

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post e) {
    var m = Minecraft.getInstance();
    if (level != m.level) {
      level = m.level;
      intensity = 0;
      frameTime = 0;
      wasActive = false;
      SandevistanTrails.clear();
      BlockAnimations.release();
      if (hum != null) m.getSoundManager().stop(hum);
      hum = null;
      SandevistanLens.release();
    }
    while (KEY.consumeClick())
      if (m.player != null
          && m.screen == null
          && !dev.cybernetics2077.client.ui.ScannerClient.active())
        PacketDistributor.sendToServer(new CyberwareNetwork.Action(13));
  }

  @SubscribeEvent
  public static void stage(RenderLevelStageEvent e) {
    if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS)
      SandevistanTrails.render(e);
  }

  @SubscribeEvent
  public static void name(RenderNameTagEvent e) {
    if (SandevistanTrails.capturing)
      e.setCanRender(net.neoforged.neoforge.common.util.TriState.FALSE);
  }

  @SubscribeEvent
  public static void fov(ViewportEvent.ComputeFov e) {
    if (Minecraft.getInstance().screen == null) e.setFOV(e.getFOV() * (1 + intensity * .035));
  }

  @EventBusSubscriber(
      modid = Cybernetics2077.ID,
      value = Dist.CLIENT,
      bus = EventBusSubscriber.Bus.MOD)
  public static final class Setup {
    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent e) {
      e.register(KEY);
    }

    @SubscribeEvent
    public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e) {
      e.enqueueWork(() -> Sandevistan.client = SandevistanClient::view);
    }

    @SubscribeEvent
    public static void shaders(RegisterShadersEvent e) throws java.io.IOException {
      AnimationShaders.register(e);
      TemporalMaterial.register(e);
      SandevistanLens.register(e);
    }
  }

  private SandevistanClient() {}
}
