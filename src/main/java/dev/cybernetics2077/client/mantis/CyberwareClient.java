package dev.cybernetics2077.client.mantis;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.client.ui.ScannerClient;
import dev.cybernetics2077.combat.*;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Cybernetics2077.ID, value = Dist.CLIENT)
public final class CyberwareClient {
  public static final KeyMapping DRAW =
      new KeyMapping(
          "key.cybernetics2077.mantis",
          net.neoforged.neoforge.client.settings.KeyConflictContext.IN_GAME,
          com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
          GLFW.GLFW_KEY_G,
          "key.categories.cybernetics2077");
  private static final Map<Integer, CompoundTag> STATES = new HashMap<>();
  public static final Map<Integer, Long> IMPACTS = new HashMap<>();
  private static Object level;
  private static boolean chargeHeld;

  public static void receive(CompoundTag t) {
    STATES.put(t.getInt("entity"), t);
  }

  public static CompoundTag own() {
    var m = Minecraft.getInstance();
    return m.player == null
        ? new CompoundTag()
        : STATES.getOrDefault(m.player.getId(), new CompoundTag());
  }

  public static CompoundTag state(int id) {
    return STATES.getOrDefault(id, new CompoundTag());
  }

  public static boolean active() {
    return own().getBoolean("deployed");
  }

  public static boolean visible(AbstractClientPlayer p) {
    var t = state(p.getId());
    return t.getBoolean("claws")
        && (t.getBoolean("deployed")
            || p.level().getGameTime() - t.getLong("changed") < CombatRules.DEPLOY);
  }

  public static MantisMotion.Pose pose(AbstractClientPlayer p, boolean left, float partial) {
    var t = state(p.getId());
    long now = p.level().getGameTime();
    return MantisMotion.sample(
        t.getBoolean("deployed"),
        now - t.getLong("changed") + partial,
        now - t.getLong("attack") + partial,
        t.getInt("combo"),
        t.getLong("charge") >= 0,
        now - t.getLong("charge") + partial,
        p.getMainArm() == HumanoidArm.LEFT ? !left : left);
  }

  private static long lungeId = Long.MIN_VALUE;
  private static int lungeAge;
  private static boolean lungeBlocked;

  /**
   * Used by the local player's travel hook, so ground friction and gravity cannot swallow a leap.
   */
  public static boolean moveLunge(net.minecraft.world.entity.player.Player player) {
    var t = own().getCompound("lunge");
    if (!active() || !t.contains("started") || !player.isAlive() || player.isPassenger())
      return false;
    long id = t.getLong("started");
    if (id != lungeId) {
      lungeId = id;
      lungeAge = 0;
      lungeBlocked = false;
    }
    if (lungeBlocked) return false;
    var path =
        new LungePath(
            t.getDouble("sx"),
            t.getDouble("sy"),
            t.getDouble("sz"),
            t.getDouble("ex"),
            t.getDouble("ey"),
            t.getDouble("ez"),
            t.getDouble("arc"),
            t.getInt("ticks"));
    if (lungeAge >= path.ticks()) {
      player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
      return false;
    }
    var step = path.step(player.getX(), player.getY(), player.getZ(), lungeAge++);
    var before = player.position();
    player.setDeltaMovement(step.x(), step.y(), step.z());
    player.move(net.minecraft.world.entity.MoverType.SELF, player.getDeltaMovement());
    player.resetFallDistance();
    if (player.position().distanceToSqr(before) < .0004 || player.horizontalCollision) {
      lungeBlocked = true;
      player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
    }
    return true;
  }

  public static void attack() {
    if (!ScannerClient.active()) PacketDistributor.sendToServer(new CyberwareNetwork.Action(1));
  }

  public static void charge() {
    if (!ScannerClient.active() && !chargeHeld) {
      chargeHeld = true;
      PacketDistributor.sendToServer(new CyberwareNetwork.Action(2));
    }
  }

  @EventBusSubscriber(
      modid = Cybernetics2077.ID,
      value = Dist.CLIENT,
      bus = EventBusSubscriber.Bus.MOD)
  public static final class Setup {
    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent e) {
      e.register(DRAW);
    }

    @SubscribeEvent
    public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e) {
      e.enqueueWork(
          () -> {
            CyberwareNetwork.receiver = CyberwareClient::receive;
            CyberwareNetwork.impact =
                id -> {
                  var m = Minecraft.getInstance();
                  if (m.level != null) IMPACTS.put(id, m.level.getGameTime());
                };
          });
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.AddLayers e) {
      for (var skin : e.getSkins()) {
        net.minecraft.client.renderer.entity.player.PlayerRenderer r = e.getSkin(skin);
        r.addLayer(new MantisLayer(r));
      }
    }
  }

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post e) {
    var m = Minecraft.getInstance();
    if (level != m.level) {
      if (m.level == null) {
        STATES.clear();
        IMPACTS.clear();
      } else if (level != null) {
        // A dimension-sync payload may precede the first tick in its new level. Retain the
        // local player's equipment snapshot; only discard remote entities from the old level.
        int ownId = m.player == null ? -1 : m.player.getId();
        STATES.keySet().removeIf(id -> id != ownId);
        IMPACTS.clear();
      }
      level = m.level;
      chargeHeld = false;
      lungeId = Long.MIN_VALUE;
    }
    while (DRAW.consumeClick())
      if (m.player != null && m.screen == null && !ScannerClient.active())
        PacketDistributor.sendToServer(new CyberwareNetwork.Action(0));
    if (chargeHeld && (!m.options.keyUse.isDown() || m.screen != null || !active())) {
      chargeHeld = false;
      if (m.player != null) PacketDistributor.sendToServer(new CyberwareNetwork.Action(3));
    }
    if (m.level != null)
      IMPACTS.entrySet().removeIf(x -> m.level.getGameTime() - x.getValue() > 14);
  }

  @SubscribeEvent
  public static void hand(RenderHandEvent e) {
    var m = Minecraft.getInstance();
    if (m.player == null || !visible(m.player)) return;
    e.setCanceled(true);
    if (e.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
    for (boolean left : new boolean[] {false, true}) {
      var pose = pose(m.player, left, e.getPartialTick());
      float sign = left ? -1 : 1;
      var p = e.getPoseStack();
      p.pushPose();
      // Fixed shoulder anchors extend off the bottom/sides of the view. Convert the canonical
      // player axes into camera axes before applying the SAME joint rig as third person.
      p.translate(sign * .46, -.50, -.72);
      p.scale(-.82f, -.82f, .82f);
      p.mulPose(
          MantisRig.shoulder(
              left,
              m.player.getSkin().model() == net.minecraft.client.resources.PlayerSkin.Model.SLIM,
              pose));
      MantisMesh.render(p, e.getMultiBufferSource(), e.getPackedLight(), m.player, left, pose);
      p.popPose();
    }
  }

  private CyberwareClient() {}
}
