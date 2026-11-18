package dev.cybernetics2077.city;

import dev.cybernetics2077.bench.CyberneticsBenchMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class RipperdocMenu extends CyberneticsBenchMenu {
  private final int resident;
  private final ServiceData data;

  public RipperdocMenu(int id, Inventory inv, RegistryFriendlyByteBuf b) {
    this(id, inv, b.readVarInt());
  }

  public RipperdocMenu(int id, Inventory inv, int resident) {
    super(CityContent.RIPPER_MENU.get(), id, inv, ContainerLevelAccess.NULL, 18, true);
    this.resident = resident;
    data = new ServiceData(inv.player);
    addDataSlots(data);
  }

  public int tab() {
    return data == null ? 0 : data.get(5);
  }

  public int result() {
    return data.get(6);
  }

  public long eddies() {
    return data.balance();
  }

  public int streetCred() {
    return data.get(4);
  }

  @Override
  protected boolean installationActive() {
    return tab() == 1;
  }

  @Override
  public boolean stillValid(Player p) {
    return p.isAlive()
        && p.level().getEntity(resident) instanceof CityResident npc
        && npc.isAlive()
        && !npc.fixer()
        && p.distanceToSqr(npc) <= 64;
  }

  @Override
  public void clicked(int slot, int drag, ClickType type, Player p) {
    if (installationActive() && stillValid(p)) super.clicked(slot, drag, type, p);
  }

  @Override
  public ItemStack quickMoveStack(Player p, int slot) {
    return installationActive() && stillValid(p) ? super.quickMoveStack(p, slot) : ItemStack.EMPTY;
  }

  @Override
  public boolean clickMenuButton(Player p, int button) {
    if (!stillValid(p) || !getCarried().isEmpty() || p.level().isClientSide) return false;
    if (button == 10 || button == 11) {
      data.set(5, button - 10);
      data.set(6, 0);
      broadcastChanges();
      return true;
    }
    if (tab() != 0 || button < 1000 || button >= 1000 + CyberwareCatalogue.offers().size())
      return false;
    var offer = CyberwareCatalogue.offers().get(button - 1000);
    var wallet = p.getData(CityContent.ACCOUNT);
    if (wallet.streetCred() < offer.requiredStreetCred()) {
      data.set(6, 4);
      broadcastChanges();
      return false;
    }
    if (wallet.eddies() < offer.price()) {
      data.set(6, 1);
      broadcastChanges();
      return false;
    }
    // All catalogue entries stack to one; reserve a free slot before charging.
    if (p.getInventory().getFreeSlot() < 0) {
      data.set(6, 2);
      broadcastChanges();
      return false;
    }
    var item = new ItemStack(offer.item());
    if (!wallet.spend(offer.price())) return false;
    if (!p.getInventory().add(item)) {
      wallet.credit(offer.price());
      data.set(6, 2);
      return false;
    }
    data.set(6, 3);
    p.getInventory().setChanged();
    broadcastChanges();
    return true;
  }
}
