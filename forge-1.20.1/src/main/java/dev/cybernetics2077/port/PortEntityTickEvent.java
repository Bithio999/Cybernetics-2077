package dev.cybernetics2077.port;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.Event;

public class PortEntityTickEvent extends Event {
  private final Entity entity;

  PortEntityTickEvent(Entity entity) {
    this.entity = entity;
  }

  public Entity getEntity() {
    return entity;
  }

  public static final class Pre extends PortEntityTickEvent {
    public Pre(Entity e) {
      super(e);
    }
  }

  public static final class Post extends PortEntityTickEvent {
    public Post(Entity e) {
      super(e);
    }
  }
}
