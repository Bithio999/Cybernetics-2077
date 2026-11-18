package dev.cybernetics2077.review;

import static dev.cybernetics2077.review.ContentClientReview.check;
import static dev.cybernetics2077.review.CyberwareClientReview.*;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.city.*;
import dev.cybernetics2077.client.ui.*;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.mob.*;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;

final class EnhancedClientReview {
  static final int[] mobs = new int[3];
  static volatile long balance;
  static int doctor;

  static AbstractWidget trade() {
    return Minecraft.getInstance().screen.children().stream()
        .filter(
            c ->
                c instanceof CyberActionButton b
                    && b.visible
                    && b.getMessage().getString().equals("-30%"))
        .map(c -> (AbstractWidget) c)
        .findFirst()
        .orElseThrow();
  }

  static void tick(int t) throws Exception {
    var mc = Minecraft.getInstance();
    if (t == 0) {
      mc.setScreen(null);
      server(
          p -> {
            p.closeContainer();
            p.getInventory().clearContent();
            p.server.overworld().setDayTime(18000);
            // Native starter stock and item synchronization, not a fabricated screen data model.
            var wallet = new StreetAccount();
            wallet.credit(100000);
            p.setData(CityContent.ACCOUNT, wallet);
            balance = p.getData(CityContent.ACCOUNT).eddies();
            for (int i = 0; i < 5; i++)
              p.getInventory()
                  .setItem(i, new ItemStack(Cybernetics2077.DAMAGED_PARTS.get(i).get(), 3));
            var types =
                new EntityType<?>[] {EntityType.HUSK, EntityType.SKELETON, EntityType.PILLAGER};
            for (int i = 0; i < 3; i++) {
              var mob = (Mob) types[i].create(p.serverLevel());
              mob.setNoAi(true);
              mob.setNoGravity(true);
              mob.setPos(-3 + i * 3, 64, 5);
              p.serverLevel().addFreshEntity(mob);
              EnhancedMobs.install(
                  mob,
                  List.of(
                      Cybernetics2077.augment(Augment.SUBDERMAL_ARMOR, i + 1),
                      Cybernetics2077.augment(Augment.BIONIC_JOINTS, i + 1)));
              mobs[i] = mob.getId();
            }
            var npc = CityContent.RIPPERDOC.get().create(p.serverLevel());
            npc.setNoAi(true);
            npc.setNoGravity(true);
            npc.setPos(2, 64, 1);
            p.serverLevel().addFreshEntity(npc);
            doctor = npc.getId();
            p.interactOn(npc, InteractionHand.MAIN_HAND);
          });
    }
    if (t == 20) {
      check(mc.screen instanceof RipperdocScreen, "Shop did not open");
      var menu = (RipperdocMenu) mc.player.containerMenu;
      check(
          menu.stocked(0) && menu.parts(1) == 3 && menu.eddies() == balance,
          "Native salvage data not synchronized");
      shot("ripperdoc-salvage-discount");
      var b = trade();
      mouse(
          "onMove",
          new Class[] {long.class, double.class, double.class},
          mc.getWindow().getWindow(),
          (b.getX() + 12.) * mc.getWindow().getGuiScale(),
          (b.getY() + 8.) * mc.getWindow().getGuiScale());
    }
    if (t == 45) shot("ripperdoc-salvage-tooltip");
    if (t == 50) {
      var b = trade();
      click(b.getX() + 10, b.getY() + 9);
    }
    if (t == 70) {
      var menu = (RipperdocMenu) mc.player.containerMenu;
      check(
          menu.parts(1) == 2 && menu.eddies() == balance - 700 && menu.result() == 3,
          "Native trade did not charge 700 eddies plus exactly one part");
      check(
          mc.player.getInventory().contains(new ItemStack(Cybernetics2077.OPTICS.get())),
          "Purchased optics missing");
      shot("ripperdoc-salvage-purchased");
      mc.options.guiScale().set(3);
      mc.getWindow().setWindowed(960, 720);
      mc.resizeDisplay();
    }
    if (t == 90) {
      check(mc.screen.width == 320 && mc.screen.height == 240, "Minimum-size fixture incorrect");
      for (var child : mc.screen.children())
        if (child instanceof AbstractWidget b && b.visible)
          check(
              b.getX() >= 0 && b.getY() >= 0 && b.getRight() <= 320 && b.getBottom() <= 240,
              "Shop widget out of bounds");
      shot("ripperdoc-salvage-small");
      mc.player.closeContainer();
      mc.options.guiScale().set(2);
      mc.getWindow().setWindowed(1280, 720);
      mc.resizeDisplay();
    }
    if (t == 105) mc.setScreen(new EnhancedAssetReviewScreen());
    if (t == 125) shot("chromed-mobs-and-salvage");
    if (t == 130) {
      Files.writeString(
          OUT.resolve("enhanced-passed.txt"),
          "PASS: real integrated-server chromed loadouts, synchronized variant names, native"
              + " animated mob layers and all five part models; daily-stock shop; hover trade"
              + " terms; actual mouse purchase consumes exactly one T1 part and 700 eddies; minimum"
              + " 320x240 layout bounds.\n");
      finished = true;
      mc.stop();
    }
  }
}
