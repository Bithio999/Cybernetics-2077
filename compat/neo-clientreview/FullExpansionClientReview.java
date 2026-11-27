package dev.cybernetics2077.compat.review;

import dev.cybernetics2077.*;
import dev.cybernetics2077.client.ui.*;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.mob.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Review-only native role meshes, item icons, synchronized reserve and hostile systems. */
public final class FullExpansionClientReview {
  private static volatile Throwable failure;
  private static final List<Integer> ids = new ArrayList<>();
  private static volatile int activeId;

  private static void server(
      java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> work) {
    var mc = Minecraft.getInstance();
    mc.getSingleplayerServer()
        .execute(
            () -> {
              try {
                work.accept(
                    mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()));
              } catch (Throwable ex) {
                failure = ex;
              }
            });
  }

  private static void shot(Path out, String name) {
    Screenshot.grab(
        out.toFile(), name + ".png", Minecraft.getInstance().getMainRenderTarget(), m -> {});
  }

  public static void tick(int t, Path out) throws Exception {
    var mc = Minecraft.getInstance();
    if (failure != null) throw new IllegalStateException("Expansion server fixture", failure);
    if (t == 0) {
      Files.createDirectories(out);
      mc.player.closeContainer();
      mc.setScreen(null);
      server(
          p -> {
            p.setGameMode(GameType.SURVIVAL);
            p.teleportTo(0, 90, 0);
            p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(5000);
            p.setHealth(5000);
            p.getAbilities().mayfly = true;
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            p.serverLevel().setDayTime(6000);
            p.serverLevel()
                .getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(80))
                .forEach(Entity::discard);
            for (int x = -12; x <= 12; x++)
              for (int z = -16; z <= 32; z++) {
                p.serverLevel()
                    .setBlock(new BlockPos(x, 89, z), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
                for (int y = 90; y < 98; y++)
                  p.serverLevel()
                      .setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
              }
            for (int x = -12; x <= 12; x++)
              for (int y = 90; y < 97; y++)
                p.serverLevel()
                    .setBlock(
                        new BlockPos(x, y, 30), Blocks.DEEPSLATE_TILES.defaultBlockState(), 3);
            for (int role = 0; role < 3; role++) {
              long seed = 0;
              while (CyberpsychoLoadouts.forSeed(4, seed).role().ordinal() != role) seed++;
              var m =
                  dev.cybernetics2077.city.CityContent.CYBERPSYCHO.get().create(p.serverLevel());
              m.configure(4, seed);
              m.moveTo((role - 1) * 2.5, 90, 5, 180, 0);
              m.yBodyRot = m.yHeadRot = 180;
              m.setNoAi(true);
              p.serverLevel().addFreshEntity(m);
              ids.add(m.getId());
            }
            var inv = p.getData(Cybernetics2077.CYBERWARE);
            for (int i = 0; i < inv.getSlots(); i++) inv.setStackInSlot(i, ItemStack.EMPTY);
            inv.setStackInSlot(
                CyberwareCategory.SKELETON.firstSlot(),
                new ItemStack(Cybernetics2077.augment(Augment.TITANIUM_BONES, 5)));
            p.getData(Cybernetics2077.CARRY_RESERVE)
                .setStackInSlot(0, new ItemStack(Items.DIAMOND, 4));
          });
    }
    if (t == 15) {
      mc.player.setYRot(0);
      mc.player.setXRot(4);
      mc.options.hideGui = true;
    }
    if (t == 25) shot(out, "cyberpsycho-roles-028");
    if (t == 30) {
      mc.options.hideGui = false;
      server(
          p ->
              p.openMenu(
                  new SimpleMenuProvider(
                      (id, inv, owner) -> new CarryReserveMenu(id, inv),
                      net.minecraft.network.chat.Component.literal("Carry Reserve"))));
    }
    if (t == 50) {
      if (!(mc.screen instanceof CarryReserveScreen)
          || !mc.player.containerMenu.getSlot(26).mayPlace(new ItemStack(Items.STONE)))
        throw new IllegalStateException("Tier5 reserve UI did not synchronize");
      shot(out, "titanium-reserve-028");
    }
    if (t == 60) {
      mc.player.closeContainer();
      mc.setScreen(new Icons());
    }
    if (t == 75) shot(out, "expansion-icons-028");
    if (t == 80) {
      mc.setScreen(null);
      server(
          p -> {
            for (int id : ids) {
              var m = (Cyberpsycho) p.serverLevel().getEntity(id);
              if (m.role() == CyberpsychoLoadouts.Role.NETRUNNER) {
                m.setNoAi(false);
                m.setTarget(p);
                activeId = id;
              } else m.discard();
            }
            p.teleportTo(0, 90, -8);
          });
    }
    if (t == 180) {
      server(
          p -> {
            var m = (Cyberpsycho) p.serverLevel().getEntity(activeId);
            if (m == null
                || !MobImplants.hasPrograms(m)
                || !MobImplants.state(m).contains("programIndex"))
              throw new IllegalStateException(
                  "Native netrunner never attempted an installed quickhack");
          });
    }
    if (t == 195) {
      shot(out, "netrunner-combat-028");
      server(
          p -> {
            p.serverLevel()
                .getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(80))
                .forEach(Entity::discard);
            long seed = 0;
            while (CyberpsychoLoadouts.forSeed(4, seed).role() != CyberpsychoLoadouts.Role.BLITZ)
              seed++;
            var m = dev.cybernetics2077.city.CityContent.CYBERPSYCHO.get().create(p.serverLevel());
            m.configure(4, seed);
            m.moveTo(0, 90, 12, 180, 0);
            p.serverLevel().addFreshEntity(m);
            m.setTarget(p);
            activeId = m.getId();
          });
    }
    if (t == 215) {
      mc.options.hideGui = true;
      var mob = mc.level.getEntity(activeId);
      if (!(mob instanceof Mob m) || !dev.cybernetics2077.temporal.Sandevistan.active(m))
        throw new IllegalStateException(
            "NPC Sandevistan activation was not synchronized to native client");
    }
    if (t == 225) {
      if (dev.cybernetics2077.client.temporal.SandevistanTrails.lastActors < 1
          || dev.cybernetics2077.client.temporal.SandevistanTrails.lastEchoes < 1)
        throw new IllegalStateException("Active NPC did not produce rendered afterimages");
      shot(out, "blitz-time-bubble-028");
    }
    if (t == 235) {
      mc.options.hideGui = false;
      Files.writeString(
          out.resolve("passed.txt"),
          "PASS: three real role models; expansion icons; native synchronized tier5 carry reserve;"
              + " real NPC installed-program uploads; synchronized NPC Sandevistan field and"
              + " rendered trails.\n");
      mc.stop();
    }
  }

  private static final class Icons extends net.minecraft.client.gui.screens.Screen {
    Icons() {
      super(net.minecraft.network.chat.Component.literal("Expansion"));
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float partial) {
      g.fill(0, 0, width, height, 0xff0a131b);
      g.drawCenteredString(
          font, "CYBERNETICS 2077 // EXPANDED CYBERWARE", width / 2, 12, 0xffa6e8e5);
      int i = 0;
      for (var a : Augment.values())
        if (ExpansionImplants.added(a)) {
          int gap = Math.min(34, (width - 20) / 12),
              x = width / 2 - 6 * gap + (i % 12) * gap,
              y = 35 + (i / 12) * 36;
          g.fill(x - 2, y - 2, x + 30, y + 30, 0xff18272f);
          g.pose().pushPose();
          g.pose().translate(x, y, 0);
          g.pose().scale(1.6f, 1.6f, 1);
          g.renderItem(new ItemStack(Cybernetics2077.augment(a, 5)), 0, 0);
          g.pose().popPose();
          i++;
        }
    }
  }
}
