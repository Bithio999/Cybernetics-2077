package dev.cybernetics2077.bench;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.scanner.ScannerNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Native server-owned slots provide pickup, swap, shift-click and cursor recovery. */
public class CyberneticsBenchMenu extends AbstractContainerMenu {
  private final ContainerLevelAccess access;
  private final CyberwareInventory implants;
  private final Player owner;
  private final ContainerData capacity;

  public CyberneticsBenchMenu(int id, Inventory inv) {
    this(id, inv, ContainerLevelAccess.NULL);
  }

  public CyberneticsBenchMenu(int id, Inventory inv, ContainerLevelAccess access) {
    this(Cybernetics2077.BENCH_MENU.get(), id, inv, access, 0);
  }

  protected CyberneticsBenchMenu(
      MenuType<?> type, int id, Inventory inv, ContainerLevelAccess access, int offset) {
    this(type, id, inv, access, offset, false);
  }

  protected CyberneticsBenchMenu(
      MenuType<?> type,
      int id,
      Inventory inv,
      ContainerLevelAccess access,
      int offset,
      boolean compact) {
    super(type, id);
    this.access = access;
    owner = inv.player;
    implants =
        owner.level().isClientSide
            ? new CyberwareInventory()
            : owner.getData(Cybernetics2077.CYBERWARE);
    capacity =
        owner.level().isClientSide
            ? new SimpleContainerData(2)
            : new ContainerData() {
              public int get(int i) {
                return StatWords.word(CyberwareCapacity.limit(owner), i);
              }

              public void set(int i, int value) {}

              public int getCount() {
                return 2;
              }
            };
    addDataSlots(capacity);
    for (var category : CyberwareCategory.values())
      for (int n = 0; n < category.sockets; n++) {
        int index = category.firstSlot() + n;
        addSlot(
            new SlotItemHandler(
                implants,
                index,
                x(category) + 76 + n * 18,
                (compact ? 25 + (category.ordinal() / 2) * 18 : y(category)) + offset) {
              @Override
              public boolean isActive() {
                return installationActive();
              }

              @Override
              public boolean mayPickup(Player player) {
                return installationActive()
                    && capacityAfter(index, ItemStack.EMPTY)
                        >= capacityUsed()
                            - (getItem().getItem() instanceof CyberwareItem item
                                ? item.capacityCost()
                                : 0);
              }

              @Override
              public boolean mayPlace(ItemStack stack) {
                return installationActive() && fits(index, stack);
              }

              @Override
              public int getMaxStackSize() {
                return 1;
              }
            });
      }
    for (int row = 0; row < 3; row++)
      for (int col = 0; col < 9; col++)
        addSlot(
            inventorySlot(
                inv, col + row * 9 + 9, 84 + col * 18, (compact ? 137 : 119 + offset) + row * 18));
    for (int col = 0; col < 9; col++)
      addSlot(inventorySlot(inv, col, 84 + col * 18, (compact ? 195 : 177 + offset)));
  }

  protected boolean installationActive() {
    return true;
  }

  private Slot inventorySlot(Inventory inv, int index, int x, int y) {
    return new Slot(inv, index, x, y) {
      @Override
      public boolean isActive() {
        return installationActive();
      }

      @Override
      public boolean mayPickup(Player p) {
        return installationActive();
      }

      @Override
      public boolean mayPlace(ItemStack stack) {
        return installationActive();
      }
    };
  }

  public static int x(CyberwareCategory c) {
    return 10 + (c.ordinal() % 2) * 164;
  }

  public static int y(CyberwareCategory c) {
    return 25 + (c.ordinal() / 2) * 18;
  }

  public int capacityUsed() {
    return implants.capacityUsed();
  }

  public int capacityLimit() {
    return StatWords.combine(capacity.get(0), capacity.get(1));
  }

  private int capacityAfter(int index, ItemStack stack) {
    int limit = capacityLimit();
    if (index == CyberwareCategory.OPERATING_SYSTEM.firstSlot()) {
      var old = implants.getStackInSlot(index);
      int before =
          old.getItem() instanceof AugmentItem a && a.augment() == Augment.CHROME_COMPRESSOR
              ? (a.tier().level == 4 ? 40 : 70)
              : 0;
      int after =
          stack.getItem() instanceof AugmentItem a && a.augment() == Augment.CHROME_COMPRESSOR
              ? (a.tier().level == 4 ? 40 : 70)
              : 0;
      limit = limit - before + after;
    }
    return limit;
  }

  public boolean fits(int index, ItemStack stack) {
    if (!implants.isItemValid(index, stack) || stack.isEmpty()) return false;
    int previous =
        implants.getStackInSlot(index).getItem() instanceof CyberwareItem i ? i.capacityCost() : 0;
    return (long) capacityUsed() - previous + ((CyberwareItem) stack.getItem()).capacityCost()
        <= capacityAfter(index, stack);
  }

  public boolean hasDeck() {
    return implants.getStackInSlot(CyberwareCategory.OPERATING_SYSTEM.firstSlot()).getItem()
        instanceof dev.cybernetics2077.deck.CyberdeckItem;
  }

  @Override
  public boolean clickMenuButton(Player p, int button) {
    if (button != 0
        || !hasDeck()
        || !stillValid(p)
        || !getCarried().isEmpty()
        || !(p instanceof ServerPlayer server)) return false;
    var deck = implants.getStackInSlot(CyberwareCategory.OPERATING_SYSTEM.firstSlot());
    server.openMenu(
        new net.minecraft.world.SimpleMenuProvider(
            (id, inv, owner) -> new dev.cybernetics2077.deck.CyberdeckMenu(id, inv, access, deck),
            net.minecraft.network.chat.Component.translatable("cybernetics2077.deck.title")),
        data -> ItemStack.STREAM_CODEC.encode(data, deck));
    return true;
  }

  @Override
  public boolean stillValid(Player player) {
    return player.isAlive() && stillValid(access, player, Cybernetics2077.BENCH.get());
  }

  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
    var slot = slots.get(index);
    if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
    var stack = slot.getItem();
    var original = stack.copy();
    if (index < 21) {
      if (!moveItemStackTo(stack, 21, 57, true)) return ItemStack.EMPTY;
    } else {
      if (!(stack.getItem() instanceof CyberwareItem item)) return ItemStack.EMPTY;
      int start = item.category().firstSlot();
      if (!moveItemStackTo(stack, start, start + item.category().sockets, false))
        return ItemStack.EMPTY;
    }
    if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
    else slot.setChanged();
    slot.onTake(player, stack);
    return original;
  }

  @Override
  public void broadcastChanges() {
    super.broadcastChanges();
    if (owner instanceof ServerPlayer p) ScannerNetwork.syncIfChanged(p);
  }

  @Override
  public void removed(Player player) {
    super.removed(player);
    if (player instanceof ServerPlayer p) ScannerNetwork.sync(p);
  }
}
