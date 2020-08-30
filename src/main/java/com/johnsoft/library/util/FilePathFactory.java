package com.johnsoft.library.util;

/**
 * A interface which will indicate how to define the file name if writing to file perhaps.
 *
 * @author John Kenrinus Lee
 * @version 2016-02-18
 */
public interface FilePathFactory {
    String getNextFilePath();
}