package com.example.dissertation.ui.inventory;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.dissertation.DatabaseHelper;
import com.example.dissertation.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryFragment extends Fragment {

    private EditText editTextName, editTextQuantity, editTextDescription;
    private Spinner spinnerType, spinnerSubType;
    private ArrayAdapter<String> adapter;
    private DatabaseHelper dbHelper;
    private List<Integer> itemIDs;
    private final Map<String, List<String>> typeSubTypeMapping = new HashMap<>();

    @SuppressLint("Range")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inventory, container, false);

        ListView listViewItems = view.findViewById(R.id.listViewItems);
        Button buttonAddItem = view.findViewById(R.id.buttonAddItem);
        Button buttonFilter = view.findViewById(R.id.buttonFilter);
        Button buttonLoadAll = view.findViewById(R.id.buttonLoadAll);

        dbHelper = new DatabaseHelper(getActivity());
        itemIDs = new ArrayList<>();
        adapter = new ArrayAdapter<>(requireActivity(), android.R.layout.simple_list_item_1, new ArrayList<>());
        listViewItems.setAdapter(adapter);

        loadItems(); // Load data early to ensure UI is populated before any user interaction

        buttonAddItem.setOnClickListener(v -> saveItem());

        listViewItems.setOnItemClickListener((parent, view1, position, id) -> {
            int itemID = itemIDs.get(position); // Retrieve the ID of the item to update

            try (Cursor cursor = dbHelper.readItemByID(itemID)) {
                if (cursor.moveToFirst()) {
                    String itemName = cursor.getString(cursor.getColumnIndex("itemName"));
                    int itemQuantity = cursor.getInt(cursor.getColumnIndex("itemQuantity"));
                    String itemType = cursor.getString(cursor.getColumnIndex("itemType"));
                    String itemSubType = cursor.getString(cursor.getColumnIndex("itemSubType"));
                    String itemDescription = cursor.getString(cursor.getColumnIndex("itemDescription"));

                    showUpdateDialog(itemID, itemName, itemQuantity, itemType, itemSubType, itemDescription);
                }
            } catch (Exception e) {
                Toast.makeText(getActivity(), "Error loading item: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        listViewItems.setOnItemLongClickListener((parent, view12, position, id) -> {
            confirmDeletion(itemIDs.get(position)); // Retrieve the ID of the item to delete
            return true;
        });

        buttonFilter.setOnClickListener(v -> showFilterDialog());

        buttonLoadAll.setOnClickListener(v -> loadItems());

        return view;
    }

    private void saveItem() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        initializeTypeSubTypeMapping();

        // Initialize the input fields
        editTextName = new EditText(getContext());
        editTextName.setHint("Item Name");

        editTextQuantity = new EditText(getContext());
        editTextQuantity.setHint("Quantity");
        editTextQuantity.setInputType(InputType.TYPE_CLASS_NUMBER);

        spinnerType = new Spinner(getContext());
        spinnerSubType = new Spinner(getContext());

        List<String> types = new ArrayList<>(typeSubTypeMapping.keySet());
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, types);
        spinnerType.setAdapter(typeAdapter);

        ArrayAdapter<String> subTypeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, new ArrayList<>(typeSubTypeMapping.get(types.get(0))));
        spinnerSubType.setAdapter(subTypeAdapter);

        editTextDescription = new EditText(getContext());
        editTextDescription.setHint("Description");

        // Layout to contain the EditText and Spinner fields
        LinearLayout layout = new LinearLayout(getContext());

        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 10, 40, 10); // Set padding around the layout

        layout.addView(editTextName);
        layout.addView(editTextQuantity);
        layout.addView(spinnerType);
        layout.addView(spinnerSubType);
        layout.addView(editTextDescription);

        builder.setView(layout); // Set the view to AlertDialog

        // Set up the buttons
        builder.setPositiveButton("Add", (dialog, which) -> {
            // Make all fields into strings
            String itemName = editTextName.getText().toString();
            String itemQuantityStr = editTextQuantity.getText().toString();
            String itemType = spinnerType.getSelectedItem().toString();
            String itemSubType = spinnerSubType.getSelectedItem().toString();
            String itemDescription = editTextDescription.getText().toString();

            try {
                // Change the datatype by parsing the string argument into an int/double
                int itemQuantity = Integer.parseInt(itemQuantityStr);

                // Insert the values into the database table and show a toast pop-up alerting the user
                dbHelper.insertItem(itemName, itemQuantity, itemType, itemSubType, itemDescription);
                Toast.makeText(getActivity(), "Item added successfully", Toast.LENGTH_SHORT).show();

                loadItems(); // Reload the list of items
            } catch (NumberFormatException e) {
                // Handle number format exception if parsing fails
                Toast.makeText(getContext(), "Please enter valid numbers for quantity.", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                // Handle other exceptions that might occur during database operations
                Toast.makeText(getContext(), "Failed to add item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = types.get(position);
                List<String> subTypes = new ArrayList<>(typeSubTypeMapping.get(selectedType));
                subTypeAdapter.clear();
                subTypeAdapter.addAll(subTypes);
                subTypeAdapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        builder.show();
    }

    @SuppressLint("Range")
    private void loadItems() {
        try (Cursor cursor = dbHelper.readItem()) {

            List<String> items = new ArrayList<>();
            itemIDs.clear();

            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndex("itemID"));
                String itemName = cursor.getString(cursor.getColumnIndex("itemName"));
                String itemQuantity = cursor.getString(cursor.getColumnIndex("itemQuantity"));
                String itemType = cursor.getString(cursor.getColumnIndex("itemType"));
                String itemSubType = cursor.getString(cursor.getColumnIndex("itemSubType"));
                String itemDescription = cursor.getString(cursor.getColumnIndex("itemDescription"));

                String itemDetails = itemName +
                        " (" + itemQuantity +
                        ") - " + itemType +
                        " (" + itemSubType +
                        ")" + "\n" + itemDescription;
                items.add(itemDetails);
                itemIDs.add(id);
            }
            adapter.clear();
            adapter.addAll(items);
            adapter.notifyDataSetChanged();
        }
        catch (Exception e) {
            Toast.makeText(getActivity(), "Error loading items: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void deleteItem(int itemID) {
        try {
            dbHelper.deleteItem(itemID);
            Toast.makeText(getActivity(), "Item deleted", Toast.LENGTH_SHORT).show();

            loadItems(); // Reload the items to reflect the deletion
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Failed to delete item: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void confirmDeletion(int itemID) {
        new AlertDialog.Builder(requireActivity())
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Delete", (dialog, which) -> deleteItem(itemID))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @SuppressLint("Range")
    private void showUpdateDialog(int itemID, String currentName, int currentQuantity, String currentType, String currentSubType, String currentDescription) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());

        // Set up the input fields
        editTextName = new EditText(getContext());
        editTextName.setText(currentName);

        editTextQuantity = new EditText(getContext());
        editTextQuantity.setInputType(InputType.TYPE_CLASS_NUMBER);
        editTextQuantity.setText(String.valueOf(currentQuantity));

        spinnerType = new Spinner(getContext());
        spinnerSubType = new Spinner(getContext());

        initializeTypeSubTypeMapping(); // Call method to populate type-subtypes

        List<String> types = new ArrayList<>(typeSubTypeMapping.keySet());

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, types);
        spinnerType.setAdapter(typeAdapter);
        ArrayAdapter<String> subTypeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item);
        spinnerSubType.setAdapter(subTypeAdapter);

        spinnerType.setSelection(typeAdapter.getPosition(currentType));
        List<String> subTypes = typeSubTypeMapping.get(currentType);

        if (subTypes != null) {
            subTypeAdapter.clear();
            subTypeAdapter.addAll(subTypes);
            spinnerSubType.setSelection(subTypeAdapter.getPosition(currentSubType));
        }

        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = types.get(position);
                List<String> subTypes = typeSubTypeMapping.get(selectedType);
                subTypeAdapter.clear();
                subTypeAdapter.addAll(subTypes);
                subTypeAdapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        editTextDescription = new EditText(getContext());
        editTextDescription.setText(currentDescription);

        // Layout to contain the EditText and Spinner fields
        LinearLayout layout = new LinearLayout(getContext());

        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 10, 40, 10); // Set padding around the layout

        layout.addView(editTextName);
        layout.addView(editTextQuantity);
        layout.addView(spinnerType);
        layout.addView(spinnerSubType);
        layout.addView(editTextDescription);

        builder.setView(layout); // Set the view to AlertDialog

        // Set up the buttons
        builder.setPositiveButton("Update", (dialog, which) -> {
            try {
                String itemName = editTextName.getText().toString();
                int itemQuantity = Integer.parseInt(editTextQuantity.getText().toString());
                String itemType = spinnerType.getSelectedItem().toString();
                String itemSubType = spinnerSubType.getSelectedItem().toString();
                String itemDescription = editTextDescription.getText().toString();

                updateItem(itemID, itemName, itemQuantity, itemType, itemSubType, itemDescription);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Please enter valid numbers for quantity.", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        initializeTypeSubTypeMapping();

        spinnerType = new Spinner(getContext());
        spinnerSubType = new Spinner(getContext());

        List<String> types = new ArrayList<>(typeSubTypeMapping.keySet());
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, types);
        spinnerType.setAdapter(typeAdapter);

        // Add "All" option to the list of subtypes and automatically select it,
        // In case the user wants to only filter by Type (without a SubType)
        List<String> initialSubTypes = new ArrayList<>(typeSubTypeMapping.get(types.get(0)));
        initialSubTypes.add(0, "All");
        ArrayAdapter<String> subTypeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, initialSubTypes);
        spinnerSubType.setAdapter(subTypeAdapter);

        // Update sub-types when the type changes
        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = types.get(position);
                List<String> subTypes = new ArrayList<>(typeSubTypeMapping.get(selectedType));
                subTypes.add(0, "All"); // Add "All" option
                subTypeAdapter.clear();
                subTypeAdapter.addAll(subTypes);
                subTypeAdapter.notifyDataSetChanged();
            }

            // Method recommended by the IDE to handle no selection
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Layout to contain the Spinner fields
        LinearLayout layout = new LinearLayout(getContext());

        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 10, 40, 10); // Set padding around the layout

        layout.addView(spinnerType);
        layout.addView(spinnerSubType);

        builder.setView(layout); // Set the view to AlertDialog

        // Set up the buttons
        builder.setPositiveButton("Filter", (dialog, which) -> {
            try {
                String selectedType = spinnerType.getSelectedItem().toString();
                String selectedSubType = spinnerSubType.getSelectedItem().toString();

                // Use "All" to indicate null
                if (selectedSubType.equals("All")) {
                    selectedSubType = null;
                }

                loadFilteredItems(selectedType, selectedSubType);
            } catch (Exception e) {
                Toast.makeText(getActivity(), "Error filtering items: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    @SuppressLint("Range")
    private void loadFilteredItems(String type, String subtype) {
        try (Cursor cursor = dbHelper.readFilteredItems(type, subtype)) {

            List<String> items = new ArrayList<>();

            itemIDs.clear();

            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndex("itemID"));
                String itemName = cursor.getString(cursor.getColumnIndex("itemName"));
                String itemQuantity = cursor.getString(cursor.getColumnIndex("itemQuantity"));
                String itemType = cursor.getString(cursor.getColumnIndex("itemType"));
                String itemSubType = cursor.getString(cursor.getColumnIndex("itemSubType"));
                String itemDescription = cursor.getString(cursor.getColumnIndex("itemDescription"));

                String itemDetails = itemName + " (" +
                        itemQuantity + ") - " +
                        itemType + " (" +
                        itemSubType + ")" +
                        "\n" + itemDescription;
                items.add(itemDetails);
                itemIDs.add(id);
            }
            adapter.clear();
            adapter.addAll(items);
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Error loading filtered items: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void initializeTypeSubTypeMapping() {
        typeSubTypeMapping.put("Crop Products", Arrays.asList("Seeds", "Fertilizers", "Pesticides", "Herbicides", "Other"));
        typeSubTypeMapping.put("Livestock Products", Arrays.asList("Feed", "Veterinary supplies", "Other"));
        typeSubTypeMapping.put("Animals", Arrays.asList(
                "Beef Cattle M", "Beef Cattle F", "Beehives", "Chicken M", "Chicken F",
                "Duck M", "Duck F", "Fish", "Geese M", "Geese F", "Goat M", "Goat F",
                "Horse M", "Horse F", "Pig M", "Pig F", "Queen Bee", "Rabbit M", "Rabbit F",
                "Sheep M", "Sheep F", "Turkey M", "Turkey F", "Worker bees", "Other"
        ));
        typeSubTypeMapping.put("Produce", Arrays.asList("Fruits", "Vegetables", "Grains", "Nuts", "Other"));
        typeSubTypeMapping.put("Dairy", Arrays.asList("Milk", "Cheese", "Yogurt", "Butter", "Other"));
        typeSubTypeMapping.put("Meat", Arrays.asList("Beef", "Pork", "Poultry", "Lamb", "Other"));
        typeSubTypeMapping.put("Processed Foods", Arrays.asList("Jams/Jellies", "Pickles", "Canned Goods", "Dried fruits", "Dried Vegetables", "Other"));
        typeSubTypeMapping.put("Beverages", Arrays.asList(
                "Red Wine", "White Wine", "Rose Wine", "Champagne", "Sparkling Wine", "Whiskey", "Vodka", "Rum", "Tequila",
                "Gin", "Scotch", "Brandy", "Cognac", "Beer (Ale)", "Beer (Lager)", "Beer (Stout)", "Beer (Porter)", "Cider",
                "Other"
        ));
        typeSubTypeMapping.put("Farm Equipment", Arrays.asList(
                "Combine Harvester", "Cultivator", "Fencing Materials", "Forklift", "Grain Bin", "Harrow", "Irrigation System",
                "Livestock Trailer", "Mower", "Plow", "Pruning Shears", "Seeder", "Shovel", "Sprayer", "Tiller", "Tractor", "Wheelbarrow",
                "Other"
        ));
        typeSubTypeMapping.put("Packaging Materials", Arrays.asList("Boxes", "Crates", "Tape", "Labels", "Other"));
        typeSubTypeMapping.put("Clothing", Arrays.asList(
                "Arm Sleeves", "Bandana/Neck Gaiters", "Coveralls", "Ear Protection", "High-Visibility Vests", "Insulated Jackets",
                "Insulated Vests", "Knee Pads", "Protective Eye-wear", "Rain Gear", "Reflective Clothing",
                "Rubber Boots", "Steel-Toed Boots", "Thermal Underwear", "Tool Belts", "Utility Belts",
                "Work Boots", "Work Gloves", "Work hats", "Other"
        ));
    }

    private void updateItem(int itemID, String itemName, int itemQuantity, String itemType, String itemSubType, String itemDescription) {
        try {
            dbHelper.updateItem(itemID, itemName, itemQuantity, itemType, itemSubType, itemDescription);
            Toast.makeText(getActivity(), "Item Updated", Toast.LENGTH_SHORT).show();

            loadItems(); // Reload the list of items to reflect the update
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
