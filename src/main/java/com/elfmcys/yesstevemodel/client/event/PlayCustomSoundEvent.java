package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.client.sound.CustomSoundInstance;
import com.mojang.blaze3d.audio.SoundBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundSourceEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class PlayCustomSoundEvent {
    @SubscribeEvent
    public static void onPlaySoundSource(PlaySoundSourceEvent event) {
        if (event.getSound() instanceof CustomSoundInstance instance) {
            SoundBuffer soundBuffer = instance.getSoundBuffer();
            if (soundBuffer != null) {
                event.getChannel().attachStaticBuffer(soundBuffer);
                event.getChannel().play();
            }
        }
    }
}
