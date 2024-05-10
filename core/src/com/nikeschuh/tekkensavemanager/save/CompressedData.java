package com.nikeschuh.tekkensavemanager.save;

public class CompressedData {

    private final String originalName;
    private final byte[] compressedData;

    public CompressedData(String originalName, byte[] compressedData) {
        this.originalName = originalName;
        this.compressedData = compressedData;
    }

    public byte[] getCompressedData() {
        return compressedData;
    }

    public String getOriginalName() {
        return originalName;
    }
}
