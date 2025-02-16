/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package com.elfmcys.yesstevemodel.geckolib3.core.event;

import com.elfmcys.yesstevemodel.client.sound.CustomSoundInstance;
import com.elfmcys.yesstevemodel.client.sound.ICanStopSound;
import com.elfmcys.yesstevemodel.client.sound.MinecraftSoundInstance;
import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.event.EventKeyFrame;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.init.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.LinkedList;
import java.util.List;

public class SoundKeyframeExecutor {
    private final List<EventKeyFrame<String>> list;
    private List<ICanStopSound> cachePlaySounds;
    private int nextIndex = 0;

    public SoundKeyframeExecutor(List<EventKeyFrame<String>> list) {
        this.list = list;
        this.cachePlaySounds = new LinkedList<>();
    }

    public void executeTo(AnimatableEntity<?> animatable, double currentTick, boolean dryRun) {
        while (!reachEnd()) {
            EventKeyFrame<String> keyFrame = list.get(nextIndex);
            if (keyFrame.getStartTick() > currentTick) {
                return;
            }
            nextIndex++;

            if (dryRun) {
                continue;
            }

            String soundName = keyFrame.getEventData();
            if (soundName.contains(":")) {
                // 如果声音名带冒号，那么大概率就是调用原版音频，因为 Windows 中冒号不是合法的文件名
                ResourceLocation soundId = new ResourceLocation(soundName);
                SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(soundId);
                MinecraftSoundInstance instance = new MinecraftSoundInstance(soundEvent, animatable.getEntity());
                cachePlaySounds.add(instance);
                Minecraft.getInstance().getSoundManager().play(instance);
            } else {
                // 否则认为是自定义的音频文件
                CustomSoundInstance instance = new CustomSoundInstance(ModSounds.CUSTOM, soundName, animatable.getEntity());
                cachePlaySounds.add(instance);
                Minecraft.getInstance().getSoundManager().play(instance);
            }
            nextIndex++;
        }
    }

    public void reset() {
        nextIndex = 0;
        if (!cachePlaySounds.isEmpty()) {
            cachePlaySounds.forEach(ICanStopSound::setStopped);
            // 重建比删除快？
            cachePlaySounds = new LinkedList<>();
        }
    }

    public boolean reachEnd() {
        return nextIndex >= list.size();
    }
}
