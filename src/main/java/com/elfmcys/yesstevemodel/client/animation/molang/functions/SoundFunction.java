package com.elfmcys.yesstevemodel.client.animation.molang.functions;

import com.elfmcys.yesstevemodel.client.sound.CustomSoundInstance;
import com.elfmcys.yesstevemodel.client.sound.MinecraftSoundInstance;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.function.entity.EntityFunction;
import com.elfmcys.yesstevemodel.init.ModSounds;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import com.elfmcys.yesstevemodel.molang.runtime.Function;
import com.google.common.collect.Maps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.apache.commons.lang3.StringUtils;

import java.lang.ref.WeakReference;
import java.util.Map;

public class SoundFunction {
    private static final Map<String, WeakReference<MinecraftSoundInstance>> CACHE = Maps.newConcurrentMap();

    public static class Stop extends EntityFunction {
        @Override
        protected Object eval(ExecutionContext<IContext<Entity>> context, Function.ArgumentCollection arguments) {
            if (!context.entity().allowEmitting()) {
                return false;
            }
            String id = arguments.getAsString(context, 0);
            if (StringUtils.isEmpty(id)) {
                return false;
            }
            var ref = CACHE.get(id);
            if (ref != null) {
                var instance = ref.get();
                if (instance != null) {
                    instance.setStopped();
                }
                CACHE.remove(id);
            }
            return true;
        }

        @Override
        public boolean validateArgumentSize(int size) {
            return size >= 1;
        }
    }

    public static class StopAll extends EntityFunction {
        @Override
        protected Object eval(ExecutionContext<IContext<Entity>> context, Function.ArgumentCollection arguments) {
            if (!context.entity().allowEmitting()) {
                return false;
            }
            for (var ref : CACHE.values()) {
                var instance = ref.get();
                if (instance != null) {
                    instance.setStopped();
                }
            }
            CACHE.clear();
            return true;
        }

        @Override
        public boolean validateArgumentSize(int size) {
            return size == 0;
        }
    }

    public static class Play extends EntityFunction {
        @Override
        protected Object eval(ExecutionContext<IContext<Entity>> context, Function.ArgumentCollection arguments) {
            if (!context.entity().allowEmitting()) {
                return false;
            }
            String id = arguments.getAsString(context, 0);
            if (StringUtils.isEmpty(id)) {
                return false;
            }
            String soundName = arguments.getAsString(context, 1);
            if (StringUtils.isBlank(soundName)) {
                return false;
            }
            Entity targetEntity = context.entity().entity();
            if (targetEntity == null) {
                return false;
            }
            boolean force = false;
            if (arguments.size() >= 3) {
                force = arguments.getAsBoolean(context, 2);
            }
            SoundManager soundManager = Minecraft.getInstance().getSoundManager();

            // 先检查缓存
            var ref = CACHE.get(id);
            if (ref != null) {
                MinecraftSoundInstance instance = ref.get();
                // 如果不是强制播放，并且当前已经有同 ID 的音效在播放，那么就不再播放
                if (instance != null && soundManager.isActive(instance)) {
                    if (force) {
                        instance.setStopped();
                    } else {
                        return false;
                    }
                } else {
                    CACHE.remove(id);
                }
            }

            MinecraftSoundInstance sound;
            if (soundName.contains(":")) {
                // 如果声音名带冒号，那么大概率就是调用原版音频，因为 Windows 中冒号不是合法的文件名
                ResourceLocation soundId = new ResourceLocation(soundName);
                SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(soundId);
                sound = new MinecraftSoundInstance(soundEvent, targetEntity);
            } else {
                // 否则认为是自定义的音频文件
                var animatable = context.entity().animatableEntity();
                var soundData = animatable.getSoundData(soundName);
                if (soundData == null) {
                    var debugSource = animatable.getDebugSource();
                    if (debugSource != null) {
                        debugSource.print("Sound not found: " + soundName);
                    }
                    return false;
                }
                sound = new CustomSoundInstance(ModSounds.CUSTOM, soundData, targetEntity);
            }

            // 设置音量和音调
            if (arguments.size() >= 4) {
                float volume = arguments.getAsFloat(context, 3);
                sound.setConfiguredVolume(Mth.clamp(volume, 0.001f, 1000f));
            }
            if (arguments.size() >= 5) {
                float pitch = arguments.getAsFloat(context, 4);
                sound.setPitch(Mth.clamp(pitch, 0.001f, 1000f));
            }

            // 如果是 GUI 内，设置为 UI 音效
            if (context.entity().animatableEntity().isFakePlayer()) {
                sound.setAsUI();
            }

            CACHE.put(id, new WeakReference<>(sound));
            Minecraft.getInstance().execute(() -> soundManager.play(sound));
            return true;
        }

        @Override
        public boolean validateArgumentSize(int size) {
            return size >= 2;
        }
    }
}