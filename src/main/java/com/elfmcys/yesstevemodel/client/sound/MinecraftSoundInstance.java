package com.elfmcys.yesstevemodel.client.sound;

import com.elfmcys.yesstevemodel.config.ClientConfig;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

public class MinecraftSoundInstance extends AbstractTickableSoundInstance implements ICanStopSound {
    protected final Entity entity;
    protected float configuredVolume = 1.0f;

    public MinecraftSoundInstance(SoundEvent soundEvent, Entity entity) {
        super(soundEvent, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.entity = entity;
        this.x = this.entity.getX();
        this.y = this.entity.getY();
        this.z = this.entity.getZ();
    }

    @Override
    public void tick() {
        this.volume = this.configuredVolume * ClientConfig.SOUND_VOLUME.get().floatValue() / 100.0f;
        if (this.entity.isRemoved()) {
            this.stop();
        } else {
            this.x = this.entity.getX();
            this.y = this.entity.getY();
            this.z = this.entity.getZ();
        }
    }

    public void setConfiguredVolume(float volume) {
        this.configuredVolume = volume;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public void setAsUI() {
        this.attenuation = Attenuation.NONE;
        this.relative = true;
    }

    @Override
    public void setStopped() {
        this.stop();
    }
}
