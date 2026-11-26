package dev.cybernetics2077.review;

import static dev.cybernetics2077.review.ContentClientReview.check;
import static dev.cybernetics2077.review.CyberwareClientReview.*;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.city.*;
import dev.cybernetics2077.client.mantis.CyberwareClient;
import dev.cybernetics2077.client.temporal.*;
import dev.cybernetics2077.client.ui.RipperdocScreen;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.temporal.*;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Native purchase and install with real menu synchronization, followed by O activation. */
final class IconicClientReview {
  static long before;
  static int ownEchoes;

  static void tick(int tick) throws Exception {
    int t = tick - 325;
    var m = Minecraft.getInstance();
    if (t == 0) {
      Files.deleteIfExists(OUT.resolve("iconic-passed.txt"));
      m.options.keyUp.setDown(false);
      m.options.keyRight.setDown(false);
      m.options.keyLeft.setDown(false);
      m.options.setCameraType(CameraType.FIRST_PERSON);
      server(
          p -> {
            if (SandevistanClientReview.actor != null) {
              Sandevistan.stop(SandevistanClientReview.actor, AugmentRuntime.now(p));
              SandevistanClientReview.actor.discard();
              SandevistanClientReview.actor = null;
            }
            Sandevistan.stop(p, AugmentRuntime.now(p));
            p.getData(Cybernetics2077.CYBERWARE).setStackInSlot(3, ItemStack.EMPTY);
            p.getInventory().clearContent();
            p.setData(Cybernetics2077.AUGMENT_STATE, new AugmentState());
            p.teleportTo(p.serverLevel(), .5, 64, 8, Set.of(), 0, 0);
            p.getData(CityContent.ACCOUNT).credit(200_000);
            p.getData(CityContent.ACCOUNT).reputation(50);
            var doc = CityContent.RIPPERDOC.get().create(p.serverLevel());
            doc.setNoAi(true);
            doc.setNoGravity(true);
            doc.setPos(2.5, 64, 8);
            p.serverLevel().addFreshEntity(doc);
            p.interactOn(doc, InteractionHand.MAIN_HAND);
            var menu = (RipperdocMenu) p.containerMenu;
            int index = IconicGameTests.offer();
            for (int day = 0; day < 512 && !menu.stocked(index); day++)
              p.server.overworld().setDayTime(day * 24000L);
            check(menu.stocked(index), "No native Iconic stock");
            before = menu.eddies();
          });
    }
    if (t == 15) {
      check(m.screen instanceof RipperdocScreen, "Iconic purchase screen missing");
      var search =
          (EditBox)
              m.screen.children().stream()
                  .filter(w -> w instanceof EditBox)
                  .findFirst()
                  .orElseThrow();
      search.setValue("Apogee");
    }
    if (t == 20) {
      var button =
          m.screen.children().stream()
              .filter(
                  w ->
                      w instanceof net.minecraft.client.gui.components.AbstractButton b
                          && b.getMessage().getString().equals("ACQUIRE"))
              .findFirst()
              .orElseThrow();
      check(
          !((net.minecraft.client.gui.components.AbstractButton) button).active,
          "Purchase enabled without chip");
      shot("apogee-purchase-requires-chip");
    }
    if (t == 25)
      server(
          p ->
              p.getInventory()
                  .setItem(0, new ItemStack(Cybernetics2077.ICONIC_REQUISITIONS.get(4).get())));
    if (t == 40) {
      check(((RipperdocMenu) m.player.containerMenu).requisitions(5) == 1, "Chip not synchronized");
      shot("apogee-ripperdoc-stock");
      CityClientReview.clickButton("ACQUIRE");
    }
    if (t == 55) {
      var menu = (RipperdocMenu) m.player.containerMenu;
      check(
          menu.result() == 3 && menu.eddies() == before - 75000 && menu.requisitions(5) == 0,
          "Native purchase did not consume correct payment");
      check(
          m.player
              .getInventory()
              .contains(new ItemStack(Cybernetics2077.augment(Augment.MILITECH_APOGEE, 5))),
          "Purchased Apogee not present");
      shot("apogee-purchase-complete");
      CityClientReview.clickButton("INSTALL");
    }
    if (t == 70) ContentClientReview.quick(48);
    if (t == 85) {
      check(
          CyberwareClient.own().getBoolean("sandyIconic"),
          "Native installed model not synchronized");
      shot("apogee-installed");
      AugmentClientReview.close();
    }
    if (t == 95)
      m.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(m.player));
    if (t == 105) openButton();
    if (t == 120) AugmentClientReview.select(3);
    if (t == 135) shot("apogee-details");
    if (t == 140) AugmentClientReview.close();
    if (t == 150) AugmentClientReview.press(SandevistanClient.KEY);
    if (t == 160) {
      check(
          Sandevistan.active(m.player) && Sandevistan.model(m.player) == SandevistanModel.APOGEE,
          "Native O activation ignored Iconic model");
      check(
          Math.abs(
                  Sandevistan.environmentPace(
                          m.level, m.player.getX(), m.player.getY(), m.player.getZ(), false)
                      - .1875)
              < 1e-6,
          "Native client time bubble used Dynalar strength");
      m.options.keyDown.setDown(true);
    }
    if (t >= 165 && t <= 205) ownEchoes = Math.max(ownEchoes, SandevistanTrails.lastOwnEchoes);
    if (t == 190) shot("apogee-first-person");
    if (t == 205) {
      m.options.keyDown.setDown(false);
      check(ownEchoes > 0, "Iconic missing first-person full-body echoes");
      AugmentClientReview.press(SandevistanClient.KEY);
    }
    if (t == 220) {
      check(!Sandevistan.active(m.player), "Iconic failed to toggle off");
      shot("apogee-recharge");
      Files.writeString(
          OUT.resolve("iconic-passed.txt"),
          "PASS: real stock screen and disabled chip gate; chip synchronization; native purchase,"
              + " payment and installation; Iconic detail page; O activation with 85% relative time"
              + " bubble; first-person body echoes; early off and recharge HUD.\n");
      Files.writeString(
          OUT.resolve("iconic-native-results.json"),
          new com.google.gson.GsonBuilder()
              .setPrettyPrinting()
              .create()
              .toJson(
                  Map.of(
                      "visibleOwnEchoes",
                      ownEchoes,
                      "environmentPace",
                      .1875,
                      "price",
                      75000,
                      "tier",
                      5)));
      finished = true;
      m.stop();
    }
  }
}
