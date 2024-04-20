package com.example.dissertation;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "myDatabase.db";
    private static final int DATABASE_VERSION = 2;

    // Creating the table
    private static final String CREATE_NOTES_TABLE = "CREATE TABLE noteTable (noteID INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, content TEXT)";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create your tables here
        db.execSQL(CREATE_NOTES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("DROP TABLE IF EXISTS noteTable");
            onCreate(db);
        }
    }

    // Insert data
    public void insertNoteData(String title, String content) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("title", title);
        contentValues.put("content", content);
        db.insert("noteTable", null, contentValues);
    }

    // Read data
    public Cursor readNoteData() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM noteTable";
        return db.rawQuery(query, null);
    }

    // Update data
    public void updateNoteData(int id, String title, String content) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("title", title);
        contentValues.put("content", content);
        db.update("noteTable", contentValues, "noteID=?", new String[]{String.valueOf(id)});
    }

    // Delete data
    public void deleteNoteData(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("noteTable", "noteID=?", new String[]{String.valueOf(id)});
    }
}