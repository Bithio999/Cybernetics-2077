package dev.cybernetics2077.compat.review;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.combat.CyberwareNetwork;
import dev.cybernetics2077.cyberware.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Actual client trigger, packets and ammo across repeated extreme-slow transitions. */
public final class GunRecoveryReview {
  private static final List<String> trace = new ArrayList<>();
  private static volatile Throwable failure;
  private static int initialAmmo;

  private static Object call(Object o, String name, Object... args) throws Exception {
    return Arrays.stream(o.getClass().getMethods())
        .filter(m -> m.getName().equals(name) && m.getParameterCount() == args.length)
        .findFirst()
        .orElseThrow()
        .invoke(o, args);
  }

  private static Object operator() throws Exception {
    return Class.forName("com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator")
        .getMethod("fromLocalPlayer", net.minecraft.client.player.LocalPlayer.class)
        .invoke(null, Minecraft.getInstance().player);
  }

  private static void server(
      java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> run) {
    var mc = Minecraft.getInstance();
    mc.getSingleplayerServer()
        .execute(
            () -> {
              try {
                run.accept(
                    mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()));
              } catch (Throwable t) {
                failure = t;
              }
            });
  }

  private static void equip(net.minecraft.server.level.ServerPlayer p, String id) {
    try {
      var m =
          RicochetFireReview.class.getDeclaredMethod(
              "equipGun", net.minecraft.server.level.ServerPlayer.class, String.class);
      m.setAccessible(true);
      m.invoke(null, p, id);
      initialAmmo = totalAmmo(p.getMainHandItem());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private static int totalAmmo(ItemStack gun) throws Exception {
    return ((Number) call(gun.getItem(), "getCurrentAmmoCount", gun)).intValue()
        + ((Boolean) call(gun.getItem(), "hasBulletInBarrel", gun) ? 1 : 0);
  }

  private static void fire(String label) throws Exception {
    Object op = operator();
    long cd = ((Number) call(op, "getClientShootCoolDown")).longValue();
    String result = call(op, "shoot").toString();
    trace.add(label + ": client cooldown=" + cd + ", native trigger=" + result);
    if (!result.equals("SUCCESS")) throw new IllegalStateException(label + ": " + result);
  }

  private static void toggle() {
    PacketDistributor.sendToServer(new CyberwareNetwork.Action(13));
  }

  public static void tick(int t, Path out) throws Exception {
    var mc = Minecraft.getInstance();
    if (failure != null) throw new IllegalStateException("Server verification", failure);
    if (t == 0) {
      mc.setScreen(null);
      server(
          p -> {
            var inv = p.getData(Cybernetics2077.CYBERWARE);
            for (int i = 0; i < inv.getSlots(); i++) inv.setStackInSlot(i, ItemStack.EMPTY);
            inv.setStackInSlot(
                CyberwareCategory.OPERATING_SYSTEM.firstSlot(),
                new ItemStack(Cybernetics2077.augment(Augment.CREATIVE_SANDEVISTAN, 5)));
            var s = AugmentRuntime.state(p);
            s.sandyEnd = 0;
            s.sandyCharge = 1;
            s.sandyRearm = 0;
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            equip(p, "ak47");
          });
    }
    if (t == 15 || t == 45 || t == 90 || t == 125) toggle();
    if (t == 25) fire("AKM in 99.9% slow");
    if (t == 30 || t == 60 || t == 105) toggle();
    if (t == 65) fire("AKM after slow ends");
    if (t == 55) fire("AKM after slow reactivation");
    if (t == 70)
      server(
          p -> {
            try {
              int count = totalAmmo(p.getMainHandItem());
              trace.add("AKM server accepted shots=" + (initialAmmo - count));
              if (initialAmmo - count != 3)
                throw new IllegalStateException(
                    "AKM expected 3 accepted shots, consumed=" + (initialAmmo - count));
              equip(p, "deagle");
            } catch (Exception e) {
              throw new IllegalStateException(e);
            }
          });
    if (t == 100) fire("Deagle in 99.9% slow");
    if (t == 120) fire("Deagle after slow ends");
    if (t == 140) fire("Deagle after slow reactivation");
    if (t == 150) {
      toggle();
      server(
          p -> {
            try {
              int count = totalAmmo(p.getMainHandItem());
              trace.add("Deagle server accepted shots=" + (initialAmmo - count));
              if (initialAmmo - count != 3)
                throw new IllegalStateException(
                    "Deagle expected 3 accepted shots, consumed=" + (initialAmmo - count));
            } catch (Exception e) {
              throw new IllegalStateException(e);
            }
          });
    }
    if (t == 160) {
      Files.createDirectories(out);
      Files.write(out.resolve("gun-recovery-native.txt"), trace);
      Files.writeString(
          out.resolve("passed.txt"),
          "PASS: actual AKM and Deagle fire before/after slow and after reactivation; six client"
              + " triggers accepted by server, native ammo consumed.\n");
      mc.stop();
    }
  }
}
