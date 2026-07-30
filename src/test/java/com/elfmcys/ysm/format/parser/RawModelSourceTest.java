package com.elfmcys.ysm.format.parser;

import com.elfmcys.ysm.buffer.NativeBuffer;
import com.elfmcys.ysm.format.vfs.VirtualFileSystem;
import com.elfmcys.ysm.natives.Blake3;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class RawModelSourceTest {
    @Test
    void hashesOriginalImageBytesBeforeDownstreamProcessing() throws IOException {
        try (var file = NativeBuffer.allocate(4)) {
            file.nio().put(new byte[]{1, 2, 3, 4});
            var expected = new ModelHashCanonicalizer();
            expected.add("texture", "textures/main.png", Blake3.computeHash(file));

            var source = new RawModelSource(new SingleFileVfs("textures/main.png", file));
            var borrowed = source.readFile("textures/main.png", "texture", true).orElseThrow();
            borrowed.nio().put(0, (byte) 99);

            assertArrayEquals(expected.aggregate(), source.aggregateHash());
        }
    }

    private record SingleFileVfs(String path, NativeBuffer file) implements VirtualFileSystem {
        @Override
        public String[] listFiles(String path) {
            return new String[0];
        }

        @Override
        public String[] listDirectories(String path) {
            return new String[0];
        }

        @Override
        public boolean hasFile(String fileName) {
            return path.equals(fileName);
        }

        @Override
        public NativeBuffer getFile(String fileName) {
            return hasFile(fileName) ? file : null;
        }
    }
}
