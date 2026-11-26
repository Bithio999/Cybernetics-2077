package dev.cybernetics2077.temporal;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.combat.CyberwareRuntime;
import dev.cybernetics2077.cyberware.AugmentRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.*;

/** Bonuses use native hit positions and confirmed deaths; no extra damage/kill packets. */
@EventBusSubscriber(modid = Cybernetics2077.ID)
public final class ApogeeEffects {
  private static final String HIT = "cybernetics2077:apogee_headshot";

  private static boolean active(ServerPlayer p) {
    return Sandevistan.active(p) && Sandevistan.model(p) == SandevistanModel.APOGEE;
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public static void impact(ProjectileImpactEvent e) {
    if (!(e.getProjectile() instanceof AbstractArrow arrow) || arrow.level().isClientSide) return;
    arrow.getPersistentData().remove(HIT);
    if (!(arrow.getOwner() instanceof ServerPlayer p)
        || !active(p)
        || !(e.getRayTraceResult() instanceof EntityHitResult hit)
        || !(hit.getEntity() instanceof LivingEntity target)
        || !CyberwareRuntime.canAttack(p, target)) return;
    // Top quarter of a living hitbox. Very small mobs still require an upper-body impact.
    if (hit.getLocation().y < target.getBoundingBox().maxY - target.getBbHeight() * .25) return;
    var tag = new net.minecraft.nbt.CompoundTag();
    tag.putUUID("target", target.getUUID());
    tag.putLong("at", arrow.level().getGameTime());
    arrow.getPersistentData().put(HIT, tag);
  }

  @SubscribeEvent
  public static void damage(LivingIncomingDamageEvent e) {
    if (!(e.getSource().getDirectEntity() instanceof AbstractArrow arrow)
        || !(e.getSource().getEntity() instanceof ServerPlayer p)
        || !e.getSource().is(DamageTypeTags.IS_PROJECTILE)) return;
    var tag = arrow.getPersistentData().getCompound(HIT);
    arrow.getPersistentData().remove(HIT);
    if (active(p)
        && tag.hasUUID("target")
        && tag.getUUID("target").equals(e.getEntity().getUUID())
        && tag.getLong("at") == p.level().getGameTime()
        && e.getAmount() > 0) e.setAmount(e.getAmount() * 1.2f);
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public static void kill(LivingDeathEvent e) {
    if (!(e.getSource().getEntity() instanceof ServerPlayer p)
        || !active(p)
        || p == e.getEntity()
        || p.isAlliedTo(e.getEntity())
        || !AugmentRuntime.enemyKind(p, e.getEntity())) return;
    if (e.getEntity() instanceof net.minecraft.world.entity.player.Player other
        && (!p.server.isPvpAllowed() || !p.canHarmPlayer(other))) return;
    var state = AugmentRuntime.state(p);
    long now = AugmentRuntime.now(p);
    state.sandyEnd = Math.min(now + 120, state.sandyEnd + 12);
    // Minecraft has no stamina bar. Recover 22% of one exhaustion cycle, never food points.
    p.getFoodData().setExhaustion(Math.max(0, p.getFoodData().getExhaustionLevel() - .88f));
    CyberwareRuntime.broadcast(p);
  }

  private ApogeeEffects() {}
}
