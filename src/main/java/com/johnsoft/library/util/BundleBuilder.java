package com.johnsoft.library.util;

import java.io.Serializable;
import java.util.ArrayList;

import android.annotation.TargetApi;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.annotation.Nullable;

/**
 * Bundle的Builder类, 方便使用
 * @author John Kenrinus Lee
 * @version 2017-04-01
 */
public final class BundleBuilder {
    private final Bundle bundle = new Bundle();
    private volatile boolean hadBuild;

    public BundleBuilder() {
    }

    private void checkHadBuild() {
        if (hadBuild) {
            throw new IllegalStateException("you called build() before!");
        }
    }

    /**
     * Changes the ClassLoader this Bundle uses when instantiating objects.
     *
     * @param loader An explicit ClassLoader to use when instantiating objects
     * inside of the Bundle.
     */
    public BundleBuilder setClassLoader(ClassLoader loader) {
        checkHadBuild();
        bundle.setClassLoader(loader);
        return this;
    }

    /**
     * Inserts a Boolean value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a boolean
     */
    public BundleBuilder putBoolean(@Nullable String key, boolean value) {
        checkHadBuild();
        bundle.putBoolean(key, value);
        return this;
    }

    /**
     * Inserts a byte value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key a String, or null
     * @param value a byte
     */
    public BundleBuilder putByte(@Nullable String key, byte value) {
        checkHadBuild();
        bundle.putByte(key, value);
        return this;
    }

    /**
     * Inserts a char value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key a String, or null
     * @param value a char
     */
    public BundleBuilder putChar(@Nullable String key, char value) {
        checkHadBuild();
        bundle.putChar(key, value);
        return this;
    }

    /**
     * Inserts a short value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key a String, or null
     * @param value a short
     */
    public BundleBuilder putShort(@Nullable String key, short value) {
        checkHadBuild();
        bundle.putShort(key, value);
        return this;
    }

    /**
     * Inserts an int value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key a String, or null
     * @param value an int
     */
    public BundleBuilder putInt(@Nullable String key, int value) {
        checkHadBuild();
        bundle.putInt(key, value);
        return this;
    }

    /**
     * Inserts a long value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key a String, or null
     * @param value a long
     */
    public BundleBuilder putLong(@Nullable String key, long value) {
        checkHadBuild();
        bundle.putLong(key, value);
        return this;
    }

    /**
     * Inserts a float value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key a String, or null
     * @param value a float
     */
    public BundleBuilder putFloat(@Nullable String key, float value) {
        checkHadBuild();
        bundle.putFloat(key, value);
        return this;
    }

    /**
     * Inserts a double value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key a String, or null
     * @param value a double
     */
    public BundleBuilder putDouble(@Nullable String key, double value) {
        checkHadBuild();
        bundle.putDouble(key, value);
        return this;
    }

    /**
     * Inserts a CharSequence value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a CharSequence, or null
     */
    public BundleBuilder putCharSequence(@Nullable String key, @Nullable CharSequence value) {
        checkHadBuild();
        bundle.putCharSequence(key, value);
        return this;
    }

    /**
     * Inserts a String value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a String, or null
     */
    public BundleBuilder putString(@Nullable String key, @Nullable String value) {
        checkHadBuild();
        bundle.putString(key, value);
        return this;
    }

    /**
     * Inserts a boolean array value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a boolean array object, or null
     */
    public BundleBuilder putBooleanArray(@Nullable String key, @Nullable boolean[] value) {
        checkHadBuild();
        bundle.putBooleanArray(key, value);
        return this;
    }

    /**
     * Inserts a byte array value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a byte array object, or null
     */
    public BundleBuilder putByteArray(@Nullable String key, @Nullable byte[] value) {
        checkHadBuild();
        bundle.putByteArray(key, value);
        return this;
    }

    /**
     * Inserts a char array value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a char array object, or null
     */
    public BundleBuilder putCharArray(@Nullable String key, @Nullable char[] value) {
        checkHadBuild();
        bundle.putCharArray(key, value);
        return this;
    }

    /**
     * Inserts a short array value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a short array object, or null
     */
    public BundleBuilder putShortArray(@Nullable String key, @Nullable short[] value) {
        checkHadBuild();
        bundle.putShortArray(key, value);
        return this;
    }

    /**
     * Inserts an int array value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value an int array object, or null
     */
    public BundleBuilder putIntArray(@Nullable String key, @Nullable int[] value) {
        checkHadBuild();
        bundle.putIntArray(key, value);
        return this;
    }

    /**
     * Inserts a long array value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a long array object, or null
     */
    public BundleBuilder putLongArray(@Nullable String key, @Nullable long[] value) {
        checkHadBuild();
        bundle.putLongArray(key, value);
        return this;
    }

    /**
     * Inserts a float array value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a float array object, or null
     */
    public BundleBuilder putFloatArray(@Nullable String key, @Nullable float[] value) {
        checkHadBuild();
        bundle.putFloatArray(key, value);
        return this;
    }

    /**
     * Inserts a double array value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a double array object, or null
     */
    public BundleBuilder putDoubleArray(@Nullable String key, @Nullable double[] value) {
        checkHadBuild();
        bundle.putDoubleArray(key, value);
        return this;
    }

    /**
     * Inserts a CharSequence array value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a CharSequence array object, or null
     */
    public BundleBuilder putCharSequenceArray(@Nullable String key, @Nullable CharSequence[] value) {
        checkHadBuild();
        bundle.putCharSequenceArray(key, value);
        return this;
    }

    /**
     * Inserts a String array value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a String array object, or null
     */
    public BundleBuilder putStringArray(@Nullable String key, @Nullable String[] value) {
        checkHadBuild();
        bundle.putStringArray(key, value);
        return this;
    }

    /**
     * Inserts an ArrayList<Integer> value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value an ArrayList<Integer> object, or null
     */
    public BundleBuilder putIntegerArrayList(@Nullable String key, @Nullable ArrayList<Integer> value) {
        checkHadBuild();
        bundle.putIntegerArrayList(key, value);
        return this;
    }

    /**
     * Inserts an ArrayList<CharSequence> value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value an ArrayList<CharSequence> object, or null
     */
    public BundleBuilder putCharSequenceArrayList(@Nullable String key,
                                                  @Nullable ArrayList<CharSequence> value) {
        checkHadBuild();
        bundle.putCharSequenceArrayList(key, value);
        return this;
    }

    /**
     * Inserts a Serializable value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a Serializable object, or null
     */
    public BundleBuilder putSerializable(@Nullable String key, @Nullable Serializable value) {
        checkHadBuild();
        bundle.putSerializable(key, value);
        return this;
    }

    /**
     * Inserts a Parcelable value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a Parcelable object, or null
     */
    public BundleBuilder putParcelable(@Nullable String key, @Nullable Parcelable value) {
        checkHadBuild();
        bundle.putParcelable(key, value);
        return this;
    }

    /**
     * Inserts an array of Parcelable values into the mapping of this Bundle,
     * replacing any existing value for the given key.  Either key or value may
     * be null.
     *
     * @param key a String, or null
     * @param value an array of Parcelable objects, or null
     */
    public BundleBuilder putParcelableArray(@Nullable String key, @Nullable Parcelable[] value) {
        checkHadBuild();
        bundle.putParcelableArray(key, value);
        return this;
    }

    /**
     * Inserts a List of Parcelable values into the mapping of this Bundle,
     * replacing any existing value for the given key.  Either key or value may
     * be null.
     *
     * @param key a String, or null
     * @param value an ArrayList of Parcelable objects, or null
     */
    public BundleBuilder putParcelableArrayList(@Nullable String key,
                                                @Nullable ArrayList<? extends Parcelable> value) {
        checkHadBuild();
        bundle.putParcelableArrayList(key, value);
        return this;
    }

    /**
     * Inserts an {@link IBinder} value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * <p class="note">You should be very careful when using this function.  In many
     * places where Bundles are used (such as inside of Intent objects), the Bundle
     * can live longer inside of another process than the process that had originally
     * created it.  In that case, the IBinder you supply here will become invalid
     * when your process goes away, and no longer usable, even if a new process is
     * created for you later on.</p>
     *
     * @param key a String, or null
     * @param value an IBinder object, or null
     */
    @TargetApi(18)
    public BundleBuilder putBinder(@Nullable String key, @Nullable IBinder value) {
        checkHadBuild();
        bundle.putBinder(key, value);
        return this;
    }

    /**
     * Inserts a Bundle value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a Bundle object, or null
     */
    public BundleBuilder putBundle(@Nullable String key, @Nullable Bundle value) {
        checkHadBuild();
        bundle.putBundle(key, value);
        return this;
    }

    /**
     * Inserts all mappings from the given Bundle into this Bundle.
     *
     * @param bundle a Bundle
     */
    public BundleBuilder putAll(Bundle bundle) {
        checkHadBuild();
        bundle.putAll(bundle);
        return this;
    }

    public Bundle build() {
        checkHadBuild();
        hadBuild = true;
        return bundle;
    }
}
