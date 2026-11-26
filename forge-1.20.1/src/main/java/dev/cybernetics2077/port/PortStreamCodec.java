package dev.cybernetics2077.port;

import java.util.function.*;

public record PortStreamCodec<B, T>(BiConsumer<B, T> encode, Function<B, T> decode) {
  public void encode(B buffer, T value) {
    encode.accept(buffer, value);
  }

  public T decode(B buffer) {
    return decode.apply(buffer);
  }

  public static <B, T> PortStreamCodec<B, T> of(BiConsumer<B, T> encode, Function<B, T> decode) {
    return new PortStreamCodec<>(encode, decode);
  }

  public static <B, T> PortStreamCodec<B, T> unit(T value) {
    return of((b, v) -> {}, b -> value);
  }
}
