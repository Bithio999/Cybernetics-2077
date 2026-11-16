package dev.cybernetics2077.cyberware;

import dev.cybernetics2077.Cybernetics2077;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class CyberwareMenu extends AbstractContainerMenu {
  private final ContainerData data;

  public CyberwareMenu(int id, Inventory inventory) {
    this(id, inventory, new CyberwareInventory(), new SimpleContainerData(6));
  }

  public static CyberwareMenu server(int id, Inventory inventory) {
    var implants = inventory.player.getData(Cybernetics2077.CYBERWARE);
    return new CyberwareMenu(
        id,
        inventory,
        implants,
        new ContainerData() {
          public int get(int index) {
            int value =
                switch (index / 2) {
                  case 0 -> implants.capacityUsed();
                  case 1 -> Cybernetics2077.CAPACITY.get();
                  case 2 -> Math.round((float) inventory.player.getArmorValue() * 100);
                  default -> 0;
                };
            return StatWords.word(value, index % 2);
          }

          public void set(int index, int value) {}

          public int getCount() {
            return 6;
          }
        });
  }

  private CyberwareMenu(
      int id, Inventory inventory, CyberwareInventory implants, ContainerData data) {
    super(Cybernetics2077.MENU.get(), id);
    this.data = data;
    for (int n = 0; n < CyberwareCategory.totalSlots(); n++)
      addSlot(
          new SlotItemHandler(implants, n, 0, 0) {
            @Override
            public boolean mayPlace(ItemStack stack) {
              return false;
            }

            @Override
            public boolean mayPickup(Player player) {
              return false;
            }
          });
    addDataSlots(data);
  }

  private int stat(int index) {
    return StatWords.combine(data.get(index * 2), data.get(index * 2 + 1));
  }

  public int capacityUsed() {
    return stat(0);
  }

  public int capacityLimit() {
    return stat(1);
  }

  public double armor() {
    return stat(2) / 100d;
  }

  public int installedCount() {
    return (int) slots.stream().filter(Slot::hasItem).count();
  }

  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    return ItemStack.EMPTY;
  }

  @Override
  public boolean stillValid(Player player) {
    return player.isAlive();
  }

  // The menu is an equipment viewer. Reject forged clicks as well as normal pickup/quick-move.
  @Override
  public void clicked(int slot, int button, ClickType type, Player player) {}
}
