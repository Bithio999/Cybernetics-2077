package dev.cybernetics2077.city;

import dev.cybernetics2077.bench.CyberneticsBenchMenu;
import dev.cybernetics2077.cyberware.CyberwareItem;
import dev.cybernetics2077.mob.DamagedCyberneticPart;
import dev.cybernetics2077.mob.IconicRequisition;
import java.util.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class RipperdocMenu extends CyberneticsBenchMenu {
  private final int resident;
  private final ServiceData data;
  private final Player owner;
  // Vanilla menu data are signed shorts: four lanes for the day, one for remaining ticks,
  // then 16-bit stock masks. Only the server chooses items; the seed is never sent to clients.
  private final SimpleContainerData stock;
  private long lastDay = Long.MIN_VALUE;
  private int lastCred = -1;

  public RipperdocMenu(int id, Inventory inv, RegistryFriendlyByteBuf b) {
    this(id, inv, b.readVarInt());
  }

  public RipperdocMenu(int id, Inventory inv, int resident) {
    super(CityContent.RIPPER_MENU.get(), id, inv, ContainerLevelAccess.NULL, 18, true);
    this.resident = resident;
    owner = inv.player;
    data = new ServiceData(inv.player);
    addDataSlots(data);
    stock = new SimpleContainerData(5 + (CyberwareCatalogue.offers().size() + 15) / 16);
    addDataSlots(stock);
    refreshStock();
  }

  private void refreshStock() {
    if (stock == null || !(owner instanceof ServerPlayer p)) return;
    long time = p.server.overworld().getDayTime();
    long day = Math.floorDiv(time, 24000);
    int cred = p.getData(CityContent.ACCOUNT).streetCred();
    stock.set(4, (int) (24000 - Math.floorMod(time, 24000)));
    if (day == lastDay && cred == lastCred) return;
    if (!(p.level().getEntity(resident) instanceof CityResident npc)) return;
    var all = CyberwareCatalogue.offers();
    int[] tiers = all.stream().mapToInt(CyberwareCatalogue.Offer::tier).toArray();
    int optics =
        java.util.stream.IntStream.range(0, all.size())
            .filter(i -> all.get(i).item() == dev.cybernetics2077.Cybernetics2077.OPTICS.get())
            .findFirst()
            .orElse(-1);
    long seed =
        DailyStock.mix(
            p.server.overworld().getSeed()
                ^ npc.getUUID().getMostSignificantBits()
                ^ Long.rotateLeft(npc.getUUID().getLeastSignificantBits(), 19)
                ^ p.level().dimension().location().hashCode());
    var chosen = DailyStock.select(tiers, seed, day, cred, optics);
    for (int i = 0; i < 4; i++) stock.set(i, (int) (day >>> (i * 16)) & 65535);
    for (int word = 5; word < stock.getCount(); word++) {
      int bits = 0;
      for (int bit = 0; bit < 16; bit++) if (chosen.get((word - 5) * 16 + bit)) bits |= 1 << bit;
      stock.set(word, bits);
    }
    lastDay = day;
    lastCred = cred;
  }

  public boolean stocked(int index) {
    refreshStock();
    return index >= 0
        && index < CyberwareCatalogue.offers().size()
        && (stock.get(5 + index / 16) & (1 << (index % 16))) != 0;
  }

  public long stockDay() {
    long day = 0;
    for (int i = 0; i < 4; i++) day |= (long) (stock.get(i) & 65535) << (i * 16);
    return day;
  }

  public int restockTicks() {
    return stock.get(4);
  }

  @Override
  public void broadcastChanges() {
    refreshStock();
    super.broadcastChanges();
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

  public int parts(int tier) {
    int count = 0;
    for (int i = 0; i < 36; i++) {
      var stack = owner.getInventory().getItem(i);
      if (stack.getItem() instanceof DamagedCyberneticPart part && part.tier().level == tier)
        count += stack.getCount();
    }
    return count;
  }

  private int partSlot(Player p, int tier) {
    for (int i = 0; i < 36; i++)
      if (p.getInventory().getItem(i).getItem() instanceof DamagedCyberneticPart part
          && part.tier().level == tier) return i;
    return -1;
  }

  public int requisitions(int tier) {
    int count = 0;
    for (int i = 0; i < 36; i++) {
      var stack = owner.getInventory().getItem(i);
      if (stack.getItem() instanceof IconicRequisition chip && chip.tier().level == tier)
        count += stack.getCount();
    }
    return count;
  }

  private int requisitionSlot(Player p, int tier) {
    for (int i = 0; i < 36; i++)
      if (p.getInventory().getItem(i).getItem() instanceof IconicRequisition chip
          && chip.tier().level == tier) return i;
    return -1;
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
    boolean salvage = button >= 2000;
    int index = button - (salvage ? 2000 : 1000);
    if (tab() != 0 || index < 0 || index >= CyberwareCatalogue.offers().size()) return false;
    var offer = CyberwareCatalogue.offers().get(index);
    var wallet = p.getData(CityContent.ACCOUNT);
    if (wallet.streetCred() < offer.requiredStreetCred()) {
      data.set(6, 4);
      broadcastChanges();
      return false;
    }
    if (!stocked(index)) {
      data.set(6, 5);
      broadcastChanges();
      return false;
    }
    // Neither full-price nor salvage packets can bypass an Iconic's authorization.
    int paymentSlot =
        offer.iconic()
            ? requisitionSlot(p, offer.tier())
            : salvage ? partSlot(p, offer.tier()) : -1;
    if (offer.iconic() && (salvage || paymentSlot < 0)) {
      data.set(6, 7);
      broadcastChanges();
      return false;
    }
    if (salvage && (!(offer.item() instanceof CyberwareItem) || paymentSlot < 0)) {
      data.set(6, 6);
      broadcastChanges();
      return false;
    }
    int price = salvage ? DamagedCyberneticPart.price(offer) : offer.price();
    if (wallet.eddies() < price) {
      data.set(6, 1);
      broadcastChanges();
      return false;
    }
    // All catalogue entries stack to one; reserve a free slot before charging.
    boolean partFreesSlot =
        paymentSlot >= 0 && p.getInventory().getItem(paymentSlot).getCount() == 1;
    if (p.getInventory().getFreeSlot() < 0 && !partFreesSlot) {
      data.set(6, 2);
      broadcastChanges();
      return false;
    }
    var item = new ItemStack(offer.item());
    if (!wallet.spend(price)) return false;
    ItemStack payment =
        paymentSlot >= 0 ? p.getInventory().removeItem(paymentSlot, 1) : ItemStack.EMPTY;
    if (!p.getInventory().add(item)) {
      wallet.credit(price);
      if (!payment.isEmpty()) {
        var rest = p.getInventory().getItem(paymentSlot);
        if (rest.isEmpty()) p.getInventory().setItem(paymentSlot, payment);
        else rest.grow(1);
      }
      data.set(6, 2);
      broadcastChanges();
      return false;
    }
    data.set(6, 3);
    p.getInventory().setChanged();
    broadcastChanges();
    return true;
  }
}
