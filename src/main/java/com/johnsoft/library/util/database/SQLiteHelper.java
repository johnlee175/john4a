package com.johnsoft.library.util.database;

import com.johnsoft.library.util.Pool;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteException;
import android.util.Log;

/**
 * copy from SQLiteOpenHelper, and change it for:
 * your SQLiteDatabase got by getWritableDatabase() and getReadableDatabase() is different;
 * as database write, you got the same one, as database read, you got the one from pool;
 * you should extends this class and the child should use singleton.
 * for example, init the only one on application create, and call close just when application exit;
 *
 * @author John Kenrinus Lee 2014-06-01
 */
public abstract class SQLiteHelper {
    private static final String TAG = SQLiteHelper.class.getSimpleName();

    private final Context mContext;
    private final String mName;
    private final CursorFactory mFactory;
    private final int mNewVersion;
    private SQLiteDatabase mWDatabase;
    private Pool<SQLiteDatabase> mRDatabases;

    private boolean mIsInitializing;
    private final DatabaseErrorHandler mErrorHandler; //if the min api is 10, comment it

    public SQLiteHelper(Context context, String name, CursorFactory factory, int version) {
        this(context, name, factory, version, null);
    }

    public SQLiteHelper(Context context, String name, CursorFactory factory, int version,
                        DatabaseErrorHandler errorHandler) {
        if (version < 1) {
            throw new IllegalArgumentException("Version must be >= 1, was " + version);
        }

        mContext = context;
        mName = name;
        mFactory = factory;
        mNewVersion = version;
        mErrorHandler = errorHandler; //if the min api is 10, comment it
        getWritableDatabase(); //call it here because of the getReadableDatabase() won't create or upgrade the database
    }

    public String getDatabaseName() {
        return mName;
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public void onOpen(SQLiteDatabase db) {
    }

    public void onConfigure(SQLiteDatabase db) {
    }

    public abstract void onCreate(SQLiteDatabase db);

    public abstract void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);

    public synchronized void close() {
        if (mIsInitializing) {
            throw new IllegalStateException("Closed during initialization");
        }

        if (mWDatabase != null && mWDatabase.isOpen()) {
            mWDatabase.close();
            mWDatabase = null;
        }

        mRDatabases.destory();
        mRDatabases = null;
    }

    /**
     * each operate of database write, you should get instance of SQLiteDatabase by calling this method
     */
    public synchronized SQLiteDatabase getWritableDatabase() {
        if (mWDatabase != null) {
            if (!mWDatabase.isOpen()) {
                // Darn! The user closed the database by calling
                // mDatabase.close().
                mWDatabase = null;
            } else if (!mWDatabase.isReadOnly()) {
                // The database is already open for business.
                return mWDatabase;
            }
        }

        if (mIsInitializing) {
            throw new IllegalStateException("getDatabase called recursively");
        }

        SQLiteDatabase db = mWDatabase;
        try {
            mIsInitializing = true;

            if (mName == null) {
                db = SQLiteDatabase.create(null);
            } else {
                try {
                    db = mContext.openOrCreateDatabase(mName, Context.MODE_ENABLE_WRITE_AHEAD_LOGGING/* use 0 if the min api
                    is 10 */,
                            mFactory, mErrorHandler);
                } catch (SQLiteException ex) {
                    ex.printStackTrace();
                    Log.e(TAG, "Couldn't open " + mName + " for writing (will try read-only):", ex);
                    final String path = mContext.getDatabasePath(mName).getPath();
                    db = SQLiteDatabase.openDatabase(path, mFactory, SQLiteDatabase.OPEN_READONLY,
                            mErrorHandler);
                    // db = SQLiteDatabase.openDatabase(path, mFactory, SQLiteDatabase.OPEN_READONLY);//if min api is 10
                }
            }

            onConfigure(db);

            final int version = db.getVersion();
            if (version != mNewVersion) {
                if (db.isReadOnly()) {
                    throw new SQLiteException("Can't upgrade read-only database from version "
                            + db.getVersion() + " to " + mNewVersion + ": " + mName);
                }

                db.beginTransaction();
                try {
                    if (version == 0) {
                        onCreate(db);
                    } else {
                        if (version > mNewVersion) {
                            onDowngrade(db, version, mNewVersion);
                        } else {
                            onUpgrade(db, version, mNewVersion);
                        }
                    }
                    db.setVersion(mNewVersion);
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            onOpen(db);

            if (db.isReadOnly()) {
                Log.w(TAG, "Opened " + mName + " in read-only mode");
            }

            mWDatabase = db;
            return db;
        } finally {
            mIsInitializing = false;
            if (db != null && db != mWDatabase) {
                db.close();
            }
        }
    }

    /**
     * each operate of database read, you should get instance of SQLiteDatabase by calling this method, and call
     * releaseReadableDatabase() when something is done
     */
    public SQLiteDatabase getReadableDatabase() {
        if (mRDatabases == null) {
            synchronized (SQLiteHelper.this) {
                if (mRDatabases == null) {
                    if (mIsInitializing) {
                        throw new IllegalStateException("getDatabase called recursively");
                    }

                    try {
                        mIsInitializing = true;

                        mRDatabases = new Pool<SQLiteDatabase>(4) {
                            @Override
                            protected SQLiteDatabase create() {
                                synchronized(SQLiteHelper.this) {
                                    if (mIsInitializing) {
                                        throw new IllegalStateException("getDatabase called recursively");
                                    }

                                    SQLiteDatabase db = null;
                                    try {
                                        mIsInitializing = true;

                                        if (mName == null) {
                                            db = SQLiteDatabase.create(null);
                                        } else {
                                            try {
                                                db = mContext.openOrCreateDatabase(mName,
                                                        Context.MODE_ENABLE_WRITE_AHEAD_LOGGING/* use 0 if the min api is 10 */,
                                                        mFactory,
                                                        mErrorHandler);
                                            } catch (SQLiteException ex) {
                                                ex.printStackTrace();
                                                Log.e(TAG, "Couldn't open " + mName
                                                        + " for writing (will try read-only):", ex);
                                                final String path = mContext.getDatabasePath(mName).getPath();
                                                db = SQLiteDatabase.openDatabase(path, mFactory,
                                                        SQLiteDatabase.OPEN_READONLY, mErrorHandler);
                                                // db = SQLiteDatabase.openDatabase(path, mFactory, SQLiteDatabase.OPEN_READONLY);
                                                // if min api is 10
                                            }
                                        }

                                        onConfigure(db);

                                        onOpen(db);

                                        if (db.isReadOnly()) {
                                            Log.w(TAG, "Opened " + mName + " in read-only mode");
                                        }

                                        return db;
                                    } finally {
                                        mIsInitializing = false;
                                    }
                                }
                            }

                            @Override
                            protected void release(SQLiteDatabase t) {
                                t.close();
                                t = null;
                            }
                        };
                    } finally {
                        mIsInitializing = false;
                    }
                }
            }
        }

        SQLiteDatabase mRDatabase = mRDatabases.use();
        if (mRDatabase != null) {
            if (!mRDatabase.isOpen()) {
                Log.e(TAG, "the database closed by user, expert closed by this class with close()");
                return null;
            }
        }
        return mRDatabase;
    }

    public synchronized void releaseReadableDatabase(SQLiteDatabase database) {
        mRDatabases.unuse(database);
    }
}
