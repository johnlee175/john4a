package com.johnsoft.library.util.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.util.Log;

/**
 * Created by john on 15-4-17.
 */
public final class DbUtils
{
    private static final String TAG = "DbUtils";

    public static final String getString(@NonNull final Cursor pCursor, @NonNull final String pColumnName) {
        return pCursor.getString(pCursor.getColumnIndexOrThrow(pColumnName));
    }

    public static final int getInt(@NonNull final Cursor pCursor, @NonNull final String pColumnName) {
        return pCursor.getInt(pCursor.getColumnIndexOrThrow(pColumnName));
    }

    public static final byte[] getBlob(@NonNull final Cursor pCursor, @NonNull final String pColumnName) {
        return pCursor.getBlob(pCursor.getColumnIndexOrThrow(pColumnName));
    }

    public static final boolean getBoolean(@NonNull final Cursor pCursor, @NonNull final String pColumnName) {
        return getInt(pCursor, pColumnName) == 1 ? true : false;
    }

    public static final String preParseSql(String sql, String...args) {
        String[] segments = sql.split("#\\{\\w+?\\}");
        StringBuilder stringBuilder = new StringBuilder();
        assert segments.length == args.length + 1;
        for (int i = 0; i < args.length; i++) {
            stringBuilder.append(segments[i]).append(args[i]);
        }
        return stringBuilder.append(segments[segments.length - 1]).toString();
    }

    public static final <T> T query(@NonNull final Cursor pCursor,
                                    final T pDefaultValue,
                                    @NonNull final DqlFunction<T> pFunction) {
        try {
            return pFunction.doQuery(pCursor);
        } catch (Exception e) {
            Log.e(TAG, "", e);
            return pDefaultValue;
        } finally  {
            if (pCursor != null && !pCursor.isClosed()) pCursor.close();
        }
    }

    public static final <T> T exec(@NonNull final SQLiteDatabase db,
                                   final T pDefaultValue,
                                   @NonNull final DmlFunction<T> pFunction) {
        try {
            db.beginTransaction();
            T result = pFunction.doExec(db);
            db.setTransactionSuccessful();
            return result;
        } catch (Exception e) {
            Log.e(TAG, "", e);
            return pDefaultValue;
        } finally  {
            if (db != null && db.inTransaction()) db.endTransaction();
        }
    }

    public interface DqlFunction<T> {
        public T doQuery(Cursor pCursor);
    }

    public interface DmlFunction<T> {
        public T doExec(SQLiteDatabase db);
    }

}
