package com.elfmcys.yesstevemodel.client.sound;

import com.elfmcys.yesstevemodel.capability.PlayerGeoCapabilityProvider;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.TlmCompat;
import com.mojang.blaze3d.audio.SoundBuffer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class CustomSoundInstance extends MinecraftSoundInstance {
    private final String soundPath;

    public CustomSoundInstance(SoundEvent soundEvent, String soundPath, Entity entity) {
        super(soundEvent, entity);
        this.soundPath = soundPath;
    }

    @Nullable
    public SoundBuffer getSoundBuffer() {
        if (TlmCompat.isMaid(entity)) {
            return TlmCompat.getSoundBuffer(entity, soundPath);
        }
        return entity.getCapability(PlayerGeoCapabilityProvider.CAP)
                .map(cap -> ClientModelManager.getModel(cap.getModelId())
                        .map(model -> model.sounds().get(soundPath)))
                .orElse(Optional.empty())
                .map(data -> new SoundBuffer(data.byteBuffer(), data.audioFormat()))
                .orElse(null);
    }
}
