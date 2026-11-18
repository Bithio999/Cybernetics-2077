package dev.cybernetics2077.review;

import static dev.cybernetics2077.cyberware.Augment.*;
import static dev.cybernetics2077.review.ContentClientReview.check;
import static dev.cybernetics2077.review.CyberwareClientReview.*;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.client.mantis.CyberwareClient;
import dev.cybernetics2077.cyberware.*;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

final class ExpansionClientReview {
  static void tick(int t) throws Exception {
    var m = Minecraft.getInstance();
    if (t == 0) {
      m.setScreen(new Catalogue());
      server(
          p ->
              p.serverLevel()
                  .getEntitiesOfClass(
                      net.minecraft.world.entity.Mob.class, p.getBoundingBox().inflate(40))
                  .forEach(net.minecraft.world.entity.Entity::discard));
    }
    if (t == 15) shot("cyberware-0.14-catalogue");
    if (t == 20)
      AugmentClientReview.openBench(
          List.of(
              AugmentClientReview.item(RAM_UPGRADE),
              AugmentClientReview.item(SELF_ICE),
              AugmentClientReview.item(STALKER),
              AugmentClientReview.item(BIONIC_JOINTS),
              AugmentClientReview.item(PARA_BELLUM),
              AugmentClientReview.item(LYNX_PAWS),
              new ItemStack(Cybernetics2077.CYBERDECK_TIERS.get(4).get())));
    if (t == 40) {
      check(
          m.player.containerMenu instanceof dev.cybernetics2077.bench.CyberneticsBenchMenu,
          "First bench did not open");
      for (int i = 0; i < 7; i++) ContentClientReview.quick(48 + i);
    }
    if (t == 55) {
      check(
          m.player.containerMenu.getSlot(1).getItem().getItem() instanceof AugmentItem item
              && item.augment() == SELF_ICE,
          "Native bench did not install Self-ICE");
      check(CyberwareClient.own().getInt("iceTier") == 5, "Self-ICE HUD snapshot not synchronized");
      shot("cyberware-0.14-bench");
      AugmentClientReview.close();
      m.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(m.player));
    }
    if (t == 65) openButton();
    if (t == 80) AugmentClientReview.select(1);
    if (t == 95) shot("cyberware-0.14-self-ice");
    if (t == 100) AugmentClientReview.close();
    // Let the old close packet reach the server before opening a new native container.
    if (t == 110)
      AugmentClientReview.openBench(
          List.of(
              AugmentClientReview.item(BIOMONITOR),
              AugmentClientReview.item(SECOND_HEART),
              AugmentClientReview.item(BLOOD_PUMP),
              AugmentClientReview.item(PAIN_EDITOR)));
    if (t == 130) for (int i = 0; i < 4; i++) ContentClientReview.quick(48 + i);
    if (t == 145) {
      check(
          m.player.containerMenu.getSlot(15).getItem().getItem() instanceof AugmentItem item
              && item.augment() == SECOND_HEART,
          "Native bench failed medical loadout");
      check(
          CyberwareClient.own().getInt("heartTier") == 5
              && CyberwareClient.own().getInt("pumpTier") == 5,
          "Medical effects not synchronized from server");
      server(
          p ->
              check(
                  AugmentRuntime.tier(p, SECOND_HEART) == 5,
                  "Server medical installation missing"));
      shot("cyberware-0.14-medical");
      AugmentClientReview.close();
    }
    if (t == 160) shot("cyberware-0.14-cooldown-hud");
    if (t == 165)
      server(
          p -> {
            // Timers in this disposable save must not carry over between repeated reviews.
            AugmentRuntime.state(p).heartReady = 0;
            p.setHealth(1);
            p.invulnerableTime = 0;
            p.hurt(p.damageSources().generic(), 10000);
            check(
                p.isAlive()
                    && p.getHealth() == p.getMaxHealth()
                    && AugmentRuntime.state(p).heartReady > AugmentRuntime.now(p),
                "Real integrated player Second Heart failed: tier="
                    + AugmentRuntime.tier(p, SECOND_HEART)
                    + ", hp="
                    + p.getHealth()
                    + ", ready="
                    + AugmentRuntime.state(p).heartReady);
          });
    if (t == 175) {
      check(
          m.player.isAlive()
              && m.player.getHealth() == m.player.getMaxHealth()
              && CyberwareClient.own().getLong("heartReady") > m.level.getGameTime(),
          "Revival/HUD failed to synchronize");
      shot("cyberware-0.14-second-heart");
    }
    if (t == 185) {
      Files.writeString(
          OUT.resolve("expansion-passed.txt"),
          "PASS: all 56 new item models/translations resolve in native renderer; fourteen-family"
              + " catalogue; real bench shift-click installation; client Self-ICE snapshot; native"
              + " implant detail menu and medical loadout; real integrated-player fatal hit"
              + " cancelled by Second Heart, full HP and cooldown HUD synchronized.\n");
      finished = true;
      m.stop();
    }
  }

  static final class Catalogue extends Screen {
    Catalogue() {
      super(Component.literal("Cyberware expansion"));
    }

    @Override
    public boolean isPauseScreen() {
      return false;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
      g.fill(0, 0, width, height, 0xff0b131c);
      g.drawString(font, "CYBERWARE // 14 NEW IMPLANTS", 22, 15, 0xff79dae5);
      g.drawString(
          font,
          "Daily ripperdoc stock / Researched starting tiers through Tier 5",
          22,
          30,
          0xffa2b2bf);
      var kinds =
          Arrays.stream(Augment.values())
              .filter(a -> a.ordinal() > FORTIFIED_ANKLES.ordinal())
              .toList();
      for (int i = 0; i < kinds.size(); i++) {
        var a = kinds.get(i);
        int x = 20 + (i % 2) * (width / 2), y = 48 + (i / 2) * 43;
        g.fill(x, y, x + width / 2 - 28, y + 38, 0xff15222e);
        for (int t = a.firstTier; t <= 5; t++) {
          var stack = new ItemStack(Cybernetics2077.augment(a, t));
          check(
              minecraft.getItemRenderer().getModel(stack, minecraft.level, minecraft.player, 0)
                  != minecraft.getModelManager().getMissingModel(),
              "Missing new item model " + a + t);
          check(
              !stack.getHoverName().getString().startsWith("item."),
              "Untranslated new implant " + a + t);
        }
        var stack = new ItemStack(Cybernetics2077.augment(a, 5));
        g.pose().pushPose();
        g.pose().translate(x + 5, y + 6, 0);
        g.pose().scale(1.5f, 1.5f, 1.5f);
        g.renderItem(stack, 0, 0);
        g.pose().popPose();
        g.drawString(font, stack.getHoverName(), x + 37, y + 7, 0xffd9e9ed);
        g.drawString(
            font,
            Component.translatable(a.category.translation()).getString()
                + " / T"
                + a.firstTier
                + "-5",
            x + 37,
            y + 22,
            0xff9eb6c4);
      }
    }
  }
}
