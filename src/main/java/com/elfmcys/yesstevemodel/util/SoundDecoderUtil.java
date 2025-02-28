package com.elfmcys.yesstevemodel.util;

import com.elfmcys.yesstevemodel.client.sound.SoundData;
import com.elfmcys.yesstevemodel.lib.concentus.OpusDecoder;
import com.elfmcys.yesstevemodel.lib.concentus.OpusException;
import com.elfmcys.yesstevemodel.lib.gagravarr.ogg.OggFile;
import com.elfmcys.yesstevemodel.lib.gagravarr.ogg.OggStreamIdentifier;
import com.elfmcys.yesstevemodel.lib.gagravarr.opus.OpusAudioData;
import com.elfmcys.yesstevemodel.lib.gagravarr.opus.OpusFile;
import com.mojang.blaze3d.audio.OggAudioStream;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;

import javax.sound.sampled.AudioFormat;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

@OnlyIn(Dist.CLIENT)
public final class SoundDecoderUtil {
    /**
     * Opus 支持的帧长有：2.5ms、5ms、10ms、20ms、40ms、60ms
     * <p>
     * 因为我们对延迟不是很敏感，所以选 60ms
     */
    private static final int MAX_FRAME_SIZE = 60;

    @Nullable
    public static SoundData bufferToSoundData(byte[] data) throws IOException, OpusException {
        ByteBuffer bytebuffer = null;
        AudioFormat format = null;

        // 依据 ogg 类型，分别读取 Vorbis 和 Opus 音频
        Type type = getOggType(data);
        if (type == Type.VORBIS) {
            try (InputStream stream = new ByteArrayInputStream(data); OggAudioStream audioStream = new OggAudioStream(stream)) {
                bytebuffer = audioStream.readAll();
                format = audioStream.getFormat();
            }
        } else if (type == Type.OPUS) {
            Pair<AudioFormat, byte[]> output = decodeOpus(data);
            byte[] pcm = output.getRight();
            bytebuffer = BufferUtils.createByteBuffer(pcm.length);
            bytebuffer.put(pcm);
            bytebuffer.flip();
            format = output.getLeft();
        }

        if (bytebuffer != null && format != null) {
            // 双声道转单声道
            ByteBuffer monoData = mergeStereoToMono(bytebuffer, format);
            AudioFormat monoFormat = new AudioFormat(format.getEncoding(), format.getSampleRate(),
                    format.getSampleSizeInBits(), 1, 2,
                    format.getFrameRate(), format.isBigEndian(), format.properties());
            return new SoundData(monoData, monoFormat);
        }
        return null;
    }

    private static Type getOggType(byte[] data) throws IOException {
        try (OggFile oggFile = new OggFile(new ByteArrayInputStream(data))) {
            OggStreamIdentifier.OggStreamType streamType = OggStreamIdentifier.identifyType(oggFile.getPacketReader().getNextPacket());
            if (streamType == OggStreamIdentifier.OGG_VORBIS) {
                return Type.VORBIS;
            } else if (streamType == OggStreamIdentifier.OPUS_AUDIO || streamType == OggStreamIdentifier.OPUS_AUDIO_ALT) {
                return Type.OPUS;
            } else {
                return Type.UNKNOWN;
            }
        }
    }

    /**
     * 为了方便，我们直接一口气把它全部解码了
     */
    private static Pair<AudioFormat, byte[]> decodeOpus(byte[] data) throws IOException, OpusException {
        try (OpusFile opusFile = new OpusFile(new OggFile(new ByteArrayInputStream(data)));
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            int sampleRate = opusFile.getInfo().getSampleRate();
            int channels = opusFile.getInfo().getNumChannels();
            int frameSize = sampleRate * MAX_FRAME_SIZE / 1000;

            OpusDecoder decoder = new OpusDecoder(sampleRate, channels);
            byte[] pcmBytes = new byte[sampleRate * channels * 2];
            OpusAudioData packet;

            while ((packet = opusFile.getNextAudioPacket()) != null) {
                byte[] packetBytes = packet.getData();
                int packetLength = packetBytes.length;
                int samplesDecoded = decoder.decode(packetBytes, 0, packetLength, pcmBytes, 0, frameSize, false);
                outputStream.write(pcmBytes, 0, samplesDecoded * channels * 2);
            }
            byte[] outputData = outputStream.toByteArray();
            AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, 16,
                    channels, channels * 2, sampleRate, false);
            return Pair.of(audioFormat, outputData);
        }
    }

    private static ByteBuffer mergeStereoToMono(ByteBuffer stereoData, AudioFormat audioFormat) {
        // 如果是单声道，那么原样返回即可
        if (audioFormat.getChannels() == 1) {
            return stereoData;
        }

        int frameSize = audioFormat.getFrameSize();
        int frames = stereoData.remaining() / frameSize;
        int monoFrameSize = audioFormat.getSampleSizeInBits() / 8;
        ByteBuffer monoData = BufferUtils.createByteBuffer(frames * monoFrameSize);

        // 重置游标，以防万一
        stereoData.rewind();
        for (int i = 0; i < frames; i++) {
            short leftSample = stereoData.getShort();
            short rightSample = stereoData.getShort();
            // 平均左、右声道的值
            short monoSample = (short) ((leftSample + rightSample) / 2);
            monoData.putShort(monoSample);
        }
        monoData.flip();
        return monoData;
    }

    private enum Type {
        VORBIS,
        OPUS,
        UNKNOWN
    }
}

