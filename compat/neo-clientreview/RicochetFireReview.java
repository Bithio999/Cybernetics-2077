package dev.cybernetics2077.compat.review;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.combat.*;
import dev.cybernetics2077.compat.TacZCompat;
import dev.cybernetics2077.cyberware.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Actual client trigger -> native C2S fire -> spawned bullets and normal world ticks. */
@EventBusSubscriber(modid = "cybernetics2077", value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class RicochetFireReview {
  private static final List<String> TRACE = new CopyOnWriteArrayList<>();
  private static volatile Throwable failure;
  private static volatile int target;
  private static String gun = "ak47";
  private static Path output;
  private static final Map<UUID, Integer> clientIds = new HashMap<>();
  private static final List<dev.cybernetics2077.combat.CyberwareNetwork.Rebound> segments =
      new CopyOnWriteArrayList<>();
  private static final Set<String> captured = new HashSet<>();
  private static UUID lastTrace;
  private static org.joml.Matrix4f view, projection;
  private static net.minecraft.world.phys.Vec3 camera;

  private static void pixels() {
    var mc = Minecraft.getInstance();
    var p = segments.get(segments.size() - 1);
    var world =
        new net.minecraft.world.phys.Vec3(p.x(), p.y(), p.z())
            .lerp(new net.minecraft.world.phys.Vec3(p.tx(), p.ty(), p.tz()), .4)
            .subtract(camera);
    var clip =
        new org.joml.Vector4f((float) world.x, (float) world.y, (float) world.z, 1)
            .mul(view)
            .mul(projection);
    try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
      int x = Math.round((clip.x / clip.w + 1) * image.getWidth() / 2),
          y = Math.round((1 - clip.y / clip.w) * image.getHeight() / 2);
      boolean visible = false;
      for (int yy = Math.max(0, y - 4); yy <= Math.min(image.getHeight() - 1, y + 4); yy++)
        for (int xx = Math.max(0, x - 4); xx <= Math.min(image.getWidth() - 1, x + 4); xx++) {
          int color = image.getPixelRGBA(xx, yy),
              r = color & 255,
              g = color >> 8 & 255,
              b = color >> 16 & 255;
          visible |= g - r >= 7 && b - r >= 4 && g > 85;
        }
      trace("CLIENT reflected pixels " + gun + " at " + x + "," + y + " visible=" + visible);
      if (!visible)
        throw new IllegalStateException(
            "Reflected segment missing in actual framebuffer: " + gun + " at " + x + "," + y);
    }
  }

  @SubscribeEvent
  public static void hud(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event) {
    var mc = Minecraft.getInstance();
    if (output == null || lastTrace == null || captured.contains(gun)) return;
    var id = clientIds.get(lastTrace);
    if (id != null
        && mc.level != null
        && mc.level.getEntity(id) == null
        && dev.cybernetics2077.client.ReboundTrailClient.covers(lastTrace)) {
      Screenshot.grab(
          output.toFile(), gun + "-bounce-return.png", mc.getMainRenderTarget(), m -> {});
      pixels();
      captured.add(gun);
      trace("CLIENT reflected trace rendered after native round removal: " + gun);
    }
  }

  private static final Map<Integer, Long> rendered = new HashMap<>();

  @SubscribeEvent
  public static void render(net.neoforged.neoforge.client.event.RenderLevelStageEvent e) {
    var mc = Minecraft.getInstance();
    if (!Boolean.getBoolean("cybernetics2077.ricochetReview")
        || mc.level == null
        || e.getStage()
            != net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_ENTITIES)
      return;
    view = new org.joml.Matrix4f(e.getModelViewMatrix());
    projection = new org.joml.Matrix4f(e.getProjectionMatrix());
    camera = e.getCamera().getPosition();
    for (var b : mc.level.entitiesForRendering())
      if (TacZCompat.bullet(b)
          && !java.util.Objects.equals(
              rendered.put(b.getId(), mc.level.getGameTime()), mc.level.getGameTime()))
        trace(
            "CLIENT render id="
                + b.getId()
                + " tick="
                + mc.level.getGameTime()
                + " managed="
                + dev.cybernetics2077.client.GuidedBulletClient.managed(b)
                + " pos="
                + b.position()
                + " velocity="
                + b.getDeltaMovement());
  }

  private static Object call(Object o, String name, Object... args) throws Exception {
    return Arrays.stream(o.getClass().getMethods())
        .filter(m -> m.getName().equals(name) && m.getParameterCount() == args.length)
        .findFirst()
        .orElseThrow()
        .invoke(o, args);
  }

  private static void trace(String s) {
    TRACE.add(s);
    System.out.println("RICOCHET_REVIEW " + s);
  }

  private static void server(Consumer<ServerPlayer> action) {
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

  @SubscribeEvent
  public static void joined(EntityJoinLevelEvent e) {
    if (!Boolean.getBoolean("cybernetics2077.ricochetReview") || !TacZCompat.bullet(e.getEntity()))
      return;
    var b = (Projectile) e.getEntity();
    if (e.getLevel().isClientSide) clientIds.put(b.getUUID(), b.getId());
    trace(
        (e.getLevel().isClientSide ? "CLIENT" : "SERVER")
            + " spawn id="
            + b.getId()
            + " owner="
            + b.getOwner()
            + " speed="
            + b.getDeltaMovement()
            + " pos="
            + b.position()
            + " tier="
            + (b.getOwner() instanceof ServerPlayer p
                ? AugmentRuntime.tier(p, Augment.BALLISTIC_COPROCESSOR)
                : -1));
  }

  @SubscribeEvent
  public static void tick(EntityTickEvent.Post e) {
    if (!Boolean.getBoolean("cybernetics2077.ricochetReview") || !TacZCompat.bullet(e.getEntity()))
      return;
    var b = e.getEntity();
    if (!b.level().isClientSide)
      trace(
          "SERVER step id="
              + b.getId()
              + " tick="
              + b.tickCount
              + " alive="
              + b.isAlive()
              + " speed="
              + b.getDeltaMovement()
              + " pos="
              + b.position()
              + " data="
              + b.getPersistentData().getCompound(TacZCompat.DATA));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static void listen() throws Exception {
    Class type = Class.forName("com.tacz.guns.api.event.server.AmmoHitBlockEvent");
    NeoForge.EVENT_BUS.addListener(
        EventPriority.LOWEST,
        true,
        type,
        (Consumer)
            (Consumer<Object>)
                event -> {
                  try {
                    var b = (Projectile) call(event, "getAmmo");
                    trace(
                        "IMPACT id="
                            + b.getId()
                            + " canceled="
                            + ((ICancellableEvent) event).isCanceled()
                            + " hit="
                            + call(event, "getHitResult")
                            + " data="
                            + b.getPersistentData().getCompound(TacZCompat.DATA));
                  } catch (Exception ex) {
                    failure = ex;
                  }
                });
  }

  private static void equipGun(ServerPlayer p, String id) {
    try {
      var builder =
          Class.forName("com.tacz.guns.api.item.builder.GunItemBuilder")
              .getMethod("create")
              .invoke(null);
      call(builder, "setId", ResourceLocation.parse("tacz:" + id));
      call(builder, "setAmmoCount", id.equals("ak47") ? 30 : 7);
      call(builder, "setAmmoInBarrel", true);
      call(
          builder,
          "setFireMode",
          Class.forName("com.tacz.guns.api.item.gun.FireMode").getField("SEMI").get(null));
      var stack = (ItemStack) call(builder, "build", p.registryAccess());
      p.getInventory().setItem(0, stack);
      p.getInventory().selected = 0;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private static void aim() throws Exception {
    var mc = Minecraft.getInstance();
    mc.player.setYRot(-30);
    mc.player.setXRot(0);
    ((KeyMapping) Class.forName("com.tacz.guns.client.input.AimKey").getField("AIM_KEY").get(null))
        .setDown(true);
    var op =
        Class.forName("com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator")
            .getMethod("fromLocalPlayer", net.minecraft.client.player.LocalPlayer.class)
            .invoke(null, mc.player);
    call(op, "aim", true);
  }

  private static void fire() throws Exception {
    var mc = Minecraft.getInstance();
    mc.player.setYRot(-30);
    mc.player.setXRot(0);
    var op =
        Class.forName("com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator")
            .getMethod("fromLocalPlayer", net.minecraft.client.player.LocalPlayer.class)
            .invoke(null, mc.player);
    trace("CLIENT trigger " + gun + " result=" + call(op, "shoot"));
    ((KeyMapping) Class.forName("com.tacz.guns.client.input.AimKey").getField("AIM_KEY").get(null))
        .setDown(false);
    call(op, "aim", false);
  }

  public static void tick(int tick, Path out) throws Exception {
    var mc = Minecraft.getInstance();
    if (failure != null) throw new IllegalStateException("Fixture server failure", failure);
    if (tick == 0) {
      output = out;
      Files.createDirectories(out);
      listen();
      var receive = CyberwareNetwork.rebound;
      CyberwareNetwork.rebound =
          p -> {
            receive.accept(p);
            if (new net.minecraft.world.phys.Vec3(p.x(), p.y(), p.z())
                    .distanceToSqr(new net.minecraft.world.phys.Vec3(p.tx(), p.ty(), p.tz()))
                > .01) {
              segments.add(p);
              lastTrace = p.uuid();
              trace("CLIENT reflected segment=" + p);
            }
          };
      mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
      mc.getToasts().clear();
      mc.options.pauseOnLostFocus = false;
      server(
          p -> {
            p.teleportTo(0, 90, 0);
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            p.setHealth(p.getMaxHealth());
            p.getFoodData().setFoodLevel(20);
            p.getInventory().clearContent();
            for (int i = 0; i < 21; i++)
              p.getData(Cybernetics2077.CYBERWARE).setStackInSlot(i, ItemStack.EMPTY);
            p.getData(Cybernetics2077.CYBERWARE)
                .setStackInSlot(
                    CyberwareCategory.HANDS.firstSlot(),
                    new ItemStack(Cybernetics2077.augment(Augment.BALLISTIC_COPROCESSOR, 5)));
            for (var mob :
                p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(70)))
              mob.discard();
            for (int x = -10; x <= 45; x++)
              for (int z = -25; z <= 40; z++) {
                p.serverLevel()
                    .setBlock(new BlockPos(x, 89, z), Blocks.STONE.defaultBlockState(), 3);
                for (int y = 90; y <= 96; y++)
                  p.serverLevel()
                      .setBlock(
                          new BlockPos(x, y, z),
                          z == 10
                              ? Blocks.STONE.defaultBlockState()
                              : Blocks.AIR.defaultBlockState(),
                          3);
              }
            var mob = EntityType.HUSK.create(p.serverLevel());
            mob.moveTo(10, 90, 2.67, 0, 0);
            mob.setNoAi(true);
            mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
                .setBaseValue(1000);
            mob.setHealth(1000);
            p.serverLevel().addFreshEntity(mob);
            target = mob.getId();
            equipGun(p, "ak47");
            dev.cybernetics2077.scanner.ScannerNetwork.sync(p);
            CyberwareRuntime.broadcast(p);
          });
    }
    if (tick == 35 || tick == 60 || tick == 85 || tick == 145 || tick == 170 || tick == 195) aim();
    if (tick == 50 || tick == 75 || tick == 100 || tick == 160 || tick == 185 || tick == 210)
      fire();
    if (tick == 120) {
      gun = "deagle";
      server(
          p -> {
            trace(
                "AKM target health="
                    + ((LivingEntity) p.serverLevel().getEntity(target)).getHealth());
            if (((LivingEntity) p.serverLevel().getEntity(target)).getHealth() >= 1000)
              throw new IllegalStateException("AKM failed reflected damage");
            try {
              if (((Number)
                          call(
                              p.getMainHandItem().getItem(),
                              "getCurrentAmmoCount",
                              p.getMainHandItem()))
                      .intValue()
                  != 27) throw new IllegalStateException("AKM ammo not consumed by native trigger");
            } catch (Exception ex) {
              throw new RuntimeException(ex);
            }
            p.getData(Cybernetics2077.CYBERWARE)
                .setStackInSlot(
                    CyberwareCategory.HANDS.firstSlot(),
                    new ItemStack(Cybernetics2077.augment(Augment.BALLISTIC_COPROCESSOR, 1)));
            dev.cybernetics2077.scanner.ScannerNetwork.sync(p);
            CyberwareRuntime.broadcast(p);
            equipGun(p, "deagle");
          });
    }
    if (tick == 240) {
      server(
          p -> {
            trace(
                "Deagle target health="
                    + ((LivingEntity) p.serverLevel().getEntity(target)).getHealth());
            try {
              if (((Number)
                          call(
                              p.getMainHandItem().getItem(),
                              "getCurrentAmmoCount",
                              p.getMainHandItem()))
                      .intValue()
                  != 4)
                throw new IllegalStateException("Deagle ammo not consumed by native trigger");
            } catch (Exception ex) {
              throw new RuntimeException(ex);
            }
          });
    }
    if (tick == 260) {
      if (segments.size() != 6 || !captured.containsAll(List.of("ak47", "deagle")))
        throw new IllegalStateException(
            "Missing reflected-path packets or post-removal frames: segments="
                + segments.size()
                + " captured="
                + captured);
      for (var p : segments) {
        if (p.z() >= 10 || p.tz() >= p.z() || p.tx() <= p.x() || p.tx() > 10.5)
          throw new IllegalStateException(
              "Visual reflection crosses wall or overshoots actual target: " + p);
      }
      Files.writeString(out.resolve("trace.txt"), String.join("\n", TRACE));
      Files.writeString(
          out.resolve("passed.txt"),
          "PASS: six real AKM/Desert Eagle client trigger pulls; native ammunition consumed; actual"
              + " reflected damage; six exact server flight segments; both guns rendered a"
              + " reflected trace after native entity removal.\n");
      mc.stop();
    }
  }
}
