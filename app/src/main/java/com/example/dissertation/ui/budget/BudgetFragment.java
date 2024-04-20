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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.dissertation.DatabaseHelper;
import com.example.dissertation.R;

import java.util.ArrayList;

public class BudgetFragment extends Fragment {

    private EditText editTextType, editTextDescription, editTextQuantity, editTextSellingPrice;
    private ListView listViewBudgets;
    private ArrayAdapter<String> adapter;
    private ArrayList<Integer> budgetIds; // This will store the ids of budget entries
    private DatabaseHelper dbHelper;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget, container, false);

        editTextType = view.findViewById(R.id.editTextBudgetType);
        editTextDescription = view.findViewById(R.id.editTextBudgetDescription);
        editTextQuantity = view.findViewById(R.id.editTextBudgetQuantity);
        editTextSellingPrice = view.findViewById(R.id.editTextBudgetSellingPrice);
        listViewBudgets = view.findViewById(R.id.listViewBudgets);
        Button buttonSave = view.findViewById(R.id.buttonSaveBudget);

        dbHelper = new DatabaseHelper(getActivity());
        budgetIds = new ArrayList<>();
        adapter = new ArrayAdapter<>(requireActivity(), android.R.layout.simple_list_item_1, new ArrayList<String>());
        listViewBudgets.setAdapter(adapter);

        buttonSave.setOnClickListener(v -> saveBudget());

        listViewBudgets.setOnItemLongClickListener((parent, view12, position, id) -> {
            int budgetId = budgetIds.get(position);  // Assuming budgetIds is populated correctly in loadBudgets
            confirmDeletion(budgetId);
            return true;
        });

        loadBudgets(); // Load existing budgets to ListView
        return view;
    }

    private void saveBudget() {
        try {
            String type = editTextType.getText().toString();
            String description = editTextDescription.getText().toString();
            int quantity = Integer.parseInt(editTextQuantity.getText().toString());
            double sellingPrice = Double.parseDouble(editTextSellingPrice.getText().toString());

            dbHelper.insertBudget(type, description, quantity, sellingPrice);
            Toast.makeText(getActivity(), "Budget Saved", Toast.LENGTH_SHORT).show();
            loadBudgets(); // Reload the budgets
        } catch (NumberFormatException e) {
            Toast.makeText(getActivity(), "Please enter valid numbers for quantity and price.", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("Range")
    private void loadBudgets() {
        try (Cursor cursor = dbHelper.readBudgetData()) {
            ArrayList<String> listItems = new ArrayList<>();
            budgetIds.clear();
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndex("budgetID"));
                int quantity = cursor.getInt(cursor.getColumnIndex("quantity"));
                double sellingPrice = cursor.getDouble(cursor.getColumnIndex("sellingPrice"));
                double total = cursor.getDouble(cursor.getColumnIndex("total"));
                String item = "Type: " + cursor.getString(cursor.getColumnIndex("type")) +
                        ", Desc: " + cursor.getString(cursor.getColumnIndex("description")) +
                        ", Qty: " + quantity +
                        ", Price: $" + sellingPrice +
                        ", Total: $" + total;
                listItems.add(item);
                budgetIds.add(id);
            }
            adapter.clear();
            adapter.addAll(listItems);
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Error loading budgets: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void deleteBudget(int budgetId) {
        dbHelper.deleteBudgetData(budgetId);
        Toast.makeText(getActivity(), "Budget deleted", Toast.LENGTH_SHORT).show();
        loadBudgets();  // Reload the budgets
    }

    private void confirmDeletion(int budgetId) {
        new AlertDialog.Builder(requireActivity())
                .setTitle("Delete Budget")
                .setMessage("Are you sure you want to delete this budget entry?")
                .setPositiveButton("Delete", (dialog, which) -> deleteBudget(budgetId))
                .setNegativeButton("Cancel", null)
                .show();
    }
}
