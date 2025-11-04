package com.elfmcys.yesstevemodel.client.gui.overlay;

import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapabilityProvider;
import com.elfmcys.yesstevemodel.capability.ProjectileAnimatableCapabilityProvider;
import com.elfmcys.yesstevemodel.capability.VehicleAnimatableCapabilityProvider;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.capability.YsmMaidCapabilityProvider;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.TlmClientCompat;
import com.elfmcys.yesstevemodel.client.entity.CustomEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.IAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.DebugInfo;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Objects;

public class DebugAnimationScreen {
    private static final DebugInfo DEBUG_INFO = new DebugInfo();
    private static final ReferenceArrayList<String> DEBUG_CONTROLLERS = new ReferenceArrayList<>();
    private static WeakReference<CustomEntity<?>> TARGET = null;

    public static IGuiOverlay getGuiOverlay() {
        return (ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) -> {
            renderCustom(gui, graphics, screenWidth, screenHeight);
        };
    }

    public static DebugInfo getDebugInfo() {
        return DEBUG_INFO;
    }

    public static boolean isEnabled() {
        return getTarget() != null;
    }

    public static boolean enable() {
        if (Minecraft.getInstance().hitResult instanceof EntityHitResult result) {
            return enable(result.getEntity());
        }
        return enableForLocalPlayer();
    }

    public static boolean enableForLocalPlayer() {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.getCapability(PlayerAnimatableCapabilityProvider.CAP).ifPresent(DebugAnimationScreen::enable);
            return true;
        }
        disable();
        return false;
    }

    public static boolean enable(Entity entity) {
        LazyOptional<? extends CustomEntity<?>> opt;
        if (entity instanceof Player) {
            opt = entity.getCapability(PlayerAnimatableCapabilityProvider.CAP);
        } else if (TlmClientCompat.isMaid(entity)) {
            opt = entity.getCapability(YsmMaidCapabilityProvider.CAP);
        } else if (entity instanceof Projectile) {
            opt = entity.getCapability(ProjectileAnimatableCapabilityProvider.CAP);
        } else {
            opt = entity.getCapability(VehicleAnimatableCapabilityProvider.CAP);
        }
        return opt.map(cap -> {
            DebugAnimationScreen.enable(cap);
            return true;
        }).orElseGet(() -> {
            disable();
            return false;
        });
    }

    public static void enable(CustomEntity<?> customEntity) {
        disable();
        TARGET = new WeakReference<>(customEntity);
        customEntity.setDebugInfo(DEBUG_INFO);
        var entity = customEntity.getEntity();
        var localPlayer = Minecraft.getInstance().player;
        if (localPlayer != null) {
            localPlayer.sendSystemMessage(Component.translatable("message.yes_steve_model.model.debug_animation.true")
                    .append(" -> ")
                    .append(Objects.requireNonNullElseGet(entity.getCustomName(), entity::getDisplayName)));
        }
    }

    public static void disable() {
        if (TARGET != null) {
            var target = TARGET.get();
            if (target != null) {
                target.setDebugInfo(null);
            }
            TARGET = null;
            var localPlayer = Minecraft.getInstance().player;
            if (localPlayer != null) {
                localPlayer.sendSystemMessage(Component.translatable("message.yes_steve_model.model.debug_animation.false"));
            }
        }
    }

    public static void addDebugController(String controllerName) {
        DEBUG_CONTROLLERS.add(0, controllerName);
    }

    public static void clearDebugController() {
        DEBUG_CONTROLLERS.clear();
    }

    @Nullable
    public static CustomEntity<?> getTarget() {
        if (TARGET != null){
            var entity = TARGET.get();
            if (entity != null && entity.isActive()) {
                return entity;
            }
            disable();
        }
        return null;
    }

    @SuppressWarnings("all")
    private static void renderCustom(ForgeGui gui, GuiGraphics graphics, int screenWidth, int screenHeight) {
        CustomEntity<?> target = getTarget();
        if (target == null) {
            return;
        }

        int[] y = {5};

        DebugInfo debugInfo = DEBUG_INFO;
        debugInfo.enumerate((name, result) -> {
            renderCustomText(gui, graphics, y, name, result, screenWidth, screenHeight);
        });

        // 渲染状态机信息
        DEBUG_CONTROLLERS.forEach(name -> {
            IAnimationController controller = target.getAnimationData().getAnimationController(name);
            renderCustomText(gui, graphics, y, name, controller != null ? controller.getState() : "(N/A)", screenWidth, screenHeight);
        });
    }

    private static void renderCustomText(ForgeGui gui, GuiGraphics graphics, int[] y, String name, String result, int screenWidth, int screenHeight) {
        Font font = gui.getFont();
        if ((y[0] - 5) % 20 == 0) {
            graphics.fill(2, y[0] - 1, screenWidth, y[0] + 9, 0xc0505050);
        } else {
            graphics.fill(2, y[0] - 1, screenWidth, y[0] + 9, 0xc0506050);
        }
        graphics.drawString(font, name, 5, y[0], 0xffffff);
        graphics.drawString(font, result, screenWidth / 2, y[0], 0xffffff);
        y[0] = y[0] + 10;
    }
}
