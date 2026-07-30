package com.elfmcys.ysm.model;

import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

// Native Access
public class ExportModelResult {
    private final boolean success;
    @Nullable
    private final Component message;
    // 相对路径
    private final String filePath;
    private final String fileName;
    private final int fileSize;

    // Native Access
    public ExportModelResult(boolean success, @Nullable Object message, String filePath, String fileName, int fileSize) {
        this.success = success;
        this.message = (Component)message;
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileSize = fileSize;
    }

    public boolean success() {
        return success;
    }

    @Nullable
    public Component message() {
        return message;
    }

    public String filePath() {
        return filePath;
    }

    public String fileName() {
        return fileName;
    }

    public int fileSize() {
        return fileSize;
    }
}
