package dev.cybernetics2077.compat.review;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.city.*;
import dev.cybernetics2077.client.ui.*;
import dev.cybernetics2077.combat.*;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.temporal.*;
import java.nio.file.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Native shop purchase, install, activation and recharge; test save only. */
public final class DavidClientReview {
  private static long day;

  private static Object field(Object o, String name) throws Exception {
    var f = o.getClass().getDeclaredField(name);
    f.setAccessible(true);
    return f.get(o);
  }

  private static int index() {
    for (int i = 0; i < CyberwareCatalogue.offers().size(); i++)
      if (CyberwareCatalogue.offers().get(i).item()
          == Cybernetics2077.augment(Augment.DAVIDS_SANDEVISTAN, 5)) return i;
    throw new IllegalStateException("Missing David offer");
  }

  public static void tick(int t, Path out) throws Exception {
    var mc = Minecraft.getInstance();
    if (t == 0)
      mc.getSingleplayerServer()
          .execute(
              () -> {
                var p = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                day = p.server.overworld().getDayTime();
                p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                var inv = p.getData(Cybernetics2077.CYBERWARE);
                for (int i = 0; i < inv.getSlots(); i++) inv.setStackInSlot(i, ItemStack.EMPTY);
                p.getInventory().clearContent();
                p.getInventory()
                    .setItem(0, new ItemStack(Cybernetics2077.ICONIC_REQUISITIONS.get(4).get()));
                p.getInventory()
                    .setItem(1, new ItemStack(Cybernetics2077.augment(Augment.MILITECH_APOGEE, 5)));
                var wallet = p.getData(CityContent.ACCOUNT);
                wallet.reputation(50);
                wallet.spend((int) wallet.eddies());
                wallet.credit(150000);
                var state = AugmentRuntime.state(p);
                state.sandyEnd = 0;
                state.sandyCharge = 1;
                state.sandyClock = AugmentRuntime.now(p);
                state.sandyRearm = 0;
                var npc = CityContent.RIPPERDOC.get().create(p.serverLevel());
                npc.setPos(p.position().add(2, 0, 0));
                npc.setNoAi(true);
                p.serverLevel().addFreshEntity(npc);
                var check = new RipperdocMenu(1, p.getInventory(), npc.getId());
                for (int d = 0; d < 512 && !check.stocked(index()); d++)
                  p.server.overworld().setDayTime(d * 24000L);
                if (!check.stocked(index())) throw new IllegalStateException("David not stocked");
                p.openMenu(
                    new net.minecraft.world.SimpleMenuProvider(
                        (id, inventory, owner) -> new RipperdocMenu(id, inventory, npc.getId()),
                        Component.literal("Ripperdoc")),
                    b -> b.writeVarInt(npc.getId()));
              });
    if (t == 20) {
      if (!(mc.screen instanceof RipperdocScreen))
        throw new IllegalStateException("Native ripperdoc did not open");
      ((EditBox) field(mc.screen, "search")).setValue("David");
    }
    if (t == 30) shot(out, "david-shop");
    if (t == 35) {
      var buttons = (java.util.List<?>) field(mc.screen, "buys");
      var buy = (AbstractButton) buttons.getFirst();
      if (!buy.active) throw new IllegalStateException("Valid upgrade disabled");
      buy.onPress();
    }
    if (t == 50) {
      var menu = (RipperdocMenu) mc.player.containerMenu;
      if (menu.eddies() != 0
          || menu.requisitions(5) != 0
          || menu.prerequisites(CyberwareCatalogue.offers().get(index())) != 0
          || !mc.player
              .getInventory()
              .contains(new ItemStack(Cybernetics2077.augment(Augment.DAVIDS_SANDEVISTAN, 5))))
        throw new IllegalStateException("Native purchase/payment failed");
      ((AbstractButton) field(mc.screen, "install")).onPress();
    }
    if (t == 65) {
      var menu = mc.player.containerMenu;
      int slot = -1;
      for (int i = 0; i < menu.slots.size(); i++)
        if (menu.getSlot(i).getItem().is(Cybernetics2077.augment(Augment.DAVIDS_SANDEVISTAN, 5)))
          slot = i;
      if (slot < 0) throw new IllegalStateException("Purchased implant not in native menu");
      mc.gameMode.handleInventoryMouseClick(
          menu.containerId, slot, 0, ClickType.QUICK_MOVE, mc.player);
    }
    if (t == 80) {
      if (mc.player.containerMenu instanceof RipperdocMenu menu && menu.capacityUsed() != 60)
        throw new IllegalStateException("David failed to install");
      shot(out, "david-installed");
      mc.player.closeContainer();
      mc.setScreen(null);
    }
    if (t == 90)
      net.neoforged.neoforge.network.PacketDistributor.sendToServer(
          new CyberwareNetwork.Action(13));
    if (t == 105) {
      if (Sandevistan.model(mc.player) != SandevistanModel.DAVID || !Sandevistan.active(mc.player))
        throw new IllegalStateException("David model/activation not synchronized");
      double pace =
          Sandevistan.environmentPace(
              mc.level, mc.player.getX(), mc.player.getY(), mc.player.getZ(), false);
      if (Math.abs(pace - .0625) > 1e-7)
        throw new IllegalStateException("Wrong native David field pace: " + pace);
      mc.options.keyDown.setDown(true);
    }
    if (t == 125) shot(out, "david-active");
    if (t == 130) {
      mc.options.keyDown.setDown(false);
      net.neoforged.neoforge.network.PacketDistributor.sendToServer(
          new CyberwareNetwork.Action(13));
    }
    if (t == 145) {
      if (Sandevistan.active(mc.player)) throw new IllegalStateException("David did not stop");
      shot(out, "david-recharge");
      mc.getSingleplayerServer()
          .execute(() -> mc.getSingleplayerServer().overworld().setDayTime(day));
      Files.writeString(
          out.resolve("david-passed.txt"),
          "PASS: native stock/search/acquire, eddies + tier 5 chip + Apogee consumed, native"
              + " installation at 60 capacity, DAVID HUD/model, 95% relative time bubble, early"
              + " stop and recharge.\n");
      mc.stop();
    }
  }

  private static void shot(Path out, String name) throws Exception {
    Files.createDirectories(out);
    Screenshot.grab(
        out.toFile(), name + ".png", Minecraft.getInstance().getMainRenderTarget(), m -> {});
  }
}
