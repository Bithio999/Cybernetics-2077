package dev.cybernetics2077.mixin.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.*;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpriteContents.class)
public abstract class PortSpriteMetadataMixin
    implements dev.cybernetics2077.port.PortSpriteMetadata {
  @Unique private AnimationMetadataSection cybernetics$meta = AnimationMetadataSection.EMPTY;

  @Inject(
      method =
          "<init>(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/client/resources/metadata/animation/FrameSize;Lcom/mojang/blaze3d/platform/NativeImage;Lnet/minecraft/client/resources/metadata/animation/AnimationMetadataSection;Lnet/minecraftforge/client/textures/ForgeTextureMetadata;)V",
      at = @At("RETURN"))
  private void cybernetics$capture(
      ResourceLocation id,
      FrameSize size,
      NativeImage image,
      AnimationMetadataSection meta,
      net.minecraftforge.client.textures.ForgeTextureMetadata forge,
      CallbackInfo ci) {
    cybernetics$meta = meta;
  }

  public AnimationMetadataSection cybernetics$metadata() {
    return cybernetics$meta;
  }
}
