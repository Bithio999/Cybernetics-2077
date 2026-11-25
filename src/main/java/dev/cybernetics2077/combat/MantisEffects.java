package dev.cybernetics2077.combat;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.cyberware.*;
import java.util.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = Cybernetics2077.ID)
public final class MantisEffects {
  private record Bleed(
      java.lang.ref.WeakReference<ServerPlayer> owner, float damage, long end, long next) {}

  private static final Map<LivingEntity, Bleed> BLEEDS = new WeakHashMap<>();
  public static final ResourceKey<DamageType> BLEED =
      ResourceKey.create(Registries.DAMAGE_TYPE, Cybernetics2077.id("mantis_bleed"));

  public static void hit(
      ServerPlayer p, LivingEntity target, MantisVariant variant, int tier, boolean lunge) {
    var point = target.getBoundingBox().getCenter();
    p.serverLevel()
        .sendParticles(
            switch (variant) {
              case PHYSICAL -> ParticleTypes.DAMAGE_INDICATOR;
              case THERMAL -> ParticleTypes.FLAME;
              case ELECTRICAL -> ParticleTypes.ELECTRIC_SPARK;
              case TOXIC -> ParticleTypes.SPORE_BLOSSOM_AIR;
            },
            point.x,
            point.y,
            point.z,
            variant == MantisVariant.PHYSICAL ? 2 : 9,
            .2,
            .25,
            .2,
            .015);
    if (!target.isAlive() || !lunge && p.getRandom().nextFloat() >= .2f) return;
    switch (variant) {
      case PHYSICAL -> {
        long now = target.level().getGameTime();
        BLEEDS.put(
            target,
            new Bleed(new java.lang.ref.WeakReference<>(p), 1 + tier * .25f, now + 80, now + 20));
      }
      case THERMAL -> {
        if (!target.fireImmune()) target.igniteForSeconds(4);
      }
      case ELECTRICAL -> {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1), p);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0), p);
      }
      case TOXIC -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0), p);
    }
  }

  @SubscribeEvent
  public static void tick(EntityTickEvent.Post e) {
    if (!(e.getEntity() instanceof LivingEntity target) || target.level().isClientSide) return;
    var bleed = BLEEDS.get(target);
    if (bleed == null) return;
    var owner = bleed.owner.get();
    long now = target.level().getGameTime();
    if (owner == null
        || owner.level() != target.level()
        || now > bleed.end
        || !CyberwareRuntime.canAttack(owner, target)) {
      BLEEDS.remove(target);
      return;
    }
    if (now >= bleed.next) {
      BLEEDS.put(target, new Bleed(bleed.owner, bleed.damage, bleed.end, now + 20));
      var source =
          new DamageSource(
              owner
                  .registryAccess()
                  .registryOrThrow(Registries.DAMAGE_TYPE)
                  .getHolderOrThrow(BLEED),
              owner,
              owner);
      SecondaryDamage.hurt(target, source, bleed.damage);
    }
  }

  private MantisEffects() {}
}
