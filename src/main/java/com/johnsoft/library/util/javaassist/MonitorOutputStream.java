package com.johnsoft.library.util.javaassist;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.util.Log;

public class MonitorOutputStream extends FilterOutputStream {
    private static final String LOG_TAG = "SocketImplStream";

    public MonitorOutputStream(OutputStream in) {
        super(in);
        Log.i(LOG_TAG, "MonitorOutputStream created!");
    }

    @Override
    public void close() throws IOException {
        super.close();
        Log.i(LOG_TAG, "MonitorOutputStream closed!");
    }

    @Override
    public void flush() throws IOException {
        super.flush();
    }

    @Override
    public void write(byte[] buffer, int offset, int length) throws IOException {
//        System.err.println("write(byte[], int, int)");
        super.write(buffer, offset, length);
    }

    @Override
    public void write(int oneByte) throws IOException {
//        System.err.println("write(int)");
        super.write(oneByte);
    }

    @Override
    public void write(byte[] buffer) throws IOException {
//        System.err.println("write(byte[])");
        super.write(buffer);
    }
}