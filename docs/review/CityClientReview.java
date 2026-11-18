package dev.cybernetics2077.review;

import static dev.cybernetics2077.review.ContentClientReview.check;
import static dev.cybernetics2077.review.CyberwareClientReview.*;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.city.*;
import dev.cybernetics2077.client.ui.*;
import java.nio.file.Files;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.minecraft.world.phys.*;

final class CityClientReview {
  static volatile int doc, fixer;
  static volatile net.minecraft.world.level.levelgen.structure.BoundingBox villageBox;

  static void camera(double x, double y, double z, double tx, double ty, double tz) {
    var mc = Minecraft.getInstance();
    mc.player.getAbilities().flying = true;
    mc.player.setNoGravity(true);
    server(
        p -> {
          p.setNoGravity(true);
          p.getAbilities().flying = true;
          p.onUpdateAbilities();
          p.teleportTo(p.serverLevel(), x, y, z, Set.of(), 0, 0);
          p.lookAt(Anchor.EYES, new Vec3(tx, ty, tz));
          p.connection.teleport(p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot());
        });
  }

  static void clickButton(String label) throws Exception {
    var mc = Minecraft.getInstance();
    var b =
        mc.screen.children().stream()
            .filter(
                v ->
                    v instanceof net.minecraft.client.gui.components.AbstractButton a
                        && a.getMessage().getString().equals(label))
            .map(v -> (net.minecraft.client.gui.components.AbstractButton) v)
            .findFirst()
            .orElseThrow();
    click(b.getX() + b.getWidth() / 2, b.getY() + b.getHeight() / 2);
  }

  static void tick(int t) throws Exception {
    var mc = Minecraft.getInstance();
    if (t == 0)
      server(
          p -> {
            p.closeContainer();
            for (var item :
                p.serverLevel()
                    .getEntitiesOfClass(
                        net.minecraft.world.entity.item.ItemEntity.class,
                        new AABB(40, 0, -48, 210, 200, 64))) item.discard();
            p.setGameMode(GameType.CREATIVE);
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            p.getInventory().clearContent();
            for (var n :
                p.serverLevel()
                    .getEntitiesOfClass(CityResident.class, new AABB(70, 0, -16, 190, 160, 40)))
              n.discard();
            for (int cx = 4; cx <= 11; cx++)
              for (int cz = -1; cz <= 2; cz++) p.serverLevel().setChunkForced(cx, cz, true);
            for (int x = 40; x < 210; x++)
              for (int z = -48; z < 64; z++) {
                for (int y = 60; y < 63; y++)
                  p.serverLevel()
                      .setBlock(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState(), 2);
                p.serverLevel()
                    .setBlock(new BlockPos(x, 63, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                if (x >= 74 && x < 184 && z >= -8 && z < 32)
                  for (int y = 64; y < 101; y++)
                    p.serverLevel()
                        .setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
              }
            var mgr = p.serverLevel().getStructureManager();
            String[] names = {"clinic_small", "clinic_large", "afterlife"};
            int[] xs = {80, 110, 150};
            for (int n = 0; n < 3; n++) {
              var pos = new BlockPos(xs[n], n == 2 ? 61 : 63, 0);
              var template = mgr.getOrCreate(Cybernetics2077.id("village/" + names[n]));
              check(
                  template.placeInWorld(
                      p.serverLevel(),
                      pos,
                      pos,
                      new StructurePlaceSettings()
                          .setKnownShape(true)
                          .setIgnoreEntities(false)
                          .setFinalizeEntities(true)
                          .addProcessor(JigsawReplacementProcessor.INSTANCE),
                      p.getRandom(),
                      18),
                  "City placement failed");
            }
            doc =
                p
                    .serverLevel()
                    .getEntitiesOfClass(CityResident.class, new AABB(80, 63, 0, 93, 73, 15))
                    .stream()
                    .filter(n -> !n.fixer())
                    .findFirst()
                    .orElseThrow()
                    .getId();
            fixer =
                p
                    .serverLevel()
                    .getEntitiesOfClass(CityResident.class, new AABB(150, 61, 0, 175, 71, 25))
                    .stream()
                    .filter(CityResident::fixer)
                    .findFirst()
                    .orElseThrow()
                    .getId();
            p.getData(CityContent.ACCOUNT).credit(200_000);
            p.serverLevel().setDayTime(6000);
            p.serverLevel().setWeatherParameters(60000, 0, false, false);
          });
    if (t == 25)
      server(
          p -> {
            for (var item :
                p.serverLevel()
                    .getEntitiesOfClass(
                        net.minecraft.world.entity.item.ItemEntity.class,
                        new AABB(40, 0, -48, 210, 200, 64))) item.discard();
          });
    if (t == 30) {
      mc.options.hideGui = true;
      camera(75, 69, -6, 85.5, 66, 6);
    }
    if (t == 65) shot("clinic-small-exterior");
    if (t == 80) camera(85.8, 64.1, 4.0, 86.2, 65, 9.5);
    if (t == 110) shot("clinic-small-interior");
    if (t == 125) camera(104, 73, -7, 116.5, 69, 6);
    if (t == 160) shot("clinic-large-exterior");
    if (t == 175) camera(115.2, 70.1, 10.6, 118.5, 71, 8);
    if (t == 205) shot("clinic-large-surgery");
    if (t == 220) camera(146, 70, -6, 156.5, 65, 6);
    if (t == 260) shot("afterlife-exterior");
    if (t == 275) camera(157.2, 62.1, 5.5, 152.5, 64, 10.5);
    if (t == 310) shot("afterlife-bar");
    if (t == 325) camera(157.6, 62, 6.8, 160.7, 63, 7);
    if (t == 355) shot("afterlife-booth");
    if (t == 370) {
      mc.options.hideGui = false;
      server(
          p -> {
            p.teleportTo(p.serverLevel(), 83.5, 64, 4.5, Set.of(), 0, 0);
            var n = (CityResident) p.serverLevel().getEntity(doc);
            n.interact(p, net.minecraft.world.InteractionHand.MAIN_HAND);
          });
    }
    if (t == 390)
      mouse(
          "onMove",
          new Class[] {long.class, double.class, double.class},
          mc.getWindow().getWindow(),
          5.,
          5.);
    if (t == 400) {
      check(mc.screen instanceof RipperdocScreen, "Ripperdoc screen missing");
      check(((RipperdocMenu) mc.player.containerMenu).eddies() >= 200_000, "Wallet did not sync");
      shot("ripperdoc-purchase");
    }
    if (t == 410) clickButton("BUY");
    if (t == 430) {
      server(
          p -> {
            check(
                p.getInventory().contains(new ItemStack(Cybernetics2077.OPTICS.get())),
                "Purchase missing");
            check(p.getData(CityContent.ACCOUNT).eddies() >= 199_000, "Wrong transaction");
          });
      clickButton("INSTALL");
    }
    if (t == 450) {
      check(((RipperdocMenu) mc.player.containerMenu).tab() == 1, "Install tab did not sync");
      mc.gameMode.handleInventoryMouseClick(
          mc.player.containerMenu.containerId, 48, 0, ClickType.QUICK_MOVE, mc.player);
    }
    if (t == 475) {
      check(
          mc.player.containerMenu.getSlot(5).getItem().is(Cybernetics2077.OPTICS.get()),
          "NPC installation failed");
      shot("ripperdoc-install");
    }
    if (t == 478) {
      org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 960, 720);
      mc.options.guiScale().set(3);
      mc.resizeDisplay();
    }
    if (t == 484) {
      var screen =
          (net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) mc.screen;
      check(
          screen.getGuiLeft() >= 0
              && screen.getGuiTop() >= 0
              && screen.getGuiLeft() + 318 <= screen.width
              && screen.getGuiTop() + 234 <= screen.height,
          "Service menu clipped at minimum GUI size");
      shot("ripperdoc-install-compact");
    }
    if (t == 488) {
      org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1280, 720);
      mc.options.guiScale().set(2);
      mc.resizeDisplay();
    }
    if (t == 490) {
      mc.player.closeContainer();
      server(
          p -> {
            var n = (CityResident) p.serverLevel().getEntity(fixer);
            p.teleportTo(p.serverLevel(), n.getX() - 2, n.getY(), n.getZ(), Set.of(), 0, 0);
            n.interact(p, net.minecraft.world.InteractionHand.MAIN_HAND);
          });
    }
    if (t == 520) {
      check(mc.screen instanceof FixerScreen, "Fixer screen missing");
      shot("afterlife-fixer");
    }
    if (t == 530) {
      mc.player.closeContainer();
      mc.options.hideGui = true;
      camera(130, 73, 20, 116, 70, 11);
    }
    if (t == 538) shot("clinic-large-rear");
    if (t == 540) {
      mc.player.closeContainer();
      mc.options.hideGui = true;
      server(
          p -> {
            p.closeContainer();
            var l = p.serverLevel();
            var generator = l.getChunkSource().getGenerator();
            var structure =
                l.registryAccess()
                    .registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                    .get(
                        net.minecraft.resources.ResourceLocation.withDefaultNamespace(
                            "village_plains"));
            net.minecraft.world.level.levelgen.structure.StructureStart start = null;
            for (int seed = 0; seed < 256; seed++) {
              var candidate =
                  structure.generate(
                      l.registryAccess(),
                      generator,
                      generator.getBiomeSource(),
                      l.getChunkSource().randomState(),
                      l.getStructureManager(),
                      seed * 9973L,
                      new net.minecraft.world.level.ChunkPos(24, 24),
                      0,
                      l,
                      b -> true);
              var city =
                  candidate.getPieces().stream()
                      .filter(
                          piece ->
                              piece
                                      instanceof
                                      net.minecraft.world.level.levelgen.structure
                                                  .PoolElementStructurePiece
                                              v
                                  && v.getElement() instanceof CityPoolElement)
                      .map(Object::toString)
                      .toList();
              if (city.stream().anyMatch(n -> n.contains("afterlife"))
                  && city.stream().anyMatch(n -> n.contains("clinic_small"))) {
                start = candidate;
                System.out.println("Native village layout seed " + seed + ": " + city);
                break;
              }
            }
            check(start != null, "No review village found");
            int dy = 64 - start.getPieces().getFirst().getBoundingBox().minY();
            for (var piece : start.getPieces()) piece.move(0, dy, 0);
            var box = start.getBoundingBox();
            villageBox = box;
            for (int cx = (box.minX() - 32) >> 4; cx <= (box.maxX() + 32) >> 4; cx++)
              for (int cz = (box.minZ() - 32) >> 4; cz <= (box.maxZ() + 32) >> 4; cz++) {
                l.setChunkForced(cx, cz, true);
                l.getChunk(cx, cz);
              }
            // A flat review terrain patch. Actual village pieces are placed by StructureStart,
            // using the same chunk clipping and pool elements as normal generation.
            for (int x = box.minX() - 32; x <= box.maxX() + 32; x++)
              for (int z = box.minZ() - 32; z <= box.maxZ() + 32; z++) {
                l.setBlock(new BlockPos(x, 62, z), Blocks.STONE.defaultBlockState(), 2);
                l.setBlock(new BlockPos(x, 63, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
              }
            for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++)
              for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                start.placeInChunk(
                    l,
                    l.structureManager(),
                    generator,
                    l.random,
                    new net.minecraft.world.level.levelgen.structure.BoundingBox(
                        cx * 16,
                        l.getMinBuildHeight(),
                        cz * 16,
                        cx * 16 + 15,
                        l.getMaxBuildHeight() - 1,
                        cz * 16 + 15),
                    new net.minecraft.world.level.ChunkPos(cx, cz));
              }
            var residents =
                l.getEntitiesOfClass(
                    CityResident.class,
                    new AABB(
                        box.minX(),
                        l.getMinBuildHeight(),
                        box.minZ(),
                        box.maxX() + 1,
                        l.getMaxBuildHeight(),
                        box.maxZ() + 1));
            check(
                residents.stream().filter(CityResident::fixer).count() >= 2,
                "Generated village missing fixers");
            check(
                residents.stream().anyMatch(n -> !n.fixer()), "Generated village missing doctors");
          });
    }
    if (t == 600) {
      check(villageBox != null, "Village setup incomplete");
      var b = villageBox;
      double x = (b.minX() + b.maxX()) / 2., z = (b.minZ() + b.maxZ()) / 2.;
      camera(x - 43, 99, z - 43, x, 65, z);
    }
    if (t == 650) shot("village-services");
    if (t == 680) {
      Files.writeString(
          OUT.resolve("city-passed.txt"),
          "PASS: native interiors/exteriors, NPC interactions, wallet synchronization, purchase,"
              + " installation, fixer dossier and actual vanilla village chunk placement with"
              + " doctors/fixers.\n");
      finished = true;
      mc.stop();
    }
  }
}
