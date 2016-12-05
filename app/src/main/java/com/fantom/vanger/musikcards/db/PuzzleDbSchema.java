package com.fantom.vanger.musikcards.db;

import android.net.Uri;

/**
 * Created by vanger on 11/11/16.
 */

public class PuzzleDbSchema {

    public static final String AUTHORITY = "com.fantom.vanger.musikcards.ProviderPuzzle";
    public static final String DB_NAME = "puzzleBase.db";
    public static final int DB_VERSION = 1;

    public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/");
    public static final String CONTENT_PUZZLES = "vnd.android.cursor.dir/vnd.musikcards.puzzle";
    public static final String CONTENT_PUZZLE_ITEM = "vnd.android.cursor.item/vnd.musikcards.puzzle";
    public static final String CONTENT_ALBUMS = "vnd.android.cursor.dir/vnd.musikcards.album";
    public static final String CONTENT_ALBUM_ITEM = "vnd.android.cursor.item/vnd.musikcards.album";
    public static final String ORDER_BY = "_id DESC";

    public static final class PuzzleTable {
        public static final String TABLE_NAME = "puzzles";

        public static final String ID = "_id";
        public static final String TITLE = "title";
        public static final String PATH = "path";
        public static final String PATH_TYPE = "path_type";
        public static final String ALBUM = "album";
        public static final String LINK = "link";
    }

    public static final class AlbumsTable {
        public static final String TABLE_NAME = "albums";

        public static final String ID="_id";
        public static final String TITLE = "title";
        public static final String ABOUT = "about";
        public static final String AUTHOR = "author";
        public static final String IMAGE_URI = "image_uri";
    }
}
