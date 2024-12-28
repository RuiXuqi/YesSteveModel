package com.elfmcys.yesstevemodel.client.sound;

import com.elfmcys.yesstevemodel.capability.PlayerGeoCapabilityProvider;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.mojang.blaze3d.audio.SoundBuffer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class CustomSoundInstance extends AbstractTickableSoundInstance {
    private final String soundPath;
    private final Entity entity;

    public CustomSoundInstance(SoundEvent soundEvent, String soundPath, Entity entity) {
        super(soundEvent, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.soundPath = soundPath;
        this.entity = entity;
    }

    @Override
    public void tick() {
        if (this.entity.isRemoved()) {
            this.stop();
        } else {
            this.x = this.entity.getX();
            this.y = this.entity.getY();
            this.z = this.entity.getZ();
        }
    }

    public void setStopped() {
        this.stop();
    }

    @Nullable
    public SoundBuffer getSoundBuffer() {
        return entity.getCapability(PlayerGeoCapabilityProvider.CAP)
                .map(cap -> ClientModelManager.getModel(cap.getModelId())
                        .map(model -> model.sounds().get(soundPath)))
                .orElse(Optional.empty())
                .map(data -> new SoundBuffer(data.byteBuffer(), data.audioFormat()))
                .orElse(null);
    }
}
