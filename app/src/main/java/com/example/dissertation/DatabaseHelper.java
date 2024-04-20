package com.example.dissertation;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "myDatabase.db";
    private static final int DATABASE_VERSION = 3;

    // Creating the tables
    private static final String CREATE_NOTES_TABLE = "CREATE TABLE noteTable (noteID INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, content TEXT)";

    private static final String CREATE_BUDGET_TABLE = "CREATE TABLE budgetTable (budgetID INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT, description TEXT, quantity INTEGER, sellingPrice REAL, total REAL)";

    private static final String CREATE_EXPENSE_TABLE = "CREATE TABLE expenseTable (expenseID INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT, description TEXT, quantity INTEGER, expenseValue REAL, total REAL)";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create your tables here
        db.execSQL(CREATE_NOTES_TABLE);
        db.execSQL(CREATE_BUDGET_TABLE);
        db.execSQL(CREATE_EXPENSE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            db.execSQL("DROP TABLE IF EXISTS noteTable");
            db.execSQL("DROP TABLE IF EXISTS budgetTable");
            db.execSQL("DROP TABLE IF EXISTS expenseTable");
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

    public void insertBudget(String type, String description, int quantity, double sellingPrice) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("type", type);
        values.put("description", description);
        values.put("quantity", quantity);
        values.put("sellingPrice", sellingPrice);
        values.put("total", quantity * sellingPrice);  // Calculating total
        db.insert("budgetTable", null, values);
    }

    public void insertExpense(String type, String description, int quantity, double expenseValue) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("type", type);
        values.put("description", description);
        values.put("quantity", quantity);
        values.put("expenseValue", expenseValue);
        values.put("total", quantity * expenseValue);  // Calculating total
        db.insert("expenseTable", null, values);
    }

    // Read data
    public Cursor readNoteData() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM noteTable";
        return db.rawQuery(query, null);
    }

    public Cursor readBudgetData() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM budgetTable";
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

    public void deleteBudgetData(int budgetID) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("budgetTable", "budgetID=?", new String[]{String.valueOf(budgetID)});
    }
}