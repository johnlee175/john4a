package com.johnsoft.library.util.javaassist;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.util.Log;

public class MonitorInputStream extends FilterInputStream {
    private static final String LOG_TAG = "SocketImplStream";

    public MonitorInputStream(InputStream in) {
        super(in);
        Log.i(LOG_TAG, "MonitorInputStream created!");
    }

    @Override
    public int available() throws IOException {
        return super.available();
    }

    @Override
    public void close() throws IOException {
        super.close();
        Log.i(LOG_TAG, "MonitorInputStream closed!");
    }

    @Override
    public synchronized void mark(int readlimit) {
        super.mark(readlimit);
    }

    @Override
    public boolean markSupported() {
        return super.markSupported();
    }

    @Override
    public int read() throws IOException {
//        System.err.println("read()");
        int result = super.read();
        if (result <= 0) {
            Log.i(LOG_TAG, "Socket inputStream EOF!");
        }
        return result;
    }

    @Override
    public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
//        System.err.println("read(byte[], int, int)");
        int result = super.read(buffer, byteOffset, byteCount);
        if (result <= 0) {
            Log.i(LOG_TAG, "Socket inputStream EOF!");
        }
        return result;
    }

    @Override
    public int read(byte[] buffer) throws IOException {
//        System.err.println("read(byte[])");
        int result = super.read(buffer);
        if (result <= 0) {
            Log.i(LOG_TAG, "Socket inputStream EOF!");
        }
        return result;
    }

    @Override
    public synchronized void reset() throws IOException {
        super.reset();
    }

    @Override
    public long skip(long byteCount) throws IOException {
        return super.skip(byteCount);
    }
}