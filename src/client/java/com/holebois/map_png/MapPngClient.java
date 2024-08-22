package com.holebois.map_png;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.holebois.map_png.mixin.client.MapRendererInvoker;
import com.holebois.map_png.mixin.client.MapTextureAccessor;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;



public class MapPngClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("map_png");
	private static final KeyBinding download_key = new KeyBinding(
		"key.map_png.download",
		InputUtil.Type.KEYSYM,
		GLFW.GLFW_KEY_F8,
		"key.category.map_png");

    public static void showToast(Text title, Text msg) {
        new Thread(() -> {
            try {
                SystemToast t = new SystemToast(SystemToast.Type.PACK_LOAD_FAILURE, title, msg);
                MinecraftClient.getInstance().getToastManager().add(t);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }).start();
    }

	public void downloadMap(MapState mapState, MapIdComponent mapId) {
        MinecraftClient client = MinecraftClient.getInstance();
		MapRenderer.MapTexture texture = ((MapRendererInvoker)client.gameRenderer.getMapRenderer()).invokeGetMapTexture(mapId, mapState);
        
        File save_dir = new File(client.runDirectory, "maps");
        if (client.isInSingleplayer()) {
            save_dir = new File(save_dir, "singleplayer");
            save_dir = new File(save_dir, client.getServer().getSavePath(WorldSavePath.ROOT).getParent().getFileName().toString());
        } else {
            save_dir = new File(save_dir, "multiplayer");
            save_dir = new File(save_dir, client.getCurrentServerEntry().address.replace(":", "_"));
        }


        if(!save_dir.exists() && !save_dir.mkdirs()) {
            LOGGER.error("Could not create directory " + save_dir.getAbsolutePath() + " cannot continue!");
            return;
        }
        
        File mapfile = new File(save_dir, "map_" + mapId.id() + ".png");

        try {
            Optional<NativeImageBackedTexture> backing_texture = Optional.ofNullable(((MapTextureAccessor)texture).getTexture());
            if (backing_texture.isEmpty()) { // immediatelyFast workaround
                Field atlasf = MapRenderer.MapTexture.class.getDeclaredField("immediatelyFast$atlasTexture");
                atlasf.setAccessible(true);
                Object atlas = atlasf.get(texture);

                Field atlasx = MapRenderer.MapTexture.class.getDeclaredField("immediatelyFast$atlasX");
                atlasx.setAccessible(true);
                int x = (int) atlasx.get(texture);
                Field atlasy = MapRenderer.MapTexture.class.getDeclaredField("immediatelyFast$atlasY");
                atlasy.setAccessible(true);
                int y = (int) atlasy.get(texture);

                
                
                Method getTextureMethod = atlas.getClass().getDeclaredMethod("getTexture");
                NativeImageBackedTexture back = (NativeImageBackedTexture) getTextureMethod.invoke(atlas);

                NativeImage img = new NativeImage(128, 128, true);
                back.getImage().copyRect(img, x, y, 0, 0, 128, 128, false, false);
                img.writeTo(mapfile); 
                img.close();
            } else {
                backing_texture.get().getImage().writeTo(mapfile);
            }
            

            Text text = (Text.literal (mapfile.getName())).formatted(Formatting.UNDERLINE, Formatting.GREEN).styled((style) ->
            style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, mapfile.getAbsolutePath())));
            client.player.sendMessage(Text.translatable("map_png.success", text), false);
        } catch (Exception e) {
            e.printStackTrace();
            showToast(Text.translatable("map_png.error.title"), Text.translatable("map_png.error.msg"));
        }
	}

    public static MapIdComponent getMapId(ItemStack stack) {
        return stack.get(DataComponentTypes.MAP_ID);
    }

	

	@Override
	public void onInitializeClient() {
		KeyBindingHelper.registerKeyBinding(download_key);
		
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (download_key.wasPressed()) {
				ItemStack held = client.player.getMainHandStack();
				if (held.getItem() == Items.FILLED_MAP) {
					MapState mapState = FilledMapItem.getMapState(held, client.world);
                    downloadMap(mapState, getMapId(held));
					return;
				}
                HitResult hit = client.crosshairTarget;
                if (hit.getType() == HitResult.Type.ENTITY) {
                    EntityHitResult entityHit = (EntityHitResult) hit;
                    if (entityHit.getEntity() instanceof ItemFrameEntity) {
                        ItemFrameEntity frame = (ItemFrameEntity) entityHit.getEntity();
                        ItemStack map = frame.getHeldItemStack();
                        if (map.getItem() == Items.FILLED_MAP) {
                            MapState mapState = FilledMapItem.getMapState(map, client.world);
                            downloadMap(mapState, frame.getMapId());
                            return;
                        }
                    
                    }
                }

                showToast(Text.translatable("map_png.invalid_target.title"), Text.translatable("map_png.invalid_target.msg"));
				
			}
		});



		LOGGER.info("map_png client initialized!");
	}
}