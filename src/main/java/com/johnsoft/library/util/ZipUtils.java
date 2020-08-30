package com.johnsoft.library.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 一个辅助工具类, 用于ZIP压缩/ZIP解压缩/ZIP条目查看.
 * 注: 此类所有方法并未具备甄别是否为ZIP压缩包的能力, 如果非ZIP压缩包, 可能会抛出异常
 * @author John Kenrinus Lee
 * @version 2016-05-06
 */
public final class ZipUtils {

    private ZipUtils() {}

    private static final int BUFFER_SIZE = 512 * 1024; // 512KB

    /** 抑制异常并压缩文件, 压缩失败将清理可能产生的ZIP压缩包 */
    public static void zipFilesQuietly(File zipFile, File...originFiles) {
        if (zipFile == null || originFiles == null) return;
        try {
            zipFiles(Arrays.asList(originFiles), zipFile, "", Deflater.DEFAULT_COMPRESSION, true, null);
        } catch (IOException e) {
            e.printStackTrace();
            if (zipFile != null && zipFile.exists()) {
                deleteExistsWithNoPermissionCheck(zipFile);
            }
        }
    }

    /**
     * 压缩文件
     * @param originFiles 待压缩的文件集, 可以是目录文件, 如果其中有目录文件, 则自动递归添加目录下子文件
     * @param zipFile ZIP压缩包路径, 不存在则创建
     * @param comment 对ZIP压缩包或其所有条目的批注
     * @param compressLevel ZIP压缩率级别, 默认为-1, 压缩比最高为9, 最低为0(不压缩), 超出范围则忽略
     * @param recover 如果ZIP压缩包在压缩前就已存在, 是否先删除
     * @param filter 什么模式的文件应该添加进ZIP压缩包中, 为null则不过滤
     * @throws IOException 所有的异常会被包装成IOException而抛出
     * @see FilePathSegmentFilter#accept(String)
     */
    public static void zipFiles(Collection<File> originFiles, File zipFile, String comment,
                                int compressLevel, boolean recover, FilePathSegmentFilter filter) throws IOException {
        try {
            if (originFiles == null || originFiles.isEmpty() || zipFile == null) {
                throw new IllegalArgumentException("Zipping files: src or dst is invalid.");
            }
            if (zipFile.exists()) {
                if (zipFile.isDirectory() || recover) {
                    if (!deleteExistsWithNoPermissionCheck(zipFile)) {
                        throw new IllegalStateException("Zipping files: "
                                + "the method deleteExistsWithNoPermissionCheck("
                                + zipFile.getAbsolutePath() + ") return false.");
                    }
                } else {
                    System.err.println("Zipping files: the target compressed file named ["
                            + zipFile.getAbsolutePath() + "] exist already.");
                    return;
                }
            } else {
                final File parent = zipFile.getParentFile();
                if (parent == null || (!parent.exists() && !parent.mkdirs())) {  // null if security restricted
                    throw new IllegalStateException("Zipping files: "
                            + "the target directory which store compressed file can't create.");
                }
            }
            ZipOutputStream zipOutput = null;
            try {
                zipOutput = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile), BUFFER_SIZE));
                if (comment != null && !comment.trim().isEmpty()) {
                    zipOutput.setComment(comment);
                }
                if (compressLevel > Deflater.DEFAULT_COMPRESSION && compressLevel <= Deflater.BEST_COMPRESSION) {
                    zipOutput.setLevel(compressLevel);
                }
                for (final File originFile : originFiles) {
                    if (originFile == null || !originFile.exists()) {
                        throw new IllegalArgumentException("Zipping files: src or dst is invalid. Origin: ["
                                + (originFile == null ? "null" : originFile.getAbsolutePath()) + "].");
                    }
                    zipFile(originFile, zipOutput, "", filter);
                }
            } finally {
                if (zipOutput != null) {
                    try {
                        zipOutput.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        } catch (Throwable e) {
            if (!(e instanceof IOException)) {
                throw new IOException(e);
            } else {
                throw e;
            }
        }
    }

    private static void zipFile(File originFile, ZipOutputStream zipOutput, String rootPath,
                                FilePathSegmentFilter filter) throws IOException {
        rootPath = rootPath + (rootPath.endsWith(File.separator) ? "" : File.separator) + originFile.getName();
        if (originFile.isDirectory()) {
            final File[] files = originFile.listFiles();
            if (files == null) {  // null if security restricted
                throw new IllegalStateException("Zipping files: failed to list contents of ["
                        + originFile.getAbsolutePath() + "].");
            }
            for (final File file : files) {
                zipFile(file, zipOutput, rootPath, filter);
            }
        } else {
            if (filter != null && !filter.accept(rootPath)) {
                return;
            }
            final byte[] buffer = new byte[BUFFER_SIZE];
            BufferedInputStream input = null;
            try {
                input = new BufferedInputStream(new FileInputStream(originFile), BUFFER_SIZE);
                zipOutput.putNextEntry(new ZipEntry(rootPath));
                int realLength;
                while ((realLength = input.read(buffer)) > 0) {
                    zipOutput.write(buffer, 0, realLength);
                }
                zipOutput.flush();
                zipOutput.closeEntry();
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
    }

    /** 抑制异常并解压文件, 解缩失败将清理可能产生的部分解压成功的文件, 如果最后一个参数为true, 则最后会删掉ZIP压缩包 */
    public static void unzipFilesQuietly(File zipFile, String unzipToDstDirPath, boolean deleteOnUnzipSuccess) {
        try {
            unzipFile(zipFile, unzipToDstDirPath, true, null, null);
            if (deleteOnUnzipSuccess) {
                try {
                    deleteExistsWithNoPermissionCheck(zipFile);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (unzipToDstDirPath != null && !unzipToDstDirPath.trim().isEmpty()) {
                final File targetDir = new File(unzipToDstDirPath);
                if (targetDir.exists()) {
                    deleteExistsWithNoPermissionCheck(targetDir);
                }
            }
        }
    }

    /**
     * 解压文件<br/>
     * 注: ZipFile实现
     * @param zipFile ZIP压缩包路径
     * @param unzipToDstDirPath 解压文件条目到哪个基目录下
     * @param recover 如果待解压的文件在解压前已存在于磁盘, 是否先删除
     * @param filter 什么模式的文件应该添加进ZIP压缩包中, 为null则不过滤
     * @param collect 凡是成功得到解压的文件对象将放入此集合, 以便后续迭代, 可以为null
     * @throws IOException 所有的异常会被包装成IOException而抛出
     * @see FilePathSegmentFilter#accept(String)
     */
    public static void unzipFile(File zipFile, String unzipToDstDirPath, boolean recover,
                                 FilePathSegmentFilter filter, Collection<File> collect) throws IOException {
        try {
            if (zipFile == null || !zipFile.exists() || zipFile.isDirectory()
                    || unzipToDstDirPath == null || unzipToDstDirPath.trim().isEmpty()) {
                throw new IllegalArgumentException("Unzipping: zip file or unzip to target directory path is invalid");
            }
            unzipToDstDirPath = unzipToDstDirPath
                    + (unzipToDstDirPath.endsWith(File.separator) ? "" : File.separator);
            final File targetDir = new File(unzipToDstDirPath);
            if (!targetDir.exists() && !targetDir.mkdirs()) {
                throw new IllegalStateException("Unzipping: "
                        + "the target directory which store uncompressed files can't create.");
            }
            ZipFile zf = null;
            try {
                zf = new ZipFile(zipFile);
                final Enumeration<?> entries = zf.entries();
                if (entries != null) {
                    while (entries.hasMoreElements()) {
                        final ZipEntry entry = (ZipEntry)entries.nextElement();
                        final String entryName = entry.getName();
                        if (filter == null || filter.accept(entryName)) {
                            extractEntry(zf.getInputStream(entry), entryName, targetDir, recover, collect, true);
                        }
                    }
                }
            } finally {
                if (zf != null) {
                    try {
                        zf.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        } catch (Throwable e) {
            if (!(e instanceof IOException)) {
                throw new IOException(e);
            } else {
                throw e;
            }
        }
    }

    private static void extractEntry(InputStream is, String entryName, File targetDir,
                                     boolean recover, Collection<File> collect,
                                     boolean closeInput) throws IOException {
        BufferedInputStream input = null;
        try {
            input = new BufferedInputStream(is, BUFFER_SIZE);
            final File dstFile = new File(targetDir, entryName);
            if (dstFile.exists()) {
                if (dstFile.isDirectory() || recover) {
                    if (!deleteExistsWithNoPermissionCheck(dstFile)) {
                        throw new IllegalStateException("Unzipping: "
                                + "the method deleteExistsWithNoPermissionCheck("
                                + dstFile.getAbsolutePath() + ") return false.");
                    }
                } else {
                    System.err.println("Unzipping: the target uncompressed file named ["
                            + dstFile.getAbsolutePath() + "] exist already.");
                    return;
                }
            } else {
                final File parent = dstFile.getParentFile();
                if (parent == null || (!parent.exists() && !parent.mkdirs())) {  // null if security restricted
                    throw new IllegalStateException("Unzipping: "
                            + "the target directory which store uncompressed files can't create.");
                }
            }
            if (!dstFile.createNewFile()) {
                throw new IllegalStateException("Unzipping: "
                        + "the target uncompressed files can't create.");
            }
            if (collect != null) {
                collect.add(dstFile);
            }
            BufferedOutputStream output = null;
            try {
                output = new BufferedOutputStream(new FileOutputStream(dstFile), BUFFER_SIZE);
                final byte buffer[] = new byte[BUFFER_SIZE];
                int realLength;
                while ((realLength = input.read(buffer)) > 0) {
                    output.write(buffer, 0, realLength);
                }
                output.flush();
            } finally {
                if (output != null) {
                    try {
                        output.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        } finally {
            if (closeInput && input != null) {
                try {
                    input.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /** 列出zip文件中的所有文件条目 */
    public static List<String> getEntriesNames(File zipFile) throws IOException {
        if (zipFile == null || !zipFile.exists() || zipFile.isDirectory()) {
            throw new IOException(new IllegalArgumentException("zipFile is invalid"));
        }
        final ArrayList<String> entriesNames = new ArrayList<>();
        final Enumeration<?> entries = new ZipFile(zipFile).entries();
        if (entries != null) {
            while (entries.hasMoreElements()) {
                final ZipEntry entry = (ZipEntry)entries.nextElement();
                if (entry != null) {
                    entriesNames.add(entry.getName());
                }
            }
        }
        return entriesNames;
    }

    ////////////////////////////////////// Utilities //////////////////////////////////////

    /**
     * Delete any file or any directory which be specified from parameter,
     * but no parameter check, no file exists check, no file read, write, execute permission check.
     */
    private static boolean deleteExistsWithNoPermissionCheck(File target) {
        boolean result = true;
        if (target.isDirectory()) {
            result = deleteDirectoryNoCheck(target);
        }
        if (result) {
            result = deleteSingleFileNoCheck(target);
        }
        return result;
    }

    private static boolean deleteDirectoryNoCheck(File directory) {
        final File[] files = directory.listFiles();
        if (files == null) {  // null if security restricted
            System.out.println("Failed to list contents of [" + directory.getAbsolutePath() + "].");
            return false;
        }
        for (final File file : files) {
            if (!deleteExistsWithNoPermissionCheck(file)) {
                return false;
            }
        }
        return true;
    }

    private static boolean deleteSingleFileNoCheck(File file) {
        File deleteFile = file;
        final String deletePath = file.getAbsolutePath();
        final File newTempFile = new File(deletePath + ".delete");
        if (file.renameTo(newTempFile)) {
            deleteFile = newTempFile;
        }
        boolean deleteOk = deleteFile.exists() && deleteFile.delete();
        if (!deleteOk) {
            System.out.println("Could not delete [" + deletePath + "].");
        }
        return deleteOk;
    }

    public interface FilePathSegmentFilter {
        /**
         * @param pathname Maybe a full file absolute path, may be just file path segment, may be empty string.
         *                 Zip entry name is apply on ZipUtils class.
         */
        boolean accept(String pathname);
    }

    ////////////////////////////////////// just for test //////////////////////////////////////

    // Benchmark                     Mode  Samples         Score          Error  Units
    // UnzipTest.zipfile             avgt        4  70085414.616 ± 19146631.125  ns/op
    // UnzipTest.zipinputstream      avgt        4  81746111.179 ± 23409444.648  ns/op
    // UnzipTest.zipinputstream      avgt        4  78950748.131 ± 48377116.992  ns/op
    // UnzipTest.zipfile             avgt        4  68650953.926 ± 24305554.065  ns/op
    @Deprecated
    public static void unzipFileByZipInputStream(File zipFile, String unzipToDstDirPath, boolean recover,
                                 FilePathSegmentFilter filter, Collection<File> collect) throws IOException {
        try {
            if (zipFile == null || !zipFile.exists() || zipFile.isDirectory()
                    || unzipToDstDirPath == null || unzipToDstDirPath.trim().isEmpty()) {
                throw new IllegalArgumentException("Unzipping: zip file or unzip to target directory path is invalid");
            }
            unzipToDstDirPath = unzipToDstDirPath
                    + (unzipToDstDirPath.endsWith(File.separator) ? "" : File.separator);
            final File targetDir = new File(unzipToDstDirPath);
            if (!targetDir.exists() && !targetDir.mkdirs()) {
                throw new IllegalStateException("Unzipping: "
                        + "the target directory which store uncompressed files can't create.");
            }
            ZipInputStream zis = null;
            try {
                zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
                ZipEntry ze;
                while ((ze = zis.getNextEntry()) != null) {
                    final String entryName = ze.getName();
                    if (filter == null || filter.accept(entryName)) {
                        extractEntry(zis, entryName, targetDir, recover, collect, false);
                    }
                }
            } finally {
                if (zis != null) {
                    try {
                        zis.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        } catch (Throwable e) {
            if (!(e instanceof IOException)) {
                throw new IOException(e);
            } else {
                throw e;
            }
        }
    }

}
