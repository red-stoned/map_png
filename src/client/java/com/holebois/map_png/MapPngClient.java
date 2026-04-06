package com.holebois.map_png;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.holebois.map_png.mixin.client.MapRendererInvoker;
import com.holebois.map_png.mixin.client.MapTextureAccessor;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.MapTextureManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.LevelResource;



@SuppressWarnings("ReferenceToMixin")
public class MapPngClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("map_png");
    private static final KeyMapping.Category MAP_PNG_KEY_CATEGORY = KeyMapping.Category.register(Identifier.parse("map_png"));
	private static final KeyMapping download_key = new KeyMapping("key.map_png.download", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, MAP_PNG_KEY_CATEGORY);

    public static void showToast(Component title, Component msg) {
        SystemToast t = new SystemToast(SystemToast.SystemToastId.PACK_LOAD_FAILURE, title, msg);
        Minecraft.getInstance().getToastManager().addToast(t);
    }

	public void downloadMap(MapItemSavedData mapState, MapId mapId) {
        Minecraft client = Minecraft.getInstance();
		MapTextureManager.MapInstance texture = ((MapRendererInvoker)client.getMapTextureManager()).mappng$invokeGetMapTexture(mapId, mapState);
        
        File save_dir = new File(client.gameDirectory, "maps");
        if (client.isLocalServer()) {
            save_dir = new File(save_dir, "singleplayer");
            save_dir = new File(save_dir, client.getSingleplayerServer().getWorldPath(LevelResource.ROOT).getParent().getFileName().toString());
        } else {
            save_dir = new File(save_dir, "multiplayer");
            save_dir = new File(save_dir, client.getCurrentServer().ip.replace(":", "_"));
        }


        if(!save_dir.exists() && !save_dir.mkdirs()) {
            LOGGER.error("Could not create directory {} cannot continue!", save_dir.getAbsolutePath());
            return;
        }
        
        File mapfile = new File(save_dir, "map_" + mapId.id() + ".png");

        try {
            Optional<DynamicTexture> backing_texture = Optional.ofNullable(((MapTextureAccessor)texture).mappng$getTexture());
            // todo(piz) this is ass code from 2 years ago, pls fix
            if (backing_texture.isEmpty()) { // immediatelyFast workaround
                Field atlasf = MapTextureManager.MapInstance.class.getDeclaredField("immediatelyFast$atlasTexture");
                atlasf.setAccessible(true);
                Object atlas = atlasf.get(texture);

                Field atlasx = MapTextureManager.MapInstance.class.getDeclaredField("immediatelyFast$atlasX");
                atlasx.setAccessible(true);
                int x = (int) atlasx.get(texture);
                Field atlasy = MapTextureManager.MapInstance.class.getDeclaredField("immediatelyFast$atlasY");
                atlasy.setAccessible(true);
                int y = (int) atlasy.get(texture);

                Method getTextureMethod = atlas.getClass().getDeclaredMethod("getTexture");
                DynamicTexture back = (DynamicTexture) getTextureMethod.invoke(atlas);

                NativeImage img = new NativeImage(128, 128, true);
                back.getPixels().copyRect(img, x, y, 0, 0, 128, 128, false, false);
                img.writeToFile(mapfile); 
                img.close();
            } else {
                backing_texture.get().getPixels().writeToFile(mapfile);
            }

            Component map_file_text = Component
                .literal(mapfile.getName())
                .withStyle(style -> style
                    .withUnderlined(true)
                    .withColor(ChatFormatting.GREEN)
                    .withClickEvent(new ClickEvent.OpenFile(mapfile.getAbsolutePath()))
                );
            client.player.sendSystemMessage(Component.translatable("map_png.success", map_file_text));
        } catch (Exception e) {
            e.printStackTrace();
            showToast(Component.translatable("map_png.error.title"), Component.translatable("map_png.error.msg"));
        }
	}

    public static MapId getMapId(ItemStack stack) {
        return stack.get(DataComponents.MAP_ID);
    }

	@Override
	public void onInitializeClient() {
		KeyMappingHelper.registerKeyMapping(download_key);
		
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (download_key.consumeClick()) {
				ItemStack held = client.player.getMainHandItem();
				if (held.getItem() == Items.FILLED_MAP) {
					MapItemSavedData mapState = MapItem.getSavedData(held, client.level);
                    downloadMap(mapState, getMapId(held));
					return;
				}
                HitResult hit = client.hitResult;
                if (hit.getType() == HitResult.Type.ENTITY) {
                    EntityHitResult entityHit = (EntityHitResult) hit;
                    if (entityHit.getEntity() instanceof ItemFrame frame) {
                        ItemStack map = frame.getItem();
                        if (map.getItem() == Items.FILLED_MAP) {
                            MapItemSavedData mapState = MapItem.getSavedData(map, client.level);
                            downloadMap(mapState, frame.getFramedMapId(map));
                            return;
                        }
                    
                    }
                }

                showToast(Component.translatable("map_png.invalid_target.title"), Component.translatable("map_png.invalid_target.msg"));
			}
		});

		LOGGER.info("map_png client initialized!");
	}
}