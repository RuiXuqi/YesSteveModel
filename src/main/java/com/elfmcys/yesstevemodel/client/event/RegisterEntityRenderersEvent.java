package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.client.compat.backpack.sophisticated.SophisticatedCompat;
import com.elfmcys.yesstevemodel.client.renderer.CustomArrowRenderer;
import com.elfmcys.yesstevemodel.client.renderer.CustomPlayerRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class RegisterEntityRenderersEvent {
    private static CustomPlayerRenderer CUSTOM_PLAYER_RENDERER;
    private static CustomArrowRenderer CUSTOM_ARROW_RENDERER;

    private static void init(ResourceManager resourceManager) {
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        ItemInHandRenderer itemInHandRenderer = dispatcher.getItemInHandRenderer();
        EntityModelSet entityModels = Minecraft.getInstance().getEntityModels();
        Font font = Minecraft.getInstance().font;
        EntityRendererProvider.Context context = new EntityRendererProvider.Context(dispatcher, itemRenderer, blockRenderer, itemInHandRenderer, resourceManager, entityModels, font);
        CUSTOM_PLAYER_RENDERER = new CustomPlayerRenderer(context);
        CUSTOM_ARROW_RENDERER = new CustomArrowRenderer(context);
        SophisticatedCompat.init();
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener((ResourceManagerReloadListener) RegisterEntityRenderersEvent::init);
    }

    public static CustomPlayerRenderer getPlayerRenderer() {
        if (CUSTOM_PLAYER_RENDERER == null) {
            init(Minecraft.getInstance().getResourceManager());
        }
        return CUSTOM_PLAYER_RENDERER;
    }

    public static CustomArrowRenderer getArrowRenderer() {
        if (CUSTOM_ARROW_RENDERER == null) {
            init(Minecraft.getInstance().getResourceManager());
        }
        return CUSTOM_ARROW_RENDERER;
    }
}
