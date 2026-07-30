package com.elfmcys.ysm.client.sound.data;

import com.elfmcys.ysm.client.sound.stream.CustomAudioStream;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;

public interface ModelSoundHolder {
    CustomAudioStream openStream(SoundData soundData) throws IOException, UnsupportedAudioFileException;
}
