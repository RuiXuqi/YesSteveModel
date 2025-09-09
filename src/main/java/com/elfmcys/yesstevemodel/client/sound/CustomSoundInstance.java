package com.elfmcys.yesstevemodel.client.sound;

import com.mojang.blaze3d.audio.SoundBuffer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public class CustomSoundInstance extends MinecraftSoundInstance {
    private final SoundData soundData;

    public CustomSoundInstance(SoundEvent soundEvent, SoundData soundData, Entity entity) {
        super(soundEvent, entity);
        this.soundData = soundData;
    }

    @Nullable
    public SoundBuffer getSoundBuffer() {
        return new SoundBuffer(soundData.byteBuffer(), soundData.audioFormat());
    }
}
