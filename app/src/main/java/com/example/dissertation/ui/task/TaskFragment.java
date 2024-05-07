package com.example.dissertation.ui.task;

import static android.text.format.DateUtils.DAY_IN_MILLIS;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.dissertation.DatabaseHelper;
import com.example.dissertation.R;
import com.example.dissertation.notifications.NotificationHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TaskFragment extends Fragment {
    private static final String TAG = "TaskFragment";
    private DatabaseHelper dbHelper;
    private long selectedDate;
    private ArrayAdapter<String> adapter;
    private final ArrayList<String> taskList = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.UK);
    private NotificationHelper notificationHelper;

    @SuppressLint("Range")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Set up the view and link our .xml file to it
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);

        // Initialize views by their specific IDs
        CalendarView calendarView = view.findViewById(R.id.calendarView);
        ListView listViewTasks = view.findViewById(R.id.listViewTasks);
        Button addButton = view.findViewById(R.id.addTaskButton);

        // Initialize the DatabaseHelper to facilitate database operations (CRUD)
        dbHelper = new DatabaseHelper(getActivity());

        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, taskList);
        listViewTasks.setAdapter(adapter);

        // Initialize the NotificationHelper to facilitate notification capabilities
        notificationHelper = new NotificationHelper(getActivity());

        selectedDate = calendarView.getDate(); // Set selectedDate to the date the user selected on the calendar view

        loadTasks(selectedDate); // Load tasks for the selected date

        // Click listener to show the add dialog when pressed
        addButton.setOnClickListener(v -> showAddTaskDialog());

        // Check if the calendar date has changed and load tasks again if so
        calendarView.setOnDateChangeListener((view12, year, month, dayOfMonth) -> {
            selectedDate = convertToTimestamp(year, month, dayOfMonth);
            loadTasks(selectedDate);
        });

        // Click listener to show the edit dialog menu when a task entry is clicked
        listViewTasks.setOnItemClickListener((parent, view1, position, id) -> {
            try (Cursor cursor = dbHelper.readTask(selectedDate, selectedDate + DAY_IN_MILLIS - 1)) {
                if (cursor != null && cursor.moveToPosition(position)) {
                    // Extract needed data from SQL
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

    // Handle saving all entries inputted by the user
    @SuppressLint("Range")
    private void saveTask(String title, String description, String type, long selectedDate, long startTime, long endTime, int priority, int isCompleted) {
        try {
            if (title.isEmpty() || description.isEmpty() || type.isEmpty()) {
                Toast.makeText(getContext(), "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }
            addTask(title, description, type, selectedDate, startTime, endTime, priority, isCompleted);

        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid format. Please check your inputs.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to save task: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Handle loading tasks
    @SuppressLint("Range")
    private void loadTasks(long date) {
        // Local calendar instance to avoid side effects
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/London"), Locale.UK);
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

        taskList.clear(); // Ensure page is clear before loading

        try (Cursor cursor = dbHelper.readTask(dayStart, dayEnd)) {
            while (cursor.moveToNext()) {
                // Extract needed data from SQL
                String title = cursor.getString(cursor.getColumnIndex("title"));
                String description = cursor.getString(cursor.getColumnIndex("description"));

                // Add the extracted data
                taskList.add(title + " - " + description);
            }
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Error loading tasks: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Handle deletion and visual feedback for the user
    private void deleteTask(int taskId) {
        try {
            dbHelper.deleteTask(taskId);
            Toast.makeText(getContext(), "Task deleted", Toast.LENGTH_SHORT).show();
            loadTasks(selectedDate); // Reload the tasks to reflect the deletion
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to delete task: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Show an alert dialog to make sure the user intends to delete
    private void confirmDeletion(int taskId) {
        new AlertDialog.Builder(requireActivity())
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete this task entry?")
                .setPositiveButton("Delete", (dialog, which) -> deleteTask(taskId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Handle the construction and interaction of the edit dialog pop up
    @SuppressLint("SetTextI18n")
    private void showEditTaskDialog(int taskID, String title, String description, String type, long selectedDate, long startTime, long endTime, int priority, int isCompleted) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Edit Task");

        // Setup input fields
        EditText inputTitle = new EditText(getContext());
        inputTitle.setInputType(InputType.TYPE_CLASS_TEXT);
        inputTitle.setText(title);

        EditText inputDescription = new EditText(getContext());
        inputDescription.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        inputDescription.setText(description);

        // Setup type Spinner
        Spinner typeSpinner = new Spinner(getContext());
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, new String[]{"Task", "Event", "Other"});
        typeSpinner.setAdapter(typeAdapter);
        typeSpinner.setSelection(typeAdapter.getPosition(type));

        EditText inputStartTime = new EditText(getContext());
        inputStartTime.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        inputStartTime.setText(timeFormat.format(new Date(startTime)));

        EditText inputEndTime = new EditText(getContext());
        inputEndTime.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        inputEndTime.setText(timeFormat.format(new Date(endTime)));

        EditText inputPriority = new EditText(getContext());
        inputPriority.setInputType(InputType.TYPE_CLASS_NUMBER);
        inputPriority.setText(String.valueOf(priority));

        EditText inputCompleted = new EditText(getContext());
        inputCompleted.setInputType(InputType.TYPE_CLASS_NUMBER);
        inputCompleted.setText(String.valueOf(isCompleted));

        // Layout to contain all input fields
        LinearLayout layout = new LinearLayout(getContext());

        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 10, 40, 10); // Set padding around the layout

        layout.addView(inputTitle);
        layout.addView(inputDescription);
        layout.addView(typeSpinner);
        layout.addView(inputStartTime);
        layout.addView(inputEndTime);
        layout.addView(inputPriority);
        layout.addView(inputCompleted);

        builder.setView(layout); // Set the view to AlertDialog

        // Setup dialog buttons
        builder.setPositiveButton("Update", (dialog, which) -> updateTask(taskID, inputTitle.getText().toString(), inputDescription.getText().toString(),
                typeSpinner.getSelectedItem().toString(), selectedDate,
                parseTime(inputStartTime.getText().toString()), parseTime(inputEndTime.getText().toString()),
                Integer.parseInt(inputPriority.getText().toString()), Integer.parseInt(inputCompleted.getText().toString())));

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.setNeutralButton("Delete", (dialog, which) -> confirmDeletion(taskID));

        // Display the built dialog
        builder.show();
    }

    // Handle the construction and interaction of the add task dialog pop up
    @SuppressLint("SetTextI18n")
    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Add New Task");

        // Set up the input fields
        EditText inputTitle = new EditText(getContext());
        inputTitle.setInputType(InputType.TYPE_CLASS_TEXT);
        inputTitle.setHint("Title");

        EditText inputDescription = new EditText(getContext());
        inputDescription.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        inputDescription.setHint("Description");

        Spinner typeSpinner = new Spinner(getContext());
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, new String[]{"Task", "Event", "Other"});
        typeSpinner.setAdapter(typeAdapter);

        EditText inputStartTime = new EditText(getContext());
        inputStartTime.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        inputStartTime.setHint("Start Time (HH:mm)");

        EditText inputEndTime = new EditText(getContext());
        inputEndTime.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        inputEndTime.setHint("End Time (HH:mm)");

        EditText inputPriority = new EditText(getContext());
        inputPriority.setInputType(InputType.TYPE_CLASS_NUMBER);
        inputPriority.setHint("Priority (1-10)");

        EditText inputCompleted = new EditText(getContext());
        inputCompleted.setInputType(InputType.TYPE_CLASS_NUMBER);
        inputCompleted.setHint("Completed? (0 or 1)");

        CheckBox alarmCheckbox = new CheckBox(getContext());
        alarmCheckbox.setText("Set reminder");


        // Create layout to hold input fields
        LinearLayout layout = new LinearLayout(getContext());

        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 10, 40, 10); // Set padding around the layout

        layout.addView(inputTitle);
        layout.addView(inputDescription);
        layout.addView(typeSpinner);
        layout.addView(inputStartTime);
        layout.addView(inputEndTime);
        layout.addView(inputPriority);
        layout.addView(inputCompleted);
        layout.addView(alarmCheckbox);

        builder.setView(layout); // Set the view to AlertDialog

        // Setup buttons for dialog
        builder.setPositiveButton("Add", (dialog, which) -> {
            String title = inputTitle.getText().toString();
            String description = inputDescription.getText().toString();
            String type = typeSpinner.getSelectedItem().toString();
            long startTime = parseTime(inputStartTime.getText().toString());
            long endTime = parseTime(inputEndTime.getText().toString());
            int priority = Integer.parseInt(inputPriority.getText().toString());
            int isCompleted = Integer.parseInt(inputCompleted.getText().toString());

            saveTask(title, description, type, selectedDate, startTime, endTime, priority, isCompleted);

            if (alarmCheckbox.isChecked()) {
                long notificationTime = convertToTimestampWithTime(selectedDate, inputStartTime.getText().toString());
                Log.d(TAG, "Scheduling notification at: " + new Date(notificationTime).toString() + " with title: " + title);
                notificationHelper.scheduleNotification(notificationTime, title, "Reminder: " + title, "task");
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        // Display the built dialog
        builder.show();
    }

    // Handle adding task and visual feedback for the user
    private void addTask(String title, String description, String type, long selectedDate, long startTime, long endTime, int priority, int isCompleted) {
        dbHelper.insertTask(title, description, type, selectedDate, startTime, endTime, priority, isCompleted);
        Toast.makeText(getContext(), "Task added", Toast.LENGTH_SHORT).show();
        loadTasks(selectedDate);
    }

    // Handle updating and visual feedback for the user
    private void updateTask(int taskID, String title, String description, String type, long selectedDate, long startTime, long endTime, int priority, int isCompleted) {
        if (title.isEmpty() || description.isEmpty() || type.isEmpty()) {
            Toast.makeText(getContext(), "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }
        dbHelper.updateTask(taskID, title, description, type, selectedDate, startTime, endTime, priority, isCompleted);
        Toast.makeText(getContext(), "Task updated", Toast.LENGTH_SHORT).show();
        loadTasks(selectedDate);
    }

    // Parses time into milliseconds from the start of the day
    private long parseTime(String time) {
        try {
            Date date = timeFormat.parse(time);

            // Return the time in milliseconds modulo 24 hours to get milliseconds from start of the day
            return date != null ? date.getTime() % (24 * 60 * 60 * 1000) : 0;
        } catch (ParseException e) {
            Toast.makeText(getContext(), "Invalid time format", Toast.LENGTH_SHORT).show();
            return 0;
        }
    }

    // Converts a given year, month, and day to a timestamp in milliseconds
    private long convertToTimestamp(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, 0, 0, 0);
        return calendar.getTimeInMillis();
    }

    // Converts a given date and time into a timestamp in milliseconds
    private long convertToTimestampWithTime(long date, String time) {
        // Initialize calendar object to specific UK timezone
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/London"), Locale.UK);
        calendar.setTimeInMillis(date);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.UK);
        sdf.setTimeZone(TimeZone.getTimeZone("Europe/London"));

        try {
            Date timeDate = sdf.parse(time);
            Calendar timeCalendar = Calendar.getInstance();

            // Combine the date with the time
            timeCalendar.setTime(timeDate);

            // Set hours and minutes from the parsed time to the date's calendar
            calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
            calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
            Log.d(TAG, "Converted time: " + calendar.getTime());
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing time: " + time, e);
        }
        return calendar.getTimeInMillis();
    }
}