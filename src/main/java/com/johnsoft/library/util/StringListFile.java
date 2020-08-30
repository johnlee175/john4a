/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package com.johnsoft.library.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;
import java.util.concurrent.ConcurrentHashMap;

import com.johnsoft.library.annotation.NotThreadSafe;

import android.support.annotation.NonNull;

/**
 * A String List, which synchronized with a file, is also a proxy of ArrayList.
 * It not thread safe, and it should be closed after all using.
 *
 * @author John Kenrinus Lee
 * @version 2015-11-10
 */
@NotThreadSafe
public class StringListFile
        implements List<String>, RandomAccess, Closeable {
    private static final ConcurrentHashMap<String, StringListFile> INSTANCES = new ConcurrentHashMap<>();

    /**
     * It will return single instance of StringListFile for per file absolute path.
     * So if you new two File objects A and B, but if A.equals(B), the method will return same instance.
     * If the file not exists, will be create.
     * If the file is directory, or the file can't be create or read or write, will be got null.
     */
    public static StringListFile getSingleInstanceFor(File file) {
        if (file == null || file.isDirectory()) {
            return null;
        }
        final String path = file.getAbsolutePath();
        StringListFile listFile = INSTANCES.get(path);
        if (listFile != null) {
            return listFile;
        }
        try {
            listFile = new StringListFile(file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        if(INSTANCES.putIfAbsent(path, listFile) == null) {
            return listFile;
        } else {
            return INSTANCES.get(path);
        }
    }

    public static void destroy(File file) {
        if (file == null || file.isDirectory()) {
            return;
        }
        final String path = file.getAbsolutePath();
        StringListFile listFile = INSTANCES.get(path);
        if (listFile != null) {
            listFile.close();
            INSTANCES.remove(path);
        }
    }

    private ArrayList<String> list;
    private File file;
    private OutputStream os;

    private StringListFile(File file) throws IOException {
        this.file = file;
        list = new ArrayList<>();

        if (file.exists()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    list.add(line);
                }
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception e) {
                    /* close silently */
                    }
                }
            }
        } else {
            file.createNewFile();
        }

        open();
    }

    @Override
    public void close() {
        if (os != null) {
            try {
                os.close();
            } catch (IOException e) {
                /* close silently */
            }
            os = null;
        }
    }

    /**
     * @return a clone of the internal string array list
     */
    public ArrayList<String> getList() {
        return (ArrayList<String>)list.clone();
    }

    /**
     * @return a clone of the file which synchronized with string array list
     */
    public File getFile() {
        return new File(file.getAbsolutePath());
    }

    private void open() throws IOException {
        os = new BufferedOutputStream(new FileOutputStream(file));
    }

    private void reopen() throws IOException {
        close();
        open();
    }

    private void println(String item) throws IOException {
        try {
            os.write(((item == null ? "null" : item) + "\n").getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void writeList() throws IOException {
        reopen(); // to clear all data in file
        final ArrayList<String> temp = (ArrayList<String>)list.clone();
        for (String item : temp) {
            println(item);
        }
    }

    @Override
    public void add(int location, String object) {
        list.add(location, object);
        try {
            writeList();
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    @Override
    public boolean add(String object) {
        boolean result = list.add(object);
        try {
            println(object);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        return result;
    }

    @Override
    public boolean addAll(int location, Collection<? extends String> collection) {
        boolean result = list.addAll(location, collection);
        try {
            writeList();
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        return result;
    }

    @Override
    public boolean addAll(Collection<? extends String> collection) {
        boolean result = list.addAll(collection);
        try {
            for (String item : collection) {
                println(item);
            }
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        return result;
    }

    @Override
    public void clear() {
        list.clear();
        try {
            reopen();
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    @Override
    public boolean contains(Object object) {
        return list.contains(object);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return list.containsAll(collection);
    }

    @Override
    public String get(int location) {
        return list.get(location);
    }

    @Override
    public int indexOf(Object object) {
        return list.indexOf(object);
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @NonNull
    @Override
    public Iterator<String> iterator() {
        return list.iterator();
    }

    @Override
    public int lastIndexOf(Object object) {
        return list.lastIndexOf(object);
    }

    @Override
    public ListIterator<String> listIterator() {
        return list.listIterator();
    }

    @NonNull
    @Override
    public ListIterator<String> listIterator(int location) {
        return list.listIterator(location);
    }

    @Override
    public String remove(int location) {
        String result = list.remove(location);
        try {
            writeList();
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        return result;
    }

    @Override
    public boolean remove(Object object) {
        boolean result = list.remove(object);
        try {
            writeList();
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        return result;
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        boolean result = list.removeAll(collection);
        try {
            writeList();
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        boolean result = list.retainAll(collection);
        try {
            writeList();
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        return result;
    }

    @Override
    public String set(int location, String object) {
        String result = list.set(location, object);
        try {
            writeList();
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        return result;
    }

    @Override
    public int size() {
        return list.size();
    }

    /**
     * @return a java.util.List interface implements instance, but not a instance of StringListFile;
     */
    @NonNull
    @Override
    public List<String> subList(int start, int end) {
        return list.subList(start, end);
    }

    @NonNull
    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    @NonNull
    @Override
    public <T> T[] toArray(T[] array) {
        return list.toArray(array);
    }

    public static final class IORuntimeException extends RuntimeException {
        public IORuntimeException(IOException thr) {
            super(thr);
        }
    }
}
