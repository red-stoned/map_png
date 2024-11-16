package com.holebois.map_png.mixin.client;

import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.texture.NativeImageBackedTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MapRenderer.MapTexture.class)
public interface MapTextureAccessor {

    @Accessor
    NativeImageBackedTexture getTexture();
}
