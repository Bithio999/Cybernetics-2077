package dev.cybernetics2077.city;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class FixerMenu extends AbstractContainerMenu {
  private final int resident;
  private final ServiceData data;

  public FixerMenu(int id, Inventory inv, RegistryFriendlyByteBuf b) {
    this(id, inv, b.readVarInt());
  }

  public FixerMenu(int id, Inventory inv, int resident) {
    super(CityContent.FIXER_MENU.get(), id);
    this.resident = resident;
    data = new ServiceData(inv.player);
    if (inv.player instanceof net.minecraft.server.level.ServerPlayer p)
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

  @Override
  public boolean clickMenuButton(Player p, int button) {
    if (button != 1
        || !(p instanceof net.minecraft.server.level.ServerPlayer server)
        || !stillValid(p)
        || !getCarried().isEmpty()) return false;
    var fixer = (CityResident) p.level().getEntity(resident);
    int result = DeliveryContracts.issue(server, fixer);
    data.set(6, result);
    broadcastChanges();
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
