package com.elfmcys.ysm.natives.image;

import java.io.IOException;

/** A repeatable external source of encoded image data. */
@FunctionalInterface
public interface ImageSource {
    Image open() throws IOException;
}
