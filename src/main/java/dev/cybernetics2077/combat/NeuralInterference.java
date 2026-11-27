package dev.cybernetics2077.combat;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.cyberware.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Jamming prevents firing before ammunition/item consumption; existing shots remain physical. */
@EventBusSubscriber(modid = Cybernetics2077.ID)
public final class NeuralInterference {
  private static boolean ranged(ItemStack s) {
    return s.getItem() instanceof ProjectileWeaponItem
        || s.getItem() instanceof TridentItem
        || dev.cybernetics2077.compat.TacZCompat.gun(s);
  }

  @SubscribeEvent
  public static void start(LivingEntityUseItemEvent.Start e) {
    if ((ExpansionRuntime.jammed(e.getEntity())
            || e.getEntity() instanceof Mob m && dev.cybernetics2077.mob.MobImplants.berserk(m))
        && ranged(e.getItem())) e.setCanceled(true);
  }

  @SubscribeEvent
  public static void interact(PlayerInteractEvent.RightClickItem e) {
    if (!e.getLevel().isClientSide
        && ExpansionRuntime.jammed(e.getEntity())
        && ranged(e.getItemStack())) e.setCanceled(true);
  }

  @SubscribeEvent
  public static void spawned(EntityJoinLevelEvent e) {
    if (!e.getLevel().isClientSide
        && e.getEntity() instanceof Projectile p
        && p.getOwner() instanceof LivingEntity owner
        && (ExpansionRuntime.jammed(owner)
            || owner instanceof Mob m && dev.cybernetics2077.mob.MobImplants.berserk(m)))
      e.setCanceled(true);
  }

  @SubscribeEvent
  public static void charge(LivingEntityUseItemEvent.Tick e) {
    if (e.getEntity().level().isClientSide
        || !ranged(e.getItem())
        || dev.cybernetics2077.compat.TacZCompat.gun(e.getItem())) return;
    double bonus = ExpansionRuntime.gunRate(e.getEntity()) - 1;
    if (bonus > 0
        && e.getEntity().tickCount % Math.max(1, (int) Math.ceil(1 / bonus)) == 0
        && e.getDuration() > 1) e.setDuration(e.getDuration() - 1);
  }

  @SubscribeEvent
  public static void consumed(LivingEntityUseItemEvent.Finish e) {
    if (!e.getEntity().level().isClientSide && e.getItem().getItem() instanceof PotionItem) {
      var contents =
          e.getItem()
              .getOrDefault(
                  net.minecraft.core.component.DataComponents.POTION_CONTENTS,
                  net.minecraft.world.item.alchemy.PotionContents.EMPTY);
      for (var effect : contents.getAllEffects())
        if (effect.getEffect().equals(net.minecraft.world.effect.MobEffects.HEAL)
            || effect.getEffect().equals(net.minecraft.world.effect.MobEffects.REGENERATION)) {
          ExpansionRuntime.medicalUsed(e.getEntity());
          break;
        }
    }
  }

  @SubscribeEvent
  public static void hit(LivingDamageEvent.Post e) {
    if (e.getNewDamage() > 0
        && e.getSource().getDirectEntity() instanceof AbstractArrow arrow
        && arrow.isCritArrow()
        && e.getSource().getEntity() instanceof LivingEntity owner)
      ExpansionRuntime.precisionHit(owner, e.getEntity());
  }
}
