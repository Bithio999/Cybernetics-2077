package dev.cybernetics2077.mob;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.cyberware.*;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.*;

/**
 * Vanilla mobs keep their own AI, equipment and spawn rules. No extra mob spawns or per-player
 * scans.
 */
@EventBusSubscriber(modid = Cybernetics2077.ID)
public final class EnhancedMobs {
  private static final String DATA = "cybernetics2077:enhanced";
  private static final Set<EntityType<?>> TYPES =
      Set.of(
          EntityType.ZOMBIE,
          EntityType.HUSK,
          EntityType.DROWNED,
          EntityType.SKELETON,
          EntityType.STRAY,
          EntityType.BOGGED,
          EntityType.PILLAGER,
          EntityType.VINDICATOR);

  public record Loadout(int count, double averageTier) {}

  public static boolean eligible(Mob mob, MobSpawnType reason) {
    return TYPES.contains(mob.getType())
        && (reason == MobSpawnType.NATURAL || reason == MobSpawnType.STRUCTURE);
  }

  public static boolean enhanced(LivingEntity mob) {
    return mob instanceof Mob
        && mob.getPersistentData().contains(DATA, 10)
        && mob.getPersistentData().getCompound(DATA).getBoolean("active");
  }

  private static CompoundTag state(Mob mob) {
    var root = mob.getPersistentData();
    if (!root.contains(DATA)) root.put(DATA, new CompoundTag());
    return root.getCompound(DATA);
  }

  public static Loadout loadout(Mob mob) {
    if (!enhanced(mob)) return new Loadout(0, 0);
    var inv = mob.getData(Cybernetics2077.CYBERWARE);
    int count = 0, sum = 0;
    for (int i = 0; i < inv.getSlots(); i++)
      if (inv.getStackInSlot(i).getItem() instanceof CyberwareItem item) {
        count++;
        sum += item.tier().level;
      }
    return new Loadout(count, count == 0 ? 0 : (double) sum / count);
  }

  public static int tier(Mob mob, Augment kind) {
    return AugmentRuntime.tier(mob, kind);
  }

  @SubscribeEvent
  public static void spawn(FinalizeSpawnEvent e) {
    var mob = e.getEntity();
    if (eligible(mob, e.getSpawnType())
        && !enhanced(mob)
        && mob.getRandom().nextDouble() < Cybernetics2077.ENHANCED_SPAWN_CHANCE.get())
      state(mob).putBoolean("pending", true);
  }

  @SubscribeEvent
  public static void join(EntityJoinLevelEvent e) {
    if (e.getLevel().isClientSide || !(e.getEntity() instanceof Mob mob)) return;
    if (mob.getPersistentData().getCompound(DATA).getBoolean("pending")) {
      double r = mob.getRandom().nextDouble();
      int tier = r < .62 ? 1 : r < .87 ? 2 : r < .97 ? 3 : r < .997 ? 4 : 5;
      double c = mob.getRandom().nextDouble();
      int count = c < .40 ? 1 : c < .72 ? 2 : c < .90 ? 3 : c < .97 ? 4 : c < .995 ? 5 : 6;
      install(mob, MobCyberwarePool.roll(tier, count, mob.getRandom()));
    } else if (enhanced(mob)) apply(mob, false);
  }

  /** Also used by the review fixture; rejects invalid loadouts before touching the entity. */
  public static void install(Mob mob, List<? extends CyberwareItem> implants) {
    if (enhanced(mob)) return;
    if (!TYPES.contains(mob.getType()) || implants.isEmpty() || implants.size() > 6)
      throw new IllegalArgumentException("Unsupported enhanced mob/loadout");
    var inv = new CyberwareInventory();
    for (var implant : implants) {
      if (!MobCyberwarePool.pool().contains(implant))
        throw new IllegalArgumentException("Unsupported mob implant");
      boolean inserted = false;
      for (int s = implant.category().firstSlot();
          s < implant.category().firstSlot() + implant.category().sockets;
          s++)
        if (inv.getStackInSlot(s).isEmpty() && inv.isItemValid(s, new ItemStack(implant))) {
          inv.setStackInSlot(s, new ItemStack(implant));
          inserted = true;
          break;
        }
      if (!inserted) throw new IllegalArgumentException("Duplicate/invalid implant sockets");
    }
    mob.setData(Cybernetics2077.CYBERWARE, inv);
    state(mob).putBoolean("active", true);
    state(mob).remove("pending");
    apply(mob, true);
    MobImplants.initialize(mob);
  }

  private static void modifier(
      Mob mob,
      net.minecraft.core.Holder<Attribute> attribute,
      String name,
      double value,
      AttributeModifier.Operation operation) {
    var instance = mob.getAttribute(attribute);
    if (instance == null) return;
    var id = Cybernetics2077.id("enhanced_" + name);
    var old = instance.getModifier(id);
    if (old != null && old.amount() == value && old.operation() == operation) return;
    instance.removeModifier(id);
    if (value != 0) instance.addPermanentModifier(new AttributeModifier(id, value, operation));
  }

  public static void refresh(Mob mob) {
    apply(mob, false);
  }

  private static void apply(Mob mob, boolean fill) {
    var l = loadout(mob);
    if (l.count == 0) return;
    double power = l.count * l.averageTier;
    int skeleton = tier(mob, Augment.EPIMORPHIC_SKELETON);
    modifier(
        mob,
        Attributes.MAX_HEALTH,
        "chassis",
        power * .15 + Math.max(0, skeleton - 2) * .05,
        AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    modifier(
        mob,
        Attributes.ATTACK_DAMAGE,
        "power",
        power * .08,
        AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    double armor = 0, toughness = 0;
    for (var kind : Augment.values()) {
      int t = tier(mob, kind);
      armor += kind.armor(t);
      toughness += kind.toughness(t);
    }
    modifier(mob, Attributes.ARMOR, "plating", armor, AttributeModifier.Operation.ADD_VALUE);
    modifier(
        mob,
        Attributes.ARMOR_TOUGHNESS,
        "toughness",
        toughness,
        AttributeModifier.Operation.ADD_VALUE);
    if (fill) mob.setHealth(mob.getMaxHealth());
    if (!mob.getPersistentData().contains(dev.cybernetics2077.city.BountyContracts.MOB_DATA))
      mob.setCustomName(
          Component.translatable(
                  "cybernetics2077.enhanced.name",
                  mob.getType().getDescription(),
                  String.format(Locale.ROOT, "%.1f", l.averageTier),
                  l.count)
              .withStyle(ChatFormatting.AQUA));
    // Naming does not make these ordinary hostile mobs persistent: normal despawning still applies.
  }

  @SubscribeEvent
  public static void drops(LivingDropsEvent e) {
    if (!(e.getEntity() instanceof Mob mob)
        || mob.level().isClientSide
        || !enhanced(mob)
        || !mob.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)
        || state(mob).getBoolean("salvaged")) return;
    var l = loadout(mob);
    if (l.count == 0) return;
    int tier = SalvageOdds.roll(l.averageTier, l.count, mob.getRandom().nextDouble());
    e.getDrops()
        .add(
            new ItemEntity(
                mob.level(),
                mob.getX(),
                mob.getY() + .3,
                mob.getZ(),
                new ItemStack(Cybernetics2077.DAMAGED_PARTS.get(tier - 1).get())));
    if (mob.getRandom().nextDouble() < IconicOdds.chance(l.averageTier, l.count)) {
      int iconicTier = SalvageOdds.roll(l.averageTier, l.count, mob.getRandom().nextDouble());
      e.getDrops()
          .add(
              new ItemEntity(
                  mob.level(),
                  mob.getX(),
                  mob.getY() + .3,
                  mob.getZ(),
                  new ItemStack(Cybernetics2077.ICONIC_REQUISITIONS.get(iconicTier - 1).get())));
    }
    state(mob).putBoolean("salvaged", true);
  }

  @SubscribeEvent
  public static void conversion(LivingConversionEvent.Post e) {
    if (!(e.getEntity() instanceof Mob before)
        || !(e.getOutcome() instanceof Mob after)
        || !enhanced(before)) return;
    after.setData(Cybernetics2077.CYBERWARE, before.getData(Cybernetics2077.CYBERWARE));
    after.getPersistentData().put(DATA, state(before).copy());
    apply(after, false);
    after.setHealth(after.getMaxHealth() * before.getHealth() / before.getMaxHealth());
  }

  private EnhancedMobs() {}
}
