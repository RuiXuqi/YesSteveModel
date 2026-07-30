package com.elfmcys.ysm.format.container;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;

final class ByteArraySeekableChannel implements SeekableByteChannel {
    private final byte[] data;
    private int position;
    private boolean open = true;

    ByteArraySeekableChannel(byte[] data) {
        this.data = data;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        checkOpen();
        if (position == data.length) {
            return -1;
        }
        var length = Math.min(dst.remaining(), data.length - position);
        dst.put(data, position, length);
        position += length;
        return length;
    }

    @Override
    public int write(ByteBuffer src) {
        throw new UnsupportedOperationException("read only");
    }

    @Override
    public long position() throws IOException {
        checkOpen();
        return position;
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        checkOpen();
        if (newPosition < 0 || newPosition > data.length) {
            throw new IllegalArgumentException("Invalid position: " + newPosition);
        }
        position = Math.toIntExact(newPosition);
        return this;
    }

    @Override
    public long size() throws IOException {
        checkOpen();
        return data.length;
    }

    @Override
    public SeekableByteChannel truncate(long size) {
        throw new UnsupportedOperationException("read only");
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public void close() {
        open = false;
    }

    private void checkOpen() throws ClosedChannelException {
        if (!open) {
            throw new ClosedChannelException();
        }
    }
}
