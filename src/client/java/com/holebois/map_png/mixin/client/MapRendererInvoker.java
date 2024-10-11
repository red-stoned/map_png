package com.holebois.map_png.mixin.client;

// import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.texture.MapTextureManager;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MapTextureManager.class)
public interface MapRendererInvoker {

    @Invoker("getMapTexture")
    MapTextureManager.MapTexture invokeGetMapTexture(MapIdComponent id, MapState state);
}
