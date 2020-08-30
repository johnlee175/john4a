package com.johnsoft.library.util.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * A sample
 *
 * @author John Kenrinus Lee
 * @version 2017-09-12
 */
class DbExamples {
    private static SQLiteHelper singleHelper;

    void setUp() {
        SQLiteWorker.getSharedInstance().start();
        // create custom SQLiteHelper singleHelper;
    }

    void testDQL() {
        final String tableName = "your table name";
        doDQL("select * from " + tableName, null, null, new DbUtils.DqlFunction<Object>() {
            @Override
            public Object doQuery(Cursor pCursor) {
                pCursor.moveToFirst();
                int type = DbUtils.getInt(pCursor, "type");
                String content = DbUtils.getString(pCursor, "content");
                return null;
            }
        }, new OnCompletedListener() {
            @Override
            public void onCompleted(Object event) {
                System.out.println("onCompleted");
            }
        });
    }

    void testDML() {
        doDML(null, new DbUtils.DmlFunction<Void>() {
            @Override
            public Void doExec(SQLiteDatabase db) {
                final String tableName = "your table name";
                db.execSQL("insert into " + tableName + " values (0, 'this is test message')");
                return null;
            }
        }, new OnCompletedListener() {
            @Override
            public void onCompleted(Object event) {
                System.out.println("onCompleted");
            }
        });
    }

    void tearDown() {
        SQLiteWorker.getSharedInstance().stop();
    }

    interface OnCompletedListener {
        void onCompleted(Object event);
    }

    static <T> void doDQL(final String sql, final String[] args, final T defaultValue,
                          final DbUtils.DqlFunction<T> function, final OnCompletedListener onCompleted) {
        SQLiteWorker.getSharedInstance().postDQL(new SQLiteWorker.AbstractSQLable() {
            // work thread
            @Override
            public Object doAysncSQL() {
                final SQLiteDatabase db = singleHelper.getReadableDatabase();
                final T result = DbUtils.query(db.rawQuery(sql, args), defaultValue, function);
                singleHelper.releaseReadableDatabase(db);
                return result;
            }

            // main thread
            @Override
            public void onCompleted(Object event) {
                if (onCompleted != null) {
                    onCompleted.onCompleted(event);
                }
            }
        });
    }

    static <T> void doDML(final T defaultValue,
                          final DbUtils.DmlFunction<T> function, final OnCompletedListener onCompleted) {
        SQLiteWorker.getSharedInstance().postDML(new SQLiteWorker.AbstractSQLable() {
            // work thread
            @Override
            public Object doAysncSQL() {
                final SQLiteDatabase db = singleHelper.getWritableDatabase();
                return DbUtils.exec(db, defaultValue, function);
            }

            // main thread
            @Override
            public void onCompleted(Object event) {
                if (onCompleted != null) {
                    onCompleted.onCompleted(event);
                }
            }
        });
    }
}
