/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package com.elfmcys.yesstevemodel.geckolib3.core.event;

import com.elfmcys.yesstevemodel.client.sound.CustomSoundInstance;
import com.elfmcys.yesstevemodel.geckolib3.core.IAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.event.EventKeyFrame;
import com.elfmcys.yesstevemodel.init.ModSounds;
import net.minecraft.client.Minecraft;

import java.util.LinkedList;
import java.util.List;

public class SoundKeyframeEvecutor {
    private final List<EventKeyFrame<String>> list;
    private List<CustomSoundInstance> cachePlaySounds;
    private int nextIndex = 0;

    public SoundKeyframeEvecutor(List<EventKeyFrame<String>> list) {
        this.list = list;
        this.cachePlaySounds = new LinkedList<>();
    }

    public <T extends IAnimatable<?>> void executeTo(T animatable, double currentTick) {
        while (!reachEnd()) {
            EventKeyFrame<String> keyFrame = list.get(nextIndex);
            if (keyFrame.getStartTick() > currentTick) {
                return;
            }
            CustomSoundInstance instance = new CustomSoundInstance(ModSounds.CUSTOM.get(), keyFrame.getEventData(), animatable.getEntity());
            cachePlaySounds.add(instance);
            Minecraft.getInstance().getSoundManager().play(instance);
            nextIndex++;
        }
    }

    public void reset() {
        nextIndex = 0;
        cachePlaySounds.forEach(CustomSoundInstance::setStopped);
        // 重建比删除快？
        cachePlaySounds = new LinkedList<>();
    }

    public boolean reachEnd() {
        return nextIndex >= list.size();
    }
}
