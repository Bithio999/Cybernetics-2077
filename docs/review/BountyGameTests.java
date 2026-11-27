package dev.cybernetics2077.review;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.city.*;
import dev.cybernetics2077.mob.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(Cybernetics2077.ID)
@PrefixGameTestTemplate(false)
public final class BountyGameTests {
  private static ServerPlayer player(GameTestHelper h) {
    var p =
        new FakePlayer(
            h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "BountyReview"));
    var pos = h.absolutePos(new BlockPos(2, 90, 2));
    p.setPos(pos.getX(), pos.getY(), pos.getZ());
    return p;
  }

  private static CityResident fixer(GameTestHelper h, ServerPlayer p, int profile) {
    var f = CityContent.FIXER.get().create(h.getLevel());
    var tag = f.saveWithoutId(new CompoundTag());
    tag.putInt("CityStyle", profile);
    f.load(tag);
    f.setPos(p.position().add(2, 0, 0));
    f.setNoGravity(true);
    h.getLevel().addFreshEntity(f);
    return f;
  }

  private static void accept(GameTestHelper h, ServerPlayer p, CityResident fixer) {
    h.assertTrue(
        BountyContracts.accept(p, fixer, BountyContracts.offers(p, fixer).getFirst()) == 0,
        "Bounty acceptance failed");
  }

  private static Mob mark(GameTestHelper h, ServerPlayer p) {
    var job = BountyContracts.job(p);
    Mob m =
        BountyContracts.kind(job) == BountyRules.Kind.CYBERPSYCHO
            ? CityContent.CYBERPSYCHO.get().create(h.getLevel())
            : EntityType.HUSK.create(h.getLevel());
    m.setPos(p.position().add(4, 0, 0));
    m.setNoGravity(true);
    m.setNoAi(true);
    if (m instanceof Cyberpsycho psycho) psycho.configure(job.getInt("rank"));
    else
      EnhancedMobs.install(
          m,
          MobCyberwarePool.rollExactTier(
              job.getInt("tier"), job.getInt("implants"), m.getRandom()));
    var t = new CompoundTag();
    t.putUUID("owner", p.getUUID());
    t.putUUID("token", job.getUUID("token"));
    t.putInt("role", 0);
    m.getPersistentData().put(BountyContracts.MOB_DATA, t);
    job.putUUID("target", m.getUUID());
    job.putBoolean("spawned", true);
    BountyLedger.get(p.server).put(p.getUUID(), job);
    h.getLevel().addFreshEntity(m);
    return m;
  }

  private static void kill(GameTestHelper h, ServerPlayer p, Mob target) {
    h.assertTrue(
        target.hurt(h.getLevel().damageSources().playerAttack(p), 100000), "Native attack failed");
    h.assertTrue(!target.isAlive(), "Mark survived fatal attack");
  }

  private static void clear(ServerPlayer p) {
    BountyLedger.get(p.server).remove(p.getUUID());
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void acceptIsOwnerBoundFullInventoryIsAtomicAndReissueKeepsMission(
      GameTestHelper h) {
    var p = player(h);
    var f = fixer(h, p, 0);
    var offer = BountyContracts.offers(p, f).getFirst();
    for (int i = 0; i < 36; i++) p.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
    h.assertTrue(
        BountyContracts.accept(p, f, offer) == 2
            && BountyContracts.job(p).isEmpty()
            && !p.getData(CityContent.ACCOUNT).bountyUsed(offer.day(), offer.id()),
        "Full inventory consumed offer");
    p.getInventory().setItem(0, ItemStack.EMPTY);
    accept(h, p, f);
    var token = BountyContracts.job(p).getUUID("token");
    h.assertTrue(
        p.getInventory().getItem(0).is(CityContent.BOUNTY_CONTRACT.get()), "No dossier issued");
    h.assertTrue(
        BountyContracts.reissue(p, f) == 0 && BountyContracts.job(p).getUUID("token").equals(token),
        "Reissue rerolled active job");
    h.assertTrue(
        BountyContracts.accept(p, f, BountyContracts.offers(p, f).get(1)) == 10,
        "Multiple concurrent jobs accepted");
    h.assertTrue(BountyContracts.abandon(p, f), "Abandon failed");
    h.assertTrue(
        BountyContracts.accept(p, f, offer) == 11 && BountyContracts.job(p).isEmpty(),
        "Abandon rerolled same daily offer");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void onlyAssignedMarkPaysAndDebriefIncludesOptionalBonusesOnce(GameTestHelper h) {
    var p = player(h);
    var f = fixer(h, p, 0);
    var other = fixer(h, p, 1);
    accept(h, p, f);
    var ordinary = EntityType.HUSK.create(h.getLevel());
    ordinary.setPos(p.position().add(5, 0, 0));
    h.getLevel().addFreshEntity(ordinary);
    kill(h, p, ordinary);
    h.assertTrue(BountyContracts.job(p).getInt("stage") == 1, "Unrelated kill completed bounty");
    var target = mark(h, p);
    p.getData(Cybernetics2077.CYBERWARE)
        .setStackInSlot(
            dev.cybernetics2077.cyberware.CyberwareCategory.FACE.firstSlot(),
            new ItemStack(Cybernetics2077.OPTICS.get()));
    BountyContracts.scanned(p, target);
    kill(h, p, target);
    h.assertTrue(
        BountyContracts.job(p).getInt("stage") == 2
            && BountyContracts.navigation(p).getString("objective").contains("RETURN"),
        "Confirmed kill did not switch to return route");
    h.assertTrue(
        BountyContracts.claim(p, other) == 21 && p.getData(CityContent.ACCOUNT).eddies() == 0,
        "Wrong fixer network paid");
    h.assertTrue(BountyContracts.claim(p, f) == 0, "Claim failed");
    var account = p.getData(CityContent.ACCOUNT);
    h.assertTrue(
        account.eddies() == 960
            && account.streetCred() == 2
            && account.bountyReport().getString("rating").equals("S"),
        "Optional payment/rating incorrect");
    h.assertTrue(
        BountyContracts.claim(p, f) == 20 && account.eddies() == 960, "Duplicate claim paid twice");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void anotherPlayersKillDoesNotCompleteYourBounty(GameTestHelper h) {
    var p = player(h);
    var f = fixer(h, p, 0);
    accept(h, p, f);
    var mark = mark(h, p);
    var stranger = player(h);
    kill(h, stranger, mark);
    h.assertTrue(
        BountyContracts.job(p).getInt("stage") == 3
            && BountyContracts.claim(p, f) == 20
            && p.getData(CityContent.ACCOUNT).eddies() == 0,
        "Foreign kill paid owner's bounty");
    BountyContracts.abandon(p, f);
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void tamedPetKillCreditsOwnerAndStartsTheRealCombatTimer(GameTestHelper h) {
    var p = player(h);
    var f = fixer(h, p, 1);
    accept(h, p, f);
    var target = mark(h, p);
    var wolf = EntityType.WOLF.create(h.getLevel());
    wolf.setOwnerUUID(p.getUUID());
    wolf.setTame(true, false);
    wolf.setPos(p.position().add(3, 0, 0));
    wolf.setNoAi(true);
    h.getLevel().addFreshEntity(wolf);
    target.hurt(h.getLevel().damageSources().mobAttack(wolf), 1);
    var job = BountyContracts.job(p);
    h.assertTrue(job.getBoolean("engaged"), "Pet attack did not start bounty timer");
    job.putLong("engagement", p.server.overworld().getGameTime() - 2000);
    BountyLedger.get(p.server).put(p.getUUID(), job);
    target.hurt(h.getLevel().damageSources().mobAttack(wolf), 100000);
    h.assertTrue(
        BountyContracts.job(p).getInt("stage") == 2 && BountyContracts.claim(p, f) == 0,
        "Pet kill did not credit its owner");
    h.assertTrue(
        !p.getData(CityContent.ACCOUNT).bountyReport().getBoolean("specialty")
            && p.getData(CityContent.ACCOUNT).eddies() == 840,
        "Pet kill bypassed elapsed-time optional objective");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void saveCopiesDailyRotationAndAcceptedDossierSurvive(GameTestHelper h) {
    var p = player(h);
    var f = fixer(h, p, 0);
    accept(h, p, f);
    var job = BountyContracts.job(p);
    long day = p.server.overworld().getDayTime();
    try {
      var snapshot =
          BountyLedger.get(p.server).save(new CompoundTag(), h.getLevel().registryAccess());
      var restored = BountyLedger.load(snapshot, h.getLevel().registryAccess());
      h.assertTrue(restored.job(p.getUUID()).equals(job), "Saved bounty lost target/dossier state");
      var copy = restored.job(p.getUUID());
      copy.putInt("rank", 99);
      h.assertTrue(restored.job(p.getUUID()).getInt("rank") != 99, "Ledger exposed mutable job");
      var account = new StreetAccount();
      account.deserializeNBT(
          h.getLevel().registryAccess(),
          p.getData(CityContent.ACCOUNT).serializeNBT(h.getLevel().registryAccess()));
      h.assertTrue(
          account.bountyUsed(job.getLong("day"), job.getUUID("offer")),
          "Used offer lost on save/death copy");
      p.server.overworld().setDayTime(day + 24000);
      h.assertTrue(BountyContracts.job(p).equals(job), "Dawn rerolled accepted bounty");
      h.assertTrue(
          !BountyContracts.offers(p, f).getFirst().id().equals(job.getUUID("offer")),
          "Daily board did not rotate");
    } finally {
      p.server.overworld().setDayTime(day);
      clear(p);
    }
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void missedOptionalsStillPayBaseAndDeathDoesNotAbandon(GameTestHelper h) {
    var p = player(h);
    var f = fixer(h, p, 1);
    accept(h, p, f);
    var mark = mark(h, p);
    BountyContracts.death(new LivingDeathEvent(p, h.getLevel().damageSources().generic()));
    var civilian = EntityType.VILLAGER.create(h.getLevel());
    civilian.setPos(p.position().add(4, 0, 0));
    h.getLevel().addFreshEntity(civilian);
    civilian.hurt(h.getLevel().damageSources().playerAttack(p), 100000);
    var job = BountyContracts.job(p);
    job.putBoolean("engaged", true);
    job.putLong("engagement", p.server.overworld().getGameTime() - 2000);
    BountyLedger.get(p.server).put(p.getUUID(), job);
    h.assertTrue(
        job.getInt("stage") == 1 && !job.getBoolean("survival") && !job.getBoolean("untouched"),
        "Optional failures abandoned the main objective");
    kill(h, p, mark);
    h.assertTrue(BountyContracts.claim(p, f) == 0, "Base claim rejected");
    h.assertTrue(
        p.getData(CityContent.ACCOUNT).eddies() == 600
            && p.getData(CityContent.ACCOUNT).bountyReport().getString("rating").equals("C"),
        "Optional failure reduced base pay");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void threeJobLoyaltyAndEliteChipRewardCannotBeDuplicated(GameTestHelper h) {
    var p = player(h);
    var f = fixer(h, p, 1);
    p.getData(CityContent.ACCOUNT).reputation(50);
    long day = p.server.overworld().getDayTime();
    try {
      for (int i = 0; i < 6; i++) {
        p.server.overworld().setDayTime(day + (i + 1) * 24000);
        accept(h, p, f);
        kill(h, p, mark(h, p));
        h.assertTrue(BountyContracts.claim(p, f) == 0, "Elite claim failed");
        var report = p.getData(CityContent.ACCOUNT).bountyReport();
        h.assertTrue(
            report.getInt("chainBonus") == ((i + 1) % 3 == 0 ? 1200 : 0),
            "Loyalty bonus not paid every third job");
      }
      int chips =
          p.getInventory().items.stream()
              .filter(s -> s.is(Cybernetics2077.ICONIC_REQUISITIONS.get(4).get()))
              .mapToInt(ItemStack::getCount)
              .sum();
      h.assertTrue(
          chips == 1 && p.getData(CityContent.ACCOUNT).iconicRewardClaimed(),
          "Elite chain duplicated or lost one-time chip");
    } finally {
      p.server.overworld().setDayTime(day);
      clear(p);
    }
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void staleBoardButtonsAndUnauthorizedMenuActionsAreRejected(GameTestHelper h) {
    var p = player(h);
    var f = fixer(h, p, 0);
    var menu = new FixerMenu(77, p.getInventory(), f.getId());
    p.containerMenu = menu;
    menu.broadcastChanges();
    int old = menu.board().getInt("acceptBase");
    long day = p.server.overworld().getDayTime();
    try {
      p.server.overworld().setDayTime(day + 24000);
      h.assertTrue(
          !menu.clickMenuButton(p, old) && BountyContracts.job(p).isEmpty(),
          "Old daily button accepted a new job");
      var other = player(h);
      other.setPos(p.position());
      h.assertTrue(
          !menu.clickMenuButton(other, menu.board().getInt("acceptBase")),
          "Another player used your menu");
      h.assertTrue(
          menu.clickMenuButton(p, menu.board().getInt("acceptBase")),
          "Refreshed native accept button failed");
    } finally {
      p.server.overworld().setDayTime(day);
      clear(p);
    }
    h.succeed();
  }

  @GameTest(template = "empty", batch = "bountyDeployment", timeoutTicks = 100)
  public static void deploysExactTierTargetAndGuardsWithoutBlockEditsOrForcedChunks(
      GameTestHelper h) {
    var p = player(h);
    var f = fixer(h, p, 0);
    p.getData(CityContent.ACCOUNT).reputation(50);
    accept(h, p, f);
    var base = p.blockPosition();
    for (int x = 18; x <= 70; x++)
      for (int z = -26; z <= 26; z++)
        h.getLevel().setBlock(base.offset(x, -1, z), Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
    var job = BountyContracts.job(p);
    job.putInt("x", base.getX() + 60);
    job.putInt("y", base.getY());
    job.putInt("z", base.getZ());
    BountyLedger.get(p.server).put(p.getUUID(), job);
    h.assertTrue(
        !BountyContracts.deploy(p) && !BountyContracts.job(p).getBoolean("spawned"),
        "Encounter spawned on hazardous magma or a remote floor below the player");
    for (int x = 18; x <= 70; x++)
      for (int z = -26; z <= 26; z++)
        h.getLevel().setBlock(base.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
    int loaded = h.getLevel().getChunkSource().getLoadedChunksCount();
    h.assertTrue(BountyContracts.deploy(p), "Safe loaded ground did not deploy bounty");
    job = BountyContracts.job(p);
    var target = (Mob) h.getLevel().getEntity(job.getUUID("target"));
    h.assertTrue(
        target != null
            && EnhancedMobs.loadout(target).equals(new EnhancedMobs.Loadout(6, 5))
            && target.distanceToSqr(p) >= 16 * 16
            && job.getList("guardIds", 8).size() == 3,
        "Wrong elite tier/count/placement/guards");
    h.assertTrue(
        h.getLevel().getChunkSource().getLoadedChunksCount() == loaded,
        "Deployment forced extra chunks");
    for (int x = 18; x <= 70; x++)
      for (int z = -26; z <= 26; z++)
        h.assertTrue(
            h.getLevel().getBlockState(base.offset(x, -1, z)).is(Blocks.STONE),
            "Deployment changed terrain");
    var saved = target.saveWithoutId(new CompoundTag());
    var type = target.getType();
    BountyContracts.abandon(p, f);
    h.assertTrue(target.isRemoved(), "Abandon did not remove loaded target");
    var stale = type.create(h.getLevel());
    stale.load(saved);
    h.assertTrue(
        !h.getLevel().addFreshEntity(stale) && stale.isRemoved(),
        "Unloaded abandoned target survived rejoining");
    var far = new BlockPos(12000000, 100, 12000000);
    loaded = h.getLevel().getChunkSource().getLoadedChunksCount();
    h.assertTrue(
        BountyContracts.safeGround(h.getLevel(), far, p, Set.of()).isEmpty()
            && h.getLevel().getChunkSource().getLoadedChunksCount() == loaded,
        "Ground search generated unloaded terrain");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 80)
  public static void cyberpsychoOverkillContainsRatherThanKillsAndPaysOnce(GameTestHelper h) {
    var p = player(h);
    var f = fixer(h, p, 0);
    h.assertTrue(
        BountyContracts.accept(p, f, BountyContracts.offers(p, f).get(1)) == 0,
        "Sighting could not be accepted");
    var target = mark(h, p);
    target.setNoAi(false);
    target.setHealth(target.getMaxHealth());
    target.hurt(h.getLevel().damageSources().playerAttack(p), 100000);
    var job = BountyContracts.job(p);
    h.assertTrue(
        target.isAlive()
            && target.getHealth() <= target.getMaxHealth() * .1f + .01f
            && target.isNoAi()
            && job.getInt("stage") == 2
            && job.getBoolean("contained"),
        "Lethal owner attack did not safely contain cyberpsycho");
    target.invulnerableTime = 0;
    target.hurt(h.getLevel().damageSources().playerAttack(p), 100000);
    h.assertTrue(target.isAlive(), "Follow-up hit killed contained target");
    var saved = target.saveWithoutId(new CompoundTag());
    var clone = (Mob) target.getType().create(h.getLevel());
    clone.load(saved);
    var joining = new net.neoforged.neoforge.event.entity.EntityJoinLevelEvent(clone, h.getLevel());
    BountyContracts.join(joining);
    h.assertTrue(
        !joining.isCanceled()
            && clone.isNoAi()
            && clone.isInvulnerableTo(h.getLevel().damageSources().playerAttack(p)),
        "Contained state did not survive reload");
    h.assertTrue(
        BountyContracts.claim(p, f) == 0 && p.getData(CityContent.ACCOUNT).eddies() == 2520,
        "Containment reward incorrect");
    h.assertTrue(
        BountyContracts.claim(p, f) == 20 && target.isRemoved(),
        "Repeated claim or contained target cleanup failed");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 80)
  public static void outsiderCannotContainAndEnvironmentalDeathFailsSighting(GameTestHelper h) {
    var p = player(h);
    var f = fixer(h, p, 0);
    BountyContracts.accept(p, f, BountyContracts.offers(p, f).get(1));
    var target = mark(h, p);
    var outsider = player(h);
    target.hurt(h.getLevel().damageSources().playerAttack(outsider), 100000);
    h.assertTrue(
        !target.isAlive()
            && BountyContracts.job(p).getInt("stage") == 3
            && BountyContracts.claim(p, f) == 20,
        "Foreign kill completed a containment job");
    BountyContracts.abandon(p, f);
    h.succeed();
  }

  @GameTest(template = "empty", batch = "recoveryCache", timeoutTicks = 100)
  public static void recoveryRequiresOwnerNearVisibleCacheAndCannotPayTwice(GameTestHelper h) {
    var p = player(h);
    var f = fixer(h, p, 1);
    BountyContracts.accept(p, f, BountyContracts.offers(p, f).get(2));
    var target = mark(h, p);
    var floor = target.blockPosition().below();
    for (int x = -3; x <= 3; x++)
      for (int z = -3; z <= 3; z++)
        h.getLevel().setBlock(floor.offset(x, 0, z), Blocks.STONE.defaultBlockState(), 3);
    kill(h, p, target);
    var job = BountyContracts.job(p);
    h.assertTrue(
        job.getInt("stage") == 4
            && job.hasUUID("cache")
            && h.getLevel().getEntity(job.getUUID("cache")) instanceof Display.BlockDisplay,
        "Courier did not leave recovery cache");
    h.assertTrue(
        BountyContracts.claim(p, f) == 20
            && BountyContracts.navigation(p).getString("objective").contains("RECOVER"),
        "Courier kill skipped recovery phase");
    var cache = h.getLevel().getEntity(job.getUUID("cache"));
    var outsider = player(h);
    outsider.setPos(cache.position());
    h.assertTrue(!BountyContracts.recover(outsider), "Other player stole recovery objective");
    p.setPos(cache.position().add(10, 0, 0));
    h.assertTrue(!BountyContracts.recover(p), "Remote recovery accepted");
    p.setPos(cache.position().add(2, 0, 0));
    var copied = p.getInventory().getItem(0).copy();
    var forged =
        copied
            .getOrDefault(
                net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY)
            .copyTag();
    forged.putUUID("owner", outsider.getUUID());
    copied.set(
        net.minecraft.core.component.DataComponents.CUSTOM_DATA,
        net.minecraft.world.item.component.CustomData.of(forged));
    p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, copied);
    ((BountyContract) copied.getItem())
        .use(h.getLevel(), p, net.minecraft.world.InteractionHand.MAIN_HAND);
    h.assertTrue(BountyContracts.job(p).getInt("stage") == 4, "Foreign shard recovered the cache");
    forged.putUUID("owner", p.getUUID());
    copied.set(
        net.minecraft.core.component.DataComponents.CUSTOM_DATA,
        net.minecraft.world.item.component.CustomData.of(forged));
    ((BountyContract) copied.getItem())
        .use(h.getLevel(), p, net.minecraft.world.InteractionHand.MAIN_HAND);
    h.assertTrue(
        BountyContracts.job(p).getInt("stage") == 2
            && BountyContracts.job(p).getBoolean("recovered")
            && cache.isRemoved(),
        "Native shard use did not secure cache");
    p.setPos(f.position().add(1, 0, 0));
    h.assertTrue(
        BountyContracts.claim(p, f) == 0 && p.getData(CityContent.ACCOUNT).eddies() == 1056,
        "Recovery payout incorrect");
    h.assertTrue(
        BountyContracts.claim(p, f) == 20 && !BountyContracts.recover(p), "Recovery paid twice");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "recoveryCleanup", timeoutTicks = 80)
  public static void recoveryCacheSavesAndAbandonRejectsStaleCache(GameTestHelper h) {
    var p = player(h);
    var f = fixer(h, p, 0);
    BountyContracts.accept(p, f, BountyContracts.offers(p, f).get(2));
    kill(h, p, mark(h, p));
    var job = BountyContracts.job(p);
    var ledger = BountyLedger.get(p.server);
    var saved =
        BountyLedger.load(
            ledger.save(new CompoundTag(), h.getLevel().registryAccess()),
            h.getLevel().registryAccess());
    h.assertTrue(
        saved.job(p.getUUID()).getInt("stage") == 4
            && saved.job(p.getUUID()).getUUID("cache").equals(job.getUUID("cache")),
        "Recovery state lost on save");
    var cache = h.getLevel().getEntity(job.getUUID("cache"));
    var data = cache.saveWithoutId(new CompoundTag());
    BountyContracts.abandon(p, f);
    h.assertTrue(cache.isRemoved(), "Abandon left cache behind");
    var stale = EntityType.BLOCK_DISPLAY.create(h.getLevel());
    stale.load(data);
    h.assertTrue(!h.getLevel().addFreshEntity(stale), "Stale recovery cache survived reload");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 80)
  public static void namedCombatChassisReloadDoesNotStackOrRefillHealth(GameTestHelper h) {
    var p = player(h);
    var f = fixer(h, p, 0);
    accept(h, p, f);
    var target = mark(h, p);
    float max = target.getMaxHealth();
    h.assertTrue(
        max >= 60
            && target.getArmorValue() >= 4
            && target.getAttributeValue(
                    net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
                > 4,
        "Named target remains ordinary chromed strength");
    target.setHealth(10);
    for (int i = 0; i < 3; i++) BountyCombat.apply(target, 1, false, false);
    h.assertTrue(
        target.getMaxHealth() == max && target.getHealth() == 10,
        "Reload stacked chassis or healed target");
    for (int tier = 1; tier <= 5; tier++)
      for (int count = 1; count <= 6; count++)
        h.assertTrue(
            BountyCombat.loadout(tier, count).size() == count, "Combat loadout incomplete");
    BountyContracts.abandon(p, f);
    h.succeed();
  }

  @GameTest(batch = "noDamageBonus", template = "empty", timeoutTicks = 60)
  public static void noDamageOptionalOnlyFailsOnActualHpLossAndStopsAtCompletion(GameTestHelper h) {
    var p = AugmentGameTests.player(h);
    var f = fixer(h, p, 0);
    accept(h, p, f);
    p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_ABSORPTION)
        .setBaseValue(10);
    p.setAbsorptionAmount(10);
    p.hurt(h.getLevel().damageSources().generic(), 5);
    h.assertTrue(
        BountyContracts.job(p).getBoolean("untouched"),
        "Fully absorbed hit lost HP-only objective");
    p.setAbsorptionAmount(0);
    p.invulnerableTime = 0;
    p.hurt(h.getLevel().damageSources().generic(), 1);
    h.assertTrue(
        !BountyContracts.job(p).getBoolean("untouched"), "Actual HP damage kept the bonus");
    var job = BountyContracts.job(p);
    job.putInt("stage", 2);
    job.putBoolean("untouched", true);
    BountyLedger.get(p.server).put(p.getUUID(), job);
    p.invulnerableTime = 0;
    p.hurt(h.getLevel().damageSources().generic(), 1);
    h.assertTrue(
        BountyContracts.job(p).getBoolean("untouched"), "Damage after completion rewrote result");
    clear(p);
    f.discard();
    h.succeed();
  }
}
