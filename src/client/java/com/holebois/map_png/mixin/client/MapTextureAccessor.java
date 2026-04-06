package com.holebois.map_png.mixin.client;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.MapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MapTextureManager.MapInstance.class)
public interface MapTextureAccessor {

    @Accessor("texture")
    DynamicTexture mappng$getTexture();
}
