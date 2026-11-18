package dev.cybernetics2077.review;

import static dev.cybernetics2077.review.ContentClientReview.check;
import static dev.cybernetics2077.review.CyberwareClientReview.*;

import dev.cybernetics2077.city.*;
import dev.cybernetics2077.client.ui.RipperdocScreen;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

/** Continues the real delivery playthrough into daily stock and live reputation/day changes. */
final class StockClientReview {
  static BitSet starter, yesterday;
  static volatile BitSet expected;
  static long balance;

  static BitSet selection(RipperdocMenu menu) {
    var set = new BitSet();
    for (int i = 0; i < CyberwareCatalogue.offers().size(); i++) if (menu.stocked(i)) set.set(i);
    return set;
  }

  static void eligible(RipperdocMenu menu, int tier) {
    var chosen = selection(menu);
    check(!chosen.isEmpty() && chosen.cardinality() <= 10, "Invalid native stock size");
    chosen.stream()
        .forEach(
            i ->
                check(
                    CyberwareCatalogue.offers().get(i).tier() <= tier,
                    "Locked tier present in native stock"));
  }

  static void tick(int t) throws Exception {
    var mc = Minecraft.getInstance();
    if (t == 0) {
      mc.player.closeContainer();
      mc.options.guiScale().set(2);
      mc.getWindow().setWindowed(1280, 720);
      mc.resizeDisplay();
      server(
          p -> {
            p.closeContainer();
            p.teleportTo(p.serverLevel(), 3.5, 64, 2.5, Set.of(), -90, 0);
            p.getData(CityContent.ACCOUNT).credit(1_000_000);
            p.server.overworld().setDayTime(6000);
            p.interactOn(
                p.serverLevel().getEntity(ContractClientReview.doctor), InteractionHand.MAIN_HAND);
          });
    }
    if (t == 10)
      mouse(
          "onMove",
          new Class[] {long.class, double.class, double.class},
          mc.getWindow().getWindow(),
          5.,
          5.);
    if (t == 20) {
      check(mc.screen instanceof RipperdocScreen, "Daily shop missing");
      var menu = (RipperdocMenu) mc.player.containerMenu;
      eligible(menu, 1);
      check(menu.stocked(0) && menu.streetCred() == 5, "Starter eye not guaranteed");
      starter = selection(menu);
      shot("ripperdoc-daily-stock-starter");
    }
    if (t == 25) server(p -> p.server.overworld().setDayTime(24000));
    if (t == 45) {
      var menu = (RipperdocMenu) mc.player.containerMenu;
      check(
          menu.stockDay() == 1
              && menu.restockTicks() > 23000
              && !starter.equals(selection(menu))
              && menu.stocked(0),
          "Dawn did not update live stock");
      shot("ripperdoc-daily-stock-dawn");
    }
    if (t == 50) server(p -> p.getData(CityContent.ACCOUNT).reputation(14));
    if (t == 70) {
      var menu = (RipperdocMenu) mc.player.containerMenu;
      check(menu.streetCred() == 19, "Reputation not synchronized");
      eligible(menu, 2);
      check(
          selection(menu).stream().anyMatch(i -> CyberwareCatalogue.offers().get(i).tier() == 2),
          "Newly eligible tier missing");
      shot("ripperdoc-daily-stock-sc19");
    }
    if (t == 75)
      server(
          p -> {
            p.getData(CityContent.ACCOUNT).reputation(31);
            p.server.overworld().setDayTime(48000);
            expected = selection((RipperdocMenu) p.containerMenu);
          });
    if (t == 95) {
      var menu = (RipperdocMenu) mc.player.containerMenu;
      eligible(menu, 5);
      check(menu.streetCred() == 50 && selection(menu).equals(expected), "Stock mask mismatch");
      check(
          selection(menu).stream().anyMatch(i -> CyberwareCatalogue.offers().get(i).tier() == 5),
          "Top-tier stock missing");
      yesterday = selection(menu);
      balance = menu.eddies();
      shot("ripperdoc-daily-stock-sc50");
    }
    if (t == 100) server(p -> p.server.overworld().setDayTime(72000));
    if (t == 120) {
      var menu = (RipperdocMenu) mc.player.containerMenu;
      var removed = (BitSet) yesterday.clone();
      removed.andNot(selection(menu));
      check(!removed.isEmpty(), "Stock did not rotate at next dawn");
      mc.gameMode.handleInventoryButtonClick(menu.containerId, 1000 + removed.nextSetBit(0));
    }
    if (t == 135) {
      var menu = (RipperdocMenu) mc.player.containerMenu;
      check(menu.result() == 5 && menu.eddies() == balance, "Stale buy was charged");
      shot("ripperdoc-daily-stock-stale");
    }
    if (t == 145) {
      Files.writeString(
          OUT.resolve("stock-passed.txt"),
          "PASS: full delivery and first-optics purchase/install, starter eye at SC5, daily stock "
              + "and countdown updated in an open menu at dawn, live SC19/50 stock eligibility, "
              + "all client/server stock masks equal, stale purchase rejected without payment.\n");
      finished = true;
      mc.stop();
    }
  }
}
