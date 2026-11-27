package dev.cybernetics2077.mob;

import static dev.cybernetics2077.cyberware.Augment.*;

import dev.cybernetics2077.Cybernetics2077;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.*;

/** NPC counterparts to the equipped player implants, using the same tier formulas and timers. */
@EventBusSubscriber(modid = Cybernetics2077.ID)
public final class CyberpsychoCombat {
  public static void microgenerator(Cyberpsycho c, LivingEntity target) {
    MobImplants.discharge(c, target);
  }

  @SubscribeEvent
  public static void drops(LivingDropsEvent e) {
    if (!(e.getEntity() instanceof Cyberpsycho c)
        || !(c.level() instanceof ServerLevel level)
        || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) return;
    var inv = c.getData(Cybernetics2077.CYBERWARE);
    int count = 0, sum = 0;
    for (int s = 0; s < inv.getSlots(); s++)
      if (inv.getStackInSlot(s).getItem()
          instanceof dev.cybernetics2077.cyberware.CyberwareItem i) {
        count++;
        sum += i.tier().level;
      }
    if (count == 0) return;
    double avg = (double) sum / count;
    count = Math.min(6, count); // Existing salvage odds saturate at six implants.
    int tier = SalvageOdds.roll(avg, count, c.getRandom().nextDouble());
    e.getDrops()
        .add(
            new ItemEntity(
                level,
                c.getX(),
                c.getY(),
                c.getZ(),
                new ItemStack(Cybernetics2077.DAMAGED_PARTS.get(tier - 1).get())));
    if (c.getRandom().nextDouble() < IconicOdds.chance(avg, count))
      e.getDrops()
          .add(
              new ItemEntity(
                  level,
                  c.getX(),
                  c.getY(),
                  c.getZ(),
                  new ItemStack(Cybernetics2077.ICONIC_REQUISITIONS.get(tier - 1).get())));
  }

  private CyberpsychoCombat() {}
}
