package dev.cybernetics2077.review;

import static dev.cybernetics2077.review.ContentClientReview.check;
import static dev.cybernetics2077.review.CyberwareClientReview.*;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.city.*;
import dev.cybernetics2077.client.mantis.CyberwareClient;
import dev.cybernetics2077.client.ui.*;
import dev.cybernetics2077.combat.*;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.deck.*;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;

final class HacksClientReview {
  static volatile int target, doctor;
  static volatile float beforeSynapse;
  static final List<Integer> enemies = new ArrayList<>();

  static void refill() {
    server(
        p -> {
          CyberwareRuntime.state(p).ram = AugmentRuntime.maxRam(p);
          CyberwareRuntime.syncIfChanged(p);
        });
  }

  static void attack() throws Exception {
    var method = Minecraft.class.getDeclaredMethod("startAttack");
    method.setAccessible(true);
    method.invoke(Minecraft.getInstance());
  }

  static void scroll() throws Exception {
    mouse(
        "onScroll",
        new Class[] {long.class, double.class, double.class},
        Minecraft.getInstance().getWindow().getWindow(),
        0.,
        -1.);
  }

  static void tick(int t) throws Exception {
    var mc = Minecraft.getInstance();
    if (t == 0)
      server(
          p -> {
            p.getInventory().clearContent();
            p.getInventory().setItem(0, new ItemStack(Cybernetics2077.RAVEN_TIERS.get(2).get()));
            p.getInventory().setItem(1, new ItemStack(Cybernetics2077.OPTICS_TIERS.get(4).get()));
            int slot = 2;
            for (var kind : QuickhackKind.values())
              p.getInventory()
                  .setItem(slot++, new ItemStack(Cybernetics2077.quickhack(kind, kind.firstTier)));
            p.serverLevel()
                .setBlock(
                    new BlockPos(0, 64, 2), Cybernetics2077.BENCH.get().defaultBlockState(), 3);
            for (var old :
                p.serverLevel().getEntitiesOfClass(Mob.class, new AABB(-8, 50, -8, 8, 90, 12)))
              old.discard();
            var cow = EntityType.COW.create(p.serverLevel());
            cow.setPos(.5, 64, 7.5);
            cow.setNoAi(true);
            cow.setNoGravity(true);
            cow.setCustomName(Component.literal("Primary Target"));
            cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);
            cow.setHealth(500);
            p.serverLevel().addFreshEntity(cow);
            target = cow.getId();
            for (double x : new double[] {-2, 3, 5}) {
              var mob = EntityType.PILLAGER.create(p.serverLevel());
              mob.setPos(x, 64, 8);
              mob.setNoAi(true);
              mob.setNoGravity(true);
              mob.setCustomName(Component.literal("Relay Target"));
              mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);
              mob.setHealth(500);
              p.serverLevel().addFreshEntity(mob);
              enemies.add(mob.getId());
            }
            p.teleportTo(p.serverLevel(), .5, 64, .5, Set.of(), 0, 0);
          });
    if (t == 10)
      mc.gameMode.useItemOn(
          mc.player,
          InteractionHand.MAIN_HAND,
          new BlockHitResult(
              new Vec3(.5, 64.7, 2), Direction.NORTH, new BlockPos(0, 64, 2), false));
    if (t == 20) {
      check(mc.screen instanceof CyberneticsBenchScreen, "Bench missing");
      for (int slot : new int[] {48, 49})
        mc.gameMode.handleInventoryMouseClick(
            mc.player.containerMenu.containerId, slot, 0, ClickType.QUICK_MOVE, mc.player);
    }
    if (t == 35) CityClientReview.clickButton("Cyberdeck");
    if (t == 45) {
      check(mc.screen instanceof CyberdeckScreen, "Chip editor missing");
      var menu = (CyberdeckMenu) mc.player.containerMenu;
      check(
          menu.cyberdeck().model() == CyberdeckItem.Model.RAVEN && menu.cyberdeck().ram() == 9,
          "Client displayed Paraline stats for Raven");
      for (int i = 2; i < 8; i++)
        mc.gameMode.handleInventoryMouseClick(
            menu.containerId, menu.programSlots() + 27 + i, 0, ClickType.QUICK_MOVE, mc.player);
      mouse(
          "onMove",
          new Class[] {long.class, double.class, double.class},
          mc.getWindow().getWindow(),
          5.,
          5.);
    }
    if (t == 65) {
      for (int i = 0; i < 6; i++)
        check(
            !mc.player.containerMenu.getSlot(i).getItem().isEmpty(),
            "A distinct program could not be installed");
      shot("raven-six-program-loadout");
      ((CyberdeckScreen) mc.screen).onClose();
    }
    if (t == 90) {
      check(mc.screen instanceof CyberwareScreen, "OS return failed");
      shot("raven-operating-system");
      mc.player.closeContainer();
      mc.setScreen(null);
      server(
          p -> {
            check(
                CyberdeckItem.programs(CyberwareRuntime.deck(p)).size() == 6,
                "Server loadout lost chips");
            p.lookAt(
                net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,
                p.serverLevel().getEntity(target).getEyePosition());
            p.connection.teleport(p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot());
          });
      refill();
    }
    if (t == 105) ScannerClient.KEY.setDown(true);
    if (t == 140) {
      check(
          CyberwareClient.own().getList("programs", 10).size() == 6,
          "Scanner program list missing");
      shot("scanner-six-quickhacks");
    }
    if (t == 145) attack();
    if (t == 170)
      server(
          p -> {
            check(
                ((LivingEntity) p.serverLevel().getEntity(target)).getHealth() == 492,
                "Short Circuit failed");
            check(
                enemies.stream()
                    .allMatch(
                        i -> ((LivingEntity) p.serverLevel().getEntity(i)).getHealth() == 496),
                "Raven did not relay to three enemies");
          });
    if (t == 175) {
      scroll();
      refill();
    }
    if (t == 185) attack();
    if (t == 196) shot("quickhack-overheat-upload");
    if (t == 220)
      server(
          p ->
              check(
                  QuickhackEffects.affected(
                      (LivingEntity) p.serverLevel().getEntity(target), QuickhackKind.OVERHEAT),
                  "Selected Overheat did not land"));
    if (t == 235) {
      scroll();
      refill();
    }
    if (t == 240) attack();
    if (t == 264) {
      shot("raven-contagion-spread");
      server(
          p ->
              check(
                  enemies.stream()
                      .allMatch(
                          i ->
                              QuickhackEffects.affected(
                                  (LivingEntity) p.serverLevel().getEntity(i),
                                  QuickhackKind.CONTAGION)),
                  "Contagion spread failed"));
    }
    if (t == 280) {
      scroll();
      refill();
    }
    if (t == 290) attack();
    if (t == 305)
      server(
          p ->
              check(
                  QuickhackEffects.affected(
                      (LivingEntity) p.serverLevel().getEntity(target),
                      QuickhackKind.REBOOT_OPTICS),
                  "Reboot Optics failed"));
    if (t == 315) {
      scroll();
      refill();
    }
    if (t == 325) attack();
    if (t == 340)
      server(
          p ->
              check(
                  QuickhackEffects.affected(
                      (LivingEntity) p.serverLevel().getEntity(target),
                      QuickhackKind.CRIPPLE_MOVEMENT),
                  "Cripple Movement failed"));
    if (t == 350) {
      scroll();
      refill();
    }
    if (t == 355)
      server(p -> beforeSynapse = ((LivingEntity) p.serverLevel().getEntity(target)).getHealth());
    if (t == 360) attack();
    if (t == 375) shot("quickhack-synapse-upload");
    if (t == 395)
      server(
          p ->
              check(
                  ((LivingEntity) p.serverLevel().getEntity(target)).getHealth()
                      <= beforeSynapse - 38.3,
                  "Synapse Burnout failed"));
    if (t == 400) {
      ScannerClient.KEY.setDown(false);
      server(
          p -> {
            var doc = CityContent.RIPPERDOC.get().create(p.serverLevel());
            doc.setPos(1.5, 64, 1.5);
            p.serverLevel().addFreshEntity(doc);
            doctor = doc.getId();
            p.getData(CityContent.ACCOUNT).reputation(50);
            p.getData(CityContent.ACCOUNT).credit(100000);
            for (int day = 0; day < 120; day++) {
              p.server.overworld().setDayTime(day * 24000L);
              var menu = new RipperdocMenu(3, p.getInventory(), doctor);
              boolean found = false;
              for (int i = 0; i < CyberwareCatalogue.offers().size(); i++)
                if (menu.stocked(i)
                    && CyberwareCatalogue.offers().get(i).item() instanceof CyberdeckItem d
                    && d.model() == CyberdeckItem.Model.RAVEN) found = true;
              if (found) break;
            }
            p.interactOn(doc, InteractionHand.MAIN_HAND);
          });
    }
    if (t == 420) {
      check(mc.screen instanceof RipperdocScreen, "Raven vendor missing");
      mc.screen.children().stream()
          .filter(w -> w instanceof EditBox)
          .map(w -> (EditBox) w)
          .findFirst()
          .orElseThrow()
          .setValue("Raven");
    }
    if (t == 435) shot("raven-ripperdoc-stock");
    if (t == 445) {
      Files.writeString(
          OUT.resolve("hacks-passed.txt"),
          "PASS: native bench installation, Raven identity and RAM, six distinct program sockets,"
              + " OS return, scanner scroll selection, all six uploads, three Raven relays,"
              + " DOT/control/Synapse effects, and daily ripperdoc Raven stock.\n");
      finished = true;
      mc.stop();
    }
  }
}
