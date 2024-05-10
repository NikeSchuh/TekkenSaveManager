package com.nikeschuh.tekkensavemanager.utils;

import java.nio.ByteBuffer;

public class ByteUtils {

    public static long readLong(byte[] array, int offset) {
        ByteBuffer buffer = ByteBuffer.wrap(array, offset, Long.BYTES);
        return buffer.getLong();
    }
}
