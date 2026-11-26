package dev.cybernetics2077.city;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public final class FixerMenu extends AbstractContainerMenu {
  private final int resident;
  private final ServiceData data;
  private final Player owner;
  private CompoundTag board = new CompoundTag(), sent;
  private List<BountyRules.Offer> shownOffers = List.of();
  private int acceptBase = 100;
  private long lastRefresh = Long.MIN_VALUE / 2;

  public FixerMenu(int id, Inventory inv, RegistryFriendlyByteBuf b) {
    this(id, inv, b.readVarInt());
  }

  public FixerMenu(int id, Inventory inv, int resident) {
    super(CityContent.FIXER_MENU.get(), id);
    this.resident = resident;
    owner = inv.player;
    data = new ServiceData(owner);
    if (owner instanceof ServerPlayer p)
      data.set(5, ClinicDirectory.get(p.server).nearest(p).isPresent() ? 1 : 0);
    addDataSlots(data);
  }

  public long eddies() {
    return data.balance();
  }

  public int streetCred() {
    return data.get(4);
  }

  public int stage() {
    return data.get(7);
  }

  public boolean hasRoute() {
    return data.get(5) == 1;
  }

  public int result() {
    return data.get(6);
  }

  public CompoundTag board() {
    return board.copy();
  }

  public void board(CompoundTag value) {
    if (owner.level().isClientSide) board = value.copy();
  }

  private void refresh(boolean force) {
    if (!(owner instanceof ServerPlayer p) || !stillValid(p)) return;
    long now = p.server.overworld().getGameTime();
    if (!force && now - lastRefresh < 20) return;
    lastRefresh = now;
    var fixer = (CityResident) p.level().getEntity(resident);
    var current = BountyContracts.offers(p, fixer);
    if (!current.equals(shownOffers)) {
      shownOffers = current;
      acceptBase = 100 + p.getRandom().nextInt(1000000) * 4;
    }
    board = BountyContracts.board(p, fixer);
    board.putInt("acceptBase", acceptBase);
    if (!board.equals(sent)) {
      PacketDistributor.sendToPlayer(p, new BountyNetwork.State(containerId, board.copy()));
      sent = board.copy();
    }
  }

  @Override
  public void broadcastChanges() {
    super.broadcastChanges();
    refresh(false);
  }

  @Override
  public boolean clickMenuButton(Player p, int button) {
    if (p != owner
        || !(p instanceof ServerPlayer server)
        || !stillValid(p)
        || !getCarried().isEmpty()) return false;
    var fixer = (CityResident) p.level().getEntity(resident);
    int result;
    if (button == 1) result = DeliveryContracts.issue(server, fixer);
    else if (button == 2) result = BountyContracts.claim(server, fixer);
    else if (button == 3) result = BountyContracts.abandon(server, fixer) ? 0 : 10;
    else if (button == 4) result = BountyContracts.reissue(server, fixer);
    else if (button >= acceptBase && button < acceptBase + 3 && shownOffers.size() == 3) {
      var offered = shownOffers.get(button - acceptBase);
      result = BountyContracts.accept(server, fixer, offered);
    } else return false;
    data.set(6, result);
    sent = null;
    refresh(true);
    super.broadcastChanges();
    return result == 0;
  }

  @Override
  public boolean stillValid(Player p) {
    return p.isAlive()
        && p.level().getEntity(resident) instanceof CityResident n
        && n.isAlive()
        && n.fixer()
        && p.distanceToSqr(n) <= 64;
  }

  @Override
  public ItemStack quickMoveStack(Player p, int i) {
    return ItemStack.EMPTY;
  }
}
