package com.elfmcys.ysm.model.storage;

import com.elfmcys.ysm.buffer.ArrayBuffer;
import com.elfmcys.ysm.buffer.NativeBuffer;
import com.elfmcys.ysm.buffer.UniBuffer;
import com.elfmcys.ysm.model.domain.ModelHash;
import com.elfmcys.ysm.natives.Blake3;
import us.hebi.quickbuf.RepeatedByte;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class ModelHashing {
    private ModelHashing() {
    }

    public static ModelHash blake3(byte[] data) {
        return new ModelHash(Blake3.computeHash(ArrayBuffer.borrow(data)));
    }

    public static ModelHash blake3(RepeatedByte data) {
        return new ModelHash(Blake3.computeHash(ArrayBuffer.borrow(data)));
    }

    public static ModelHash blake3(UniBuffer data) {
        return new ModelHash(Blake3.computeHash(data));
    }

    public static ModelHash blake3(Path file) throws IOException {
        var size = Files.size(file);
        if (size < 0 || size > UniBuffer.MAX_SIZE) {
            throw new IOException("File is too large to hash: " + file);
        }
        try (var source = NativeBuffer.allocate(Math.toIntExact(size));
             var channel = Files.newByteChannel(file, StandardOpenOption.READ)) {
            var target = source.nio();
            while (target.hasRemaining()) {
                if (channel.read(target) < 0) {
                    throw new IOException("File changed while hashing: " + file);
                }
            }
            return new ModelHash(Blake3.computeHash(source));
        }
    }

    public static ModelHash descriptorHash(byte[] containerPreamble, byte[] manifest) {
        try (var input = ArrayBuffer.allocate(containerPreamble.length + manifest.length)) {
            System.arraycopy(containerPreamble, 0, input.array(), input.arrayOffset(), containerPreamble.length);
            System.arraycopy(manifest, 0, input.array(), input.arrayOffset() + containerPreamble.length,
                    manifest.length);
            return blake3(input);
        }
    }
}
