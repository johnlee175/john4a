package com.johnsoft.library.util;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;

/**
 * 
 * A SharedPreferences proxy, enhance and provide something like modified with auto commit;
 * @author John Kenrinus Lee
 * @version  2015-01-12
 */
public final class SharedPreferencesWrapper
{
    private SharedPreferences delegate;
    
    public SharedPreferencesWrapper(Context context)
    {
        delegate = PreferenceManager.getDefaultSharedPreferences(context);
    }
    
    public SharedPreferencesWrapper(Context context, String name)
    {
        delegate = context.getSharedPreferences(name, Context.MODE_PRIVATE);
    }
    
    public SharedPreferencesWrapper(SharedPreferences sharedPreferences)
    {
        delegate = sharedPreferences;
    }
    
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener)
    {
        delegate.registerOnSharedPreferenceChangeListener(listener);
    }
    
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener)
    {
        delegate.unregisterOnSharedPreferenceChangeListener(listener);
    }
    
    public boolean contains(String key)
    {
        return delegate.contains(key);
    }
    
    public String getString(String key, String defValue)
    {
        return delegate.getString(key, defValue);
    }
    
    public int getInt(String key, int defValue)
    {
        return delegate.getInt(key, defValue);
    }
    
    public long getLong(String key, long defValue)
    {
        return delegate.getLong(key, defValue);
    }
    
    public float getFloat(String key, float defValue)
    {
        return delegate.getFloat(key, defValue);
    }
    
    public boolean getBoolean(String key, boolean defValue)
    {
        return delegate.getBoolean(key, defValue);
    }
    
    public Set<String> getStringSet(String key, Set<String> defValues)
    {
        return delegate.getStringSet(key, defValues);
    }
    
    public SharedPreferencesWrapper putString(String key, String value)
    {
        delegate.edit().putString(key, value).commit();
        return this;
    }
    
    public SharedPreferencesWrapper putInt(String key, int value)
    {
        delegate.edit().putInt(key, value).commit();
        return this;
    }
    
    public SharedPreferencesWrapper putLong(String key, long value)
    {
        delegate.edit().putLong(key, value).commit();
        return this;
    }
    
    public SharedPreferencesWrapper putFloat(String key, float value)
    {
        delegate.edit().putFloat(key, value).commit();
        return this;
    }
    
    public SharedPreferencesWrapper putBoolean(String key, boolean value)
    {
        delegate.edit().putBoolean(key, value).commit();
        return this;
    }

    public SharedPreferencesWrapper putStringSet(String key, Set<String> values)
    {
        delegate.edit().putStringSet(key, values).commit();
        return this;
    }
    
    public SharedPreferencesWrapper remove(String key)
    {
        delegate.edit().remove(key).commit();
        return this;
    }
    
    public SharedPreferencesWrapper clear()
    {
        delegate.edit().clear().commit();
        return this;
    }
    
    public String getStringWithEmptyDefault(String key)
    {
        return delegate.getString(key, "");
    }
    
    public String getStringWithNullDefault(String key)
    {
        return delegate.getString(key, null);
    }
    
    public int getIntWithZeroDefault(String key)
    {
        return delegate.getInt(key, 0);
    }
    
    public int getIntWithNegOneDefault(String key)
    {
        return delegate.getInt(key, -1);
    }
    
    public int getIntWithPosOneDefault(String key)
    {
        return delegate.getInt(key, 1);
    }
    
    public long getLongWithZeroDefault(String key)
    {
        return delegate.getLong(key, 0L);
    }
    
    public long getLongWithNegOneDefault(String key)
    {
        return delegate.getLong(key, -1L);
    }
    
    public long getLongWithPosOneDefault(String key)
    {
        return delegate.getLong(key, 1L);
    }
    
    public float getFloatWithZeroDefault(String key)
    {
        return delegate.getFloat(key, 0.0F);
    }
    
    public float getFloatWithNegOneDefault(String key)
    {
        return delegate.getFloat(key, -1.0F);
    }
    
    public float getFloatWithPosOneDefault(String key)
    {
        return delegate.getFloat(key, 1.0F);
    }
    
    public boolean getBooleanWithTrueDefault(String key)
    {
        return delegate.getBoolean(key, true);
    }
    
    public boolean getBooleanWithFalseDefault(String key)
    {
        return delegate.getBoolean(key, false);
    }
    
    public Set<String> getStringSetWithEmptyDefault(String key)
    {
        return delegate.getStringSet(key, new LinkedHashSet<String>());
    }
    
    public Set<String> getStringSetWithNullDefault(String key)
    {
        return delegate.getStringSet(key, null);
    }
    
    public SharedPreferencesWrapper putStringSet(String key, List<String> values)
    {
        delegate.edit().putStringSet(key, new LinkedHashSet<String>(values)).commit();
        return this;
    }
    
    public SharedPreferencesWrapper putStringSet(String key, String...values)
    {
        LinkedHashSet<String> set = new LinkedHashSet<String>();
        Collections.addAll(set, values);
        delegate.edit().putStringSet(key, set).commit();
        return this;
    }
    
    public SharedPreferencesWrapper putStringSetUnsafe(String key, List<String> values)
    {
        delegate.edit().putStringSet(key, new LinkedHashSet<String>(values)).apply();
        return this;
    }
    
    public SharedPreferencesWrapper putStringSetUnsafe(String key, String...values)
    {
        LinkedHashSet<String> set = new LinkedHashSet<String>();
        Collections.addAll(set, values);
        delegate.edit().putStringSet(key, set).apply();
        return this;
    }
    
    public SharedPreferencesWrapper putStringUnsafe(String key, String value)
    {
        delegate.edit().putString(key, value).apply();
        return this;
    }
    
    public SharedPreferencesWrapper putIntUnsafe(String key, int value)
    {
        delegate.edit().putInt(key, value).apply();
        return this;
    }
    
    public SharedPreferencesWrapper putLongUnsafe(String key, long value)
    {
        delegate.edit().putLong(key, value).apply();
        return this;
    }
    
    public SharedPreferencesWrapper putFloatUnsafe(String key, float value)
    {
        delegate.edit().putFloat(key, value).apply();
        return this;
    }
    
    public SharedPreferencesWrapper putBooleanUnsafe(String key, boolean value)
    {
        delegate.edit().putBoolean(key, value).apply();
        return this;
    }

    public SharedPreferencesWrapper putStringSetUnsafe(String key, Set<String> values)
    {
        delegate.edit().putStringSet(key, values).apply();
        return this;
    }
    
    public SharedPreferencesWrapper removeUnsafe(String key)
    {
        delegate.edit().remove(key).apply();
        return this;
    }
    
    public SharedPreferencesWrapper clearUnsafe()
    {
        delegate.edit().clear().apply();
        return this;
    }
    
    public SharedPreferencesWrapper putFalse(String key)
    {
        delegate.edit().putBoolean(key, false).commit();
        return this;
    }
    
    public SharedPreferencesWrapper putTrue(String key)
    {
        delegate.edit().putBoolean(key, true).commit();
        return this;
    }
    
    public SharedPreferencesWrapper putFalseUnsafe(String key)
    {
        delegate.edit().putBoolean(key, false).apply();
        return this;
    }
    
    public SharedPreferencesWrapper putTrueUnsafe(String key)
    {
        delegate.edit().putBoolean(key, true).apply();
        return this;
    }
}
