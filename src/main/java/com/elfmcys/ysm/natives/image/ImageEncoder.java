package com.elfmcys.ysm.natives.image;

import com.elfmcys.ysm.buffer.NativeBuffer;
import com.elfmcys.ysm.mixin.client.NativeImageAccessor;
import com.mojang.blaze3d.platform.NativeImage;

import java.io.IOException;

public class ImageEncoder {
    public static Image encodeLossy(NativeBuffer pixels, int width, int height, int maxWidth, int maxHeight) throws IOException {
        if (pixels.size() < width * height * 4) {
            throw new IllegalArgumentException("Illegal pixels buffer size");
        }
        return encode(pixels.ptr(), width, height, false, maxWidth, maxHeight);
    }

    public static Image encodeLossless(NativeBuffer pixels, int width, int height) throws IOException {
        if (pixels.size() < width * height * 4) {
            throw new IllegalArgumentException("Illegal pixels buffer size");
        }
        return encode(pixels.ptr(), width, height, true, 0, 0);
    }

    public static Image encodeLossy(NativeImage image, int maxWidth, int maxHeight) throws IOException {
        if (image.format() != NativeImage.Format.RGBA) {
            throw new UnsupportedOperationException("Image format not supported");
        }
        return encode(((NativeImageAccessor) (Object) image).ysm$pixels(), image.getWidth(), image.getHeight(), false, maxWidth, maxHeight);
    }

    public static Image encodeLossless(NativeImage image) throws IOException {
        if (image.format() != NativeImage.Format.RGBA) {
            throw new UnsupportedOperationException("Image format not supported");
        }
        return encode(((NativeImageAccessor) (Object) image).ysm$pixels(), image.getWidth(), image.getHeight(), true, 0, 0);
    }

    private static Image encode(long pixels, int width, int height, boolean lossless, int maxWidth, int maxHeight) throws IOException {
        try (var dstScope = NativeBuffer.allocateWithScope(width * height * 4)) {
            var result = nEncode(pixels, width, height,
                    dstScope.get().ptr(), dstScope.get().size(),
                    lossless, maxWidth, maxHeight);
            if (result == 0) {
                throw new IOException("Failed to encode lossy image");
            }
            width  = (int) ((result >>> 48) & 0xFFFFL);
            height = (int) ((result >>> 32) & 0xFFFFL);
            var format = (int) ((result >>> 28) & 0xFL);
            var size   = (int) (result & 0x0FFFFFFFL);
            return new Image(Image.Format.VALUES.get(format), width, height, dstScope.release().slice(0, size));
        }
    }

    private static native long nEncode(long pixels, int width, int height,
                                             long dst, long dst_size,
                                             boolean lossless, int maxWidth, int maxHeight);
}
