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

public class BudgetFragment extends Fragment {

    private EditText editTextType, editTextDescription, editTextQuantity, editTextSellingPrice;
    private ListView listViewBudgets;
    private ArrayAdapter<String> adapter;
    private ArrayList<Integer> budgetIds;
    private DatabaseHelper dbHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget, container, false);

        editTextType = view.findViewById(R.id.editTextBudgetType);
        editTextDescription = view.findViewById(R.id.editTextBudgetDescription);
        editTextQuantity = view.findViewById(R.id.editTextBudgetQuantity);
        editTextSellingPrice = view.findViewById(R.id.editTextBudgetSellingPrice);
        listViewBudgets = view.findViewById(R.id.listViewBudgets);
        Button buttonSave = view.findViewById(R.id.buttonSaveBudget);

        dbHelper = new DatabaseHelper(getActivity());
        budgetIds = new ArrayList<>();
        adapter = new ArrayAdapter<>(requireActivity(), android.R.layout.simple_list_item_1, new ArrayList<>());
        listViewBudgets.setAdapter(adapter);

        loadBudgets();  // Load data early to ensure UI is populated before any user interaction

        buttonSave.setOnClickListener(v -> saveBudget());

        listViewBudgets.setOnItemLongClickListener((parent, view12, position, id) -> {
            int budgetId = budgetIds.get(position);
            confirmDeletion(budgetId);
            return true;
        });

        listViewBudgets.setOnItemClickListener((parent, view1, position, id) -> {
            int budgetId = budgetIds.get(position);
            String budgetDetails = adapter.getItem(position);

            String[] parts = budgetDetails.split(", ");

            //Extract only substrings from each segment AFTER colon characters
            String type = parts[0].substring(parts[0].indexOf(": ") + 2);
            String description = parts[1].substring(parts[1].indexOf(": ") + 2);
            String quantityStr = parts[2].substring(parts[2].indexOf(": ") + 2);
            String sellingPriceStr = parts[3].substring(parts[3].indexOf(": $") + 3);

            int quantity = Integer.parseInt(quantityStr);
            double sellingPrice = Double.parseDouble(sellingPriceStr);

            showUpdateDialog(budgetId, type, description, quantity, sellingPrice);
        });

        return view;
    }

    private void saveBudget() {
        // Make all fields into strings
        String type = editTextType.getText().toString();
        String description = editTextDescription.getText().toString();
        String quantityStr = editTextQuantity.getText().toString();
        String sellingPriceStr = editTextSellingPrice.getText().toString();

        // Check if all fields are filled when saveBudget() is called, if they aren't, carry on, otherwise show a toast pop-up alerting the user
        if (!type.isEmpty() && !description.isEmpty() && !quantityStr.isEmpty() && !sellingPriceStr.isEmpty()) {
            try {
                // Change the datatype by parsing the string argument into an int/double
                int quantity = Integer.parseInt(quantityStr);
                double sellingPrice = Double.parseDouble(sellingPriceStr);

                // Insert the values into the database table and show a toast pop-up alerting the user
                dbHelper.insertBudget(type, description, quantity, sellingPrice);
                Toast.makeText(getActivity(), "Budget Saved", Toast.LENGTH_SHORT).show();

                // Clear all input fields after saving
                editTextType.setText("");
                editTextDescription.setText("");
                editTextQuantity.setText("");
                editTextSellingPrice.setText("");

                loadBudgets(); // Reload the list of budgets

            } catch (NumberFormatException e) {
                Toast.makeText(getActivity(), "Invalid number format. Please check your inputs.", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(getActivity(), "Failed to save budget: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getActivity(), "All fields are required", Toast.LENGTH_SHORT).show();
        }
    }


    @SuppressLint("Range")
    private void loadBudgets() {
        try (Cursor cursor = dbHelper.readBudget()) {
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
        try {
            dbHelper.deleteBudget(budgetId);
            Toast.makeText(getActivity(), "Budget deleted", Toast.LENGTH_SHORT).show();
            loadBudgets(); // Reload the budgets to reflect the deletion
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Failed to delete budget: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void confirmDeletion(int budgetId) {
        new AlertDialog.Builder(requireActivity())
                .setTitle("Delete Budget")
                .setMessage("Are you sure you want to delete this budget entry?")
                .setPositiveButton("Delete", (dialog, which) -> deleteBudget(budgetId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showUpdateDialog(int budgetId, String currentType, String currentDescription, int currentQuantity, double currentSellingPrice) {
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

        EditText editTextSellingPrice = new EditText(getContext());
        editTextSellingPrice.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editTextSellingPrice.setText(String.valueOf(currentSellingPrice));

        // Layout to contain the EditText fields
        LinearLayout layout = new LinearLayout(getContext());

        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 10, 40, 10); // Set padding around the layout

        layout.addView(editTextType);
        layout.addView(editTextDescription);
        layout.addView(editTextQuantity);
        layout.addView(editTextSellingPrice);

        builder.setView(layout); // Set the view to AlertDialog

        // Set up the buttons
        builder.setPositiveButton("Update", (dialog, which) -> {
            try {
                // Retrieve input and parse numbers
                String type = editTextType.getText().toString();
                String description = editTextDescription.getText().toString();
                int quantity = Integer.parseInt(editTextQuantity.getText().toString());
                double sellingPrice = Double.parseDouble(editTextSellingPrice.getText().toString());

                // Update budget in the database
                updateBudget(budgetId, type, description, quantity, sellingPrice);
            } catch (NumberFormatException e) {
                Toast.makeText(getActivity(), "Please enter valid numbers for quantity and price.", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(getActivity(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateBudget(int budgetId, String type, String description, int quantity, double sellingPrice) {
        try {
            double total = quantity * sellingPrice;

            dbHelper.updateBudget(budgetId, type, description, quantity, sellingPrice, total);
            Toast.makeText(getActivity(), "Budget Updated", Toast.LENGTH_SHORT).show();

            loadBudgets(); // Reload the list of budgets
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

}
