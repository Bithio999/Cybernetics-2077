package dev.cybernetics2077.review;

import static dev.cybernetics2077.cyberware.Augment.*;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.city.*;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.deck.*;
import dev.cybernetics2077.mob.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(Cybernetics2077.ID)
@PrefixGameTestTemplate(false)
public final class CyberpsychoGameTests {
  static Husk actor(GameTestHelper h, Augment... kinds) {
    var m = EntityType.HUSK.create(h.getLevel());
    var p = h.absolutePos(new BlockPos(2, 90, 2));
    m.moveTo(p.getX() + .5, p.getY(), p.getZ() + .5, 0, 0);
    m.setNoGravity(true);
    EnhancedMobs.install(m, Arrays.stream(kinds).map(a -> Cybernetics2077.augment(a, 5)).toList());
    h.getLevel().addFreshEntity(m);
    return m;
  }

  @GameTest(batch = "psychoPlans", template = "empty", timeoutTicks = 100)
  public static void realInstalledCostsDetermineEveryContractTierAndSockets(GameTestHelper h) {
    for (int rank = 1; rank <= 6; rank++) {
      var roles = new HashSet<CyberpsychoLoadouts.Role>();
      var distinct = new HashSet<String>();
      for (long seed = 0; seed < 40; seed++) {
        var plan = CyberpsychoLoadouts.forSeed(rank, seed);
        var inv = plan.inventory();
        roles.add(plan.role());
        h.assertTrue(
            plan.rank() == rank && inv.capacityUsed() == plan.capacity() && plan.capacity() > 150,
            "Incorrect real capacity band");
        var signature = new ArrayList<String>();
        int count = 0;
        for (int i = 0; i < inv.getSlots(); i++) {
          var stack = inv.getStackInSlot(i);
          if (stack.isEmpty()) continue;
          count++;
          h.assertTrue(
              inv.isItemValid(i, stack)
                  && stack.getItem() instanceof CyberwareItem item
                  && !item.iconic(),
              "Invalid or iconic socket equipment");
          signature.add(stack.getItem().toString());
          if (stack.getItem() instanceof CyberdeckItem deck) {
            var programs = CyberdeckItem.programs(stack);
            h.assertTrue(
                !programs.isEmpty()
                    && programs.size() <= deck.tier().programSlots()
                    && programs.stream().allMatch(q -> q.tier().level <= deck.tier().level)
                    && programs.stream().anyMatch(q -> !q.kind().control()),
                "Illegal or empty installed NPC programs");
          }
        }
        h.assertTrue(count == plan.implants().size(), "Missing equipment");
        distinct.add(signature.toString());
        var repeated = CyberpsychoLoadouts.forSeed(rank, seed);
        h.assertTrue(repeated.equals(plan), "Seed rerolled the same dossier");
      }
      h.assertTrue(
          roles.size() == 3 && distinct.size() > 20, "Loadouts lack role/equipment variety");
    }
    h.succeed();
  }

  @GameTest(batch = "npcMedicine", template = "empty", timeoutTicks = 80)
  public static void nativeAiActivatesBerserkAndBiomonitorSpendsRealPumpCharges(GameTestHelper h) {
    var m = actor(h, BIODYNE_BERSERK, BLOOD_PUMP, BIOMONITOR, ADRENALINE_CONVERTER, NEOFIBER);
    var victim = h.spawn(EntityType.COW, new BlockPos(7, 90, 2));
    victim.setNoAi(true);
    victim.setNoGravity(true);
    m.setTarget(victim);
    m.setHealth(m.getMaxHealth() * .3f);
    float before = m.getHealth();
    h.runAfterDelay(
        3,
        () -> {
          h.assertTrue(
              MobImplants.berserk(m)
                  && MobImplants.state(m).getInt("pumpCharges") == 1
                  && m.getHealth() > before,
              "Shared NPC actives did not activate/spend charge");
          h.assertTrue(
              m.getAttributeValue(Attributes.MOVEMENT_SPEED) > .23, "Adrenaline not applied");
          m.discard();
          victim.discard();
          h.succeed();
        });
  }

  @GameTest(batch = "npcJump", template = "empty", timeoutTicks = 80)
  public static void reinforcedTendonsExecutesSecondAirborneImpulse(GameTestHelper h) {
    for (int x = 1; x <= 12; x++)
      for (int z = 1; z <= 4; z++)
        h.setBlock(new BlockPos(x, 89, z), net.minecraft.world.level.block.Blocks.STONE);
    var m = actor(h, REINFORCED_TENDONS, SUBDERMAL_ARMOR);
    m.setNoGravity(false);
    m.setOnGround(true);
    var victim = h.spawn(EntityType.COW, new BlockPos(9, 90, 2));
    victim.setNoAi(true);
    m.setTarget(victim);
    h.runAfterDelay(
        9,
        () -> {
          h.assertTrue(
              MobImplants.state(m).getLong("lastDoubleJump") > 0 && m.getDeltaMovement().y > 0,
              "No real second airborne jump: " + MobImplants.state(m));
          m.discard();
          victim.discard();
          h.succeed();
        });
  }

  @GameTest(batch = "npcCharges", template = "empty", timeoutTicks = 60)
  public static void pumpChargesRechargeAndSelfIceBlocksOnlyOnceUntilReady(GameTestHelper h) {
    var m = actor(h, BLOOD_PUMP, SELF_ICE);
    m.setHealth(m.getMaxHealth() * .05f);
    h.assertTrue(
        MobImplants.usePump(m) && MobImplants.usePump(m) && !MobImplants.usePump(m),
        "Pump not charge limited");
    MobImplants.state(m).putLong("pumpNext", m.level().getGameTime());
    var attacker =
        new FakePlayer(
            h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "IceReview"));
    h.assertTrue(
        AugmentExpansion.blockHack(attacker, m) && !AugmentExpansion.blockHack(attacker, m),
        "Native hack boundary ignored ICE cooldown");
    h.runAfterDelay(
        2,
        () -> {
          h.assertTrue(MobImplants.state(m).getInt("pumpCharges") >= 1, "No pump recharge");
          m.discard();
          h.succeed();
        });
  }

  @GameTest(batch = "npcRevival", template = "empty", timeoutTicks = 60)
  public static void fatalHitUsesSecondHeartThenCooldownAllowsDeathAndSalvage(GameTestHelper h) {
    var m = actor(h, SECOND_HEART, EPIMORPHIC_SKELETON);
    m.setHealth(1);
    m.hurt(h.getLevel().damageSources().generic(), 100000);
    h.assertTrue(
        m.isAlive()
            && m.getHealth() == m.getMaxHealth()
            && MobImplants.state(m).getLong("heartReady") > m.level().getGameTime(),
        "NPC Second Heart did not stop fatal native damage");
    m.invulnerableTime = 0;
    m.hurt(h.getLevel().damageSources().generic(), 100000);
    h.assertTrue(!m.isAlive(), "Second Heart ignored rearm");
    h.succeed();
  }

  @GameTest(batch = "npcPersistence", template = "empty", timeoutTicks = 60)
  public static void saveReloadPreservesMedicalCooldownsEquipmentAndMissingHealth(
      GameTestHelper h) {
    var m = actor(h, BLOOD_PUMP, SELF_ICE, EPIMORPHIC_SKELETON, PARA_BELLUM);
    m.setHealth(m.getMaxHealth() * .1f);
    MobImplants.usePump(m);
    AugmentExpansion.blockHack(null, m);
    float hp = m.getHealth();
    double armor = m.getAttributeValue(Attributes.ARMOR);
    long ready = AugmentRuntime.state(m).iceReady;
    var save = m.saveWithoutId(new CompoundTag());
    m.discard();
    var loaded = EntityType.HUSK.create(h.getLevel());
    loaded.load(save);
    h.getLevel().addFreshEntity(loaded);
    MobImplants.attributes(loaded);
    h.assertTrue(
        loaded.getHealth() == hp
            && loaded.getAttributeValue(Attributes.ARMOR) == armor
            && MobImplants.state(loaded).getInt("pumpCharges") == 1
            && AugmentRuntime.state(loaded).iceReady == ready,
        "Reload reset medical state, HP or modifiers");
    loaded.discard();
    h.succeed();
  }

  @GameTest(batch = "npcMelee", template = "empty", timeoutTicks = 60)
  public static void nativeMeleeUsesWeaponDenseMarrowAndMicrogeneratorWithRearm(GameTestHelper h) {
    var m = actor(h, DENSE_MARROW, MICROGENERATOR);
    m.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
    var victim = h.spawn(EntityType.COW, new BlockPos(3, 90, 2));
    victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
    victim.setHealth(1000);
    victim.setNoAi(true);
    m.setTarget(victim);
    h.assertTrue(
        m.doHurtTarget(victim) && victim.getHealth() < 990, "Weapon/Dense Marrow damage missing");
    long ready = MobImplants.state(m).getLong("microReady");
    h.assertTrue(ready > m.level().getGameTime(), "Microgenerator not used");
    MobImplants.discharge(m, victim);
    h.assertTrue(
        MobImplants.state(m).getLong("microReady") == ready, "Microgenerator rearm bypass");
    m.discard();
    victim.discard();
    h.succeed();
  }
}
