package dev.cybernetics2077.review;

import static dev.cybernetics2077.cyberware.Augment.*;
import static dev.cybernetics2077.review.CyberwareClientReview.*;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.bench.CyberneticsBenchMenu;
import dev.cybernetics2077.client.mantis.CyberwareClient;
import dev.cybernetics2077.client.ui.*;
import dev.cybernetics2077.cyberware.*;
import java.util.List;
import net.minecraft.client.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;

/**
 * Real network, key input and rendering checks for the expansion, in the disposable review save.
 */
final class AugmentClientReview {
  static int enemy;

  static void openBench(List<ItemStack> items) {
    server(
        p -> {
          var implants = p.getData(Cybernetics2077.CYBERWARE);
          for (int i = 0; i < implants.getSlots(); i++) implants.setStackInSlot(i, ItemStack.EMPTY);
          p.getInventory().clearContent();
          p.getInventory().selected = 8;
          for (int i = 0; i < items.size(); i++) p.getInventory().setItem(i, items.get(i));
          p.openMenu(
              new net.minecraft.world.SimpleMenuProvider(
                  (id, inv, owner) -> new CyberneticsBenchMenu(id, inv),
                  net.minecraft.network.chat.Component.literal("Cybernetics Bench")));
        });
  }

  static ItemStack item(Augment kind) {
    return new ItemStack(Cybernetics2077.augment(kind, 5));
  }

  static void select(int index) throws Exception {
    var m = Minecraft.getInstance();
    var layout = CyberLayout.fit(m.screen.width, m.screen.height);
    var slot =
        CyberLayout.SOCKETS.stream().filter(s -> s.index() == index).findFirst().orElseThrow();
    click(layout.screenX(slot.x() + 48), layout.screenY(slot.y() + 48));
  }

  static void back() {
    Minecraft.getInstance().screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE, 0, 0);
  }

  static void close() {
    var m = Minecraft.getInstance();
    m.setScreen(null);
    m.player.closeContainer();
  }

  static void press(KeyMapping key) {
    KeyMapping.click(key.getKey());
  }

  static void tick(int t) throws Exception {
    var m = Minecraft.getInstance();
    if (t == 1340)
      openBench(
          List.of(
              item(EX_DISK),
              item(MEMORY_BOOST),
              item(CLAIRVOYANT),
              item(BIONIC_JOINTS),
              item(EPIMORPHIC_SKELETON),
              item(SUBDERMAL_ARMOR),
              new ItemStack(Cybernetics2077.CYBERDECK_TIERS.get(4).get())));
    if (t == 1360) for (int i = 0; i < 7; i++) ContentClientReview.quick(48 + i);
    if (t == 1370) {
      ContentClientReview.check(
          m.player.containerMenu instanceof CyberneticsBenchMenu bench
              && bench.capacityUsed() == 100,
          "Netrunner loadout failed installation/capacity");
      shot("expansion-netrunner-bench");
      close();
      m.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(m.player));
    }
    if (t == 1380) openButton();
    if (t == 1395) shot("expansion-netrunner-overview");
    if (t == 1400) select(0);
    if (t == 1410) shot("expansion-ex-disk");
    if (t == 1415) back();
    if (t == 1420) select(5);
    if (t == 1430) shot("expansion-clairvoyant-details");
    if (t == 1435) back();
    if (t == 1440) select(7);
    if (t == 1450) shot("expansion-epimorphic-details");
    if (t == 1455) {
      close();
      server(
          p -> {
            for (var mob :
                p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(40)))
              mob.discard();
            p.teleportTo(p.serverLevel(), .5, 64, .5, java.util.Set.of(), 0, 0);
            var zombie = EntityType.ZOMBIE.create(p.serverLevel());
            zombie.setPos(.5, 64, 5.5);
            zombie.setNoAi(true);
            zombie.setNoGravity(true);
            p.serverLevel().addFreshEntity(zombie);
            enemy = zombie.getId();
            dev.cybernetics2077.combat.CyberwareRuntime.state(p).ram = 18;
          });
    }
    if (t == 1460) {
      m.player.setXRot(0);
      m.player.setYRot(0);
      ScannerClient.KEY.setDown(true);
    }
    if (t == 1480) {
      ContentClientReview.check(
          CyberwareClient.own().getInt("ramMax") == 18, "Cortex RAM not reflected in real scanner");
      ContentClientReview.check(
          AugmentClient.highlighted(m.level.getEntity(enemy)),
          "Clairvoyant mark missing on client");
      shot("expansion-clairvoyance");
    }
    if (t == 1485) ScannerClient.KEY.setDown(false);
    if (t == 1495) {
      ContentClientReview.check(
          AugmentClient.highlighted(m.level.getEntity(enemy)), "Highlight lost on scanner release");
      shot("expansion-retained-highlight");
    }
    if (t == 1500)
      openBench(
          List.of(
              item(BIODYNE_BERSERK),
              item(BLOOD_PUMP),
              item(OPTICAL_CAMO),
              item(NEOFIBER),
              item(ADRENALINE_CONVERTER),
              item(HEAL_ON_KILL),
              item(BIONIC_JOINTS),
              item(SUBDERMAL_ARMOR),
              item(REINFORCED_TENDONS)));
    if (t == 1510) for (int i = 0; i < 9; i++) ContentClientReview.quick(48 + i);
    if (t == 1520) {
      ContentClientReview.check(
          m.player.containerMenu instanceof CyberneticsBenchMenu bench
              && bench.capacityUsed() == 97,
          "Berserk loadout failed native installation");
      shot("expansion-berserk-bench");
      close();
      m.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(m.player));
    }
    if (t == 1530) openButton();
    if (t == 1540) select(3);
    if (t == 1550) shot("expansion-berserk-details");
    if (t == 1555) {
      close();
      server(p -> p.setHealth(5));
    }
    if (t == 1565) press(AugmentClient.PUMP);
    if (t == 1580) {
      ContentClientReview.check(
          CyberwareClient.own().getInt("pumpCharges") == 1 && m.player.getHealth() >= 12,
          "Blood Pump hotkey failed");
      shot("expansion-blood-pump");
    }
    if (t == 1590) press(AugmentClient.BERSERK);
    if (t == 1600)
      ContentClientReview.check(
          CyberwareClient.own().getBoolean("berserk"), "Berserk hotkey failed");
    if (t == 1605) server(p -> p.hurt(p.damageSources().generic(), 1000));
    if (t == 1615) {
      ContentClientReview.check(
          m.player.isAlive() && m.player.getHealth() >= 5, "Native Berserk fatal-hit floor failed");
      shot("expansion-berserk-active");
    }
    if (t == 1618) press(AugmentClient.CAMO);
    if (t == 1630) {
      ContentClientReview.check(
          CyberwareClient.own().getBoolean("camo") && m.player.isInvisible(),
          "Optical Camo hotkey/visibility failed");
      shot("expansion-optical-camo");
    }
    if (t == 1640)
      server(
          p -> {
            var s = AugmentRuntime.state(p);
            s.berserkEnd = AugmentRuntime.now(p) - 1;
            s.berserkKills = 0;
            s.camoEnd = 0;
            p.setHealth(20);
            p.teleportTo(p.serverLevel(), .5, 64, .5, java.util.Set.of(), 0, 0);
            p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
          });
    if (t == 1660 || t == 1670) m.options.keyJump.setDown(true);
    if (t == 1664 || t == 1672) m.options.keyJump.setDown(false);
    if (t == 1680) {
      server(
          p ->
              ContentClientReview.check(
                  AugmentRuntime.state(p).airJumped && p.getY() > 64.8,
                  "Native double jump did not boost player"));
      shot("expansion-double-jump");
    }
    if (t == 1700)
      server(
          p -> {
            p.getData(Cybernetics2077.CYBERWARE).setStackInSlot(20, item(FORTIFIED_ANKLES));
            AugmentRuntime.state(p).ankleStart = -1;
            p.teleportTo(p.serverLevel(), .5, 64, .5, java.util.Set.of(), 0, 0);
            p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
          });
    if (t == 1710) m.options.keyJump.setDown(true);
    if (t == 1725)
      ContentClientReview.check(
          Math.abs(m.player.getY() - 64) < .1, "Ankles jumped before releasing charge");
    if (t == 1735) m.options.keyJump.setDown(false);
    if (t == 1750) {
      ContentClientReview.check(
          m.player.getY() > 66, "Charged ankle release did not launch player");
      shot("expansion-charged-jump");
    }
  }
}
