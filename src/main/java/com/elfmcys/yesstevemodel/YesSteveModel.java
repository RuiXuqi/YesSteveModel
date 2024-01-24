package com.elfmcys.yesstevemodel;

import com.elfmcys.yesstevemodel.client.compat.FirstPersonCompat;
import com.elfmcys.yesstevemodel.client.event.*;
import com.elfmcys.yesstevemodel.client.input.*;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.event.*;
import com.elfmcys.yesstevemodel.util.NativeLibUtil;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

// Native Access
@Mod(YesSteveModel.MOD_ID)
public class YesSteveModel {
    public static final String MOD_ID = "yes_steve_model";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public YesSteveModel() throws IOException {
        NativeLibUtil.loadCoreLibrary();
        // 加载完成后由 c++ 代码调用下面的 init 方法
    }

    // Native Access
    @SuppressWarnings("DuplicatedCode,unused")
    static class Init {
        // Native Access
        private static void init() {
            ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, GeneralConfig.init());
            CommandRegistry.COMMAND_ARGUMENT_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());

            FMLJavaModLoadingContext.get().getModEventBus().addListener(CommonEvent::onSetupEvent);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(CommonEvent::registerCapability);

            MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, CapabilityEvent::onAttachCapabilityEvent);
            MinecraftForge.EVENT_BUS.addListener(CapabilityEvent::onEntityJoinWorld);
            MinecraftForge.EVENT_BUS.addListener(CapabilityEvent::onPlayerCloned);
            MinecraftForge.EVENT_BUS.addListener(CapabilityEvent::onTrackingPlayer);
            MinecraftForge.EVENT_BUS.addListener(CapabilityEvent::onPlayerTickEvent);
            MinecraftForge.EVENT_BUS.addListener(CapabilityEvent::onPlayerTickEvent);
            MinecraftForge.EVENT_BUS.addListener(CommandRegistry::onServerStaring);
            MinecraftForge.EVENT_BUS.addListener(LoginEvent::onLoggedInServer);
            MinecraftForge.EVENT_BUS.addListener(LoggedOutEvent::onPlayerLoggedOut);
            MinecraftForge.EVENT_BUS.addListener(ServerStartingEvent::onServerInit);

            if (FMLEnvironment.dist == Dist.CLIENT) {
                initClient();
            }
        }

        private static void initClient() {
            FirstPersonCompat.init();

            FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientSetupEvent::onClientSetup);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientSetupEvent::onRegisterKeyMappings);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientSetupEvent::onRegisterGuiOverlays);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(RegisterEntityRenderersEvent::clientSetup);

            MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, CapabilityEvent::onAttachClientCapabilityEvent);
            MinecraftForge.EVENT_BUS.addListener(ClientLoggedOutEvent::onPlayerLoggedOut);
            MinecraftForge.EVENT_BUS.addListener(PlayerJoinWorldEvent::onPlayerJoinWorld);
            MinecraftForge.EVENT_BUS.addListener(PlayerMoveEvent::onKeyboardInput);
            MinecraftForge.EVENT_BUS.addListener(RenderFirstPlayerBackground::onRenderHand);
            MinecraftForge.EVENT_BUS.addListener(RenderFirstPlayerBackground::onRenderLevelLase);
            MinecraftForge.EVENT_BUS.addListener(ReplacePlayerHandRenderEvent::onRenderHand);
            MinecraftForge.EVENT_BUS.addListener(ReplacePlayerRenderEvent::onRender);
            MinecraftForge.EVENT_BUS.addListener(VanillaPlayerRenderEvent::onRenderPlayer); // 没错，这也是 FML 事件

            MinecraftForge.EVENT_BUS.addListener(AnimationRouletteKey::onKeyboardInput);
            MinecraftForge.EVENT_BUS.addListener(DebugAnimationKey::onKeyboardInput);
            MinecraftForge.EVENT_BUS.addListener(ExtraAnimationKey::onKeyboardInput);
            MinecraftForge.EVENT_BUS.addListener(ExtraPlayerConfigKey::onKeyboardInput);
            MinecraftForge.EVENT_BUS.addListener(PlayerModelScreenKey::onKeyboardInput);
            MinecraftForge.EVENT_BUS.addListener(LocalPlayerTickEvent::onPlayerTick);
        }
    }
}