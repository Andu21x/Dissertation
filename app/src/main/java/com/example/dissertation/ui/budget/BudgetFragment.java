package com.example.dissertation.ui.budget;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.dissertation.DatabaseHelper;
import com.example.dissertation.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class BudgetFragment extends Fragment {

    private Spinner spinnerType;
    private EditText editTextType, editTextDescription, editTextQuantity, editTextSellingPrice, editTextBudgetDate;
    private TextView textViewAggregateTotal;
    private ArrayAdapter<String> adapter;
    private ArrayList<Integer> budgetIds;
    private DatabaseHelper dbHelper;
    private Calendar calendar;

    @SuppressLint("Range")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget, container, false);

        textViewAggregateTotal = view.findViewById(R.id.textViewAggregateTotal);
        spinnerType = view.findViewById(R.id.spinnerBudgetType);
        editTextType = view.findViewById(R.id.editTextBudgetType);
        editTextDescription = view.findViewById(R.id.editTextBudgetDescription);
        editTextQuantity = view.findViewById(R.id.editTextBudgetQuantity);
        editTextSellingPrice = view.findViewById(R.id.editTextBudgetSellingPrice);
        editTextBudgetDate = view.findViewById(R.id.editTextBudgetDate);
        ListView listViewBudgets = view.findViewById(R.id.listViewBudgets);
        Button buttonSave = view.findViewById(R.id.buttonSaveBudget);

        dbHelper = new DatabaseHelper(getActivity());
        budgetIds = new ArrayList<>();
        adapter = new ArrayAdapter<>(requireActivity(), android.R.layout.simple_list_item_1, new ArrayList<>());
        listViewBudgets.setAdapter(adapter);

        calendar = Calendar.getInstance();

        updateLabel(); // Set current date as default in editTextBudgetDate

        loadBudgets(); // Load data early to ensure UI is populated before any user interaction

        buttonSave.setOnClickListener(v -> saveBudget());

        listViewBudgets.setOnItemLongClickListener((parent, view12, position, id) -> {
            confirmDeletion(budgetIds.get(position)); // Retrieve the ID of the budget to delete
            return true;
        });

        listViewBudgets.setOnItemClickListener((parent, view1, position, id) -> {
            int budgetId = budgetIds.get(position);

            try (Cursor cursor = dbHelper.readBudgetById(budgetId)) {
                if (cursor.moveToFirst()) {
                    String type = cursor.getString(cursor.getColumnIndex("type"));
                    String description = cursor.getString(cursor.getColumnIndex("description"));
                    int quantity = cursor.getInt(cursor.getColumnIndex("quantity"));
                    double sellingPrice = cursor.getDouble(cursor.getColumnIndex("sellingPrice"));

                    showUpdateDialog(budgetId, type, description, quantity, sellingPrice);
                }
            } catch (Exception e) {
                Toast.makeText(getActivity(), "Error loading budget: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        editTextBudgetDate.setOnClickListener(v -> new DatePickerDialog(getContext(), date,
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show());

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, new String[]{"Budget", "Expense"});
        spinnerType.setAdapter(typeAdapter);

        return view;
    }

    private void saveBudget() {
        // Make all fields into strings
        String type = spinnerType.getSelectedItem().toString();
        String description = editTextDescription.getText().toString();
        String quantityStr = editTextQuantity.getText().toString();
        String sellingPriceStr = editTextSellingPrice.getText().toString();

        // Check if all fields are filled when saveBudget() is called, if they aren't, carry on, otherwise show a toast pop-up alerting the user
        if (!type.isEmpty() && !description.isEmpty() && !quantityStr.isEmpty() && !sellingPriceStr.isEmpty()) {
            try {
                // Change the datatype by parsing the string argument into an int/double
                int quantity = Integer.parseInt(quantityStr);
                double sellingPrice = Double.parseDouble(sellingPriceStr);
                long dateMillis = calendar.getTimeInMillis(); // Get the date in milliseconds

                // When it's an expense, transform it into a negative number
                if ("Expense".equals(type)) {
                    sellingPrice = -sellingPrice;
                }

                // Insert the values into the database table and show a toast pop-up alerting the user
                dbHelper.insertBudget(type, description, quantity, sellingPrice, dateMillis);
                Toast.makeText(getActivity(), "Budget Saved", Toast.LENGTH_SHORT).show();

                // Clear all input fields after saving
                editTextType.setText("");
                editTextDescription.setText("");
                editTextQuantity.setText("");
                editTextSellingPrice.setText("");

                loadBudgets(); // Reload the list of budgets

            } catch (NumberFormatException e) {
                // Handle number format exception if parsing fails
                Toast.makeText(getActivity(), "Invalid number format. Please check your inputs.", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                // Handle other exceptions that might occur during database operations
                Toast.makeText(getActivity(), "Failed to save budget: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getActivity(), "All fields are required", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint({"Range", "SetTextI18n"})
    private void loadBudgets() {
        try (Cursor cursor = dbHelper.readBudget()) {

            ArrayList<String> listBudgets = new ArrayList<>();

            budgetIds.clear();

            double totalSum = 0;

            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndex("budgetID"));
                int quantity = cursor.getInt(cursor.getColumnIndex("quantity"));
                double sellingPrice = cursor.getDouble(cursor.getColumnIndex("sellingPrice"));
                double total = cursor.getDouble(cursor.getColumnIndex("total"));
                String type = cursor.getString(cursor.getColumnIndex("type"));
                String description = cursor.getString(cursor.getColumnIndex("description"));

                String budget = "Type: " + type +
                        ", Desc: " + description +
                        ", Qty: " + quantity +
                        ", Price: $" + sellingPrice +
                        ", Total: $" + total;

                listBudgets.add(budget);
                budgetIds.add(id);

                totalSum += total;
            }
            adapter.clear();
            adapter.addAll(listBudgets);
            adapter.notifyDataSetChanged();

            textViewAggregateTotal.setText("Aggregate Total: $" + totalSum);
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
        editTextType = new EditText(getContext());
        editTextType.setInputType(InputType.TYPE_CLASS_TEXT);
        editTextType.setText(currentType);

        editTextDescription = new EditText(getContext());
        editTextDescription.setInputType(InputType.TYPE_CLASS_TEXT);
        editTextDescription.setText(currentDescription);

        editTextQuantity = new EditText(getContext());
        editTextQuantity.setInputType(InputType.TYPE_CLASS_NUMBER);
        editTextQuantity.setText(String.valueOf(currentQuantity));

        editTextSellingPrice = new EditText(getContext());
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
                String type = editTextType.getText().toString();
                String description = editTextDescription.getText().toString();
                int quantity = Integer.parseInt(editTextQuantity.getText().toString());
                double sellingPrice = Double.parseDouble(editTextSellingPrice.getText().toString());

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
                Date date = sdf.parse(editTextBudgetDate.getText().toString());
                long dateMillis = date.getTime() / 1000;

                // Do the same we did above, but on update
                if ("Expense".equals(type)) {
                    sellingPrice = -sellingPrice;
                }

                updateBudget(budgetId, type, description, quantity, sellingPrice, dateMillis);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Please enter valid numbers for quantity and price.", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateBudget(int budgetId, String type, String description, int quantity, double sellingPrice, long dateMillis) {
        double total = quantity * sellingPrice; // Calculating total

        try {
            dbHelper.updateBudget(budgetId, type, description, quantity, sellingPrice, total, dateMillis);
            Toast.makeText(getActivity(), "Budget Updated", Toast.LENGTH_SHORT).show();

            loadBudgets(); // Reload the list of budgets to reflect the update
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Listener for date picker dialog
    DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, monthOfYear);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateLabel();
        }
    };

    private void updateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
        editTextBudgetDate.setText(sdf.format(calendar.getTime()));
    }
}
