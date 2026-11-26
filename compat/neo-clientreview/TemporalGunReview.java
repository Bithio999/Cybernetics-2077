package dev.cybernetics2077.compat.review;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.combat.*;
import dev.cybernetics2077.compat.TacZCompat;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.temporal.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Native trigger, packets, rendered frame positions and injected native animation clock. */
@EventBusSubscriber(modid = "cybernetics2077", value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class TemporalGunReview {
  private static final List<CyberwareNetwork.Flight> frames = new ArrayList<>();
  private static final List<String> trace = new ArrayList<>();
  private static final Set<Integer> rendered = new HashSet<>();
  private static volatile Throwable failure;
  private static boolean started;

  private static void server(
      java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> action) {
    var mc = Minecraft.getInstance();
    mc.getSingleplayerServer()
        .execute(
            () -> {
              try {
                action.accept(
                    mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()));
              } catch (Throwable t) {
                failure = t;
              }
            });
  }

  private static void equip(net.minecraft.server.level.ServerPlayer p, String gun) {
    try {
      var m =
          RicochetFireReview.class.getDeclaredMethod(
              "equipGun", net.minecraft.server.level.ServerPlayer.class, String.class);
      m.setAccessible(true);
      m.invoke(null, p, gun);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static void fire() throws Exception {
    var mc = Minecraft.getInstance();
    mc.player.setYRot(0);
    mc.player.setXRot(-3);
    var type = Class.forName("com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator");
    Object op =
        type.getMethod("fromLocalPlayer", net.minecraft.client.player.LocalPlayer.class)
            .invoke(null, mc.player);
    trace.add("Native trigger: " + type.getMethod("shoot").invoke(op));
  }

  private static void clock(String name, double expected) throws Exception {
    var animation = Class.forName("com.tacz.guns.api.client.animation.ObjectAnimation");
    var constructor = animation.getDeclaredConstructor(String.class);
    constructor.setAccessible(true);
    var runner = Class.forName("com.tacz.guns.api.client.animation.ObjectAnimationRunner");
    Object object = runner.getConstructor(animation).newInstance(constructor.newInstance(name));
    var time = Class.forName("com.tacz.guns.util.time.GunTime");
    var last = runner.getDeclaredField("lastUpdateNs");
    last.setAccessible(true);
    var elapsed = runner.getDeclaredMethod("elapsedSinceLastUpdate");
    elapsed.setAccessible(true);
    long now = ((Number) time.getMethod("nowNanos").invoke(null)).longValue();
    last.setLong(object, now - 1_000_000_000L);
    double seconds = ((Number) elapsed.invoke(object)).longValue() / 1e9;
    trace.add("Native clip " + name + ": " + seconds + " seconds / 1 real second");
    if (Math.abs(seconds - expected) > .025)
      throw new IllegalStateException("Incorrect native animation clock: " + name + " " + seconds);
  }

  @SubscribeEvent
  public static void render(RenderLevelStageEvent event) {
    var mc = Minecraft.getInstance();
    if (!started
        || mc.level == null
        || event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
    for (var b : mc.level.entitiesForRendering())
      if (TacZCompat.bullet(b) && dev.cybernetics2077.client.GuidedBulletClient.managed(b)) {
        var a = b.getPosition(.25f);
        var c = b.getPosition(.75f);
        if (a.distanceToSqr(c) > 1e-6) rendered.add(b.getId());
      }
  }

  public static void tick(int t, Path out) throws Exception {
    var mc = Minecraft.getInstance();
    if (failure != null) throw new IllegalStateException(failure);
    if (t == 0) {
      started = true;
      Files.createDirectories(out);
      var original = CyberwareNetwork.flight;
      CyberwareNetwork.flight =
          p -> {
            original.accept(p);
            frames.add(p);
          };
      server(
          p -> {
            p.teleportTo(0, 120, 0);
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            p.getInventory().clearContent();
            var inv = p.getData(Cybernetics2077.CYBERWARE);
            for (int i = 0; i < inv.getSlots(); i++) inv.setStackInSlot(i, ItemStack.EMPTY);
            inv.setStackInSlot(
                CyberwareCategory.OPERATING_SYSTEM.firstSlot(),
                new ItemStack(Cybernetics2077.augment(Augment.DAVIDS_SANDEVISTAN, 5)));
            var s = AugmentRuntime.state(p);
            s.sandyEnd = 0;
            s.sandyCharge = 1;
            s.sandyClock = AugmentRuntime.now(p);
            s.sandyRearm = 0;
            for (int x = -2; x <= 2; x++)
              for (int z = -2; z <= 70; z++) {
                p.serverLevel()
                    .setBlock(
                        new BlockPos(x, 119, z),
                        net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),
                        3);
                for (int y = 120; y <= 128; y++)
                  p.serverLevel()
                      .setBlock(
                          new BlockPos(x, y, z),
                          net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
                          3);
              }
            equip(p, "ak47");
            dev.cybernetics2077.scanner.ScannerNetwork.sync(p);
            CyberwareRuntime.broadcast(p);
          });
    }
    if (t == 25)
      net.neoforged.neoforge.network.PacketDistributor.sendToServer(
          new CyberwareNetwork.Action(13));
    if (t == 45) {
      if (!Sandevistan.active(mc.player)) throw new IllegalStateException("David not active");
      clock("shoot", .0625);
      clock("bolt", .0625);
      clock("reload_tactical", 1);
      clock("reload_empty", 1);
      clock("draw", 1);
      clock("static_idle", 1);
      fire();
    }
    if (t == 90) server(p -> equip(p, "deagle"));
    if (t == 120) fire();
    if (t == 165) {
      var byBullet = new HashMap<UUID, List<CyberwareNetwork.Flight>>();
      for (var p : frames) byBullet.computeIfAbsent(p.uuid(), k -> new ArrayList<>()).add(p);
      if (byBullet.size() != 2)
        throw new IllegalStateException("Expected two real rounds, got " + byBullet.size());
      for (var list : byBullet.values()) {
        var slow =
            list.stream().filter(p -> Math.abs(p.pace() - .0625) < 1e-7 && !p.ended()).toList();
        if (slow.size() < 8)
          throw new IllegalStateException("Insufficient continuous slowed flight: " + slow.size());
        for (int i = 1; i < slow.size(); i++) {
          var a = slow.get(i - 1);
          var b = slow.get(i);
          if (b.tick() != a.tick() + 1
              || Math.abs(b.age() - a.age() - .0625) > 1e-5
              || b.z() <= a.z())
            throw new IllegalStateException("Sparse or discontinuous native flight");
        }
        if (!rendered.contains(list.getFirst().entity()))
          throw new IllegalStateException(
              "No interpolation between actual rendered frame endpoints");
        trace.add(
            "Round "
                + list.getFirst().entity()
                + ": "
                + slow.size()
                + " consecutive fractional flight packets; rendered interpolation confirmed");
      }
      net.neoforged.neoforge.network.PacketDistributor.sendToServer(
          new CyberwareNetwork.Action(13));
    }
    if (t == 175) {
      clock("shoot", 1);
      clock("bolt", 1);
      Files.writeString(out.resolve("trace.txt"), String.join("\n", trace));
      Files.writeString(
          out.resolve("passed.txt"),
          "PASS: actual AKM + Desert Eagle triggers, continuous native fractional flight and client"
              + " frame interpolation; injected firing/bolt clock slowed, reload/handling normal,"
              + " original animation pace restored after stopping David.\n");
      mc.stop();
    }
  }
}
