package com.elfmcys.yesstevemodel.client.sound;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;

/**
 * 不能直接写进 SoundBuffer，需要一个中间类缓存一下，避免被系统回收，导致音频出错
 */
public record SoundData(ByteBuffer byteBuffer, AudioFormat audioFormat) {
}