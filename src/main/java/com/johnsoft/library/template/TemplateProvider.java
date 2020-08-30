package com.johnsoft.library.template;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

public class TemplateProvider extends ContentProvider
{

	public static final String TAG = TemplateProvider.class.getSimpleName();
	public static final String AUTHORITIES = "com.template.provider.TemplateProvider";
	public static final String MAIN_PATH_SEGMENT = "templates";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITIES + "/" + MAIN_PATH_SEGMENT);
	public static final String TYPE_DIR = "vnd.android.cursor.dir/vnd.com." + MAIN_PATH_SEGMENT;
	public static final String TYPE_ITEM = "vnd.android.cursor.item/vnd.com." + MAIN_PATH_SEGMENT;
	public static final int ALL_ROWS = 1;
	public static final int SINGLE_ROW_BY_ID = 2;
	
	private static final UriMatcher URI_MATCHER;
	
	static
	{
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(AUTHORITIES, MAIN_PATH_SEGMENT, ALL_ROWS);
		URI_MATCHER.addURI(AUTHORITIES, MAIN_PATH_SEGMENT + "/#", SINGLE_ROW_BY_ID);
	}
	
	private TemplateSQLHelper helper;
	
	@Override
	public boolean onCreate()
	{
		helper = new TemplateSQLHelper(getContext());
		return true;
	}

	@Override
	public String getType(Uri uri)
	{
		switch (URI_MATCHER.match(uri))
		{
		case ALL_ROWS:
			return TYPE_DIR;
		case SINGLE_ROW_BY_ID:
			return TYPE_ITEM;
		default:
			throw new UnsupportedUriException(uri);
		}
	}
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder)
	{
		String groupBy = null;
		String having = null;
		SQLiteDatabase db = helper.getReadableDatabase();
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables(Table.TABLE_NAME);
		queryBuilder.setProjectionMap(Table.COLUMN_ALIAS_PROJECTION_MAP);
		switch (URI_MATCHER.match(uri))
		{
		case ALL_ROWS:
			break;
		case SINGLE_ROW_BY_ID:
			queryBuilder.appendWhere(Table._ID + "=" + ContentUris.parseId(uri));
			break;
		default:
			throw new UnsupportedUriException(uri);
		}
		Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, groupBy, having, sortOrder);
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues values)
	{
		SQLiteDatabase db = helper.getWritableDatabase();
		switch (URI_MATCHER.match(uri))
		{
		case ALL_ROWS:
		{
			fullAutoCreateKeyValue(values);
			long id = db.insert(Table.TABLE_NAME, Table._ID, values);
			if(id>=0)
			{
				Uri withIdUri = ContentUris.withAppendedId(uri, id);
				getContext().getContentResolver().notifyChange(withIdUri, null);
				return withIdUri;
			}else{
				new SQLException("Failed to insert row into " + uri);
			}
		}
		case SINGLE_ROW_BY_ID:
			return null;
		default:
			throw new UnsupportedUriException(uri);
		}
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs)
	{
		SQLiteDatabase db = helper.getWritableDatabase();
		switch (URI_MATCHER.match(uri))
		{
		case ALL_ROWS:
			break;
		case SINGLE_ROW_BY_ID:
		{
			selection = DatabaseUtils.concatenateWhere(selection, Table._ID + "=?");
			long id = ContentUris.parseId(uri);
			selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[]{String.valueOf(id)});
		}
			break;
		default:
			throw new UnsupportedUriException(uri);
		}
		fullAutoCreateKeyValue(values);
		int rowCount = db.update(Table.TABLE_NAME, values, selection, selectionArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return rowCount;
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs)
	{
		SQLiteDatabase db = helper.getWritableDatabase();
		switch (URI_MATCHER.match(uri))
		{
		case ALL_ROWS:
			break;
		case SINGLE_ROW_BY_ID:
		{
			selection = DatabaseUtils.concatenateWhere(selection, Table._ID + "=?");
			long id = ContentUris.parseId(uri);
			selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[]{String.valueOf(id)});
		}
			break;
		default:
			throw new UnsupportedUriException(uri);
		}
		int rowCount = db.delete(Table.TABLE_NAME, selection, selectionArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return rowCount;
	}
	
	private void fullAutoCreateKeyValue(ContentValues values)
	{
		Table.fullAutoCreateKeyValue(values);
	}
	
	public static final class Table implements BaseColumns
	{
		public static final String TABLE_NAME = MAIN_PATH_SEGMENT;
		public static final String COLUMN_TEXT_DISPLAY_NAME = "name";
		public static final String COLUMN_REAL_LEVEL = "level";
		public static final String COLUMN_TEXT_MODIFY_DATETIME = "modify_datetime";
		
		private static final Map<String, String> COLUMN_ALIAS_PROJECTION_MAP = new HashMap<String, String>();
		
		private static final String CREATE_SQL = "CREATE TABLE " + TABLE_NAME
												  + " ( " + _ID + " INTEGER PRIMARY KEY, "
														  + COLUMN_TEXT_DISPLAY_NAME + " TEXT NOT NULL UNIQUE ON CONFLICT REPLACE, "
														  + COLUMN_REAL_LEVEL + " REAL, "
														  + COLUMN_TEXT_MODIFY_DATETIME + " TEXT NOT NULL"
												  + " ) ";
		private static final String DROP_IF_EXIST = "DROP TABLE IF EXISTS " + TABLE_NAME;
		
		static
		{
			COLUMN_ALIAS_PROJECTION_MAP.put(_ID, _ID);
			COLUMN_ALIAS_PROJECTION_MAP.put(COLUMN_TEXT_DISPLAY_NAME, COLUMN_TEXT_DISPLAY_NAME);
			COLUMN_ALIAS_PROJECTION_MAP.put(COLUMN_REAL_LEVEL, COLUMN_REAL_LEVEL);
			COLUMN_ALIAS_PROJECTION_MAP.put(COLUMN_TEXT_MODIFY_DATETIME, COLUMN_TEXT_MODIFY_DATETIME);
		}
		
		public static final boolean setColumnNameAlias(String key, String value)
		{
			if(COLUMN_ALIAS_PROJECTION_MAP.containsKey(key))
			{
				COLUMN_ALIAS_PROJECTION_MAP.put(key, value);
				return true;
			}
			return false;
		}
		
		public static final String getCurrentTimestamp()
		{
			long milliseconds = System.currentTimeMillis();
			Date date = new Date(milliseconds);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
			return sdf.format(date);
		}
		
		private static final void fullAutoCreateKeyValue(ContentValues values)
		{
			if(!values.containsKey(COLUMN_TEXT_MODIFY_DATETIME))
			{
				values.put(COLUMN_TEXT_MODIFY_DATETIME, getCurrentTimestamp());
			}
		}
	}
	
	private static class TemplateSQLHelper extends SQLiteOpenHelper
	{
		private static final String DATABASE_NAME = MAIN_PATH_SEGMENT + ".db";
		private static final int DATABASE_VERSION = 1;
		
		public TemplateSQLHelper(Context context)
		{
			this(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
		
		public TemplateSQLHelper(Context context, String name,
				CursorFactory factory, int version)
		{
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db)
		{
			db.execSQL(Table.CREATE_SQL);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
			db.execSQL(Table.DROP_IF_EXIST);
			onCreate(db);
		}
		
		void upgradeTablesBySwap(SQLiteDatabase db, String tableName,
				String tableCreate, String columns) {
			try {
				db.beginTransaction();

				// 1, Rename table.
				String tempTableName = tableName + "_temp";
				String sql = "ALTER TABLE " + tableName + " RENAME TO "
						+ tempTableName;
				db.execSQL(sql);
				// 2, Create table.
				db.execSQL(tableCreate);
				// 3, Load data
				sql = "INSERT INTO " + tableName + " (" + columns + ") "
						+ " SELECT " + columns + " FROM " + tempTableName;
				db.execSQL(sql);
				// 4, Drop the temporary table.
				sql = "DROP TABLE IF EXISTS " + tempTableName;
				db.execSQL(sql);

				db.setTransactionSuccessful();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				db.endTransaction();
			}
		}

		void upgradeTablesByAddColumn(SQLiteDatabase db, String tableName,
				String featureCol, String column) {
			String oldTableCreateSql = "";
			Cursor cursor = db.rawQuery(
					"select sql from sqlite_master where tbl_name='" + tableName
							+ "' and type='table';", null);
			if (cursor.moveToNext())
				oldTableCreateSql = cursor.getString(0);
			if (!oldTableCreateSql.contains(featureCol)) {
				StringBuffer sql = new StringBuffer();
				sql.append("ALTER TABLE ").append(tableName)
						.append(" ADD COLUMN " + column);
				db.execSQL(sql.toString());
			}
		}
	}
	
	private static class UnsupportedUriException extends IllegalArgumentException
	{
		private static final long serialVersionUID = 1L;

		public UnsupportedUriException(Uri uri)
		{
			super(TAG + ": Unsupported URI: " + uri.toString());
		}
	}

}
