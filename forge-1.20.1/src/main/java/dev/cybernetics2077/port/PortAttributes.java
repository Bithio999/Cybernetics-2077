package dev.cybernetics2077.port;

public final class PortAttributes {
  public static java.util.UUID uuid(Object id) {
    return id instanceof java.util.UUID uuid
        ? uuid
        : java.util.UUID.nameUUIDFromBytes(
            id.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }
}
