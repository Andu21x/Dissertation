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
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.UK);

    @SuppressLint("Range")
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

        addButton.setOnClickListener(v -> showAddTaskDialog());

        calendarView.setOnDateChangeListener((view12, year, month, dayOfMonth) -> {
            selectedDate = convertToTimestamp(year, month, dayOfMonth);
            loadTasks(selectedDate);
        });

        listViewTasks.setOnItemClickListener((parent, view1, position, id) -> {
            try (Cursor cursor = dbHelper.readTask(selectedDate, selectedDate + DAY_IN_MILLIS - 1)) {
                if (cursor != null && cursor.moveToPosition(position)) {
                    int taskID = cursor.getInt(cursor.getColumnIndex("taskID"));
                    String title = cursor.getString(cursor.getColumnIndex("title"));
                    String description = cursor.getString(cursor.getColumnIndex("description"));
                    String type = cursor.getString(cursor.getColumnIndex("type"));
                    long startTime = cursor.getLong(cursor.getColumnIndex("startTime"));
                    long endTime = cursor.getLong(cursor.getColumnIndex("endTime"));
                    int priority = cursor.getInt(cursor.getColumnIndex("priority"));
                    int isCompleted = cursor.getInt(cursor.getColumnIndex("isCompleted"));

                    showEditTaskDialog(taskID, title, description, type, selectedDate, startTime, endTime, priority, isCompleted);
                }
            } catch (Exception e) {
                Toast.makeText(getActivity(), "Error loading task: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        return view;
    }

    @SuppressLint("Range")
    private void saveTask(int taskID, String title, String description, String type, long selectedDate, long startTime, long endTime, int priority, int isCompleted) {
        try {
            if (title.isEmpty() || description.isEmpty() || type.isEmpty()) {
                Toast.makeText(getContext(), "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (taskID == -1) {
                // Insert new task
                dbHelper.insertTask(title, description, type, selectedDate, startTime, endTime, priority, isCompleted);
                Toast.makeText(getContext(), "Task added", Toast.LENGTH_SHORT).show();
            } else {
                // Update existing task
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

    private void showEditTaskDialog(int taskID, String title, String description, String type, long selectedDate, long startTime, long endTime, int priority, int isCompleted) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Edit Task");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 10, 40, 10);

        // Setup title EditText
        EditText inputTitle = new EditText(getContext());
        inputTitle.setInputType(InputType.TYPE_CLASS_TEXT);
        inputTitle.setText(title);
        layout.addView(inputTitle);

        // Setup description EditText
        EditText inputDescription = new EditText(getContext());
        inputDescription.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        inputDescription.setText(description);
        layout.addView(inputDescription);

        // Setup type Spinner
        Spinner typeSpinner = new Spinner(getContext());
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, new String[]{"Task", "Event", "Other"});
        typeSpinner.setAdapter(typeAdapter);
        typeSpinner.setSelection(typeAdapter.getPosition(type));
        layout.addView(typeSpinner);

        // Setup startTime EditText
        EditText inputStartTime = new EditText(getContext());
        inputStartTime.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        inputStartTime.setText(timeFormat.format(new Date(startTime)));
        layout.addView(inputStartTime);

        // Setup endTime EditText
        EditText inputEndTime = new EditText(getContext());
        inputEndTime.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        inputEndTime.setText(timeFormat.format(new Date(endTime)));
        layout.addView(inputEndTime);

        // Setup priority EditText
        EditText inputPriority = new EditText(getContext());
        inputPriority.setInputType(InputType.TYPE_CLASS_NUMBER);
        inputPriority.setText(String.valueOf(priority));
        layout.addView(inputPriority);

        // Setup completed EditText
        EditText inputCompleted = new EditText(getContext());
        inputCompleted.setInputType(InputType.TYPE_CLASS_NUMBER);
        inputCompleted.setText(String.valueOf(isCompleted));
        layout.addView(inputCompleted);

        builder.setView(layout);

        // Setup dialog buttons
        builder.setPositiveButton("Update", (dialog, which) -> {
            saveTask(taskID, inputTitle.getText().toString(), inputDescription.getText().toString(),
                    typeSpinner.getSelectedItem().toString(), selectedDate,
                    parseTime(inputStartTime.getText().toString()), parseTime(inputEndTime.getText().toString()),
                    Integer.parseInt(inputPriority.getText().toString()), Integer.parseInt(inputCompleted.getText().toString()));
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.setNeutralButton("Delete", (dialog, which) -> confirmDeletion(taskID));

        builder.show();
    }

    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Add New Task");

        // Create layout to hold input fields
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 10, 40, 10);

        // Input field for the title of the task
        EditText inputTitle = new EditText(getContext());
        inputTitle.setInputType(InputType.TYPE_CLASS_TEXT);
        inputTitle.setHint("Title");
        layout.addView(inputTitle);

        // Input field for the description of the task
        EditText inputDescription = new EditText(getContext());
        inputDescription.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        inputDescription.setHint("Description");
        layout.addView(inputDescription);

        // Spinner for selecting the type of task
        Spinner typeSpinner = new Spinner(getContext());
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, new String[]{"Task", "Event", "Other"});
        typeSpinner.setAdapter(typeAdapter);
        layout.addView(typeSpinner);

        // Input field for the start time
        EditText inputStartTime = new EditText(getContext());
        inputStartTime.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        inputStartTime.setHint("Start Time (HH:mm)");
        layout.addView(inputStartTime);

        // Input field for the end time
        EditText inputEndTime = new EditText(getContext());
        inputEndTime.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        inputEndTime.setHint("End Time (HH:mm)");
        layout.addView(inputEndTime);

        // Input field for priority
        EditText inputPriority = new EditText(getContext());
        inputPriority.setInputType(InputType.TYPE_CLASS_NUMBER);
        inputPriority.setHint("Priority (1-10)");
        layout.addView(inputPriority);

        // Input field for completion status
        EditText inputCompleted = new EditText(getContext());
        inputCompleted.setInputType(InputType.TYPE_CLASS_NUMBER);
        inputCompleted.setHint("Completed? (0 or 1)");
        layout.addView(inputCompleted);

        builder.setView(layout);

        // Setup buttons for dialog
        builder.setPositiveButton("Add", (dialog, which) -> {
            String title = inputTitle.getText().toString();
            String description = inputDescription.getText().toString();
            String type = typeSpinner.getSelectedItem().toString();
            long startTime = parseTime(inputStartTime.getText().toString());
            long endTime = parseTime(inputEndTime.getText().toString());
            int priority = Integer.parseInt(inputPriority.getText().toString());
            int isCompleted = Integer.parseInt(inputCompleted.getText().toString());

            saveTask(-1, title, description, type, selectedDate, startTime, endTime, priority, isCompleted);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void updateTask(int taskID, String title, String description, String type, long selectedDate, long startTime, long endTime, int priority, int isCompleted) {
        if (title.isEmpty() || description.isEmpty() || type.isEmpty()) {
            Toast.makeText(getContext(), "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }
        dbHelper.updateTask(taskID, title, description, type, selectedDate, startTime, endTime, priority, isCompleted);
        Toast.makeText(getContext(), "Task updated", Toast.LENGTH_SHORT).show();
        loadTasks(selectedDate);
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

    private long convertToTimestamp(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, 0, 0, 0);
        return calendar.getTimeInMillis();
    }
}
