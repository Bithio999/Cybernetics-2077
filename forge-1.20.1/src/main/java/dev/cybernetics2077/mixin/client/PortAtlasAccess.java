package dev.cybernetics2077.mixin.client;

import java.util.Map;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TextureAtlas.class)
public interface PortAtlasAccess {
  @Accessor("texturesByName")
  Map<ResourceLocation, TextureAtlasSprite> cybernetics$sprites();
}
