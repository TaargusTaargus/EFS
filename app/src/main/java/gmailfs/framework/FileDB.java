package gmailfs.framework;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import gmailfs.framework.Filter.FilterKey;
import gmailfs.framework.Filter.FilterType;

import java.util.LinkedList;

public class FileDB extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "gfs.db";
    private static final String FILTER_TABLE_NAME = "filters";
    private static final String PATH_KEY = "PATH";
    private static final String KEY_KEY = "KEY";
    private static final String TITLE_KEY = "TITLE";
    private static final String TEXT_KEY = "TEXT";
    private static final String TYPE_KEY = "TYPE";
    private static final String [] DATABASE_KEYS = {
            FileDB.PATH_KEY + " TEXT",
            FileDB.TITLE_KEY + " TEXT",
            FileDB.TEXT_KEY + " TEXT",
            FileDB.KEY_KEY + " TEXT",
            FileDB.TYPE_KEY + " TEXT"
    };
    private static final String FILTER_TABLE_CREATE =
            "CREATE TABLE IF NOT EXISTS " + FILTER_TABLE_NAME + " (" +
                    TextUtils.join( ", ", DATABASE_KEYS ) + ");";

    public FileDB( Context context ) {
        super( context, DATABASE_NAME, null, DATABASE_VERSION );
    }

    @Override
    public void onCreate( SQLiteDatabase db ) {
        db.execSQL( FILTER_TABLE_CREATE );
    }

    @Override
    public void onUpgrade( SQLiteDatabase db, int oldVersion, int newVersion ) { }

    public void insertFilter( Filter filter, Path path ) {
        SQLiteDatabase write = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put( FileDB.PATH_KEY, path.toString() );
        values.put( FileDB.KEY_KEY, filter.getFilterKey().toString() );
        values.put( FileDB.TITLE_KEY, filter.getFileTitle() );
        values.put( FileDB.TEXT_KEY, filter.getFilterText() );
        values.put( FileDB.TYPE_KEY, filter.getFilterType().toString() );

        write.insert( FileDB.FILTER_TABLE_NAME, null, values );
    }

    public LinkedList< Filter > retrieveChildren( Path path ) {
        SQLiteDatabase read = getReadableDatabase();
        Cursor results = read.query( FileDB.FILTER_TABLE_NAME,
                                        new String [] { FileDB.PATH_KEY, FileDB.KEY_KEY, FileDB.TITLE_KEY, FileDB.TEXT_KEY, FileDB.TYPE_KEY },
                                        FileDB.PATH_KEY + "=?", new String [] { path.toString() },
                                        null, null, FileDB.TITLE_KEY + " DESC" );
        LinkedList< Filter > children = new LinkedList();
        results.moveToFirst();
        while( results.getCount() > 0 && !results.isLast() ) {
            children.add(
                    new Filter( results.getString( results.getColumnIndexOrThrow( FileDB.TEXT_KEY ) ),
                                    results.getString( results.getColumnIndexOrThrow( FileDB.TITLE_KEY ) ),
                                    FilterKey.stringToFilterKey (
                                        results.getString( results.getColumnIndexOrThrow( FileDB.KEY_KEY ) )
                                    ),
                                    FilterType.stringToFilterType (
                                            results.getString( results.getColumnIndexOrThrow( FileDB.TYPE_KEY ) )
                                    ) )
            );
            results.moveToNext();
        }
        results.close();
        return children;
    }

}