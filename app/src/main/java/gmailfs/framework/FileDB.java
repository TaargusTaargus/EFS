package gmailfs.framework;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import gmailfs.framework.Filter.FilterKey;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class FileDB extends SQLiteOpenHelper {

    // database info
    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "gfs.db";


    // filter table info
    public static final String FILTER_TABLE_NAME = "filters";
    private static final String ACCOUNT_KEY = "ACCOUNT";
    private static final String PATH_KEY = "PATH";
    private static final String KEY_KEY = "KEY";
    private static final String TITLE_KEY = "TITLE";
    private static final String TEXT_KEY = "TEXT";
    private static final String [] FILTER_TABLE_KEYS = {
            FileDB.ACCOUNT_KEY + " TEXT",
            FileDB.PATH_KEY + " TEXT",
            FileDB.TITLE_KEY + " TEXT",
            FileDB.TEXT_KEY + " TEXT",
            FileDB.KEY_KEY + " TEXT"
    };
    private static final String FILTER_TABLE_CREATE =
            "CREATE TABLE IF NOT EXISTS " + FILTER_TABLE_NAME + " (" +
                    TextUtils.join( ", ", FILTER_TABLE_KEYS) + ");";


    // item table info
    public static final String ITEM_TABLE_NAME = "items";
    private static final String ID_KEY = "FILEID";
    private static final String SUBJECT_KEY = "SUBJECT";
    private static final String SENDER_KEY = "SENDER";
    private static final String TIME_KEY = "TIME";
    private static final String [] ITEM_TABLE_KEYS = {
            FileDB.ID_KEY + " TEXT",
            FileDB.SUBJECT_KEY + " TEXT",
            FileDB.SENDER_KEY + " TEXT",
            FileDB.TIME_KEY + " DATE"
    };
    private static final String ITEM_TABLE_CREATE =
            "CREATE TABLE IF NOT EXISTS " + ITEM_TABLE_NAME + " (" +
                    TextUtils.join( ", ", ITEM_TABLE_KEYS) + ");";

    private String accountName;

    public FileDB( Context context ) {
        super( context, DATABASE_NAME, null, DATABASE_VERSION );
    }

    @Override
    public void onCreate( SQLiteDatabase db ) {
        db.execSQL( FILTER_TABLE_CREATE );
        db.execSQL( ITEM_TABLE_CREATE );
    }

    @Override
    public void onUpgrade( SQLiteDatabase db, int oldVersion, int newVersion ) { }

    public void insertFiles( List< File > files ) {
        SQLiteDatabase write = getWritableDatabase();
        for( File file : files ) {
            ContentValues values = new ContentValues();
            values.put( FileDB.ID_KEY, file.getFileID() );
            values.put( FileDB.SUBJECT_KEY, file.getSubject() );
            values.put( FileDB.SENDER_KEY, file.getSender() );
            values.put( FileDB.TIME_KEY, file.getTimestamp() );

            write.insert( FileDB.ITEM_TABLE_NAME, null, values );
        }
        write.close();
    }

    public void insertFile( File file ) {
        SQLiteDatabase write = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put( FileDB.ID_KEY, file.getFileID() );
        values.put( FileDB.SUBJECT_KEY, file.getSubject() );
        values.put( FileDB.SENDER_KEY, file.getSender() );
        values.put( FileDB.TIME_KEY, file.getTimestamp() );

        write.insert( FileDB.ITEM_TABLE_NAME, null, values );
        write.close();
    }

    public void insertFilter( Filter filter, Path path ) {
        SQLiteDatabase write = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put( FileDB.ACCOUNT_KEY, accountName );
        values.put( FileDB.PATH_KEY, path.getFilterPath() );
        values.put( FileDB.KEY_KEY, filter.getFilterKey().toString() );
        values.put( FileDB.TITLE_KEY, filter.getFilterID() );
        values.put( FileDB.TEXT_KEY, filter.getFilterText() );

        write.insert( FileDB.FILTER_TABLE_NAME, null, values );
        write.close();
    }

    public void recursiveRemoveFilter( Filter filter, Path path ) {
        SQLiteDatabase write = getWritableDatabase();
        String where = FileDB.PATH_KEY + " LIKE '%" + path.getFilterPath() + filter.getFilterID() + "%'"
                            + " AND " + FileDB.ACCOUNT_KEY + "='" + accountName + "'";

        write.delete( FileDB.FILTER_TABLE_NAME, where, null );
        write.close();
        removeFilter( filter, path );
    }

    public void removeFilter( Filter filter, Path path ) {
        SQLiteDatabase write = getWritableDatabase();
        String where = FileDB.PATH_KEY + "=? AND "
                                    + FileDB.TITLE_KEY + "=?"
                        + " AND " + FileDB.ACCOUNT_KEY + "=?";

        write.delete( FileDB.FILTER_TABLE_NAME, where, new String [] { path.getFilterPath(), filter.getFilterID(), accountName } );
        write.close();
    }

    public void updateFilter( Filter old, Filter filter, Path pathToFilter ) {
        removeFilter( old, pathToFilter );
        insertFilter( filter, pathToFilter );

        SQLiteDatabase write = getWritableDatabase();
        String updateRecursive = "UPDATE " + FileDB.FILTER_TABLE_NAME + " SET " + FileDB.PATH_KEY
                        + " = REPLACE(" + FileDB.PATH_KEY + ", '"
                        + pathToFilter.getFilterPath() + old.getFilterID() + "', '"
                        + pathToFilter.getFilterPath() + filter.getFilterID() + "')"
                        + " WHERE " + FileDB.PATH_KEY + " LIKE '"
                        + pathToFilter.getFilterPath() + old.getFilterID() + "%'"
                        + " AND " + FileDB.ACCOUNT_KEY + "='" + accountName + "'";
        write.execSQL( updateRecursive );
        write.close();
    }

    public HashMap< String, File > retrieveAllFiles() {
        SQLiteDatabase read = getReadableDatabase();
        Cursor results = read.query( FileDB.ITEM_TABLE_NAME,
                new String [] { FileDB.ID_KEY, FileDB.SUBJECT_KEY, FileDB.SENDER_KEY, FileDB.TIME_KEY },
                null , null, null, null, FileDB.TIME_KEY + " DESC" );
        HashMap< String, File > files = new HashMap();
        while( results.moveToNext() )
            files.put( results.getString( results.getColumnIndexOrThrow( FileDB.ID_KEY ) ),
                    new File( results.getString( results.getColumnIndexOrThrow( FileDB.ID_KEY ) ),
                            results.getString( results.getColumnIndexOrThrow( FileDB.SENDER_KEY ) ),
                            results.getString( results.getColumnIndexOrThrow( FileDB.SUBJECT_KEY ) ),
                            results.getString( results.getColumnIndexOrThrow( FileDB.TIME_KEY ) ) )
            );
        read.close();
        return files;
    }

    public HashMap< String, File > retrieveFilesFromQuery( String query, FilterKey key ) {
        SQLiteDatabase read = getReadableDatabase();
        Cursor results = read.query( FileDB.ITEM_TABLE_NAME,
                new String [] { FileDB.ID_KEY, FileDB.SUBJECT_KEY, FileDB.SENDER_KEY, FileDB.TIME_KEY },
                key.getHeaderKeyword() + " LIKE " + "'%" + query + "%'", null, null, null,
                FileDB.TIME_KEY + " DESC" );
        HashMap< String, File > files = new HashMap();
        while( results.moveToNext() )
            files.put( results.getString( results.getColumnIndexOrThrow( FileDB.ID_KEY ) ),
                            new File( results.getString( results.getColumnIndexOrThrow( FileDB.ID_KEY ) ),
                                    results.getString( results.getColumnIndexOrThrow( FileDB.SENDER_KEY ) ),
                                    results.getString( results.getColumnIndexOrThrow( FileDB.SUBJECT_KEY ) ),
                                    results.getString( results.getColumnIndexOrThrow( FileDB.TIME_KEY ) ) )
            );
        read.close();
        return files;
    }

    public LinkedList< Filter > retrieveFiltersFromPath( Path path ) {
        SQLiteDatabase read = getReadableDatabase();
        Cursor results = read.query( FileDB.FILTER_TABLE_NAME,
                new String [] { FileDB.PATH_KEY, FileDB.KEY_KEY, FileDB.TITLE_KEY, FileDB.TEXT_KEY },
                FileDB.PATH_KEY + "=?" + " AND " + FileDB.ACCOUNT_KEY + "=?", new String [] { path.getFilterPath(), accountName },
                null, null, FileDB.TITLE_KEY + " ASC" );
        LinkedList< Filter > children = new LinkedList();
        while( results.moveToNext() ) {
            children.add(
                    new Filter( results.getString( results.getColumnIndexOrThrow( FileDB.TEXT_KEY ) ),
                            results.getString( results.getColumnIndexOrThrow( FileDB.TITLE_KEY ) ),
                            FilterKey.stringToFilterKey (
                                    results.getString( results.getColumnIndexOrThrow( FileDB.KEY_KEY ) )
                            ) )
            );
        }
        read.close();
        return children;
    }

    public Integer getRowCount( String tableName ) {
        String countQuery = "SELECT  * FROM " + tableName;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery( countQuery, null );
        int cnt = cursor.getCount();
        cursor.close();
        db.close();
        return cnt;
    }

    public void printFileDBtoLogcat() {
        SQLiteDatabase read = getReadableDatabase();
        String [] keys = new String [] { FileDB.ID_KEY, FileDB.SENDER_KEY, FileDB.SUBJECT_KEY, FileDB.TIME_KEY };
        Cursor results = read.rawQuery( "SELECT  * FROM " + FileDB.ITEM_TABLE_NAME, null );
        Log.d( "FileDB: ", TextUtils.join( ", ", keys ) );
        while( results.moveToNext() ){
            Log.d( "FileDB: ", results.getString( results.getColumnIndexOrThrow( FileDB.ID_KEY ) ) + ", "
                                    +  results.getString( results.getColumnIndexOrThrow( FileDB.SENDER_KEY ) ) + ", "
                                    + results.getString( results.getColumnIndexOrThrow( FileDB.SUBJECT_KEY ) ) + ", "
                                    + results.getString( results.getColumnIndexOrThrow( FileDB.TIME_KEY ) ) );
        }
        results.close();
        read.close();

    }

    public void printFilterDBtoLogcat() {
        SQLiteDatabase read = getReadableDatabase();
        String [] keys = new String [] { FileDB.ACCOUNT_KEY, FileDB.PATH_KEY, FileDB.TITLE_KEY, FileDB.TEXT_KEY, FileDB.KEY_KEY };
        Cursor results = read.rawQuery( "SELECT  * FROM " + FileDB.FILTER_TABLE_NAME, null );
        Log.d( "FileDB: ", TextUtils.join( ", ", keys ) );
        while( results.moveToNext() ){
            Log.d( "FileDB: ", results.getString( results.getColumnIndexOrThrow( FileDB.ACCOUNT_KEY ) ) + ", "
                    + results.getString( results.getColumnIndexOrThrow( FileDB.PATH_KEY ) ) + ", "
                    + results.getString( results.getColumnIndexOrThrow( FileDB.TITLE_KEY ) ) + ", "
                    + results.getString( results.getColumnIndexOrThrow( FileDB.TEXT_KEY ) ) + ", "
                    + results.getString( results.getColumnIndexOrThrow( FileDB.KEY_KEY ) ) );
        }
        results.close();
        read.close();

    }

    public void setAccountName( String accountName ) { this.accountName = accountName; }

}