package com.example.dissertation;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "myDatabase.db";
    private static final int DATABASE_VERSION = 15;

    // Creating the tables
    private static final String CREATE_NOTES_TABLE = "CREATE TABLE noteTable (noteID INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, content TEXT, creationDate INTEGER, editDate INTEGER, deletedDate INTEGER)";

    private static final String CREATE_BUDGET_TABLE = "CREATE TABLE budgetTable (budgetID INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT, description TEXT, quantity INTEGER, sellingPrice REAL, total REAL, budgetDate INTEGER)";

    private static final String CREATE_TASK_TABLE = "CREATE TABLE taskTable (taskID INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, description TEXT, type TEXT, taskDate INTEGER, startTime INTEGER, endTime INTEGER, priority INTEGER, isCompleted INTEGER DEFAULT 0)";

    private static final String CREATE_INVENTORY_TABLE = "CREATE TABLE inventoryTable (itemID INTEGER PRIMARY KEY AUTOINCREMENT, itemName TEXT, itemQuantity INTEGER, itemType TEXT, itemSubType TEXT, itemDescription TEXT)";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create your tables here
        db.execSQL(CREATE_NOTES_TABLE);
        db.execSQL(CREATE_BUDGET_TABLE);
        db.execSQL(CREATE_TASK_TABLE);
        db.execSQL(CREATE_INVENTORY_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 15) {
            db.execSQL("DROP TABLE IF EXISTS noteTable");
            db.execSQL("DROP TABLE IF EXISTS budgetTable");
            db.execSQL("DROP TABLE IF EXISTS taskTable");
            db.execSQL("DROP TABLE IF EXISTS inventoryTable");
            onCreate(db);
        }
    }








    // Insert data methods for all tables and their respective values
    public void insertNote(String title, String content) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("content", content);

        long currentTime = System.currentTimeMillis() / 1000; // Current time in seconds
        values.put("creationDate", currentTime);
        values.put("editDate", currentTime);

        db.insert("noteTable", null, values);
    }

    public void insertBudget(String type, String description, int quantity, double sellingPrice, long budgetDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("type", type);
        values.put("description", description);
        values.put("quantity", quantity);
        values.put("sellingPrice", sellingPrice);
        values.put("total", quantity * sellingPrice);  // Calculating total
        values.put("budgetDate", budgetDate);
        db.insert("budgetTable", null, values);
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

    public void insertItem(String itemName, int itemQuantity, String itemType, String itemSubType, String itemDescription) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("itemName", itemName);
        values.put("itemQuantity", itemQuantity);
        values.put("itemType", itemType);
        values.put("itemSubType", itemSubType);
        values.put("itemDescription", itemDescription);
        db.insert("inventoryTable", null, values);
    }






















    // Read data methods
    public Cursor readNote() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM noteTable WHERE deletedDate IS NULL";
        return db.rawQuery(query, null);
    }

    public Cursor readNoteById(int noteId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM notes WHERE noteID = ?";
        return db.rawQuery(query, new String[]{String.valueOf(noteId)});
    }

    public Cursor readDeletedNotes() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM noteTable WHERE deletedDate IS NOT NULL";
        return db.rawQuery(query, null);
    }

    public Cursor readBudget() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM budgetTable";
        return db.rawQuery(query, null);
    }

    public Cursor readBudgetById(int budgetId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM budgets WHERE budgetID = ?";
        return db.rawQuery(query, new String[]{String.valueOf(budgetId)});
    }

    // Extra parameters necessary for my solution, find the tasks on dates starting from 00:00 to 23:59
    public Cursor readTask(long dayStart, long dayEnd) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM taskTable WHERE taskDate BETWEEN ? AND ?";
        return db.rawQuery(query, new String[]{String.valueOf(dayStart), String.valueOf(dayEnd)});
    }

    public Cursor readChart(String tableName, String column) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + column + " FROM " + tableName;
        return db.rawQuery(query, null);
    }

    public Cursor readItem() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM inventoryTable";
        return db.rawQuery(query, null);
    }

    public Cursor readItemByID(int itemID) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM items WHERE itemID = ?", new String[]{String.valueOf(itemID)});
    }

    public Cursor readFilteredItems(String type, String subtype) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query;
        String[] selectionArgs;

        if (subtype == null) {
            query = "SELECT * FROM inventoryTable WHERE itemType=?";
            selectionArgs = new String[]{type};
        } else {
            query = "SELECT * FROM inventoryTable WHERE itemType=? AND itemSubType=?";
            selectionArgs = new String[]{type, subtype};
        }

        return db.rawQuery(query, selectionArgs);
    }























    // Update data methods
    public void updateNote(int id, String title, String content) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("content", content);

        long currentTime = System.currentTimeMillis() / 1000; // Current time in seconds
        values.put("editDate", currentTime);

        db.update("noteTable", values, "noteID=?", new String[]{String.valueOf(id)});
    }

    public void updateBudget(int budgetID, String type, String description, int quantity, double sellingPrice, double total, long budgetDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("type", type);
        values.put("description", description);
        values.put("quantity", quantity);
        values.put("sellingPrice", sellingPrice);
        values.put("total", total);
        values.put("budgetDate", budgetDate);
        db.update("budgetTable", values, "budgetID=?", new String[]{String.valueOf(budgetID)});
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

    public void updateItem(int itemID, String itemName, int itemQuantity, String itemType, String itemSubType, String itemDescription) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("itemName", itemName);
        values.put("itemQuantity", itemQuantity);
        values.put("itemType", itemType);
        values.put("itemSubType", itemSubType);
        values.put("itemDescription", itemDescription);
        db.update("inventoryTable", values, "itemID=?", new String[]{String.valueOf(itemID)});
    }




















    // Delete data methods
    public void deleteNote(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        long deletedDate = System.currentTimeMillis() / 1000; // Current time in seconds
        values.put("deletedDate", deletedDate);
        db.update("noteTable", values, "noteID=?", new String[]{String.valueOf(id)});
    }

    public void deleteNotePermanently(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("noteTable", "noteID=?", new String[]{String.valueOf(id)});
    }

    public void restoreNote(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("deletedDate", (Integer) null);
        db.update("noteTable", values, "noteID=?", new String[]{String.valueOf(id)});
    }

    public void cleanUpTrash() {
        SQLiteDatabase db = this.getWritableDatabase();
        long currentTime = System.currentTimeMillis() / 1000; // Current time in seconds
        long thirtyDaysAgo = currentTime - (30 * 24 * 60 * 60); // 30 days ago in seconds
        db.delete("noteTable", "deletedDate < ?", new String[]{String.valueOf(thirtyDaysAgo)});
    }

    public void deleteBudget(int budgetID) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("budgetTable", "budgetID=?", new String[]{String.valueOf(budgetID)});
    }

    public void deleteTask(int taskID) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("taskTable", "taskID=?", new String[]{String.valueOf(taskID)});
    }

    public void deleteItem(int itemID) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("inventoryTable", "itemID=?", new String[]{String.valueOf(itemID)});
    }
}