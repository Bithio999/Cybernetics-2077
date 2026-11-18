package dev.cybernetics2077.deck;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.cyberware.*;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.items.*;

/**
 * An installed-deck chip editor, opened from the cyberware viewer or bench. Removing/switching the
 * backing deck invalidates access.
 */
public final class CyberdeckMenu extends AbstractContainerMenu {
  private final ContainerLevelAccess access;
  private final ItemStack deck;
  private final Player owner;
  private final CyberwareTier tier;
  private final int programSlots;

  public CyberdeckMenu(int id, Inventory inv, net.minecraft.network.RegistryFriendlyByteBuf data) {
    this(id, inv, ContainerLevelAccess.NULL, ItemStack.STREAM_CODEC.decode(data));
  }

  public CyberdeckMenu(int id, Inventory inv, ContainerLevelAccess access, ItemStack deck) {
    super(Cybernetics2077.DECK_MENU.get(), id);
    this.access = access;
    this.deck = deck;
    this.owner = inv.player;
    this.tier = deck.getItem() instanceof CyberdeckItem item ? item.tier() : CyberwareTier.ONE;
    this.programSlots = tier.programSlots();
    var store =
        new ItemStackHandler(programSlots) {
          @Override
          public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof QuickhackItem program
                && program.tier().level <= tier.level
                && !java.util.stream.IntStream.range(0, programSlots)
                    .anyMatch(
                        i ->
                            i != slot
                                && getStackInSlot(i).getItem() instanceof QuickhackItem existing
                                && existing.kind() == program.kind());
          }

          @Override
          public int getSlotLimit(int slot) {
            return 1;
          }

          @Override
          protected void onContentsChanged(int slot) {
            if (!owner.level().isClientSide && !deck.isEmpty()) {
              var items = NonNullList.withSize(programSlots, ItemStack.EMPTY);
              for (int i = 0; i < programSlots; i++) items.set(i, getStackInSlot(i).copy());
              deck.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
            }
          }
        };
    for (int i = 0; i < programSlots; i++) {
      // Populate directly before exposing slots, without erasing another chip during
      // initialization.
      final int index = i;
      addSlot(
          new SlotItemHandler(store, i, slotX(i), slotY(i)) {
            @Override
            public boolean mayPlace(ItemStack stack) {
              return store.isItemValid(index, stack);
            }

            @Override
            public int getMaxStackSize() {
              return 1;
            }
          });
    }
    var initial = NonNullList.withSize(programSlots, ItemStack.EMPTY);
    deck.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(initial);
    // Snapshot all slots before initializing the handlers, then restore the entire component.
    for (int i = 0; i < programSlots; i++) store.setStackInSlot(i, initial.get(i).copy());
    if (!owner.level().isClientSide && !deck.isEmpty())
      deck.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(initial));
    for (int row = 0; row < 3; row++)
      for (int col = 0; col < 9; col++)
        addSlot(new Slot(inv, col + row * 9 + 9, 58 + col * 18, 134 + row * 18));
    for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col, 58 + col * 18, 196));
  }

  public CyberwareTier tier() {
    return tier;
  }

  public CyberdeckItem cyberdeck() {
    return (CyberdeckItem) deck.getItem();
  }

  public int programSlots() {
    return programSlots;
  }

  public static int slotX(int index) {
    return 112 + index % 4 * 34;
  }

  public static int slotY(int index) {
    return 62 + index / 4 * 34;
  }

  @Override
  public boolean stillValid(Player p) {
    return p.isAlive()
        && (p.level().isClientSide
            || p.getData(Cybernetics2077.CYBERWARE)
                        .getStackInSlot(CyberwareCategory.OPERATING_SYSTEM.firstSlot())
                    == deck
                && deck.getItem() instanceof CyberdeckItem)
        && stillValid(access, p, Cybernetics2077.BENCH.get());
  }

  @Override
  public ItemStack quickMoveStack(Player p, int index) {
    if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
    var slot = getSlot(index);
    if (!slot.hasItem()) return ItemStack.EMPTY;
    var stack = slot.getItem();
    var original = stack.copy();
    if (index < programSlots) {
      if (!moveItemStackTo(stack, programSlots, programSlots + 36, true)) return ItemStack.EMPTY;
    } else if (!(stack.getItem() instanceof QuickhackItem)
        || !moveItemStackTo(stack, 0, programSlots, false)) return ItemStack.EMPTY;
    if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
    else slot.setChanged();
    slot.onTake(p, stack);
    return original;
  }

  @Override
  public void broadcastChanges() {
    super.broadcastChanges();
    if (owner instanceof net.minecraft.server.level.ServerPlayer p)
      dev.cybernetics2077.combat.CyberwareRuntime.syncIfChanged(p);
  }
}
