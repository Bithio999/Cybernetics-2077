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
public final class CyberneticsBenchMenu extends AbstractContainerMenu {
  private final ContainerLevelAccess access;
  private final CyberwareInventory implants;
  private final Player owner;

  public CyberneticsBenchMenu(int id, Inventory inv) {
    this(id, inv, ContainerLevelAccess.NULL);
  }

  public CyberneticsBenchMenu(int id, Inventory inv, ContainerLevelAccess access) {
    super(Cybernetics2077.BENCH_MENU.get(), id);
    this.access = access;
    owner = inv.player;
    implants =
        owner.level().isClientSide
            ? new CyberwareInventory()
            : owner.getData(Cybernetics2077.CYBERWARE);
    for (var category : CyberwareCategory.values())
      for (int n = 0; n < category.sockets; n++) {
        int index = category.firstSlot() + n;
        addSlot(
            new SlotItemHandler(implants, index, x(category) + 76 + n * 18, y(category)) {
              @Override
              public boolean mayPlace(ItemStack stack) {
                return fits(index, stack);
              }

              @Override
              public int getMaxStackSize() {
                return 1;
              }
            });
      }
    for (int row = 0; row < 3; row++)
      for (int col = 0; col < 9; col++)
        addSlot(new Slot(inv, col + row * 9 + 9, 84 + col * 18, 143 + row * 18));
    for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col, 84 + col * 18, 205));
  }

  public static int x(CyberwareCategory c) {
    return 10 + (c.ordinal() % 2) * 164;
  }

  public static int y(CyberwareCategory c) {
    return 31 + (c.ordinal() / 2) * 21;
  }

  public int capacityUsed() {
    return implants.capacityUsed();
  }

  public boolean fits(int index, ItemStack stack) {
    if (!implants.isItemValid(index, stack) || stack.isEmpty()) return false;
    int previous =
        implants.getStackInSlot(index).getItem() instanceof CyberwareItem i ? i.capacityCost() : 0;
    return (long) capacityUsed() - previous + ((CyberwareItem) stack.getItem()).capacityCost()
        <= Cybernetics2077.CAPACITY.get();
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
        data ->
            data.writeVarInt(
                ((dev.cybernetics2077.deck.CyberdeckItem) deck.getItem()).tier().level));
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
    if (!slot.hasItem()) return ItemStack.EMPTY;
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
