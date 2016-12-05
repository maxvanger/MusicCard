package com.fantom.vanger.musikcards;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.fantom.vanger.musikcards.db.PuzzleDbHelper;
import com.fantom.vanger.musikcards.db.PuzzleDbSchema;
import com.fantom.vanger.musikcards.db.PuzzleDbSchema.AlbumsTable;
import com.fantom.vanger.musikcards.db.PuzzleDbSchema.PuzzleTable;

/**
 * Created by Vanger on 20.11.2016.
 */

public class ProviderPuzzle extends ContentProvider {
    private static final UriMatcher sUriMatcher;
    public static final int URI_ID_PUZZLES = 11201;
    public static final int URI_ID_PUZZLE = 11202;
    public static final int URI_ID_PUZZLE_PAIRS = 11203;
    public static final int URI_ID_ALBUMS = 11204;
    public static final int URI_ID_ALBUM = 11205;
    static {
        //may be in future to do so:
        //puzzles/#         AUTHORITY/puzzles/[albumId]
        //puzzles/#/#       AUTHORITY/puzzles/[albumId]/[amount]
        //puzzles/#/pairs/# AUTHORITY/puzzles/[albumID]/pairs/[amount]
        //puzzles/id/#      AUTHORITY/puzzles/id/[puzzleID]
        //but now exist are following URIs:
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(PuzzleDbSchema.AUTHORITY, PuzzleTable.TABLE_NAME+"/#", URI_ID_PUZZLES);
        sUriMatcher.addURI(PuzzleDbSchema.AUTHORITY, PuzzleTable.TABLE_NAME+"/#/#", URI_ID_PUZZLE);
        sUriMatcher.addURI(PuzzleDbSchema.AUTHORITY, PuzzleTable.TABLE_NAME+"/#/pairs", URI_ID_PUZZLE_PAIRS);
        sUriMatcher.addURI(PuzzleDbSchema.AUTHORITY,AlbumsTable.TABLE_NAME, URI_ID_ALBUMS);
        sUriMatcher.addURI(PuzzleDbSchema.AUTHORITY,AlbumsTable.TABLE_NAME+"/#", URI_ID_ALBUM);
    }

    private PuzzleDbHelper puzzleHelper;

    public ProviderPuzzle() {
        super();
    }

    @Override
    public boolean onCreate() {
        puzzleHelper = new PuzzleDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String orderBy) {
        SQLiteDatabase db;
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        queryBuilder.setTables(PuzzleTable.TABLE_NAME);
        db = puzzleHelper.getReadableDatabase();

        switch(sUriMatcher.match(uri)) {
            case URI_ID_PUZZLES:
                queryBuilder.appendWhere(PuzzleTable.ALBUM+"="+uri.getPathSegments().get(1));
                break;
            case URI_ID_PUZZLE:
                queryBuilder.appendWhere(PuzzleTable.ID+"="+uri.getPathSegments().get(2));
                break;
            case URI_ID_PUZZLE_PAIRS:
                //TODO
                //get random puzzles(mAmount/2) with Link!=-1
                //SELECT * FROM Puzzletable WHERE LINK>=0 ORDER BY RANDOM() LIMIT Amount
                //while(!isAfterLast()){ listID.add(cursor.getLink);listID.add(cursor.getId);}
                // cur = SELECT * FROM PuzzleTable WHERE ID=(lock1|key1|lock2|key2);
                // return cur;
                Cursor cursorLock = db.query(true,
                        PuzzleTable.TABLE_NAME,
                        new String[]{PuzzleTable.LINK,PuzzleTable.ID},
                        PuzzleTable.LINK+">=0 AND "+PuzzleTable.ALBUM+"=?",
                        new String[]{uri.getPathSegments().get(1)}, //album ID
                        PuzzleTable.LINK,null,orderBy,null);
                cursorLock.moveToFirst();
                StringBuilder sb = new StringBuilder("(");
                while(!cursorLock.isAfterLast()){
                    sb.append(cursorLock.getInt(cursorLock.getColumnIndex(PuzzleTable.ID)));
                    sb.append(",");
                    sb.append(cursorLock.getInt(cursorLock.getColumnIndex(PuzzleTable.LINK)));
                    if(cursorLock.moveToNext()) sb.append(",");
                }
                sb.append(")");
                queryBuilder.appendWhere(PuzzleTable.ID+" IN "+sb.toString());
                break;
            case URI_ID_ALBUM:
                queryBuilder.appendWhere(AlbumsTable.ID+"="+uri.getPathSegments().get(1));
            case URI_ID_ALBUMS:
                queryBuilder.setTables(AlbumsTable.TABLE_NAME);
                db = puzzleHelper.getReadableDatabase();
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: "+ uri);
        }

        String order = TextUtils.isEmpty(orderBy) ? PuzzleDbSchema.ORDER_BY : orderBy;
        if (sUriMatcher.match(uri)==URI_ID_PUZZLE_PAIRS) order="";
        Cursor c = queryBuilder.query(db,projection,selection,selectionArgs,null,null,order);
        c.setNotificationUri(getContext().getContentResolver(),uri);
        return c;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        switch(sUriMatcher.match(uri)){
            case URI_ID_PUZZLE_PAIRS:
            case URI_ID_PUZZLES:
                return PuzzleDbSchema.CONTENT_PUZZLES;
            case URI_ID_PUZZLE:
                return PuzzleDbSchema.CONTENT_PUZZLE_ITEM;
            case URI_ID_ALBUMS:
                return PuzzleDbSchema.CONTENT_ALBUMS;
            case URI_ID_ALBUM:
                return PuzzleDbSchema.CONTENT_ALBUM_ITEM;
            default:
                throw new IllegalArgumentException("Unknown URI: "+uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        SQLiteOpenHelper helper;
        String tableName;
        helper = new PuzzleDbHelper(getContext());
        switch(sUriMatcher.match(uri)){
            case URI_ID_PUZZLES:
                tableName = PuzzleTable.TABLE_NAME;
                //newUri = Uri.parse(PuzzleDbSchema.CONTENT_URI+tableName+ContentUris.parseId(uri));
                break;
            case URI_ID_ALBUMS:
                //helper = new AlbumDbHelper(getContext());
                tableName = AlbumsTable.TABLE_NAME;
                //newUri = Uri.parse(PuzzleDbSchema.CONTENT_URI+tableName);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: "+uri);
        }
        SQLiteDatabase db = helper.getWritableDatabase();
        long rowId = db.insert(tableName,null,contentValues);
        if (rowId>0){
            Uri insertedUri = ContentUris.withAppendedId(uri,rowId);//(uri,rowId)
            getContext().getContentResolver().notifyChange(insertedUri,null);
            return insertedUri;
        }
        throw new SQLException("Failed to insert row into "+uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteOpenHelper helper;
        String tableName;
        helper = new PuzzleDbHelper(getContext());
        switch(sUriMatcher.match(uri)){
            case URI_ID_PUZZLES:
            case URI_ID_PUZZLE:
                tableName = PuzzleTable.TABLE_NAME;
                break;
            case URI_ID_ALBUMS:
            case URI_ID_ALBUM:
                //helper = new AlbumDbHelper(getContext());
                tableName = AlbumsTable.TABLE_NAME;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: "+uri);
        }
        SQLiteDatabase db = helper.getWritableDatabase();
        int count = db.delete(tableName,where,whereArgs);
        getContext().getContentResolver().notifyChange(uri,null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String where, String[] whereArgs) {
        SQLiteOpenHelper helper;
        String tableName;
        helper = new PuzzleDbHelper(getContext());
        switch(sUriMatcher.match(uri)){
            case URI_ID_PUZZLES:
            case URI_ID_PUZZLE:
                tableName = PuzzleTable.TABLE_NAME;
                break;
            case URI_ID_ALBUMS:
            case URI_ID_ALBUM:
                //helper = new AlbumDbHelper(getContext());
                tableName = AlbumsTable.TABLE_NAME;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: "+uri);
        }
        SQLiteDatabase db = helper.getWritableDatabase();
        int count = db.update(tableName,contentValues,where,whereArgs);
        getContext().getContentResolver().notifyChange(uri,null);
        return count;
    }
}
