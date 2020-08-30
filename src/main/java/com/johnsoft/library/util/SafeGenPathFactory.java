package com.johnsoft.library.util;

import java.io.File;
import java.util.Date;

/**
 * 可不断生成路径名的路径生成器, 用于只写情况, 比如{@link DiskBlockSliceSink}. <br>
 * 此类是不可变类, 线程安全.
 * @author John Kenrinus Lee
 * @version 2016-02-18
 */
public class SafeGenPathFactory implements FilePathFactory {
    private final String prefix;
    private final String suffix;

    public SafeGenPathFactory(String folderPath, String token, String fileType) {
        if (folderPath == null || folderPath.trim().isEmpty()
                || token == null || token.trim().isEmpty()
                || fileType == null || fileType.trim().isEmpty()) {
            throw new NullPointerException("SafeGenPathFactory: param named folderPath or token or fileType is empty.");
        }
        File folder = new File(folderPath);
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                throw new IllegalArgumentException("SafeGenPathFactory: make directory failed with param folderPath.");
            }
        } else if (!folder.isDirectory() || !folder.canWrite()) {
            throw new IllegalArgumentException("SafeGenPathFactory: folderPath exists, "
                    + "but isn't a directory or can't write.");
        }
        folderPath = folderPath.endsWith("/") ? folderPath : (folderPath + "/");
        prefix = folderPath + token;
        suffix = fileType.startsWith(".") ? fileType : ("." + fileType);
    }

    @Override
    public String getNextFilePath() {
        return prefix + ConcurrentDateFormat.getUnsigned().format(new Date()) + suffix;
    }
}