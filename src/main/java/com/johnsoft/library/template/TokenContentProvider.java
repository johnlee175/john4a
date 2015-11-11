package com.johnsoft.library.template;

import java.util.HashMap;
import java.util.Map;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

/**
 * ContentProvider for tokens
 *
 * @author John Kenrinus Lee
 * @version 2015-08-26
 */
public final class TokenContentProvider extends ContentProvider {
    //Logging helper tag. No significance to providers.
    private static final String TAG = "TokenContentProvider";

    //uri and mime type definitions
    private static final String AUTHORITY = "com.johnsoft.provider.TokenContentProvider";
    private static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.johnsoft.tokens";
    private static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.johnsoft.tokens";
    private static final int INCOMING_DIR_URI_INDICATOR = 1;
    private static final int INCOMING_ITEM_URI_INDICATOR = 2;
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/tokens");

    //Projection maps are similar to "as" construct
    //in an sql statement where by you can rename the
    //columns.
    private static final Map<String, String> sProjectionMap = TokenTableMetaData.getDefaultProjectionMap();
    //Provide a mechanism to identify
    //all the incoming uri patterns.
    private static final UriMatcher sUriMatcher = createUriMatcher();

    private DatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        Log.w(TAG,"TokenContentProvider onCreate() called");
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case INCOMING_DIR_URI_INDICATOR:
                return CONTENT_TYPE;
            case INCOMING_ITEM_URI_INDICATOR:
                return CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {
            case INCOMING_DIR_URI_INDICATOR:
                qb.setTables(TokenTableMetaData.TABLE_NAME);
                qb.setProjectionMap(sProjectionMap);
                break;
            case INCOMING_ITEM_URI_INDICATOR:
                qb.setTables(TokenTableMetaData.TABLE_NAME);
                qb.setProjectionMap(sProjectionMap);
                qb.appendWhere(TokenTableMetaData._ID + "=" + uri.getPathSegments().get(1));
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = TokenTableMetaData.MODIFIED_DATETIME + " DESC ";
        } else {
            orderBy = sortOrder;
        }
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection,
                selectionArgs, null, null, orderBy);
        // Tell the cursor what uri to watch,
        // so it knows when its source data changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        // Validate the requested uri
        if (sUriMatcher.match(uri) != INCOMING_DIR_URI_INDICATOR) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        ContentValues values;
        if (contentValues != null) {
            values = new ContentValues(contentValues);
        } else {
            values = new ContentValues();
        }
        long now = System.currentTimeMillis();
        // Make sure that the fields are all set
        if (!values.containsKey(TokenTableMetaData.CREATED_DATETIME)) {
            values.put(TokenTableMetaData.CREATED_DATETIME, now);
        }
        values.put(TokenTableMetaData.MODIFIED_DATETIME, now);
        if (!values.containsKey(TokenTableMetaData.TOKEN_NAME)) {
            throw new SQLException("Failed to insert row because Token Name is needed " + uri);
        }
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert(TokenTableMetaData.TABLE_NAME,
                TokenTableMetaData.TOKEN_NAME, values);
        if (rowId > 0) {
            Uri insertedBookUri =
                    ContentUris.withAppendedId(CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(insertedBookUri, null);
            return insertedBookUri;
        }
        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case INCOMING_DIR_URI_INDICATOR:
                count = db.delete(TokenTableMetaData.TABLE_NAME,
                        where, whereArgs);
                break;
            case INCOMING_ITEM_URI_INDICATOR:
                String rowId = uri.getPathSegments().get(1);
                count = db.delete(TokenTableMetaData.TABLE_NAME,
                        TokenTableMetaData._ID + "=" + rowId
                                + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
                        whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case INCOMING_DIR_URI_INDICATOR:
                count = db.update(TokenTableMetaData.TABLE_NAME,
                        contentValues, where, whereArgs);
                break;
            case INCOMING_ITEM_URI_INDICATOR:
                String rowId = uri.getPathSegments().get(1);
                count = db.update(TokenTableMetaData.TABLE_NAME,
                        contentValues, TokenTableMetaData._ID + "=" + rowId
                                + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
                        whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    private static final UriMatcher createUriMatcher() {
        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, "tokens",
                INCOMING_DIR_URI_INDICATOR);
        uriMatcher.addURI(AUTHORITY, "tokens/#",
                INCOMING_ITEM_URI_INDICATOR);
        return uriMatcher;
    }

    public static final class TokenTableMetaData implements BaseColumns
    {
        private TokenTableMetaData() {}

        public static final String TABLE_NAME = "tokens";
        //Additional Columns start here.
        public static final String TOKEN_NAME = "name";
        public static final String TOKEN_VALUE = "value";
        public static final String TOKEN_VERSION = "version";
        public static final String EXPIRED_DATETIME = "expired_dt";
        public static final String CREATED_DATETIME = "created_dt";
        public static final String MODIFIED_DATETIME = "modified_dt";

        public static final String getCreateSqlString() {
            StringBuilder sb = new StringBuilder();
            sb.append("CREATE TABLE ").append(TABLE_NAME).append(" (");
            sb.append(_ID).append(" INTEGER PRIMARY KEY,");
            sb.append(TOKEN_NAME).append(" TEXT,");
            sb.append(TOKEN_VALUE).append(" TEXT,");
            sb.append(TOKEN_VERSION).append(" INTEGER,");
            sb.append(EXPIRED_DATETIME).append(" TEXT,");
            sb.append(CREATED_DATETIME).append(" TEXT,");
            sb.append(MODIFIED_DATETIME).append(" TEXT");
            sb.append(");");
            return sb.toString();
        }

        public static final String getDropSqlString() {
            StringBuilder sb = new StringBuilder();
            sb.append("DROP TABLE IF EXISTS ").append(TABLE_NAME);
            return sb.toString();
        }

        public static final Map<String, String> getDefaultProjectionMap() {
            HashMap<String, String> projectionMap = new HashMap<>();
            projectionMap.put(_ID, _ID);
            projectionMap.put(TOKEN_NAME, TOKEN_NAME);
            projectionMap.put(TOKEN_VALUE, TOKEN_VALUE);
            projectionMap.put(TOKEN_VERSION, TOKEN_VERSION);
            projectionMap.put(EXPIRED_DATETIME, EXPIRED_DATETIME);
            projectionMap.put(CREATED_DATETIME, CREATED_DATETIME);
            projectionMap.put(MODIFIED_DATETIME, MODIFIED_DATETIME);
            return projectionMap;
        }
    }

    /**
     * This class helps open, create, and upgrade the database file.
     */
    private static final class DatabaseHelper extends SQLiteOpenHelper {
        //database definitions
        private static final String DATABASE_NAME = "tokens.db";
        private static final int DATABASE_VERSION = 1;

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db)
        {
            Log.w(TAG, "DatabaseHelper onCreate() called");
            db.execSQL(TokenTableMetaData.getCreateSqlString());
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {
            Log.w(TAG, "DatabaseHelper onUpgrade() called with destroy all old data from "
                    + oldVersion + " to " + newVersion);
            db.execSQL(TokenTableMetaData.getDropSqlString());
            onCreate(db);
        }
    }
}
