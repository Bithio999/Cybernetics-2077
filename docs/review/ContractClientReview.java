package dev.cybernetics2077.review;

import static dev.cybernetics2077.review.ContentClientReview.check;
import static dev.cybernetics2077.review.CyberwareClientReview.*;

import dev.cybernetics2077.city.*;
import dev.cybernetics2077.client.ui.*;
import java.nio.file.Files;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.phys.AABB;

/** Native packets, tooltips, dialogue, navigation-only scanner and first paid installation. */
final class ContractClientReview {
  static volatile int doctor, fixer;

  static void tick(int t) throws Exception {
    var mc = Minecraft.getInstance();
    if (t == 0)
      server(
          p -> {
            p.setData(CityContent.ACCOUNT, new StreetAccount());
            p.getInventory().clearContent();
            for (var n :
                p.serverLevel()
                    .getEntitiesOfClass(CityResident.class, new AABB(-8, 50, -8, 8, 80, 8)))
              n.discard();
            var doc = CityContent.RIPPERDOC.get().create(p.serverLevel());
            doc.moveTo(4.5, 64, 2.5, 90, 0);
            p.serverLevel().addFreshEntity(doc);
            ClinicDirectory.get(p.server).record(doc);
            doctor = doc.getId();
            var fix = CityContent.FIXER.get().create(p.serverLevel());
            fix.moveTo(1.5, 64, -1.5, 0, 0);
            p.serverLevel().addFreshEntity(fix);
            fixer = fix.getId();
          });
    if (t == 10)
      server(p -> p.interactOn(p.serverLevel().getEntity(fixer), InteractionHand.MAIN_HAND));
    if (t == 25) {
      check(mc.screen instanceof FixerScreen, "Fixer menu missing");
      check(((FixerMenu) mc.player.containerMenu).hasRoute(), "No native clinic route");
      shot("contract-offer");
    }
    if (t == 30) CityClientReview.clickButton("ACCEPT CONTRACT");
    if (t == 45) {
      check(((FixerMenu) mc.player.containerMenu).stage() == 1, "Accept packet not synchronized");
      check(mc.player.getInventory().getItem(0).is(CityContent.CONTRACT.get()), "No native shard");
      shot("contract-active");
    }
    if (t == 50) {
      mc.player.closeContainer();
      mc.setScreen(new InventoryScreen(mc.player));
    }
    if (t == 55) {
      var screen = (InventoryScreen) mc.screen;
      var slot = mc.player.containerMenu.getSlot(36);
      mouse(
          "onMove",
          new Class[] {long.class, double.class, double.class},
          mc.getWindow().getWindow(),
          (screen.getGuiLeft() + slot.x + 8) * mc.getWindow().getGuiScale(),
          (screen.getGuiTop() + slot.y + 8) * mc.getWindow().getGuiScale());
    }
    if (t == 65) shot("contract-shard-tooltip");
    if (t == 70) {
      mc.player.closeContainer();
      ScannerClient.KEY.setDown(true);
    }
    if (t == 95) {
      check(ScannerClient.active(), "Contract navigation requires unavailable optics");
      check(
          !dev.cybernetics2077.scanner.ScannerNetwork.installed(mc.player), "Free optics granted");
      shot("contract-scanner-route");
    }
    if (t == 100) ScannerClient.KEY.setDown(false);
    if (t == 110)
      server(
          p -> {
            var doc = (CityResident) p.serverLevel().getEntity(doctor);
            p.teleportTo(p.serverLevel(), 3.5, 64, 2.5, java.util.Set.of(), -90, 0);
            p.interactOn(doc, InteractionHand.MAIN_HAND);
            check(p.getData(CityContent.ACCOUNT).deliveryStage() == 2, "Native turn-in failed");
          });
    if (t == 125) {
      shot("contract-complete-dialogue");
      server(
          p -> {
            check(
                p.getData(CityContent.ACCOUNT).eddies() == 1000
                    && p.getData(CityContent.ACCOUNT).streetCred() == 5,
                "Wrong native reward");
            p.interactOn(p.serverLevel().getEntity(doctor), InteractionHand.MAIN_HAND);
          });
    }
    if (t == 140) {
      check(mc.screen instanceof RipperdocScreen, "Doctor did not reopen shop");
      check(
          ((RipperdocMenu) mc.player.containerMenu).streetCred() == 5,
          "Street Cred not synchronized");
      mouse(
          "onMove",
          new Class[] {long.class, double.class, double.class},
          mc.getWindow().getWindow(),
          5.,
          5.);
    }
    if (t == 142) shot("ripperdoc-street-cred");
    if (t == 145) CityClientReview.clickButton("BUY");
    if (t == 155) {
      check(
          ((RipperdocMenu) mc.player.containerMenu).eddies() == 0,
          "Reward did not buy first optics");
      CityClientReview.clickButton("INSTALL");
    }
    if (t == 165) {
      mc.gameMode.handleInventoryMouseClick(
          mc.player.containerMenu.containerId, 48, 0, ClickType.QUICK_MOVE, mc.player);
    }
    if (t == 175)
      server(
          p ->
              check(
                  dev.cybernetics2077.scanner.ScannerNetwork.installed(p),
                  "Purchased optics not installed"));
    if (t == 180) {
      mc.player.closeContainer();
      ScannerClient.KEY.setDown(true);
    }
    if (t == 205) {
      check(ScannerClient.active(), "Installed scanner unavailable");
      shot("contract-after-install-scanner");
      ScannerClient.KEY.setDown(false);
    }
    if (t == 215) {
      // Minimum supported menu resolution; state retained across native resize.
      mc.options.guiScale().set(3);
      mc.getWindow().setWindowed(960, 720);
      mc.resizeDisplay();
      server(
          p -> {
            p.teleportTo(p.serverLevel(), .5, 64, .5, java.util.Set.of(), 0, 0);
            p.interactOn(p.serverLevel().getEntity(fixer), InteractionHand.MAIN_HAND);
          });
    }
    if (t == 235) {
      check(
          mc.screen instanceof FixerScreen && ((FixerMenu) mc.player.containerMenu).stage() == 2,
          "Completed one-time job was offered again");
      shot("contract-completed-small");
      Files.writeString(
          OUT.resolve("contract-passed.txt"),
          "PASS: native acceptance, virtual cargo shard and hover details, navigation without"
              + " optics, use-on-ripperdoc turn-in, dialogue, one-time 1000 eddies/+5 Street Cred,"
              + " locked stock, purchase and installation of first scanner, completed dossier at"
              + " 320x240.\n");
      finished = true;
      mc.stop();
    }
  }
}
