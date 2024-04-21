package com.example.dissertation.ui.budget;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.dissertation.DatabaseHelper;
import com.example.dissertation.R;

import java.util.ArrayList;


public class ExpenseFragment extends Fragment {

    private EditText editTextType, editTextDescription, editTextQuantity, editTextExpenseValue;
    private ListView listViewExpenses;
    private ArrayAdapter<String> adapter;
    private ArrayList<Integer> expenseIds;
    private DatabaseHelper dbHelper;

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expense, container, false);

        editTextType = view.findViewById(R.id.editTextExpenseType);
        editTextDescription = view.findViewById(R.id.editTextExpenseDescription);
        editTextQuantity = view.findViewById(R.id.editTextExpenseQuantity);
        editTextExpenseValue = view.findViewById(R.id.editTextExpenseValue);
        listViewExpenses = view.findViewById(R.id.listViewExpenses);
        Button buttonSave = view.findViewById(R.id.buttonSaveExpense);

        dbHelper = new DatabaseHelper(getActivity());
        expenseIds = new ArrayList<>();
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, new ArrayList<String>());
        listViewExpenses.setAdapter(adapter);

        buttonSave.setOnClickListener(v -> saveExpense());

        listViewExpenses.setOnItemLongClickListener((parent, view12, position, id) -> {
            int expenseId = expenseIds.get(position);  // Retrieve the ID of the expense to delete
            confirmDeletion(expenseId);
            return true;
        });

        loadExpenses(); // Load existing expenses to ListView
        return view;
    }

    private void saveExpense() {
        String type = editTextType.getText().toString();
        String description = editTextDescription.getText().toString();
        int quantity = Integer.parseInt(editTextQuantity.getText().toString());
        double expenseValue = Double.parseDouble(editTextExpenseValue.getText().toString());

        dbHelper.insertExpense(type, description, quantity, expenseValue);
        Toast.makeText(getActivity(), "Expense Saved", Toast.LENGTH_SHORT).show();
        loadExpenses();
    }

    @SuppressLint("Range")
    private void loadExpenses() {
        Cursor cursor = dbHelper.readExpenseData();
        ArrayList<String> listItems = new ArrayList<>();
        expenseIds.clear();
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndex("expenseID"));
            int quantity = cursor.getInt(cursor.getColumnIndex("quantity"));
            double expenseValue = cursor.getDouble(cursor.getColumnIndex("expenseValue"));
            double total = cursor.getDouble(cursor.getColumnIndex("total"));
            String item = "Type: " + cursor.getString(cursor.getColumnIndex("type")) +
                    ", Desc: " + cursor.getString(cursor.getColumnIndex("description")) +
                    ", Qty: " + quantity +
                    ", Expense: $" + expenseValue +
                    ", Total: $" + total;
            listItems.add(item);
            expenseIds.add(id);
        }
        adapter.clear();
        adapter.addAll(listItems);
        adapter.notifyDataSetChanged();
        cursor.close();
    }

    private void deleteExpense(int expenseId) {
        dbHelper.deleteExpenseData(expenseId);
        Toast.makeText(getActivity(), "Expense deleted", Toast.LENGTH_SHORT).show();
        loadExpenses();  // Reload the expenses
    }

    private void confirmDeletion(int expenseId) {
        new AlertDialog.Builder(requireActivity())
                .setTitle("Delete Expense")
                .setMessage("Are you sure you want to delete this expense entry?")
                .setPositiveButton("Delete", (dialog, which) -> deleteExpense(expenseId))
                .setNegativeButton("Cancel", null)
                .show();
    }
}


