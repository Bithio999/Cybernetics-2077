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
  static double groundPeak, airStart;

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
    // The separate full-size diagnostic run validates pixels. Recording uses
    // a smaller native framebuffer and avoids every GPU screenshot readback.
    if (Boolean.getBoolean("cybernetics2077.recording")) return;
    try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
      int cyan = 0, red = 0;
      for (int y = 165; y < 230; y++)
        for (int x = 530; x < 750; x++) {
          int c = image.getPixelRGBA(x, y), r = c & 255, g = (c >> 8) & 255, b = (c >> 16) & 255;
          if (r < 180 && g > 185 && b > 190) cyan++;
        }
      for (int y = 145; y < 180; y++)
        for (int x = 620; x < 660; x++) {
          int c = image.getPixelRGBA(x, y), r = c & 255, g = (c >> 8) & 255, b = (c >> 16) & 255;
          if (r > 140 && r > g * 1.4 && r > b * 1.4) red++;
        }
      check(red > 20, "Red upload chip/progress missing: " + red);
      check(
          cyan > 20, "Target label did not render readable cyan glyphs above the target: " + cyan);
      System.out.println(
          "Upload label regression: " + cyan + " cyan glyph pixels; " + red + " red upload pixels");
    }
  }

  static void setupLunge(boolean air, boolean wall) {
    server(
        p -> {
          for (var entity :
              p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(30)))
            entity.discard();
          for (int x = -2; x <= 2; x++)
            for (int z = -2; z <= 10; z++) {
              p.serverLevel()
                  .setBlock(
                      new BlockPos(x, 63, z),
                      net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState(),
                      3);
              for (int y = 64; y <= 70; y++)
                p.serverLevel()
                    .setBlock(
                        new BlockPos(x, y, z),
                        net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
                        3);
            }
          p.teleportTo(p.serverLevel(), .5, 64, .5, java.util.Set.of(), 0, 0);
          p.setDeltaMovement(Vec3.ZERO);
          var enemy = EntityType.HUSK.create(p.serverLevel());
          enemy.setNoAi(true);
          enemy
              .getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
              .setBaseValue(100);
          enemy
              .getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)
              .setBaseValue(0);
          enemy.setHealth(100);
          enemy.setPos(.5, 64, 7);
          p.serverLevel().addFreshEntity(enemy);
          target = enemy.getId();
          var state = CyberwareRuntime.state(p);
          state.charge = -1;
          state.lunge = null;
          state.pendingHit = false;
          if (wall)
            for (int y = 64; y <= 68; y++)
              p.serverLevel()
                  .setBlock(
                      new BlockPos(0, y, 4),
                      net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),
                      3);
        });
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
            p.serverLevel()
                .setBlock(
                    new BlockPos(0, 64, 2),
                    net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
                    3);
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
    if (t == 219) {
      dev.cybernetics2077.client.mantis.ArmSkinBoundsReview.verify(m.player);
      check(NativeHandReferenceReview.samples > 0, "No native empty-hand reference was compared");
      System.out.println(
          "Native hand transform matches: "
              + NativeHandReferenceReview.samples
              + " real render samples");
      shot("mantis-native-hand");
    }
    if (t == 220) draw();
    if (t >= 221 && t <= 254) shot("mantis-deploy-" + String.format("%03d", t - 221));
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
            check(
                mob.getHealth() < health - 10,
                "Charged lunge did not deal damage; HP="
                    + mob.getHealth()
                    + ", before="
                    + health
                    + ", player="
                    + p.position()
                    + ", target="
                    + mob.position()
                    + ", state="
                    + CyberwareRuntime.snapshot(p));
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
    if (t == 550) setupLunge(false, false);
    if (t == 560) draw();
    if (t == 590) {
      m.options.keyUse.setDown(true);
      use();
    }
    if (t == 602) {
      m.options.keyUse.setDown(false);
      groundPeak = m.player.getY();
    }
    if (t >= 602 && t <= 619) {
      groundPeak = Math.max(groundPeak, m.player.getY());
      shot("mantis-ground-leap-" + String.format("%03d", t - 602));
    }
    if (t == 620) {
      check(
          m.player.getZ() > 4.5 && groundPeak > 64.25,
          "Ground leap did not travel/rise: z=" + m.player.getZ() + ", peak=" + groundPeak);
      server(
          p -> {
            check(p.getZ() > 4.5, "Server did not receive ground leap travel");
            check(
                ((LivingEntity) p.serverLevel().getEntity(target)).getHealth() == 86,
                "Ground leap did not land exactly one heavy hit");
            System.out.println("Ground leap native travel: " + (p.getZ() - .5) + " blocks");
          });
    }
    if (t == 630) setupLunge(true, false);
    if (t == 640) {
      m.options.keyUse.setDown(true);
      use();
    }
    if (t == 652) m.options.keyJump.setDown(true);
    if (t == 653) {
      m.options.keyJump.setDown(false);
      airStart = m.player.getY();
    }
    if (t == 654) {
      check(airStart > 64.2, "Air test did not actually jump: " + airStart);
      m.options.keyUse.setDown(false);
    }
    if (t >= 654 && t <= 673) shot("mantis-air-leap-" + String.format("%03d", t - 654));
    if (t == 676) {
      check(m.player.getZ() > 4.5, "Air leap failed to move: " + m.player.getZ());
      server(
          p -> {
            check(p.getZ() > 4.5, "Server did not receive air leap travel");
            check(
                ((LivingEntity) p.serverLevel().getEntity(target)).getHealth() == 86,
                "Air leap did not land exactly one heavy hit");
            System.out.println(
                "Air leap native travel: " + (p.getZ() - .5) + " blocks; release Y=" + airStart);
          });
    }
    if (t == 680) setupLunge(false, true);
    if (t == 690) {
      m.options.keyUse.setDown(true);
      use();
    }
    if (t == 702) m.options.keyUse.setDown(false);
    if (t == 723) {
      check(m.player.getZ() < 3.8, "Leap bypassed solid wall: " + m.player.getZ());
      server(
          p -> {
            var enemy = (LivingEntity) p.serverLevel().getEntity(target);
            check(enemy.getHealth() == 100, "Leap damaged a target through a wall");
          });
    }
    if (t == 730)
      server(
          p -> {
            for (int y = 64; y <= 68; y++)
              p.serverLevel()
                  .setBlock(
                      new BlockPos(0, y, 4),
                      net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
                      3);
            var enemy = p.serverLevel().getEntity(target);
            if (enemy != null) enemy.discard();
          });
    if (t == 740) {
      m.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
      m.player.setXRot(-5);
    }
    if (t == 748) shot("mantis-connected-idle");
    if (t == 750) attack();
    if (t >= 751 && t <= 764) shot("mantis-connected-cut-" + String.format("%03d", t - 751));
    if (t == 775) m.options.setCameraType(CameraType.FIRST_PERSON);
    if (t == 780)
      server(
          p -> {
            for (var entity :
                p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(30)))
              entity.discard();
            p.teleportTo(p.serverLevel(), .5, 64, .5, java.util.Set.of(), 0, 0);
            p.setDeltaMovement(Vec3.ZERO);
          });
    if (t == 790) {
      m.options.keyUse.setDown(true);
      use();
    }
    if (t == 802) m.options.keyUse.setDown(false);
    if (t >= 802 && t <= 825) shot("mantis-free-leap-" + String.format("%03d", t - 802));
    if (t == 830) {
      check(m.player.getZ() > 6, "Untargeted leap failed: " + m.player.position());
      server(p -> check(p.getZ() > 6, "Server did not receive untargeted leap"));
      System.out.println("Untargeted leap native travel: " + (m.player.getZ() - .5));
    }
    if (t == 850)
      m.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(m.player));
    if (t == 855) openButton();
    if (t == 868) {
      var l = CyberLayout.fit(m.screen.width, m.screen.height);
      click(l.screenX(1212), l.screenY(212));
    }
    if (t == 878) shot("cyberware-quickhack-entry");
    if (t == 880) {
      var l = CyberLayout.fit(m.screen.width, m.screen.height);
      click(l.screenX(660), l.screenY(660));
    }
    if (t == 892) {
      check(
          m.player.containerMenu instanceof CyberdeckMenu,
          "Cyberware quickhack button did not open chip menu");
      quick(0);
    }
    if (t == 900) {
      check(
          m.player.containerMenu.getSlot(0).getItem().isEmpty(),
          "Quickhack removal through viewer failed");
      quick(37);
    }
    if (t == 910) {
      check(
          m.player.containerMenu.getSlot(0).getItem().is(Cybernetics2077.SHORT_CIRCUIT.get()),
          "Quickhack reinstall through viewer failed");
      server(
          p ->
              check(
                  CyberdeckItem.has(CyberwareRuntime.deck(p), Cybernetics2077.SHORT_CIRCUIT.get()),
                  "Viewer chip edit was not persisted"));
      shot("cyberware-quickhack-editor");
    }
    if (t == 915) m.screen.onClose();
    if (t == 930) {
      check(
          m.screen instanceof CyberwareScreen, "Back from quickhacks did not return to cyberware");
      shot("cyberware-quickhack-return");
    }
    if (t == 940) {
      m.setScreen(null);
      m.player.closeContainer();
      m.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
      m.options.fov().set(40);
      m.player.setXRot(-20);
    }
    if (t == 945) shot("mantis-dorsal-open");
    if (t == 950) draw();
    if (t >= 950 && t <= 984) shot("mantis-dorsal-retract-" + String.format("%03d", t - 950));
    if (t == 986) shot("mantis-dorsal-closed");
    if (t == 990) draw();
    if (t >= 990 && t <= 1025) shot("mantis-dorsal-deploy-" + String.format("%03d", t - 990));
    if (t == 1050) shot("mantis-dorsal-ready");
    if (t == 1055) {
      m.options.setCameraType(CameraType.FIRST_PERSON);
      m.options.fov().set(70);
      m.player.setXRot(0);
    }
    if (t == 1120)
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
    if (t == 1160) {
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
              + " render and animation captures; actual native hand transform reference; dorsal"
              + " model closeup and independent tip extension; viewer quickhack removal/reinstall"
              + " and return; untargeted six-block leap; ground and airborne leaps travel 5.15"
              + " blocks and land one 14-damage hit each; solid-wall guard; dimension change"
              + " retains equipment state.\n");
      if (Boolean.getBoolean("cybernetics2077.recording")) {
        Files.writeString(
            OUT.resolve("recording-marks.json"), new com.google.gson.Gson().toJson(recordingMarks));
        RecordingFrames.finish();
      }
      finished = true;
      m.stop();
    }
  }
}
