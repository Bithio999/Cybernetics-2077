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
    addDataSlots(data);
  }

  public long eddies() {
    return data.balance();
  }

  public int streetCred() {
    return data.get(4);
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
