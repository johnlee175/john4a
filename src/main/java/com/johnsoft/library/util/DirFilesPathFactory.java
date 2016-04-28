package com.johnsoft.library.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * 可遍历枚举特定目录下的所有路径并排序迭代提供, 用于只读情况, 比如{@link DiskBlockSliceSource}. <br>
 * 此类是不可变类, 线程安全.
 * @author John Kenrinus Lee
 * @version 2016-02-18
 */
public class DirFilesPathFactory implements FilePathFactory {
    private final List<File> files;
    private final boolean loop;
    private ListIterator<File> iterator;
    public DirFilesPathFactory(File directory, boolean loopProvider, boolean sort) {
        if (directory == null || !directory.exists()
                || !directory.isDirectory() || !directory.canRead()) {
            throw new RuntimeException("DirFilesPathFactory: the param named directory"
            + "is null or is not a folder or not exists or can't read.");
        }
        final List<File> fileList;
        if (loopProvider) {
            fileList = new ArrayList<>();
        } else {
            fileList = new LinkedList<>();
        }
        walkByDepthFirst(directory, fileList);
        if (sort) {
            Collections.sort(fileList);
        }
        files = Collections.unmodifiableList(fileList);
        iterator = files.listIterator();
        loop = loopProvider;
    }

    @Override
    public String getNextFilePath() {
        if (files.isEmpty()) {
            return null;
        }
        if (loop && !iterator.hasNext()) {
                iterator = files.listIterator();
        }
        if(iterator.hasNext()) {
            return iterator.next().getAbsolutePath();
        }
        return null;
    }

    private void walkByDepthFirst(File file, List<File> files) {
        final LinkedList<File> stack = new LinkedList<>();
        stack.addFirst(file); //push
        while (!stack.isEmpty()) {
            file = stack.removeFirst(); //pop
            if (file.isDirectory()) {
                final File[] fs = file.listFiles();
                if (fs != null) {
                    for (final File f : fs) {
                        stack.addFirst(f); //push
                    }
                }
            } else {
                files.add(file);
            }
        }
    }
}
