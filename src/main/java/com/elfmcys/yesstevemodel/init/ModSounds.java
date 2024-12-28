package com.elfmcys.yesstevemodel.init;

import com.elfmcys.yesstevemodel.YesSteveModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, YesSteveModel.MOD_ID);

    public static final RegistryObject<SoundEvent> CUSTOM = registerSound("custom");

    private static RegistryObject<SoundEvent> registerSound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createFixedRangeEvent(new ResourceLocation(YesSteveModel.MOD_ID, name), 16.0F));
    }
}
