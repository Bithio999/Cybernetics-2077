package dev.cybernetics2077.port;

import java.util.*;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.*;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "cybernetics2077")
public final class PortData {
  private static final Map<String, DataKey<?>> KEYS = new LinkedHashMap<>();
  private static final Capability<Storage> CAP = CapabilityManager.get(new CapabilityToken<>() {});

  public static <T extends INBTSerializable<CompoundTag>> Supplier<DataKey<T>> register(
      String name, Supplier<DataKey<T>> factory) {
    DataKey<T> key = factory.get();
    KEYS.put(name, key);
    return () -> key;
  }

  public static <T extends INBTSerializable<CompoundTag>> T get(
      Entity entity, Supplier<DataKey<T>> key) {
    return entity
        .getCapability(CAP)
        .orElseThrow(() -> new IllegalStateException("Missing cyberware capability"))
        .get(key.get());
  }

  public static <T extends INBTSerializable<CompoundTag>> void set(
      Entity entity, Supplier<DataKey<T>> key, T value) {
    entity.getCapability(CAP).ifPresent(s -> s.values.put(key.get(), value));
  }

  @SubscribeEvent
  public static void attach(AttachCapabilitiesEvent<Entity> e) {
    var s = new Storage();
    e.addCapability(new ResourceLocation("cybernetics2077", "data"), s);
    e.addListener(s.optional::invalidate);
  }

  @SubscribeEvent
  public static void clone(PlayerEvent.Clone e) {
    e.getOriginal().reviveCaps();
    e.getOriginal()
        .getCapability(CAP)
        .ifPresent(
            old ->
                e.getEntity()
                    .getCapability(CAP)
                    .ifPresent(next -> next.deserializeNBT(old.serializeNBT())));
    e.getOriginal().invalidateCaps();
  }

  public static final class Storage implements ICapabilitySerializable<CompoundTag> {
    final Map<DataKey<?>, INBTSerializable<CompoundTag>> values = new IdentityHashMap<>();
    final LazyOptional<Storage> optional = LazyOptional.of(() -> this);

    @SuppressWarnings("unchecked")
    <T extends INBTSerializable<CompoundTag>> T get(DataKey<T> key) {
      return (T) values.computeIfAbsent(key, k -> key.factory.get());
    }

    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
      return cap == CAP ? optional.cast() : LazyOptional.empty();
    }

    public CompoundTag serializeNBT() {
      var t = new CompoundTag();
      KEYS.forEach(
          (id, key) -> {
            if (values.containsKey(key)) t.put(id, values.get(key).serializeNBT());
          });
      return t;
    }

    public void deserializeNBT(CompoundTag t) {
      KEYS.forEach(
          (id, key) -> {
            if (t.contains(id)) get(key).deserializeNBT(t.getCompound(id));
          });
    }
  }
}
