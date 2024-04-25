package com.example.dissertation.ui.task;

import static android.text.format.DateUtils.DAY_IN_MILLIS;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.dissertation.DatabaseHelper;
import com.example.dissertation.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TaskFragment extends Fragment {
    private DatabaseHelper dbHelper;
    private long selectedDate;
    private ArrayAdapter<String> adapter;
    private final ArrayList<String> taskList = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);

        CalendarView calendarView = view.findViewById(R.id.calendarView);
        ListView listViewTasks = view.findViewById(R.id.listViewTasks);
        Button addButton = view.findViewById(R.id.addTaskButton);

        dbHelper = new DatabaseHelper(getActivity());
        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, taskList);
        listViewTasks.setAdapter(adapter);

        selectedDate = calendarView.getDate(); // Set selectedDate to the date the user selected on the calendar view

        loadTasks(selectedDate); // Load tasks for the selected date

        addButton.setOnClickListener(v -> showUpdateDialog(null));

        calendarView.setOnDateChangeListener((view12, year, month, dayOfMonth) -> {
            selectedDate = convertToTimestamp(year, month, dayOfMonth);
            loadTasks(selectedDate);
        });

        listViewTasks.setOnItemClickListener((parent, view1, position, id) -> {
            Cursor cursor = dbHelper.readTask(selectedDate, selectedDate + DAY_IN_MILLIS - 1);
            if (cursor != null && cursor.moveToPosition(position)) {
                showUpdateDialog(cursor);
            }
        });

        return view;
    }

    @SuppressLint("Range")
    private void saveTask(Cursor cursor, EditText inputTitle, EditText inputDescription, Spinner typeSpinner, long selectedDate, EditText inputStartTime, EditText inputEndTime, EditText inputPriority, EditText inputCompleted) {
        int taskID = cursor != null ? cursor.getInt(cursor.getColumnIndex("taskID")) : -1;

        String title = inputTitle.getText().toString();
        String description = inputDescription.getText().toString();
        String type = typeSpinner.getSelectedItem().toString();
        long startTime = parseTime(inputStartTime.getText().toString());
        long endTime = parseTime(inputEndTime.getText().toString());

        try {
            // Change the datatype by parsing the string argument into an int/double
            int priority = Integer.parseInt(inputPriority.getText().toString());
            int isCompleted = Integer.parseInt(inputCompleted.getText().toString());

            if (title.isEmpty() || description.isEmpty() || type.isEmpty()) {
                Toast.makeText(getContext(), "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (taskID == -1) {
                // Insert the values into the database table and show a toast pop-up alerting the user
                dbHelper.insertTask(title, description, type, selectedDate, startTime, endTime, priority, isCompleted);
                Toast.makeText(getContext(), "Task added", Toast.LENGTH_SHORT).show();
            } else {
                updateTask(taskID, title, description, type, selectedDate, startTime, endTime, priority, isCompleted);
            }

            loadTasks(selectedDate); // Reload the list of tasks at the selectedDate

        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid format. Please check your inputs.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to save task: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @SuppressLint("Range")
    private void loadTasks(long date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long dayStart = calendar.getTimeInMillis();

        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        long dayEnd = calendar.getTimeInMillis();

        taskList.clear();

        try (Cursor cursor = dbHelper.readTask(dayStart, dayEnd)) {
            while (cursor.moveToNext()) {
                String title = cursor.getString(cursor.getColumnIndex("title"));
                String description = cursor.getString(cursor.getColumnIndex("description"));
                taskList.add(title + " - " + description);
            }
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Error loading tasks: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void deleteTask(int taskId) {
        try {
            dbHelper.deleteTask(taskId);
            Toast.makeText(getContext(), "Task deleted", Toast.LENGTH_SHORT).show();
            loadTasks(selectedDate); // Reload the tasks to reflect the deletion
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to delete task: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void confirmDeletion(int taskId) {
        new AlertDialog.Builder(requireActivity())
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete this task entry?")
                .setPositiveButton("Delete", (dialog, which) -> deleteTask(taskId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @SuppressLint("Range")
    private void showUpdateDialog(Cursor cursor) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(cursor == null ? "Add New Task" : "Edit Task");

        // Set up the input fields
        EditText inputTitle = new EditText(getContext());
        inputTitle.setInputType(InputType.TYPE_CLASS_TEXT);
        inputTitle.setHint("Title");
        if (cursor != null) {
            inputTitle.setText(cursor.getString(cursor.getColumnIndex("title")));
        }

        EditText inputDescription = new EditText(getContext());
        inputDescription.setInputType(InputType.TYPE_CLASS_TEXT);
        inputDescription.setHint("Description");
        if (cursor != null) {
            inputDescription.setText(cursor.getString(cursor.getColumnIndex("description")));
        }

        EditText inputTaskDate = new EditText(getContext());
        inputTaskDate.setInputType(InputType.TYPE_NULL);
        inputTaskDate.setFocusable(false);
        inputTaskDate.setHint("Task Date");
        inputTaskDate.setText(getDateString(selectedDate));

        EditText inputStartTime = new EditText(getContext());
        inputStartTime.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        inputStartTime.setHint("Start Time (HH:mm)");

        EditText inputEndTime = new EditText(getContext());
        inputEndTime.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        inputEndTime.setHint("End Time (HH:mm)");

        EditText inputPriority = new EditText(getContext());
        inputPriority.setInputType(InputType.TYPE_CLASS_NUMBER);
        inputPriority.setHint("Priority (1 to 3)");

        EditText inputCompleted = new EditText(getContext());
        inputCompleted.setInputType(InputType.TYPE_CLASS_NUMBER);
        inputCompleted.setHint("Completed? (0 or 1)");

        // Set up spinner to show up a menu when user clicks on type
        Spinner typeSpinner = new Spinner(getContext());
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, new String[]{"Task", "Event", "Other"});
        typeSpinner.setAdapter(typeAdapter);
        if (cursor != null) {
            int position = typeAdapter.getPosition(cursor.getString(cursor.getColumnIndex("type")));
            typeSpinner.setSelection(position);
        }

        // Layout to contain the EditText fields
        LinearLayout layout = new LinearLayout(getContext());

        layout.setOrientation(LinearLayout.VERTICAL);

        layout.addView(inputTitle);
        layout.addView(inputDescription);
        layout.addView(typeSpinner);
        layout.addView(inputTaskDate);
        layout.addView(inputStartTime);
        layout.addView(inputEndTime);
        layout.addView(inputPriority);
        layout.addView(inputCompleted);

        builder.setView(layout); // Set the view to AlertDialog

        // Set up the buttons
        builder.setPositiveButton(cursor == null ? "Add" : "Update", (dialog, which) -> saveTask(cursor, inputTitle, inputDescription, typeSpinner, selectedDate, inputStartTime, inputEndTime, inputPriority, inputCompleted));

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        if (cursor != null) {
            builder.setNeutralButton("Delete", (dialog, which) -> confirmDeletion(cursor.getInt(cursor.getColumnIndex("taskID"))));
        }

        builder.show();
    }

    private void updateTask(int taskID, String title, String description, String type, long taskDate, long startTime, long endTime, int priority, int isCompleted) {
        try {
            dbHelper.updateTask(taskID, title, description, type, taskDate, startTime, endTime, priority, isCompleted);
            Toast.makeText(getContext(), "Task updated", Toast.LENGTH_SHORT).show();

            loadTasks(selectedDate); // Reload the list of tasks at the selectedDate
        } catch (Exception e) {
            Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private long parseTime(String time) {
        try {
            Date date = timeFormat.parse(time);
            return date != null ? date.getTime() % (24 * 60 * 60 * 1000) : 0;
        } catch (ParseException e) {
            Toast.makeText(getContext(), "Invalid time format", Toast.LENGTH_SHORT).show();
            return 0;
        }
    }

    private String getDateString(long timestamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.UK);
        return dateFormat.format(new Date(timestamp));
    }

    private long convertToTimestamp(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, 0, 0, 0);
        return calendar.getTimeInMillis();
    }
}
