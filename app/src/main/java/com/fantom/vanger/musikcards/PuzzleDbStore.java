package com.fantom.vanger.musikcards;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.fantom.vanger.musikcards.db.PuzzleDbHelper;
import com.fantom.vanger.musikcards.db.PuzzleCursorWrapper;
import com.fantom.vanger.musikcards.db.PuzzleDbSchema.PuzzleTable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vanger on 11/11/16.
 */

public class PuzzleDbStore {
    private static PuzzleDbStore sPuzzleDbStore;
    private List<Puzzle> puzzleList;
    private Context ctx;
    private SQLiteDatabase dataBase;


    public static PuzzleDbStore newInstance(Context ctx){
        if (sPuzzleDbStore == null) {
            sPuzzleDbStore = new PuzzleDbStore(ctx);
        }
        return sPuzzleDbStore;
    }

    private PuzzleDbStore(Context context){
        ctx=context;
        dataBase = new PuzzleDbHelper(ctx).getWritableDatabase();
        puzzleList = new ArrayList<>();
    }

    public void addPuzzle(Puzzle p){
        ContentValues cv = getContentValues(p);
        dataBase.insert(PuzzleTable.TABLE_NAME,null,cv);
    }

    public void updatePuzzle(Puzzle p){
        ContentValues cv = getContentValues(p);
        dataBase.update(PuzzleTable.TABLE_NAME,cv,
                "_id=?", new String[] {Integer.toString(p.getId())});
    }

    public List<Puzzle> getPuzzles(){
        List<Puzzle> puzzles = new ArrayList<>();
        PuzzleCursorWrapper cursor = queryPuzzles(null,null);

        try{
            cursor.moveToFirst();
            while(!cursor.isAfterLast()){
                puzzles.add(cursor.getPuzzle());
                cursor.moveToNext();
            }
        } finally { cursor.close(); }

        return puzzles;
    }

    public Puzzle getPuzzle(int id){
        PuzzleCursorWrapper cursor = queryPuzzles("_id=?",new String[] {Integer.toString(id)});
        try{
            if(cursor.getCount()==0) return null;
            cursor.moveToFirst();
            return cursor.getPuzzle();
        } finally {cursor.close();}
    }

    private PuzzleCursorWrapper queryPuzzles(String where, String[] whereArgs){
        Cursor cursor = dataBase.query(
                PuzzleTable.TABLE_NAME,
                null,               // all columns
                where,whereArgs,
                null,               // groupBy
                null,               // having
                null                // orderBy
        );
        return new PuzzleCursorWrapper(cursor);
    }

    private static ContentValues getContentValues(Puzzle p){
        ContentValues cv = new ContentValues();
        cv.put(PuzzleTable.TITLE,p.getTitle());
        cv.put(PuzzleTable.PATH,p.getSoundPath());
        cv.put(PuzzleTable.PATH_TYPE,p.getPathType());
        cv.put(PuzzleTable.ALBUM,p.getAlbumId());
        cv.put(PuzzleTable.LINK,p.getLinkId());
        return cv;
    }
}
