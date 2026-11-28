package dev.cybernetics2077.cyberware;

import java.util.List;
import java.util.Locale;

/** Current 2.x implant families. Starting tiers come from their obtainable versions. */
public enum Augment {
  EX_DISK("ex_disk", CyberwareCategory.FRONTAL_CORTEX, 3, 10),
  MEMORY_BOOST("memory_boost", CyberwareCategory.FRONTAL_CORTEX, 2, 18),
  BIODYNE_BERSERK("biodyne_berserk", CyberwareCategory.OPERATING_SYSTEM, 2, 20),
  CLAIRVOYANT("kiroshi_clairvoyant_optics", CyberwareCategory.FACE, 3, 8),
  BIONIC_JOINTS("bionic_joints", CyberwareCategory.SKELETON, 1, 4),
  EPIMORPHIC_SKELETON("epimorphic_skeleton", CyberwareCategory.SKELETON, 4, 32),
  NEOFIBER("neofiber", CyberwareCategory.NERVOUS_SYSTEM, 3, 14),
  ADRENALINE_CONVERTER("adrenaline_converter", CyberwareCategory.NERVOUS_SYSTEM, 1, 6),
  BLOOD_PUMP("blood_pump", CyberwareCategory.CIRCULATORY_SYSTEM, 2, 15),
  HEAL_ON_KILL("heal_on_kill", CyberwareCategory.CIRCULATORY_SYSTEM, 1, 10),
  SUBDERMAL_ARMOR("subdermal_armor", CyberwareCategory.INTEGUMENTARY_SYSTEM, 1, 4),
  OPTICAL_CAMO("optical_camo", CyberwareCategory.INTEGUMENTARY_SYSTEM, 2, 16),
  REINFORCED_TENDONS("reinforced_tendons", CyberwareCategory.LEGS, 2, 8),
  FORTIFIED_ANKLES("fortified_ankles", CyberwareCategory.LEGS, 2, 6),
  RAM_UPGRADE("ram_upgrade", CyberwareCategory.FRONTAL_CORTEX, 1, 8),
  SELF_ICE("self_ice", CyberwareCategory.FRONTAL_CORTEX, 3, 5),
  DOOMSAYER("kiroshi_doomsayer_optics", CyberwareCategory.FACE, 1, 6),
  STALKER("kiroshi_stalker_optics", CyberwareCategory.FACE, 1, 6),
  DENSE_MARROW("dense_marrow", CyberwareCategory.SKELETON, 2, 16),
  PARA_BELLUM("para_bellum", CyberwareCategory.SKELETON, 3, 25),
  STABBER("stabber", CyberwareCategory.NERVOUS_SYSTEM, 2, 12),
  VISUAL_CORTEX_SUPPORT("visual_cortex_support", CyberwareCategory.NERVOUS_SYSTEM, 1, 16),
  BIOMONITOR("biomonitor", CyberwareCategory.CIRCULATORY_SYSTEM, 1, 14),
  SECOND_HEART("second_heart", CyberwareCategory.CIRCULATORY_SYSTEM, 4, 30),
  SHOCK_N_AWE("shock_n_awe", CyberwareCategory.INTEGUMENTARY_SYSTEM, 1, 25),
  PAIN_EDITOR("pain_editor", CyberwareCategory.INTEGUMENTARY_SYSTEM, 4, 35),
  LYNX_PAWS("lynx_paws", CyberwareCategory.LEGS, 2, 5),
  JENKINS_TENDONS("jenkins_tendons", CyberwareCategory.LEGS, 2, 6),
  DYNALAR_SANDEVISTAN("dynalar_sandevistan", CyberwareCategory.OPERATING_SYSTEM, 2, 18),
  SMART_LINK("smart_link", CyberwareCategory.HANDS, 1, 4),
  BALLISTIC_COPROCESSOR("ballistic_coprocessor", CyberwareCategory.HANDS, 1, 2),
  MICROGENERATOR("microgenerator", CyberwareCategory.HANDS, 1, 9),
  HANDLE_WRAP("handle_wrap", CyberwareCategory.HANDS, 3, 8),
  SHOCK_ABSORBER("shock_absorber", CyberwareCategory.HANDS, 1, 12),
  IMMOVABLE_FORCE("immovable_force", CyberwareCategory.HANDS, 2, 35),
  MILITECH_APOGEE("militech_apogee_sandevistan", CyberwareCategory.OPERATING_SYSTEM, 5, 44),
  DAVIDS_SANDEVISTAN("davids_sandevistan", CyberwareCategory.OPERATING_SYSTEM, 5, 60),
  AXOLOTL("axolotl", CyberwareCategory.FRONTAL_CORTEX, 4, 55),
  BIOCONDUCTOR("bioconductor", CyberwareCategory.FRONTAL_CORTEX, 3, 20),
  CAMILLO_RAM_MANAGER("camillo_ram_manager", CyberwareCategory.FRONTAL_CORTEX, 2, 10),
  COX_2("cox_2_cybersomatic_optimizer", CyberwareCategory.FRONTAL_CORTEX, 5, 50),
  KERENZIKOV_BOOST("kerenzikov_boost_system", CyberwareCategory.FRONTAL_CORTEX, 3, 8),
  MECHATRONIC_CORE("mechatronic_core", CyberwareCategory.FRONTAL_CORTEX, 1, 5),
  NEWTON_MODULE("newton_module", CyberwareCategory.FRONTAL_CORTEX, 2, 14),
  QUANTUM_TUNER("quantum_tuner", CyberwareCategory.FRONTAL_CORTEX, 5, 45),
  RAM_REALLOCATOR("ram_reallocator", CyberwareCategory.FRONTAL_CORTEX, 4, 40),
  ZETATECH_SANDEVISTAN("zetatech_sandevistan", CyberwareCategory.OPERATING_SYSTEM, 2, 14),
  QIANT_WARP_DANCER("qiant_warp_dancer", CyberwareCategory.OPERATING_SYSTEM, 2, 12),
  MILITECH_FALCON("militech_falcon_sandevistan", CyberwareCategory.OPERATING_SYSTEM, 4, 39),
  MOORE_TECH_BERSERK("moore_tech_berserk", CyberwareCategory.OPERATING_SYSTEM, 2, 12),
  ZETATECH_BERSERK("zetatech_berserk", CyberwareCategory.OPERATING_SYSTEM, 2, 16),
  MILITECH_BERSERK("militech_berserk", CyberwareCategory.OPERATING_SYSTEM, 4, 35),
  CHROME_COMPRESSOR("chrome_compressor", CyberwareCategory.OPERATING_SYSTEM, 4, 0),
  COCKATRICE("kiroshi_cockatrice_optics", CyberwareCategory.FACE, 5, 30),
  SENTRY("kiroshi_sentry_optics", CyberwareCategory.FACE, 1, 2),
  ORACLE("kiroshi_oracle_optics", CyberwareCategory.FACE, 4, 10),
  BEHAVIORAL_FACEPLATE("behavioral_imprint_synced_faceplate", CyberwareCategory.FACE, 5, 6),
  FEEN_X("feen_x", CyberwareCategory.SKELETON, 3, 16),
  KINETIC_FRAME("kinetic_frame", CyberwareCategory.SKELETON, 2, 16),
  RARA_AVIS("rara_avis", CyberwareCategory.SKELETON, 5, 60),
  RAM_RECOUP("ram_recoup", CyberwareCategory.SKELETON, 1, 14),
  SCAR_COALESCER("scar_coalescer", CyberwareCategory.SKELETON, 3, 20),
  SCARAB("scarab", CyberwareCategory.SKELETON, 2, 14),
  SPRING_JOINTS("spring_joints", CyberwareCategory.SKELETON, 2, 16),
  TITANIUM_BONES("titanium_bones", CyberwareCategory.SKELETON, 1, 6),
  UNIVERSAL_BOOSTER("universal_booster", CyberwareCategory.SKELETON, 4, 25),
  TYGER_IMPRINT("tyger_claws_dermal_imprint", CyberwareCategory.HANDS, 1, 0),
  JOHNNYS_SPECIAL("johnnys_special", CyberwareCategory.HANDS, 1, 0),
  TOGETHER_FOREVER("together_forever", CyberwareCategory.HANDS, 1, 0),
  ADRENO_TRIGGER("adreno_trigger", CyberwareCategory.NERVOUS_SYSTEM, 4, 20),
  ATOMIC_SENSORS("atomic_sensors", CyberwareCategory.NERVOUS_SYSTEM, 1, 5),
  DEEP_FIELD("deep_field_visual_interface", CyberwareCategory.NERVOUS_SYSTEM, 4, 40),
  KERENZIKOV("kerenzikov", CyberwareCategory.NERVOUS_SYSTEM, 2, 12),
  REFLEX_TUNER("reflex_tuner", CyberwareCategory.NERVOUS_SYSTEM, 2, 8),
  REVULSOR("revulsor", CyberwareCategory.NERVOUS_SYSTEM, 4, 35),
  SYNAPTIC_ACCELERATOR("synaptic_accelerator", CyberwareCategory.NERVOUS_SYSTEM, 2, 5),
  TYROSINE_INJECTOR("tyrosine_injector", CyberwareCategory.NERVOUS_SYSTEM, 1, 8),
  ADRENALINE_BOOSTER("adrenaline_booster", CyberwareCategory.CIRCULATORY_SYSTEM, 1, 14),
  BLACK_MAMBA("black_mamba", CyberwareCategory.CIRCULATORY_SYSTEM, 3, 16),
  CLUTCH_PADDING("clutch_padding", CyberwareCategory.CIRCULATORY_SYSTEM, 2, 12),
  ELECTROMAG_RECYCLER("electromag_recycler", CyberwareCategory.CIRCULATORY_SYSTEM, 4, 40),
  FEEDBACK_CIRCUIT("feedback_circuit", CyberwareCategory.CIRCULATORY_SYSTEM, 3, 14),
  ISOMETRIC_STABILIZER("isometric_stabilizer", CyberwareCategory.CIRCULATORY_SYSTEM, 4, 40),
  MICROROTORS("microrotors", CyberwareCategory.CIRCULATORY_SYSTEM, 2, 12),
  THREATEVAC("threatevac", CyberwareCategory.CIRCULATORY_SYSTEM, 2, 5),
  CARAPACE("carapace", CyberwareCategory.INTEGUMENTARY_SYSTEM, 2, 16),
  CELLULAR_ADAPTER("cellular_adapter", CyberwareCategory.INTEGUMENTARY_SYSTEM, 3, 30),
  CHITIN("chitin", CyberwareCategory.INTEGUMENTARY_SYSTEM, 5, 50),
  COGITO_LATTICE("cogito_lattice", CyberwareCategory.INTEGUMENTARY_SYSTEM, 3, 12),
  COUNTERSHELL("countershell", CyberwareCategory.INTEGUMENTARY_SYSTEM, 2, 12),
  DEFENZIKOV("defenzikov", CyberwareCategory.INTEGUMENTARY_SYSTEM, 3, 25),
  NANO_PLATING("nano_plating", CyberwareCategory.INTEGUMENTARY_SYSTEM, 2, 20),
  PAINDUCER("painducer", CyberwareCategory.INTEGUMENTARY_SYSTEM, 4, 30),
  PERIPHERAL_INVERSE("peripheral_inverse", CyberwareCategory.INTEGUMENTARY_SYSTEM, 4, 40),
  PROXISHIELD("proxishield", CyberwareCategory.INTEGUMENTARY_SYSTEM, 2, 10),
  RANGEGUARD("rangeguard", CyberwareCategory.INTEGUMENTARY_SYSTEM, 2, 14),
  LEEROY_LIGAMENT("leeroy_ligament_system", CyberwareCategory.LEGS, 4, 8),
  CREATIVE_SANDEVISTAN("creative_sandevistan", CyberwareCategory.OPERATING_SYSTEM, 5, 0);

  public final String id;
  public final CyberwareCategory category;
  public final int firstTier, capacity;

  Augment(String id, CyberwareCategory category, int firstTier, int capacity) {
    this.id = id;
    this.category = category;
    this.firstTier = firstTier;
    this.capacity = capacity;
  }

  public String translation() {
    return "cybernetics2077.augment." + id;
  }

  public boolean active() {
    return this == BIODYNE_BERSERK
        || this == BLOOD_PUMP
        || this == OPTICAL_CAMO
        || this == DYNALAR_SANDEVISTAN
        || this == MILITECH_APOGEE
        || this == DAVIDS_SANDEVISTAN
        || category == CyberwareCategory.OPERATING_SYSTEM && this != CHROME_COMPRESSOR
        || this == BEHAVIORAL_FACEPLATE;
  }

  public boolean creativeOnly() {
    return this == CREATIVE_SANDEVISTAN;
  }

  public boolean iconic() {
    return this == MILITECH_APOGEE
        || this == DAVIDS_SANDEVISTAN
        || this == IMMOVABLE_FORCE
        || this == AXOLOTL
        || this == COX_2
        || this == QUANTUM_TUNER
        || this == RAM_REALLOCATOR
        || this == MILITECH_FALCON
        || this == MILITECH_BERSERK
        || this == CHROME_COMPRESSOR
        || this == COCKATRICE
        || this == BEHAVIORAL_FACEPLATE
        || this == RARA_AVIS
        || this == ADRENO_TRIGGER
        || this == DEEP_FIELD
        || this == REVULSOR
        || this == ELECTROMAG_RECYCLER
        || this == ISOMETRIC_STABILIZER
        || this == CHITIN
        || this == PERIPHERAL_INVERSE
        || this == LEEROY_LIGAMENT;
  }

  public boolean optics() {
    return category == CyberwareCategory.FACE && this != BEHAVIORAL_FACEPLATE;
  }

  public int scanRange(int tier) {
    return this == DOOMSAYER || this == STALKER
        ? 24 + 4 * tier
        : CyberwareTier.of(tier).scanRange();
  }

  public double armor(int tier) {
    if (tier == 0) return 0;
    return switch (this) {
      case BIONIC_JOINTS -> tier + 1;
      case EPIMORPHIC_SKELETON -> tier * 2 - 2;
      case SUBDERMAL_ARMOR, PAIN_EDITOR -> tier;
      case SHOCK_N_AWE -> (tier + 1) / 2;
      default -> ExpansionImplants.armor(this, tier);
    };
  }

  public double toughness(int tier) {
    return tier
        * switch (this) {
          case BIONIC_JOINTS, PARA_BELLUM -> .3;
          case EPIMORPHIC_SKELETON, PAIN_EDITOR -> .4;
          case SUBDERMAL_ARMOR, SHOCK_N_AWE -> .2;
          default -> ExpansionImplants.armor(this, tier) > 0 ? .2 : 0;
        };
  }

  public static String number(double value) {
    return String.format(Locale.ROOT, "%.1f", value);
  }

  public List<String> details(int t) {
    return switch (this) {
      case CREATIVE_SANDEVISTAN ->
          List.of(
              "Creative only. O: 99.9% slow / 32 blocks.",
              "Battery 10s / recharge 30s. Early off saves charge. Rearm 1s.");
      case DAVIDS_SANDEVISTAN ->
          List.of(
              "O: 95% slow / 32 blocks. Battery 10s / recharge 40s.",
              "+25% crit chance, +30% crit damage, +25% headshot damage.",
              "Kills: +1s battery, heal 10% HP (1s cooldown), -0.88 exhaustion.",
              "Early deactivation saves charge. Rearm: 1s.");
      case MILITECH_APOGEE ->
          List.of(
              "O: 85% slow / 32 blocks. Battery 6s / recharge 25s.",
              "+20% crit chance, crit damage and projectile headshot damage.",
              "Kills: +0.6s battery, -0.88 exhaustion. Rearm: 1s.");
      case DYNALAR_SANDEVISTAN ->
          List.of(
              "O: 50% slow / 32 blocks. Battery " + (5 + t) + "s / recharge " + (55 - t * 5) + "s.",
              "+" + (t * 3) + "% weapon crit chance and crit damage while active.",
              "Early off saves charge. Rearm 1s / minimum charge 20%.");
      case SMART_LINK ->
          List.of(
              "Scan to guide arrows, tridents and TaCZ rounds toward a target.",
              "Lock: "
                  + (2 + t)
                  + "s / "
                  + (24 + 4 * t)
                  + " blocks. Requires optics and clear sight.");
      case BALLISTIC_COPROCESSOR ->
          List.of(
              "Arrows and non-explosive TaCZ rounds ricochet once.",
              "After bounce: " + (60 + 5 * t) + "% speed / +" + (5 * t) + "% damage.",
              "Aim a gun or draw a bow to preview the bank shot.");
      case MICROGENERATOR ->
          List.of(
              "Weapon hits discharge " + (2 + 2 * t) + " electric damage within 3 blocks.",
              "Up to 4 enemies / cooldown " + (12 - t) + "s.");
      case HANDLE_WRAP ->
          List.of(
              "Drawing a weapon or deploying blades grants +" + (t * 4) + "% crit chance.",
              "Throwing a trident also triggers it. Duration 6s / rearm 10s.");
      case SHOCK_ABSORBER ->
          List.of(
              "While aiming: -" + (30 + 10 * t) + "% knockback.",
              "TaCZ: -" + (8 * t) + "% camera recoil.");
      case IMMOVABLE_FORCE ->
          List.of(
              "Iconic stability: -" + (t * 10) + "% TaCZ recoil / -" + (t * 8) + "% spread.",
              "Bows, crossbows and tridents gain 1 charge tick every "
                  + Math.max(2, 6 - t)
                  + " ticks.",
              "Arrows: -" + (20 + 8 * t) + "% gravity. Works while moving or airborne.");
      case EX_DISK -> List.of("+" + t + " RAM / +" + (t * 5) + "% quickhack upload speed.");
      case MEMORY_BOOST -> List.of("+1 RAM. Kills restore " + number(.35 + .2 * t) + " RAM.");
      case RAM_UPGRADE ->
          List.of(
              "+"
                  + (1 + t / 3)
                  + " RAM / +"
                  + String.format(Locale.ROOT, "%.2f", .06 * t)
                  + " RAM/s.");
      case SELF_ICE ->
          List.of("Blocks one incoming quickhack. +2 RAM.", "Recharge: " + (50 - 6 * t) + "s.");
      case BIODYNE_BERSERK ->
          List.of(
              "I: Berserk for " + (6 + t) + "s / cooldown " + (60 - t * 5) + "s.",
              "-" + (t * 10) + "% damage taken; HP cannot fall below 25%.",
              "+"
                  + (5 + t * 5)
                  + "% melee speed, +"
                  + (t * 5 - 5)
                  + "% crit chance, +"
                  + ((t - 1) * 25)
                  + "% crit damage.",
              "Melee only. On expiry: heal 25% HP per kill.");
      case CLAIRVOYANT ->
          List.of(
              "Scan: " + scanRange(t) + " blocks.",
              "Scanning highlights visible enemies within " + (t * 4) + " blocks for 60s.");
      case DOOMSAYER ->
          List.of(
              "Scan: " + scanRange(t) + " blocks.",
              "Marks nearby creepers, TNT and fireballs for 60s; radius " + (8 + t * 4) + ".");
      case STALKER ->
          List.of(
              "Scan: " + scanRange(t) + " blocks.",
              "Reveals enemies through cover in a " + (12 + t * 4) + "-block forward cone.",
              "Marks fade after 3s. Hacking requires clear sight.");
      case BIONIC_JOINTS ->
          List.of("+" + (t + 1) + " armor / +" + number(toughness(t)) + " toughness.");
      case EPIMORPHIC_SKELETON ->
          List.of(
              "+"
                  + (t * 2 - 2)
                  + " armor / +"
                  + number(toughness(t))
                  + " toughness / +"
                  + ((t - 2) * 5)
                  + "% max HP.");
      case SUBDERMAL_ARMOR ->
          List.of("+" + t + " armor / +" + number(toughness(t)) + " toughness.");
      case PARA_BELLUM ->
          List.of("+" + (t * 4) + "% total armor / +" + number(toughness(t)) + " toughness.");
      case PAIN_EDITOR ->
          List.of(
              "+" + t + " armor / +" + number(toughness(t)) + " toughness.",
              "-" + (t * 2) + "% incoming damage.");
      case NEOFIBER -> List.of((t * 2) + "% chance to reduce a hit by " + (50 + t * 2) + "%.");
      case ADRENALINE_CONVERTER ->
          List.of(
              "Entering combat: +" + (t * 8) + "% speed for " + (4 + t) + "s.",
              "Rearm: 20s. Requires 5s out of combat.");
      case BLOOD_PUMP ->
          List.of(
              "H: heal " + (10 + t * 5) + "% HP, then " + number(t * .5) + "% HP/s for 6s.",
              "2 charges / recharge " + (50 - t * 5) + "s.");
      case HEAL_ON_KILL -> List.of("Kills heal " + (t + 1) + "% max HP.");
      case BIOMONITOR ->
          List.of(
              "Below 50% HP: uses a Blood Pump charge or healing potion.",
              "+" + (t * 4) + "% healing received / trigger cooldown 2s.");
      case SECOND_HEART ->
          List.of(
              "A fatal hit restores full HP. Recharge: " + (400 - t * 40) + "s.",
              "Does not prevent void or /kill deaths.");
      case SHOCK_N_AWE ->
          List.of(
              "+" + number(armor(t)) + " armor / +" + number(toughness(t)) + " toughness.",
              (t * 2) + "% chance when hurt: " + (2 + 2 * t) + " electric damage within 4 blocks.",
              "Up to 8 enemies / cooldown 2s.");
      case OPTICAL_CAMO ->
          List.of(
              "C: cloak for " + (3 + t) + "s / cooldown " + (70 - t * 5) + "s.",
              "Distant mobs cannot acquire you while cloaked.");
      case REINFORCED_TENDONS ->
          List.of(
              "Jump again in midair to double-jump.",
              "Impulse: " + number(.42 + t * .035) + ". Fall damage still applies.");
      case FORTIFIED_ANKLES ->
          List.of(
              "Hold jump for 1s, then release for a high jump.",
              "Maximum impulse: " + number(.6 + t * .08) + ". Tap for a normal jump.");
      case LYNX_PAWS ->
          List.of(
              "50% quieter footsteps / +" + (t * 4) + "% crouching speed.",
              "-" + (t * 4) + "% fall damage. Sculk can still detect you.");
      case JENKINS_TENDONS ->
          List.of(
              "Sprint speed: +" + (t * 12) + "%, fading to +" + (t * 2) + "% over 5s.",
              "Recovers when not sprinting.");
      case DENSE_MARROW ->
          List.of(
              "+" + (t * 6) + "% melee damage, including Mantis Blades.",
              "Melee hits add "
                  + String.format(Locale.ROOT, "%.3f", t * .004)
                  + " hunger exhaustion.");
      case STABBER ->
          List.of(
              "+" + (t * 4) + "% crit chance: swords, blades and thrown tridents.",
              "Base crit damage: 1.5x.");
      case VISUAL_CORTEX_SUPPORT ->
          List.of("Up to +" + (t * 6) + "% weapon crit chance at 32 blocks.");
      default -> ExpansionImplants.details(this, t);
    };
  }
}
