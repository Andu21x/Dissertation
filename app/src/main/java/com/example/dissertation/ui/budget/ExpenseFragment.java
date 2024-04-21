package com.example.dissertation.ui.budget;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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

        listViewExpenses.setOnItemClickListener((parent, view1, position, id) -> {
            int expenseId = expenseIds.get(position);
            String expenseDetails = adapter.getItem(position);

            // Parsing the expense details
            String[] parts = expenseDetails.split(", ");
            String type = parts[0].substring(parts[0].indexOf(": ") + 2);
            String description = parts[1].substring(parts[1].indexOf(": ") + 2);
            int quantity = Integer.parseInt(parts[2].substring(parts[2].indexOf(": ") + 2));
            double expenseValue = Double.parseDouble(parts[3].substring(parts[3].indexOf(": $") + 3));

            showUpdateDialog(expenseId, type, description, quantity, expenseValue);
        });

        loadExpenses(); // Reload the list of expenses
        return view;
    }

    private void saveExpense() {
        // Make all fields into strings
        String type = editTextType.getText().toString();
        String description = editTextDescription.getText().toString();
        String quantityStr = editTextQuantity.getText().toString();
        String expenseValueStr = editTextExpenseValue.getText().toString();

        // Check if all fields are filled when saveExpense() is called, if they aren't, carry on, otherwise show a toast pop-up alerting the user
        if (!type.isEmpty() && !description.isEmpty() && !quantityStr.isEmpty() && !expenseValueStr.isEmpty()) {
            try {
                // Change the datatype by parsing the string argument into an int/double
                int quantity = Integer.parseInt(quantityStr);
                double expenseValue = Double.parseDouble(expenseValueStr);

                // Insert the values into the database table and show a toast pop-up alerting the user
                dbHelper.insertExpense(type, description, quantity, expenseValue);
                Toast.makeText(getActivity(), "Expense Saved", Toast.LENGTH_SHORT).show();

                // Clear all input fields after saving
                editTextType.setText("");
                editTextDescription.setText("");
                editTextQuantity.setText("");
                editTextExpenseValue.setText("");

                loadExpenses(); // Reload the list of expenses

            } catch (NumberFormatException e) {
                // Handle number format exception if parsing fails
                Toast.makeText(getActivity(), "Invalid number format. Please check your inputs.", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                // Handle other exceptions that might occur during database operations
                Toast.makeText(getActivity(), "Failed to save expense: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getActivity(), "All fields are required", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("Range")
    private void loadExpenses() {
        try (Cursor cursor = dbHelper.readExpense()) {
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
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Error loading expenses: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void deleteExpense(int expenseId) {
        try {
            dbHelper.deleteExpense(expenseId);
            Toast.makeText(getActivity(), "Expense deleted", Toast.LENGTH_SHORT).show();

            loadExpenses();  // Reload the expenses
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Failed to delete expense: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void confirmDeletion(int expenseId) {
        new AlertDialog.Builder(requireActivity())
                .setTitle("Delete Expense")
                .setMessage("Are you sure you want to delete this expense entry?")
                .setPositiveButton("Delete", (dialog, which) -> deleteExpense(expenseId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showUpdateDialog(int expenseId, String currentType, String currentDescription, int currentQuantity, double currentExpenseValue) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());

        // Set up the input fields
        EditText editTextType = new EditText(getContext());
        editTextType.setInputType(InputType.TYPE_CLASS_TEXT);
        editTextType.setText(currentType);

        EditText editTextDescription = new EditText(getContext());
        editTextDescription.setInputType(InputType.TYPE_CLASS_TEXT);
        editTextDescription.setText(currentDescription);

        EditText editTextQuantity = new EditText(getContext());
        editTextQuantity.setInputType(InputType.TYPE_CLASS_NUMBER);
        editTextQuantity.setText(String.valueOf(currentQuantity));

        EditText editTextExpenseValue = new EditText(getContext());
        editTextExpenseValue.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editTextExpenseValue.setText(String.valueOf(currentExpenseValue));

        // Layout to contain the EditText fields
        LinearLayout layout = new LinearLayout(getContext());

        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 10, 40, 10); // Set padding around the layout

        layout.addView(editTextType);
        layout.addView(editTextDescription);
        layout.addView(editTextQuantity);
        layout.addView(editTextExpenseValue);

        builder.setView(layout); // Set the view to AlertDialog

        // Set up the buttons
        builder.setPositiveButton("Update", (dialog, which) -> {
            try {
                String type = editTextType.getText().toString();
                String description = editTextDescription.getText().toString();
                int quantity = Integer.parseInt(editTextQuantity.getText().toString()); // Convert Data Type to be compatible with database
                double expenseValue = Double.parseDouble(editTextExpenseValue.getText().toString()); // Same here
                updateExpense(expenseId, type, description, quantity, expenseValue);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }


    private void updateExpense(int expenseId, String type, String description, int quantity, double expenseValue) {
        try {
            double total = quantity * expenseValue;

            dbHelper.updateExpense(expenseId, type, description, quantity, expenseValue, total);
            Toast.makeText(getActivity(), "Expense Updated", Toast.LENGTH_SHORT).show();

            loadExpenses(); // Reload the list of expenses
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show(); // Error handling
        }
    }
}


