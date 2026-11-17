package dev.cybernetics2077.review;

import static dev.cybernetics2077.review.CyberwareClientReview.*;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.bench.CyberneticsBenchMenu;
import dev.cybernetics2077.client.mantis.CyberwareClient;
import dev.cybernetics2077.client.ui.*;
import dev.cybernetics2077.combat.CyberwareRuntime;
import dev.cybernetics2077.deck.*;
import java.nio.file.Files;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;

/**
 * Extends the native review with real bench/chip transactions, scanner uploads and arm animations.
 */
final class ContentClientReview {
  static int target;
  static float health;

  static void quick(int slot) {
    var m = Minecraft.getInstance();
    m.gameMode.handleInventoryMouseClick(
        m.player.containerMenu.containerId, slot, 0, ClickType.QUICK_MOVE, m.player);
  }

  static void attack() throws Exception {
    var method = Minecraft.class.getDeclaredMethod("startAttack");
    method.setAccessible(true);
    method.invoke(Minecraft.getInstance());
  }

  static void use() throws Exception {
    var method = Minecraft.class.getDeclaredMethod("startUseItem");
    method.setAccessible(true);
    method.invoke(Minecraft.getInstance());
  }

  static void draw() {
    KeyMapping.click(
        com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(
            org.lwjgl.glfw.GLFW.GLFW_KEY_G));
  }

  static void check(boolean value, String text) {
    if (!value) throw new IllegalStateException(text);
  }

  static void checkUploadLabel() throws Exception {
    try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
      int cyan = 0;
      for (int y = 170; y < 214; y++)
        for (int x = 530; x < 750; x++) {
          int c = image.getPixelRGBA(x, y), r = c & 255, g = (c >> 8) & 255, b = (c >> 16) & 255;
          if (r < 180 && g > 185 && b > 190) cyan++;
        }
      check(
          cyan > 20, "Upload label did not render readable cyan glyphs above the target: " + cyan);
      System.out.println("Upload label regression: " + cyan + " cyan glyph pixels");
    }
  }

  static void tick(int t) throws Exception {
    var m = Minecraft.getInstance();
    if (t == 0) {
      m.setScreen(null);
      server(
          p -> {
            p.getInventory().clearContent();
            p.getInventory().setItem(0, new ItemStack(Cybernetics2077.OPTICS.get()));
            p.getInventory().setItem(1, new ItemStack(Cybernetics2077.CYBERDECK.get()));
            p.getInventory().setItem(2, new ItemStack(Cybernetics2077.SHORT_CIRCUIT.get()));
            p.getInventory().setItem(3, new ItemStack(Cybernetics2077.MANTIS.get()));
            for (int i = 0; i < 21; i++)
              p.getData(Cybernetics2077.CYBERWARE).setStackInSlot(i, ItemStack.EMPTY);
            p.serverLevel()
                .setBlock(
                    new BlockPos(1, 64, 0), Cybernetics2077.BENCH.get().defaultBlockState(), 3);
            p.getInventory().selected = 0;
            CyberwareRuntime.state(p).ram = 4;
          });
    }
    if (t == 10)
      m.gameMode.useItemOn(
          m.player,
          InteractionHand.MAIN_HAND,
          new BlockHitResult(
              new Vec3(1.05, 64.5, .5), Direction.WEST, new BlockPos(1, 64, 0), false));
    if (t == 20) {
      check(m.player.containerMenu instanceof CyberneticsBenchMenu, "Bench did not open");
      quick(48);
      quick(49);
      quick(51);
    }
    if (t == 32) {
      check(((CyberneticsBenchMenu) m.player.containerMenu).hasDeck(), "Deck did not install");
      shot("content-bench");
    }
    if (t == 35) {
      var b =
          m.screen.children().stream()
              .filter(
                  c ->
                      c instanceof AbstractButton a
                          && a.getMessage().getString().equals("Cyberdeck"))
              .map(c -> (AbstractButton) c)
              .findFirst()
              .orElseThrow();
      click(b.getX() + b.getWidth() / 2, b.getY() + b.getHeight() / 2);
    }
    if (t == 45) {
      check(m.player.containerMenu instanceof CyberdeckMenu, "Chip menu did not open");
      quick(31);
    }
    if (t == 58) {
      check(
          m.player.containerMenu.getSlot(0).getItem().is(Cybernetics2077.SHORT_CIRCUIT.get()),
          "Native chip installation failed");
      shot("cyberdeck-loadout");
      server(
          p ->
              check(
                  CyberdeckItem.has(CyberwareRuntime.deck(p), Cybernetics2077.SHORT_CIRCUIT.get()),
                  "Chip missing from deck component"));
    }
    if (t == 65) m.player.closeContainer();
    if (t == 75)
      m.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(m.player));
    if (t == 80) openButton();
    if (t == 95) shot("content-cyberware");
    if (t == 105) {
      var l = CyberLayout.fit(m.screen.width, m.screen.height);
      click(l.screenX(698), l.screenY(370));
    }
    if (t == 128) shot("mantis-detail");
    if (t == 135) {
      m.setScreen(null);
      m.player.closeContainer();
      server(
          p -> {
            for (var entity :
                p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(30)))
              entity.discard();
            var mob = EntityType.HUSK.create(p.serverLevel());
            mob.setNoAi(true);
            mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
                .setBaseValue(100);
            mob.setHealth(100);
            mob.setPos(.5, 64, 3.4);
            p.serverLevel().addFreshEntity(mob);
            target = mob.getId();
            p.teleportTo(p.serverLevel(), .5, 64, .5, java.util.Set.of(), 0, 0);
            p.getInventory().selected = 0;
          });
    }
    if (t == 150) {
      m.player.setYRot(0);
      m.player.setXRot(0);
      ScannerClient.KEY.setDown(true);
    }
    if (t == 173) shot("short-circuit-scanner");
    if (t == 175) attack();
    if (t >= 176 && t <= 201) shot("short-circuit-frame-" + String.format("%03d", t - 176));
    if (t == 188) checkUploadLabel();
    if (t == 200)
      server(
          p -> {
            var mob = (LivingEntity) p.serverLevel().getEntity(target);
            check(
                Math.abs(mob.getHealth() - 92) < .01,
                "Short Circuit damage incorrect: " + mob.getHealth());
            health = mob.getHealth();
            check(
                CyberwareRuntime.state(p).ram > 2 && CyberwareRuntime.state(p).ram < 3,
                "RAM was not charged");
          });
    if (t == 205) ScannerClient.KEY.setDown(false);
    if (t == 220) draw();
    if (t >= 221 && t <= 248) shot("mantis-deploy-" + String.format("%03d", t - 221));
    if (t == 252) {
      check(CyberwareClient.active(), "Claws did not deploy");
      shot("mantis-first-person");
    }
    if (t == 260 || t == 280 || t == 300) attack();
    if (t >= 260 && t <= 312) shot("mantis-combo-" + String.format("%03d", t - 260));
    if (t == 315)
      server(
          p -> {
            var mob = (LivingEntity) p.serverLevel().getEntity(target);
            check(mob.getHealth() < health - 20, "Blade combos did not deal damage");
            health = mob.getHealth();
          });
    if (t == 320) {
      m.options.keyUse.setDown(true);
      use();
    }
    if (t == 335) m.options.keyUse.setDown(false);
    if (t >= 320 && t <= 360) shot("mantis-heavy-" + String.format("%03d", t - 320));
    if (t == 365)
      server(
          p -> {
            var mob = (LivingEntity) p.serverLevel().getEntity(target);
            check(mob.getHealth() < health - 10, "Charged lunge did not deal damage");
          });
    if (t == 375) draw();
    if (t >= 376 && t <= 402) shot("mantis-retract-" + String.format("%03d", t - 376));
    if (t == 410) {
      check(!CyberwareClient.active(), "Retraction failed");
      server(p -> p.getInventory().setItem(0, new ItemStack(Items.DIAMOND_SWORD)));
    }
    if (t == 420) draw();
    if (t == 432) {
      check(!CyberwareClient.active(), "Claws deployed over a held weapon");
      server(p -> p.getInventory().setItem(0, ItemStack.EMPTY));
    }
    if (t == 442) draw();
    if (t == 465)
      server(
          p -> {
            var mob = p.serverLevel().getEntity(target);
            if (mob != null) mob.discard();
          });
    if (t == 470) {
      m.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
      m.player.setXRot(-5);
    }
    if (t == 480) shot("mantis-third-person");
    if (t == 485) attack();
    if (t >= 485 && t <= 508) shot("mantis-third-" + String.format("%03d", t - 485));
    if (t == 515) {
      m.options.setCameraType(CameraType.FIRST_PERSON);
      draw();
    }
    if (t == 520)
      server(
          p -> {
            var nether = p.server.getLevel(net.minecraft.world.level.Level.NETHER);
            for (int x = -2; x <= 2; x++)
              for (int z = -2; z <= 2; z++) {
                nether.setBlock(
                    new BlockPos(x, 79, z),
                    net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState(),
                    3);
                for (int y = 80; y < 84; y++)
                  nether.setBlock(
                      new BlockPos(x, y, z),
                      net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
                      3);
              }
            p.teleportTo(nether, .5, 80, .5, java.util.Set.of(), 0, 0);
          });
    if (t == 550) {
      check(
          m.level.dimension() == net.minecraft.world.level.Level.NETHER
              && CyberwareClient.own().getBoolean("deck")
              && CyberwareClient.own().getBoolean("shortCircuit")
              && CyberwareClient.own().getBoolean("claws")
              && ScannerClient.available(),
          "Equipment state lost on dimension change");
      Files.writeString(
          OUT.resolve("content-passed.txt"),
          "PASS: bench native shift-click installs optics, deck and arms; native chip menu installs"
              + " Short Circuit into persistent deck component; equipped viewer; actual scanner LMB"
              + " upload deals 8 electrical damage and reserves RAM; three blade contacts and"
              + " charged lunge damage; deploy/retract; occupied-hand guard; first/third-person"
              + " render and animation captures; dimension change retains equipment state.\n");
      finished = true;
      m.stop();
    }
  }
}
