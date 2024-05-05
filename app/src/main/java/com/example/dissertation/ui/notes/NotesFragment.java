package com.example.dissertation.ui.notes;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.dissertation.DatabaseHelper;
import com.example.dissertation.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class NotesFragment extends Fragment {

    private EditText editTextTitle, editTextContent;
    private ArrayAdapter<String> adapter;
    private DatabaseHelper dbHelper;
    private ArrayList<Integer> noteIds;

    @Override
    @SuppressLint("Range")
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notes, container, false);

        editTextTitle = view.findViewById(R.id.editTextTitle);
        editTextContent = view.findViewById(R.id.editTextContent);
        ListView listViewNotes = view.findViewById(R.id.listViewNotes);
        Button buttonSave = view.findViewById(R.id.buttonSave);
        Button buttonTrash = view.findViewById(R.id.buttonTrash);

        dbHelper = new DatabaseHelper(getActivity());
        noteIds = new ArrayList<>();
        adapter = new ArrayAdapter<>(requireActivity(), android.R.layout.simple_list_item_1, new ArrayList<>());
        listViewNotes.setAdapter(adapter);

        loadNotes();  // Load data early to ensure UI is populated before any user interaction

        buttonSave.setOnClickListener(v -> saveNote());

        buttonTrash.setOnClickListener(v -> openTrash());

        // Click listener for updating a note
        listViewNotes.setOnItemClickListener((parent, view1, position, id) -> {
            int noteId = noteIds.get(position);
            try (Cursor cursor = dbHelper.readNoteById(noteId)) {
                if (cursor.moveToFirst()) {
                    String title = cursor.getString(cursor.getColumnIndex("title"));
                    String content = cursor.getString(cursor.getColumnIndex("content"));
                    showUpdateDialog(noteId, title, content);
                }
            } catch (Exception e) {
                Toast.makeText(getActivity(), "Error loading note: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        // Long click listener for deleting a note
        listViewNotes.setOnItemLongClickListener((parent, view12, position, id) -> {
            confirmDeletion(noteIds.get(position));
            return true;
        });

        return view;
    }

    private void saveNote() {
        String title = editTextTitle.getText().toString();
        String content = editTextContent.getText().toString();

        // Check if text fields are not empty when saveNote() is called, if they are, then insert and clear after
        if (!title.isEmpty() || !content.isEmpty()) {
            try {
                // Insert the values into the database table and show a toast pop-up alerting the user
                dbHelper.insertNote(title, content);
                Toast.makeText(getActivity(), "Saved", Toast.LENGTH_SHORT).show();

                // Clear all input fields after saving
                editTextTitle.setText("");
                editTextContent.setText("");

                loadNotes(); // Reload the notes

            } catch (Exception e) {
                Toast.makeText(getActivity(), "Error saving note: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getActivity(), "Please enter a title or content", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("Range")
    private void loadNotes() {
        try (Cursor cursor = dbHelper.readNote()) {
            ArrayList<String> notes = new ArrayList<>();
            noteIds.clear();
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndex("noteID"));
                String title = cursor.getString(cursor.getColumnIndex("title"));
                String content = cursor.getString(cursor.getColumnIndex("content"));
                notes.add(title + "\n" + content);
                noteIds.add(id);
            }
            adapter.clear();
            adapter.addAll(notes);
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Error loading notes: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void deleteNote(int noteId) {
        try {
            dbHelper.deleteNote(noteId);
            Toast.makeText(getActivity(), "Note deleted", Toast.LENGTH_SHORT).show();

            loadNotes(); // Reload the list of notes
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Error deleting note: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void confirmDeletion(int noteId) {
        new AlertDialog.Builder(requireActivity())
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Delete", (dialog, which) -> deleteNote(noteId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @SuppressLint({"Range", "SetTextI18n"})
    private void showUpdateDialog(final int noteId, String currentTitle, String currentContent) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());

        // Initialization of variables for date strings
        String creationDateString = "";
        String editDateString = "";

        // Use try-with-resources to ensure the Cursor is closed automatically
        try (Cursor cursor = dbHelper.readNote()) {
            if (cursor.moveToFirst()) {
                long creationDate = cursor.getLong(cursor.getColumnIndex("creationDate"));
                long editDate = cursor.getLong(cursor.getColumnIndex("editDate"));

                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.UK);
                creationDateString = sdf.format(new Date(creationDate * 1000)); // Convert to milliseconds
                editDateString = sdf.format(new Date(editDate * 1000)); // Convert to milliseconds
            }
        } catch (Exception e) {
            Log.e("DialogSetup", "Failed to load data", e);
        }

        // Set up the input fields
        editTextTitle = new EditText(getContext());
        editTextTitle.setInputType(InputType.TYPE_CLASS_TEXT);
        editTextTitle.setText(currentTitle);

        editTextContent = new EditText(getContext());
        editTextContent.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editTextContent.setText(currentContent);

        // TextViews for showing creation and edit dates
        TextView creationDateTextView = new TextView(getContext());
        creationDateTextView.setText("Created: " + creationDateString);

        TextView editDateTextView = new TextView(getContext());
        editDateTextView.setText("Last Edited: " + editDateString);

        // Layout to contain the EditText fields
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 10, 40, 10); // Set padding around the layout

        layout.addView(editTextTitle);
        layout.addView(editTextContent);
        layout.addView(creationDateTextView);
        layout.addView(editDateTextView);

        builder.setView(layout); // Set the view to AlertDialog

        // Set up the buttons
        builder.setPositiveButton("Update", (dialog, which) -> {
            try {
                String title = editTextTitle.getText().toString();
                String content = editTextContent.getText().toString();
                updateNote(noteId, title, content);
            } catch (Exception e) {
                Toast.makeText(getActivity(), "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }


    private void updateNote(int noteId, String title, String content) {
        try {
            dbHelper.updateNote(noteId, title, content);
            Toast.makeText(getActivity(), "Note Updated", Toast.LENGTH_SHORT).show();

            loadNotes(); // Reload the list of notes
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Failed to update note: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void openTrash() {
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
        navController.navigate(R.id.action_notes_to_trash);
    }
}
