package dev.cybernetics2077.compat.review;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.client.mantis.CyberwareClient;
import dev.cybernetics2077.client.ui.*;
import dev.cybernetics2077.combat.*;
import dev.cybernetics2077.compat.TacZCompat;
import dev.cybernetics2077.cyberware.*;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.Arrays;
import net.minecraft.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;

/** Real gun/ADS/scanner captures after the normal integrated-client review. */
@net.neoforged.fml.common.EventBusSubscriber(
    modid = "cybernetics2077",
    value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class TacZHudReview {
  private static int hiddenCrosshairFrames, canceledCrosshairFrames;
  private static long normalInterval;
  private static float normalReload;

  @net.neoforged.bus.api.SubscribeEvent(
      priority = net.neoforged.bus.api.EventPriority.LOWEST,
      receiveCanceled = true)
  public static void crosshair(net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Pre event) {
    var p = Minecraft.getInstance().player;
    if (p != null
        && event.getName().equals(net.neoforged.neoforge.client.gui.VanillaGuiLayers.CROSSHAIR)
        && TacZCompat.gun(p.getMainHandItem())
        && ScannerClient.active()
        && NativeHudTransition.visible(NativeHudTransition.Group.CROSSHAIR) <= .00001) {
      hiddenCrosshairFrames++;
      if (event.isCanceled()) canceledCrosshairFrames++;
    }
  }

  private static Object call(Object target, String name, Object... args) throws Exception {
    Method m =
        Arrays.stream(target.getClass().getMethods())
            .filter(v -> v.getName().equals(name) && v.getParameterCount() == args.length)
            .findFirst()
            .orElseThrow();
    return m.invoke(target, args);
  }

  public static void tick(int tick, Path out) throws Exception {
    var mc = Minecraft.getInstance();
    if (tick == 0) {
      mc.options.pauseOnLostFocus = false;
      mc.getSingleplayerServer()
          .execute(
              () -> {
                try {
                  var p = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                  var inv = p.getData(Cybernetics2077.CYBERWARE);
                  inv.setStackInSlot(CyberwareCategory.ARMS.firstSlot(), ItemStack.EMPTY);
                  inv.setStackInSlot(
                      CyberwareCategory.OPERATING_SYSTEM.firstSlot(),
                      new ItemStack(Cybernetics2077.CYBERDECK.get()));
                  inv.setStackInSlot(
                      CyberwareCategory.HANDS.firstSlot(),
                      new ItemStack(Cybernetics2077.augment(Augment.SMART_LINK, 5)));
                  Object builder =
                      Class.forName("com.tacz.guns.api.item.builder.GunItemBuilder")
                          .getMethod("create")
                          .invoke(null);
                  call(builder, "setId", ResourceLocation.fromNamespaceAndPath("tacz", "ak47"));
                  call(builder, "setAmmoCount", 30);
                  call(builder, "setAmmoInBarrel", true);
                  Class<?> mode = Class.forName("com.tacz.guns.api.item.gun.FireMode");
                  call(builder, "setFireMode", mode.getField("AUTO").get(null));
                  ItemStack gun = (ItemStack) call(builder, "build", p.registryAccess());
                  if (gun.isEmpty()) throw new IllegalStateException("Native AK builder failed");
                  p.getInventory().selected = 0;
                  p.getInventory().setItem(0, gun);
                  p.setYRot(0);
                  p.setXRot(6);
                  var target =
                      p.serverLevel()
                          .getEntitiesOfClass(
                              net.minecraft.world.entity.monster.Husk.class,
                              p.getBoundingBox().inflate(12))
                          .getFirst();
                  HandsRuntime.lock(p, target);
                  dev.cybernetics2077.scanner.ScannerNetwork.sync(p);
                  CyberwareRuntime.broadcast(p);
                } catch (Exception ex) {
                  throw new RuntimeException(ex);
                }
              });
      mc.player.setYRot(0);
      mc.player.setXRot(6);
    }
    if (tick == 10) mc.player.getInventory().selected = 0;
    if (tick == 30) shot(out, "gun-idle");
    if (tick == 35) aim(true);
    if (tick == 50) {
      if (!TacZCompat.aiming(mc.player)
          || CyberwareClient.own().getInt("smartTier") != 5
          || CyberwareClient.own().getInt("smartTarget") < 0)
        throw new IllegalStateException(
            "Native ADS or Smart Link state missing: aiming="
                + TacZCompat.aiming(mc.player)
                + ", smartTier="
                + CyberwareClient.own().getInt("smartTier")
                + ", target="
                + CyberwareClient.own().getInt("smartTarget")
                + ", hand="
                + mc.player.getMainHandItem());
      shot(out, "gun-smart-lock");
    }
    if (tick == 55) aim(false);
    if (tick == 65) ScannerClient.KEY.setDown(true);
    if (tick == 67) shot(out, "gun-scanner-opening");
    if (tick == 85) {
      if (!ScannerClient.active()
          || NativeHudTransition.visible(NativeHudTransition.Group.HOTBAR) > .01
          || NativeHudTransition.visible(NativeHudTransition.Group.CROSSHAIR) > .01)
        throw new IllegalStateException("Scanner did not replace the native gun HUD");
      var playerApi = Class.forName("com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator");
      var operator =
          playerApi
              .getMethod("fromLocalPlayer", net.minecraft.client.player.LocalPlayer.class)
              .invoke(null, mc.player);
      if (!call(operator, "shoot").toString().equals("FORGE_EVENT_CANCEL"))
        throw new IllegalStateException("Native gun did not reject scanner fire");
      shot(out, "gun-scanner-open");
    }
    if (tick == 95) ScannerClient.KEY.setDown(false);
    if (tick == 110) {
      if (NativeHudTransition.visible(NativeHudTransition.Group.HOTBAR) < .99
          || NativeHudTransition.visible(NativeHudTransition.Group.CROSSHAIR) < .99)
        throw new IllegalStateException("Gun HUD did not return after scanner release");
      if (hiddenCrosshairFrames == 0 || hiddenCrosshairFrames != canceledCrosshairFrames)
        throw new IllegalStateException("TaCZ crosshair cancellation changed while scanning");
      shot(out, "gun-scanner-restored");
      Object api =
          Class.forName("com.tacz.guns.api.item.IGun")
              .getMethod("getIGunOrNull", ItemStack.class)
              .invoke(null, mc.player.getMainHandItem());
      if (((Number) call(api, "getCurrentAmmoCount", mc.player.getMainHandItem())).intValue() != 30)
        throw new IllegalStateException("Scanner/ADS changed ammunition");
      var gun = mc.player.getMainHandItem();
      Object data = gunData(gun), mode = call(gun.getItem(), "getFireMode", gun);
      normalInterval = ((Number) call(data, "getShootInterval", mc.player, mode, gun)).longValue();
      normalReload =
          ((Number) call(call(call(data, "getReloadData"), "getFeed"), "getEmptyTime"))
              .floatValue();
      mc.getSingleplayerServer()
          .execute(
              () -> {
                var p = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                p.getData(Cybernetics2077.CYBERWARE)
                    .setStackInSlot(
                        CyberwareCategory.OPERATING_SYSTEM.firstSlot(),
                        new ItemStack(Cybernetics2077.augment(Augment.DAVIDS_SANDEVISTAN, 5)));
                AugmentRuntime.state(p).sandyRearm = 0;
                AugmentRuntime.state(p).sandyCharge = 1;
                dev.cybernetics2077.temporal.Sandevistan.toggle(p);
              });
    }
    if (tick == 140) {
      var gun = mc.player.getMainHandItem();
      Object data = gunData(gun), mode = call(gun.getItem(), "getFireMode", gun);
      long slowed = ((Number) call(data, "getShootInterval", mc.player, mode, gun)).longValue();
      float reload =
          ((Number) call(call(call(data, "getReloadData"), "getFeed"), "getEmptyTime"))
              .floatValue();
      if (normalInterval <= 0
          || slowed != normalInterval * 16
          || reload != normalReload
          || !dev.cybernetics2077.temporal.Sandevistan.active(mc.player))
        throw new IllegalStateException(
            "Client firing clock or reload speed incorrect: " + normalInterval + " -> " + slowed);
      shot(out, "gun-david-world-clock");
      mc.getSingleplayerServer()
          .execute(
              () -> {
                var p = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                dev.cybernetics2077.temporal.Sandevistan.stop(p, AugmentRuntime.now(p));
              });
    }
    if (tick == 160) {
      Files.writeString(
          out.resolve("passed.txt"),
          "PASS: native ADS, Smart Link HUD, scanner handover and ammo preservation; client David"
              + " gun interval x16 with native reload timing unchanged.\n");
      mc.stop();
    }
  }

  private static Object gunData(ItemStack gun) throws Exception {
    var id = call(gun.getItem(), "getGunId", gun);
    var index =
        (java.util.Optional<?>)
            Class.forName("com.tacz.guns.api.TimelessAPI")
                .getMethod("getCommonGunIndex", ResourceLocation.class)
                .invoke(null, id);
    return call(index.orElseThrow(), "getGunData");
  }

  private static void aim(boolean aiming) throws Exception {
    ((net.minecraft.client.KeyMapping)
            Class.forName("com.tacz.guns.client.input.AimKey").getField("AIM_KEY").get(null))
        .setDown(aiming);
    var p = Minecraft.getInstance().player;
    Object operator =
        Class.forName("com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator")
            .getMethod("fromLocalPlayer", net.minecraft.client.player.LocalPlayer.class)
            .invoke(null, p);
    call(operator, "aim", aiming);
  }

  private static void shot(Path out, String name) throws Exception {
    Files.createDirectories(out);
    Screenshot.grab(
        out.toFile(), name + ".png", Minecraft.getInstance().getMainRenderTarget(), m -> {});
  }
}
