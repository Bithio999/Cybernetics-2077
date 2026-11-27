package dev.cybernetics2077.cyberware;

import static dev.cybernetics2077.cyberware.Augment.*;

import java.util.*;
import net.minecraft.world.entity.LivingEntity;

/** Minecraft-scale definitions for the remaining current (2.x) non-arm catalogue. */
public final class ExpansionImplants {
  public static boolean added(Augment a) {
    return a.ordinal() > DAVIDS_SANDEVISTAN.ordinal();
  }

  public static boolean sandy(Augment a) {
    return a == DYNALAR_SANDEVISTAN
        || a == MILITECH_APOGEE
        || a == DAVIDS_SANDEVISTAN
        || a == ZETATECH_SANDEVISTAN
        || a == QIANT_WARP_DANCER
        || a == MILITECH_FALCON;
  }

  public static boolean berserk(Augment a) {
    return a == BIODYNE_BERSERK
        || a == MOORE_TECH_BERSERK
        || a == ZETATECH_BERSERK
        || a == MILITECH_BERSERK;
  }

  public static Augment os(LivingEntity e) {
    var stack =
        e.getData(dev.cybernetics2077.Cybernetics2077.CYBERWARE)
            .getStackInSlot(CyberwareCategory.OPERATING_SYSTEM.firstSlot());
    return stack.getItem() instanceof AugmentItem i ? i.augment() : null;
  }

  public static double armor(Augment a, int t) {
    if (t <= 0 || !added(a)) return 0;
    if (a.category == CyberwareCategory.INTEGUMENTARY_SYSTEM) return t * (a == CHITIN ? 1.6 : .8);
    if (a.category == CyberwareCategory.SKELETON && a != RARA_AVIS) return t * .6;
    if (a == LEEROY_LIGAMENT) return t * .4;
    return 0;
  }

  public static List<String> details(Augment a, int t) {
    String s =
        switch (a) {
          case AXOLOTL -> "Kills recover " + (t * 1.5) + "% of active cyberware cooldowns.";
          case NEWTON_MODULE -> "Kills recover " + (t * .3) + "% of active cyberware cooldowns.";
          case BIOCONDUCTOR ->
              "Quickhacks gain " + (t * 8) + "% crit chance (1.5× damage). -4 max RAM.";
          case COX_2 -> "Quickhacks always critically hit for 1.5× damage. -4 max RAM.";
          case CAMILLO_RAM_MANAGER ->
              "Below 20% RAM: restore " + (10 + 4 * t) + "% RAM. +2 max RAM / recharge 80s.";
          case RAM_REALLOCATOR ->
              "Below 20% RAM: restore " + (20 + 5 * t) + "% RAM. +2 max RAM / recharge 75s.";
          case KERENZIKOV_BOOST -> "Kerenzikov lasts 1s longer and slows enemies by another 10%.";
          case MECHATRONIC_CORE ->
              "+" + (t * 8) + "% damage against golems and chromed enemies. +2 max RAM.";
          case QUANTUM_TUNER ->
              "Restores up to 50s of a spent active's cooldown. Rearms after 60s out of combat;"
                  + " other cooldowns -15%.";
          case ZETATECH_SANDEVISTAN ->
              "O: 30% slow on ground / 60% in air. "
                  + (5 + t)
                  + "s / "
                  + (50 - 6 * t)
                  + "s recharge. Airborne shots gain +"
                  + (t * 8)
                  + "% headshot damage.";
          case QIANT_WARP_DANCER ->
              "O: 20% slow for "
                  + (5 + t)
                  + "s / "
                  + (90 - 10 * t)
                  + "s recharge. +24% mitigation chance and +60% elemental resistance while"
                  + " active.";
          case MILITECH_FALCON ->
              "O: 70% slow for 10s / recharge 30s. +10% damage, +15% crit chance. Kills heal 12% HP"
                  + " and extend battery.";
          case MOORE_TECH_BERSERK ->
              "I: Berserk for "
                  + (6 + t)
                  + "s / recharge "
                  + (60 - 5 * t)
                  + "s. -"
                  + (t * 10)
                  + "% damage; 25% HP floor. Melee only.";
          case ZETATECH_BERSERK ->
              "I: Berserk for "
                  + (6 + t)
                  + "s / recharge "
                  + (50 - 5 * t)
                  + "s. +20% melee speed, -30% fall damage. Airborne melee slams nearby enemies.";
          case MILITECH_BERSERK ->
              "I: Berserk for 12s / recharge 25s. Damage immunity, +30% melee speed and +20%"
                  + " movement. Up to +50% melee damage at low HP.";
          case CHROME_COMPRESSOR ->
              "+" + (t == 4 ? 40 : 70) + " cyberware capacity. Occupies Operating System.";
          case COCKATRICE -> "Optical scanner. +35% weapon crit chance.";
          case SENTRY ->
              "Scanning highlights armed/chromed enemies and golems in "
                  + (24 + 4 * t)
                  + " blocks for 60s.";
          case ORACLE ->
              "Optical scanner combines enemy, threat and cover highlights within "
                  + (24 + 4 * t)
                  + " blocks.";
          case BEHAVIORAL_FACEPLATE ->
              "J: shed mob pursuit when out of combat for 5s. Disguise for 30s / recharge 15"
                  + " minutes. Players can still recognize you.";
          case FEEN_X -> "Below 7 RAM: +" + (t * 50) + "% RAM regeneration. Requires a cyberdeck.";
          case KINETIC_FRAME ->
              "Above 85% hunger (mobs: above 85% HP): +" + (t * 3.2) + "% mitigation chance.";
          case RARA_AVIS -> "+40% total armor.";
          case RAM_RECOUP ->
              "Damage received restores " + (t * .56) + "% RAM per point of damage. +2 max RAM.";
          case SCAR_COALESCER -> "Below 50% HP: +" + (t * 4.4) + "% total armor.";
          case SCARAB -> "Crouching: +" + (t * 1.2) + " armor, -20% movement speed.";
          case SPRING_JOINTS ->
              "+" + (t * 3.2) + "% mitigation strength; total mitigation strength capped at 90%.";
          case TITANIUM_BONES ->
              "B: open a "
                  + (9 * ((t + 1) / 2))
                  + "-slot carry reserve. Contents persist when uninstalled.";
          case UNIVERSAL_BOOSTER ->
              "Blood Pump/healing potions grant +"
                  + (t * 2)
                  + "% armor and faster attack recovery for 5s.";
          case TYGER_IMPRINT ->
              "Enables Smart Link guidance at Tier 1 strength; no RAM/crit bonus. +20% lock"
                  + " duration.";
          case JOHNNYS_SPECIAL, TOGETHER_FOREVER ->
              "Cosmetic dermal tattoo. No capacity cost or combat bonus.";
          case ADRENO_TRIGGER ->
              "Entering combat grants +30% movement speed for " + (15 + 5 * t) + "s; rearm 40s.";
          case ATOMIC_SENSORS ->
              "Before combat, a hostile enemy within 16 blocks grants up to +"
                  + (t * 12.8)
                  + "% approach speed.";
          case DEEP_FIELD ->
              "Weapon crit chance increases with distance, up to 100% at 100 blocks.";
          case KERENZIKOV ->
              "Aim a ranged weapon while sprinting or airborne: 60% enemy slow for "
                  + (2 + t * .35)
                  + "s / recharge 6s.";
          case REFLEX_TUNER ->
              "Below 25% HP: 60% slow for "
                  + ((45 + t * 9) / 20.)
                  + "s, including your own movement. Recharge 35s.";
          case REVULSOR ->
              "Below 25% HP: 60% enemy slow for 4.5s. You keep full movement speed. Recharge 35s.";
          case SYNAPTIC_ACCELERATOR ->
              "Entering combat: 50% slow for "
                  + (2 + t * .5)
                  + "s, including your movement. Recharge 60s.";
          case TYROSINE_INJECTOR ->
              "Melee kills grant +"
                  + (t * 4.2)
                  + "% headshot damage and +"
                  + (t * 2.2)
                  + "% movement for 15s.";
          case ADRENALINE_BOOSTER ->
              "Melee kills restore " + t + " hunger and reduce jump recovery (NPCs).";
          case BLACK_MAMBA ->
              "Against poisoned targets: +"
                  + (t * 4.4)
                  + "% non-poison damage; your poison damage -90%.";
          case CLUTCH_PADDING ->
              "Ranged attack recovery +" + (t * 4) + "%. Works with TaCZ firing mechanisms.";
          case ELECTROMAG_RECYCLER ->
              "Steady aimed TaCZ shots / fully drawn bows heal 5% HP and restore 1 hunger on hit"
                  + " (1s rearm).";
          case FEEDBACK_CIRCUIT ->
              "Steady aimed TaCZ shots / fully drawn bows heal "
                  + (t * .7)
                  + "% HP on hit (1s rearm).";
          case ISOMETRIC_STABILIZER ->
              "All weapon attack recovery +20%, including TaCZ mechanisms.";
          case MICROROTORS -> "+" + (t * 5) + "% melee attack speed.";
          case THREATEVAC ->
              "Below 25% HP: movement speed rises toward +" + (t * 7.8) + "% as HP falls.";
          case CARAPACE -> "Side/rear attacks are reduced by " + (t * 6.4) + "%.";
          case CELLULAR_ADAPTER ->
              "+"
                  + (t * 2)
                  + "% explosion resistance, +"
                  + (t * 2)
                  + "% steady aimed ranged damage; medical recharge +"
                  + (t * 2)
                  + "%.";
          case CHITIN -> "After 5s without damage, regenerate 1% maximum HP each second.";
          case COGITO_LATTICE -> "Below 7 RAM with a cyberdeck: +" + (t * 1.5) + " armor.";
          case COUNTERSHELL ->
              "Lose 35% HP within 3s: +50% mitigation chance for 4s / recharge 6s.";
          case DEFENZIKOV -> "When Kerenzikov ends: +90% mitigation chance for 4s.";
          case NANO_PLATING ->
              (t * 1.4)
                  + "% chance to block a projectile, doubled while sprinting/airborne; at most 3"
                  + " blocks per 5s.";
          case PAINDUCER ->
              "Defers 30% of incoming damage over 5s; delayed damage cannot be mitigated again.";
          case PERIPHERAL_INVERSE ->
              "Up to -45% damage from attackers within 3 blocks, fading to 0% at 6 blocks.";
          case PROXISHIELD ->
              "Up to -" + (t * 4) + "% damage within 3 blocks, fading to 0% at 6 blocks.";
          case RANGEGUARD -> "+" + (t * 1.2) + " armor while no hostile enemy is within 6 blocks.";
          case LEEROY_LIGAMENT -> "+20% movement speed.";
          default -> throw new IllegalArgumentException("Not an expansion implant: " + a);
        };
    var result = new ArrayList<String>();
    result.add(s);
    if (armor(a, t) > 0)
      result.add(
          "+"
              + Augment.number(armor(a, t))
              + " armor / +"
              + Augment.number(t * .2)
              + " toughness.");
    return List.copyOf(result);
  }

  private ExpansionImplants() {}
}
