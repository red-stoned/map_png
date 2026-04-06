package com.holebois.map_png.mixin.client;

import net.minecraft.client.resources.MapTextureManager;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MapTextureManager.class)
public interface MapRendererInvoker {

    @Invoker("getOrCreateMapInstance")
    MapTextureManager.MapInstance mappng$invokeGetMapTexture(MapId id, MapItemSavedData state);
}
