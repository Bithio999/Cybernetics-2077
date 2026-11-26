package dev.cybernetics2077.port;

import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public final class DataKey<T extends INBTSerializable<CompoundTag>> {
  final Supplier<T> factory;

  DataKey(Supplier<T> factory) {
    this.factory = factory;
  }

  public static <T extends INBTSerializable<CompoundTag>> DataKey<T> serializable(
      Supplier<T> factory) {
    return new DataKey<>(factory);
  }

  public DataKey<T> copyOnDeath() {
    return this;
  }

  public DataKey<T> build() {
    return this;
  }
}
