package com.example.dissertation;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "myDatabase.db";
    private static final int DATABASE_VERSION = 6;

    // Creating the tables
    private static final String CREATE_NOTES_TABLE = "CREATE TABLE noteTable (noteID INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, content TEXT)";

    private static final String CREATE_BUDGET_TABLE = "CREATE TABLE budgetTable (budgetID INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT, description TEXT, quantity INTEGER, sellingPrice REAL, total REAL)";

    private static final String CREATE_EXPENSE_TABLE = "CREATE TABLE expenseTable (expenseID INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT, description TEXT, quantity INTEGER, expenseValue REAL, total REAL)";

    private static final String CREATE_TASK_TABLE = "CREATE TABLE taskTable (taskID INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, description TEXT, type TEXT, taskDate INTEGER, startTime INTEGER, endTime INTEGER, priority INTEGER, isCompleted INTEGER DEFAULT 0)";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create your tables here
        db.execSQL(CREATE_NOTES_TABLE);
        db.execSQL(CREATE_BUDGET_TABLE);
        db.execSQL(CREATE_EXPENSE_TABLE);
        db.execSQL(CREATE_TASK_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 6) {
            db.execSQL("DROP TABLE IF EXISTS noteTable");
            db.execSQL("DROP TABLE IF EXISTS budgetTable");
            db.execSQL("DROP TABLE IF EXISTS expenseTable");
            db.execSQL("DROP TABLE IF EXISTS taskTable");
            onCreate(db);
        }
    }








    // Insert data methods for all tables and their respective values
    public void insertNote(String title, String content) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("content", content);
        db.insert("noteTable", null, values);
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

    public void insertTask(String title, String description, String type, long taskDate, long startTime, long endTime, int priority, int isCompleted) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("description", description);
        values.put("type", type);
        values.put("taskDate", taskDate);
        values.put("startTime", startTime);
        values.put("endTime", endTime);
        values.put("priority", priority);
        values.put("isCompleted", isCompleted);
        db.insert("taskTable", null, values);
    }






















    // Read data methods
    public Cursor readNote() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM noteTable";
        return db.rawQuery(query, null);
    }

    public Cursor readBudget() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM budgetTable";
        return db.rawQuery(query, null);
    }

    public Cursor readExpense() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM expenseTable";
        return db.rawQuery(query, null);
    }

    // Extra parameters necessary for my solution, find the tasks on dates starting from 00:00 to 23:59
    public Cursor readTask(long dayStart, long dayEnd) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM taskTable WHERE taskDate BETWEEN ? AND ?";
        return db.rawQuery(query, new String[]{String.valueOf(dayStart), String.valueOf(dayEnd)});
    }






















    // Update data methods
    public void updateNote(int id, String title, String content) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("content", content);
        db.update("noteTable", values, "noteID=?", new String[]{String.valueOf(id)});
    }

    public void updateBudget(int budgetID, String type, String description, int quantity, double sellingPrice, double total) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("type", type);
        values.put("description", description);
        values.put("quantity", quantity);
        values.put("sellingPrice", sellingPrice);
        values.put("total", total);
        db.update("budgetTable", values, "budgetID=?", new String[]{String.valueOf(budgetID)});
    }

    public void updateExpense(int expenseID, String type, String description, int quantity, double expenseValue, double total) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("type", type);
        values.put("description", description);
        values.put("quantity", quantity);
        values.put("expenseValue", expenseValue);
        values.put("total", total);
        db.update("expenseTable", values, "expenseID=?", new String[]{String.valueOf(expenseID)});
    }

    public void updateTask(int taskID, String title, String description, String type, long taskDate, long startTime, long endTime, int priority, int isCompleted) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("description", description);
        values.put("type", type);
        values.put("taskDate", taskDate);
        values.put("startTime", startTime);
        values.put("endTime", endTime);
        values.put("priority", priority);
        values.put("isCompleted", isCompleted);
        db.update("taskTable", values, "taskID=?", new String[]{String.valueOf(taskID)});
    }




















    // Delete data methods
    public void deleteNote(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("noteTable", "noteID=?", new String[]{String.valueOf(id)});
    }

    public void deleteBudget(int budgetID) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("budgetTable", "budgetID=?", new String[]{String.valueOf(budgetID)});
    }

    public void deleteExpense(int expenseID) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("expenseTable", "expenseID=?", new String[]{String.valueOf(expenseID)});
    }

    public void deleteTask(int taskID) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("taskTable", "taskID=?", new String[]{String.valueOf(taskID)});
    }
}