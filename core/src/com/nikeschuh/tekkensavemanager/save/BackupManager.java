package com.nikeschuh.tekkensavemanager.save;

import com.badlogic.gdx.files.FileHandle;
import com.nikeschuh.tekkensavemanager.SaveManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class BackupManager {

    public static final long FORMAT_SIGNATURE = 5129850125256421236L;

    public static void createBackup(FileHandle saveDirectory, FileHandle target, Consumer<FileHandle> consumer, boolean backupReplays, boolean backupGhosts) {
        DataOutputStream outputStream = new DataOutputStream(target.write(false));
        SaveManager.progressBar.setVisible(true);
        FileHandle[] saveFiles = Arrays.stream(saveDirectory.list("sav"))
                .filter(fileHandle -> {
                    if (!backupGhosts && fileHandle.name().startsWith("ghost")) return false;
                    if (!backupReplays && fileHandle.name().startsWith("replay_game")) return false;
                    return true;
                })
                .toArray(FileHandle[]::new);
        List<CompressedData> compressedData = new ArrayList<>();

        try {
            for (int i = 0; i < saveFiles.length; i++) {
                FileHandle saveFile = saveFiles[i];
                byte[] rawBytes = saveFile.readBytes();
               // byte[] compressedBytes = compressBytes(rawBytes);
               // System.out.println(rawBytes.length + "->" + compressedBytes.length);
                compressedData.add(new CompressedData(saveFile.name(), rawBytes));
            }

            outputStream.writeLong(FORMAT_SIGNATURE);
            outputStream.writeLong(System.currentTimeMillis());
            outputStream.writeInt(saveFiles.length);


            int index = 1;
            for (CompressedData data : compressedData) {
                byte[] nameData = data.getOriginalName().getBytes(StandardCharsets.UTF_8);
                outputStream.writeInt(nameData.length);
                outputStream.write(nameData);
                outputStream.writeInt(data.getCompressedData().length);
                outputStream.write(data.getCompressedData());
                SaveManager.progressBar.setValue((float) index / saveFiles.length);
                index++;
            }

            outputStream.flush();
            outputStream.close();

            SaveManager.progressBar.setVisible(false);

            consumer.accept(target);
        }catch (IOException e) {
            System.err.println("Error creating backup.");
            e.printStackTrace();
        }
    }

    public static void readBackup(FileHandle backupFile, FileHandle targetDirectory) {
        if(!targetDirectory.exists()) return;
        if(!targetDirectory.isDirectory()) return;

        DataInputStream inputStream = new DataInputStream(backupFile.read());

        try {
            long signature = inputStream.readLong();
            if(signature != FORMAT_SIGNATURE) {
                System.err.println(backupFile + " is not in the correct format. Signature bytes wrong.");
                return;
            }

            long timeStamp = inputStream.readLong();

            int numberFiles = inputStream.readInt();
            if(numberFiles <= 0) {
                System.err.println("Empty backup! Or corrupt :/");
                return;
            }

            SaveManager.progressBar.setVisible(true);


            for(int i =0; i < numberFiles; i++) {
                int nameDataLength = inputStream.readInt();
                byte[] nameData = new byte[nameDataLength];
                int nameResult = inputStream.read(nameData);

                if (nameResult == -1) {
                    System.err.println("Error stream ended abruptly. CORRUPTT");
                    return;
                }

                String originalFileName = new String(nameData, StandardCharsets.UTF_8);
                int compressedDataLength = inputStream.readInt();
                byte[] compressedData = new byte[compressedDataLength];
                inputStream.readFully(compressedData);
               // byte[] uncompressedData = decompressBytes(compressedData);

                FileHandle destinationFile = targetDirectory.child(originalFileName);
                destinationFile.writeBytes(compressedData, false);
                SaveManager.progressBar.setValue(((float)(i+ 1) / numberFiles));
            }
        }catch (IOException e) {
            System.err.println("Error reading backup file " + backupFile);
            e.printStackTrace();
        }

        try {
            inputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        SaveManager.progressBar.setVisible(false);

    }


    public static byte[] compressBytes(byte[] in) {
        Deflater deflater = new Deflater();
        deflater.setInput(in);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(in.length);
        deflater.finish();
        byte[] buffer = new byte[1024]; // Buffer to hold compressed data
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer); // Compress data into buffer
            outputStream.write(buffer, 0, count); // Write compressed data to output stream
        }


        deflater.end();
        return outputStream.toByteArray();
    }

    public static byte[] decompressBytes(byte[] in) {
        Inflater inflater = new Inflater();
        inflater.setInput(in);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(in.length);

        byte[] buffer = new byte[1024]; // Buffer to hold decompressed data
        try {
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer); // Decompress data into buffer
                outputStream.write(buffer, 0, count); // Write decompressed data to output stream
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                outputStream.close();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
            inflater.end();
        }

        return outputStream.toByteArray();
    }

}
