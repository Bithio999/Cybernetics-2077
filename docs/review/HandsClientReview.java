package dev.cybernetics2077.review;

import static dev.cybernetics2077.review.ContentClientReview.check;
import static dev.cybernetics2077.review.CyberwareClientReview.*;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.client.mantis.*;
import dev.cybernetics2077.combat.*;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.temporal.Sandevistan;
import java.nio.file.Files;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

final class HandsClientReview {
  static void tick(int t) throws Exception {
    var m = Minecraft.getInstance();
    int elapsed = t - 325;
    if (elapsed < 400) {
      int type = elapsed / 100, phase = elapsed % 100;
      if (phase == 0) {
        m.options.setCameraType(CameraType.FIRST_PERSON);
        server(
            p -> {
              Sandevistan.stop(p, AugmentRuntime.now(p));
              if (SandevistanClientReview.actor != null)
                Sandevistan.stop(SandevistanClientReview.actor, AugmentRuntime.now(p));
              p.getInventory().clearContent();
              for (var mob :
                  p.serverLevel()
                      .getEntitiesOfClass(
                          net.minecraft.world.entity.Mob.class, p.getBoundingBox().inflate(64)))
                mob.discard();
              if (SandevistanClientReview.actor != null) {
                SandevistanClientReview.actor.discard();
                SandevistanClientReview.actor = null;
              }
              p.getData(Cybernetics2077.CYBERWARE)
                  .setStackInSlot(
                      CyberwareCategory.ARMS.firstSlot(),
                      new ItemStack(Cybernetics2077.mantis(MantisVariant.of(type), 5)));
              var s = CyberwareRuntime.state(p);
              s.deployed = false;
              s.changed = p.level().getGameTime() - 100;
              p.teleportTo(p.serverLevel(), .5, 64, 8, java.util.Set.of(), 0, 0);
              CyberwareRuntime.broadcast(p);
            });
      }
      if (phase == 10) AugmentClientReview.press(CyberwareClient.DRAW);
      if (phase == 48) {
        check(
            CyberwareClient.own().getInt("bladeVariant") == type && CyberwareClient.active(),
            "Variant was not synced/deployed: " + type);
        var model = MantisBlenderMesh.model(MantisVariant.of(type), false);
        check(model.parts().size() == 6, "Variant rigid parts missing");
        shot("hands-" + MantisVariant.of(type).id + "-ready");
      }
      if (phase == 51) ContentClientReview.attack();
      if (phase == 55) shot("hands-" + MantisVariant.of(type).id + "-slash");
      if (phase == 65) m.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
      if (phase == 78) {
        check(
            m.options.getCameraType() == CameraType.THIRD_PERSON_FRONT,
            "Third-person camera was not active");
        shot("hands-" + MantisVariant.of(type).id + "-third-person");
      }
      if (phase == 88) m.options.setCameraType(CameraType.FIRST_PERSON);
      if (phase == 93) AugmentClientReview.press(CyberwareClient.DRAW);
    }
    if (elapsed == 405) m.setScreen(new Gallery());
    if (elapsed == 420) shot("hands-catalogue");
    if (elapsed == 430) {
      m.setScreen(null);
      server(
          p -> {
            p.getData(Cybernetics2077.CYBERWARE)
                .setStackInSlot(
                    CyberwareCategory.HANDS.firstSlot(),
                    new ItemStack(Cybernetics2077.augment(Augment.SMART_LINK, 5)));
            p.getData(Cybernetics2077.CYBERWARE)
                .setStackInSlot(
                    CyberwareCategory.HANDS.firstSlot() + 1,
                    new ItemStack(Cybernetics2077.augment(Augment.BALLISTIC_COPROCESSOR, 5)));
            p.getInventory().setItem(0, new ItemStack(Items.BOW));
            p.getInventory().setItem(1, new ItemStack(Items.ARROW, 32));
            p.getInventory().selected = 0;
            CyberwareRuntime.broadcast(p);
          });
    }
    if (elapsed == 440) m.options.keyUse.setDown(true);
    if (elapsed == 455) shot("hands-bow-preview");
    if (elapsed == 460) {
      m.options.keyUse.setDown(false);
      Files.writeString(
          OUT.resolve("hands-passed.txt"),
          "PASS: all four variant meshes, synced equipped types, native first/third person"
              + " deployment and attacks, original item icons and every tier model, bow preview"
              + " with two installed hands implants.\n");
      finished = true;
      m.stop();
    }
  }

  static final class Gallery extends Screen {
    Gallery() {
      super(Component.literal("Hands and Mantis catalogue"));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
      g.fill(0, 0, width, height, 0xff09131b);
      g.drawString(font, "HANDS / ARM SYSTEMS", 18, 12, 0xff74dfd8, false);
      var kinds =
          java.util.Arrays.stream(Augment.values())
              .filter(a -> a.category == CyberwareCategory.HANDS)
              .toList();
      for (int i = 0; i < kinds.size(); i++) {
        var a = kinds.get(i);
        int x = 18 + (i % 2) * (width / 2), y = 35 + (i / 2) * 48;
        g.fill(x, y, x + width / 2 - 32, y + 42, 0xff172a35);
        g.drawString(
            font, Component.translatable(a.translation()), x + 6, y + 4, 0xffb8d0d7, false);
        for (int t = a.firstTier; t <= 5; t++)
          item(
              g,
              new ItemStack(Cybernetics2077.augment(a, t)),
              x + 8 + (t - a.firstTier) * 35,
              y + 19,
              t);
      }
      for (int i = 0; i < 4; i++) {
        int x = 18 + (i % 2) * (width / 2), y = 184 + (i / 2) * 48;
        var v = MantisVariant.of(i);
        g.fill(x, y, x + width / 2 - 32, y + 42, 0xff172a35);
        g.drawString(font, v.title + " Mantis Blades", x + 6, y + 4, 0xff000000 | v.color, false);
        for (int t = 2; t <= 5; t++)
          item(g, new ItemStack(Cybernetics2077.mantis(v, t)), x + 8 + (t - 2) * 35, y + 19, t);
      }
    }

    void item(GuiGraphics g, ItemStack stack, int x, int y, int tier) {
      check(
          minecraft.getItemRenderer().getModel(stack, minecraft.level, minecraft.player, 0)
              != minecraft.getModelManager().getMissingModel(),
          "Missing model: " + stack);
      check(!stack.getHoverName().getString().startsWith("item."), "Untranslated implant");
      g.renderItem(stack, x, y);
      g.drawString(font, "T" + tier, x + 17, y + 6, 0xff9eaeb8, false);
    }
  }
}
